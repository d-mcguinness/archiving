<script lang="ts">
import { page } from '$app/stores';
import { goto } from '$app/navigation';
import { client } from '$lib/apollo';
import { GET_ALL_USERS, GET_ALL_TENANTS, UPDATE_TENANT, CREATE_TENANT } from '$lib/graphql/queries';
import { onMount } from 'svelte';
import { get } from 'svelte/store';
import { toasts } from '$lib/stores/toastStore';

let tenantId = '';
let tenant: any | null = null;
let users: any[] = [];
let loading = true;
let error: string | null = null;
let updating = false;
let isCreateMode = false;

let form: {
  name: string;
  domain: string;
  displayName: string;
  description: string;
  ownerId: string;
  plan: string;
} = { name: '', domain: '', displayName: '', description: '', ownerId: '', plan: 'FREE' };

const plans = ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM'];

onMount(async () => {
  const url = new URL(get(page).url);
  tenantId = url.searchParams.get('tenantId') || '';

  // If no tenantId is provided, this is create mode
  if (!tenantId) {
    isCreateMode = true;
    loading = false;
  }

  try {
    // Always load users for the owner dropdown
    const usersResult = await client.query({ query: GET_ALL_USERS });
    users = usersResult?.data?.getAllUsers || [];

    // Only load tenant data if we're in update mode
    if (!isCreateMode) {
      const tenantsResult = await client.query({ query: GET_ALL_TENANTS });
      tenant = tenantsResult?.data?.getAllTenants?.find((t: any) => t.id === tenantId);

      if (tenant) {
        form = {
          name: tenant.name || '',
          domain: tenant.domain || '',
          displayName: tenant.displayName || '',
          description: tenant.description || '',
          ownerId: tenant.ownerId || '',
          plan: tenant.plan || 'FREE'
        };
      } else {
        error = 'Tenant not found';
        loading = false;
        return;
      }
    }

    error = null;
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
  } finally {
    loading = false;
  }
});

async function saveTenant() {
  if (!form.name || !form.domain || !form.ownerId) return;

  try {
    updating = true;

    if (isCreateMode) {
      // Create new tenant
      await client.mutate({
        mutation: CREATE_TENANT,
        variables: {
          input: {
            name: form.name,
            domain: form.domain,
            displayName: form.displayName || form.name,
            description: form.description,
            ownerId: form.ownerId,
            plan: form.plan
          }
        }
      });
    } else {
      // Update existing tenant - only send updateable fields
      await client.mutate({
        mutation: UPDATE_TENANT,
        variables: {
          input: {
            tenantId,
            name: form.name,
            displayName: form.displayName || form.name,
            description: form.description,
            plan: form.plan
            // Note: domain and ownerId are NOT updateable
          }
        }
      });
    }

    toasts.add(`Tenant "${form.displayName || form.name}" ${isCreateMode ? 'created' : 'updated'} successfully`, 'success');
    goto('/tenants');
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
    toasts.add(`Failed to ${isCreateMode ? 'create' : 'update'} tenant: ${error}`, 'error');
  } finally {
    updating = false;
  }
}

function goBack() {
  goto('/tenants');
}
</script>

<svelte:head>
  <title>{isCreateMode ? 'Create' : 'Update'} Tenant - Archiving System</title>
</svelte:head>

