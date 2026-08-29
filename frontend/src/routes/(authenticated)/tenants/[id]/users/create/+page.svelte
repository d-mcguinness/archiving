<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { CREATE_USER, GET_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData { tenantId: string; }
  export let data: PageData;

  let tenant: any = null;
  let newUser = { name: '', email: '', age: '' };
  let creating = false;
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to create users');
      goto(`/tenants/${data.tenantId}/users`);
      return;
    }

    try {
      const result = await client.query({ query: GET_TENANT, variables: { id: data.tenantId }, fetchPolicy: 'network-only' });
      tenant = result?.data?.getTenant;
    } catch (e) {
      console.error('Failed to load tenant:', e);
    } finally {
      loading = false;
    }
  });

  async function createUser() {
    if (!newUser.name || !newUser.email) return;
    try {
      creating = true;
      error = null;
      const result = await client.mutate({
        mutation: CREATE_USER,
        variables: {
          input: {
            name: newUser.name,
            email: newUser.email,
            age: newUser.age ? parseInt(newUser.age) : null
          }
        }
      });
      if (result.data.createUser) {
        toasts.add(`User "${newUser.name}" created successfully`, 'success');
        goto(`/tenants/${data.tenantId}/users`);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      toasts.add(`Failed to create user: ${error}`, 'error');
    } finally {
      creating = false;
    }
  }

  function handleCancel() {
    goto(`/tenants/${data.tenantId}/users`);
  }
</script>

<svelte:head><title>Create User - {tenant?.displayName || tenant?.name || 'Tenant'} - Arcana</title></svelte:head>

{#if !hasAccess && !loading}
  <div class="access-denied"><div class="access-denied-icon">🚫</div><h1>Access Denied</h1><p>You don't have permission to create users.</p><p class="redirect-message">Redirecting...</p></div>
{:else}
  <div class="user-container">
    <Breadcrumb context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }} items={[{ label: 'Users', href: `/tenants/${data.tenantId}/users` }, { label: 'Create' }]} />

    <div class="form-header">
      <span class="eyebrow">User management</span>
      <h1>Create User</h1>
      {#if tenant}<p class="subtitle">Tenant: {tenant.displayName || tenant.name}</p>{/if}
    </div>

    {#if error}<div class="alert alert-error">{error}<button on:click={() => error = null}>x</button></div>{/if}

    {#if loading}
      <div class="loading-state"><div class="spinner"></div><p>Loading...</p></div>
    {:else}
      <form on:submit|preventDefault={createUser}>
        <section class="form-section">
          <div class="form-group">
            <label for="userName">Name <span class="req">*</span></label>
            <input type="text" id="userName" bind:value={newUser.name} required placeholder="Enter full name" />
          </div>
          <div class="form-group">
            <label for="userEmail">Email <span class="req">*</span></label>
            <input type="email" id="userEmail" bind:value={newUser.email} required placeholder="Enter email address" />
          </div>
          <div class="form-group">
            <label for="userAge">Age</label>
            <input type="number" id="userAge" bind:value={newUser.age} placeholder="Enter age (optional)" />
          </div>
        </section>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" on:click={handleCancel}>Cancel</button>
          <button type="submit" class="btn btn-primary" disabled={creating || !newUser.name || !newUser.email}>
            {creating ? 'Creating...' : 'Create User'}
          </button>
        </div>
      </form>
    {/if}
  </div>
{/if}

<style>
  .access-denied { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; text-align: center; padding: 3rem; }
  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0 0 1rem 0; color: var(--arc-ink, #0f172a); font-size: 2rem; }
  .access-denied p { margin: 0.5rem 0; color: var(--arc-muted, #64748b); font-size: 1.125rem; }
  .redirect-message { color: var(--arc-indigo, #6366f1); font-weight: 500; animation: pulse 1.5s ease-in-out infinite; }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
  .user-container { max-width: 600px; margin: 2rem auto; background: var(--arc-card, #fff); padding: 2.5rem; border-radius: 1rem; box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04)); border: 1px solid var(--arc-line, #e8edf3); }
  .form-header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 1px solid var(--arc-line, #e8edf3); }
  .form-header h1 { margin: 0 0 0.5rem 0; color: var(--arc-ink, #0f172a); font-size: 1.75rem; font-weight: 700; }
  .subtitle { margin: 0; color: var(--arc-muted, #64748b); font-size: 0.925rem; }
  .alert { padding: 0.875rem 1rem; border-radius: 0.375rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
  .alert-error { background: var(--arc-alert-red-bg, #fef2f2); color: var(--arc-alert-red-ink, #dc2626); border: 1px solid var(--arc-alert-red-border, #fecaca); }
  .alert button { background: none; border: none; box-shadow: none; padding: 0; color: inherit; font-size: 1.25rem; cursor: pointer; }
  .alert button:hover { transform: none; box-shadow: none; }
  .loading-state { display: flex; flex-direction: column; align-items: center; padding: 3rem 2rem; gap: 1rem; }
  .loading-state .spinner { width: 2.5rem; height: 2.5rem; border: 3px solid var(--arc-line-strong, #e2e8f0); border-top-color: var(--arc-indigo, #6366f1); border-radius: 50%; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .loading-state p { color: var(--arc-muted, #64748b); font-size: 0.875rem; }
  .form-section { margin-bottom: 2rem; }
  .form-group { margin-bottom: 1.25rem; }
  .form-group label { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; color: var(--arc-ink, #0f172a); font-weight: 600; font-size: 0.85rem; }
  .req { color: #ef4444; font-weight: 600; }
  /* inputs inherit the global Arcana input styling from app.css */
  .form-actions { display: flex; justify-content: space-between; gap: 1rem; padding-top: 1.5rem; border-top: 1px solid var(--arc-line, #e8edf3); }
  .btn { padding: 0.75rem 2rem; border-radius: 0.65rem; font-weight: 700; cursor: pointer; font-size: 0.875rem; }
  /* .btn-primary inherits the global brand-gradient button styling from app.css */
  .btn-secondary { background: var(--arc-card, #fff); color: var(--arc-ink, #1e293b); border: 1.5px solid var(--arc-line-strong, #cbd5e1); box-shadow: none; transition: border-color 0.18s ease, color 0.18s ease, transform 0.18s ease; }
  .btn-secondary:hover { background: var(--arc-card, #fff); border-color: var(--arc-indigo, #6366f1); color: var(--arc-link, #4f46e5); }
</style>