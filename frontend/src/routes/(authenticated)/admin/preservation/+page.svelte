<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_PRESERVATIONS, GET_PRESERVATIONS_BY_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let aips: any[] = [];
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let currentTenantId: number | null = null;

  // Extract dialog state
  let showExtractDialog = false;
  let selectedPreservationForExtract: any = null;
  let extractPassword = '';
  let extracting = false;
  let extractError: string | null = null;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    currentTenantId = authState.tenantId;

    if (currentRole === 'ADMIN') {
      await Promise.all([loadAllPreservations(), loadUsers()]);
    } else if (currentRole === 'TENANT' && currentTenantId) {
      await Promise.all([loadTenantPreservations(currentTenantId), loadUsers()]);
    } else {
      goto('/login', { replaceState: true });
    }
  });

  async function loadAllPreservations() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_PRESERVATIONS,
        fetchPolicy: 'network-only'
      });
      aips = result?.data?.getAllPreservations || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load Preservations';
      toasts.error(`Failed to load Preservations: ${error}`);
    } finally {
      loading = false;
    }
  }

  async function loadTenantPreservations(tenantId: number) {
    try {
      loading = true;
      const result = await client.query({
        query: GET_PRESERVATIONS_BY_TENANT,
        variables: { tenantId: tenantId.toString() },
        fetchPolicy: 'network-only'
      });
      aips = result?.data?.getPreservationsByTenant || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load Preservations';
      toasts.error(`Failed to load Preservations: ${error}`);
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

  function openExtractDialog(aip: any) {
    selectedPreservationForExtract = aip;
    extractPassword = '';
    extractError = null;
    showExtractDialog = true;
  }

  function closeExtractDialog() {
    showExtractDialog = false;
    selectedPreservationForExtract = null;
    extractPassword = '';
    extractError = null;
    extracting = false;
  }

  async function handleExtract() {
    if (!extractPassword || !selectedPreservationForExtract) return;
    extracting = true;
    extractError = null;
    try {
      const response = await fetch(`${API_BASE}/api/archives/${selectedPreservationForExtract.id}/extract`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: extractPassword })
      });
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || `Server error: ${response.status}`);
      }
      const contentDisposition = response.headers.get('Content-Disposition');
      const filenameMatch = contentDisposition?.match(/filename="?(.+?)"?$/);
      const filename = filenameMatch?.[1] || `aip_${selectedPreservationForExtract.id}_export.json`;
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      closeExtractDialog();
      toasts.success('Preservation extracted successfully');
    } catch (e) {
      extractError = e instanceof Error ? e.message : 'Failed to extract Preservation';
    } finally {
      extracting = false;
    }
  }
</script>

<svelte:head>
  <title>Preservations - Archiving System</title>
</svelte:head>

