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
      <a href="/tenants/create" class="add-tenant-btn btn-primary">Add Tenant</a>
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
      <div class="table-container table-card">
      <table class="data-table arc-table">
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
                  class="btn-action btn-mimic btn-chip violet"
                  on:click={() => mimicTenant(tenant)}
                  title="Sign in as this tenant"
                >
                  🎭 Mimic
                </button>
                <button
                  class="btn-action btn-edit btn-chip"
                  on:click={() => openEditModal(tenant)}
                  title="Edit tenant"
                >
                  ✏️ Edit
                </button>
                <a href="/tenants/delete?tenantId={tenant.id}" class="btn-action btn-delete btn-chip red">
                  🗑️ Delete
                </a>
                <a href="/tenants/{tenant.id}/users" class="btn-action btn-users btn-chip slate">
                  👥 Users
                </a>
                <a href="/tenants/{tenant.id}/archives" class="btn-action btn-archives btn-chip cyan">
                  📁 Archives
                </a>
                <a href="/tenants/{tenant.id}/intakes" class="btn-action btn-sips btn-chip pink">
                  📦 Intakes
                </a>
                <a href="/tenants/{tenant.id}/preservations" class="btn-action btn-aips btn-chip indigo">
                  🏗️ Preservations
                </a>
                <a href="/tenants/{tenant.id}/releases" class="btn-action btn-dips btn-chip orange">
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
    <div class="modal-content modal" on:click|stopPropagation>
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

  /* .add-tenant-btn, .error, .loading and .spinner use the global kit (app.css). */
  .loading {
    min-height: 400px;
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

  /* Table chrome comes from .table-card / table.arc-table; only the
     page-specific scroll + column sizing stays local. */
  .table-container {
    overflow-x: auto;
  }

  .data-table {
    min-width: 1200px;
  }

  .data-table th {
    white-space: nowrap;
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

  /* .badge base is global; these tenant status/plan hues are page-specific. */
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

  /* Row actions ride on .btn-chip; only the row spacing is local. */
  .btn-action {
    margin-left: 0.5rem;
  }

  /* Edit is an outlined chip rather than a tinted one. */
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

  /* Modal Styles — .modal-overlay/.modal come from app.css; this dialog
     is wider and pads its own header/body sections. */
  .modal {
    padding: 0;
    max-width: 600px;
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

  /* .btn-primary / .btn-secondary come from the global kit. */

  /* No blue chip token exists — dark-theme override keeps the blue hue readable */
  :global(html[data-theme='dark']) .status-trial,
  :global(html[data-theme='dark']) .plan-basic {
    background: rgba(59, 130, 246, 0.18);
    color: #93c5fd;
  }
</style>


