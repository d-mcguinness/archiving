<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
  }

  export let data: PageData;

  let tenant: any = null;
  let allUsers: any[] = [];
  let tenantUsers: any[] = [];
  let availableUsers: any[] = [];
  let loading = true;
  let error: string | null = null;
  let showAddUserDialog = false;
  let selectedUserId = '';
  let addingUser = false;

  // Security
  let currentRole = '';
  let hasAccess = false;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;
    const authUser = authState.user;

    // Only ADMIN and TENANT can access tenant users page
    if (currentRole !== 'ADMIN' && currentRole !== 'TENANT') {
      hasAccess = false;
      loading = false;

      // Redirect USER to their documents page
      if (currentRole === 'USER' && tenantId && authUser) {
        goto(`/tenants/${tenantId}/users/${authUser.id}/documents`);
      } else {
        goto('/');
      }
      return;
    }

    hasAccess = true;
    await loadTenantAndUsers();
  });

  async function loadTenantAndUsers() {
    try {
      loading = true;

      // Fetch tenant details and all users
      const [tenantResult, usersResult] = await Promise.all([
        client.query({
          query: GET_TENANT,
          variables: { id: data.tenantId },
          fetchPolicy: 'network-only'
        }),
        client.query({
          query: GET_ALL_USERS,
          fetchPolicy: 'network-only'
        })
      ]);

      tenant = tenantResult?.data?.getTenant;
      allUsers = usersResult?.data?.getAllUsers || [];

      if (!tenant) {
        error = 'Tenant not found';
        return;
      }

      // Filter users that belong to this tenant
      // Assuming users have a tenantId or tenant relationship
      // For now, we'll show all users and you can add tenant assignment logic
      tenantUsers = allUsers; // TODO: Filter by actual tenant relationship
      availableUsers = allUsers; // TODO: Filter out users already in tenant

      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load tenant users error:', e);
      toasts.error(`Failed to load data: ${error}`);
    } finally {
      loading = false;
    }
  }

  function openAddUserDialog() {
    showAddUserDialog = true;
    selectedUserId = '';
  }

  function closeAddUserDialog() {
    showAddUserDialog = false;
    selectedUserId = '';
  }

  async function addUserToTenant() {
    if (!selectedUserId) {
      toasts.error('Please select a user');
      return;
    }

    try {
      addingUser = true;

      // TODO: Call mutation to assign user to tenant
      // For now, just show success message
      console.log(`Adding user ${selectedUserId} to tenant ${data.tenantId}`);

      toasts.success('User added to tenant successfully');
      closeAddUserDialog();
      await loadTenantAndUsers();
    } catch (e) {
      const errorMsg = e instanceof Error ? e.message : 'Failed to add user';
      console.error('Add user error:', e);
      toasts.error(`Failed to add user: ${errorMsg}`);
    } finally {
      addingUser = false;
    }
  }

  async function removeUserFromTenant(userId: number, userName: string) {
    if (!confirm(`Remove ${userName} from this tenant?`)) {
      return;
    }

    try {
      // TODO: Call mutation to remove user from tenant
      console.log(`Removing user ${userId} from tenant ${data.tenantId}`);

      toasts.success(`${userName} removed from tenant`);
      await loadTenantAndUsers();
    } catch (e) {
      const errorMsg = e instanceof Error ? e.message : 'Failed to remove user';
      console.error('Remove user error:', e);
      toasts.error(`Failed to remove user: ${errorMsg}`);
    }
  }
</script>

