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
  .spinner { border: 4px solid #f3f4f6; border-top: 4px solid #06b6d4; border-radius: 50%; width: 40px; height: 40px; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .error { background: #fee2e2; color: #991b1b; padding: 1rem; border-radius: 0.5rem; }

  .archive-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 2rem; }
  .archive-header h1 { margin: 0 0 0.35rem; color: #0f172a; font-size: 1.75rem; }
  .desc { margin: 0 0 0.75rem; color: #64748b; font-size: 0.9rem; }
  .badges { display: flex; gap: 0.5rem; }

  .badge { display: inline-block; padding: 0.2rem 0.6rem; border-radius: 0.25rem; font-size: 0.7rem; font-weight: 600; text-transform: uppercase; }
  .status-active, .status-published { background: #dcfce7; color: #166534; }
  .status-draft { background: #fef3c7; color: #92400e; }
  .status-archived { background: #f3f4f6; color: #4b5563; }
  .standard { background: #eef2ff; color: #3730a3; }

  .header-actions { display: flex; gap: 0.5rem; flex-shrink: 0; }
  .btn { padding: 0.6rem 1.1rem; border: none; border-radius: 0.5rem; font-weight: 600; font-size: 0.85rem; cursor: pointer; text-decoration: none; transition: background 0.2s; }
  .btn-edit { background: #dbeafe; color: #1e40af; }
  .btn-edit:hover { background: #bfdbfe; }
  .btn-extract { background: #dcfce7; color: #166534; }
  .btn-extract:hover { background: #bbf7d0; }

  .stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
  .stat-card { background: white; border: 1px solid #e2e8f0; border-radius: 0.75rem; padding: 1.25rem; text-align: center; text-decoration: none; color: inherit; transition: border-color 0.2s, transform 0.2s, box-shadow 0.2s; }
  .stat-card:hover { border-color: #3b82f6; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
  .stat-num { display: block; font-size: 2rem; font-weight: 800; color: #0f172a; line-height: 1; margin-bottom: 0.3rem; }
  .stat-label { font-size: 0.75rem; color: #64748b; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; }

  .grid-2col { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 1.25rem; margin-bottom: 2rem; }

  .panel { background: white; border: 1px solid #e2e8f0; border-radius: 0.75rem; padding: 1.5rem; }
  .panel-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
  .panel h2 { margin: 0; font-size: 1rem; font-weight: 700; color: #1e293b; }
  .link { font-size: 0.8rem; color: #3b82f6; text-decoration: none; font-weight: 600; }
  .link:hover { color: #2563eb; }

  .list-row { display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0.75rem; background: #f8fafc; border-radius: 0.375rem; margin-bottom: 0.4rem; }
  .list-row-link { text-decoration: none; color: inherit; transition: background 0.15s; }
  .list-row-link:hover { background: #eff6ff; }
  .list-info { display: flex; flex-direction: column; gap: 0.1rem; }
  .list-title { font-weight: 600; color: #1e293b; font-size: 0.875rem; }
  .list-sub { color: #64748b; font-size: 0.75rem; }
  .muted { color: #94a3b8; font-size: 0.85rem; margin: 0; }

  .doc-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 0.5rem; }
  .doc-card { padding: 0.6rem 0.75rem; background: #f8fafc; border-radius: 0.375rem; display: flex; flex-direction: column; gap: 0.15rem; }
  .doc-title { font-weight: 500; color: #1e293b; font-size: 0.85rem; }
  .doc-meta { color: #94a3b8; font-size: 0.7rem; }

  @media (max-width: 768px) { .archive-header { flex-direction: column; } .grid-2col { grid-template-columns: 1fr; } }
</style>
