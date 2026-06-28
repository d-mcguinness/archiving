<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_RELEASES, GET_RELEASES_BY_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let dips: any[] = [];
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let currentTenantId: number | null = null;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    currentTenantId = authState.tenantId;

    if (currentRole === 'ADMIN') {
      await Promise.all([loadAllReleases(), loadUsers()]);
    } else if (currentRole === 'TENANT' && currentTenantId) {
      await Promise.all([loadTenantReleases(currentTenantId), loadUsers()]);
    } else {
      goto('/login', { replaceState: true });
    }
  });

  async function loadAllReleases() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_RELEASES,
        fetchPolicy: 'network-only'
      });
      dips = result?.data?.getAllReleases || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load Releases';
      toasts.error(`Failed to load Releases: ${error}`);
    } finally {
      loading = false;
    }
  }

  async function loadTenantReleases(tenantId: number) {
    try {
      loading = true;
      const result = await client.query({
        query: GET_RELEASES_BY_TENANT,
        variables: { tenantId: tenantId.toString() },
        fetchPolicy: 'network-only'
      });
      dips = result?.data?.getReleasesByTenant || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load Releases';
      toasts.error(`Failed to load Releases: ${error}`);
    } finally {
      loading = false;
    }
  }

  async function loadUsers() {
    try {
      const result = await client.query({ query: GET_ALL_USERS });
      users = result?.data?.getAllUsers || [];
    } catch (e) {
      console.error('Failed to load users:', e);
    }
  }

  function getUserName(userId: string) {
    const user = users.find((u: any) => u.id === userId);
    return user ? user.name : `User #${userId}`;
  }

  function getStatusClass(status: string) {
    return 'status-' + status.toLowerCase();
  }
</script>

<svelte:head>
  <title>Releases - Archiving System</title>
</svelte:head>

<div class="dips-page">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Releases' }]} />
  <div class="page-header">
    <h1>📤 Release packages</h1>
    <a href="/release/create" class="btn-create">+ Create Release</a>
  </div>

  {#if error}
    <div class="error">
      {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading Releases...</p>
    </div>
  {:else if dips.length === 0}
    <div class="empty-state">
      <span class="empty-icon">📤</span>
      <h3>No Releases found</h3>
      <p>Create your first Release package to get started.</p>
      <a href="/release/create" class="btn-primary-link">Create Release</a>
    </div>
  {:else}
    <div class="dips-count">
      <span class="count-label">Total Releases:</span>
      <span class="count-value">{dips.length}</span>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
            <th>Standard</th>
            <th>Root Entity</th>
            <th>Status</th>
            <th>Owner</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {#each dips as dip (dip.id)}
            <tr>
              <td class="id-cell">{dip.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="dip-title">{dip.title}</div>
                  {#if dip.description}
                    <div class="dip-description">{dip.description}</div>
                  {/if}
                </div>
              </td>
              <td class="standard-cell">
                <span class="badge standard-badge">{dip.standard}</span>
              </td>
              <td class="entity-cell">
                {#if dip.rootElement}
                  <div class="entity-info">
                    <span class="entity-name">{dip.rootElement.entityName}</span>
                    <span class="entity-type">({dip.rootElement.entityType})</span>
                  </div>
                  {#if dip.rootElement.fields && dip.rootElement.fields.length > 0}
                    <div class="field-count">{dip.rootElement.fields.length} fields</div>
                  {/if}
                {:else}
                  <span class="no-entity">-</span>
                {/if}
              </td>
              <td class="status-cell">
                <span class="badge {getStatusClass(dip.status)}">{dip.status}</span>
              </td>
              <td class="owner-cell">{getUserName(dip.ownerId)}</td>
              <td class="date-cell">{new Date(dip.createdAt).toLocaleDateString()}</td>
              <td class="actions-cell">
                <a href="/release/edit/{dip.id}" class="btn-action btn-edit">Edit</a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

<style>
  .dips-page {
    max-width: 1600px;
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

  .btn-create {
    padding: 0.75rem 1.5rem;
    background: #f97316;
    color: white;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 600;
    transition: all 0.2s;
  }

  .btn-create:hover {
    background: #ea580c;
  }

  .error {
    background: #fee2e2;
    color: #991b1b;
    padding: 1rem;
    border-radius: 0.5rem;
    border: 1px solid #fca5a5;
    margin-bottom: 1.5rem;
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
    border-top: 4px solid #f97316;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .empty-icon { font-size: 5rem; display: block; margin-bottom: 1rem; }
  .empty-state h3 { margin: 0 0 0.5rem 0; color: #1e293b; }
  .empty-state p { margin: 0 0 1.5rem 0; color: #64748b; }

  .btn-primary-link {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background: #f97316;
    color: white;
    text-decoration: none;
    border-radius: 0.5rem;
    font-weight: 600;
  }

  .btn-primary-link:hover { background: #ea580c; }

  .dips-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: #fff7ed;
    border-radius: 0.5rem;
    border: 1px solid #fed7aa;
  }

  .count-label {
    color: #9a3412;
    font-weight: 600;
    text-transform: uppercase;
    font-size: 0.875rem;
    letter-spacing: 0.05em;
  }

  .count-value {
    color: #7c2d12;
    font-weight: 700;
    font-size: 1.25rem;
  }

  .table-container {
    background: white;
    border-radius: 0.75rem;
    overflow-x: auto;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 900px;
  }

  .data-table thead {
    background: #fff7ed;
    border-bottom: 2px solid #fed7aa;
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    color: #9a3412;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .data-table td {
    padding: 1rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .data-table tbody tr:last-child td { border-bottom: none; }
  .data-table tbody tr:hover { background: #fff7ed; }

  .id-cell { color: #64748b; font-family: monospace; font-size: 0.875rem; width: 60px; }

  .title-cell { min-width: 250px; }
  .title-wrapper { display: flex; flex-direction: column; gap: 0.25rem; }
  .dip-title { font-weight: 600; color: #1e293b; }
  .dip-description { font-size: 0.875rem; color: #64748b; }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .standard-badge { background: #ffedd5; color: #9a3412; }

  .badge.status-draft { background: #fef3c7; color: #92400e; }
  .badge.status-prepared { background: #dbeafe; color: #1e40af; }
  .badge.status-disseminated { background: #dcfce7; color: #166534; }
  .badge.status-expired { background: #f1f5f9; color: #475569; }
  .badge.status-rejected { background: #fee2e2; color: #991b1b; }

  .entity-cell { min-width: 150px; }
  .entity-info { display: flex; gap: 0.25rem; align-items: baseline; }
  .entity-name { font-weight: 600; color: #1e293b; font-size: 0.875rem; }
  .entity-type { color: #64748b; font-size: 0.75rem; }
  .field-count { color: #94a3b8; font-size: 0.75rem; margin-top: 0.125rem; }
  .no-entity { color: #cbd5e1; }

  .owner-cell { color: #64748b; font-size: 0.875rem; white-space: nowrap; }
  .date-cell { color: #64748b; font-size: 0.875rem; white-space: nowrap; }
  .actions-cell { white-space: nowrap; }

  .btn-action {
    display: inline-block;
    padding: 0.375rem 0.75rem;
    border: none;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-decoration: none;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-edit { background: #dbeafe; color: #1e40af; }
  .btn-edit:hover { background: #bfdbfe; }

  @media (max-width: 768px) {
    .dips-page { padding: 1rem; }
    .page-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  }
</style>
