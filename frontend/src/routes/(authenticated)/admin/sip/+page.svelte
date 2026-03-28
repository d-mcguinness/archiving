<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_SIPS_V2, GET_SIPS_BY_TENANT_V2, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';

  let sips: any[] = [];
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let currentTenantId: number | null = null;

  // Extract dialog state
  let showExtractDialog = false;
  let selectedSipForExtract: any = null;
  let extractPassword = '';
  let extracting = false;
  let extractError: string | null = null;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    currentTenantId = authState.tenantId;

    if (currentRole === 'ADMIN') {
      await Promise.all([loadAllSips(), loadUsers()]);
    } else if (currentRole === 'TENANT' && currentTenantId) {
      await Promise.all([loadTenantSips(currentTenantId), loadUsers()]);
    } else {
      goto('/login', { replaceState: true });
    }
  });

  async function loadAllSips() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_SIPS_V2,
        fetchPolicy: 'network-only'
      });
      sips = result?.data?.getAllSipsV2 || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load SIPs';
      toasts.error(`Failed to load SIPs: ${error}`);
    } finally {
      loading = false;
    }
  }

  async function loadTenantSips(tenantId: number) {
    try {
      loading = true;
      const result = await client.query({
        query: GET_SIPS_BY_TENANT_V2,
        variables: { tenantId: tenantId.toString() },
        fetchPolicy: 'network-only'
      });
      sips = result?.data?.getSipsByTenantV2 || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load SIPs';
      toasts.error(`Failed to load SIPs: ${error}`);
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
    selectedSipForExtract = sip;
    extractPassword = '';
    extractError = null;
    showExtractDialog = true;
  }

  function closeExtractDialog() {
    showExtractDialog = false;
    selectedSipForExtract = null;
    extractPassword = '';
    extractError = null;
    extracting = false;
  }

  async function handleExtract() {
    if (!extractPassword || !selectedSipForExtract) return;
    extracting = true;
    extractError = null;
    try {
      const response = await fetch(`http://localhost:2020/api/archives/${selectedSipForExtract.id}/extract`, {
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
      const filename = filenameMatch?.[1] || `sip_${selectedSipForExtract.id}_export.json`;
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
      toasts.success('SIP extracted successfully');
    } catch (e) {
      extractError = e instanceof Error ? e.message : 'Failed to extract SIP';
    } finally {
      extracting = false;
    }
  }
</script>

<svelte:head>
  <title>SIPs - Archiving System</title>
</svelte:head>

<div class="sips-page">
  <div class="page-header">
    <h1>📦 Submission Information Packages</h1>
    <a href="/sip/create" class="btn-create">+ Create SIP</a>
  </div>

  {#if error}
    <div class="error">
      {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading SIPs...</p>
    </div>
  {:else if sips.length === 0}
    <div class="empty-state">
      <span class="empty-icon">📦</span>
      <h3>No SIPs found</h3>
      <p>Create your first Submission Information Package to get started.</p>
      <a href="/sip/create" class="btn-primary-link">Create SIP</a>
    </div>
  {:else}
    <div class="sips-count">
      <span class="count-label">Total SIPs:</span>
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
                <a href="/sip/edit/{sip.id}" class="btn-action btn-edit">
                  ✏️ Edit
                </a>
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
</div>

<!-- Extract Dialog -->
{#if showExtractDialog}
  <div class="modal-overlay" on:click={closeExtractDialog} role="dialog" aria-modal="true">
    <div class="modal-content" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>📥 Extract SIP</h3>
        <button class="modal-close" on:click={closeExtractDialog} aria-label="Close">×</button>
      </div>
      <div class="modal-body">
        {#if selectedSipForExtract}
          <p class="sip-info">SIP: <strong>{selectedSipForExtract.title}</strong></p>
          <p class="info-text">Enter your password to extract and download the SIP contents.</p>
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
    color: #1e293b;
    font-size: 2rem;
  }

  .btn-create {
    padding: 0.75rem 1.5rem;
    background: #ec4899;
    color: white;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 600;
    transition: all 0.2s;
  }

  .btn-create:hover {
    background: #db2777;
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
    border-top: 4px solid #ec4899;
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
    background: #ec4899;
    color: white;
    text-decoration: none;
    border-radius: 0.5rem;
    font-weight: 600;
  }

  .btn-primary-link:hover { background: #db2777; }

  .sips-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: #fdf2f8;
    border-radius: 0.5rem;
    border: 1px solid #fbcfe8;
  }

  .count-label {
    color: #9d174d;
    font-weight: 600;
    text-transform: uppercase;
    font-size: 0.875rem;
    letter-spacing: 0.05em;
  }

  .count-value {
    color: #831843;
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
    min-width: 1000px;
  }

  .data-table thead {
    background: #fdf2f8;
    border-bottom: 2px solid #fbcfe8;
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    color: #9d174d;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .data-table td {
    padding: 1rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .data-table tbody tr:last-child td { border-bottom: none; }
  .data-table tbody tr:hover { background: #fdf2f8; }

  .id-cell { color: #64748b; font-family: monospace; font-size: 0.875rem; width: 60px; }

  .title-cell { min-width: 250px; }
  .title-wrapper { display: flex; flex-direction: column; gap: 0.25rem; }
  .sip-title { font-weight: 600; color: #1e293b; }
  .sip-description { font-size: 0.875rem; color: #64748b; }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .standard-badge { background: #fce7f3; color: #9d174d; }

  .badge.draft { background: #fef3c7; color: #92400e; }
  .badge.published { background: #dcfce7; color: #166534; }
  .badge.archived { background: #f1f5f9; color: #475569; }

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
    margin: 0 0.25rem;
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
  .btn-extract { background: #dcfce7; color: #166534; }
  .btn-extract:hover { background: #bbf7d0; }

  /* Modal */
  .modal-overlay {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex; align-items: center; justify-content: center;
    z-index: 1000;
  }

  .modal-content {
    background: white; border-radius: 0.75rem;
    max-width: 500px; width: 90%;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  }

  .modal-header {
    display: flex; justify-content: space-between; align-items: center;
    padding: 1.5rem; border-bottom: 1px solid #e2e8f0;
  }

  .modal-header h3 { margin: 0; color: #1e293b; }

  .modal-close {
    background: none; border: none; font-size: 1.5rem;
    color: #64748b; cursor: pointer;
  }

  .modal-body { padding: 1.5rem; }
  .sip-info { margin: 0 0 1rem 0; color: #1e293b; }
  .info-text { margin: 0 0 1.5rem 0; color: #64748b; font-size: 0.875rem; }

  .alert { padding: 0.75rem 1rem; border-radius: 0.5rem; margin-bottom: 1rem; }
  .alert-error { background: #fee2e2; color: #991b1b; border: 1px solid #fca5a5; }

  .form-group { margin-bottom: 1rem; }
  .form-group label { display: block; margin-bottom: 0.5rem; color: #1e293b; font-weight: 600; }
  .form-group input {
    width: 100%; padding: 0.75rem; border: 1px solid #e2e8f0;
    border-radius: 0.5rem; font-size: 1rem;
  }
  .form-group input:focus { outline: none; border-color: #ec4899; box-shadow: 0 0 0 3px rgba(236, 72, 153, 0.1); }

  .modal-footer {
    display: flex; justify-content: flex-end; gap: 0.75rem;
    padding: 1.5rem; border-top: 1px solid #e2e8f0;
  }

  .btn-secondary, .btn-primary {
    padding: 0.75rem 1.5rem; border: none; border-radius: 0.5rem;
    font-weight: 600; cursor: pointer; transition: all 0.2s;
  }

  .btn-secondary { background: #f1f5f9; color: #475569; }
  .btn-secondary:hover:not(:disabled) { background: #e2e8f0; }

  .btn-primary { background: #ec4899; color: white; }
  .btn-primary:hover:not(:disabled) { background: #db2777; }
  .btn-primary:disabled, .btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

  @media (max-width: 768px) {
    .sips-page { padding: 1rem; }
    .page-header { flex-direction: column; align-items: flex-start; gap: 1rem; }
  }
</style>
