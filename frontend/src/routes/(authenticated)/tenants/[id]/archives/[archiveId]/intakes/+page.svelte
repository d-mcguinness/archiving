<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_INTAKES_BY_TENANT_V2, GET_ALL_USERS, GET_TENANT } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData { tenantId: string; archiveId: string; }
  export let data: PageData;

  let archive: any = null;
  let sips: any[] = [];
  let users: any[] = [];
  let tenant: any = null;
  let loading = true;

  const GET_ARCHIVE = gql`query GetArchive($id: ID!) { getArchive(id: $id) { id title standard } }`;

  onMount(async () => {
    await Promise.all([loadArchive(), loadTenant(), loadIntakes(), loadUsers()]);
    loading = false;
  });

  async function loadArchive() {
    try { const r = await client.query({ query: GET_ARCHIVE, variables: { id: data.archiveId }, fetchPolicy: 'network-only' }); archive = r?.data?.getArchive; } catch (e) { console.error(e); }
  }
  async function loadTenant() {
    try { const r = await client.query({ query: GET_TENANT, variables: { id: data.tenantId }, fetchPolicy: 'network-only' }); tenant = r?.data?.getTenant; } catch (e) { console.error(e); }
  }
  async function loadIntakes() {
    try { const r = await client.query({ query: GET_INTAKES_BY_TENANT_V2, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' }); sips = r?.data?.getIntakesByTenantV2 || []; } catch (e) { toasts.error('Failed to load Intakes'); }
  }
  async function loadUsers() {
    try { const r = await client.query({ query: GET_ALL_USERS }); users = r?.data?.getAllUsers || []; } catch (e) { console.error(e); }
  }

  function getUserName(uid: string) { return users.find((u: any) => u.id === uid)?.name || `User #${uid}`; }
  function getStatusClass(s: string) { return s?.toLowerCase() || ''; }

  // Filter Intakes to only those matching this archive's standard
  $: filteredIntakes = archive?.standard
    ? sips.filter((s: any) => s.standard === archive.standard)
    : sips;
</script>

<svelte:head><title>Intakes - {archive?.title || 'Archive'} - Arcana</title></svelte:head>

<div class="page">
  {#if loading}
    <div class="loading"><div class="spinner"></div><p>Loading...</p></div>
  {:else}
    <Breadcrumb context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }} items={[ { label: 'Archives', href: `/tenants/${data.tenantId}/archives` }, { label: archive?.title || 'Archive', href: `/tenants/${data.tenantId}/archives/${data.archiveId}` }, { label: 'Intakes' } ]} />

    <div class="page-header">
      <div>
        <span class="eyebrow">Intakes</span>
        <h1>📦 Intake packages</h1>
        {#if archive}<p class="subtitle">Archive: {archive.title} ({archive.standard})</p>{/if}
      </div>
      <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/intakes/create" class="btn-create">+ Create Intake</a>
    </div>

    {#if filteredIntakes.length === 0}
      <div class="empty"><h3>No Intakes found for this archive</h3><p>Create a Intake with the {archive?.standard || ''} standard to get started.</p></div>
    {:else}
      <div class="table-container">
        <table class="data-table">
          <thead><tr><th>ID</th><th>Title</th><th>Standard</th><th>Status</th><th>Owner</th><th>Created</th><th>Actions</th></tr></thead>
          <tbody>
            {#each filteredIntakes as sip (sip.id)}
              <tr>
                <td class="id-cell">{sip.id}</td>
                <td class="title-cell"><div class="item-title">{sip.title}</div>{#if sip.description}<div class="item-desc">{sip.description}</div>{/if}</td>
                <td><span class="badge standard">{sip.standard}</span></td>
                <td><span class="badge status-{getStatusClass(sip.status)}">{sip.status}</span></td>
                <td class="owner-cell">{getUserName(sip.ownerId)}</td>
                <td class="date-cell">{new Date(sip.createdAt).toLocaleDateString()}</td>
                <td class="actions-cell"><a href="/tenants/{data.tenantId}/archives/{data.archiveId}/intakes/{sip.id}/edit" class="btn-action btn-edit">✏️ Edit</a></td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  {/if}
</div>

<style>
  .page { max-width: 1400px; margin: 0 auto; padding: 2rem; }
  .loading { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; gap: 1rem; }
  .spinner { border: 4px solid var(--arc-line-strong); border-top: 4px solid var(--arc-indigo); border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem; gap: 1rem; }
  .page-header h1 { margin: 0 0 0.35rem; color: var(--arc-ink); font-size: 1.75rem; }
  .subtitle { margin: 0; color: var(--arc-muted); font-size: 0.9rem; }
  .btn-create { padding: 0.65rem 1.25rem; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; border-radius: 0.65rem; text-decoration: none; font-weight: 700; font-size: 0.875rem; box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6); transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease; white-space: nowrap; }
  .btn-create:hover { background: linear-gradient(135deg, #4f46e5, #7c3aed); transform: translateY(-2px); }
  .empty { text-align: center; padding: 4rem 2rem; background: var(--arc-card); border-radius: 1rem; border: 1px solid var(--arc-line); box-shadow: var(--arc-shadow-card); }
  .empty h3 { margin: 0 0 0.5rem; color: var(--arc-ink); } .empty p { margin: 0; color: var(--arc-muted); }
  .table-container { background: var(--arc-card); border-radius: 1rem; overflow-x: auto; box-shadow: var(--arc-shadow-card); border: 1px solid var(--arc-line); border-top: 3px solid #ec4899; }
  .data-table { width: 100%; border-collapse: collapse; min-width: 800px; }
  .data-table thead { background: var(--arc-card-2); border-bottom: 1px solid var(--arc-line); }
  .data-table th { padding: 1rem; text-align: left; font-weight: 700; color: var(--arc-muted); font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.08em; }
  .data-table td { padding: 1rem; border-bottom: 1px solid var(--arc-line); }
  .data-table tbody tr:last-child td { border-bottom: none; }
  .data-table tbody tr:hover { background: var(--arc-card-2); }
  .id-cell { color: var(--arc-muted); font-family: monospace; font-size: 0.875rem; width: 60px; }
  .title-cell { min-width: 200px; } .item-title { font-weight: 600; color: var(--arc-ink); } .item-desc { font-size: 0.825rem; color: var(--arc-muted); margin-top: 0.15rem; }
  .owner-cell, .date-cell { color: var(--arc-muted); font-size: 0.875rem; white-space: nowrap; }
  .actions-cell { white-space: nowrap; }
  .badge { display: inline-block; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; }
  .standard { background: var(--arc-chip-pink-bg); color: var(--arc-chip-pink-ink); }
  .status-draft { background: var(--arc-chip-amber-bg); color: var(--arc-chip-amber-ink); } .status-submitted { background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); } .status-validated { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); } .status-accepted { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); } .status-rejected { background: var(--arc-chip-red-bg); color: var(--arc-chip-red-ink); }
  .btn-action { display: inline-block; padding: 0.375rem 0.75rem; margin: 0 0.25rem; border: none; border-radius: 0.5rem; font-size: 0.75rem; font-weight: 700; text-decoration: none; cursor: pointer; transition: background 0.18s ease, color 0.18s ease; }
  .btn-edit { background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); } .btn-edit:hover { background: var(--arc-chip-indigo-hover); }
  @media (max-width: 768px) { .page-header { flex-direction: column; } }

  @media (prefers-reduced-motion: reduce) {
    .spinner { animation: none; }
    .btn-create, .btn-action { transition: none; }
    .btn-create:hover { transform: none; }
  }
</style>
