<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_TENANTS, UPDATE_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

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
  let hasAccess = false;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId;

    // Only ADMIN can access the tenants list page
    if (currentRole !== 'ADMIN') {
      hasAccess = false;
      loading = false;

      // Redirect TENANT users to their tenant page
      if (currentRole === 'TENANT' && tenantId) {
        goto(`/tenants/${tenantId}`);
      } else if (currentRole === 'USER' && tenantId) {
        goto(`/tenants/${tenantId}/users`);
      } else {
        goto('/');
      }
      return;
    }

    hasAccess = true;
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
            tenantId: editingTenant.id,
            name: editForm.name.trim(),
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

  function mimicTenant(tenant: any) {
    const token = 'Bearer_mimic_' + tenant.ownerId + '_TENANT_' + Date.now();
    const mimicData = {
      id: tenant.ownerId,
      username: (tenant.name || 'tenant').toLowerCase().replace(/\s+/g, ''),
      name: tenant.displayName || tenant.name,
      email: `${(tenant.name || 'tenant').toLowerCase().replace(/\s+/g, '')}@${tenant.domain || 'archiving.com'}`,
      role: 'TENANT',
      tenantId: parseInt(tenant.id, 10),
    };
    auth.login(token, mimicData, 'TENANT', parseInt(tenant.id, 10));
    toasts.success(`Now viewing as tenant: ${tenant.displayName || tenant.name}`);
    goto(`/tenants/${tenant.id}/users`);
  }
</script>

<svelte:head>
  <title>Tenants - Archiving System</title>
</svelte:head>

