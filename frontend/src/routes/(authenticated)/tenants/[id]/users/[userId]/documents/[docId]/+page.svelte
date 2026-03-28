<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';

  const STATUSES = ['ACTIVE', 'ARCHIVED', 'DELETED', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'];

  interface PageData {
    tenantId: string;
    userId: string;
    docId: string;
  }

  export let data: PageData;

  let doc: any = null;
  let loading = true;
  let error: string | null = null;
  let hasAccess = false;

  // Edit state
  let editing = false;
  let saving = false;
  let editForm = {
    title: '',
    description: '',
    status: ''
  };

  onMount(async () => {
    const authState = get(auth);
    const currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    const authUserId = authState.user?.id?.toString() ?? null;

    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else if (currentRole === 'USER' && authUserId === data.userId) {
      hasAccess = true;
    } else {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to view this document');
      goto('/');
      return;
    }

    await loadDocument();
  });

  async function loadDocument() {
    loading = true;
    try {
      const response = await fetch(`http://localhost:2020/api/documents/${data.docId}`);
      if (!response.ok) throw new Error('Failed to fetch document');
      const result = await response.json();
      if (result.success) {
        doc = result.document;
        editForm = {
          title: doc.title || '',
          description: doc.description || '',
          status: doc.status || 'ACTIVE'
        };
      } else {
        throw new Error(result.error || 'Document not found');
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load document';
    } finally {
      loading = false;
    }
  }

  function startEdit() {
    editForm = {
      title: doc.title || '',
      description: doc.description || '',
      status: doc.status || 'ACTIVE'
    };
    editing = true;
  }

  function cancelEdit() {
    editing = false;
  }

  async function saveEdit() {
    saving = true;
    try {
      const response = await fetch(`http://localhost:2020/api/documents/${data.docId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: editForm.title,
          description: editForm.description,
          status: editForm.status
        })
      });
      const result = await response.json();
      if (result.success) {
        doc = result.document;
        editing = false;
        toasts.success('Document updated successfully');
      } else {
        throw new Error(result.error || 'Update failed');
      }
    } catch (e) {
      toasts.error(e instanceof Error ? e.message : 'Failed to update document');
    } finally {
      saving = false;
    }
  }

  async function handleDownload() {
    try {
      const response = await fetch(`http://localhost:2020/api/documents/${data.docId}/file`);
      if (!response.ok) throw new Error('Download failed');
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = doc.fileName || 'download';
      window.document.body.appendChild(link);
      link.click();
      window.document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (e) {
      toasts.error(e instanceof Error ? e.message : 'Failed to download');
    }
  }

  async function handleDelete() {
    if (!confirm('Are you sure you want to delete this document?')) return;
    try {
      const response = await fetch(`http://localhost:2020/api/documents/${data.docId}`, {
        method: 'DELETE'
      });
      const result = await response.json();
      if (result.success) {
        toasts.success('Document deleted');
        goto(`/tenants/${data.tenantId}/users/${data.userId}/documents`);
      } else {
        throw new Error(result.error || 'Delete failed');
      }
    } catch (e) {
      toasts.error(e instanceof Error ? e.message : 'Failed to delete document');
    }
  }

  function formatFileSize(bytes: number): string {
    if (!bytes) return 'Unknown';
    const mb = bytes / (1024 * 1024);
    if (mb > 1) return `${mb.toFixed(2)} MB`;
    return `${(bytes / 1024).toFixed(2)} KB`;
  }

  function formatDate(dateString: string): string {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleString();
  }
</script>

<svelte:head>
  <title>{doc ? doc.title : 'Document'} - Archiving System</title>
</svelte:head>

<div class="doc-detail-page">
  {#if !hasAccess && !loading}
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
    </div>
  {:else if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading document...</p>
    </div>
  {:else if error}
    <div class="error-msg">❌ {error}</div>
  {:else if doc}
    <div class="breadcrumb">
      <a href="/tenants/{data.tenantId}">Tenant</a>
      <span class="sep">/</span>
      <a href="/tenants/{data.tenantId}/users/{data.userId}">User</a>
      <span class="sep">/</span>
      <a href="/tenants/{data.tenantId}/users/{data.userId}/documents">Documents</a>
      <span class="sep">/</span>
      <span>{doc.title || doc.fileName}</span>
    </div>

    <div class="page-header">
      <div class="header-content">
        <h1>{doc.title || doc.fileName}</h1>
        <span class="status-badge status-{doc.status?.toLowerCase()}">{doc.status}</span>
      </div>
      <div class="header-actions">
        <button class="btn-download" on:click={handleDownload}>📥 Download</button>
        {#if !editing}
          <button class="btn-edit" on:click={startEdit}>✏️ Edit</button>
        {/if}
        <button class="btn-delete" on:click={handleDelete}>🗑️ Delete</button>
      </div>
    </div>

    <!-- Document Info -->
    <div class="panel">
      <h2>Details</h2>

      {#if editing}
        <form on:submit|preventDefault={saveEdit}>
          <div class="form-group">
            <label for="edit-title">Title</label>
            <input id="edit-title" type="text" bind:value={editForm.title} disabled={saving} />
          </div>
          <div class="form-group">
            <label for="edit-desc">Description</label>
            <textarea id="edit-desc" bind:value={editForm.description} rows="3" disabled={saving}></textarea>
          </div>
          <div class="form-group">
            <label for="edit-status">Status</label>
            <select id="edit-status" bind:value={editForm.status} disabled={saving}>
              {#each STATUSES as status}
                <option value={status}>{status.replace('_', ' ')}</option>
              {/each}
            </select>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
            <button type="button" class="btn-secondary" on:click={cancelEdit} disabled={saving}>Cancel</button>
          </div>
        </form>
      {:else}
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">Title</span>
            <span class="info-value">{doc.title || '-'}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Description</span>
            <span class="info-value">{doc.description || '-'}</span>
          </div>
          <div class="info-item">
            <span class="info-label">File Name</span>
            <span class="info-value mono">{doc.fileName}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Size</span>
            <span class="info-value">{formatFileSize(doc.fileSize)}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Content Type</span>
            <span class="info-value mono">{doc.contentType || 'Unknown'}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Status</span>
            <span class="info-value">
              <span class="status-badge status-{doc.status?.toLowerCase()}">{doc.status}</span>
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">Uploaded</span>
            <span class="info-value">{formatDate(doc.uploadedAt)}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Created</span>
            <span class="info-value">{formatDate(doc.createdAt)}</span>
          </div>
          {#if doc.updatedAt}
            <div class="info-item">
              <span class="info-label">Last Updated</span>
              <span class="info-value">{formatDate(doc.updatedAt)}</span>
            </div>
          {/if}
          {#if doc.archiveId}
            <div class="info-item">
              <span class="info-label">Archive ID</span>
              <span class="info-value mono">{doc.archiveId}</span>
            </div>
          {/if}
          {#if doc.sipId}
            <div class="info-item">
              <span class="info-label">SIP ID</span>
              <span class="info-value mono">{doc.sipId}</span>
            </div>
          {/if}
        </div>
      {/if}
    </div>
  {/if}
</div>

<style>
  .doc-detail-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 2rem;
  }

  .access-denied {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; text-align: center;
  }
  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0; color: #1e293b; }

  .loading {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; gap: 1rem;
  }
  .spinner {
    border: 4px solid #f3f4f6; border-top: 4px solid #8b5cf6;
    border-radius: 50%; width: 40px; height: 40px;
    animation: spin 1s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .error-msg {
    background: #fee2e2; color: #991b1b; padding: 1rem;
    border-radius: 0.5rem; border: 1px solid #fca5a5;
  }

  .breadcrumb {
    display: flex; align-items: center; gap: 0.5rem;
    margin-bottom: 1.5rem; font-size: 0.875rem; flex-wrap: wrap;
  }
  .breadcrumb a { color: #3b82f6; text-decoration: none; font-weight: 500; }
  .breadcrumb a:hover { color: #2563eb; }
  .sep { color: #94a3b8; }
  .breadcrumb > span:last-child { color: #64748b; }

  .page-header {
    display: flex; justify-content: space-between; align-items: flex-start;
    margin-bottom: 2rem; gap: 1rem;
  }
  .header-content { flex: 1; }
  .page-header h1 {
    margin: 0 0 0.5rem; color: #1e293b; font-size: 1.75rem;
    word-break: break-word;
  }

  .header-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }

  .btn-download, .btn-edit, .btn-delete {
    padding: 0.6rem 1rem; border: none; border-radius: 0.5rem;
    font-weight: 600; font-size: 0.85rem; cursor: pointer; transition: background 0.2s;
  }
  .btn-download { background: #3b82f6; color: white; }
  .btn-download:hover { background: #2563eb; }
  .btn-edit { background: #dbeafe; color: #1e40af; }
  .btn-edit:hover { background: #bfdbfe; }
  .btn-delete { background: #fee2e2; color: #dc2626; }
  .btn-delete:hover { background: #fecaca; }

  .panel {
    background: white; border: 1px solid #e2e8f0; border-radius: 0.75rem;
    padding: 1.5rem; margin-bottom: 1.5rem;
  }
  .panel h2 {
    margin: 0 0 1.25rem; font-size: 1.1rem; color: #1e293b;
    padding-bottom: 0.75rem; border-bottom: 1px solid #f1f5f9;
  }

  .info-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 1.25rem;
  }
  .info-item { display: flex; flex-direction: column; gap: 0.25rem; }
  .info-label {
    font-size: 0.7rem; font-weight: 600; color: #64748b;
    text-transform: uppercase; letter-spacing: 0.05em;
  }
  .info-value { font-size: 0.95rem; color: #1e293b; }
  .mono { font-family: monospace; color: #475569; font-size: 0.85rem; }

  .status-badge {
    display: inline-block; padding: 0.2rem 0.6rem; border-radius: 0.25rem;
    font-size: 0.7rem; font-weight: 600; text-transform: uppercase;
  }
  .status-active { background: #dcfce7; color: #166534; }
  .status-archived { background: #f3f4f6; color: #4b5563; }
  .status-deleted { background: #fee2e2; color: #991b1b; }
  .status-pending_review { background: #fef3c7; color: #92400e; }
  .status-approved { background: #dbeafe; color: #1e40af; }
  .status-rejected { background: #fee2e2; color: #991b1b; }

  .form-group { margin-bottom: 1rem; }
  .form-group label {
    display: block; margin-bottom: 0.35rem;
    font-weight: 600; color: #475569; font-size: 0.85rem;
  }
  .form-group input, .form-group textarea, .form-group select {
    width: 100%; padding: 0.6rem; border: 1px solid #cbd5e1;
    border-radius: 0.375rem; font-size: 0.95rem; box-sizing: border-box;
  }
  .form-group textarea { resize: vertical; }
  .form-group input:focus, .form-group textarea:focus, .form-group select:focus {
    outline: none; border-color: #8b5cf6;
    box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
  }
  .form-group input:disabled, .form-group textarea:disabled, .form-group select:disabled {
    background: #f1f5f9; cursor: not-allowed;
  }

  .form-actions { display: flex; gap: 0.5rem; margin-top: 1.25rem; }
  .btn-primary, .btn-secondary {
    padding: 0.6rem 1.2rem; border: none; border-radius: 0.375rem;
    font-weight: 600; cursor: pointer; transition: all 0.2s;
  }
  .btn-primary { background: #8b5cf6; color: white; }
  .btn-primary:hover:not(:disabled) { background: #7c3aed; }
  .btn-primary:disabled { background: #94a3b8; cursor: not-allowed; }
  .btn-secondary { background: #e2e8f0; color: #475569; }
  .btn-secondary:hover:not(:disabled) { background: #cbd5e1; }

  @media (max-width: 640px) {
    .page-header { flex-direction: column; }
    .header-actions { width: 100%; }
    .info-grid { grid-template-columns: 1fr; }
  }
</style>
