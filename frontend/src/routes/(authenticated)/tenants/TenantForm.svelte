<script>
  import { createEventDispatcher } from 'svelte';
  import { client } from '../../lib/apollo';
  import { CREATE_TENANT, UPDATE_TENANT } from '../../lib/graphql/queries';

  export let tenant = null; // null for create, tenant object for edit

  const dispatch = createEventDispatcher();

  // Form state
  let form = {
    name: '',
    domain: '',
    displayName: '',
    description: '',
    ownerId: '',
    plan: 'FREE',
    status: 'ACTIVE'
  };

  let loading = false;
  let error = null;
  let validationErrors = {};

  // Plan and status options
  const planOptions = [
    { value: 'FREE', label: 'Free' },
    { value: 'BASIC', label: 'Basic' },
    { value: 'PROFESSIONAL', label: 'Professional' },
    { value: 'ENTERPRISE', label: 'Enterprise' },
    { value: 'CUSTOM', label: 'Custom' }
  ];

  const statusOptions = [
    { value: 'ACTIVE', label: 'Active' },
    { value: 'INACTIVE', label: 'Inactive' },
    { value: 'SUSPENDED', label: 'Suspended' },
    { value: 'PENDING_ACTIVATION', label: 'Pending Activation' },
    { value: 'TRIAL', label: 'Trial' },
    { value: 'EXPIRED', label: 'Expired' }
  ];

  // Initialize form for editing
  $: if (tenant) {
    form = {
      name: tenant.name || '',
      domain: tenant.domain || '',
      displayName: tenant.displayName || '',
      description: tenant.description || '',
      ownerId: tenant.ownerId || '',
      plan: tenant.plan || 'FREE',
      status: tenant.status || 'ACTIVE'
    };
  }

  function validateForm() {
    validationErrors = {};

    if (!form.name.trim()) {
      validationErrors.name = 'Name is required';
    }

    if (!form.domain.trim()) {
      validationErrors.domain = 'Domain is required';
    } else if (!/^[a-z0-9-]+$/.test(form.domain)) {
      validationErrors.domain = 'Domain must contain only lowercase letters, numbers, and hyphens';
    }

    if (!form.displayName.trim()) {
      validationErrors.displayName = 'Display name is required';
    }

    if (!form.ownerId.trim()) {
      validationErrors.ownerId = 'Owner ID is required';
    }

    return Object.keys(validationErrors).length === 0;
  }

  async function handleSubmit() {
    if (!validateForm()) {
      return;
    }

    try {
      loading = true;
      error = null;

      if (tenant) {
        // Update existing tenant
        const updateInput = {
          tenantId: tenant.id,
          name: form.name,
          displayName: form.displayName,
          description: form.description || null,
          status: form.status,
          plan: form.plan
        };

        const result = await client.mutate({
          mutation: UPDATE_TENANT,
          variables: { input: updateInput }
        });

        dispatch('saved', result.data.updateTenant);
      } else {
        // Create new tenant
        const createInput = {
          name: form.name,
          domain: form.domain,
          displayName: form.displayName,
          description: form.description || null,
          ownerId: form.ownerId,
          plan: form.plan
        };

        const result = await client.mutate({
          mutation: CREATE_TENANT,
          variables: { input: createInput }
        });

        dispatch('saved', result.data.createTenant);
      }

      // Reset form for create mode
      if (!tenant) {
        form = {
          name: '',
          domain: '',
          displayName: '',
          description: '',
          ownerId: '',
          plan: 'FREE',
          status: 'ACTIVE'
        };
      }

    } catch (err) {
      error = err.message;
      console.error('Error saving tenant:', err);
    } finally {
      loading = false;
    }
  }

  function handleCancel() {
    dispatch('cancelled');
  }
</script>

