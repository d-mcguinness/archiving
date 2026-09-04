<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { authHeaders, API_BASE } from '$lib/api';
  import { GET_DASHBOARD_STATS, GET_ALL_TENANTS, GET_ALL_USERS, GET_ALL_INTAKES_V2, GET_ALL_PRESERVATIONS, GET_ALL_RELEASES } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let loading = true;
  let stats = {
    totalUsers: 0,
    totalTenants: 0,
    totalArchives: 0,
    activeArchives: 0,
    draftArchives: 0,
    archivedArchives: 0,
  };

  let totalIntakes = 0;
  let totalPreservations = 0;
  let totalReleases = 0;
  let totalDocuments = 0;

  let tenants: any[] = [];
  let recentUsers: any[] = [];
  let recentIntakes: any[] = [];

  // Status breakdowns
  let sipStatuses: Record<string, number> = {};
  let aipStatuses: Record<string, number> = {};
  let dipStatuses: Record<string, number> = {};
  let tenantPlans: Record<string, number> = {};
  let tenantStatusCounts: Record<string, number> = {};

  onMount(async () => {
    const authState = get(auth);
    if (authState.role !== 'ADMIN') {
      goto('/');
      return;
    }
    await loadAll();
  });

  async function loadAll() {
    loading = true;
    try {
      const [statsResult, tenantsResult, usersResult, sipsResult, aipsResult, dipsResult, docsResult] = await Promise.all([
        client.query({ query: GET_DASHBOARD_STATS, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_TENANTS, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_USERS, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_INTAKES_V2, fetchPolicy: 'network-only' }).catch(() => ({ data: { getAllIntakesV2: [] } })),
        client.query({ query: GET_ALL_PRESERVATIONS, fetchPolicy: 'network-only' }).catch(() => ({ data: { getAllPreservations: [] } })),
        client.query({ query: GET_ALL_RELEASES, fetchPolicy: 'network-only' }).catch(() => ({ data: { getAllReleases: [] } })),
        fetch(`${API_BASE}/api/documents?role=ADMIN`, { headers: { ...authHeaders() } }).then(r => r.json()).catch(() => ({ documents: [] })),
      ]);

      const s = statsResult?.data?.getDashboardStats;
      if (s) {
        stats = {
          totalUsers: s.totalUsers || 0,
          totalTenants: s.totalTenants || 0,
          totalArchives: s.totalArchives || 0,
          activeArchives: s.activeArchives || 0,
          draftArchives: s.draftArchives || 0,
          archivedArchives: s.archivedArchives || 0,
        };
      }

      tenants = tenantsResult?.data?.getAllTenants || [];
      recentUsers = (usersResult?.data?.getAllUsers || []).slice(0, 5);

      const sips = sipsResult?.data?.getAllIntakesV2 || [];
      totalIntakes = sips.length;
      recentIntakes = sips.slice(0, 5);
      sipStatuses = countBy(sips, 'status');

      const aips = aipsResult?.data?.getAllPreservations || [];
      totalPreservations = aips.length;
      aipStatuses = countBy(aips, 'status');

      const dips = dipsResult?.data?.getAllReleases || [];
      totalReleases = dips.length;
      dipStatuses = countBy(dips, 'status');

      totalDocuments = docsResult?.documents?.length || docsResult?.count || 0;

      tenantPlans = countBy(tenants, 'plan');
      tenantStatusCounts = countBy(tenants, 'status');
    } catch (e) {
      console.error('Dashboard load error:', e);
      toasts.error('Failed to load some dashboard data');
    } finally {
      loading = false;
    }
  }

  function countBy(arr: any[], key: string): Record<string, number> {
    const counts: Record<string, number> = {};
    for (const item of arr) {
      const val = item[key] || 'UNKNOWN';
      counts[val] = (counts[val] || 0) + 1;
    }
    return counts;
  }

  function formatDate(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString();
  }

  function statusColor(status: string): string {
    const map: Record<string, string> = {
      DRAFT: '#f59e0b', SUBMITTED: '#6366f1', VALIDATED: '#06b6d4',
      ACCEPTED: '#10b981', REJECTED: '#ef4444', ACTIVE: '#10b981',
      ARCHIVED: '#64748b', PUBLISHED: '#10b981', PENDING: '#f59e0b',
    };
    return map[status] || '#94a3b8';
  }

  function planColor(plan: string): string {
    const map: Record<string, string> = {
      FREE: '#94a3b8', BASIC: '#06b6d4', PROFESSIONAL: '#6366f1',
      ENTERPRISE: '#8b5cf6', CUSTOM: '#f59e0b',
    };
    return map[plan] || '#94a3b8';
  }

  function tenantStatusColor(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: '#10b981', INACTIVE: '#64748b', SUSPENDED: '#ef4444',
      TRIAL: '#6366f1', PENDING_ACTIVATION: '#f59e0b',
    };
    return map[status] || '#94a3b8';
  }
