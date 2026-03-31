<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ARCHIVES_BY_TENANT, GET_ALL_USERS, GET_TENANT } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
  }

  export let data: PageData;

  let archives: any[] = [];
  let users: any[] = [];
  let tenant: any = null;
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

  // Extract dialog state
  let showExtractDialog = false;
  let selectedArchiveForExtract: any = null;
  let extractPassword = '';
  let extracting = false;
  let extractError: string | null = null;

  // Export package state
  let exporting = false;

  // Create archive modal state
  let showCreateModal = false;
  let creating = false;
  let createError: string | null = null;
  let newArchive = {
    ownerId: '',
    userId: '',
    title: '',
    description: '',
    content: '',
    standard: 'NOARK5'
  };

  const standards = ['NOARK5', 'OAIS', 'PREMIS', 'Dublin Core', 'METS', 'EAD', 'BagIt', 'ISAD(G)', 'MODS'];

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    // Check access - ADMIN can view any tenant, TENANT can view their own
    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else if (currentRole === 'USER') {
      // USER should not access this page
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

    await Promise.all([loadTenant(), loadArchives(), loadUsers()]);
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

  async function loadArchives() {
    try {
      loading = true;

      // Fetch archives by tenant
      const result = await client.query({
        query: GET_ARCHIVES_BY_TENANT,
        variables: { tenantId: data.tenantId },
        fetchPolicy: 'network-only'
      });

      archives = result?.data?.getArchivesByTenant || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load archives error:', e);
      toasts.error('Failed to load archives');
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

      closeExtractDialog();
      toasts.success('Archive extracted successfully');
    } catch (e) {
      extractError = e instanceof Error ? e.message : 'Failed to extract archive';
    } finally {
      extracting = false;
    }
  }

  async function handleExportPackage(archive: any) {
    exporting = true;
    try {
      const response = await fetch(`http://localhost:2020/api/archives/${archive.id}/export-package`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || `Export failed: ${response.status}`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      const contentDisposition = response.headers.get('Content-Disposition');
      const filenameMatch = contentDisposition?.match(/filename="?(.+?)"?$/);
      link.download = filenameMatch?.[1] || `archive_${archive.id}_package.zip`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      toasts.success('Package exported successfully');
    } catch (e) {
      toasts.error(e instanceof Error ? e.message : 'Failed to export package');
    } finally {
      exporting = false;
    }
  }

  function openCreateModal() {
    newArchive = { ownerId: '', userId: '', title: '', description: '', content: '', standard: 'NOARK5' };
    createError = null;
    showCreateModal = true;
  }

  function closeCreateModal() {
    showCreateModal = false;
    createError = null;
    creating = false;
  }

  function fillRandomArchive() {
    const titles = ['Annual Report Archive', 'Legal Documents', 'Project Files', 'Financial Records', 'HR Documentation', 'Technical Specs', 'Client Correspondence', 'Research Data'];
    const descs = ['Collection of important organizational documents', 'Archived records for compliance purposes', 'Historical data preservation', 'Critical business documentation'];
    const contents = ['Archived content ready for long-term preservation', 'Digital records maintained per regulatory requirements', 'Organizational knowledge base archive', 'Structured data collection for institutional memory'];
    newArchive.title = titles[Math.floor(Math.random() * titles.length)];
    newArchive.description = descs[Math.floor(Math.random() * descs.length)];
    newArchive.content = contents[Math.floor(Math.random() * contents.length)];
    newArchive.standard = standards[Math.floor(Math.random() * standards.length)];
    if (users.length > 0) {
      newArchive.ownerId = users[Math.floor(Math.random() * users.length)].id;
      newArchive.userId = users[Math.floor(Math.random() * users.length)].id;
    }
  }

  async function handleCreateArchive() {
    if (!newArchive.userId || !newArchive.ownerId || !newArchive.title || !newArchive.content) {
      createError = 'Please fill in all required fields';
      return;
    }

    const standardMap: Record<string, string> = {
      'NOARK5': 'NOARK5', 'OAIS': 'OAIS', 'PREMIS': 'PREMIS',
      'Dublin Core': 'DUBLIN_CORE', 'METS': 'METS', 'EAD': 'EAD',
      'BagIt': 'BAGIT', 'ISAD(G)': 'ISADG', 'MODS': 'MODS'
    };

    try {
      creating = true;
      createError = null;

      const CREATE_ARCHIVE = gql`
        mutation CreateArchive($input: CreateArchiveInput!) {
          createArchive(input: $input) {
            id
            title
            status
            standard
          }
        }
      `;

      const result = await client.mutate({
        mutation: CREATE_ARCHIVE,
        variables: {
          input: {
            tenantId: parseInt(data.tenantId),
            ownerId: parseInt(newArchive.ownerId),
            userId: parseInt(newArchive.userId),
            title: newArchive.title,
            description: newArchive.description || null,
            content: newArchive.content,
            standard: standardMap[newArchive.standard] || newArchive.standard
          }
        }
      });

      if (result.data?.createArchive) {
        toasts.success(`Archive "${newArchive.title}" created successfully`);
        closeCreateModal();
        await loadArchives();
      }
    } catch (e) {
      createError = e instanceof Error ? e.message : 'An unknown error occurred';
      toasts.error(`Failed to create archive: ${createError}`);
    } finally {
      creating = false;
    }
  }
</script>

<svelte:head>
  <title>{tenant ? `${tenant.displayName || tenant.name} - ` : ''}Archives - Archiving System</title>
</svelte:head>

<div class="tenant-archives-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to view these archives.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[{ label: 'Archives' }]}
    />

    <!-- Page Header -->
    <div class="page-header">
      <div class="header-content">
        <h1>📁 Archives</h1>
        {#if tenant}
          <div class="tenant-badge">
            <span class="tenant-icon">🏢</span>
            <span class="tenant-name">{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
      </div>
      <div class="header-buttons">
        <button class="add-archive-btn" on:click={openCreateModal}>+ Create Archive</button>
      </div>
    </div>

    {#if error}
      <div class="error">
        ❌ {error}
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
        <p>This tenant doesn't have any archives yet.</p>
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
              <th>Status</th>
              <th>Standard</th>
              <th>Created</th>
              <th>Updated</th>
              <th>Assigned Users</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {#each archives as archive (archive.id)}
              <tr class="clickable-row" on:click={() => goto(`/tenants/${data.tenantId}/archives/${archive.id}`)}>
                <td class="id-cell">{archive.id}</td>
                <td class="title-cell">
                  <div class="title-wrapper">
                    <a href="/tenants/{data.tenantId}/archives/{archive.id}" class="archive-title-link">{archive.title}</a>
                    {#if archive.description}
                      <div class="archive-description">{archive.description}</div>
                    {/if}
                  </div>
                </td>
                <td class="status-cell">
                  <span class="badge {getStatusBadgeClass(archive.status)}">{archive.status}</span>
                </td>
                <td class="standard-cell">
                  <span class="badge standard-badge">{archive.standard}</span>
                </td>
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
                  <a href="/tenants/{data.tenantId}/archives/{archive.id}/update" class="btn-action btn-edit">
                    ✏️ Edit
                  </a>
                  <a href="/tenants/{data.tenantId}/archives/{archive.id}/sips" class="btn-action btn-sips">
                    📦 SIPs
                  </a>
                  <a href="/tenants/{data.tenantId}/aips" class="btn-action btn-aips">
                    🏗️ AIPs
                  </a>
                  <a href="/tenants/{data.tenantId}/dips" class="btn-action btn-dips">
                    📤 DIPs
                  </a>
                  <a href="/tenants/{data.tenantId}/users" class="btn-action btn-users">
                    👥 Users
                  </a>
                  {#if currentRole === 'ADMIN'}
                    <a href="/archives/delete/{archive.id}" class="btn-action btn-delete">
                      🗑️ Delete
                    </a>
                    <a
                      href="/tenants/{data.tenantId}/archives/{archive.id}/extract"
                      class="btn-action btn-extract"
                      title="Extract archive as JSON"
                    >
                      📥 Extract
                    </a>
                    <button
                      class="btn-action btn-package"
                      on:click={() => handleExportPackage(archive)}
                      disabled={exporting}
                      title="Export full package as ZIP"
                    >
                      {exporting ? '...' : '📦 Package'}
                    </button>
                  {/if}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
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
              id="extractPassword"
              type="password"
              bind:value={extractPassword}
              placeholder="Enter password"
              disabled={extracting}
              on:keydown={(e) => e.key === 'Enter' && handleExtract()}
            />
          </div>
        {/if}
      </div>

      <div class="modal-footer">
        <button class="btn-secondary" on:click={closeExtractDialog} disabled={extracting}>
          Cancel
        </button>
        <button class="btn-primary" on:click={handleExtract} disabled={extracting || !extractPassword}>
          {extracting ? '⏳ Extracting...' : '📥 Extract'}
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- Create Archive Modal -->
{#if showCreateModal}
  <div class="modal-overlay" on:click={closeCreateModal} role="dialog" aria-modal="true">
    <div class="modal-content modal-wide" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>Create New Archive</h3>
        <div class="modal-header-actions">
          <button type="button" class="btn-fill" on:click={fillRandomArchive}>Fill Random</button>
          <button class="modal-close" on:click={closeCreateModal} aria-label="Close">×</button>
        </div>
      </div>

      <div class="modal-body">
        {#if createError}
          <div class="alert alert-error">
            <span class="alert-icon">⚠️</span>
            <span>{createError}</span>
          </div>
        {/if}

        <form on:submit|preventDefault={handleCreateArchive}>
          <div class="form-row">
            <div class="form-group">
              <label for="createOwnerId">Archive Owner *</label>
              <select id="createOwnerId" bind:value={newArchive.ownerId} required disabled={creating}>
                <option value="">Select owner</option>
                {#each users as user}
                  <option value={user.id}>{user.name} ({user.email})</option>
                {/each}
              </select>
            </div>
            <div class="form-group">
              <label for="createUserId">Creator *</label>
              <select id="createUserId" bind:value={newArchive.userId} required disabled={creating}>
                <option value="">Select creator</option>
                {#each users as user}
                  <option value={user.id}>{user.name} ({user.email})</option>
                {/each}
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="createTitle">Title *</label>
              <input type="text" id="createTitle" bind:value={newArchive.title} required disabled={creating} placeholder="Enter archive title" />
            </div>
            <div class="form-group">
              <label for="createStandard">Standard *</label>
              <select id="createStandard" bind:value={newArchive.standard} required disabled={creating}>
                {#each standards as std}
                  <option value={std}>{std}</option>
                {/each}
              </select>
            </div>
          </div>

          <div class="form-group">
            <label for="createDescription">Description</label>
            <textarea id="createDescription" bind:value={newArchive.description} disabled={creating} rows="2" placeholder="Optional description"></textarea>
          </div>

          <div class="form-group">
            <label for="createContent">Content *</label>
            <textarea id="createContent" bind:value={newArchive.content} required disabled={creating} rows="3" placeholder="Enter archive content"></textarea>
          </div>
        </form>
      </div>

      <div class="modal-footer">
        <button class="btn-secondary" on:click={closeCreateModal} disabled={creating}>Cancel</button>
        <button
          class="btn-primary"
          on:click={handleCreateArchive}
          disabled={creating || !newArchive.ownerId || !newArchive.userId || !newArchive.title || !newArchive.content}
        >
          {creating ? 'Creating...' : 'Create Archive'}
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  .tenant-archives-page {
    max-width: 1600px;
    margin: 0 auto;
    padding: 2rem;
  }

  /* Access Denied */
  .access-denied {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    text-align: center;
    padding: 3rem;
  }

  .access-denied-icon {
    font-size: 5rem;
    margin-bottom: 1.5rem;
  }

  .access-denied h1 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: #64748b;
    font-size: 1.125rem;
  }

  .redirect-message {
    color: #3b82f6;
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  /* Breadcrumb */
  .breadcrumb {
    display: flex; align-items: center; gap: 0.5rem;
    margin-bottom: 1.5rem; font-size: 0.875rem;
  }

  .breadcrumb a {
    color: #3b82f6;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s;
  }

  .breadcrumb a:hover {
    color: #2563eb;
  }

  .breadcrumb .sep { color: #94a3b8; }
  .breadcrumb > span:last-child { color: #64748b; font-weight: 600;
  }

  /* Page Header */
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 2rem;
    gap: 1rem;
  }

  .header-content {
    flex: 1;
  }

  .page-header h1 {
    margin: 0 0 0.75rem 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .tenant-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 1rem;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 0.5rem;
    font-weight: 600;
  }

  .tenant-icon {
    font-size: 1.25rem;
  }

  .add-archive-btn {
    padding: 0.75rem 1.5rem;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
  }

  .add-archive-btn:hover {
    background: #2563eb;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  /* Loading */
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

  /* Error */
  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.5rem;
    border: 1px solid #fcc;
    margin-bottom: 1.5rem;
  }

  /* Empty State */
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

  /* Archives Count */
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

  /* Table */
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
    border-bottom: 2px solid #e2e8f0;
  }

  .data-table td {
    padding: 1rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .data-table tbody tr:last-child td {
    border-bottom: none;
  }

  .data-table tbody tr:hover {
    background: #f8fafc;
  }

  .id-cell {
    color: #64748b;
    font-weight: 500;
    width: 60px;
  }

  .title-cell {
    min-width: 250px;
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

  .badge.published {
    background: #dcfce7;
    color: #166534;
  }

  .badge.draft {
    background: #fef3c7;
    color: #92400e;
  }

  .badge.archived {
    background: #f1f5f9;
    color: #475569;
  }

  .standard-badge {
    background: #dbeafe;
    color: #1e40af;
  }

  .date-cell {
    color: #64748b;
    font-size: 0.875rem;
    white-space: nowrap;
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
    color: #94a3b8;
    font-size: 0.875rem;
  }

  .actions-cell {
    white-space: nowrap;
  }

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

  .btn-delete {
    background: #fee;
    color: #c00;
  }

  .btn-delete:hover {
    background: #fcc;
  }

  .btn-edit {
    background: #dbeafe;
    color: #1e40af;
  }

  .btn-edit:hover {
    background: #bfdbfe;
  }

  .header-buttons {
    display: flex;
    gap: 0.5rem;
    align-items: center;
  }

  .btn-package {
    background: #8b5cf6;
    color: white;
  }

  .btn-package:hover:not(:disabled) {
    background: #7c3aed;
  }

  .btn-package:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .clickable-row { cursor: pointer; }
  .clickable-row:hover { background: #f1f5f9 !important; }

  .archive-title-link {
    color: #1e40af;
    text-decoration: none;
    font-weight: 600;
  }
  .archive-title-link:hover { text-decoration: underline; }

  .btn-sips { background: #fce7f3; color: #9d174d; }
  .btn-sips:hover { background: #fbcfe8; }

  .btn-aips { background: #eef2ff; color: #3730a3; }
  .btn-aips:hover { background: #c7d2fe; }

  .btn-dips { background: #fff7ed; color: #9a3412; }
  .btn-dips:hover { background: #fed7aa; }

  .btn-users { background: #fef3c7; color: #92400e; }
  .btn-users:hover { background: #fde68a; }

  .btn-extract {
    background: #dcfce7;
    color: #166534;
  }

  .btn-extract:hover {
    background: #bbf7d0;
  }

  /* Modal */
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
  }

  .modal-content {
    background: white;
    border-radius: 0.75rem;
    max-width: 500px;
    width: 90%;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  }

  .modal-wide {
    max-width: 700px;
  }

  .modal-header-actions {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .btn-fill {
    padding: 0.375rem 0.75rem;
    background: #f0fdf4;
    color: #16a34a;
    border: 1px solid #bbf7d0;
    border-radius: 0.375rem;
    font-weight: 600;
    font-size: 0.8rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-fill:hover {
    background: #dcfce7;
    border-color: #86efac;
  }

  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }

  .form-group select,
  .form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-family: inherit;
  }

  .form-group select:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .form-group select:disabled,
  .form-group textarea:disabled,
  .form-group input:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .form-group textarea {
    resize: vertical;
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
  }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: #64748b;
    cursor: pointer;
    transition: color 0.2s;
  }

  .modal-close:hover {
    color: #1e293b;
  }

  .modal-body {
    padding: 1.5rem;
  }

  .archive-info {
    margin: 0 0 1rem 0;
    color: #1e293b;
  }

  .info-text {
    margin: 0 0 1.5rem 0;
    color: #64748b;
    font-size: 0.875rem;
  }

  .alert {
    padding: 0.75rem 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .alert-error {
    background: #fee;
    color: #c00;
    border: 1px solid #fcc;
  }

  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #1e293b;
    font-weight: 600;
  }

  .form-group input {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    font-size: 1rem;
  }

  .form-group input:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    padding: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn-secondary,
  .btn-primary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-secondary {
    background: #f1f5f9;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #e2e8f0;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-primary:disabled,
  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  @media (max-width: 768px) {
    .tenant-archives-page {
      padding: 1rem;
    }

    .page-header {
      flex-direction: column;
    }

    .table-container {
      overflow-x: auto;
    }

    .data-table {
      min-width: 800px;
    }
  }
</style>

