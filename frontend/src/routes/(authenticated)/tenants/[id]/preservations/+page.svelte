<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_PRESERVATIONS_BY_TENANT, GET_ALL_USERS, GET_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
  }

  export let data: PageData;

  let aips: any[] = [];
  let users: any[] = [];
  let tenant: any = null;
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

  // Extract dialog state
  let showExtractDialog = false;
  let selectedPreservationForExtract: any = null;
  let extractPassword = '';
  let extracting = false;
  let extractError: string | null = null;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to view this page');
      goto('/');
      return;
    }

    await Promise.all([loadTenant(), loadPreservations(), loadUsers()]);
  });

  async function loadTenant() {
    try {
      const result = await client.query({
        query: GET_TENANT,
        variables: { id: data.tenantId },
        fetchPolicy: 'network-only'
      });
      tenant = result?.data?.getTenant || null;
    } catch (e) {
      console.error('Failed to load tenant:', e);
      toasts.error('Failed to load tenant information');
    }
  }

  async function loadPreservations() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_PRESERVATIONS_BY_TENANT,
        variables: { tenantId: data.tenantId },
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
    return status.toLowerCase();
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
  <title>{tenant ? `${tenant.displayName || tenant.name} - ` : ''}Preservations - Archiving System</title>
</svelte:head>

