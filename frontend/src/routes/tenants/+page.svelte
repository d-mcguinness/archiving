<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_TENANTS } from '$lib/graphql/queries';

  let tenants: any[] = [];
  let loading: boolean = true;
  let error: string | null = null;

  onMount(async () => {
    await loadTenants();
  });

  async function loadTenants() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_TENANTS,
        fetchPolicy: 'network-only'
      });
      tenants = result.data.getAllTenants || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
    } finally {
      loading = false;
    }
  }

  function formatBytes(bytes: number | null | undefined): string {
    if (!bytes) return 'N/A';
    const gb = bytes / (1024 ** 3);
    return `${gb.toFixed(0)} GB`;
  }

  function getStatusBadgeClass(status: string): string {
    const classes: Record<string, string> = {
      'ACTIVE': 'status-active',
      'INACTIVE': 'status-inactive',
      'SUSPENDED': 'status-suspended',
      'TRIAL': 'status-trial',
      'PENDING_ACTIVATION': 'status-pending',
      'EXPIRED': 'status-expired'
    };
    return classes[status] || 'status-default';
  }

  function getPlanBadgeClass(plan: string): string {
    const classes: Record<string, string> = {
      'FREE': 'plan-free',
      'BASIC': 'plan-basic',
      'PROFESSIONAL': 'plan-professional',
      'ENTERPRISE': 'plan-enterprise',
      'CUSTOM': 'plan-custom'
    };
    return classes[plan] || 'plan-default';
  }
</script>

<svelte:head>
  <title>Tenants - Archiving System</title>
</svelte:head>

<div class="tenants-page">
  <div class="page-header">
    <h1>Tenants</h1>
    <a href="/tenants/create" class="add-tenant-btn">Add Tenant</a>
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
  {:else}
    <div class="tenants-grid">
      {#each tenants as tenant (tenant.id)}
        <div class="tenant-card">
          <div class="tenant-header">
            <h3>{tenant.displayName || tenant.name}</h3>
            <div class="badges">
              <span class="badge {getStatusBadgeClass(tenant.status)}">{tenant.status}</span>
              <span class="badge {getPlanBadgeClass(tenant.plan)}">{tenant.plan}</span>
            </div>
          </div>

          <p class="domain">{tenant.domain}</p>

          {#if tenant.description}
            <p class="description">{tenant.description}</p>
          {/if}

          <div class="tenant-details">
            <div class="detail-item">
              <span class="label">Owner:</span>
              <span class="value">{tenant.ownerId}</span>
            </div>
            {#if tenant.settings}
              <div class="detail-item">
                <span class="label">Max Users:</span>
                <span class="value">{tenant.settings.maxUsers || 'N/A'}</span>
              </div>
              <div class="detail-item">
                <span class="label">Max Archives:</span>
                <span class="value">{tenant.settings.maxArchives || 'N/A'}</span>
              </div>
              <div class="detail-item">
                <span class="label">Storage:</span>
                <span class="value">{formatBytes(tenant.settings.maxStorageBytes)}</span>
              </div>
              <div class="detail-item">
                <span class="label">Timezone:</span>
                <span class="value">{tenant.settings.timezone || 'N/A'}</span>
              </div>
            {/if}
          </div>

          <div class="tenant-id">ID: {tenant.id}</div>

          <div class="tenant-actions">
            <a href={`/tenants/update?tenantId=${tenant.id}`} class="update-btn">Update</a>
            <a href={`/tenants/delete?tenantId=${tenant.id}`} class="delete-btn">Delete</a>
          </div>
        </div>
      {:else}
        <div class="empty-state">
          <p>No tenants found. Create your first tenant to get started!</p>
        </div>
      {/each}
    </div>
  {/if}
</div>

<style>
  .tenants-page {
    max-width: 1200px;
    margin: 0 auto;
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
  }

  .add-tenant-btn {
    background: #3b82f6;
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    text-decoration: none;
    font-weight: 500;
    transition: background 0.2s;
  }

  .add-tenant-btn:hover {
    background: #2563eb;
  }

  .error {
    background: #fee;
    border: 1px solid #fcc;
    padding: 1rem;
    border-radius: 0.25rem;
    color: #c00;
    margin-bottom: 1rem;
  }

  .loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 200px;
  }

  .spinner {
    width: 40px;
    height: 40px;
    border: 4px solid #e2e8f0;
    border-top-color: #3b82f6;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .tenants-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .tenant-card {
    background: white;
    padding: 1.5rem;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .tenant-header {
    margin-bottom: 1rem;
  }

  .tenant-header h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
  }

  .badges {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .badge {
    padding: 0.25rem 0.75rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .status-active {
    background: #dcfce7;
    color: #166534;
  }

  .status-inactive {
    background: #f3f4f6;
    color: #6b7280;
  }

  .status-suspended {
    background: #fee2e2;
    color: #991b1b;
  }

  .status-trial {
    background: #dbeafe;
    color: #1e40af;
  }

  .status-pending {
    background: #fef3c7;
    color: #92400e;
  }

  .status-expired {
    background: #fecaca;
    color: #7f1d1d;
  }

  .plan-free {
    background: #f3f4f6;
    color: #374151;
  }

  .plan-basic {
    background: #dbeafe;
    color: #1e40af;
  }

  .plan-professional {
    background: #e0e7ff;
    color: #4338ca;
  }

  .plan-enterprise {
    background: #fce7f3;
    color: #9f1239;
  }

  .plan-custom {
    background: #f3e8ff;
    color: #6b21a8;
  }

  .domain {
    color: #3b82f6;
    margin: 0.5rem 0;
    font-weight: 500;
  }

  .description {
    color: #64748b;
    margin: 0.75rem 0;
    font-size: 0.875rem;
    line-height: 1.4;
  }

  .tenant-details {
    margin: 1rem 0;
    padding: 1rem;
    background: #f8fafc;
    border-radius: 0.25rem;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.5rem;
    font-size: 0.875rem;
  }

  .detail-item {
    display: flex;
    flex-direction: column;
  }

  .detail-item .label {
    color: #64748b;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .detail-item .value {
    color: #1e293b;
    font-weight: 500;
    margin-top: 0.25rem;
  }

  .tenant-id {
    font-size: 0.75rem;
    color: #9ca3af;
    margin-top: 1rem;
    font-family: monospace;
  }

  .tenant-actions {
    margin-top: 1rem;
    display: flex;
    gap: 1rem;
  }

  .update-btn,
  .delete-btn {
    padding: 0.5rem 1rem;
    border-radius: 0.25rem;
    text-decoration: none;
    font-weight: 500;
    transition: background 0.2s;
    flex: 1;
    text-align: center;
  }

  .update-btn {
    background: #4caf50;
    color: white;
  }

  .update-btn:hover {
    background: #388e3c;
  }

  .delete-btn {
    background: #f44336;
    color: white;
  }

  .delete-btn:hover {
    background: #c62828;
  }

  .empty-state {
    grid-column: 1 / -1;
    text-align: center;
    padding: 3rem;
    color: #64748b;
  }

  @media (max-width: 768px) {
    .tenants-grid {
      grid-template-columns: 1fr;
    }

    .tenant-details {
      grid-template-columns: 1fr;
    }
  }
</style>
