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
  .spinner { border: 4px solid #f3f4f6; border-top: 4px solid #ec4899; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem; gap: 1rem; }
  .page-header h1 { margin: 0 0 0.35rem; color: #1e293b; font-size: 1.75rem; }
  .subtitle { margin: 0; color: #64748b; font-size: 0.9rem; }
  .btn-create { padding: 0.65rem 1.25rem; background: #ec4899; color: white; border-radius: 0.5rem; text-decoration: none; font-weight: 600; font-size: 0.875rem; transition: background 0.2s; white-space: nowrap; }
  .btn-create:hover { background: #db2777; }
  .empty { text-align: center; padding: 4rem 2rem; background: white; border-radius: 0.75rem; border: 1px solid #e2e8f0; }
  .empty h3 { margin: 0 0 0.5rem; color: #1e293b; } .empty p { margin: 0; color: #64748b; }
  .table-container { background: white; border-radius: 0.75rem; overflow-x: auto; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e2e8f0; }
  .data-table { width: 100%; border-collapse: collapse; min-width: 800px; }
  .data-table thead { background: #fdf2f8; border-bottom: 2px solid #fbcfe8; }
  .data-table th { padding: 1rem; text-align: left; font-weight: 600; color: #9d174d; font-size: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; }
  .data-table td { padding: 1rem; border-bottom: 1px solid #e2e8f0; }
  .data-table tbody tr:last-child td { border-bottom: none; }
  .data-table tbody tr:hover { background: #fdf2f8; }
  .id-cell { color: #64748b; font-family: monospace; font-size: 0.875rem; width: 60px; }
  .title-cell { min-width: 200px; } .item-title { font-weight: 600; color: #1e293b; } .item-desc { font-size: 0.825rem; color: #64748b; margin-top: 0.15rem; }
  .owner-cell, .date-cell { color: #64748b; font-size: 0.875rem; white-space: nowrap; }
  .actions-cell { white-space: nowrap; }
  .badge { display: inline-block; padding: 0.2rem 0.6rem; border-radius: 0.25rem; font-size: 0.7rem; font-weight: 600; text-transform: uppercase; }
  .standard { background: #fce7f3; color: #9d174d; }
  .status-draft { background: #fef3c7; color: #92400e; } .status-submitted { background: #dbeafe; color: #1e40af; } .status-validated { background: #d1fae5; color: #065f46; } .status-accepted { background: #dcfce7; color: #166534; } .status-rejected { background: #fee2e2; color: #991b1b; }
  .btn-action { display: inline-block; padding: 0.375rem 0.75rem; margin: 0 0.25rem; border: none; border-radius: 0.375rem; font-size: 0.75rem; font-weight: 600; text-decoration: none; cursor: pointer; transition: all 0.2s; }
  .btn-edit { background: #dbeafe; color: #1e40af; } .btn-edit:hover { background: #bfdbfe; }
  @media (max-width: 768px) { .page-header { flex-direction: column; } }
</style>