<div class="tenant-form">
  {#if error}
    <div class="alert alert-error">
      {error}
      <button on:click={() => error = null}>×</button>
    </div>
  {/if}

  <form on:submit|preventDefault={handleSubmit}>
    <div class="form-grid">
      <div class="form-group">
        <label for="name">Name *</label>
        <input
          id="name"
          type="text"
          bind:value={form.name}
          placeholder="Enter tenant name"
          class:error={validationErrors.name}
          disabled={loading}
        />
        {#if validationErrors.name}
          <span class="error-message">{validationErrors.name}</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="domain">Domain *</label>
        <input
          id="domain"
          type="text"
          bind:value={form.domain}
          placeholder="e.g., acme-corp"
          class:error={validationErrors.domain}
          disabled={loading || tenant}
          readonly={tenant}
        />
        {#if validationErrors.domain}
          <span class="error-message">{validationErrors.domain}</span>
        {/if}
        {#if tenant}
          <span class="help-text">Domain cannot be changed after creation</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="displayName">Display Name *</label>
        <input
          id="displayName"
          type="text"
          bind:value={form.displayName}
          placeholder="Enter display name"
          class:error={validationErrors.displayName}
          disabled={loading}
        />
        {#if validationErrors.displayName}
          <span class="error-message">{validationErrors.displayName}</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="ownerId">Owner ID *</label>
        <input
          id="ownerId"
          type="text"
          bind:value={form.ownerId}
          placeholder="Enter owner user ID"
          class:error={validationErrors.ownerId}
          disabled={loading || tenant}
          readonly={tenant}
        />
        {#if validationErrors.ownerId}
          <span class="error-message">{validationErrors.ownerId}</span>
        {/if}
        {#if tenant}
          <span class="help-text">Owner cannot be changed after creation</span>
        {/if}
      </div>

      <div class="form-group">
        <label for="plan">Plan</label>
        <select id="plan" bind:value={form.plan} disabled={loading}>
          {#each planOptions as option}
            <option value={option.value}>{option.label}</option>
          {/each}
        </select>
      </div>

      {#if tenant}
        <div class="form-group">
          <label for="status">Status</label>
          <select id="status" bind:value={form.status} disabled={loading}>
            {#each statusOptions as option}
              <option value={option.value}>{option.label}</option>
            {/each}
          </select>
        </div>
      {/if}
    </div>

    <div class="form-group full-width">
      <label for="description">Description</label>
      <textarea
        id="description"
        bind:value={form.description}
        placeholder="Enter tenant description (optional)"
        rows="3"
        disabled={loading}
      ></textarea>
    </div>

    <div class="form-actions">
      <button type="button" class="btn btn-secondary" on:click={handleCancel} disabled={loading}>
        Cancel
      </button>
      <button type="submit" class="btn btn-primary" disabled={loading}>
        {#if loading}
          Saving...
        {:else}
          {tenant ? 'Update Tenant' : 'Create Tenant'}
        {/if}
      </button>
    </div>
  </form>
</div>

<style>
  .tenant-form {
    width: 100%;
  }

  .alert {
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .alert-error {
    background: var(--arc-alert-red-bg, #fef2f2);
    color: var(--arc-alert-red-ink, #dc2626);
    border: 1px solid var(--arc-alert-red-border, #fecaca);
  }

  .alert button {
    background: none;
    border: none;
    color: inherit;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    width: 1.5rem;
    height: 1.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: none;
  }

  .alert button:hover {
    background: none;
    transform: none;
    box-shadow: none;
  }

  .form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 1.5rem;
    margin-bottom: 1.5rem;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .form-group.full-width {
    grid-column: 1 / -1;
  }

  input.error, select.error, textarea.error {
    border-color: #dc2626;
  }

  input:disabled, select:disabled, textarea:disabled {
    background-color: var(--arc-card-2, #f8fafc);
    color: var(--arc-muted, #64748b);
    cursor: not-allowed;
  }

  input[readonly] {
    background-color: var(--arc-card-2, #f8fafc);
    color: var(--arc-muted, #64748b);
  }

  .error-message {
    color: #dc2626;
    font-size: 0.875rem;
    font-weight: 500;
  }

  .help-text {
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    font-style: italic;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    padding-top: 1.5rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.65rem;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s ease;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
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

  .btn-secondary {
    background: var(--arc-card, #fff);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    color: var(--arc-ink, #1e293b);
    box-shadow: none;
  }

  .btn-secondary:hover:not(:disabled) {
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-indigo-deep, #4f46e5);
    transform: none;
  }

  @media (prefers-reduced-motion: reduce) {
    .btn {
      transition: none;
    }
    .btn-primary:hover:not(:disabled) {
      transform: none;
    }
  }

  @media (max-width: 768px) {
    .form-grid {
      grid-template-columns: 1fr;
    }

    .form-actions {
      flex-direction: column;
    }
  }
</style>
