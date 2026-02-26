<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

  let tenant: any = null;
  let loading = true;
  let error: string | null = null;
  let tenantId: string;

  $: tenantId = $page.params.id || '';

  onMount(async () => {
    if (tenantId) {
      await loadTenant();
    }
  });

  async function loadTenant() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_TENANT,
        variables: { id: tenantId },
        fetchPolicy: 'network-only'
      });

      tenant = result?.data?.getTenant || null;
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load tenant error:', e);
      toasts.error('Failed to load tenant details');
    } finally {
      loading = false;
    }
  }

  function getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return '#10b981';
      case 'INACTIVE': return '#6b7280';
      case 'SUSPENDED': return '#ef4444';
      case 'TRIAL': return '#3b82f6';
      case 'PENDING_ACTIVATION': return '#f59e0b';
      default: return '#64748b';
    }
  }

  function getPlanColor(plan: string): string {
    switch (plan) {
      case 'ENTERPRISE': return '#8b5cf6';
      case 'PROFESSIONAL': return '#6366f1';
      case 'BASIC': return '#3b82f6';
      case 'FREE': return '#9ca3af';
      default: return '#64748b';
    }
  }

  function formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
</script>

<svelte:head>
  <title>{tenant ? tenant.displayName || tenant.name : 'Tenant'} - Archiving System</title>
</svelte:head>

