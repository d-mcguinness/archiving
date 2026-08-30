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
        <span class="eyebrow">Users</span>
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
        <div class="table-container table-card">
          <table class="data-table arc-table">
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
                    <a href="/tenants/{data.tenantId}/users/{user.id}" class="btn-action btn-view btn-chip indigo">
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
  <div class="dialog-overlay modal-overlay" on:click={closeAddUserDialog}>
    <div class="dialog modal" on:click|stopPropagation>
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
  h1 { margin: 0 0 0.5rem 0; color: var(--arc-ink); font-size: 2rem; }
  .archive-info { color: var(--arc-muted); margin: 0; font-size: 1rem; }
  .separator { margin: 0 0.5rem; color: var(--arc-faint); }
  .header-actions { display: flex; gap: 1rem; flex-shrink: 0; }
  /* .btn-primary / .btn-secondary come from the kit; this page runs them at a
     compact size. */
  .btn-primary, .btn-secondary { padding: 0.6rem 1.1rem; }
  .access-denied { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; text-align: center; padding: 3rem; }
  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0 0 1rem 0; color: var(--arc-ink); font-size: 2rem; }
  .access-denied p { margin: 0.5rem 0; color: var(--arc-muted); font-size: 1.125rem; }
  .redirect-message { color: var(--arc-link); font-weight: 500; animation: pulse 1.5s ease-in-out infinite; }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
  .stats-bar { background: var(--arc-card); padding: 1rem 1.5rem; border-radius: 1rem; box-shadow: var(--arc-shadow-card); border: 1px solid var(--arc-line); margin-bottom: 1.5rem; }
  .stat-item { display: inline-flex; align-items: center; gap: 0.5rem; }
  .stat-label { color: var(--arc-muted); font-weight: 500; }
  .stat-value { color: var(--arc-ink); font-family: 'Space Grotesk', 'Inter', sans-serif; letter-spacing: -0.02em; font-weight: 700; font-size: 1.25rem; }
  /* .loading / .spinner / .error come from the global kit; only the roomier
     padding and the larger spinner are page-specific. */
  .loading { text-align: center; padding: 4rem 2rem; }
  .spinner { width: 3rem; height: 3rem; border-width: 4px; margin: 0 auto 1rem; }
  .error { text-align: center; padding: 2rem; }
  .empty-state { text-align: center; padding: 4rem 2rem; background: var(--arc-card); border-radius: 1rem; box-shadow: var(--arc-shadow-card); border: 1px solid var(--arc-line); }
  .empty-icon { font-size: 4rem; margin-bottom: 1rem; }
  .empty-title { font-size: 1.5rem; font-weight: 600; color: var(--arc-ink); margin-bottom: 0.5rem; }
  .empty-description { color: var(--arc-muted); margin-bottom: 2rem; }
  /* .table-card + .arc-table from the kit; this table scrolls sideways, runs a
     roomier cell scale and keeps its closing hairline. */
  .table-container { overflow-x: auto; }
  .data-table th, .data-table td { padding: 1rem; vertical-align: baseline; }
  .data-table td { font-size: inherit; }
  .data-table tbody tr:last-child td { border-bottom: 1px solid var(--arc-line); }
  .user-name { display: flex; align-items: center; gap: 0.5rem; }
  .user-avatar { font-size: 1.25rem; }
  .actions-cell { display: flex; gap: 0.5rem; }
  /* .btn-view takes its tint from .btn-chip.indigo; row actions run a size of
     their own, and Remove keeps its bespoke flat red. */
  .btn-action { padding: 0.5rem 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 600; text-decoration: none; border: none; cursor: pointer; box-shadow: none; transition: background 0.18s ease, color 0.18s ease; }
  .btn-action:hover { transform: none; box-shadow: none; }
  .btn-remove { background: linear-gradient(135deg, #ef4444, #dc2626); color: white; }
  .btn-remove:hover { background: linear-gradient(135deg, #dc2626, #b91c1c); }
  /* .modal-overlay + .modal from the kit; this dialog sits above everything,
     runs edge-to-edge sections and sizes itself. */
  .dialog-overlay { z-index: 1000; padding: 0; }
  .dialog { padding: 0; width: 90%; max-width: 500px; }
  .dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 1.5rem; border-bottom: 1px solid var(--arc-line); }
  .dialog-header h2 { margin: 0; font-size: 1.25rem; color: var(--arc-ink); }
  .dialog-close { background: none; border: none; box-shadow: none; font-size: 1.5rem; color: var(--arc-muted); cursor: pointer; padding: 0; width: 2rem; height: 2rem; display: flex; align-items: center; justify-content: center; border-radius: 0.4rem; transition: background 0.18s ease; }
  .dialog-close:hover { background: var(--arc-card-2); transform: none; box-shadow: none; }
  .dialog-body { padding: 1.5rem; }
  .form-group { margin-bottom: 1rem; }
  .form-group label { display: block; margin-bottom: 0.5rem; font-weight: 500; color: var(--arc-ink); }
  .dialog-footer { display: flex; justify-content: flex-end; gap: 1rem; padding: 1.5rem; border-top: 1px solid var(--arc-line); }
  @media (max-width: 768px) { .archive-users-page { padding: 1rem; } .header { flex-direction: column; gap: 1rem; } .header-actions { width: 100%; flex-direction: column; } .data-table { font-size: 0.875rem; } .actions-cell { flex-direction: column; } }

  @media (prefers-reduced-motion: reduce) {
    .redirect-message { animation: none; }
    .btn-primary, .btn-secondary, .btn-action, .dialog-close { transition: none; }
    .btn-primary:hover:not(:disabled) { transform: none; }
  }
</style>
