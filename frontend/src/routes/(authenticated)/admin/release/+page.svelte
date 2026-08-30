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
    <div class="page-heading">
      <span class="eyebrow">Admin console</span>
      <h1>📤 Release packages</h1>
    </div>
    <a href="/release/create" class="btn-create btn-primary">+ Create Release</a>
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
      <a href="/release/create" class="btn-primary-link btn-primary">Create Release</a>
    </div>
  {:else}
    <div class="dips-count">
      <span class="count-label">Total Releases:</span>
      <span class="count-value">{dips.length}</span>
    </div>

    <div class="table-container table-card">
      <table class="data-table arc-table">
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
                <span class="badge orange">{dip.standard}</span>
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
                <a href="/release/edit/{dip.id}" class="btn-action btn-edit btn-chip indigo">Edit</a>
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
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  /* .btn-create, .error and .spinner use the global kit (app.css);
     this loader stacks a caption under the spinner. */
  .loading {
    flex-direction: column;
    min-height: 400px;
    gap: 1rem;
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .empty-icon { font-size: 5rem; display: block; margin-bottom: 1rem; }
  .empty-state h3 { margin: 0 0 0.5rem 0; color: var(--arc-ink, #0f172a); }
  .empty-state p { margin: 0 0 1.5rem 0; color: var(--arc-muted, #64748b); }

  .dips-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: var(--arc-card, #fff);
    border-radius: 0.75rem;
    border: 1px solid var(--arc-line, #e8edf3);
    border-left: 3px solid #f97316;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .count-label {
    color: var(--arc-muted, #64748b);
    font-weight: 700;
    text-transform: uppercase;
    font-size: 0.78rem;
    letter-spacing: 0.08em;
  }

  .count-value {
    color: var(--arc-ink, #0f172a);
    font-weight: 700;
    font-size: 1.25rem;
  }

  /* Table chrome comes from .table-card / table.arc-table. */
  .table-container { overflow-x: auto; }

  .data-table { min-width: 900px; }

  .id-cell { color: var(--arc-muted, #64748b); font-family: monospace; font-size: 0.875rem; width: 60px; }

  .title-cell { min-width: 250px; }
  .title-wrapper { display: flex; flex-direction: column; gap: 0.25rem; }
  .dip-title { font-weight: 600; color: var(--arc-ink, #1e293b); }
  .dip-description { font-size: 0.875rem; color: var(--arc-muted, #64748b); }

  /* .badge base + hues are global; these Release statuses are page-specific. */
  .badge.status-draft { background: var(--arc-chip-amber-bg, #fef3c7); color: var(--arc-chip-amber-ink, #92400e); }
  .badge.status-prepared { background: #dbeafe; color: #1e40af; }
  .badge.status-disseminated { background: var(--arc-chip-green-bg, #dcfce7); color: var(--arc-chip-green-ink, #166534); }
  .badge.status-expired { background: var(--arc-chip-slate-bg, #f1f5f9); color: var(--arc-chip-slate-ink, #475569); }
  .badge.status-rejected { background: var(--arc-chip-red-bg, #fee2e2); color: var(--arc-chip-red-ink, #991b1b); }

  .entity-cell { min-width: 150px; }
  .entity-info { display: flex; gap: 0.25rem; align-items: baseline; }
  .entity-name { font-weight: 600; color: var(--arc-ink, #1e293b); font-size: 0.875rem; }
  .entity-type { color: var(--arc-muted, #64748b); font-size: 0.75rem; }
  .field-count { color: var(--arc-faint, #94a3b8); font-size: 0.75rem; margin-top: 0.125rem; }
  .no-entity { color: var(--arc-faint, #cbd5e1); }

  .owner-cell { color: var(--arc-muted, #64748b); font-size: 0.875rem; white-space: nowrap; }
  .date-cell { color: var(--arc-muted, #64748b); font-size: 0.875rem; white-space: nowrap; }
  .actions-cell { white-space: nowrap; }

  /* Row actions ride on .btn-chip (app.css). */

  @media (max-width: 768px) {
    .dips-page { padding: 1rem; }
    .page-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  }

  /* No blue chip token exists — dark-theme override keeps the blue hue readable */
  :global(html[data-theme='dark']) .badge.status-prepared {
    background: rgba(59, 130, 246, 0.18);
    color: #93c5fd;
  }
</style>