<div class="update-tenant-page">
  <div class="page-header">
    <div class="header-text">
      <span class="eyebrow">Tenants</span>
      <h1>{isCreateMode ? 'Create' : 'Update'} Tenant</h1>
    </div>
    <div class="actions">
      <button class="btn btn-secondary" on:click={goBack}>
        ← Back to Tenants
      </button>
    </div>
  </div>

  {#if loading}
    <div class="loading-container">
      <div class="loading-spinner"></div>
      <p>Loading tenant information...</p>
    </div>
  {:else if error}
    <div class="alert alert-error">
      <div class="alert-content">
        <strong>Error:</strong> {error}
        <button class="alert-close" on:click={() => error = null} aria-label="Close error">×</button>
      </div>
      <div class="alert-actions">
        <button class="btn btn-secondary" on:click={goBack}>Go Back</button>
        {#if tenantId}
          <button class="btn btn-primary" on:click={() => window.location.reload()}>Retry</button>
        {/if}
      </div>
    </div>
  {:else if tenant || isCreateMode}
    <div class="form-section">
      <div class="tenant-info">
        <h3>{isCreateMode ? 'Creating New Tenant' : 'Updating: ' + tenant.name}</h3>
        {#if !isCreateMode}
          <p class="tenant-id">ID: {tenant.id}</p>
        {/if}
      </div>

      <form on:submit|preventDefault={saveTenant} class="form-container">
        <div class="form-row">
          <div class="form-group">
            <label for="name">Name *</label>
            <input
              type="text"
              id="name"
              bind:value={form.name}
              required
              disabled={updating}
              placeholder={isCreateMode ? "acme-corp" : "Enter tenant name"}
            />
          </div>
          <div class="form-group">
            <label for="domain">Domain *</label>
            <input
              type="text"
              id="domain"
              bind:value={form.domain}
              required
              disabled={updating || !isCreateMode}
              placeholder={isCreateMode ? "acme" : "Enter domain"}
              title={!isCreateMode ? "Domain cannot be changed after creation" : ""}
            />
            {#if !isCreateMode}
              <small class="field-note">Domain cannot be changed after creation</small>
            {/if}
          </div>
        </div>

        <div class="form-group">
          <label for="displayName">Display Name</label>
          <input
            type="text"
            id="displayName"
            bind:value={form.displayName}
            disabled={updating}
            placeholder={isCreateMode ? "ACME Corporation" : "Enter display name (optional)"}
          />
        </div>

        <div class="form-group">
          <label for="description">Description</label>
          <textarea
            id="description"
            bind:value={form.description}
            disabled={updating}
            rows="3"
            placeholder={isCreateMode ? "Tenant description..." : "Enter description (optional)"}
          ></textarea>
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="ownerId">Owner *</label>
            <select id="ownerId" bind:value={form.ownerId} required disabled={updating || !isCreateMode}>
              <option value="">Select an owner</option>
              {#each users as user}
                <option value={user.id}>{user.name} ({user.email})</option>
              {/each}
            </select>
            {#if !isCreateMode}
              <small class="field-note">Owner cannot be changed after creation</small>
            {/if}
          </div>
          <div class="form-group">
            <label for="plan">Plan *</label>
            <select id="plan" bind:value={form.plan} required disabled={updating}>
              {#each plans as plan}
                <option value={plan}>{plan}</option>
              {/each}
            </select>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="button"
            class="btn btn-secondary"
            on:click={goBack}
            disabled={updating}
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn btn-primary"
            disabled={updating || !form.name || !form.domain || !form.ownerId}
          >
            {#if updating}
              <span class="loading-spinner small"></span>
              {isCreateMode ? 'Creating...' : 'Updating...'}
            {:else}
              {isCreateMode ? 'Create Tenant' : 'Update Tenant'}
            {/if}
          </button>
        </div>
      </form>
    </div>
  {/if}
</div>

<style>
  .update-tenant-page {
    padding: 2rem;
    max-width: 800px;
    margin: 0 auto;
    background: var(--arc-ground);
    min-height: 100vh;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid var(--arc-line-strong);
  }

  .page-header h1 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2.25rem;
    font-weight: 700;
  }

  .actions {
    display: flex;
    gap: 1rem;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.65rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.875rem;
  }

  .btn-primary {
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    font-weight: 700;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
  }

  .btn-primary:hover:not(:disabled) {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .btn-secondary {
    background: var(--arc-card);
    border: 1.5px solid var(--arc-line-strong);
    color: var(--arc-ink);
    box-shadow: none;
  }

  .btn-secondary:hover:not(:disabled) {
    background: var(--arc-card);
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-link);
    box-shadow: none;
  }

  .btn:disabled {
    background: var(--arc-disabled-bg);
    cursor: not-allowed;
    transform: none;
    opacity: 0.6;
    box-shadow: none;
  }

  .loading-container {
    text-align: center;
    padding: 4rem 2rem;
    color: var(--arc-muted);
  }

  .loading-container p {
    margin-top: 1rem;
    font-size: 1.1rem;
  }

  .loading-spinner {
    border: 4px solid var(--arc-line-strong);
    border-top: 4px solid var(--arc-indigo, #6366f1);
    border-radius: 50%;
    width: 3rem;
    height: 3rem;
    animation: spin 1s linear infinite;
    display: inline-block;
  }

  .loading-spinner.small {
    width: 1rem;
    height: 1rem;
    border-width: 2px;
    margin-right: 0.5rem;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  @media (prefers-reduced-motion: reduce) {
    .loading-spinner {
      animation: none;
    }
    .btn {
      transition: none;
    }
  }

  .alert {
    padding: 1.5rem;
    border-radius: 0.75rem;
    margin-bottom: 2rem;
    box-shadow: var(--arc-shadow-card, 0 4px 6px -1px rgba(0, 0, 0, 0.1));
  }

  .alert-error {
    background: var(--arc-alert-red-bg);
    color: var(--arc-alert-red-ink);
    border: 1px solid var(--arc-alert-red-border);
  }

  .alert-content {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 1rem;
  }

  .alert-close {
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
    border-radius: 0.25rem;
    box-shadow: none;
    transition: background 0.2s;
  }

  .alert-close:hover {
    background: rgba(0, 0, 0, 0.1);
    transform: none;
    box-shadow: none;
  }

  .alert-actions {
    display: flex;
    gap: 0.75rem;
  }

  .form-section {
    background: var(--arc-card);
    border-radius: 1rem;
    overflow: hidden;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .tenant-info {
    padding: 1.5rem 2rem;
    background: linear-gradient(135deg, var(--arc-card-2) 0%, var(--arc-line-strong) 100%);
    border-bottom: 1px solid var(--arc-line-strong);
  }

  .tenant-info h3 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.25rem;
    font-weight: 700;
  }

  .tenant-id {
    margin: 0;
    color: var(--arc-muted);
    font-size: 0.875rem;
    font-family: 'Monaco', 'Menlo', monospace;
  }

  .form-container {
    padding: 2rem;
  }

  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group input:disabled,
  .form-group select:disabled,
  .form-group textarea:disabled {
    background: var(--arc-card-2);
    color: var(--arc-faint);
    cursor: not-allowed;
  }

  .form-group textarea {
    resize: vertical;
    min-height: 3rem;
  }

  .field-note {
    display: block;
    margin-top: 0.5rem;
    color: var(--arc-muted);
    font-size: 0.75rem;
    font-style: italic;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 1px solid var(--arc-line-strong);
  }

  @media (max-width: 768px) {
    .update-tenant-page {
      padding: 1rem;
    }

    .page-header {
      flex-direction: column;
      gap: 1rem;
      align-items: stretch;
    }

    .actions {
      justify-content: center;
    }

    .form-row {
      grid-template-columns: 1fr;
      gap: 0;
    }

    .form-actions {
      flex-direction: column-reverse;
    }

    .btn {
      width: 100%;
      justify-content: center;
    }

    .tenant-info {
      padding: 1rem 1.5rem;
    }

    .form-container {
      padding: 1.5rem;
    }

    .alert-actions {
      flex-direction: column;
    }
  }
</style>
