<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, UPDATE_USER } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { authHeaders, API_BASE } from '$lib/api';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let users: any[] = [];
  let loading = true;
  let error: string | null = null;

  // File upload state
  let selectedFile: File | null = null;
  let uploading = false;
  let uploadMessage = '';
  let uploadError = '';


  // Edit modal state
  let showEditModal = false;
  let editingUser: any = null;
  let editForm = {
    name: '',
    email: '',
    age: null as number | null
  };
  let saving = false;

  onMount(async () => {
    await loadUsers();
  });

  async function loadUsers() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_USERS,
        fetchPolicy: 'network-only'
      });
      users = result?.data?.getAllUsers || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load users error:', e);
    } finally {
      loading = false;
    }
  }

  async function mimicUser(user: any) {
    const token = 'Bearer_mimic_' + user.id + '_USER_' + Date.now();
    const mimicData = {
      id: user.id,
      username: user.name?.toLowerCase().replace(/\s+/g, '') || 'user',
      name: user.name,
      email: user.email,
      role: 'USER',
    };
    auth.login(token, mimicData, 'USER', null);
    toasts.success(`Now viewing as ${user.name}`);
    goto('/');
  }

  function handleFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      selectedFile = input.files[0];
      uploadMessage = '';
      uploadError = '';
    }
  }

  async function handleUpload() {
    if (!selectedFile) {
      uploadError = 'Please select a file first';
      return;
    }

    uploading = true;
    uploadMessage = '';
    uploadError = '';

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const response = await fetch(`${API_BASE}/api/upload`, {
        method: 'POST',
        headers: { ...authHeaders() },
        body: formData
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Upload failed: ${response.status}`);
      }

      const result = await response.json();
      uploadMessage = result.message || 'File uploaded successfully!';
      selectedFile = null;

      // Reset file input
      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
      if (fileInput) fileInput.value = '';

      // Reload users list to show newly uploaded users
      await loadUsers();
    } catch (e) {
      uploadError = e instanceof Error ? e.message : 'Failed to upload file';
      console.error('Upload error:', e);
    } finally {
      uploading = false;
    }
  }

  function openEditModal(user: any) {
    editingUser = user;
    editForm = {
      name: user.name,
      email: user.email,
      age: user.age
    };
    showEditModal = true;
  }

  function closeEditModal() {
    showEditModal = false;
    editingUser = null;
    editForm = {
      name: '',
      email: '',
      age: null
    };
  }

  async function handleSaveEdit() {
    if (!editingUser) return;

    // Validate
    if (!editForm.name.trim()) {
      toasts.error('Name is required');
      return;
    }
    if (!editForm.email.trim()) {
      toasts.error('Email is required');
      return;
    }

    saving = true;
    try {
      const result = await client.mutate({
        mutation: UPDATE_USER,
        variables: {
          id: editingUser.id,
          input: {
            name: editForm.name.trim(),
            email: editForm.email.trim(),
            age: editForm.age
          }
        }
      });

      if (result.data?.updateUser) {
        toasts.success('User updated successfully');
        closeEditModal();
        await loadUsers(); // Reload the list
      }
    } catch (e) {
      console.error('Update user error:', e);
      toasts.error(e instanceof Error ? e.message : 'Failed to update user');
    } finally {
      saving = false;
    }
  }
</script>

<svelte:head>
  <title>Users - Archiving System</title>
</svelte:head>

<div class="users-page">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Users' }]} />
  <div class="page-header">
    <div class="page-heading">
      <span class="eyebrow">Admin console</span>
      <h1>Users</h1>
    </div>
    <a href="/users/create" class="add-user-btn btn-primary">Add User</a>
  </div>

  <!-- File Upload Section -->
  <div class="file-upload-section">
    <h2>📤 Upload Users File</h2>
    <div class="upload-card">
      <div class="upload-area">
        <input
          type="file"
          id="file-upload"
          on:change={handleFileSelect}
          disabled={uploading}
          class="file-input"
        />
        <label for="file-upload" class="file-label">
          <span class="upload-icon">📁</span>
          <span class="upload-text">
            {selectedFile ? selectedFile.name : 'Choose a file to upload'}
          </span>
        </label>
      </div>

      {#if uploadMessage}
        <div class="upload-success success">
          <span class="success-icon">✅</span>
          <span>{uploadMessage}</span>
        </div>
      {/if}

      {#if uploadError}
        <div class="upload-error error">
          <span class="error-icon">❌</span>
          <span>{uploadError}</span>
        </div>
      {/if}

      <button
        class="upload-button"
        on:click={handleUpload}
        disabled={!selectedFile || uploading}
      >
        {uploading ? '⏳ Uploading...' : '📤 Upload File'}
      </button>
    </div>
  </div>

  {#if error}
    <div class="error">
      Error: {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
    </div>
  {:else if users.length === 0}
    <div class="empty-state">
      <p>No users found. Create your first user to get started!</p>
    </div>
  {:else}
    <div class="table-container table-card">
      <table class="data-table arc-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>Age</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {#each users as user (user.id)}
            <tr>
              <td class="id-cell">{user.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="user-title">{user.name}</div>
                  <div class="user-email">{user.email}</div>
                </div>
              </td>
              <td class="age-cell">{user.age || '-'}</td>
              <td class="actions-cell">
                <button
                  class="btn-action btn-mimic btn-chip violet"
                  on:click={() => mimicUser(user)}
                  title="Sign in as this user"
                >
                  🎭 Mimic
                </button>
                <button
                  class="btn-action btn-edit btn-chip indigo"
                  on:click={() => openEditModal(user)}
                  title="Edit user"
                >
                  ✏️ Edit
                </button>
                <a href="/users/delete?userId={user.id}" class="btn-action btn-delete btn-chip red">
                  🗑️ Delete
                </a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

<!-- Edit Modal -->
{#if showEditModal && editingUser}
  <div class="modal-overlay" on:click={closeEditModal}>
    <div class="modal-content modal" on:click|stopPropagation>
      <div class="modal-header">
        <h2>Edit User</h2>
        <button class="modal-close" on:click={closeEditModal}>✕</button>
      </div>

      <form on:submit|preventDefault={handleSaveEdit}>
        <div class="form-group">
          <label for="edit-name">Name *</label>
          <input
            id="edit-name"
            type="text"
            bind:value={editForm.name}
            placeholder="Enter name"
            required
            disabled={saving}
          />
        </div>

        <div class="form-group">
          <label for="edit-email">Email *</label>
          <input
            id="edit-email"
            type="email"
            bind:value={editForm.email}
            placeholder="Enter email"
            required
            disabled={saving}
          />
        </div>

        <div class="form-group">
          <label for="edit-age">Age</label>
          <input
            id="edit-age"
            type="number"
            bind:value={editForm.age}
            placeholder="Enter age"
            min="0"
            max="150"
            disabled={saving}
          />
        </div>

        <div class="modal-actions">
          <button
            type="button"
            class="btn-secondary"
            on:click={closeEditModal}
            disabled={saving}
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            disabled={saving}
          >
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}

<style>
  .users-page {
    max-width: 1400px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
  }

  .page-header h1 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  /* .add-user-btn, .error, .loading and .spinner use the global kit (app.css). */
  .loading {
    min-height: 400px;
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    color: var(--arc-muted, #64748b);
  }

  /* File Upload Section */
  .file-upload-section {
    margin-bottom: 2rem;
  }

  .file-upload-section h2 {
    margin-bottom: 1rem;
    color: var(--arc-ink, #0f172a);
    font-size: 1.25rem;
  }

  .upload-card {
    background: var(--arc-card, #fff);
    padding: 2rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .upload-area {
    margin-bottom: 1.5rem;
  }

  .file-input {
    display: none;
  }

  .file-label {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1.5rem;
    border: 2px dashed var(--arc-line-strong, #cbd5e1);
    border-radius: 0.6rem;
    cursor: pointer;
    transition: all 0.2s ease;
    background: var(--arc-card-2, #f8fafc);
  }

  .file-label:hover {
    border-color: var(--arc-indigo, #6366f1);
    background: var(--arc-chip-soft-indigo-bg, #eef2ff);
  }

  .upload-icon {
    font-size: 2rem;
  }

  .upload-text {
    color: var(--arc-body, #475569);
    font-weight: 500;
  }

  /* Upload feedback rows are the global .success / .error panels with an icon. */
  .upload-success,
  .upload-error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .success-icon {
    font-size: 1.25rem;
  }

  .error-icon {
    font-size: 1.25rem;
  }

  /* .upload-button inherits the global brand-gradient button styling from app.css */
  .upload-button {
    width: 100%;
  }

  /* Table chrome comes from .table-card / table.arc-table; only the
     page-specific scroll + column sizing stays local. */
  .table-container {
    overflow-x: auto;
  }

  .data-table {
    min-width: 800px;
  }

  .data-table th {
    white-space: nowrap;
  }

  .id-cell {
    font-family: 'Monaco', 'Courier New', monospace;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    width: 60px;
  }

  .title-cell {
    min-width: 300px;
    max-width: 400px;
  }

  .title-wrapper {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .user-title {
    font-weight: 500;
    color: var(--arc-ink, #1e293b);
  }

  .user-email {
    font-size: 0.875rem;
    color: var(--arc-muted, #64748b);
    line-height: 1.4;
  }

  .age-cell {
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    width: 100px;
  }

  .actions-cell {
    text-align: right;
    white-space: nowrap;
    width: 200px;
  }

  /* Row actions ride on .btn-chip; only the row spacing is local. */
  .btn-action {
    margin-left: 0.5rem;
  }

  /* Modal Styles — .modal-overlay/.modal come from app.css; this dialog
     pads its own header/body sections. */
  .modal {
    padding: 0;
    max-width: 500px;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .modal-header h2 {
    margin: 0;
    font-size: 1.5rem;
    color: var(--arc-ink, #0f172a);
  }

  .modal-close {
    background: none;
    border: none;
    box-shadow: none;
    font-size: 1.5rem;
    color: var(--arc-muted, #64748b);
    cursor: pointer;
    padding: 0.25rem;
    line-height: 1;
    transition: color 0.2s;
  }

  .modal-close:hover {
    color: var(--arc-ink, #1e293b);
  }

  .modal-content form {
    padding: 1.5rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group input:disabled {
    background: var(--arc-card-2, #f1f5f9);
    cursor: not-allowed;
  }

  .modal-actions {
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
    padding-top: 1rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  /* .btn-primary / .btn-secondary come from the global kit. */
</style>