<div class="tenants-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to access the tenants list.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Tenants' }]} />
    <div class="page-header">
      <div class="page-heading">
        <span class="eyebrow">Admin console</span>
        <h1>Tenants</h1>
      </div>
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
                <button
                  class="btn-action btn-mimic"
                  on:click={() => mimicTenant(tenant)}
                  title="Sign in as this tenant"
                >
                  🎭 Mimic
                </button>
                <button
                  class="btn-action btn-edit"
                  on:click={() => openEditModal(tenant)}
                  title="Edit tenant"
                >
                  ✏️ Edit
                </button>
                <a href="/tenants/delete?tenantId={tenant.id}" class="btn-action btn-delete">
                  🗑️ Delete
                </a>
                <a href="/tenants/{tenant.id}/users" class="btn-action btn-users">
                  👥 Users
                </a>
                <a href="/tenants/{tenant.id}/archives" class="btn-action btn-archives">
                  📁 Archives
                </a>
                <a href="/tenants/{tenant.id}/intakes" class="btn-action btn-sips">
                  📦 Intakes
                </a>
                <a href="/tenants/{tenant.id}/preservations" class="btn-action btn-aips">
                  🏗️ Preservations
                </a>
                <a href="/tenants/{tenant.id}/releases" class="btn-action btn-dips">
                  📤 Releases
                </a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
    {/if}
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
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  .add-tenant-btn {
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    transition: all 0.2s ease;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
  }

  .add-tenant-btn:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
    box-shadow: var(--arc-shadow-btn-hover, 0 16px 40px -8px rgba(124, 58, 237, 0.75));
  }

  .error {
    background: var(--arc-alert-red-bg, #fef2f2);
    color: var(--arc-alert-red-ink, #991b1b);
    padding: 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    border: 1px solid var(--arc-alert-red-border, #fecaca);
  }

  .loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
  }

  .spinner {
    border: 4px solid var(--arc-line-strong, #e2e8f0);
    border-top: 4px solid var(--arc-indigo, #6366f1);
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
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    color: var(--arc-muted, #64748b);
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
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: var(--arc-muted, #64748b);
    font-size: 1.125rem;
  }

  .redirect-message {
    color: var(--arc-indigo, #6366f1);
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  /* Table Styles */
  .table-container {
    background: var(--arc-card, #fff);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    overflow-x: auto;
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 1200px;
  }

  .data-table thead {
    background: var(--arc-card-2, #f8fafc);
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 700;
    color: var(--arc-muted, #64748b);
    font-size: 0.72rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    white-space: nowrap;
  }

  .data-table tbody tr {
    border-bottom: 1px solid var(--arc-line, #e8edf3);
    transition: background-color 0.15s;
  }

  .data-table tbody tr:hover {
    background: var(--arc-card-2, #f8fafc);
  }

  .data-table tbody tr:last-child {
    border-bottom: none;
  }

  .data-table td {
    padding: 1rem;
    color: var(--arc-ink, #0f172a);
  }

  .id-cell {
    font-family: 'Monaco', 'Courier New', monospace;
    color: var(--arc-muted, #64748b);
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
    color: var(--arc-ink, #1e293b);
  }

  .tenant-description {
    font-size: 0.875rem;
    color: var(--arc-muted, #64748b);
    line-height: 1.4;
  }

  .domain-cell {
    color: var(--arc-muted, #64748b);
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
    border-radius: 9999px;
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .status-active {
    background: var(--arc-chip-green-bg, #dcfce7);
    color: var(--arc-chip-green-ink, #166534);
  }

  .status-inactive {
    background: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #475569);
  }

  .status-suspended {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #991b1b);
  }

  .status-trial {
    background: #dbeafe;
    color: #1e40af;
  }

  .status-pending {
    background: var(--arc-chip-amber-bg, #fef3c7);
    color: var(--arc-chip-amber-ink, #92400e);
  }

  .status-expired {
    background: var(--arc-chip-red-bg, #fecaca);
    color: var(--arc-chip-red-ink, #7f1d1d);
  }

  .plan-free {
    background: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #475569);
  }

  .plan-basic {
    background: #dbeafe;
    color: #1e40af;
  }

  .plan-professional {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
  }

  .plan-enterprise {
    background: var(--arc-chip-pink-bg, #fce7f3);
    color: var(--arc-chip-pink-ink, #9f1239);
  }

  .plan-custom {
    background: var(--arc-chip-violet-bg, #f3e8ff);
    color: var(--arc-chip-violet-ink, #6b21a8);
  }

  .owner-cell {
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
  }

  .date-cell {
    color: var(--arc-muted, #64748b);
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
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 600;
    transition: all 0.2s ease;
    margin-left: 0.5rem;
    border: none;
    cursor: pointer;
    text-decoration: none;
    box-shadow: none;
  }

  .btn-mimic {
    background: var(--arc-chip-violet-bg, #ede9fe);
    color: var(--arc-chip-violet-ink, #5b21b6);
  }

  .btn-mimic:hover {
    background: var(--arc-chip-violet-hover, #ddd6fe);
  }

  .btn-edit {
    background: var(--arc-card, #fff);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    color: var(--arc-ink, #1e293b);
  }

  .btn-edit:hover {
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-link, #4f46e5);
    background: var(--arc-card, #fff);
  }

  .btn-users {
    background: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #334155);
  }

  .btn-users:hover {
    background: var(--arc-chip-slate-hover, #e2e8f0);
  }

  .btn-archives {
    background: var(--arc-chip-cyan-bg, #cffafe);
    color: var(--arc-chip-cyan-ink, #155e75);
  }

  .btn-archives:hover {
    background: var(--arc-chip-cyan-hover, #a5f3fc);
  }

  .btn-sips {
    background: var(--arc-chip-pink-bg, #fce7f3);
    color: var(--arc-chip-pink-ink, #9d174d);
  }

  .btn-sips:hover {
    background: var(--arc-chip-pink-hover, #fbcfe8);
  }

  .btn-aips {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
  }

  .btn-aips:hover {
    background: var(--arc-chip-indigo-hover, #c7d2fe);
  }

  .btn-dips {
    background: var(--arc-chip-orange-bg, #ffedd5);
    color: var(--arc-chip-orange-ink, #9a3412);
  }

  .btn-dips:hover {
    background: var(--arc-chip-orange-hover, #fed7aa);
  }

  .btn-delete {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #b91c1c);
  }

  .btn-delete:hover {
    background: var(--arc-chip-red-hover, #fecaca);
  }

  /* Modal Styles */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--arc-overlay, rgba(15, 23, 42, 0.55));
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    padding: 1rem;
  }

  .modal-content {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
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
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .modal-header h2 {
    margin: 0;
    font-size: 1.5rem;
    color: var(--arc-ink, #0f172a);
  }

  .modal-close {
    background: none;
    border: none;
    box-shadow: none;
    font-size: 1.5rem;
    color: var(--arc-muted, #64748b);
    cursor: pointer;
    padding: 0.25rem;
    line-height: 1;
    transition: color 0.2s;
  }

  .modal-close:hover {
    color: var(--arc-ink, #1e293b);
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
    color: var(--arc-body, #475569);
    font-size: 0.875rem;
  }

  .form-group input:disabled,
  .form-group textarea:disabled,
  .form-group select:disabled {
    background: var(--arc-card-2, #f1f5f9);
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
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  .btn-primary,
  .btn-secondary {
    padding: 0.75rem 1.5rem;
    border-radius: 0.65rem;
    font-weight: 700;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s ease;
    border: none;
  }

  .btn-primary {
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
  }

  .btn-primary:hover:not(:disabled) {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .btn-primary:disabled {
    background: var(--arc-disabled-bg, #c7cdd8);
    color: var(--arc-disabled-ink, #fff);
    cursor: not-allowed;
    box-shadow: none;
  }

  .btn-secondary {
    background: var(--arc-card, #fff);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    color: var(--arc-ink, #1e293b);
    box-shadow: none;
  }

  .btn-secondary:hover:not(:disabled) {
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-link, #4f46e5);
    background: var(--arc-card, #fff);
  }

  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* No blue chip token exists — dark-theme override keeps the blue hue readable */
  :global(html[data-theme='dark']) .status-trial,
  :global(html[data-theme='dark']) .plan-basic {
    background: rgba(59, 130, 246, 0.18);
    color: #93c5fd;
  }
</style>


