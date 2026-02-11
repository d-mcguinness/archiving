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
  {:else if tenants.length === 0}
    <div class="empty-state">
      <p>No tenants found. Create your first tenant to get started!</p>
    </div>
  {:else}
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Domain</th>
            <th>Status</th>
            <th>Plan</th>
            <th>Owner ID</th>
            <th>Created</th>
            <th>Updated</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {#each tenants as tenant (tenant.id)}
            <tr>
              <td class="id-cell">{tenant.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="tenant-title">{tenant.displayName || tenant.name}</div>
                  {#if tenant.description}
                    <div class="tenant-description">{tenant.description}</div>
                  {/if}
                </div>
              </td>
              <td class="domain-cell">{tenant.domain}</td>
              <td class="status-cell">
                <span class="badge {getStatusBadgeClass(tenant.status)}">{tenant.status}</span>
              </td>
              <td class="plan-cell">
                <span class="badge {getPlanBadgeClass(tenant.plan)}">{tenant.plan}</span>
              </td>
              <td class="owner-cell">{tenant.ownerId}</td>
              <td class="date-cell">{new Date(tenant.createdAt).toLocaleDateString()}</td>
              <td class="date-cell">{tenant.updatedAt ? new Date(tenant.updatedAt).toLocaleDateString() : '-'}</td>
              <td class="actions-cell">
                <a href="/archives?tenantId={tenant.id}" class="btn-action btn-archives">
                  📁 Archives
                </a>
                <a href="/tenants/update?tenantId={tenant.id}" class="btn-action btn-edit">
                  ✏️ Edit
                </a>
                <a href="/tenants/delete?tenantId={tenant.id}" class="btn-action btn-delete">
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

<style>
  .tenants-page {
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

  .add-tenant-btn {
    background: #3b82f6;
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.375rem;
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .add-tenant-btn:hover {
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
    min-width: 1200px;
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
    min-width: 250px;
    max-width: 350px;
  }

  .title-wrapper {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .tenant-title {
    font-weight: 500;
    color: #1e293b;
  }

  .tenant-description {
    font-size: 0.875rem;
    color: #64748b;
    line-height: 1.4;
  }

  .domain-cell {
    color: #64748b;
    font-size: 0.875rem;
    font-family: 'Monaco', 'Courier New', monospace;
  }

  .status-cell,
  .plan-cell {
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

  .owner-cell {
    color: #64748b;
    font-size: 0.875rem;
  }

  .date-cell {
    color: #64748b;
    font-size: 0.875rem;
    white-space: nowrap;
    width: 120px;
  }

  .actions-cell {
    text-align: right;
    white-space: nowrap;
    width: 350px;
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

  .btn-archives {
    background: #8b5cf6;
    color: white;
  }

  .btn-archives:hover {
    background: #7c3aed;
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
</style>