</script>

<svelte:head>
  <title>Admin Dashboard - Arcana</title>
</svelte:head>

<div class="admin-page">
  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading dashboard...</p>
    </div>
  {:else}
    <Breadcrumb items={[{ label: 'Dashboard' }]} />
    <div class="page-header">
      <span class="eyebrow">Admin console</span>
      <h1>Admin Dashboard</h1>
      <p class="subtitle">System-wide overview and statistics</p>
    </div>

    <!-- Primary Stats -->
    <div class="stats-row">
      <a href="/admin/users" class="stat-card">
        <span class="stat-num">{stats.totalUsers}</span>
        <span class="stat-label">Users</span>
      </a>
      <a href="/admin/tenants" class="stat-card">
        <span class="stat-num">{stats.totalTenants}</span>
        <span class="stat-label">Tenants</span>
      </a>
      <a href="/admin/archives" class="stat-card">
        <span class="stat-num">{stats.totalArchives}</span>
        <span class="stat-label">Archives</span>
      </a>
      <a href="/admin/intake" class="stat-card">
        <span class="stat-num">{totalIntakes}</span>
        <span class="stat-label">Intakes</span>
      </a>
      <a href="/admin/preservation" class="stat-card">
        <span class="stat-num">{totalPreservations}</span>
        <span class="stat-label">Preservations</span>
      </a>
      <a href="/admin/release" class="stat-card">
        <span class="stat-num">{totalReleases}</span>
        <span class="stat-label">Releases</span>
      </a>
      <a href="/admin/documents" class="stat-card">
        <span class="stat-num">{totalDocuments}</span>
        <span class="stat-label">Documents</span>
      </a>
    </div>

    <div class="grid-2col">
      <!-- Archive Status Breakdown -->
      <div class="panel">
        <h2>Archive Status</h2>
        <div class="breakdown-list">
          <div class="breakdown-item">
            <span class="breakdown-dot active"></span>
            <span class="breakdown-label">Active</span>
            <span class="breakdown-val">{stats.activeArchives}</span>
            <div class="breakdown-bar"><div class="bar-fill active" style="width: {stats.totalArchives ? (stats.activeArchives / stats.totalArchives * 100) : 0}%"></div></div>
          </div>
          <div class="breakdown-item">
            <span class="breakdown-dot draft"></span>
            <span class="breakdown-label">Draft</span>
            <span class="breakdown-val">{stats.draftArchives}</span>
            <div class="breakdown-bar"><div class="bar-fill draft" style="width: {stats.totalArchives ? (stats.draftArchives / stats.totalArchives * 100) : 0}%"></div></div>
          </div>
          <div class="breakdown-item">
            <span class="breakdown-dot archived"></span>
            <span class="breakdown-label">Archived</span>
            <span class="breakdown-val">{stats.archivedArchives}</span>
            <div class="breakdown-bar"><div class="bar-fill archived" style="width: {stats.totalArchives ? (stats.archivedArchives / stats.totalArchives * 100) : 0}%"></div></div>
          </div>
        </div>
      </div>

      <!-- Intake Status Breakdown -->
      <div class="panel">
        <h2>Intake Status</h2>
        <div class="breakdown-list">
          {#each Object.entries(sipStatuses) as [status, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {statusColor(status)}"></span>
              <span class="breakdown-label">{status}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {totalIntakes ? (count / totalIntakes * 100) : 0}%; background: {statusColor(status)}"></div></div>
            </div>
          {/each}
          {#if Object.keys(sipStatuses).length === 0}
            <p class="muted">No Intakes yet</p>
          {/if}
        </div>
      </div>

      <!-- Preservation Status Breakdown -->
      <div class="panel">
        <h2>Preservation Status</h2>
        <div class="breakdown-list">
          {#each Object.entries(aipStatuses) as [status, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {statusColor(status)}"></span>
              <span class="breakdown-label">{status}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {totalPreservations ? (count / totalPreservations * 100) : 0}%; background: {statusColor(status)}"></div></div>
            </div>
          {/each}
          {#if Object.keys(aipStatuses).length === 0}
            <p class="muted">No Preservations yet</p>
          {/if}
        </div>
      </div>

      <!-- Release Status Breakdown -->
      <div class="panel">
        <h2>Release Status</h2>
        <div class="breakdown-list">
          {#each Object.entries(dipStatuses) as [status, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {statusColor(status)}"></span>
              <span class="breakdown-label">{status}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {totalReleases ? (count / totalReleases * 100) : 0}%; background: {statusColor(status)}"></div></div>
            </div>
          {/each}
          {#if Object.keys(dipStatuses).length === 0}
            <p class="muted">No Releases yet</p>
          {/if}
        </div>
      </div>

      <!-- Tenant Plans -->
      <div class="panel">
        <h2>Tenant Plans</h2>
        <div class="breakdown-list">
          {#each Object.entries(tenantPlans) as [plan, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {planColor(plan)}"></span>
              <span class="breakdown-label">{plan}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {tenants.length ? (count / tenants.length * 100) : 0}%; background: {planColor(plan)}"></div></div>
            </div>
          {/each}
        </div>
      </div>

      <!-- Tenant Status -->
      <div class="panel">
        <h2>Tenant Status</h2>
        <div class="breakdown-list">
          {#each Object.entries(tenantStatusCounts) as [status, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {tenantStatusColor(status)}"></span>
              <span class="breakdown-label">{status}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {tenants.length ? (count / tenants.length * 100) : 0}%; background: {tenantStatusColor(status)}"></div></div>
            </div>
          {/each}
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="grid-2col">
      <div class="panel">
        <div class="panel-header">
          <h2>Recent Users</h2>
          <a href="/admin/users" class="panel-link">View All →</a>
        </div>
        {#if recentUsers.length === 0}
          <p class="muted">No users yet</p>
        {:else}
          <div class="list">
            {#each recentUsers as user}
              <div class="list-row">
                <div class="list-info">
                  <span class="list-title">{user.name}</span>
                  <span class="list-sub">{user.email}</span>
                </div>
                <span class="list-meta">ID: {user.id}</span>
              </div>
            {/each}
          </div>
        {/if}
      </div>

      <div class="panel">
        <div class="panel-header">
          <h2>Recent Intakes</h2>
          <a href="/admin/intake" class="panel-link">View All →</a>
        </div>
        {#if recentIntakes.length === 0}
          <p class="muted">No Intakes yet</p>
        {:else}
          <div class="list">
            {#each recentIntakes as sip}
              <div class="list-row">
                <div class="list-info">
                  <span class="list-title">{sip.title}</span>
                  <span class="list-sub">{sip.standard}</span>
                </div>
                <span class="list-badge" style="background: {statusColor(sip.status)}; color: white;">{sip.status}</span>
              </div>
            {/each}
          </div>
        {/if}
      </div>
    </div>

    <!-- Quick Nav -->
    <div class="quick-nav">
      <a href="/admin/tenants" class="qn-card">🏢 Manage Tenants</a>
      <a href="/admin/users" class="qn-card">👥 Manage Users</a>
      <a href="/admin/archives" class="qn-card">📁 All Archives</a>
      <a href="/admin/intake" class="qn-card">📦 All Intakes</a>
      <a href="/admin/preservation" class="qn-card">🏗️ All Preservations</a>
      <a href="/admin/release" class="qn-card">📤 All Releases</a>
      <a href="/admin/documents" class="qn-card">📄 All Documents</a>
      <a href="/ingest" class="qn-card">Ingest Standards</a>
    </div>
  {/if}
</div>


<style>
  .admin-page {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
  }

  /* .loading and .spinner come from the global kit (app.css); only the
     column stack under the spinner is page-specific. */
  .loading {
    flex-direction: column; min-height: 400px; gap: 1rem;
  }

  .page-header { margin-bottom: 2rem; }
  .page-header h1 { margin: 0 0 0.25rem; color: var(--arc-ink); font-size: 2rem; font-weight: 800; }
  .subtitle { margin: 0; color: var(--arc-muted); font-size: 1rem; }

  /* Stats row */
  .stats-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
    gap: 1rem;
    margin-bottom: 2rem;
  }

  .stat-card {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 1.25rem;
    text-align: center;
    text-decoration: none;
    color: inherit;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  }

  a.stat-card:hover {
    border-color: var(--arc-hover-border);
    transform: translateY(-4px);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  .stat-num {
    display: block;
    font-family: var(--arc-font-display, 'Space Grotesk', 'Inter', sans-serif);
    letter-spacing: -0.02em;
    font-size: 2rem;
    font-weight: 800;
    color: var(--arc-ink);
    line-height: 1;
    margin-bottom: 0.35rem;
  }

  .stat-label {
    font-size: 0.75rem;
    color: var(--arc-muted);
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  /* Grid */
  .grid-2col {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
    gap: 1.25rem;
    margin-bottom: 2rem;
  }

  .panel {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 1.5rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .panel h2 {
    margin: 0 0 1rem;
    font-size: 1rem;
    font-weight: 700;
    color: var(--arc-ink);
  }

  .panel-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 1rem;
  }
  .panel-header h2 { margin: 0; }

  .panel-link {
    font-size: 0.8rem; color: var(--arc-link); text-decoration: none; font-weight: 600;
  }
  .panel-link:hover { color: var(--arc-indigo-deep, #4f46e5); }

  /* Breakdown */
  .breakdown-list { display: flex; flex-direction: column; gap: 0.75rem; }

  .breakdown-item {
    display: grid;
    grid-template-columns: 0.75rem 1fr auto 6rem;
    align-items: center;
    gap: 0.6rem;
  }

  .breakdown-dot {
    width: 0.6rem; height: 0.6rem; border-radius: 50%;
  }
  .breakdown-dot.active { background: #10b981; }
  .breakdown-dot.draft { background: #f59e0b; }
  .breakdown-dot.archived { background: #64748b; }

  .breakdown-label { font-size: 0.85rem; color: var(--arc-body); }
  .breakdown-val { font-size: 0.85rem; font-weight: 700; color: var(--arc-ink); text-align: right; }

  .breakdown-bar {
    height: 0.375rem; background: var(--arc-card-2); border-radius: 1rem; overflow: hidden;
  }
  .bar-fill { height: 100%; border-radius: 1rem; transition: width 0.5s ease; }
  .bar-fill.active { background: #10b981; }
  .bar-fill.draft { background: #f59e0b; }
  .bar-fill.archived { background: #64748b; }

  .muted { color: var(--arc-faint); font-size: 0.85rem; margin: 0; }

  /* Lists */
  .list { display: flex; flex-direction: column; gap: 0.5rem; }

  .list-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 0.6rem 0.75rem; background: var(--arc-card-2); border-radius: 0.375rem;
  }

  .list-info { display: flex; flex-direction: column; gap: 0.1rem; }
  .list-title { font-weight: 600; color: var(--arc-ink); font-size: 0.875rem; }
  .list-sub { color: var(--arc-muted); font-size: 0.75rem; }
  .list-meta { color: var(--arc-faint); font-size: 0.75rem; font-family: monospace; }

  .list-badge {
    padding: 0.15rem 0.6rem; border-radius: 9999px;
    font-size: 0.65rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.04em;
  }

  /* Quick nav */
  .quick-nav {
    display: flex; flex-wrap: wrap; gap: 0.75rem;
  }

  .qn-card {
    padding: 0.6rem 1.25rem;
    background: var(--arc-card, #fff); border: 1px solid var(--arc-line-strong, #e2e8f0); border-radius: 0.65rem;
    text-decoration: none; color: var(--arc-body); font-weight: 600; font-size: 0.85rem;
    transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, transform 0.2s ease;
  }

  .qn-card:hover {
    border-color: var(--arc-indigo, #6366f1); background: var(--arc-chip-soft-indigo-bg); color: var(--arc-link);
    transform: translateY(-2px);
  }

  @media (prefers-reduced-motion: reduce) {
    .qn-card:hover, a.stat-card:hover { transform: none; }
  }

  @media (max-width: 768px) {
    .stats-row { grid-template-columns: repeat(auto-fit, minmax(100px, 1fr)); }
    .grid-2col { grid-template-columns: 1fr; }
  }

  :global(html[data-theme='dark']) .panel-link:hover { color: #c7d2fe; }
</style>
