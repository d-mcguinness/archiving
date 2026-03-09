<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_ARCHIVES, GET_ARCHIVES_BY_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

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
    const role = localStorage.getItem('auth_role');
    const tenantId = localStorage.getItem('auth_tenantId');
    currentRole = role || '';
    currentTenantId = tenantId ? parseInt(tenantId, 10) : null;

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
      const response = await fetch(`http://localhost:2020/api/archives/${selectedArchiveForExtract.id}/extract`, {
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
  <div class="page-header">
    <h1>Archives Management</h1>
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
                  href="/sip/create?standard={encodeURIComponent(archive.standard)}&userId={archive.ownerId}&title={encodeURIComponent(archive.title)}&description={encodeURIComponent(archive.description || '')}&archiveId={archive.id}"
                  class="btn-action btn-sip"
                  title="Create SIP from this archive"
                >
                  📦 Create SIP
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
    color: #1e293b;
    font-size: 2rem;
  }

  .add-archive-btn {
    background: #3b82f6;
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 600;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .add-archive-btn:hover {
    background: #2563eb;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
    border: 1px solid #fcc;
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
    border-top: 4px solid #3b82f6;
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
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .empty-icon {
    font-size: 5rem;
    display: block;
    margin-bottom: 1rem;
  }

  .empty-state h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
  }

  .empty-state p {
    margin: 0 0 1.5rem 0;
    color: #64748b;
  }

  .btn-create {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background: #3b82f6;
    color: white;
    text-decoration: none;
    border-radius: 0.5rem;
    font-weight: 600;
    transition: background 0.2s;
  }

  .btn-create:hover {
    background: #2563eb;
  }

  .archives-count {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    padding: 1rem 1.5rem;
    background: #f8fafc;
    border-radius: 0.5rem;
    border: 1px solid #e2e8f0;
  }

  .count-label {
    color: #64748b;
    font-weight: 600;
    text-transform: uppercase;
    font-size: 0.875rem;
    letter-spacing: 0.05em;
  }

  .count-value {
    color: #1e293b;
    font-weight: 700;
    font-size: 1.25rem;
  }

  .table-container {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow-x: auto;
    border: 1px solid #e2e8f0;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 1400px;
  }

  .data-table thead {
    background: #f8fafc;
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    color: #475569;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    white-space: nowrap;
    border-bottom: 2px solid #e2e8f0;
  }

  .data-table tbody tr {
    border-bottom: 1px solid #e2e8f0;
    transition: background-color 0.15s;
  }

  .data-table tbody tr:hover {
    background: #f8fafc;
  }

  .data-table tbody tr:last-child {
    border-bottom: none;
  }

  .data-table td {
    padding: 1rem;
    color: #1e293b;
  }

  .id-cell {
    font-family: 'Monaco', 'Courier New', monospace;
    color: #64748b;
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
    color: #1e293b;
  }

  .archive-description {
    font-size: 0.875rem;
    color: #64748b;
    line-height: 1.4;
  }

  .content-preview {
    font-size: 0.75rem;
    color: #94a3b8;
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
    color: #1e293b;
    font-size: 0.875rem;
  }

  .tenant-domain {
    font-size: 0.75rem;
    color: #64748b;
    font-family: 'Monaco', 'Courier New', monospace;
  }

  .no-tenant {
    color: #cbd5e1;
    font-style: italic;
  }

  .status-cell,
  .standard-cell {
    white-space: nowrap;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
  }

  .badge.active,
  .badge.published {
    background: #dcfce7;
    color: #166534;
  }

  .badge.draft {
    background: #fef3c7;
    color: #92400e;
  }

  .badge.archived,
  .badge.deleted {
    background: #fee2e2;
    color: #991b1b;
  }

  .standard-badge {
    background: #dbeafe;
    color: #1e40af;
  }

  .owner-cell {
    color: #64748b;
    font-size: 0.875rem;
    white-space: nowrap;
  }

  .date-cell {
    color: #64748b;
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
    background: #e0e7ff;
    color: #3730a3;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 500;
  }

  .more-badge {
    display: inline-block;
    padding: 0.125rem 0.5rem;
    background: #f1f5f9;
    color: #64748b;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .no-users {
    color: #cbd5e1;
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
    border-radius: 0.375rem;
    font-size: 0.875rem;
    font-weight: 600;
    transition: all 0.2s;
    margin-left: 0.5rem;
    border: none;
    cursor: pointer;
    text-decoration: none;
  }

  .btn-edit {
    background: #f59e0b;
    color: white;
  }

  .btn-edit:hover {
    background: #d97706;
  }

  .btn-sip {
    background: #ec4899;
    color: white;
  }

  .btn-sip:hover {
    background: #db2777;
  }

  .btn-extract {
    background: #8b5cf6;
    color: white;
  }

  .btn-extract:hover {
    background: #7c3aed;
  }

  .btn-delete {
    background: #dc2626;
    color: white;
  }

  .btn-delete:hover {
    background: #b91c1c;
  }

  /* Modal Styles */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    backdrop-filter: blur(2px);
  }

  .modal-content {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
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
    border-bottom: 1px solid #e2e8f0;
  }

  .modal-header h3 {
    margin: 0;
    color: #1e293b;
    font-size: 1.25rem;
    font-weight: 600;
  }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.75rem;
    cursor: pointer;
    color: #64748b;
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
    background: #f1f5f9;
    color: #1e293b;
  }

  .modal-body {
    padding: 1.5rem;
  }

  .archive-info {
    margin: 0 0 1rem 0;
    color: #475569;
    font-size: 0.875rem;
  }

  .archive-info strong {
    color: #1e293b;
  }

  .info-text {
    margin: 0 0 1.5rem 0;
    color: #64748b;
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #475569;
    font-weight: 500;
    font-size: 0.875rem;
  }

  .form-group input[type="password"] {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .form-group input[type="password"]:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .form-group input[type="password"]:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .helper-text {
    display: block;
    margin-top: 0.375rem;
    color: #64748b;
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
    background: #fef2f2;
    border: 1px solid #fecaca;
    color: #991b1b;
  }

  .alert-icon {
    font-size: 1.125rem;
  }

  .modal-footer {
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
    padding: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #cbd5e1;
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
