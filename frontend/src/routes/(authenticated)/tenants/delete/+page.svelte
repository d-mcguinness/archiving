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

<span class="eyebrow">Tenants</span>
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
  h2 {
    margin: 0 0 1rem;
    color: var(--arc-ink, #0f172a);
  }
  .actions {
    margin-top: 2rem;
    display: flex;
    gap: 1rem;
  }
  .delete-btn {
    background: linear-gradient(135deg, #ef4444, #dc2626);
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.65rem;
    font-weight: 700;
    border: none;
    cursor: pointer;
    box-shadow: 0 10px 30px -8px rgba(220, 38, 38, 0.5);
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
  }
  .delete-btn:disabled {
    background: #fca5a5;
    cursor: not-allowed;
    box-shadow: none;
  }
  .delete-btn:hover:not(:disabled) {
    background: linear-gradient(135deg, #dc2626, #b91c1c);
    transform: translateY(-2px);
  }
  .cancel-btn {
    background: var(--arc-card);
    color: var(--arc-ink);
    padding: 0.5rem 1.5rem;
    border-radius: 0.65rem;
    font-weight: 600;
    border: 1.5px solid var(--arc-line-strong);
    cursor: pointer;
    box-shadow: none;
    transition: border-color 0.18s ease, color 0.18s ease;
  }
  .cancel-btn:hover:not(:disabled) {
    background: var(--arc-card);
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-link);
    box-shadow: none;
  }
  .cancel-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .loading {
    text-align: center;
    margin: 2rem;
    color: var(--arc-muted);
  }
</style>
