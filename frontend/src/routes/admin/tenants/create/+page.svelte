<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { CREATE_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

  let newTenant = {
    name: '',
    domain: '',
    displayName: '',
    description: '',
    ownerId: '',
    plan: 'FREE'
  };

  let users: any[] = [];
  let creating = false;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

  const plans = ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM'];

  onMount(async () => {
    // Check authentication and role
    const role = localStorage.getItem('auth_role');
    const tenantId = localStorage.getItem('auth_tenantId');
    currentRole = role || '';

    // Only ADMIN can create tenants
    if (currentRole !== 'ADMIN') {
      hasAccess = false;

      // Redirect non-admin users to appropriate pages
      if (currentRole === 'TENANT' && tenantId) {
        goto(`/tenants/${tenantId}`);
      } else if (currentRole === 'USER') {
        goto('/');
      } else {
        goto('/login');
      }
      return;
    }

    hasAccess = true;
    await loadUsers();
  });

  async function loadUsers() {
    try {
      const result = await client.query({
        query: GET_ALL_USERS,
        fetchPolicy: 'network-only'
      });
      users = result?.data?.getAllUsers || [];
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load users';
      console.error('Load users error:', e);
      toasts.error(`Failed to load users: ${error}`);
    }
  }

  async function createTenant() {
    if (!newTenant.name || !newTenant.domain || !newTenant.ownerId) {
      toasts.error('Please fill in all required fields');
      return;
    }

    try {
      creating = true;
      error = null;

      const result = await client.mutate({
        mutation: CREATE_TENANT,
        variables: {
          input: {
            name: newTenant.name,
            domain: newTenant.domain,
            displayName: newTenant.displayName || newTenant.name,
            description: newTenant.description,
            ownerId: newTenant.ownerId,
            plan: newTenant.plan
          }
        }
      });

      if (result.data?.createTenant) {
        toasts.success(`Tenant "${newTenant.displayName || newTenant.name}" created successfully`);
        goto('/admin/tenants');
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Create tenant error:', e);
      toasts.error(`Failed to create tenant: ${error}`);
    } finally {
      creating = false;
    }
  }

  function fillRandom() {
    const adjectives = ['Acme', 'Global', 'Nordic', 'Summit', 'Atlas', 'Apex', 'Vanguard', 'Pinnacle'];
    const nouns = ['Corp', 'Systems', 'Solutions', 'Industries', 'Labs', 'Digital', 'Technologies', 'Group'];
    const adj = adjectives[Math.floor(Math.random() * adjectives.length)];
    const noun = nouns[Math.floor(Math.random() * nouns.length)];
    const slug = `${adj.toLowerCase()}-${noun.toLowerCase()}`;
    newTenant.name = slug;
    newTenant.domain = `${slug}.example.com`;
    newTenant.displayName = `${adj} ${noun}`;
    newTenant.description = `${adj} ${noun} is a leading provider of archiving solutions.`;
    newTenant.plan = plans[Math.floor(Math.random() * plans.length)];
    if (users.length > 0) {
      newTenant.ownerId = users[Math.floor(Math.random() * users.length)].id;
    }
  }

  function handleCancel() {
    goto('/admin/tenants');
  }
</script>

<svelte:head>
  <title>Create Tenant - Admin - Archiving System</title>
</svelte:head>

<div class="create-tenant-page">
  {#if !hasAccess}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to create tenants.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <div class="page-header">
      <h1>Create New Tenant</h1>
      <div class="header-actions">
        <button type="button" class="btn-fill" on:click={fillRandom}>Fill Random</button>
        <button class="btn-cancel" on:click={handleCancel}>
          ← Back to Tenants
        </button>
      </div>
    </div>

    {#if error}
      <div class="error-banner">
        ❌ {error}
      </div>
    {/if}

    <div class="form-container">
      <form on:submit|preventDefault={createTenant}>
        <div class="form-section">
          <h3>Basic Information</h3>

          <div class="form-row">
            <div class="form-group">
              <label for="name">Name *</label>
              <input
                type="text"
                id="name"
                bind:value={newTenant.name}
                required
                disabled={creating}
                placeholder="acme-corp"
              />
              <small class="field-hint">Unique identifier for the tenant</small>
            </div>

            <div class="form-group">
              <label for="domain">Domain *</label>
              <input
                type="text"
                id="domain"
                bind:value={newTenant.domain}
                required
                disabled={creating}
                placeholder="acme.example.com"
              />
              <small class="field-hint">Tenant's domain name</small>
            </div>
          </div>

          <div class="form-group">
            <label for="displayName">Display Name</label>
            <input
              type="text"
              id="displayName"
              bind:value={newTenant.displayName}
              disabled={creating}
              placeholder="ACME Corporation"
            />
            <small class="field-hint">Friendly name shown in UI (optional)</small>
          </div>

          <div class="form-group">
            <label for="description">Description</label>
            <textarea
              id="description"
              bind:value={newTenant.description}
              disabled={creating}
              rows="4"
              placeholder="Enter a description for this tenant..."
            ></textarea>
            <small class="field-hint">Optional description of the tenant</small>
          </div>
        </div>

        <div class="form-section">
          <h3>Settings</h3>

          <div class="form-row">
            <div class="form-group">
              <label for="ownerId">Owner *</label>
              <select
                id="ownerId"
                bind:value={newTenant.ownerId}
                required
                disabled={creating}
              >
                <option value="">Select an owner</option>
                {#each users as user}
                  <option value={user.id}>{user.name} ({user.email})</option>
                {/each}
              </select>
              <small class="field-hint">User who will own this tenant</small>
            </div>

            <div class="form-group">
              <label for="plan">Plan *</label>
              <select
                id="plan"
                bind:value={newTenant.plan}
                required
                disabled={creating}
              >
                {#each plans as plan}
                  <option value={plan}>{plan}</option>
                {/each}
              </select>
              <small class="field-hint">Subscription plan for the tenant</small>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="button"
            class="btn-secondary"
            on:click={handleCancel}
            disabled={creating}
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            disabled={creating || !newTenant.name || !newTenant.domain || !newTenant.ownerId}
          >
            {creating ? '⏳ Creating...' : '✅ Create Tenant'}
          </button>
        </div>
      </form>
    </div>
  {/if}
</div>

<style>
  .create-tenant-page {
    max-width: 900px;
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

  .header-actions {
    display: flex;
    gap: 1rem;
    align-items: center;
  }

  .btn-fill {
    padding: 0.5rem 1rem;
    background: #f0fdf4;
    color: #16a34a;
    border: 1px solid #bbf7d0;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-fill:hover {
    background: #dcfce7;
    border-color: #86efac;
  }

  .btn-cancel {
    padding: 0.75rem 1.5rem;
    background: #f1f5f9;
    color: #475569;
    border: none;
    border-radius: 0.5rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-cancel:hover {
    background: #e2e8f0;
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

  .error-banner {
    background: #fee2e2;
    border: 1px solid #fca5a5;
    color: #991b1b;
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
    text-align: center;
  }

  .form-container {
    background: white;
    padding: 2rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .form-section {
    margin-bottom: 2rem;
    padding-bottom: 2rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .form-section:last-of-type {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }

  .form-section h3 {
    margin: 0 0 1.5rem 0;
    color: #1e293b;
    font-size: 1.25rem;
  }

  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #475569;
    font-weight: 600;
    font-size: 0.875rem;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 1rem;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .form-group input:focus,
  .form-group select:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .form-group input:disabled,
  .form-group select:disabled,
  .form-group textarea:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .form-group textarea {
    resize: vertical;
    min-height: 100px;
  }

  .field-hint {
    display: block;
    margin-top: 0.375rem;
    color: #64748b;
    font-size: 0.75rem;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 2rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn-primary,
  .btn-secondary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
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

  @media (max-width: 768px) {
    .create-tenant-page {
      padding: 1rem;
    }

    .page-header {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }

    .form-row {
      grid-template-columns: 1fr;
    }

    .form-actions {
      flex-direction: column-reverse;
    }

    .form-actions button {
      width: 100%;
    }
  }
</style>

