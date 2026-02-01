<script>
import { page } from '$app/stores';
import { goto } from '$app/navigation';
import { client } from '$lib/apollo';
import { GET_ALL_TENANTS, DELETE_TENANT } from '$lib/graphql/queries';
import { onMount } from 'svelte';
import { get } from 'svelte/store';
import { toasts } from '$lib/stores/toastStore';

let tenantId = '';
let tenant = null;
let loading = true;
let error = null;
let deleting = false;
let success = false;

onMount(async () => {
  const url = new URL(get(page).url);
  tenantId = url.searchParams.get('tenantId') || '';
  try {
    const tenantsResult = await client.query({ query: GET_ALL_TENANTS });
    tenant = tenantsResult.data.getAllTenants.find(t => t.id === tenantId);
    error = null;
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
    toasts.add(`Failed to load tenant: ${error}`, 'error');
  } finally {
    loading = false;
  }
});

async function handleDelete() {
  if (!tenantId) return;
  try {
    deleting = true;
    const result = await client.mutate({
      mutation: DELETE_TENANT,
      variables: { id: tenantId }
    });
    if (result.data.deleteTenant) {
      success = true;
      toasts.add(`Tenant "${tenant?.displayName || tenant?.name || 'tenant'}" deleted successfully`, 'success');
      setTimeout(() => goto('/tenants'), 1200);
    } else {
      error = 'Tenant could not be deleted.';
      toasts.add(error, 'error');
    }
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
    toasts.add(`Failed to delete tenant: ${error}`, 'error');
  } finally {
    deleting = false;
  }
}

function handleCancel() {
  goto('/tenants');
}
</script>

<h2>Delete Tenant</h2>
{#if loading}
  <div class="loading">Loading...</div>
{:else if error}
  <div class="error">Error: {error}</div>
{:else if success}
  <div class="success">Tenant deleted successfully. Redirecting...</div>
{:else if tenant}
  <p>Are you sure you want to delete tenant <strong>{tenant.displayName}</strong>?</p>
  <div class="actions">
    <button on:click={handleDelete} disabled={deleting} class="delete-btn">
      {deleting ? 'Deleting...' : 'Delete'}
    </button>
    <button on:click={handleCancel} disabled={deleting} class="cancel-btn">Cancel</button>
  </div>
{:else}
  <p>No tenant selected for deletion.</p>
{/if}

<style>
  .actions {
    margin-top: 2rem;
    display: flex;
    gap: 1rem;
  }
  .delete-btn {
    background: #f44336;
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    font-weight: 500;
    border: none;
    cursor: pointer;
    transition: background 0.2s;
  }
  .delete-btn:disabled {
    background: #e57373;
    cursor: not-allowed;
  }
  .delete-btn:hover:not(:disabled) {
    background: #c62828;
  }
  .cancel-btn {
    background: #e0e0e0;
    color: #333;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    font-weight: 500;
    border: none;
    cursor: pointer;
    transition: background 0.2s;
  }
  .cancel-btn:hover:not(:disabled) {
    background: #bdbdbd;
  }
  .loading {
    text-align: center;
    margin: 2rem;
    color: #64748b;
  }
  .error {
    color: #f44336;
    margin: 2rem 0;
    text-align: center;
  }
  .success {
    color: #10b981;
    margin: 2rem 0;
    text-align: center;
  }
</style>
