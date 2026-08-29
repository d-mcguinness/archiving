<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { client } from '$lib/apollo';
  import { GET_TENANT, GET_ALL_USERS, GET_INTAKES_BY_TENANT_V2, GET_PRESERVATIONS_BY_TENANT, GET_RELEASES_BY_TENANT } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { authHeaders, API_BASE } from '$lib/api';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
    archiveId: string;
  }

  export let data: PageData;

  let archive: any = null;
  let tenant: any = null;
  let sips: any[] = [];
  let aips: any[] = [];
  let dips: any[] = [];
  let documents: any[] = [];
  let users: any[] = [];
  let loading = true;

  const GET_ARCHIVE = gql`
    query GetArchive($id: ID!) {
      getArchive(id: $id) {
        id
        title
        description
        status
        standard
        createdAt
        updatedAt
        ownerId
        assignedUsers { id name email }
      }
    }
  `;

  onMount(async () => {
    await Promise.all([
      loadArchive(),
      loadTenant(),
      loadIntakes(),
      loadPreservations(),
      loadReleases(),
      loadDocuments(),
      loadUsers(),
    ]);
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

  async function loadIntakes() {
    try {
      const result = await client.query({ query: GET_INTAKES_BY_TENANT_V2, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' });
      sips = result?.data?.getIntakesByTenantV2 || [];
    } catch (e) { console.error('Failed to load Intakes:', e); }
  }

  async function loadPreservations() {
    try {
      const result = await client.query({ query: GET_PRESERVATIONS_BY_TENANT, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' });
      aips = result?.data?.getPreservationsByTenant || [];
    } catch (e) { console.error('Failed to load Preservations:', e); }
  }

  async function loadReleases() {
    try {
      const result = await client.query({ query: GET_RELEASES_BY_TENANT, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' });
      dips = result?.data?.getReleasesByTenant || [];
    } catch (e) { console.error('Failed to load Releases:', e); }
  }

  async function loadDocuments() {
    try {
      const params = new URLSearchParams({ role: 'TENANT', tenantId: data.tenantId });
      const response = await fetch(`${API_BASE}/api/documents?${params}`, { headers: { ...authHeaders() } });
      if (response.ok) {
        const result = await response.json();
        if (result.success) documents = result.documents || [];
      }
    } catch (e) { console.error('Failed to load documents:', e); }
  }

  async function loadUsers() {
    try {
      const result = await client.query({ query: GET_ALL_USERS, fetchPolicy: 'network-only' });
      const allUsers = result?.data?.getAllUsers || [];
      // Filter to assigned users once archive loads
      users = allUsers;
    } catch (e) { console.error('Failed to load users:', e); }
  }

  function getStatusClass(status: string) { return status?.toLowerCase() || ''; }
  function formatDate(d: string) { return d ? new Date(d).toLocaleDateString() : '-'; }

  $: assignedUsers = archive?.assignedUsers || [];
  $: archiveDocuments = documents.filter((d: any) => d.archiveId?.toString() === data.archiveId);
  $: archiveIntakes = archive?.standard ? sips.filter((s: any) => s.standard === archive.standard) : sips;
  $: archivePreservations = archive?.standard ? aips.filter((a: any) => a.standard === archive.standard) : aips;
  $: archiveReleases = archive?.standard ? dips.filter((d: any) => d.standard === archive.standard) : dips;
</script>

<svelte:head>
  <title>{archive?.title || 'Archive'} - Arcana</title>
</svelte:head>

<div class="archive-detail">
  {#if loading}
    <div class="loading"><div class="spinner"></div><p>Loading archive...</p></div>
  {:else if !archive}
    <div class="error">Archive not found.</div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[
        { label: 'Archives', href: `/tenants/${data.tenantId}/archives` },
        { label: archive.title }
      ]}
    />

    <!-- Archive Header -->
    <div class="archive-header">
      <div>
        <span class="eyebrow">Archive</span>
        <h1>{archive.title}</h1>
        {#if archive.description}
          <p class="desc">{archive.description}</p>
        {/if}
        <div class="badges">
          <span class="badge status-{getStatusClass(archive.status)}">{archive.status}</span>
          <span class="badge standard">{archive.standard}</span>
        </div>
      </div>
      <div class="header-actions">
        <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/update" class="btn btn-edit">✏️ Edit</a>
        <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/extract" class="btn btn-extract">📥 Extract</a>
      </div>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/intakes" class="stat-card">
        <span class="stat-num">{archiveIntakes.length}</span>
        <span class="stat-label">Intakes</span>
      </a>
      <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/preservations" class="stat-card">
        <span class="stat-num">{archivePreservations.length}</span>
        <span class="stat-label">Preservations</span>
      </a>
      <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/releases" class="stat-card">
        <span class="stat-num">{archiveReleases.length}</span>
        <span class="stat-label">Releases</span>
      </a>
      <a href="/tenants/{data.tenantId}/users" class="stat-card">
        <span class="stat-num">{assignedUsers.length}</span>
        <span class="stat-label">Users</span>
      </a>
      <a href="/tenants/{data.tenantId}/documents" class="stat-card">
        <span class="stat-num">{archiveDocuments.length}</span>
        <span class="stat-label">Documents</span>
      </a>
    </div>

    <div class="grid-2col">
      <!-- Recent Intakes -->
      <div class="panel">
        <div class="panel-top">
          <h2>📦 Intakes</h2>
          <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/intakes" class="link">View All →</a>
        </div>
        {#if archiveIntakes.length === 0}
          <p class="muted">No Intakes yet</p>
        {:else}
          {#each archiveIntakes.slice(0, 5) as sip}
            <div class="list-row">
              <div class="list-info">
                <span class="list-title">{sip.title}</span>
                <span class="list-sub">{sip.standard} &middot; {formatDate(sip.createdAt)}</span>
              </div>
              <span class="badge status-{getStatusClass(sip.status)}">{sip.status}</span>
            </div>
          {/each}
        {/if}
      </div>

      <!-- Recent Preservations -->
      <div class="panel">
        <div class="panel-top">
          <h2>🏗️ Preservations</h2>
          <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/preservations" class="link">View All →</a>
        </div>
        {#if archivePreservations.length === 0}
          <p class="muted">No Preservations yet</p>
        {:else}
          {#each archivePreservations.slice(0, 5) as aip}
            <div class="list-row">
              <div class="list-info">
                <span class="list-title">{aip.title}</span>
                <span class="list-sub">{aip.standard} &middot; {formatDate(aip.createdAt)}</span>
              </div>
              <span class="badge status-{getStatusClass(aip.status)}">{aip.status}</span>
            </div>
          {/each}
        {/if}
      </div>

      <!-- Recent Releases -->
      <div class="panel">
        <div class="panel-top">
          <h2>📤 Releases</h2>
          <a href="/tenants/{data.tenantId}/archives/{data.archiveId}/releases" class="link">View All →</a>
        </div>
        {#if archiveReleases.length === 0}
          <p class="muted">No Releases yet</p>
        {:else}
          {#each archiveReleases.slice(0, 5) as dip}
            <div class="list-row">
              <div class="list-info">
                <span class="list-title">{dip.title}</span>
                <span class="list-sub">{dip.standard} &middot; {formatDate(dip.createdAt)}</span>
              </div>
              <span class="badge status-{getStatusClass(dip.status)}">{dip.status}</span>
            </div>
          {/each}
        {/if}
      </div>

      <!-- Assigned Users -->
      <div class="panel">
        <div class="panel-top">
          <h2>👥 Assigned Users</h2>
          <a href="/tenants/{data.tenantId}/users" class="link">View All →</a>
        </div>
        {#if assignedUsers.length === 0}
          <p class="muted">No users assigned</p>
        {:else}
          {#each assignedUsers as user}
            <a href="/tenants/{data.tenantId}/users/{user.id}" class="list-row list-row-link">
              <div class="list-info">
                <span class="list-title">{user.name}</span>
                <span class="list-sub">{user.email}</span>
              </div>
            </a>
          {/each}
        {/if}
      </div>
    </div>

    <!-- Documents -->
    <div class="panel">
      <div class="panel-top">
        <h2>📄 Documents ({archiveDocuments.length})</h2>
        <a href="/tenants/{data.tenantId}/documents" class="link">View All →</a>
      </div>
      {#if archiveDocuments.length === 0}
        <p class="muted">No documents linked to this archive</p>
      {:else}
        <div class="doc-grid">
          {#each archiveDocuments.slice(0, 8) as doc}
            <div class="doc-card">
              <span class="doc-title">{doc.title || doc.fileName}</span>
              <span class="doc-meta">{doc.contentType} &middot; {formatDate(doc.uploadedAt)}</span>
            </div>
          {/each}
        </div>
        {#if archiveDocuments.length > 8}
          <p class="muted" style="margin-top: 0.75rem;">+ {archiveDocuments.length - 8} more</p>
        {/if}
      {/if}
    </div>
  {/if}
</div>

<style>
  .archive-detail { max-width: 1100px; margin: 0 auto; padding: 2rem; }

  .loading { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; gap: 1rem; }
  .spinner { border: 4px solid var(--arc-line-strong); border-top: 4px solid var(--arc-indigo); border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .error { background: var(--arc-alert-red-bg); border: 1px solid var(--arc-alert-red-border); color: var(--arc-alert-red-ink); padding: 1rem; border-radius: 0.6rem; }

  .archive-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 2rem; }
  .archive-header h1 { margin: 0 0 0.35rem; color: var(--arc-ink); font-size: 1.85rem; }
  .desc { margin: 0 0 0.75rem; color: var(--arc-muted); font-size: 0.9rem; }
  .badges { display: flex; gap: 0.5rem; }

  .badge { display: inline-block; padding: 0.25rem 0.75rem; border-radius: 9999px; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase; }
  .status-active, .status-published { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); }
  .status-draft { background: var(--arc-chip-amber-bg); color: var(--arc-chip-amber-ink); }
  .status-archived { background: var(--arc-chip-slate-bg); color: var(--arc-chip-slate-ink); }
  .standard { background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); }

  .header-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }
  .btn { padding: 0.6rem 1.1rem; border: none; border-radius: 0.65rem; font-weight: 700; font-size: 0.85rem; cursor: pointer; text-decoration: none; transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, border-color 0.18s ease, color 0.18s ease; }
  .btn-edit { background: var(--arc-card); border: 1.5px solid var(--arc-line-strong); color: var(--arc-ink); }
  .btn-edit:hover { border-color: var(--arc-indigo); color: var(--arc-link); transform: translateY(-2px); }
  .btn-extract { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6); }
  .btn-extract:hover { background: linear-gradient(135deg, #4f46e5, #7c3aed); transform: translateY(-2px); }

  .stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
  .stat-card { background: var(--arc-card); border: 1px solid var(--arc-line); border-radius: 1rem; padding: 1.25rem; text-align: center; text-decoration: none; color: inherit; box-shadow: var(--arc-shadow-card); transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease; }
  .stat-card:hover { border-color: var(--arc-hover-border); transform: translateY(-4px); box-shadow: var(--arc-shadow-lift); }
  .stat-num { display: block; font-family: 'Space Grotesk', 'Inter', sans-serif; letter-spacing: -0.02em; font-size: 2rem; font-weight: 700; color: var(--arc-ink); line-height: 1; margin-bottom: 0.3rem; }
  .stat-label { font-size: 0.72rem; color: var(--arc-muted); font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; }

  .grid-2col { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.25rem; margin-bottom: 2rem; }

  .panel { background: var(--arc-card); border: 1px solid var(--arc-line); border-radius: 1rem; padding: 1.5rem; box-shadow: var(--arc-shadow-card); }
  .panel-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
  .panel h2 { margin: 0; font-size: 1rem; font-weight: 700; color: var(--arc-ink); }
  .link { font-size: 0.8rem; color: var(--arc-link); text-decoration: none; font-weight: 600; transition: color 0.18s ease; }
  .link:hover { color: var(--arc-eyebrow-ink); }

  .list-row { display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0.75rem; background: var(--arc-card-2); border-radius: 0.5rem; margin-bottom: 0.4rem; }
  .list-row-link { text-decoration: none; color: inherit; transition: background 0.18s ease; }
  .list-row-link:hover { background: var(--arc-chip-soft-indigo-bg); }
  .list-info { display: flex; flex-direction: column; gap: 0.1rem; }
  .list-title { font-weight: 600; color: var(--arc-ink); font-size: 0.875rem; }
  .list-sub { color: var(--arc-muted); font-size: 0.75rem; }
  .muted { color: var(--arc-faint); font-size: 0.85rem; margin: 0; }

  .doc-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 0.5rem; }
  .doc-card { padding: 0.6rem 0.75rem; background: var(--arc-card-2); border-radius: 0.375rem; display: flex; flex-direction: column; gap: 0.15rem; }
  .doc-title { font-weight: 500; color: var(--arc-ink); font-size: 0.85rem; }
  .doc-meta { color: var(--arc-faint); font-size: 0.7rem; }

  @media (max-width: 768px) { .archive-header { flex-direction: column; } .grid-2col { grid-template-columns: 1fr; } }

  @media (prefers-reduced-motion: reduce) {
    .spinner { animation: none; }
    .btn, .stat-card, .link, .list-row-link { transition: none; }
    .btn:hover, .stat-card:hover { transform: none; }
  }
</style>
