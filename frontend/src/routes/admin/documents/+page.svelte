<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';

  let documents: any[] = [];
  let loading = true;
  let error = '';
  let currentUser: any = null;
  let currentRole = '';
  let hasAccess = false;

  // Upload form
  let showUploadForm = false;
  let uploadFile: File | null = null;
  let uploadTitle = '';
  let uploadDescription = '';
  let uploading = false;

  onMount(async () => {
    checkAuth();
  });

  function checkAuth() {
    if (typeof window === 'undefined') return;

    const token = localStorage.getItem('auth_token');
    const user = localStorage.getItem('auth_user');
    const role = localStorage.getItem('auth_role');
    const tenantId = localStorage.getItem('auth_tenantId');

    if (!token || !user) {
      goto('/login');
      return;
    }

    try {
      currentUser = JSON.parse(user);
      currentRole = role || '';

      // Only ADMIN can access the admin documents page
      if (currentRole !== 'ADMIN') {
        hasAccess = false;
        loading = false;

        // Redirect non-admin users to appropriate pages
        if (currentRole === 'TENANT' && tenantId) {
          goto(`/tenants/${tenantId}/documents`);
        } else if (currentRole === 'USER' && tenantId && currentUser?.id) {
          goto(`/tenants/${tenantId}/users/${currentUser.id}/documents`);
        } else {
          goto('/');
        }
        return;
      }

      hasAccess = true;
      loadDocuments();
    } catch (e) {
      console.error('Error parsing user data:', e);
      goto('/login');
    }
  }

  async function loadDocuments() {
    loading = true;
    error = '';

    try {
      // ADMIN gets all documents
      const params = new URLSearchParams();
      params.append('role', 'ADMIN');

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

  function getFileIcon(contentType: string): string {
    if (!contentType) return '📎';
    if (contentType.includes('pdf')) return '📄';
    if (contentType.includes('image')) return '🖼️';
    if (contentType.includes('video')) return '🎥';
    if (contentType.includes('word') || contentType.includes('document')) return '📝';
    if (contentType.includes('spreadsheet') || contentType.includes('excel')) return '📊';
    return '📎';
  }
</script>

<svelte:head>
  <title>Documents - Admin - Archiving System</title>
</svelte:head>

<div class="documents-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to access the admin documents list.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <div class="page-header">
      <h1>📄 Documents Management</h1>
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
              {uploading ? '⏳ Uploading...' : '📤 Upload'}
            </button>
            <button type="button" class="btn-secondary" on:click={() => showUploadForm = false} disabled={uploading}>
              Cancel
            </button>
          </div>
        </form>
      </div>
    {/if}

    {#if loading}
      <div class="loading">
        <div class="spinner"></div>
        <p>Loading documents...</p>
      </div>
    {:else if error}
      <div class="error">
        ❌ {error}
      </div>
    {:else if documents.length === 0}
      <div class="empty-state">
        <span class="empty-icon">📭</span>
        <h3>No documents found</h3>
        <p>Upload your first document to get started!</p>
      </div>
    {:else}
      <div class="documents-count">
        <span class="count-label">Total Documents:</span>
        <span class="count-value">{documents.length}</span>
      </div>

      <div class="documents-grid">
        {#each documents as document}
          <div class="document-card">
            <div class="document-icon">
              {getFileIcon(document.contentType)}
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
                <p><strong>User ID:</strong> {document.userId}</p>
                {#if document.tenantId}
                  <p><strong>Tenant ID:</strong> {document.tenantId}</p>
                {/if}
                <p><strong>Status:</strong> <span class="status status-{document.status?.toLowerCase() || 'unknown'}">{document.status || 'Unknown'}</span></p>
              </div>
            </div>
            <div class="document-actions">
              <button class="btn-download" on:click={() => handleDownload(document)}>
                ⬇️ Download
              </button>
              <button class="btn-delete" on:click={() => handleDelete(document.id)}>
                🗑️ Delete
              </button>
            </div>
          </div>
        {/each}
      </div>
    {/if}
  {/if}
</div>

<style>
  .documents-page {
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
    font-size: 2rem;
    color: #1e293b;
    margin: 0;
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

  .upload-form {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 2rem;
    margin-bottom: 2rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .upload-form h2 {
    margin-top: 0;
    color: #1e293b;
    font-size: 1.5rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #475569;
    font-size: 0.875rem;
  }

  .form-group input[type="text"],
  .form-group input[type="file"],
  .form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .form-group input:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
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
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
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

  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
    border: 1px solid #fcc;
    text-align: center;
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
    margin: 0;
    color: #64748b;
  }

  /* Documents Count */
  .documents-count {
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

  .documents-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .document-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
    transition: all 0.2s;
  }

  .document-card:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    border-color: #3b82f6;
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
    font-weight: 600;
  }

  .description {
    color: #64748b;
    margin-bottom: 1rem;
    font-size: 0.875rem;
    line-height: 1.5;
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
    padding: 0.25rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
  }

  .status-active,
  .status-approved {
    background-color: #dcfce7;
    color: #166534;
  }

  .status-archived {
    background-color: #f3f4f6;
    color: #4b5563;
  }

  .status-pending_review,
  .status-pending {
    background-color: #fef3c7;
    color: #92400e;
  }

  .status-rejected {
    background-color: #fee2e2;
    color: #991b1b;
  }

  .status-unknown {
    background-color: #f1f5f9;
    color: #64748b;
  }

  .document-actions {
    display: flex;
    gap: 0.5rem;
  }

  .btn-download, .btn-delete {
    flex: 1;
    padding: 0.75rem;
    border: none;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-download {
    background-color: #3b82f6;
    color: white;
  }

  .btn-download:hover {
    background-color: #2563eb;
  }

  .btn-delete {
    background-color: #dc2626;
    color: white;
  }

  .btn-delete:hover {
    background-color: #b91c1c;
  }

  @media (max-width: 768px) {
    .documents-page {
      padding: 1rem;
    }

    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }

    .documents-grid {
      grid-template-columns: 1fr;
    }
  }
</style>

