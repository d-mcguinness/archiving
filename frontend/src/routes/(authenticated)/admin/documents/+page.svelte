<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { authHeaders, API_BASE } from '$lib/api';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let documents: any[] = [];
  let loading = true;
  let error = '';
  let currentUser: any = null;
  let currentRole = '';

  // Upload form
  let showUploadForm = false;
  let uploadFile: File | null = null;
  let uploadTitle = '';
  let uploadDescription = '';
  let uploading = false;

  onMount(async () => {
    const authState = get(auth);
    if (!authState.isLoggedIn) {
      goto('/login');
      return;
    }
    currentUser = authState.user;
    currentRole = authState.role;
    await loadDocuments();
  });

  async function loadDocuments() {
    loading = true;
    error = '';

    try {
      // Build query params based on role
      const params = new URLSearchParams();
      params.append('role', currentRole);

      if (currentRole === 'USER' && currentUser?.id) {
        params.append('userId', currentUser.id.toString());
      } else if (currentRole === 'TENANT') {
        // For TENANT role, we need to provide userId so backend can find their tenant
        // Backend will use userId to determine which tenant documents to show
        if (currentUser?.id) {
          params.append('userId', currentUser.id.toString());
        }
      }
      // ADMIN doesn't need extra params - will get all documents

      const response = await fetch(`${API_BASE}/api/documents?${params.toString()}`, {
        headers: { ...authHeaders() }
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || `Failed to fetch documents: ${response.statusText}`);
      }

      const data = await response.json();

      if (data.success) {
        documents = data.documents || [];
      } else {
        throw new Error(data.error || 'Failed to load documents');
      }
    } catch (err: any) {
      console.error('Error loading documents:', err);
      error = err.message || 'Failed to load documents';
      toasts.error(error);
    } finally {
      loading = false;
    }
  }

  function handleFileSelect(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      uploadFile = target.files[0];
      if (!uploadTitle) {
        uploadTitle = uploadFile.name;
      }
    }
  }

  async function handleUpload() {
    if (!uploadFile || !currentUser) {
      toasts.error('Please select a file');
      return;
    }

    uploading = true;

    try {
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('userId', currentUser.id.toString());

      if (currentUser.tenantId) {
        formData.append('tenantId', currentUser.tenantId.toString());
      }

      if (uploadTitle) {
        formData.append('title', uploadTitle);
      }

      if (uploadDescription) {
        formData.append('description', uploadDescription);
      }

      const uploadToken = typeof window !== 'undefined' ? localStorage.getItem('auth_token') : null;
      const response = await fetch(`${API_BASE}/api/documents/upload`, {
        method: 'POST',
        headers: { ...authHeaders(), ...(uploadToken ? { Authorization: uploadToken } : {}) },
        body: formData
      });

      const data = await response.json();

      if (data.success) {
        toasts.success('Document uploaded successfully');
        showUploadForm = false;
        uploadFile = null;
        uploadTitle = '';
        uploadDescription = '';
        await loadDocuments();
      } else {
        throw new Error(data.error || 'Upload failed');
      }
    } catch (err: any) {
      console.error('Upload error:', err);
      toasts.error(err.message || 'Failed to upload document');
    } finally {
      uploading = false;
    }
  }

  async function handleDownload(doc: any) {
    try {
      const response = await fetch(`${API_BASE}/api/documents/${doc.id}/file`, {
        headers: { ...authHeaders() }
      });
      if (!response.ok) {
        throw new Error(`Download failed: ${response.statusText}`);
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = window.document.createElement('a');
      link.href = url;
      link.download = doc.fileName || 'download';
      window.document.body.appendChild(link);
      link.click();
      window.document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      console.error('Download error:', err);
      toasts.error(err.message || 'Failed to download document');
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Are you sure you want to delete this document?')) {
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/api/documents/${id}`, {
        method: 'DELETE',
        headers: { ...authHeaders() }
      });

      const data = await response.json();

      if (data.success) {
        toasts.success('Document deleted successfully');
        await loadDocuments();
      } else {
        throw new Error(data.error || 'Delete failed');
      }
    } catch (err: any) {
      console.error('Delete error:', err);
      toasts.error(err.message || 'Failed to delete document');
    }
  }

  function formatFileSize(bytes: number): string {
    if (!bytes) return 'Unknown';
    const mb = bytes / (1024 * 1024);
    if (mb > 1) {
      return `${mb.toFixed(2)} MB`;
    }
    const kb = bytes / 1024;
    return `${kb.toFixed(2)} KB`;
  }

  function formatDate(dateString: string): string {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleString();
  }
</script>

<svelte:head>
  <title>Documents - Archiving System</title>
</svelte:head>

<div class="documents-page">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Documents' }]} />
  <div class="page-header">
    <div class="page-heading">
      <span class="eyebrow">Admin console</span>
      <h1>📄 Documents</h1>
    </div>
    <button class="btn-primary" on:click={() => showUploadForm = !showUploadForm}>
      ➕ Upload Document
    </button>
  </div>

  {#if showUploadForm}
    <div class="upload-form form-container">
      <h2>Upload New Document</h2>
      <form on:submit|preventDefault={handleUpload}>
        <div class="form-group">
          <label for="file">File *</label>
          <input
            type="file"
            id="file"
            on:change={handleFileSelect}
            required
            disabled={uploading}
          />
          {#if uploadFile}
            <p class="file-info">Selected: {uploadFile.name} ({formatFileSize(uploadFile.size)})</p>
          {/if}
        </div>

        <div class="form-group">
          <label for="title">Title</label>
          <input
            type="text"
            id="title"
            bind:value={uploadTitle}
            placeholder="Document title (optional)"
            disabled={uploading}
          />
        </div>

        <div class="form-group">
          <label for="description">Description</label>
          <textarea
            id="description"
            bind:value={uploadDescription}
            placeholder="Document description (optional)"
            rows="3"
            disabled={uploading}
          ></textarea>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn-primary" disabled={!uploadFile || uploading}>
            {uploading ? 'Uploading...' : 'Upload'}
          </button>
          <button type="button" class="btn-secondary" on:click={() => showUploadForm = false} disabled={uploading}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  {/if}

  {#if loading}
    <div class="loading">Loading documents...</div>
  {:else if error}
    <div class="error">{error}</div>
  {:else if documents.length === 0}
    <div class="empty-state">
      <p>No documents found.</p>
      <p>Upload your first document to get started!</p>
    </div>
  {:else}
    <div class="documents-list">
      <div class="documents-grid">
        {#each documents as document}
          <div class="document-card">
            <div class="document-icon">
              {#if document.contentType?.includes('pdf')}
                📄
              {:else if document.contentType?.includes('image')}
                🖼️
              {:else if document.contentType?.includes('video')}
                🎥
              {:else if document.contentType?.includes('word') || document.contentType?.includes('document')}
                📝
              {:else if document.contentType?.includes('spreadsheet') || document.contentType?.includes('excel')}
                📊
              {:else}
                📎
              {/if}
            </div>
            <div class="document-info">
              <h3>{document.title}</h3>
              {#if document.description}
                <p class="description">{document.description}</p>
              {/if}
              <div class="document-meta">
                <p><strong>File:</strong> {document.fileName}</p>
                <p><strong>Size:</strong> {formatFileSize(document.fileSize)}</p>
                <p><strong>Type:</strong> {document.contentType || 'Unknown'}</p>
                <p><strong>Uploaded:</strong> {formatDate(document.uploadedAt)}</p>
                <p><strong>Status:</strong> <span class="badge status status-{document.status.toLowerCase()}">{document.status}</span></p>
              </div>
            </div>
            <div class="document-actions">
              <button class="btn-download" on:click={() => handleDownload(document)}>
                ⬇️ Download
              </button>
              {#if currentRole === 'ADMIN' || document.userId === currentUser?.id}
                <button class="btn-delete" on:click={() => handleDelete(document.id)}>
                  🗑️ Delete
                </button>
              {/if}
            </div>
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>

<style>
  .documents-page {
    max-width: 1200px;
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
    font-size: 2rem;
    color: var(--arc-ink, #0f172a);
    margin: 0;
  }

  /* The panel itself is the global .form-container (app.css). */
  .upload-form h2 {
    margin-top: 0;
    color: var(--arc-ink, #0f172a);
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .file-info {
    margin-top: 0.5rem;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
  }

  /* .form-actions, .btn-primary and .btn-secondary come from the global
     kit (app.css). */

  .loading, .error, .empty-state {
    text-align: center;
    padding: 3rem;
    color: var(--arc-muted, #64748b);
  }

  .error {
    color: #dc2626;
  }

  .documents-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .document-card {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    padding: 1.5rem;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .document-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border, #c7d2fe);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  @media (prefers-reduced-motion: reduce) {
    .document-card {
      transition: none;
    }

    .document-card:hover {
      transform: none;
    }
  }

  .document-icon {
    font-size: 3rem;
    text-align: center;
    margin-bottom: 1rem;
  }

  .document-info h3 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.25rem;
  }

  .description {
    color: var(--arc-muted, #64748b);
    margin-bottom: 1rem;
    font-size: 0.875rem;
  }

  .document-meta {
    font-size: 0.875rem;
    color: var(--arc-muted, #64748b);
    margin-bottom: 1rem;
  }

  .document-meta p {
    margin: 0.25rem 0;
  }

  /* .badge base is global; these document status hues are page-specific. */
  .status-active {
    background-color: var(--arc-chip-green-bg, #dcfce7);
    color: var(--arc-chip-green-ink, #166534);
  }

  .status-archived {
    background-color: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #475569);
  }

  .status-pending_review {
    background-color: var(--arc-chip-amber-bg, #fef3c7);
    color: var(--arc-chip-amber-ink, #92400e);
  }

  .document-actions {
    display: flex;
    gap: 0.5rem;
  }

  .btn-download, .btn-delete {
    flex: 1;
    padding: 0.5rem;
    border: none;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 700;
    cursor: pointer;
    box-shadow: none;
    transition: all 0.2s ease;
  }

  .btn-download {
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
  }

  .btn-download:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
  }

  .btn-delete {
    background: linear-gradient(135deg, #ef4444, #dc2626);
    color: white;
  }

  .btn-delete:hover {
    background: linear-gradient(135deg, #dc2626, #b91c1c);
  }
</style>

