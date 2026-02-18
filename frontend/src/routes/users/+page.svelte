<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, UPDATE_USER } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

  let users: any[] = [];
  let loading = true;
  let error: string | null = null;

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
  <div class="page-header">
    <h1>Users</h1>
    <a href="/users/create" class="add-user-btn">Add User</a>
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
</div>

<!-- Edit Modal -->
{#if showEditModal && editingUser}
  <div class="modal-overlay" on:click={closeEditModal}>
    <div class="modal-content" on:click|stopPropagation>
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
    color: #1e293b;
    font-size: 2rem;
  }

  .add-user-btn {
    background: #3b82f6;
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.375rem;
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .add-user-btn:hover {
    background: #2563eb;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    border: 1px solid #fcc;
  }

  .loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
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
    background: #f9fafb;
    border-radius: 0.5rem;
    color: #64748b;
  }

  /* Table Styles */
  .table-container {
    background: white;
    border-radius: 0.5rem;
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
    border-bottom: 2px solid #e2e8f0;
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
    font-weight: 500;
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
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 500;
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
</style>