<div class="aips-page">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Preservations' }]} />
  <div class="page-header">
    <div class="page-heading">
      <span class="eyebrow">Admin console</span>
      <h1>🏗️ Preservation packages</h1>
    </div>
    <a href="/preservation/create" class="btn-create btn-primary">+ Create Preservation</a>
  </div>

  {#if error}
    <div class="error">
      {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading Preservations...</p>
    </div>
  {:else if aips.length === 0}
    <div class="empty-state">
      <span class="empty-icon">🏗️</span>
      <h3>No Preservations found</h3>
      <p>Create your first Preservation package to get started.</p>
      <a href="/preservation/create" class="btn-primary-link btn-primary">Create Preservation</a>
    </div>
  {:else}
    <div class="aips-count">
      <span class="count-label">Total Preservations:</span>
      <span class="count-value">{aips.length}</span>
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
          {#each aips as aip (aip.id)}
            <tr>
              <td class="id-cell">{aip.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="aip-title">{aip.title}</div>
                  {#if aip.description}
                    <div class="aip-description">{aip.description}</div>
                  {/if}
                </div>
              </td>
              <td class="standard-cell">
                <span class="badge indigo">{aip.standard}</span>
              </td>
              <td class="entity-cell">
                {#if aip.rootElement}
                  <div class="entity-info">
                    <span class="entity-name">{aip.rootElement.entityName}</span>
                    <span class="entity-type">({aip.rootElement.entityType})</span>
                  </div>
                  {#if aip.rootElement.fields && aip.rootElement.fields.length > 0}
                    <div class="field-count">{aip.rootElement.fields.length} fields</div>
                  {/if}
                {:else}
                  <span class="no-entity">-</span>
                {/if}
              </td>
              <td class="status-cell">
                <span class="badge {getStatusClass(aip.status)}">{aip.status}</span>
              </td>
              <td class="owner-cell">{getUserName(aip.ownerId)}</td>
              <td class="date-cell">{new Date(aip.createdAt).toLocaleDateString()}</td>
              <td class="actions-cell">
                <a href="/preservation/edit/{aip.id}" class="btn-action btn-edit btn-chip indigo">
                  ✏️ Edit
                </a>
                <button class="btn-action btn-extract btn-chip violet" on:click={() => openExtractDialog(aip)}>
                  📥 Extract
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

<!-- Extract Dialog -->
{#if showExtractDialog}
  <div class="modal-overlay" on:click={closeExtractDialog} role="dialog" aria-modal="true">
    <div class="modal-content modal" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>📥 Extract Preservation</h3>
        <button class="modal-close" on:click={closeExtractDialog} aria-label="Close">×</button>
      </div>
      <div class="modal-body">
        {#if selectedPreservationForExtract}
          <p class="aip-info">Preservation: <strong>{selectedPreservationForExtract.title}</strong></p>
          <p class="info-text">Enter your password to extract and download the Preservation contents.</p>
          {#if extractError}
            <div class="alert alert-error error">{extractError}</div>
          {/if}
          <div class="form-group">
            <label for="extractPassword">Password *</label>
            <input type="password" id="extractPassword" bind:value={extractPassword} placeholder="Enter password" disabled={extracting} on:keydown={(e) => e.key === 'Enter' && handleExtract()} />
          </div>
        {/if}
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" on:click={closeExtractDialog} disabled={extracting}>Cancel</button>
        <button class="btn-primary" on:click={handleExtract} disabled={extracting || !extractPassword}>
          {extracting ? 'Extracting...' : '📥 Extract'}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .aips-page {
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

  .aips-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: var(--arc-card, #fff);
    border-radius: 0.75rem;
    border: 1px solid var(--arc-line, #e8edf3);
    border-left: 3px solid var(--arc-indigo, #6366f1);
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

  .data-table { min-width: 1000px; }

  .id-cell { color: var(--arc-muted, #64748b); font-family: monospace; font-size: 0.875rem; width: 60px; }

  .title-cell { min-width: 250px; }
  .title-wrapper { display: flex; flex-direction: column; gap: 0.25rem; }
  .aip-title { font-weight: 600; color: var(--arc-ink, #1e293b); }
  .aip-description { font-size: 0.875rem; color: var(--arc-muted, #64748b); }

  /* .badge base + hues are global; these Preservation statuses are page-specific. */
  .badge.status-draft { background: var(--arc-chip-amber-bg, #fef3c7); color: var(--arc-chip-amber-ink, #92400e); }
  .badge.status-building { background: #dbeafe; color: #1e40af; }
  .badge.status-validated { background: var(--arc-chip-green-bg, #dcfce7); color: var(--arc-chip-green-ink, #166534); }
  .badge.status-stored { background: var(--arc-chip-violet-bg, #ede9fe); color: var(--arc-chip-violet-ink, #5b21b6); }
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

  /* Row actions ride on .btn-chip; only the row spacing is local. */
  .btn-action { margin: 0 0.25rem; }

  /* Modal — .modal-overlay/.modal come from app.css; this dialog is
     narrower and pads its own header/body/footer sections. */
  .modal {
    padding: 0;
    max-width: 500px;
    width: 90%;
  }

  .modal-header {
    display: flex; justify-content: space-between; align-items: center;
    padding: 1.5rem; border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .modal-header h3 { margin: 0; color: var(--arc-ink, #0f172a); }

  .modal-close {
    background: none; border: none; box-shadow: none; font-size: 1.5rem;
    color: var(--arc-muted, #64748b); cursor: pointer;
  }

  .modal-body { padding: 1.5rem; }
  .aip-info { margin: 0 0 1rem 0; color: var(--arc-ink, #1e293b); }
  .info-text { margin: 0 0 1.5rem 0; color: var(--arc-muted, #64748b); font-size: 0.875rem; }

  /* The extract failure panel and the form group/label use the global kit. */

  .modal-footer {
    display: flex; justify-content: flex-end; gap: 0.75rem;
    padding: 1.5rem; border-top: 1px solid var(--arc-line, #e8edf3);
  }

  /* .btn-primary / .btn-secondary come from the global kit. */

  @media (max-width: 768px) {
    .aips-page { padding: 1rem; }
    .page-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  }

  /* No blue chip token exists — dark-theme override keeps the blue hue readable */
  :global(html[data-theme='dark']) .badge.status-building {
    background: rgba(59, 130, 246, 0.18);
    color: #93c5fd;
  }
</style>
