<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_ARCHIVES, GET_ARCHIVES_BY_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let archives: any[] = [];
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;

  // Extract dialog state
  let showExtractDialog = false;
  let selectedArchiveForExtract: any = null;
  let extractPassword = '';
  let extracting = false;
  let extractError: string | null = null;

  let currentRole = '';
  let currentTenantId: number | null = null;
  let showTenantColumn = false;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    currentTenantId = authState.tenantId;

    if (currentRole === 'ADMIN') {
      showTenantColumn = true;
      await Promise.all([loadAllArchives(), loadUsers()]);
    } else if (currentRole === 'TENANT' && currentTenantId) {
      showTenantColumn = false;
      await Promise.all([loadTenantArchives(currentTenantId), loadUsers()]);
    } else {
      goto('/login', { replaceState: true });
    }
  });

  async function loadAllArchives() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_ARCHIVES,
        fetchPolicy: 'network-only'
      });
      archives = result?.data?.getAllArchives || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
    } finally {
      loading = false;
    }
  }

  async function loadTenantArchives(tenantId: number) {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ARCHIVES_BY_TENANT,
        variables: { tenantId: tenantId.toString() },
        fetchPolicy: 'network-only'
      });
      archives = result?.data?.getArchivesByTenant || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
    } finally {
      loading = false;
    }
  }

  async function loadUsers() {
    try {
      const result = await client.query({
        query: GET_ALL_USERS,
        fetchPolicy: 'network-only'
      });
      users = result.data.getAllUsers || [];
    } catch (e) {
      console.error('Failed to load users:', e instanceof Error ? e.message : 'Unknown error');
    }
  }

  function getStatusBadgeClass(status: string) {
    return status.toLowerCase();
  }

  function getUserName(userId: string) {
    const user = users.find((u: any) => u.id === userId);
    return user ? user.name : 'Unknown User';
  }

  function openExtractDialog(archive: any) {
    selectedArchiveForExtract = archive;
    extractPassword = '';
    extractError = null;
    showExtractDialog = true;
  }

  function closeExtractDialog() {
    showExtractDialog = false;
    selectedArchiveForExtract = null;
    extractPassword = '';
    extractError = null;
    extracting = false;
  }

  async function handleExtract() {
    if (!extractPassword) {
      extractError = 'Password is required';
      return;
    }

    if (!selectedArchiveForExtract) {
      extractError = 'No archive selected';
      return;
    }

    extracting = true;
    extractError = null;

    try {
      const response = await fetch(`${API_BASE}/api/archives/${selectedArchiveForExtract.id}/extract`, {
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
      const filename = filenameMatch?.[1] || `archive_${selectedArchiveForExtract.id}_export.json`;

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
      toasts.success('Archive extracted successfully');
    } catch (e) {
      extractError = e instanceof Error ? e.message : 'Failed to extract archive';
    } finally {
      extracting = false;
    }
  }
</script>

<svelte:head>
  <title>Archives - Archiving System</title>
</svelte:head>

<div class="archives-page">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Archives' }]} />
  <div class="page-header">
    <div class="page-heading">
      <span class="eyebrow">Admin console</span>
      <h1>Archives Management</h1>
    </div>
    <a href="/archives/create" class="add-archive-btn">+ Add Archive</a>
  </div>

  {#if error}
    <div class="error">
      Error: {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading archives...</p>
    </div>
  {:else if archives.length === 0}
    <div class="empty-state">
      <span class="empty-icon">📁</span>
      <h3>No archives found</h3>
      <p>Create your first archive to get started!</p>
      <a href="/archives/create" class="btn-create">Create First Archive</a>
    </div>
  {:else}
    <div class="archives-count">
      <span class="count-label">Total Archives:</span>
      <span class="count-value">{archives.length}</span>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
            {#if showTenantColumn}
              <th>Tenant</th>
            {/if}
            <th>Status</th>
            <th>Standard</th>
            <th>Owner</th>
            <th>Created</th>
            <th>Updated</th>
            <th>Assigned Users</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {#each archives as archive (archive.id)}
            <tr>
              <td class="id-cell">{archive.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="archive-title">{archive.title}</div>
                  {#if archive.description}
                    <div class="archive-description">{archive.description}</div>
                  {/if}
                  {#if archive.content}
                    <div class="content-preview">
                      {archive.content.substring(0, 100)}{archive.content.length > 100 ? '...' : ''}
                    </div>
                  {/if}
                </div>
              </td>
              {#if showTenantColumn}
                <td class="tenant-cell">
                  {#if archive.tenant}
                    <div class="tenant-info">
                      <div class="tenant-name">{archive.tenant.displayName || archive.tenant.name}</div>
                      <div class="tenant-domain">{archive.tenant.domain}</div>
                    </div>
                  {:else}
                    <span class="no-tenant">-</span>
                  {/if}
                </td>
              {/if}
              <td class="status-cell">
                <span class="badge {getStatusBadgeClass(archive.status)}">{archive.status}</span>
              </td>
              <td class="standard-cell">
                <span class="badge standard-badge">{archive.standard}</span>
              </td>
              <td class="owner-cell">{getUserName(archive.ownerId)}</td>
              <td class="date-cell">{new Date(archive.createdAt).toLocaleDateString()}</td>
              <td class="date-cell">{new Date(archive.updatedAt).toLocaleDateString()}</td>
              <td class="assigned-cell">
                {#if archive.assignedUsers && archive.assignedUsers.length > 0}
                  <div class="assigned-users-compact">
                    {#each archive.assignedUsers.slice(0, 2) as user}
                      <span class="user-badge">{user.name}</span>
                    {/each}
                    {#if archive.assignedUsers.length > 2}
                      <span class="more-badge">+{archive.assignedUsers.length - 2}</span>
                    {/if}
                  </div>
                {:else}
                  <span class="no-users">None</span>
                {/if}
              </td>
              <td class="actions-cell">
                <a
                  href="/admin/archive/{archive.id}/intake/create"
                  class="btn-action btn-sip"
                  title="Create Intake from this archive"
                >
                  📦 Create Intake
                </a>
                <a
                  href="/admin/archive/{archive.id}/preservation/create"
                  class="btn-action btn-aip"
                  title="Create Preservation from this archive"
                >
                  🏗️ Create Preservation
                </a>
                <a
                  href="/admin/archive/{archive.id}/release/create"
                  class="btn-action btn-dip"
                  title="Create Release from this archive"
                >
                  📤 Create Release
                </a>
                <a href="/archives/delete/{archive.id}" class="btn-action btn-delete">
                  🗑️ Delete
                </a>
                <a href="/archives/update/{archive.id}" class="btn-action btn-edit">
                  ✏️ Edit
                </a>
                <button
                  class="btn-action btn-extract"
                  on:click={() => openExtractDialog(archive)}
                  title="Extract archive"
                >
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

<!-- Extract Password Dialog -->
{#if showExtractDialog}
  <div class="modal-overlay" on:click={closeExtractDialog} role="dialog" aria-modal="true">
    <div class="modal-content" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>🔐 Extract Archive</h3>
        <button class="modal-close" on:click={closeExtractDialog} aria-label="Close">×</button>
      </div>

      <div class="modal-body">
        {#if selectedArchiveForExtract}
          <p class="archive-info">
            Archive: <strong>{selectedArchiveForExtract.title}</strong>
          </p>
          <p class="info-text">
            Enter your password to securely extract and download the archive contents.
          </p>

          {#if extractError}
            <div class="alert alert-error">
              <span class="alert-icon">⚠️</span>
              <span>{extractError}</span>
            </div>
          {/if}

          <div class="form-group">
            <label for="extractPassword">Password *</label>
            <input
              type="password"
              id="extractPassword"
              bind:value={extractPassword}
              placeholder="Enter your password"
              disabled={extracting}
              on:keypress={(e) => e.key === 'Enter' && handleExtract()}
            />
            <small class="helper-text">Your password is required to decrypt and download the archive</small>
          </div>
        {/if}
      </div>

      <div class="modal-footer">
        <button
          type="button"
          class="btn btn-secondary"
          on:click={closeExtractDialog}
          disabled={extracting}
        >
          Cancel
        </button>
        <button
          type="button"
          class="btn btn-primary"
          on:click={handleExtract}
          disabled={extracting || !extractPassword}
        >
          {extracting ? '⏳ Extracting...' : '📥 Extract & Download'}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .archives-page {
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

  .add-archive-btn {
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    transition: all 0.2s ease;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
  }

  .add-archive-btn:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
    box-shadow: var(--arc-shadow-btn-hover, 0 16px 40px -8px rgba(124, 58, 237, 0.75));
  }

  .error {
    background: var(--arc-alert-red-bg, #fef2f2);
    color: var(--arc-alert-red-ink, #991b1b);
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
    border: 1px solid var(--arc-alert-red-border, #fecaca);
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
    border: 4px solid var(--arc-line-strong, #e2e8f0);
    border-top: 4px solid var(--arc-indigo, #6366f1);
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .empty-icon {
    font-size: 5rem;
    display: block;
    margin-bottom: 1rem;
  }

  .empty-state h3 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
  }

  .empty-state p {
    margin: 0 0 1.5rem 0;
    color: var(--arc-muted, #64748b);
  }

  .btn-create {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    text-decoration: none;
    border-radius: 0.65rem;
    font-weight: 700;
    transition: all 0.2s ease;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
  }

  .btn-create:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .archives-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: var(--arc-card, #fff);
    border-radius: 0.75rem;
    border: 1px solid var(--arc-line, #e8edf3);
    border-left: 3px solid var(--arc-cyan-deep, #06b6d4);
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
    min-width: 1400px;
  }

  .data-table thead {
    background: var(--arc-card-2, #f8fafc);
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 700;
    color: var(--arc-muted, #64748b);
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    white-space: nowrap;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .data-table tbody tr {
    border-bottom: 1px solid var(--arc-line, #e8edf3);
    transition: background-color 0.15s;
  }

  .data-table tbody tr:hover {
    background: var(--arc-card-2, #f8fafc);
  }

  .data-table tbody tr:last-child {
    border-bottom: none;
  }

  .data-table td {
    padding: 1rem;
    color: var(--arc-ink, #0f172a);
  }

  .id-cell {
    font-family: 'Monaco', 'Courier New', monospace;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    width: 60px;
  }

  .title-cell {
    min-width: 300px;
    max-width: 400px;
  }

  .title-wrapper {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .archive-title {
    font-weight: 600;
    color: var(--arc-ink, #1e293b);
  }

  .archive-description {
    font-size: 0.875rem;
    color: var(--arc-muted, #64748b);
    line-height: 1.4;
  }

  .content-preview {
    font-size: 0.75rem;
    color: var(--arc-faint, #94a3b8);
    font-family: 'Monaco', 'Courier New', monospace;
    line-height: 1.4;
    margin-top: 0.25rem;
  }

  .tenant-cell {
    min-width: 180px;
    max-width: 250px;
  }

  .tenant-info {
    display: flex;
    flex-direction: column;
    gap: 0.125rem;
  }

  .tenant-name {
    font-weight: 600;
    color: var(--arc-ink, #1e293b);
    font-size: 0.875rem;
  }

  .tenant-domain {
    font-size: 0.75rem;
    color: var(--arc-muted, #64748b);
    font-family: 'Monaco', 'Courier New', monospace;
  }

  .no-tenant {
    color: var(--arc-faint, #94a3b8);
    font-style: italic;
  }

  .status-cell,
  .standard-cell {
    white-space: nowrap;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .badge.active,
  .badge.published {
    background: var(--arc-chip-green-bg, #dcfce7);
    color: var(--arc-chip-green-ink, #166534);
  }

  .badge.draft {
    background: var(--arc-chip-amber-bg, #fef3c7);
    color: var(--arc-chip-amber-ink, #92400e);
  }

  .badge.archived,
  .badge.deleted {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #991b1b);
  }

  .standard-badge {
    background: var(--arc-chip-cyan-bg, #cffafe);
    color: var(--arc-chip-cyan-ink, #155e75);
  }

  .owner-cell {
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    white-space: nowrap;
  }

  .date-cell {
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    white-space: nowrap;
    width: 120px;
  }

  .assigned-cell {
    min-width: 150px;
  }

  .assigned-users-compact {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
  }

  .user-badge {
    display: inline-block;
    padding: 0.125rem 0.5rem;
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .more-badge {
    display: inline-block;
    padding: 0.125rem 0.5rem;
    background: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #475569);
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .no-users {
    color: var(--arc-faint, #94a3b8);
    font-style: italic;
    font-size: 0.875rem;
  }

  .actions-cell {
    text-align: right;
    white-space: nowrap;
    width: 440px;
  }

  .btn-action {
    display: inline-block;
    padding: 0.5rem 1rem;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 600;
    transition: all 0.2s ease;
    margin-left: 0.5rem;
    border: none;
    cursor: pointer;
    text-decoration: none;
    box-shadow: none;
  }

  .btn-edit {
    background: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #475569);
  }

  .btn-edit:hover {
    background: var(--arc-chip-slate-hover, #e2e8f0);
  }

  .btn-sip {
    background: var(--arc-chip-pink-bg, #fce7f3);
    color: var(--arc-chip-pink-ink, #9d174d);
  }

  .btn-sip:hover {
    background: var(--arc-chip-pink-hover, #fbcfe8);
  }

  .btn-aip {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
  }

  .btn-aip:hover {
    background: var(--arc-chip-indigo-hover, #c7d2fe);
  }

  .btn-dip {
    background: var(--arc-chip-orange-bg, #ffedd5);
    color: var(--arc-chip-orange-ink, #9a3412);
  }

  .btn-dip:hover {
    background: var(--arc-chip-orange-hover, #fed7aa);
  }

  .btn-extract {
    background: var(--arc-chip-violet-bg, #ede9fe);
    color: var(--arc-chip-violet-ink, #6d28d9);
  }

  .btn-extract:hover {
    background: var(--arc-chip-violet-hover, #ddd6fe);
  }

  .btn-delete {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #991b1b);
  }

  .btn-delete:hover {
    background: var(--arc-chip-red-hover, #fecaca);
  }

  /* Modal Styles */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--arc-overlay, rgba(15, 23, 42, 0.55));
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    backdrop-filter: blur(2px);
  }

  .modal-content {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
    max-width: 500px;
    width: 90%;
    max-height: 90vh;
    overflow-y: auto;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .modal-header h3 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.25rem;
    font-weight: 600;
  }

  .modal-close {
    background: none;
    border: none;
    box-shadow: none;
    font-size: 1.75rem;
    cursor: pointer;
    color: var(--arc-muted, #64748b);
    padding: 0;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.25rem;
    transition: all 0.2s;
  }

  .modal-close:hover {
    background: var(--arc-card-2, #f1f5f9);
    color: var(--arc-ink, #1e293b);
  }

  .modal-body {
    padding: 1.5rem;
  }

  .archive-info {
    margin: 0 0 1rem 0;
    color: var(--arc-body, #475569);
    font-size: 0.875rem;
  }

  .archive-info strong {
    color: var(--arc-ink, #1e293b);
  }

  .info-text {
    margin: 0 0 1.5rem 0;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: var(--arc-body, #475569);
    font-weight: 500;
    font-size: 0.875rem;
  }

  .form-group input[type="password"]:disabled {
    background: var(--arc-card-2, #f1f5f9);
    cursor: not-allowed;
  }

  .helper-text {
    display: block;
    margin-top: 0.375rem;
    color: var(--arc-muted, #64748b);
    font-size: 0.75rem;
  }

  .alert {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 0.75rem 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
  }

  .alert-error {
    background: var(--arc-alert-red-bg, #fef2f2);
    border: 1px solid var(--arc-alert-red-border, #fecaca);
    color: var(--arc-alert-red-ink, #991b1b);
  }

  .alert-icon {
    font-size: 1.125rem;
  }

  .modal-footer {
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
    padding: 1.5rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.65rem;
    font-weight: 700;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }

  .btn-primary {
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
  }

  .btn-primary:hover:not(:disabled) {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .btn-secondary {
    background: var(--arc-card, #fff);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    color: var(--arc-ink, #1e293b);
    box-shadow: none;
  }

  .btn-secondary:hover:not(:disabled) {
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-link, #4f46e5);
    background: var(--arc-card, #fff);
  }

  @media (max-width: 768px) {
    .archives-page {
      padding: 1rem;
    }

    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }

    .table-container {
      overflow-x: auto;
    }
  }
</style>
