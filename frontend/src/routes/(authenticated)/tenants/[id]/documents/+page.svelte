<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';

  interface PageData {
    tenantId: string;
  }

  export let data: PageData;

  let documents: any[] = [];
  let tenant: any = null;
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

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
      // USER should not access tenant documents page
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

    await Promise.all([loadTenant(), loadDocuments()]);
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

  async function loadDocuments() {
    try {
      loading = true;

      // Construct query parameters
      const params = new URLSearchParams();
      params.append('role', currentRole);
      params.append('tenantId', data.tenantId);

      const response = await fetch(`http://localhost:2020/api/documents?${params.toString()}`);

      if (!response.ok) {
        throw new Error('Failed to load documents');
      }

      const result = await response.json();
      if (result.success) {
        documents = result.documents || [];
      } else {
        throw new Error(result.message || 'Failed to load documents');
      }

      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load documents';
      console.error('Load documents error:', e);
      toasts.error('Failed to load documents');
    } finally {
      loading = false;
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

  function getUserName(userId: string | number): string {
    // In a real implementation, you would look up the user name
    // For now, just return the user ID
    return `User ${userId}`;
  }
</script>

<svelte:head>
  <title>{tenant ? `${tenant.displayName || tenant.name} - ` : ''}Documents - Archiving System</title>
</svelte:head>

<div class="tenant-documents-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to view these documents.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <a href="/tenants/{data.tenantId}">← Back to Tenant</a>
    </div>

    <!-- Page Header -->
    <div class="page-header">
      <div class="header-content">
        <h1>📄 Documents</h1>
        {#if tenant}
          <div class="tenant-badge">
            <span class="tenant-icon">🏢</span>
            <span class="tenant-name">{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
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
        <p>Loading documents...</p>
      </div>
    {:else}
      <div class="documents-section">
        <div class="section-header">
          <h2>All Documents</h2>
          <span class="document-count">
            {documents.length} document{documents.length !== 1 ? 's' : ''}
          </span>
        </div>

        {#if documents.length === 0}
          <div class="empty-state">
            <span class="empty-icon">📭</span>
            <h3>No documents yet</h3>
            <p>This tenant doesn't have any documents uploaded yet.</p>
          </div>
        {:else}
          <div class="documents-grid">
            {#each documents as document}
              <a href="/tenants/{data.tenantId}/users/{document.userId}/documents/{document.id}" class="document-card-link">
                <div class="document-card">
                  <div class="document-icon-large">
                    {getFileIcon(document.contentType)}
                  </div>
                  <div class="document-info">
                    <h3 class="document-title">{document.title}</h3>
                    {#if document.description}
                      <p class="document-description">{document.description}</p>
                    {/if}
                    <div class="document-meta">
                      <div class="meta-item">
                        <span class="meta-label">Uploaded by:</span>
                        <span class="meta-value">{getUserName(document.userId)}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">File:</span>
                        <span class="meta-value">{document.fileName}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Size:</span>
                        <span class="meta-value">{formatFileSize(document.fileSize)}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Type:</span>
                        <span class="meta-value">{document.contentType || 'Unknown'}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Uploaded:</span>
                        <span class="meta-value">{formatDate(document.uploadedAt)}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Status:</span>
                        <span class="status status-{document.status?.toLowerCase() || 'unknown'}">
                          {document.status || 'Unknown'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </a>
            {/each}
          </div>
        {/if}
      </div>
    {/if}
  {/if}
</div>

<style>
  .tenant-documents-page {
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
    margin-bottom: 1.5rem;
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

  /* Page Header */
  .page-header {
    margin-bottom: 2rem;
  }

  .header-content {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .page-header h1 {
    margin: 0;
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
    width: fit-content;
  }

  .tenant-icon {
    font-size: 1.25rem;
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

  /* Documents Section */
  .documents-section {
    background: white;
    padding: 2rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid #e2e8f0;
  }

  .section-header h2 {
    margin: 0;
    color: #1e293b;
    font-size: 1.5rem;
  }

  .document-count {
    color: #64748b;
    font-weight: 600;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  /* Empty State */
  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
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

  /* Documents Grid */
  .documents-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .document-card-link {
    text-decoration: none;
    color: inherit;
    display: block;
  }

  .document-card {
    background: #f8fafc;
    padding: 1.5rem;
    border-radius: 0.75rem;
    border: 2px solid #e2e8f0;
    transition: all 0.2s;
  }

  .document-card:hover {
    border-color: #3b82f6;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  }

  .document-icon-large {
    font-size: 3rem;
    text-align: center;
    margin-bottom: 1rem;
  }

  .document-info {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .document-title {
    margin: 0;
    color: #1e293b;
    font-size: 1.125rem;
    font-weight: 600;
  }

  .document-description {
    margin: 0;
    color: #64748b;
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .document-meta {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-top: 0.5rem;
  }

  .meta-item {
    display: flex;
    justify-content: space-between;
    font-size: 0.875rem;
  }

  .meta-label {
    color: #64748b;
    font-weight: 500;
  }

  .meta-value {
    color: #1e293b;
    font-weight: 400;
  }

  .status {
    padding: 0.25rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .status-active,
  .status-approved {
    background: #dcfce7;
    color: #166534;
  }

  .status-pending,
  .status-pending_review {
    background: #fef3c7;
    color: #92400e;
  }

  .status-rejected {
    background: #fee2e2;
    color: #991b1b;
  }

  .status-unknown {
    background: #f1f5f9;
    color: #64748b;
  }

  @media (max-width: 768px) {
    .tenant-documents-page {
      padding: 1rem;
    }

    .documents-grid {
      grid-template-columns: 1fr;
    }
  }
</style>