<div class="tenant-aips-page">
  {#if !hasAccess && !loading}
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to view these Preservations.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[{ label: 'Preservations' }]}
    />

    <div class="page-header">
      <div class="header-content">
        <span class="eyebrow">Preservation</span>
        <h1>🏗️ Preservation packages</h1>
        {#if tenant}
          <div class="tenant-badge">
            <span class="tenant-icon">🏢</span>
            <span class="tenant-name">{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
      </div>
      <a href="/tenants/{data.tenantId}/preservations/create" class="btn-create btn-primary">+ Create Preservation</a>
    </div>

    {#if error}
      <div class="error">
        ❌ {error}
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
        <p>This tenant doesn't have any Preservation packages yet.</p>
        <a href="/tenants/{data.tenantId}/preservations/create" class="btn-primary-link btn-primary">Create Preservation</a>
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
                  <span class="badge indigo standard-badge">{aip.standard}</span>
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
                  <button class="btn-action btn-extract btn-chip green" on:click={() => openExtractDialog(aip)}>
                    📥 Extract
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

<!-- Extract Dialog -->
{#if showExtractDialog}
  <div class="modal-overlay" on:click={closeExtractDialog} role="dialog" aria-modal="true">
    <div class="modal-content modal" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>📥 Extract Preservation</h3>
        <button class="modal-close" on:click={closeExtractDialog}>&times;</button>
      </div>
      <div class="modal-body">
        <p class="aip-info"><strong>{selectedPreservationForExtract?.title}</strong></p>
        <p class="info-text">Enter a password to encrypt the extracted Preservation package.</p>
        {#if extractError}
          <div class="alert alert-error">❌ {extractError}</div>
        {/if}
        <div class="form-group">
          <label for="extract-password">Password</label>
          <input
            type="password"
            id="extract-password"
            bind:value={extractPassword}
            placeholder="Enter extraction password"
            disabled={extracting}
          />
        </div>
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
  .tenant-aips-page {
    max-width: 1600px;
    margin: 0 auto;
    padding: 2rem;
  }

  .access-denied {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    text-align: center;
    padding: 3rem;
  }

  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0 0 1rem 0; color: var(--arc-ink); font-size: 2rem; }
  .access-denied p { margin: 0.5rem 0; color: var(--arc-muted); font-size: 1.125rem; }

  .redirect-message {
    color: var(--arc-link);
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 2rem;
    gap: 1rem;
  }

  .header-content { flex: 1; }

  .page-header h1 {
    margin: 0 0 0.75rem 0;
    color: var(--arc-ink);
    font-size: 2rem;
  }

  .tenant-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.35rem 0.9rem;
    background: var(--arc-chip-indigo-bg);
    color: var(--arc-chip-indigo-ink);
    border-radius: 9999px;
    font-weight: 600;
    font-size: 0.9rem;
  }

  .tenant-icon { font-size: 1.25rem; }

  /* .btn-create and .btn-primary-link ride on the global .btn-primary. */
  .btn-create {
    white-space: nowrap;
  }

  /* .error, .loading and .spinner come from the global kit (app.css); this
     loader only stacks a caption under a slightly larger, slower spinner. */
  .loading {
    flex-direction: column;
    min-height: 400px;
    gap: 1rem;
  }

  .spinner {
    border-width: 4px;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  /* Empty state — dark hero panel */
  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-grad-dark, radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%));
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
  }

  .empty-icon { font-size: 5rem; display: block; margin-bottom: 1rem; }
  .empty-state h3 { margin: 0 0 0.5rem 0; color: #f8fafc; }
  .empty-state p { margin: 0 0 1.5rem 0; color: #cbd5e1; }

  .aips-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: var(--arc-card);
    border-radius: 0.75rem;
    border: 1px solid var(--arc-line);
    border-left: 3px solid #6366f1;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .count-label {
    color: var(--arc-muted);
    font-weight: 700;
    text-transform: uppercase;
    font-size: 0.72rem;
    letter-spacing: 0.08em;
  }

  .count-value {
    color: var(--arc-ink);
    font-weight: 700;
    font-size: 1.25rem;
    font-family: var(--arc-font-display, 'Space Grotesk', 'Inter', sans-serif);
    letter-spacing: -0.02em;
  }

  /* Table chrome comes from .table-card / table.arc-table; only the
     page-specific scroll + column sizing stays local. */
  .table-container {
    overflow-x: auto;
  }

  .data-table {
    min-width: 1000px;
  }

  .id-cell { color: var(--arc-muted); font-family: monospace; font-size: 0.875rem; width: 60px; }

  .title-cell { min-width: 250px; }
  .title-wrapper { display: flex; flex-direction: column; gap: 0.25rem; }
  .aip-title { font-weight: 600; color: var(--arc-ink); }
  .aip-description { font-size: 0.875rem; color: var(--arc-muted); }

  /* .badge base and the indigo standard chip are global; these package status
     hues are page-specific. */
  .badge.draft { background: var(--arc-chip-amber-bg); color: var(--arc-chip-amber-ink); }
  .badge.published { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); }
  .badge.archived { background: var(--arc-chip-slate-bg); color: var(--arc-chip-slate-ink); }

  .entity-cell { min-width: 150px; }
  .entity-info { display: flex; gap: 0.25rem; align-items: baseline; }
  .entity-name { font-weight: 600; color: var(--arc-ink); font-size: 0.875rem; }
  .entity-type { color: var(--arc-muted); font-size: 0.75rem; }
  .field-count { color: var(--arc-faint); font-size: 0.75rem; margin-top: 0.125rem; }
  .no-entity { color: var(--arc-faint); }

  .owner-cell { color: var(--arc-muted); font-size: 0.875rem; white-space: nowrap; }
  .date-cell { color: var(--arc-muted); font-size: 0.875rem; white-space: nowrap; }

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
    padding: 1.5rem; border-bottom: 1px solid var(--arc-line);
  }

  .modal-header h3 { margin: 0; color: var(--arc-ink); }

  .modal-close {
    background: none; border: none; padding: 0; box-shadow: none;
    font-size: 1.5rem; color: var(--arc-muted); cursor: pointer;
  }
  .modal-close:hover { background: none; color: var(--arc-ink); transform: none; box-shadow: none; }

  .modal-body { padding: 1.5rem; }
  .aip-info { margin: 0 0 1rem 0; color: var(--arc-ink); }
  .info-text { margin: 0 0 1.5rem 0; color: var(--arc-muted); font-size: 0.875rem; }

  .alert { padding: 0.75rem 1rem; border-radius: 0.6rem; margin-bottom: 1rem; display: flex; align-items: center; gap: 0.5rem; }
  .alert-error { background: var(--arc-alert-red-bg); color: var(--arc-alert-red-ink); border: 1px solid var(--arc-alert-red-border); }

  .form-group label { display: block; margin-bottom: 0.5rem; color: var(--arc-body); font-weight: 600; }
  /* Inputs are styled by the global app.css rules */

  .modal-footer {
    display: flex; justify-content: flex-end; gap: 0.75rem;
    padding: 1.5rem; border-top: 1px solid var(--arc-line);
  }

  /* .btn-primary / .btn-secondary come from the global kit (app.css). */

  @media (max-width: 768px) {
    .tenant-aips-page { padding: 1rem; }
    .page-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  }
</style>
