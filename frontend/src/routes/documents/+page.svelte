<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';

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
    checkAuth();
    await loadDocuments();
  });

  function checkAuth() {
    if (typeof window === 'undefined') return;

    const token = localStorage.getItem('auth_token');
    const user = localStorage.getItem('auth_user');
    const role = localStorage.getItem('auth_role');

    if (!token || !user) {
      goto('/login');
      return;
    }

    try {
      currentUser = JSON.parse(user);
      currentRole = role || '';
    } catch (e) {
      console.error('Error parsing user data:', e);
      goto('/login');
    }
  }

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

      const response = await fetch(`http://localhost:2020/api/documents?${params.toString()}`);

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

      const response = await fetch('http://localhost:2020/api/documents/upload', {
        method: 'POST',
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

  async function handleDownload(document: any) {
    try {
      const response = await fetch(`http://localhost:2020/api/documents/${document.id}/download`);
      const data = await response.json();

      if (data.success && data.downloadUrl) {
        window.open(data.downloadUrl, '_blank');
      } else {
        throw new Error(data.error || 'Failed to get download URL');
      }
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
      const response = await fetch(`http://localhost:2020/api/documents/${id}`, {
        method: 'DELETE'
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
  <div class="page-header">
    <h1>📄 Documents</h1>
    <button class="btn-primary" on:click={() => showUploadForm = !showUploadForm}>
      ➕ Upload Document
    </button>
  </div>

  {#if showUploadForm}
    <div class="upload-form">
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
                <p><strong>Status:</strong> <span class="status status-{document.status.toLowerCase()}">{document.status}</span></p>
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
    color: #1e293b;
    margin: 0;
  }

  .upload-form {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 2rem;
    margin-bottom: 2rem;
  }

  .upload-form h2 {
    margin-top: 0;
    color: #1e293b;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #475569;
  }

  .form-group input[type="text"],
  .form-group input[type="file"],
  .form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 4px;
    font-size: 1rem;
  }

  .file-info {
    margin-top: 0.5rem;
    color: #64748b;
    font-size: 0.875rem;
  }

  .form-actions {
    display: flex;
    gap: 1rem;
  }

  .btn-primary, .btn-secondary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .btn-primary {
    background-color: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background-color: #2563eb;
  }

  .btn-primary:disabled {
    background-color: #94a3b8;
    cursor: not-allowed;
  }

  .btn-secondary {
    background-color: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background-color: #cbd5e1;
  }

  .loading, .error, .empty-state {
    text-align: center;
    padding: 3rem;
    color: #64748b;
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
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 1.5rem;
    transition: box-shadow 0.2s;
  }

  .document-card:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  }

  .document-icon {
    font-size: 3rem;
    text-align: center;
    margin-bottom: 1rem;
  }

  .document-info h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1.25rem;
  }

  .description {
    color: #64748b;
    margin-bottom: 1rem;
    font-size: 0.875rem;
  }

  .document-meta {
    font-size: 0.875rem;
    color: #64748b;
    margin-bottom: 1rem;
  }

  .document-meta p {
    margin: 0.25rem 0;
  }

  .status {
    display: inline-block;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .status-active {
    background-color: #dcfce7;
    color: #166534;
  }

  .status-archived {
    background-color: #f3f4f6;
    color: #4b5563;
  }

  .status-pending_review {
    background-color: #fef3c7;
    color: #92400e;
  }

  .document-actions {
    display: flex;
    gap: 0.5rem;
  }

  .btn-download, .btn-delete {
    flex: 1;
    padding: 0.5rem;
    border: none;
    border-radius: 4px;
    font-size: 0.875rem;
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .btn-download {
    background-color: #3b82f6;
    color: white;
  }

  .btn-download:hover {
    background-color: #2563eb;
  }

  .btn-delete {
    background-color: #ef4444;
    color: white;
  }

  .btn-delete:hover {
    background-color: #dc2626;
  }
</style>

