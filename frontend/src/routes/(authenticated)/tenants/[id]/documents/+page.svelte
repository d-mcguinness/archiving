<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { authHeaders, API_BASE } from '$lib/api';
  import { GET_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

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

      const response = await fetch(`${API_BASE}/api/documents?${params.toString()}`, {
        headers: { ...authHeaders() }
      });

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
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[{ label: 'Documents' }]}
    />

    <!-- Page Header -->
    <div class="page-header">
      <div class="header-content">
        <span class="eyebrow">Tenant workspace</span>
        <h1>📄 Documents</h1>
        {#if tenant}
          <div class="tenant-badge">
            <span class="tenant-icon">🏢</span>
            <span class="tenant-name">{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
      </div>
      <a href="/tenants/{data.tenantId}/documents/create" class="btn-create">+ Upload Document</a>
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
    color: var(--arc-ink);
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: var(--arc-muted);
    font-size: 1.125rem;
  }

  .redirect-message {
    color: var(--arc-link);
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
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
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    flex: 1;
  }

  .btn-create {
    padding: 0.75rem 1.5rem;
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6);
    transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
    white-space: nowrap;
  }

  .btn-create:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .page-header h1 {
    margin: 0;
    color: var(--arc-ink);
    font-size: 2rem;
  }

  .tenant-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.35rem 0.9rem;
    background: var(--arc-chip-indigo-bg);
    color: var(--arc-chip-indigo-ink);
    border-radius: 9999px;
    font-weight: 600;
    font-size: 0.9rem;
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
    border: 4px solid var(--arc-line-strong);
    border-top: 4px solid #6366f1;
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
    background: var(--arc-alert-red-bg);
    color: var(--arc-alert-red-ink);
    padding: 1rem;
    border-radius: 0.6rem;
    border: 1px solid var(--arc-alert-red-border);
    margin-bottom: 1.5rem;
  }

  /* Documents Section */
  .documents-section {
    background: var(--arc-card);
    padding: 2rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line);
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid var(--arc-line);
  }

  .section-header h2 {
    margin: 0;
    color: var(--arc-ink);
    font-size: 1.5rem;
  }

  .document-count {
    color: var(--arc-muted);
    font-weight: 700;
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }

  /* Empty State — dark hero panel */
  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-grad-dark, radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%));
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
  }

  .empty-icon {
    font-size: 5rem;
    display: block;
    margin-bottom: 1rem;
  }

  .empty-state h3 {
    margin: 0 0 0.5rem 0;
    color: #f8fafc;
  }

  .empty-state p {
    margin: 0;
    color: #cbd5e1;
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
    background: var(--arc-card);
    padding: 1.5rem;
    border-radius: 1rem;
    border: 1px solid var(--arc-line);
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  }

  .document-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  @media (prefers-reduced-motion: reduce) {
    .document-card,
    .btn-create {
      transition: none;
    }
    .document-card:hover,
    .btn-create:hover {
      transform: none;
    }
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
    color: var(--arc-ink);
    font-size: 1.125rem;
    font-weight: 600;
  }

  .document-description {
    margin: 0;
    color: var(--arc-muted);
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
    color: var(--arc-muted);
    font-weight: 500;
  }

  .meta-value {
    color: var(--arc-ink);
    font-weight: 400;
  }

  .status {
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .status-active,
  .status-approved {
    background: var(--arc-chip-green-bg);
    color: var(--arc-chip-green-ink);
  }

  .status-pending,
  .status-pending_review {
    background: var(--arc-chip-amber-bg);
    color: var(--arc-chip-amber-ink);
  }

  .status-rejected {
    background: var(--arc-chip-red-bg);
    color: var(--arc-chip-red-ink);
  }

  .status-unknown {
    background: var(--arc-chip-slate-bg);
    color: var(--arc-chip-slate-ink);
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