<div class="tenant-users-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to access this page.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[{ label: 'Users' }]}
    />
    <div class="header">
    <div>
      <span class="eyebrow">User management</span>
      <h1>Tenant Users</h1>
      {#if tenant}
        <p class="tenant-info">
          <strong>{tenant.name}</strong>
          {#if tenant.description}
            <span class="separator">•</span>
            <span>{tenant.description}</span>
          {/if}
        </p>
      {/if}
    </div>
    <div class="header-actions">
      <a href="/tenants/{data.tenantId}/users/create" class="btn-create">+ Create User</a>
      <button class="btn-primary" on:click={openAddUserDialog}>
        ➕ Add User to Tenant
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
      <button on:click={loadTenantAndUsers}>Try Again</button>
    </div>
  {:else}
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-label">Total Users:</span>
        <span class="stat-value">{tenantUsers.length}</span>
      </div>
    </div>

    {#if tenantUsers.length === 0}
      <div class="empty-state">
        <p class="empty-icon">👥</p>
        <p class="empty-title">No users in this tenant</p>
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
              <th>Age</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {#each tenantUsers as user (user.id)}
              <tr>
                <td>{user.id}</td>
                <td>
                  <div class="user-name">
                    <span class="user-avatar">👤</span>
                    {user.name}
                  </div>
                </td>
                <td>{user.email}</td>
                <td>{user.age || 'N/A'}</td>
                <td class="actions-cell">
                  <a href="/tenants/{data.tenantId}/users/{user.id}" class="btn-action btn-view">
                    👤 View
                  </a>
                  <button
                    class="btn-action btn-remove"
                    on:click={() => removeUserFromTenant(user.id, user.name)}
                  >
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

<!-- Add User Dialog -->
{#if showAddUserDialog}
  <div class="dialog-overlay" on:click={closeAddUserDialog}>
    <div class="dialog" on:click|stopPropagation>
      <div class="dialog-header">
        <h2>Add User to Tenant</h2>
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
        <button class="btn-secondary" on:click={closeAddUserDialog} disabled={addingUser}>
          Cancel
        </button>
        <button class="btn-primary" on:click={addUserToTenant} disabled={!selectedUserId || addingUser}>
          {addingUser ? '⏳ Adding...' : '➕ Add User'}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .tenant-users-page {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 2rem;
    gap: 2rem;
  }

  h1 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  .tenant-info {
    color: var(--arc-muted, #64748b);
    margin: 0;
    font-size: 1rem;
  }

  .separator {
    margin: 0 0.5rem;
    color: var(--arc-faint, #cbd5e1);
  }

  .header-actions {
    display: flex;
    gap: 1rem;
    flex-shrink: 0;
  }

  .btn-create {
    display: inline-flex;
    align-items: center;
    padding: 0.65rem 1.25rem;
    background: var(--arc-card, #fff);
    color: var(--arc-ink, #1e293b);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 600;
    transition: border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
  }

  .btn-create:hover {
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-link, #4f46e5);
    transform: translateY(-2px);
  }

  .btn-back {
    padding: 0.5rem 1rem;
    background: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #475569);
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 500;
    transition: background 0.2s;
  }

  .btn-back:hover {
    background: var(--arc-chip-slate-hover, #e2e8f0);
  }

  /* .btn-primary inherits the global brand-gradient button styling from app.css */

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
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: var(--arc-muted, #64748b);
    font-size: 1.125rem;
  }

  .redirect-message {
    color: var(--arc-indigo, #6366f1);
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  .btn-secondary {
    padding: 0.65rem 1.25rem;
    background: var(--arc-card, #fff);
    color: var(--arc-ink, #1e293b);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    border-radius: 0.65rem;
    font-weight: 600;
    cursor: pointer;
    transition: border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
    box-shadow: none;
  }

  .btn-secondary:hover:not(:disabled) {
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-link, #4f46e5);
  }

  .stats-bar {
    background: var(--arc-card, #fff);
    padding: 1rem 1.5rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
    margin-bottom: 1.5rem;
  }

  .stat-item {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
  }

  .stat-label {
    color: var(--arc-muted, #64748b);
    font-weight: 500;
  }

  .stat-value {
    color: var(--arc-ink, #0f172a);
    font-family: var(--arc-font-display, 'Space Grotesk', 'Inter', sans-serif);
    letter-spacing: -0.02em;
    font-weight: 700;
    font-size: 1.25rem;
  }

  .loading {
    text-align: center;
    padding: 4rem 2rem;
  }

  .spinner {
    width: 3rem;
    height: 3rem;
    border: 4px solid var(--arc-line-strong, #e2e8f0);
    border-top-color: var(--arc-indigo, #6366f1);
    border-radius: 50%;
    margin: 0 auto 1rem;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .error {
    text-align: center;
    padding: 2rem;
    background: var(--arc-alert-red-bg, #fee2e2);
    border: 1px solid var(--arc-alert-red-border, #fca5a5);
    border-radius: 0.5rem;
    color: var(--arc-alert-red-ink, #991b1b);
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-card, #fff);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .empty-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
  }

  .empty-title {
    font-size: 1.5rem;
    font-weight: 600;
    color: var(--arc-ink, #0f172a);
    margin-bottom: 0.5rem;
  }

  .empty-description {
    color: var(--arc-muted, #64748b);
    margin-bottom: 2rem;
  }

  .table-container {
    background: var(--arc-card, #fff);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    overflow-x: auto;
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
  }

  .data-table thead {
    background: var(--arc-card-2, #f8fafc);
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-weight: 700;
    color: var(--arc-muted, #64748b);
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .data-table td {
    padding: 1rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .data-table tbody tr:hover {
    background: var(--arc-card-2, #f8fafc);
  }

  .user-name {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .user-avatar {
    font-size: 1.25rem;
  }

  .actions-cell {
    display: flex;
    gap: 0.5rem;
  }

  .btn-action {
    padding: 0.5rem 0.75rem;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 600;
    text-decoration: none;
    border: none;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: none;
  }

  .btn-edit {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
  }

  .btn-edit:hover {
    background: var(--arc-chip-indigo-hover, #c7d2fe);
  }

  .btn-remove {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #991b1b);
  }

  .btn-remove:hover {
    background: var(--arc-chip-red-hover, #fecaca);
  }

  .btn-view {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
  }

  .btn-view:hover {
    background: var(--arc-chip-indigo-hover, #c7d2fe);
  }

  /* Dialog Styles */
  .dialog-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--arc-overlay, rgba(0, 0, 0, 0.5));
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  .dialog {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
    max-width: 500px;
    width: 90%;
    max-height: 90vh;
    overflow: auto;
  }

  .dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .dialog-header h2 {
    margin: 0;
    font-size: 1.25rem;
    color: var(--arc-ink, #0f172a);
  }

  .dialog-close {
    background: none;
    border: none;
    box-shadow: none;
    font-size: 1.5rem;
    color: var(--arc-muted, #64748b);
    cursor: pointer;
    padding: 0;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.375rem;
    transition: background 0.2s;
  }

  .dialog-close:hover {
    background: var(--arc-card-2, #f1f5f9);
    transform: none;
    box-shadow: none;
  }

  .dialog-body {
    padding: 1.5rem;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: var(--arc-ink, #0f172a);
  }

  /* select inherits the global Arcana input styling from app.css */

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    padding: 1.5rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  @media (max-width: 768px) {
    .tenant-users-page {
      padding: 1rem;
    }

    .header {
      flex-direction: column;
      gap: 1rem;
    }

    .header-actions {
      width: 100%;
      flex-direction: column;
    }

    .data-table {
      font-size: 0.875rem;
    }

    .actions-cell {
      flex-direction: column;
    }
  }
</style>
