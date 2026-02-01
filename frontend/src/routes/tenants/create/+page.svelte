<script>
import { client } from '$lib/apollo';
import { CREATE_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
import { goto } from '$app/navigation';
import { onMount } from 'svelte';
import { toasts } from '$lib/stores/toastStore';

let newTenant = {
  name: '',
  domain: '',
  displayName: '',
  description: '',
  ownerId: '',
  plan: 'FREE'
};
let users = [];
let creating = false;
let error = null;
const plans = ['FREE', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM'];

onMount(async () => {
  try {
    const result = await client.query({ query: GET_ALL_USERS });
    users = result?.data?.getAllUsers || [];
  } catch (e) {
    error = e instanceof Error ? e.message : 'Failed to load users';
    console.error('Load users error:', e);
    toasts.add(`Failed to load users: ${error}`, 'error');
  }
});

async function createTenant() {
  if (!newTenant.name || !newTenant.domain || !newTenant.ownerId) return;
  try {
    creating = true;
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
    if (result.data.createTenant) {
      toasts.add(`Tenant "${newTenant.displayName || newTenant.name}" created successfully`, 'success');
      goto('/tenants');
      newTenant = { name: '', domain: '', displayName: '', description: '', ownerId: '', plan: 'FREE' };
    }
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
    toasts.add(`Failed to create tenant: ${error}`, 'error');
  } finally {
    creating = false;
  }
}
</script>

<div class="form-container">
  <h3>Create New Tenant</h3>
  {#if error}
    <div class="error">Error: {error}</div>
  {/if}
  <form on:submit|preventDefault={createTenant}>
    <div class="form-row">
      <div class="form-group">
        <label for="name">Name *</label>
        <input type="text" id="name" bind:value={newTenant.name} required disabled={creating} placeholder="acme-corp" />
      </div>
      <div class="form-group">
        <label for="domain">Domain *</label>
        <input type="text" id="domain" bind:value={newTenant.domain} required disabled={creating} placeholder="acme" />
      </div>
    </div>
    <div class="form-group">
      <label for="displayName">Display Name</label>
      <input type="text" id="displayName" bind:value={newTenant.displayName} disabled={creating} placeholder="ACME Corporation" />
    </div>
    <div class="form-group">
      <label for="description">Description</label>
      <textarea id="description" bind:value={newTenant.description} disabled={creating} rows="3" placeholder="Tenant description..."></textarea>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label for="ownerId">Owner *</label>
        <select id="ownerId" bind:value={newTenant.ownerId} required disabled={creating}>
          <option value="">Select an owner</option>
          {#each users as user}
            <option value={user.id}>{user.name} ({user.email})</option>
          {/each}
        </select>
      </div>
      <div class="form-group">
        <label for="plan">Plan *</label>
        <select id="plan" bind:value={newTenant.plan} required disabled={creating}>
          {#each plans as plan}
            <option value={plan}>{plan}</option>
          {/each}
        </select>
      </div>
    </div>
    <button type="submit" disabled={creating || !newTenant.name || !newTenant.domain || !newTenant.ownerId}>
      {creating ? 'Creating...' : 'Create Tenant'}
    </button>
  </form>
</div>

<style>
  .form-container {
    max-width: 500px;
    margin: 2rem auto;
    background: #fff;
    padding: 2rem;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    border: 1px solid #e2e8f0;
  }
  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }
  .form-group {
    margin-bottom: 1.5rem;
  }
  label {
    display: block;
    margin-bottom: 0.5rem;
    color: #1e293b;
    font-weight: 500;
  }
  input, select, textarea {
    width: 100%;
    padding: 0.5rem;
    border-radius: 0.25rem;
    border: 1px solid #e2e8f0;
    font-size: 1rem;
  }
  button[type="submit"] {
    background: #3b82f6;
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    font-weight: 500;
    border: none;
    cursor: pointer;
    transition: background 0.2s;
  }
  button[type="submit"]:disabled {
    background: #90cdf4;
    cursor: not-allowed;
  }
  .error {
    color: #f44336;
    margin: 2rem 0;
    text-align: center;
  }
</style>
