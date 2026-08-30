<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_PRESERVATIONS_BY_TENANT, GET_ALL_USERS, GET_TENANT } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
    archiveId: string;
  }

  export let data: PageData;

  let archive: any = null;
  let aips: any[] = [];
  let users: any[] = [];
  let tenant: any = null;
  let loading = true;

  const GET_ARCHIVE = gql`
    query GetArchive($id: ID!) {
      getArchive(id: $id) { id title standard }
    }
  `;

  onMount(async () => {
    await Promise.all([loadArchive(), loadTenant(), loadPreservations(), loadUsers()]);
    loading = false;
  });

  async function loadArchive() {
    try {
      const result = await client.query({ query: GET_ARCHIVE, variables: { id: data.archiveId }, fetchPolicy: 'network-only' });
      archive = result?.data?.getArchive || null;
    } catch (e) { console.error('Failed to load archive:', e); }
  }

  async function loadTenant() {
    try {
      const result = await client.query({ query: GET_TENANT, variables: { id: data.tenantId }, fetchPolicy: 'network-only' });
      tenant = result?.data?.getTenant || null;
    } catch (e) { console.error('Failed to load tenant:', e); }
  }

  async function loadPreservations() {
    try {
      const result = await client.query({ query: GET_PRESERVATIONS_BY_TENANT, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' });
      aips = result?.data?.getPreservationsByTenant || [];
    } catch (e) {
      toasts.error('Failed to load Preservations');
    }
  }

  async function loadUsers() {
    try {
      const result = await client.query({ query: GET_ALL_USERS });
      users = result?.data?.getAllUsers || [];
    } catch (e) { console.error('Failed to load users:', e); }
  }

  function getUserName(userId: string) {
    const user = users.find((u: any) => u.id === userId);
    return user ? user.name : `User #${userId}`;
  }

  function getStatusClass(status: string) { return status?.toLowerCase() || ''; }

  $: filteredPreservations = archive?.standard
    ? aips.filter((a: any) => a.standard === archive.standard)
    : aips;
</script>

<svelte:head>
  <title>Preservations - {archive?.title || 'Archive'} - Arcana</title>
</svelte:head>

<div class="page">
  {#if loading}
    <div class="loading"><div class="spinner"></div><p>Loading...</p></div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[
        { label: 'Archives', href: `/tenants/${data.tenantId}/archives` },
        { label: archive?.title || 'Archive', href: `/tenants/${data.tenantId}/archives/${data.archiveId}` },
        { label: 'Preservations' }
      ]}
    />

    <div class="page-header">
      <div>
        <span class="eyebrow">Preservation</span>
        <h1>🏗️ Preservation packages</h1>
        {#if archive}
          <p class="subtitle">Archive: {archive.title} ({archive.standard})</p>
        {/if}
      </div>
      <a href="/preservation/create" class="btn-create btn-primary">+ Create Preservation</a>
    </div>

    {#if filteredPreservations.length === 0}
      <div class="empty">
        <h3>No Preservations found for this archive</h3>
        <p>Create an Preservation with the {archive?.standard || ''} standard to get started.</p>
      </div>
    {:else}
      <div class="table-container table-card">
        <table class="data-table arc-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Title</th>
              <th>Standard</th>
              <th>Status</th>
              <th>Owner</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {#each filteredPreservations as aip (aip.id)}
              <tr>
                <td class="id-cell">{aip.id}</td>
                <td class="title-cell">
                  <div class="aip-title">{aip.title}</div>
                  {#if aip.description}
                    <div class="aip-desc">{aip.description}</div>
                  {/if}
                </td>
                <td><span class="badge standard indigo">{aip.standard}</span></td>
                <td><span class="badge status-{getStatusClass(aip.status)}">{aip.status}</span></td>
                <td class="owner-cell">{getUserName(aip.ownerId)}</td>
                <td class="date-cell">{new Date(aip.createdAt).toLocaleDateString()}</td>
                <td class="actions-cell">
                  <a href="/preservation/edit/{aip.id}" class="btn-action btn-edit btn-chip indigo">✏️ Edit</a>
                </td>
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
  /* .loading + .spinner come from the global kit; only the tall centered
     column layout is page-specific. */
  .loading { flex-direction: column; min-height: 400px; gap: 1rem; }

  .page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 2rem; gap: 1rem; }
  .page-header h1 { margin: 0 0 0.35rem; color: var(--arc-ink); font-size: 1.75rem; }
  .subtitle { margin: 0; color: var(--arc-muted); font-size: 0.9rem; }

  /* .btn-primary from the kit; this header action runs a compact size. */
  .btn-create { padding: 0.65rem 1.25rem; font-size: 0.875rem; white-space: nowrap; }

  .empty { text-align: center; padding: 4rem 2rem; background: var(--arc-card); border-radius: 1rem; border: 1px solid var(--arc-line); box-shadow: var(--arc-shadow-card); }
  .empty h3 { margin: 0 0 0.5rem; color: var(--arc-ink); }
  .empty p { margin: 0; color: var(--arc-muted); }

  /* .table-card + .arc-table from the kit; this page adds the indigo accent
     rail, horizontal scrolling and a roomier cell scale. */
  .table-container { overflow-x: auto; border-top: 3px solid #6366f1; }
  .data-table { min-width: 800px; }
  .data-table th, .data-table td { padding: 1rem; vertical-align: baseline; }

  .id-cell { color: var(--arc-muted); font-family: monospace; font-size: 0.875rem; width: 60px; }
  .title-cell { min-width: 200px; font-size: 1rem; }
  .aip-title { font-weight: 600; color: var(--arc-ink); }
  .aip-desc { font-size: 0.825rem; color: var(--arc-muted); margin-top: 0.15rem; }
  .owner-cell { color: var(--arc-muted); font-size: 0.875rem; white-space: nowrap; }
  .date-cell { color: var(--arc-muted); font-size: 0.875rem; white-space: nowrap; }
  .actions-cell { white-space: nowrap; }

  /* Pill geometry + the indigo standard tint come from the global .badge kit;
     these Preservation status names are page-specific. */
  .status-draft { background: var(--arc-chip-amber-bg); color: var(--arc-chip-amber-ink); }
  .status-building { background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); }
  .status-validated { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); }
  .status-stored { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); }
  .status-rejected { background: var(--arc-chip-red-bg); color: var(--arc-chip-red-ink); }

  /* Chip tint comes from .btn-chip.indigo; row actions run smaller than the kit. */
  .btn-action { display: inline-block; padding: 0.375rem 0.75rem; margin: 0 0.25rem; font-size: 0.75rem; font-weight: 700; }

  @media (max-width: 768px) { .page-header { flex-direction: column; } }

  @media (prefers-reduced-motion: reduce) {
    .btn-create, .btn-action { transition: none; }
    .btn-create:hover { transform: none; }
  }
</style>
