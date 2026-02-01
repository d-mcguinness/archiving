<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_ARCHIVES, GET_ALL_USERS } from '$lib/graphql/queries';

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

  onMount(async () => {
    await Promise.all([loadArchives(), loadUsers()]);
  });

  async function loadArchives() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_ARCHIVES,
        fetchPolicy: 'network-only' // Always fetch fresh data
      });
      archives = result?.data?.getAllArchives || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load archives error:', e);
    } finally {
      loading = false;
    }
  }

  async function loadUsers() {
    try {
      const result = await client.query({ query: GET_ALL_USERS });
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
      // Call the Spring Boot REST endpoint
      const response = await fetch(`/api/archives/${selectedArchiveForExtract.id}/extract`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: extractPassword })
      });

      if (!response.ok) {
        // Try to get error message from response
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || `Server error: ${response.status}`);
      }

      // Get filename from Content-Disposition header
      const contentDisposition = response.headers.get('Content-Disposition');
      const filenameMatch = contentDisposition?.match(/filename="?(.+?)"?$/);
      const filename = filenameMatch?.[1] || `archive_${selectedArchiveForExtract.id}_export.json`;

      // Download the file
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      // Close dialog on success
      closeExtractDialog();
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
    <h1>Archives</h1>
    <a href="/archives/create" class="add-archive-btn">Add Archive</a>
  </div>

  {#if error}
    <div class="error">
      Error: {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
    </div>
  {:else if archives.length === 0}
    <div class="empty-state">
      <p>No archives found. Create your first archive to get started!</p>
    </div>
  {:else}
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
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
                    {#each archive.assignedUsers.slice(0, 2) as assignment}
                      <span class="user-badge">{getUserName(assignment.userId)}</span>
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
                <button
                  class="btn-action btn-extract"
                  on:click={() => openExtractDialog(archive)}
                  title="Extract archive"
                >
                  📥 Extract
                </button>
                <a href="/archives/document?archiveId={archive.id}&standard={archive.standard}" class="btn-action btn-upload">
                  📄 Upload
                </a>
                <a href="/archives/update/{archive.id}" class="btn-action btn-edit">
                  ✏️ Edit
                </a>
                <a href="/archives/delete/{archive.id}" class="btn-action btn-delete">
                  🗑️ Delete
                </a>
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
    border-radius: 0.375rem;
    text-decoration: none;
    font-weight: 500;
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
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    border: 1px solid #fcc;
  }

  .loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
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
    background: #f9fafb;
    border-radius: 0.5rem;
    color: #64748b;
  }

  /* Table Styles */
  .table-container {
    background: white;
    border-radius: 0.5rem;
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
    border-bottom: 2px solid #e2e8f0;
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
    font-weight: 500;
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
    max-height: 2.8em;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .status-cell,
  .standard-cell {
    white-space: nowrap;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.25rem;
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
    background: #f3f4f6;
    color: #6b7280;
  }

  .badge.archived,
  .badge.deleted {
    background: #fef3c7;
    color: #92400e;
  }

  .standard-badge {
    background: #e0e7ff;
    color: #4338ca;
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
    background: #dbeafe;
    color: #1e40af;
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
    width: 420px;
  }

  .btn-action {
    display: inline-block;
    padding: 0.5rem 1rem;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 500;
    transition: all 0.2s;
    margin-left: 0.5rem;
    border: none;
    cursor: pointer;
    text-decoration: none;
  }

  .btn-extract {
    background: #8b5cf6;
    color: white;
  }

  .btn-extract:hover {
    background: #7c3aed;
  }

  .btn-upload {
    background: #10b981;
    color: white;
  }

  .btn-upload:hover {
    background: #059669;
  }

  .btn-edit {
    background: #3b82f6;
    color: white;
  }

  .btn-edit:hover {
    background: #2563eb;
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
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
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
    border-radius: 0.375rem;
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
    border-radius: 0.375rem;
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
    border-radius: 0.375rem;
    font-weight: 500;
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
    box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
  }

  .btn-secondary {
    background: #f1f5f9;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #e2e8f0;
  }
</style>
