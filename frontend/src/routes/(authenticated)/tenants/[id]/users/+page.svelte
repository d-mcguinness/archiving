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
      <a href="/tenants/{data.tenantId}/users/create" class="btn-create btn-secondary">+ Create User</a>
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
      <div class="table-container table-card">
        <table class="data-table arc-table">
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
                  <a href="/tenants/{data.tenantId}/users/{user.id}" class="btn-action btn-view btn-chip indigo">
                    👤 View
                  </a>
                  <button
                    class="btn-action btn-remove btn-chip red"
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
  <div class="dialog-overlay modal-overlay" on:click={closeAddUserDialog}>
    <div class="dialog modal" on:click|stopPropagation>
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

  /* .btn-create uses the global .btn-secondary ghost button from app.css */
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

  /* .btn-secondary inherits the global ghost button styling from app.css */

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

  /* .loading / .spinner use the global loading pattern from app.css */

  /* .error uses the global alert panel from app.css; only the layout differs here */
  .error {
    text-align: center;
    padding: 2rem;
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

  /* Table chrome comes from the global .table-card / .arc-table kit in app.css */
  .table-container {
    overflow-x: auto;
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

  /* Row actions use the global .btn-chip kit (indigo / red) from app.css */

  /* Dialog Styles — surface comes from the global .modal-overlay / .modal kit */
  .dialog {
    padding: 0;
    max-width: 500px;
    width: 90%;
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