<div class="tenant-detail-page">
  <div class="page-header">
    <h1>Tenant Details</h1>
  </div>

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading tenant details...</p>
    </div>
  {:else if error}
    <div class="error">
      Error: {error}
    </div>
  {:else if tenant}
    <div class="tenant-info">
      <!-- Tenant Header Card -->
      <div class="tenant-header-card">
        <div class="tenant-icon">🏢</div>
        <div class="tenant-header-content">
          <h2>{tenant.displayName || tenant.name}</h2>
          <p class="tenant-domain">{tenant.domain}</p>
          <div class="tenant-badges">
            <span class="badge badge-status" style="background-color: {getStatusColor(tenant.status)}">
              {tenant.status}
            </span>
            <span class="badge badge-plan" style="background-color: {getPlanColor(tenant.plan)}">
              {tenant.plan}
            </span>
          </div>
        </div>
      </div>

      <!-- Tenant Information -->
      <div class="info-section">
        <h3>📋 General Information</h3>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">Name:</span>
            <span class="info-value">{tenant.name}</span>
          </div>
          {#if tenant.displayName}
            <div class="info-item">
              <span class="info-label">Display Name:</span>
              <span class="info-value">{tenant.displayName}</span>
            </div>
          {/if}
          <div class="info-item">
            <span class="info-label">Domain:</span>
            <span class="info-value">{tenant.domain}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Owner ID:</span>
            <span class="info-value">{tenant.ownerId}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Created:</span>
            <span class="info-value">{formatDate(tenant.createdAt)}</span>
          </div>
          {#if tenant.updatedAt}
            <div class="info-item">
              <span class="info-label">Last Updated:</span>
              <span class="info-value">{formatDate(tenant.updatedAt)}</span>
            </div>
          {/if}
        </div>
        {#if tenant.description}
          <div class="description">
            <span class="info-label">Description:</span>
            <p>{tenant.description}</p>
          </div>
        {/if}
      </div>

      <!-- Tenant Settings -->
      {#if tenant.settings}
        <div class="info-section">
          <h3>⚙️ Settings</h3>
          <div class="info-grid">
            {#if tenant.settings.maxUsers}
              <div class="info-item">
                <span class="info-label">Max Users:</span>
                <span class="info-value">{tenant.settings.maxUsers}</span>
              </div>
            {/if}
            {#if tenant.settings.maxArchives}
              <div class="info-item">
                <span class="info-label">Max Archives:</span>
                <span class="info-value">{tenant.settings.maxArchives}</span>
              </div>
            {/if}
            {#if tenant.settings.maxStorageBytes}
              <div class="info-item">
                <span class="info-label">Max Storage:</span>
                <span class="info-value">{tenant.settings.maxStorageBytes}</span>
              </div>
            {/if}
            {#if tenant.settings.timezone}
              <div class="info-item">
                <span class="info-label">Timezone:</span>
                <span class="info-value">{tenant.settings.timezone}</span>
              </div>
            {/if}
            {#if tenant.settings.defaultLanguage}
              <div class="info-item">
                <span class="info-label">Default Language:</span>
                <span class="info-value">{tenant.settings.defaultLanguage}</span>
              </div>
            {/if}
            {#if tenant.settings.customDomain}
              <div class="info-item">
                <span class="info-label">Custom Domain:</span>
                <span class="info-value">{tenant.settings.customDomain}</span>
              </div>
            {/if}
            <div class="info-item">
              <span class="info-label">External Sharing:</span>
              <span class="info-value">{tenant.settings.allowExternalSharing ? '✅ Enabled' : '❌ Disabled'}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Audit Log:</span>
              <span class="info-value">{tenant.settings.enableAuditLog ? '✅ Enabled' : '❌ Disabled'}</span>
            </div>
          </div>
        </div>
      {/if}

      <!-- Quick Actions -->
      <div class="actions-section">
        <h3>🚀 Quick Actions</h3>
        <div class="action-buttons">
          <a href="/tenants/{tenantId}/users" class="action-btn">
            <span class="action-icon">👥</span>
            <span class="action-text">View Users</span>
          </a>
          <a href="/tenants/{tenantId}/archives" class="action-btn">
            <span class="action-icon">📁</span>
            <span class="action-text">View Archives</span>
          </a>
          <a href="/tenants/update?tenantId={tenantId}" class="action-btn action-secondary">
            <span class="action-icon">✏️</span>
            <span class="action-text">Edit Tenant</span>
          </a>
        </div>
      </div>
    </div>
  {:else}
    <div class="empty-state">
      <span class="empty-icon">🏢</span>
      <h3>Tenant Not Found</h3>
      <p>The requested tenant could not be found.</p>
      <a href="/admin/tenants" class="btn-back">Back to Tenants</a>
    </div>
  {/if}
</div>

<style>
  .tenant-detail-page {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    margin-bottom: 2rem;
  }


  .page-header h1 {
    margin: 0;
    color: #1e293b;
    font-size: 2rem;
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
    border: 1px solid #fcc;
  }

  .tenant-info {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .tenant-header-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 2rem;
    border-radius: 0.75rem;
    display: flex;
    align-items: center;
    gap: 1.5rem;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .tenant-icon {
    font-size: 4rem;
  }

  .tenant-header-content {
    flex: 1;
  }

  .tenant-header-content h2 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
  }

  .tenant-domain {
    margin: 0 0 1rem 0;
    opacity: 0.9;
    font-size: 1.125rem;
  }

  .tenant-badges {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .badge {
    padding: 0.375rem 0.875rem;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
    color: white;
  }

  .info-section {
    background: white;
    padding: 1.5rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .info-section h3 {
    margin: 0 0 1.5rem 0;
    color: #1e293b;
    font-size: 1.25rem;
  }

  .info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.25rem;
  }

  .info-item {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .info-label {
    color: #64748b;
    font-size: 0.875rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .info-value {
    color: #1e293b;
    font-size: 1rem;
    font-weight: 500;
  }

  .description {
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .description p {
    margin: 0.5rem 0 0 0;
    color: #475569;
    line-height: 1.6;
  }

  .actions-section {
    background: white;
    padding: 1.5rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .actions-section h3 {
    margin: 0 0 1.5rem 0;
    color: #1e293b;
    font-size: 1.25rem;
  }

  .action-buttons {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
  }

  .action-btn {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 1rem 1.5rem;
    background: #3b82f6;
    color: white;
    text-decoration: none;
    border-radius: 0.5rem;
    font-weight: 600;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .action-btn:hover {
    background: #2563eb;
    transform: translateY(-2px);
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .action-btn.action-secondary {
    background: #64748b;
  }

  .action-btn.action-secondary:hover {
    background: #475569;
  }

  .action-icon {
    font-size: 1.5rem;
  }

  .action-text {
    flex: 1;
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
    margin-bottom: 1rem;
    display: block;
  }

  .empty-state h3 {
    margin: 0 0 1rem 0;
    color: #1e293b;
  }

  .empty-state p {
    margin: 0 0 1.5rem 0;
    color: #64748b;
  }

  .btn-back {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background: #3b82f6;
    color: white;
    text-decoration: none;
    border-radius: 0.5rem;
    font-weight: 600;
    transition: background 0.2s;
  }

  .btn-back:hover {
    background: #2563eb;
  }

  @media (max-width: 768px) {
    .tenant-detail-page {
      padding: 1rem;
    }

    .tenant-header-card {
      flex-direction: column;
      text-align: center;
    }

    .info-grid {
      grid-template-columns: 1fr;
    }

    .action-buttons {
      grid-template-columns: 1fr;
    }
  }
</style>

