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
  <span class="eyebrow">Tenants</span>
  <h3>Create New Tenant</h3>
  <button type="button" class="btn-fill" on:click={fillRandom}>Fill Random</button>
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
  }
  h3 {
    margin: 0 0 1rem;
    font-size: 1.6rem;
    font-weight: 700;
    color: var(--arc-ink, #0f172a);
  }
  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }
  .form-group {
    margin-bottom: 1.5rem;
  }
  .btn-fill {
    padding: 0.5rem 1rem;
    background: var(--arc-chip-soft-indigo-bg);
    color: var(--arc-chip-indigo-ink);
    border: 1px solid var(--arc-hover-border);
    border-radius: 0.6rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s ease;
    margin-bottom: 1rem;
    box-shadow: none;
  }
  .btn-fill:hover {
    background: var(--arc-chip-indigo-bg);
    border-color: var(--arc-hover-border);
    box-shadow: none;
  }
</style>
