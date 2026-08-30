<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_INTAKES_V2, GET_INTAKES_BY_TENANT_V2, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let sips: any[] = [];
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let currentTenantId: number | null = null;

  // Extract dialog state
  let showExtractDialog = false;
  let selectedIntakeForExtract: any = null;
  let extractPassword = '';
  let extracting = false;
  let extractError: string | null = null;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    currentTenantId = authState.tenantId;

    if (currentRole === 'ADMIN') {
      await Promise.all([loadAllIntakes(), loadUsers()]);
    } else if (currentRole === 'TENANT' && currentTenantId) {
      await Promise.all([loadTenantIntakes(currentTenantId), loadUsers()]);
    } else {
      goto('/login', { replaceState: true });
    }
  });

  async function loadAllIntakes() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_INTAKES_V2,
        fetchPolicy: 'network-only'
      });
      sips = result?.data?.getAllIntakesV2 || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load Intakes';
      toasts.error(`Failed to load Intakes: ${error}`);
    } finally {
      loading = false;
    }
  }

  async function loadTenantIntakes(tenantId: number) {
    try {
      loading = true;
      const result = await client.query({
        query: GET_INTAKES_BY_TENANT_V2,
        variables: { tenantId: tenantId.toString() },
        fetchPolicy: 'network-only'
      });
      sips = result?.data?.getIntakesByTenantV2 || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load Intakes';
      toasts.error(`Failed to load Intakes: ${error}`);
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
    return status.toLowerCase();
  }

  function openExtractDialog(sip: any) {
    selectedIntakeForExtract = sip;
    extractPassword = '';
    extractError = null;
    showExtractDialog = true;
  }

  function closeExtractDialog() {
    showExtractDialog = false;
    selectedIntakeForExtract = null;
    extractPassword = '';
    extractError = null;
    extracting = false;
  }

  async function handleExtract() {
    if (!extractPassword || !selectedIntakeForExtract) return;
    extracting = true;
    extractError = null;
    try {
      const response = await fetch(`${API_BASE}/api/archives/${selectedIntakeForExtract.id}/extract`, {
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
      const filename = filenameMatch?.[1] || `sip_${selectedIntakeForExtract.id}_export.json`;
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
      toasts.success('Intake extracted successfully');
    } catch (e) {
      extractError = e instanceof Error ? e.message : 'Failed to extract Intake';
    } finally {
      extracting = false;
    }
  }
</script>

<svelte:head>
  <title>Intakes - Archiving System</title>
</svelte:head>

<div class="sips-page">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Intakes' }]} />
  <div class="page-header">
    <div class="page-heading">
      <span class="eyebrow">Admin console</span>
      <h1>📦 Intake packages</h1>
    </div>
    <a href="/admin/intake/create" class="btn-create btn-primary">+ Create Intake</a>
  </div>

  {#if error}
    <div class="error">
      {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading Intakes...</p>
    </div>
  {:else if sips.length === 0}
    <div class="empty-state">
      <span class="empty-icon">📦</span>
      <h3>No Intakes found</h3>
      <p>Create your first Intake package to get started.</p>
      <a href="/admin/intake/create" class="btn-primary-link btn-primary">Create Intake</a>
    </div>
  {:else}
    <div class="sips-count">
      <span class="count-label">Total Intakes:</span>
      <span class="count-value">{sips.length}</span>
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
          {#each sips as sip (sip.id)}
            <tr>
              <td class="id-cell">{sip.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="sip-title">{sip.title}</div>
                  {#if sip.description}
                    <div class="sip-description">{sip.description}</div>
                  {/if}
                </div>
              </td>
              <td class="standard-cell">
                <span class="badge pink standard-badge">{sip.standard}</span>
              </td>
              <td class="entity-cell">
                {#if sip.rootElement}
                  <div class="entity-info">
                    <span class="entity-name">{sip.rootElement.entityName}</span>
                    <span class="entity-type">({sip.rootElement.entityType})</span>
                  </div>
                  {#if sip.rootElement.fields && sip.rootElement.fields.length > 0}
                    <div class="field-count">{sip.rootElement.fields.length} fields</div>
                  {/if}
                {:else}
                  <span class="no-entity">-</span>
                {/if}
              </td>
              <td class="status-cell">
                <span class="badge {getStatusClass(sip.status)}">{sip.status}</span>
              </td>
              <td class="owner-cell">{getUserName(sip.ownerId)}</td>
              <td class="date-cell">{new Date(sip.createdAt).toLocaleDateString()}</td>
              <td class="actions-cell">
                <a href="/intake/edit/{sip.id}" class="btn-action btn-edit btn-chip indigo">
                  ✏️ Edit
                </a>
                <button class="btn-action btn-extract btn-chip violet" on:click={() => openExtractDialog(sip)}>
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
        <h3>📥 Extract Intake</h3>
        <button class="modal-close" on:click={closeExtractDialog} aria-label="Close">×</button>
      </div>
      <div class="modal-body">
        {#if selectedIntakeForExtract}
          <p class="sip-info">Intake: <strong>{selectedIntakeForExtract.title}</strong></p>
          <p class="info-text">Enter your password to extract and download the Intake contents.</p>
          {#if extractError}
            <div class="alert alert-error">{extractError}</div>
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
  .sips-page {
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

  /* .btn-create, .btn-primary-link, .error, .loading and .spinner use the
     global kit (app.css); only the column stack under the spinner is local. */
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

  .sips-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: var(--arc-card, #fff);
    border-radius: 0.75rem;
    border: 1px solid var(--arc-line, #e8edf3);
    border-left: 3px solid #ec4899;
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

  /* Table chrome comes from .table-card / table.arc-table; only the
     page-specific scroll + column sizing stays local. */
  .table-container {
    overflow-x: auto;
  }

  .data-table {
    min-width: 1000px;
  }

  .id-cell { color: var(--arc-muted, #64748b); font-family: monospace; font-size: 0.875rem; width: 60px; }

  .title-cell { min-width: 250px; }
  .title-wrapper { display: flex; flex-direction: column; gap: 0.25rem; }
  .sip-title { font-weight: 600; color: var(--arc-ink, #1e293b); }
  .sip-description { font-size: 0.875rem; color: var(--arc-muted, #64748b); }

  /* .badge base and the pink standard chip are global; these Intake status
     hues are page-specific. */
  .badge.draft { background: var(--arc-chip-amber-bg, #fef3c7); color: var(--arc-chip-amber-ink, #92400e); }
  .badge.published { background: var(--arc-chip-green-bg, #dcfce7); color: var(--arc-chip-green-ink, #166534); }
  .badge.archived { background: var(--arc-chip-slate-bg, #f1f5f9); color: var(--arc-chip-slate-ink, #475569); }

  .entity-cell { min-width: 150px; }
  .entity-info { display: flex; gap: 0.25rem; align-items: baseline; }
  .entity-name { font-weight: 600; color: var(--arc-ink, #1e293b); font-size: 0.875rem; }
  .entity-type { color: var(--arc-muted, #64748b); font-size: 0.75rem; }
  .field-count { color: var(--arc-faint, #94a3b8); font-size: 0.75rem; margin-top: 0.125rem; }
  .no-entity { color: var(--arc-faint, #cbd5e1); }

  .owner-cell { color: var(--arc-muted, #64748b); font-size: 0.875rem; white-space: nowrap; }
  .date-cell { color: var(--arc-muted, #64748b); font-size: 0.875rem; white-space: nowrap; }

  .actions-cell { white-space: nowrap; }

  /* Row actions are global .btn-chip <hue>; only this table's tighter
     chip size and spacing stay local. */
  .btn-action {
    display: inline-block;
    padding: 0.375rem 0.75rem;
    margin: 0 0.25rem;
    font-size: 0.75rem;
  }

  /* Modal chrome is global .modal-overlay / .modal; the narrower width and
     the sectioned (self-padding) body stay local. */
  .modal-overlay {
    z-index: 1000;
  }

  .modal-content {
    max-width: 500px; width: 90%; padding: 0;
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
  .sip-info { margin: 0 0 1rem 0; color: var(--arc-ink, #1e293b); }
  .info-text { margin: 0 0 1.5rem 0; color: var(--arc-muted, #64748b); font-size: 0.875rem; }

  .alert { padding: 0.75rem 1rem; border-radius: 0.5rem; margin-bottom: 1rem; }
  .alert-error { background: var(--arc-alert-red-bg, #fee2e2); color: var(--arc-alert-red-ink, #991b1b); border: 1px solid var(--arc-alert-red-border, #fca5a5); }

  .form-group label { display: block; margin-bottom: 0.5rem; color: var(--arc-ink, #1e293b); font-weight: 600; }

  .modal-footer {
    display: flex; justify-content: flex-end; gap: 0.75rem;
    padding: 1.5rem; border-top: 1px solid var(--arc-line, #e8edf3);
  }

  /* .btn-primary / .btn-secondary come from the global kit (app.css). */

  @media (max-width: 768px) {
    .sips-page { padding: 1rem; }
    .page-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  }
</style>
