<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ARCHIVE, GET_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData { tenantId: string; archiveId: string; }
  export let data: PageData;

  let archive: any = null;
  let tenant: any = null;
  let allUsers: any[] = [];
  let assignedUsers: any[] = [];
  let availableUsers: any[] = [];
  let loading = true;
  let error: string | null = null;
  let showAddUserDialog = false;
  let selectedUserId = '';
  let addingUser = false;

  let currentRole = '';
  let hasAccess = false;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;
    const authUser = authState.user;

    if (currentRole !== 'ADMIN' && currentRole !== 'TENANT') {
      hasAccess = false;
      loading = false;
      if (currentRole === 'USER' && tenantId && authUser) {
        goto(`/tenants/${tenantId}/users/${authUser.id}/documents`);
      } else {
        goto('/');
      }
      return;
    }

    hasAccess = true;
    await loadData();
  });

  async function loadData() {
    try {
      loading = true;
      const [archiveResult, tenantResult, usersResult] = await Promise.all([
        client.query({ query: GET_ARCHIVE, variables: { id: data.archiveId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_TENANT, variables: { id: data.tenantId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_USERS, fetchPolicy: 'network-only' })
      ]);

      archive = archiveResult?.data?.getArchive;
      tenant = tenantResult?.data?.getTenant;
      allUsers = usersResult?.data?.getAllUsers || [];

      if (!archive) { error = 'Archive not found'; return; }

      assignedUsers = archive.assignedUsers || [];
      const assignedIds = new Set(assignedUsers.map((u: any) => u.id));
      availableUsers = allUsers.filter((u: any) => !assignedIds.has(u.id));

      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load archive users error:', e);
      toasts.error(`Failed to load data: ${error}`);
    } finally {
      loading = false;
    }
  }

  function openAddUserDialog() { showAddUserDialog = true; selectedUserId = ''; }
  function closeAddUserDialog() { showAddUserDialog = false; selectedUserId = ''; }

  async function addUserToArchive() {
    if (!selectedUserId) { toasts.error('Please select a user'); return; }
    try {
      addingUser = true;
      // TODO: Call mutation to assign user to archive
      console.log(`Adding user ${selectedUserId} to archive ${data.archiveId}`);
      toasts.success('User added to archive successfully');
      closeAddUserDialog();
      await loadData();
    } catch (e) {
      const errorMsg = e instanceof Error ? e.message : 'Failed to add user';
      console.error('Add user error:', e);
      toasts.error(`Failed to add user: ${errorMsg}`);
    } finally {
      addingUser = false;
    }
  }

  async function removeUserFromArchive(userId: number, userName: string) {
    if (!confirm(`Remove ${userName} from this archive?`)) return;
    try {
      // TODO: Call mutation to remove user from archive
      console.log(`Removing user ${userId} from archive ${data.archiveId}`);
      toasts.success(`${userName} removed from archive`);
      await loadData();
    } catch (e) {
      const errorMsg = e instanceof Error ? e.message : 'Failed to remove user';
      console.error('Remove user error:', e);
      toasts.error(`Failed to remove user: ${errorMsg}`);
    }
  }
</script>

<div class="archive-users-page">
  {#if !hasAccess && !loading}
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to access this page.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[
        { label: 'Archives', href: `/tenants/${data.tenantId}/archives` },
        { label: archive?.title || 'Archive', href: `/tenants/${data.tenantId}/archives/${data.archiveId}` },
        { label: 'Users' }
      ]}
    />
    <div class="header">
      <div>
        <h1>Archive Users</h1>
        {#if archive}
          <p class="archive-info">
            <strong>{archive.title}</strong>
            {#if archive.standard}
              <span class="separator">•</span>
              <span>{archive.standard}</span>
            {/if}
          </p>
        {/if}
      </div>
      <div class="header-actions">
        <button class="btn-primary" on:click={openAddUserDialog}>
          ➕ Add User to Archive
        </button>
      </div>
    </div>

    {#if loading}
      <div class="loading">
        <div class="spinner"></div>
        <p>Loading users...</p>
      </div>
    {:else if error}
      <div class="error">
        <p>❌ Error: {error}</p>
        <button on:click={loadData}>Try Again</button>
      </div>
    {:else}
      <div class="stats-bar">
        <div class="stat-item">
          <span class="stat-label">Assigned Users:</span>
          <span class="stat-value">{assignedUsers.length}</span>
        </div>
      </div>

      {#if assignedUsers.length === 0}
        <div class="empty-state">
          <p class="empty-icon">👥</p>
          <p class="empty-title">No users assigned to this archive</p>
          <p class="empty-description">Add users to get started</p>
          <button class="btn-primary" on:click={openAddUserDialog}>
            ➕ Add First User
          </button>
        </div>
      {:else}
        <div class="table-container">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {#each assignedUsers as user (user.id)}
                <tr>
                  <td>{user.id}</td>
                  <td>
                    <div class="user-name">
                      <span class="user-avatar">👤</span>
                      {user.name}
                    </div>
                  </td>
                  <td>{user.email}</td>
                  <td class="actions-cell">
                    <a href="/tenants/{data.tenantId}/users/{user.id}" class="btn-action btn-view">
                      👤 View
                    </a>
                    <button class="btn-action btn-remove" on:click={() => removeUserFromArchive(user.id, user.name)}>
                      ✖️ Remove
                    </button>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    {/if}
  {/if}
</div>

{#if showAddUserDialog}
  <div class="dialog-overlay" on:click={closeAddUserDialog}>
    <div class="dialog" on:click|stopPropagation>
      <div class="dialog-header">
        <h2>Add User to Archive</h2>
        <button class="dialog-close" on:click={closeAddUserDialog}>✕</button>
      </div>
      <div class="dialog-body">
        <div class="form-group">
          <label for="user-select">Select User</label>
          <select id="user-select" bind:value={selectedUserId} disabled={addingUser}>
            <option value="">-- Choose a user --</option>
            {#each availableUsers as user (user.id)}
              <option value={user.id}>{user.name} ({user.email})</option>
            {/each}
          </select>
        </div>
      </div>
      <div class="dialog-footer">
        <button class="btn-secondary" on:click={closeAddUserDialog} disabled={addingUser}>Cancel</button>
        <button class="btn-primary" on:click={addUserToArchive} disabled={!selectedUserId || addingUser}>
          {addingUser ? '⏳ Adding...' : '➕ Add User'}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .archive-users-page { max-width: 1200px; margin: 0 auto; padding: 2rem; }
  .header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem; gap: 2rem; }
  h1 { margin: 0 0 0.5rem 0; color: #1e293b; font-size: 2rem; }
  .archive-info { color: #64748b; margin: 0; font-size: 1rem; }
  .separator { margin: 0 0.5rem; color: #cbd5e1; }
  .header-actions { display: flex; gap: 1rem; flex-shrink: 0; }
  .btn-primary { padding: 0.5rem 1rem; background: #3b82f6; color: white; border: none; border-radius: 0.5rem; font-weight: 600; cursor: pointer; transition: background 0.2s; }
  .btn-primary:hover:not(:disabled) { background: #2563eb; }
  .btn-primary:disabled { background: #cbd5e1; cursor: not-allowed; }
  .btn-secondary { padding: 0.5rem 1rem; background: #f1f5f9; color: #475569; border: none; border-radius: 0.5rem; font-weight: 500; cursor: pointer; transition: background 0.2s; }
  .btn-secondary:hover:not(:disabled) { background: #e2e8f0; }
  .access-denied { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; text-align: center; padding: 3rem; }
  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0 0 1rem 0; color: #1e293b; font-size: 2rem; }
  .access-denied p { margin: 0.5rem 0; color: #64748b; font-size: 1.125rem; }
  .redirect-message { color: #3b82f6; font-weight: 500; animation: pulse 1.5s ease-in-out infinite; }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
  .stats-bar { background: white; padding: 1rem 1.5rem; border-radius: 0.5rem; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0; margin-bottom: 1.5rem; }
  .stat-item { display: inline-flex; align-items: center; gap: 0.5rem; }
  .stat-label { color: #64748b; font-weight: 500; }
  .stat-value { color: #1e293b; font-weight: 700; font-size: 1.25rem; }
  .loading { text-align: center; padding: 4rem 2rem; }
  .spinner { width: 3rem; height: 3rem; border: 4px solid #f3f4f6; border-top-color: #3b82f6; border-radius: 50%; margin: 0 auto 1rem; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .error { text-align: center; padding: 2rem; background: #fee2e2; border: 1px solid #fca5a5; border-radius: 0.5rem; color: #991b1b; }
  .empty-state { text-align: center; padding: 4rem 2rem; background: white; border-radius: 0.5rem; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0; }
  .empty-icon { font-size: 4rem; margin-bottom: 1rem; }
  .empty-title { font-size: 1.5rem; font-weight: 600; color: #1e293b; margin-bottom: 0.5rem; }
  .empty-description { color: #64748b; margin-bottom: 2rem; }
  .table-container { background: white; border-radius: 0.5rem; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); overflow-x: auto; border: 1px solid #e2e8f0; }
  .data-table { width: 100%; border-collapse: collapse; }
  .data-table thead { background: #f8fafc; }
  .data-table th { padding: 1rem; text-align: left; font-weight: 600; color: #475569; border-bottom: 1px solid #e2e8f0; }
  .data-table td { padding: 1rem; border-bottom: 1px solid #f1f5f9; }
  .data-table tbody tr:hover { background: #f8fafc; }
  .user-name { display: flex; align-items: center; gap: 0.5rem; }
  .user-avatar { font-size: 1.25rem; }
  .actions-cell { display: flex; gap: 0.5rem; }
  .btn-action { padding: 0.5rem 0.75rem; border-radius: 0.25rem; font-size: 0.875rem; font-weight: 500; text-decoration: none; border: none; cursor: pointer; transition: all 0.2s; }
  .btn-remove { background: #ef4444; color: white; }
  .btn-remove:hover { background: #dc2626; }
  .btn-view { background: #f59e0b; color: white; }
  .btn-view:hover { background: #d97706; }
  .dialog-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
  .dialog { background: white; border-radius: 0.75rem; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1); max-width: 500px; width: 90%; max-height: 90vh; overflow: auto; }
  .dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 1.5rem; border-bottom: 1px solid #e2e8f0; }
  .dialog-header h2 { margin: 0; font-size: 1.25rem; color: #1e293b; }
  .dialog-close { background: none; border: none; font-size: 1.5rem; color: #64748b; cursor: pointer; padding: 0; width: 2rem; height: 2rem; display: flex; align-items: center; justify-content: center; border-radius: 0.25rem; transition: background 0.2s; }
  .dialog-close:hover { background: #f1f5f9; }
  .dialog-body { padding: 1.5rem; }
  .form-group { margin-bottom: 1rem; }
  .form-group label { display: block; margin-bottom: 0.5rem; font-weight: 500; color: #1e293b; }
  .form-group select { width: 100%; padding: 0.5rem; border: 1px solid #e2e8f0; border-radius: 0.5rem; font-size: 1rem; }
  .dialog-footer { display: flex; justify-content: flex-end; gap: 1rem; padding: 1.5rem; border-top: 1px solid #e2e8f0; }
  @media (max-width: 768px) { .archive-users-page { padding: 1rem; } .header { flex-direction: column; gap: 1rem; } .header-actions { width: 100%; flex-direction: column; } .data-table { font-size: 0.875rem; } .actions-cell { flex-direction: column; } }
</style>
