
<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_INTAKES_BY_TENANT_V2, GET_ALL_USERS, GET_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
  }

  export let data: PageData;

  let sips: any[] = [];
  let users: any[] = [];
  let tenant: any = null;
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

  // Extract dialog state
  let showExtractDialog = false;
  let selectedIntakeForExtract: any = null;
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
    } else if (currentRole === 'USER') {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to view this page');
      goto('/');
      return;
    } else {
      hasAccess = false;
      loading = false;
      goto('/login');
      return;
    }

    await Promise.all([loadTenant(), loadIntakes(), loadUsers()]);
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

  async function loadIntakes() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_INTAKES_BY_TENANT_V2,
        variables: { tenantId: data.tenantId },
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
  <title>{tenant ? `${tenant.displayName || tenant.name} - ` : ''}Intakes - Archiving System</title>
</svelte:head>

<div class="tenant-sips-page">
  {#if !hasAccess && !loading}
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to view these Intakes.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[{ label: 'Intakes' }]}
    />

    <div class="page-header">
      <div class="header-content">
        <span class="eyebrow">Intake</span>
        <h1>📦 Intake packages</h1>
        {#if tenant}
          <div class="tenant-badge">
            <span class="tenant-icon">🏢</span>
            <span class="tenant-name">{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
      </div>
      <a href="/tenants/{data.tenantId}/intakes/create" class="btn-create">+ Create Intake</a>
    </div>

    {#if error}
      <div class="error">
        ❌ {error}
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
        <p>This tenant doesn't have any Intake packages yet.</p>
        <a href="/tenants/{data.tenantId}/intakes/create" class="btn-primary-link">Create Intake</a>
      </div>
    {:else}
      <div class="sips-count">
        <span class="count-label">Total Intakes:</span>
        <span class="count-value">{sips.length}</span>
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
                  <span class="badge standard-badge">{sip.standard}</span>
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
                  {#if sip.archiveId}
                    <a href="/tenants/{data.tenantId}/archives/{sip.archiveId}/intakes" class="btn-action btn-edit">
                      ✏️ Edit
                    </a>
                  {:else}
                    <a href="/intake/edit/{sip.id}" class="btn-action btn-edit">
                      ✏️ Edit
                    </a>
                  {/if}
                  <button class="btn-action btn-extract" on:click={() => openExtractDialog(sip)}>
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
    <div class="modal-content" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>📥 Extract Intake</h3>
        <button class="modal-close" on:click={closeExtractDialog} aria-label="Close">×</button>
      </div>
      <div class="modal-body">
        {#if selectedIntakeForExtract}
          <p class="sip-info">Intake: <strong>{selectedIntakeForExtract.title}</strong></p>
          <p class="info-text">Enter your password to extract and download the Intake contents.</p>
          {#if extractError}
            <div class="alert alert-error">
              <span class="alert-icon">⚠️</span>
              <span>{extractError}</span>
            </div>
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
  .tenant-sips-page {
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
    background: var(--arc-chip-pink-bg);
    color: var(--arc-chip-pink-ink);
    border-radius: 9999px;
    font-weight: 600;
    font-size: 0.9rem;
  }

  .tenant-icon { font-size: 1.25rem; }

  .btn-create {
    padding: 0.75rem 1.5rem;
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6);
    transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
    white-space: nowrap;
  }

  .btn-create:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .error {
    background: var(--arc-alert-red-bg);
    color: var(--arc-alert-red-ink);
    padding: 1rem;
    border-radius: 0.6rem;
    border: 1px solid var(--arc-alert-red-border);
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
    border: 4px solid var(--arc-line-strong);
    border-top: 4px solid #6366f1;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }

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

  .btn-primary-link {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    text-decoration: none;
    border-radius: 0.65rem;
    font-weight: 700;
    box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6);
    transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
  }

  .btn-primary-link:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .sips-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: var(--arc-card);
    border-radius: 0.75rem;
    border: 1px solid var(--arc-line);
    border-left: 3px solid #ec4899;
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

  .table-container {
    background: var(--arc-card);
    border-radius: 1rem;
    overflow-x: auto;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line);
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 1000px;
  }

  .data-table thead {
    background: var(--arc-card-2);
    border-bottom: 1px solid var(--arc-line);
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 700;
    color: var(--arc-muted);
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }

  .data-table td {
    padding: 1rem;
    border-bottom: 1px solid var(--arc-line);
  }

  .data-table tbody tr:last-child td { border-bottom: none; }
  .data-table tbody tr:hover { background: var(--arc-card-2); }

  .id-cell { color: var(--arc-muted); font-family: monospace; font-size: 0.875rem; width: 60px; }

  .title-cell { min-width: 250px; }
  .title-wrapper { display: flex; flex-direction: column; gap: 0.25rem; }
  .sip-title { font-weight: 600; color: var(--arc-ink); }
  .sip-description { font-size: 0.875rem; color: var(--arc-muted); }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
  }

  .standard-badge { background: var(--arc-chip-pink-bg); color: var(--arc-chip-pink-ink); }

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

  .btn-action {
    display: inline-block;
    padding: 0.375rem 0.75rem;
    margin: 0 0.25rem;
    border: none;
    border-radius: 0.45rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-decoration: none;
    cursor: pointer;
    box-shadow: none;
    transition: all 0.2s;
  }

  .btn-edit { background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); }
  .btn-edit:hover { background: var(--arc-chip-indigo-hover); transform: none; }
  .btn-extract { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); }
  .btn-extract:hover { background: var(--arc-chip-green-hover); transform: none; }

  /* Modal */
  .modal-overlay {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    background: var(--arc-overlay);
    display: flex; align-items: center; justify-content: center;
    z-index: 1000;
  }

  .modal-content {
    background: var(--arc-card); border: 1px solid var(--arc-line); border-radius: 1rem;
    max-width: 500px; width: 90%;
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
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
  .sip-info { margin: 0 0 1rem 0; color: var(--arc-ink); }
  .info-text { margin: 0 0 1.5rem 0; color: var(--arc-muted); font-size: 0.875rem; }

  .alert { padding: 0.75rem 1rem; border-radius: 0.6rem; margin-bottom: 1rem; display: flex; align-items: center; gap: 0.5rem; }
  .alert-error { background: var(--arc-alert-red-bg); color: var(--arc-alert-red-ink); border: 1px solid var(--arc-alert-red-border); }

  .form-group { margin-bottom: 1rem; }
  .form-group label { display: block; margin-bottom: 0.5rem; color: var(--arc-body); font-weight: 600; }
  /* Inputs are styled by the global app.css rules */

  .modal-footer {
    display: flex; justify-content: flex-end; gap: 0.75rem;
    padding: 1.5rem; border-top: 1px solid var(--arc-line);
  }

  .btn-secondary, .btn-primary {
    padding: 0.75rem 1.5rem; border-radius: 0.65rem;
    font-weight: 700; cursor: pointer;
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
  }

  .btn-secondary { background: var(--arc-card); color: var(--arc-ink); border: 1.5px solid var(--arc-line-strong); box-shadow: none; }
  .btn-secondary:hover:not(:disabled) { background: var(--arc-card); border-color: var(--arc-indigo); color: var(--arc-link); transform: none; box-shadow: none; }

  .btn-primary { background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6)); color: white; border: none; box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6); }
  .btn-primary:hover:not(:disabled) { background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed)); transform: translateY(-2px); }
  .btn-primary:disabled, .btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; transform: none; box-shadow: none; }

  @media (max-width: 768px) {
    .tenant-sips-page { padding: 1rem; }
    .page-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  }
</style>