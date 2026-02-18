<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_TENANTS, UPDATE_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

  let tenants: any[] = [];
  let loading: boolean = true;
  let error: string | null = null;

  // Edit modal state
  let showEditModal = false;
  let editingTenant: any = null;
  let editForm = {
    name: '',
    domain: '',
    displayName: '',
    description: '',
    status: '',
    plan: ''
  };
  let saving = false;

  // Current user role for permissions
  let currentRole = '';

  onMount(async () => {
    // Get user role from localStorage
    const role = localStorage.getItem('auth_role');
    currentRole = role || '';

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

  function openEditModal(tenant: any) {
    editingTenant = tenant;
    editForm = {
      name: tenant.name,
      domain: tenant.domain,
      displayName: tenant.displayName || '',
      description: tenant.description || '',
      status: tenant.status,
      plan: tenant.plan
    };
    showEditModal = true;
  }

  function closeEditModal() {
    showEditModal = false;
    editingTenant = null;
    editForm = {
      name: '',
      domain: '',
      displayName: '',
      description: '',
      status: '',
      plan: ''
    };
  }

  async function handleSaveEdit() {
    if (!editingTenant) return;

    // Validate
    if (!editForm.name.trim()) {
      toasts.error('Name is required');
      return;
    }
    if (!editForm.domain.trim()) {
      toasts.error('Domain is required');
      return;
    }

    saving = true;
    try {
      const result = await client.mutate({
        mutation: UPDATE_TENANT,
        variables: {
          input: {
            id: editingTenant.id,
            name: editForm.name.trim(),
            domain: editForm.domain.trim(),
            displayName: editForm.displayName.trim() || null,
            description: editForm.description.trim() || null,
            status: editForm.status,
            plan: editForm.plan
          }
        }
      });

      if (result.data?.updateTenant) {
        toasts.success('Tenant updated successfully');
        closeEditModal();
        await loadTenants(); // Reload the list
      }
    } catch (e) {
      console.error('Update tenant error:', e);
      toasts.error(e instanceof Error ? e.message : 'Failed to update tenant');
    } finally {
      saving = false;
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
                {#if currentRole === 'ADMIN'}
                  <a href="/tenants/delete?tenantId={tenant.id}" class="btn-action btn-delete">
                    🗑️ Delete
                  </a>
                {/if}
                {#if currentRole === 'ADMIN' || currentRole === 'TENANT'}
                  <button
                    class="btn-action btn-edit"
                    on:click={() => openEditModal(tenant)}
                    title="Edit tenant"
                  >
                    ✏️ Edit
                  </button>
                {/if}
                <a href="/tenants/{tenant.id}/users" class="btn-action btn-users">
                  👥 View Users
                </a>
                <a href="/archives?tenantId={tenant.id}" class="btn-action btn-archives">
                  📁 View Archives
                </a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

<!-- Edit Modal -->
{#if showEditModal && editingTenant}
  <div class="modal-overlay" on:click={closeEditModal}>
    <div class="modal-content" on:click|stopPropagation>
      <div class="modal-header">
        <h2>Edit Tenant</h2>
        <button class="modal-close" on:click={closeEditModal}>✕</button>
      </div>

      <form on:submit|preventDefault={handleSaveEdit}>
        <div class="form-group">
          <label for="edit-name">Name *</label>
          <input
            id="edit-name"
            type="text"
            bind:value={editForm.name}
            placeholder="Enter tenant name"
            required
            disabled={saving}
          />
        </div>

        <div class="form-group">
          <label for="edit-domain">Domain *</label>
          <input
            id="edit-domain"
            type="text"
            bind:value={editForm.domain}
            placeholder="Enter domain (e.g., acme.example.com)"
            required
            disabled={saving}
          />
        </div>

        <div class="form-group">
          <label for="edit-displayName">Display Name</label>
          <input
            id="edit-displayName"
            type="text"
            bind:value={editForm.displayName}
            placeholder="Enter display name"
            disabled={saving}
          />
        </div>

        <div class="form-group">
          <label for="edit-description">Description</label>
          <textarea
            id="edit-description"
            bind:value={editForm.description}
            placeholder="Enter description"
            rows="3"
            disabled={saving}
          ></textarea>
        </div>

        <div class="form-group">
          <label for="edit-status">Status *</label>
          <select
            id="edit-status"
            bind:value={editForm.status}
            required
            disabled={saving}
          >
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
            <option value="SUSPENDED">Suspended</option>
            <option value="TRIAL">Trial</option>
            <option value="PENDING_ACTIVATION">Pending Activation</option>
            <option value="EXPIRED">Expired</option>
          </select>
        </div>

        <div class="form-group">
          <label for="edit-plan">Plan *</label>
          <select
            id="edit-plan"
            bind:value={editForm.plan}
            required
            disabled={saving}
          >
            <option value="FREE">Free</option>
            <option value="BASIC">Basic</option>
            <option value="PROFESSIONAL">Professional</option>
            <option value="ENTERPRISE">Enterprise</option>
            <option value="CUSTOM">Custom</option>
          </select>
        </div>

        <div class="modal-actions">
          <button
            type="button"
            class="btn-secondary"
            on:click={closeEditModal}
            disabled={saving}
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            disabled={saving}
          >
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}

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
    width: 280px;
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

  .btn-edit {
    background: #3b82f6;
    color: white;
  }

  .btn-edit:hover {
    background: #2563eb;
  }

  .btn-users {
    background: #f59e0b;
    color: white;
  }

  .btn-users:hover {
    background: #d97706;
  }

  .btn-archives {
    background: #8b5cf6;
    color: white;
  }

  .btn-archives:hover {
    background: #7c3aed;
  }

  .btn-delete {
    background: #dc2626;
    color: white;
  }

  .btn-delete:hover {
    background: #b91c1c;
  }

  /* Modal Styles */
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
    padding: 1rem;
  }

  .modal-content {
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
    max-width: 600px;
    width: 100%;
    max-height: 90vh;
    overflow-y: auto;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .modal-header h2 {
    margin: 0;
    font-size: 1.5rem;
    color: #1e293b;
  }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: #64748b;
    cursor: pointer;
    padding: 0.25rem;
    line-height: 1;
    transition: color 0.2s;
  }

  .modal-close:hover {
    color: #1e293b;
  }

  .modal-content form {
    padding: 1.5rem;
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

  .form-group input,
  .form-group textarea,
  .form-group select {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 1rem;
    transition: border-color 0.2s;
    font-family: inherit;
  }

  .form-group input:focus,
  .form-group textarea:focus,
  .form-group select:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .form-group input:disabled,
  .form-group textarea:disabled,
  .form-group select:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .form-group textarea {
    resize: vertical;
    min-height: 80px;
  }

  .modal-actions {
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
    padding-top: 1rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn-primary,
  .btn-secondary {
    padding: 0.75rem 1.5rem;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
    border: none;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-primary:disabled {
    background: #94a3b8;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #cbd5e1;
  }

  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
</style>


