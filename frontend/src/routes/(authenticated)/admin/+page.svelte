<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { authHeaders } from '$lib/api';
  import { GET_DASHBOARD_STATS, GET_ALL_TENANTS, GET_ALL_USERS, GET_ALL_SIPS_V2, GET_ALL_AIPS, GET_ALL_DIPS } from '$lib/graphql/queries';
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

  let totalSips = 0;
  let totalAips = 0;
  let totalDips = 0;
  let totalDocuments = 0;

  let tenants: any[] = [];
  let recentUsers: any[] = [];
  let recentSips: any[] = [];

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
        client.query({ query: GET_ALL_SIPS_V2, fetchPolicy: 'network-only' }).catch(() => ({ data: { getAllSipsV2: [] } })),
        client.query({ query: GET_ALL_AIPS, fetchPolicy: 'network-only' }).catch(() => ({ data: { getAllAips: [] } })),
        client.query({ query: GET_ALL_DIPS, fetchPolicy: 'network-only' }).catch(() => ({ data: { getAllDips: [] } })),
        fetch('http://localhost:2020/api/documents?role=ADMIN', { headers: { ...authHeaders() } }).then(r => r.json()).catch(() => ({ documents: [] })),
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

      const sips = sipsResult?.data?.getAllSipsV2 || [];
      totalSips = sips.length;
      recentSips = sips.slice(0, 5);
      sipStatuses = countBy(sips, 'status');

      const aips = aipsResult?.data?.getAllAips || [];
      totalAips = aips.length;
      aipStatuses = countBy(aips, 'status');

      const dips = dipsResult?.data?.getAllDips || [];
      totalDips = dips.length;
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
      DRAFT: '#f59e0b', SUBMITTED: '#3b82f6', VALIDATED: '#06b6d4',
      ACCEPTED: '#10b981', REJECTED: '#ef4444', ACTIVE: '#10b981',
      ARCHIVED: '#64748b', PUBLISHED: '#10b981', PENDING: '#f59e0b',
    };
    return map[status] || '#94a3b8';
  }

  function planColor(plan: string): string {
    const map: Record<string, string> = {
      FREE: '#9ca3af', BASIC: '#3b82f6', PROFESSIONAL: '#6366f1',
      ENTERPRISE: '#8b5cf6', CUSTOM: '#f59e0b',
    };
    return map[plan] || '#94a3b8';
  }

  function tenantStatusColor(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: '#10b981', INACTIVE: '#6b7280', SUSPENDED: '#ef4444',
      TRIAL: '#3b82f6', PENDING_ACTIVATION: '#f59e0b',
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
      <div class="stat-card">
        <span class="stat-num">{totalSips}</span>
        <span class="stat-label">SIPs</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{totalAips}</span>
        <span class="stat-label">AIPs</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{totalDips}</span>
        <span class="stat-label">DIPs</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{totalDocuments}</span>
        <span class="stat-label">Documents</span>
      </div>
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

      <!-- SIP Status Breakdown -->
      <div class="panel">
        <h2>SIP Status</h2>
        <div class="breakdown-list">
          {#each Object.entries(sipStatuses) as [status, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {statusColor(status)}"></span>
              <span class="breakdown-label">{status}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {totalSips ? (count / totalSips * 100) : 0}%; background: {statusColor(status)}"></div></div>
            </div>
          {/each}
          {#if Object.keys(sipStatuses).length === 0}
            <p class="muted">No SIPs yet</p>
          {/if}
        </div>
      </div>

      <!-- AIP Status Breakdown -->
      <div class="panel">
        <h2>AIP Status</h2>
        <div class="breakdown-list">
          {#each Object.entries(aipStatuses) as [status, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {statusColor(status)}"></span>
              <span class="breakdown-label">{status}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {totalAips ? (count / totalAips * 100) : 0}%; background: {statusColor(status)}"></div></div>
            </div>
          {/each}
          {#if Object.keys(aipStatuses).length === 0}
            <p class="muted">No AIPs yet</p>
          {/if}
        </div>
      </div>

      <!-- DIP Status Breakdown -->
      <div class="panel">
        <h2>DIP Status</h2>
        <div class="breakdown-list">
          {#each Object.entries(dipStatuses) as [status, count]}
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: {statusColor(status)}"></span>
              <span class="breakdown-label">{status}</span>
              <span class="breakdown-val">{count}</span>
              <div class="breakdown-bar"><div class="bar-fill" style="width: {totalDips ? (count / totalDips * 100) : 0}%; background: {statusColor(status)}"></div></div>
            </div>
          {/each}
          {#if Object.keys(dipStatuses).length === 0}
            <p class="muted">No DIPs yet</p>
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
          <h2>Recent SIPs</h2>
          <a href="/admin/sip" class="panel-link">View All →</a>
        </div>
        {#if recentSips.length === 0}
          <p class="muted">No SIPs yet</p>
        {:else}
          <div class="list">
            {#each recentSips as sip}
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
      <a href="/admin/sip" class="qn-card">📦 All SIPs</a>
      <a href="/admin/aip" class="qn-card">🏗️ All AIPs</a>
      <a href="/admin/dip" class="qn-card">📤 All DIPs</a>
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

  .loading {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; gap: 1rem;
  }
  .spinner {
    border: 4px solid #f3f4f6; border-top: 4px solid #3b82f6;
    border-radius: 50%; width: 40px; height: 40px;
    animation: spin 1s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .page-header { margin-bottom: 2rem; }
  .page-header h1 { margin: 0 0 0.25rem; color: #0f172a; font-size: 2rem; font-weight: 800; }
  .subtitle { margin: 0; color: #64748b; font-size: 1rem; }

  /* Stats row */
  .stats-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
    gap: 1rem;
    margin-bottom: 2rem;
  }

  .stat-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.25rem;
    text-align: center;
    text-decoration: none;
    color: inherit;
    transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  }

  a.stat-card:hover {
    border-color: #3b82f6;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  }

  .stat-num {
    display: block;
    font-size: 2rem;
    font-weight: 800;
    color: #0f172a;
    line-height: 1;
    margin-bottom: 0.35rem;
  }

  .stat-label {
    font-size: 0.75rem;
    color: #64748b;
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
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
  }

  .panel h2 {
    margin: 0 0 1rem;
    font-size: 1rem;
    font-weight: 700;
    color: #1e293b;
  }

  .panel-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 1rem;
  }
  .panel-header h2 { margin: 0; }

  .panel-link {
    font-size: 0.8rem; color: #3b82f6; text-decoration: none; font-weight: 600;
  }
  .panel-link:hover { color: #2563eb; }

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

  .breakdown-label { font-size: 0.85rem; color: #334155; }
  .breakdown-val { font-size: 0.85rem; font-weight: 700; color: #0f172a; text-align: right; }

  .breakdown-bar {
    height: 0.375rem; background: #f1f5f9; border-radius: 1rem; overflow: hidden;
  }
  .bar-fill { height: 100%; border-radius: 1rem; transition: width 0.5s ease; }
  .bar-fill.active { background: #10b981; }
  .bar-fill.draft { background: #f59e0b; }
  .bar-fill.archived { background: #64748b; }

  .muted { color: #94a3b8; font-size: 0.85rem; margin: 0; }

  /* Lists */
  .list { display: flex; flex-direction: column; gap: 0.5rem; }

  .list-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 0.6rem 0.75rem; background: #f8fafc; border-radius: 0.375rem;
  }

  .list-info { display: flex; flex-direction: column; gap: 0.1rem; }
  .list-title { font-weight: 600; color: #1e293b; font-size: 0.875rem; }
  .list-sub { color: #64748b; font-size: 0.75rem; }
  .list-meta { color: #94a3b8; font-size: 0.75rem; font-family: monospace; }

  .list-badge {
    padding: 0.15rem 0.5rem; border-radius: 0.25rem;
    font-size: 0.65rem; font-weight: 700; text-transform: uppercase;
  }

  /* Quick nav */
  .quick-nav {
    display: flex; flex-wrap: wrap; gap: 0.75rem;
  }

  .qn-card {
    padding: 0.6rem 1.25rem;
    background: white; border: 1px solid #e2e8f0; border-radius: 0.5rem;
    text-decoration: none; color: #334155; font-weight: 600; font-size: 0.85rem;
    transition: border-color 0.2s, background 0.2s;
  }

  .qn-card:hover {
    border-color: #3b82f6; background: #eff6ff; color: #1e40af;
  }

  @media (max-width: 768px) {
    .stats-row { grid-template-columns: repeat(auto-fit, minmax(100px, 1fr)); }
    .grid-2col { grid-template-columns: 1fr; }
  }
</style>
