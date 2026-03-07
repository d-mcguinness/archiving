<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, UPDATE_USER } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

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

  // Current user role for permissions
  let currentRole = '';
  let hasAccess = false;

  onMount(async () => {
    // Get user role from localStorage
    const role = localStorage.getItem('auth_role');
    currentRole = role || '';

    // Only ADMIN can access the users list page
    if (currentRole !== 'ADMIN') {
      hasAccess = false;
      loading = false;

      // Redirect non-admin users to appropriate pages
      const tenantId = localStorage.getItem('auth_tenantId');
      const user = localStorage.getItem('auth_user');

      if (currentRole === 'TENANT' && tenantId) {
        goto(`/tenants/${tenantId}/users`);
      } else if (currentRole === 'USER' && tenantId && user) {
        try {
          const userData = JSON.parse(user);
          goto(`/tenants/${tenantId}/users/${userData.id}/documents`);
        } catch (e) {
          goto('/');
        }
      } else {
        goto('/login');
      }
      return;
    }

    hasAccess = true;
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

      const response = await fetch('http://localhost:2020/api/upload', {
        method: 'POST',
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
      toasts.success(uploadMessage);
    } catch (e) {
      uploadError = e instanceof Error ? e.message : 'Failed to upload file';
      console.error('Upload error:', e);
      toasts.error(uploadError);
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
  <title>Users - Admin - Archiving System</title>
</svelte:head>

<div class="users-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to access the admin users list.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <div class="page-header">
      <h1>Users Management</h1>
      <a href="/users/create" class="add-user-btn">+ Add User</a>
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
          <div class="upload-success">
            <span class="success-icon">✅</span>
            <span>{uploadMessage}</span>
          </div>
        {/if}

        {#if uploadError}
          <div class="upload-error">
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
        ❌ Error: {error}
      </div>
    {/if}

    {#if loading}
      <div class="loading">
        <div class="spinner"></div>
        <p>Loading users...</p>
      </div>
    {:else if users.length === 0}
      <div class="empty-state">
        <span class="empty-icon">👥</span>
        <h3>No users found</h3>
        <p>Create your first user to get started!</p>
        <a href="/users/create" class="btn-create">Create First User</a>
      </div>
    {:else}
      <div class="users-count">
        <span class="count-label">Total Users:</span>
        <span class="count-value">{users.length}</span>
      </div>

      <div class="table-container">
        <table class="data-table">
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
                  <a href="/users/delete?userId={user.id}" class="btn-action btn-delete">
                    🗑️ Delete
                  </a>
                  <button
                    class="btn-action btn-edit"
                    on:click={() => openEditModal(user)}
                    title="Edit user"
                  >
                    ✏️ Edit
                  </button>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  {/if}
</div>

<!-- Edit Modal -->
{#if showEditModal && editingUser}
  <div class="modal-overlay" on:click={closeEditModal} role="dialog" aria-modal="true">
    <div class="modal-content" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h2>Edit User</h2>
        <button class="modal-close" on:click={closeEditModal} aria-label="Close">✕</button>
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
            {saving ? '⏳ Saving...' : '💾 Save Changes'}
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
    color: #1e293b;
    font-size: 2rem;
  }

  .add-user-btn {
    background: #3b82f6;
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 600;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .add-user-btn:hover {
    background: #2563eb;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  /* Access Denied */
  .access-denied {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    text-align: center;
    padding: 3rem;
  }

  .access-denied-icon {
    font-size: 5rem;
    margin-bottom: 1.5rem;
  }

  .access-denied h1 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: #64748b;
    font-size: 1.125rem;
  }

  .redirect-message {
    color: #3b82f6;
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
    border: 1px solid #fcc;
  }

  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    gap: 1rem;
  }

  .spinner {
    border: 4px solid #f3f4f6;
    border-top: 4px solid #3b82f6;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .empty-icon {
    font-size: 5rem;
    display: block;
    margin-bottom: 1rem;
  }

  .empty-state h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
  }

  .empty-state p {
    margin: 0 0 1.5rem 0;
    color: #64748b;
  }

  .btn-create {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background: #3b82f6;
    color: white;
    text-decoration: none;
    border-radius: 0.5rem;
    font-weight: 600;
    transition: background 0.2s;
  }

  .btn-create:hover {
    background: #2563eb;
  }

  /* Users Count */
  .users-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: #f8fafc;
    border-radius: 0.5rem;
    border: 1px solid #e2e8f0;
  }

  .count-label {
    color: #64748b;
    font-weight: 600;
    text-transform: uppercase;
    font-size: 0.875rem;
    letter-spacing: 0.05em;
  }

  .count-value {
    color: #1e293b;
    font-weight: 700;
    font-size: 1.25rem;
  }

  /* File Upload Section */
  .file-upload-section {
    margin-bottom: 2rem;
  }

  .file-upload-section h2 {
    margin-bottom: 1rem;
    color: #1e293b;
    font-size: 1.25rem;
  }

  .upload-card {
    background: white;
    padding: 2rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
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
    border: 2px dashed #cbd5e1;
    border-radius: 0.5rem;
    cursor: pointer;
    transition: all 0.2s;
    background: #f8fafc;
  }

  .file-label:hover {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  .upload-icon {
    font-size: 2rem;
  }

  .upload-text {
    color: #475569;
    font-weight: 500;
  }

  .upload-success {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 1rem;
    background: #dcfce7;
    border: 1px solid #86efac;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    color: #166534;
  }

  .success-icon {
    font-size: 1.25rem;
  }

  .upload-error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 1rem;
    background: #fee2e2;
    border: 1px solid #fca5a5;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    color: #991b1b;
  }

  .error-icon {
    font-size: 1.25rem;
  }

  .upload-button {
    width: 100%;
    padding: 0.75rem 1.5rem;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .upload-button:hover:not(:disabled) {
    background: #2563eb;
  }

  .upload-button:disabled {
    background: #cbd5e1;
    cursor: not-allowed;
  }

  /* Table Styles */
  .table-container {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow-x: auto;
    border: 1px solid #e2e8f0;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 800px;
  }

  .data-table thead {
    background: #f8fafc;
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    color: #475569;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    white-space: nowrap;
    border-bottom: 2px solid #e2e8f0;
  }

  .data-table tbody tr {
    border-bottom: 1px solid #e2e8f0;
    transition: background-color 0.15s;
  }

  .data-table tbody tr:hover {
    background: #f8fafc;
  }

  .data-table tbody tr:last-child {
    border-bottom: none;
  }

  .data-table td {
    padding: 1rem;
    color: #1e293b;
  }

  .id-cell {
    font-family: 'Monaco', 'Courier New', monospace;
    color: #64748b;
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
    font-weight: 600;
    color: #1e293b;
  }

  .user-email {
    font-size: 0.875rem;
    color: #64748b;
    line-height: 1.4;
  }

  .age-cell {
    color: #64748b;
    font-size: 0.875rem;
    width: 100px;
  }

  .actions-cell {
    text-align: right;
    white-space: nowrap;
    width: 200px;
  }

  .btn-action {
    display: inline-block;
    padding: 0.5rem 1rem;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    font-weight: 600;
    transition: all 0.2s;
    margin-left: 0.5rem;
    border: none;
    cursor: pointer;
    text-decoration: none;
  }

  .btn-edit {
    background: #3b82f6;
    color: white;
  }

  .btn-edit:hover {
    background: #2563eb;
  }

  .btn-delete {
    background: #dc2626;
    color: white;
  }

  .btn-delete:hover {
    background: #b91c1c;
  }

  /* Modal Styles */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 1rem;
  }

  .modal-content {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
    max-width: 500px;
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .modal-header h2 {
    margin: 0;
    font-size: 1.5rem;
    color: #1e293b;
  }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: #64748b;
    cursor: pointer;
    padding: 0.25rem;
    line-height: 1;
    transition: color 0.2s;
  }

  .modal-close:hover {
    color: #1e293b;
  }

  .modal-content form {
    padding: 1.5rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #475569;
    font-size: 0.875rem;
  }

  .form-group input {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .form-group input:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .form-group input:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .modal-actions {
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
    padding-top: 1rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn-primary,
  .btn-secondary {
    padding: 0.75rem 1.5rem;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
    border: none;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-primary:disabled {
    background: #94a3b8;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #cbd5e1;
  }

  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  @media (max-width: 768px) {
    .users-page {
      padding: 1rem;
    }

    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }

    .table-container {
      overflow-x: auto;
    }
  }
</style>

