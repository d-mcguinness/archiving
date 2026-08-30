<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_TENANT, GET_TENANT_DASHBOARD_STATS, GET_INTAKES_BY_TENANT_V2, GET_PRESERVATIONS_BY_TENANT, GET_RELEASES_BY_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { authHeaders, API_BASE } from '$lib/api';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  let tenant: any = null;
  let loading = true;
  let error: string | null = null;
  let tenantId: string;

  // Stats
  let tenantStats = {
    totalUsers: 0,
    totalArchives: 0,
    activeArchives: 0,
    draftArchives: 0,
    archivedArchives: 0,
  };
  let totalIntakes = 0;
  let totalPreservations = 0;
  let totalReleases = 0;
  let totalDocuments = 0;
  let sipStatuses: Record<string, number> = {};
  let aipStatuses: Record<string, number> = {};
  let dipStatuses: Record<string, number> = {};
  let recentIntakes: any[] = [];

  $: tenantId = $page.params.id || '';

  onMount(async () => {
    if (tenantId) {
      await loadAll();
    }
  });

  async function loadAll() {
    loading = true;
    try {
      const [tenantResult, statsResult, sipsResult, aipsResult, dipsResult, docsResult] = await Promise.all([
        client.query({ query: GET_TENANT, variables: { id: tenantId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_TENANT_DASHBOARD_STATS, variables: { tenantId }, fetchPolicy: 'network-only' }).catch(() => null),
        client.query({ query: GET_INTAKES_BY_TENANT_V2, variables: { tenantId }, fetchPolicy: 'network-only' }).catch(() => ({ data: { getIntakesByTenantV2: [] } })),
        client.query({ query: GET_PRESERVATIONS_BY_TENANT, variables: { tenantId }, fetchPolicy: 'network-only' }).catch(() => ({ data: { getPreservationsByTenant: [] } })),
        client.query({ query: GET_RELEASES_BY_TENANT, variables: { tenantId }, fetchPolicy: 'network-only' }).catch(() => ({ data: { getReleasesByTenant: [] } })),
        fetch(`${API_BASE}/api/documents?role=TENANT&tenantId=${tenantId}`, { headers: { ...authHeaders() } }).then(r => r.json()).catch(() => ({ documents: [] })),
      ]);

      tenant = tenantResult?.data?.getTenant || null;

      const s = statsResult?.data?.getTenantDashboardStats;
      if (s) {
        tenantStats = {
          totalUsers: s.totalUsers || 0,
          totalArchives: s.totalArchives || 0,
          activeArchives: s.activeArchives || 0,
          draftArchives: s.draftArchives || 0,
          archivedArchives: s.archivedArchives || 0,
        };
      }

      const sips = sipsResult?.data?.getIntakesByTenantV2 || [];
      totalIntakes = sips.length;
      recentIntakes = sips.slice(0, 5);
      sipStatuses = countBy(sips, 'status');

      const aips = aipsResult?.data?.getPreservationsByTenant || [];
      totalPreservations = aips.length;
      aipStatuses = countBy(aips, 'status');

      const dips = dipsResult?.data?.getReleasesByTenant || [];
      totalReleases = dips.length;
      dipStatuses = countBy(dips, 'status');

      totalDocuments = docsResult?.documents?.length || docsResult?.count || 0;

      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load error:', e);
      toasts.error('Failed to load tenant details');
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

  function statusColor(status: string): string {
    const map: Record<string, string> = {
      DRAFT: '#f59e0b', SUBMITTED: '#3b82f6', VALIDATED: '#06b6d4',
      ACCEPTED: '#10b981', REJECTED: '#ef4444', ACTIVE: '#10b981',
      ARCHIVED: '#64748b', PUBLISHED: '#10b981',
    };
    return map[status] || '#94a3b8';
  }

  function getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return '#10b981';
      case 'INACTIVE': return '#6b7280';
      case 'SUSPENDED': return '#ef4444';
      case 'TRIAL': return '#3b82f6';
      case 'PENDING_ACTIVATION': return '#f59e0b';
      default: return '#64748b';
    }
  }

  function getPlanColor(plan: string): string {
    switch (plan) {
      case 'ENTERPRISE': return '#8b5cf6';
      case 'PROFESSIONAL': return '#6366f1';
      case 'BASIC': return '#3b82f6';
      case 'FREE': return '#9ca3af';
      default: return '#64748b';
    }
  }

  function formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
</script>

<svelte:head>
  <title>{tenant ? tenant.displayName || tenant.name : 'Tenant'} - Archiving System</title>
</svelte:head>

<div class="tenant-detail-page">
  <div class="page-header">
    <span class="eyebrow">Tenants</span>
    <h1>Tenant Details</h1>
  </div>

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading tenant details...</p>
    </div>
  {:else if error}
    <div class="error">
      Error: {error}
    </div>
  {:else if tenant}
    <div class="tenant-info">
      <Breadcrumb
        context={{ tenantId, tenantName: tenant?.displayName || tenant?.name }}
        items={[]}
      />
      <!-- Tenant Header Card -->
      <div class="tenant-header-card">
        <div class="tenant-icon">🏢</div>
        <div class="tenant-header-content">
          <h2>{tenant.displayName || tenant.name}</h2>
          <p class="tenant-domain">{tenant.domain}</p>
          <div class="tenant-badges">
            <span class="badge badge-status" style="background-color: {getStatusColor(tenant.status)}">
              {tenant.status}
            </span>
            <span class="badge badge-plan" style="background-color: {getPlanColor(tenant.plan)}">
              {tenant.plan}
            </span>
          </div>
        </div>
      </div>

      <!-- Stats Row -->
      <div class="stats-row">
        <a href="/tenants/{tenantId}/users" class="stat-card">
          <span class="stat-num">{tenantStats.totalUsers}</span>
          <span class="stat-label">Users</span>
        </a>
        <a href="/tenants/{tenantId}/archives" class="stat-card">
          <span class="stat-num">{tenantStats.totalArchives}</span>
          <span class="stat-label">Archives</span>
        </a>
        <a href="/tenants/{tenantId}/intakes" class="stat-card">
          <span class="stat-num">{totalIntakes}</span>
          <span class="stat-label">Intakes</span>
        </a>
        <a href="/tenants/{tenantId}/preservations" class="stat-card">
          <span class="stat-num">{totalPreservations}</span>
          <span class="stat-label">Preservations</span>
        </a>
        <a href="/tenants/{tenantId}/releases" class="stat-card">
          <span class="stat-num">{totalReleases}</span>
          <span class="stat-label">Releases</span>
        </a>
        <a href="/tenants/{tenantId}/documents" class="stat-card">
          <span class="stat-num">{totalDocuments}</span>
          <span class="stat-label">Documents</span>
        </a>
      </div>

      <!-- Breakdowns -->
      <div class="grid-2col">
        <div class="panel">
          <h3>Archive Status</h3>
          <div class="breakdown-list">
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: #10b981;"></span>
              <span class="bd-label">Active</span>
              <span class="bd-val">{tenantStats.activeArchives}</span>
              <div class="bd-bar"><div class="bd-fill" style="width: {tenantStats.totalArchives ? (tenantStats.activeArchives / tenantStats.totalArchives * 100) : 0}%; background: #10b981;"></div></div>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: #f59e0b;"></span>
              <span class="bd-label">Draft</span>
              <span class="bd-val">{tenantStats.draftArchives}</span>
              <div class="bd-bar"><div class="bd-fill" style="width: {tenantStats.totalArchives ? (tenantStats.draftArchives / tenantStats.totalArchives * 100) : 0}%; background: #f59e0b;"></div></div>
            </div>
            <div class="breakdown-item">
              <span class="breakdown-dot" style="background: #64748b;"></span>
              <span class="bd-label">Archived</span>
              <span class="bd-val">{tenantStats.archivedArchives}</span>
              <div class="bd-bar"><div class="bd-fill" style="width: {tenantStats.totalArchives ? (tenantStats.archivedArchives / tenantStats.totalArchives * 100) : 0}%; background: #64748b;"></div></div>
            </div>
          </div>
        </div>

        <div class="panel">
          <h3>Intake Status</h3>
          <div class="breakdown-list">
            {#each Object.entries(sipStatuses) as [status, count]}
              <div class="breakdown-item">
                <span class="breakdown-dot" style="background: {statusColor(status)};"></span>
                <span class="bd-label">{status}</span>
                <span class="bd-val">{count}</span>
                <div class="bd-bar"><div class="bd-fill" style="width: {totalIntakes ? (count / totalIntakes * 100) : 0}%; background: {statusColor(status)};"></div></div>
              </div>
            {/each}
            {#if Object.keys(sipStatuses).length === 0}
              <p class="muted">No Intakes yet</p>
            {/if}
          </div>
        </div>

        <div class="panel">
          <h3>Preservation Status</h3>
          <div class="breakdown-list">
            {#each Object.entries(aipStatuses) as [status, count]}
              <div class="breakdown-item">
                <span class="breakdown-dot" style="background: {statusColor(status)};"></span>
                <span class="bd-label">{status}</span>
                <span class="bd-val">{count}</span>
                <div class="bd-bar"><div class="bd-fill" style="width: {totalPreservations ? (count / totalPreservations * 100) : 0}%; background: {statusColor(status)};"></div></div>
              </div>
            {/each}
            {#if Object.keys(aipStatuses).length === 0}
              <p class="muted">No Preservations yet</p>
            {/if}
          </div>
        </div>

        <div class="panel">
          <h3>Release Status</h3>
          <div class="breakdown-list">
            {#each Object.entries(dipStatuses) as [status, count]}
              <div class="breakdown-item">
                <span class="breakdown-dot" style="background: {statusColor(status)};"></span>
                <span class="bd-label">{status}</span>
                <span class="bd-val">{count}</span>
                <div class="bd-bar"><div class="bd-fill" style="width: {totalReleases ? (count / totalReleases * 100) : 0}%; background: {statusColor(status)};"></div></div>
              </div>
            {/each}
            {#if Object.keys(dipStatuses).length === 0}
              <p class="muted">No Releases yet</p>
            {/if}
          </div>
        </div>
      </div>

      <!-- Recent Intakes -->
      {#if recentIntakes.length > 0}
        <div class="panel" style="margin-bottom: 2rem;">
          <div class="panel-header-row">
            <h3>Recent Intakes</h3>
            <a href="/tenants/{tenantId}/intakes" class="panel-link">View All →</a>
          </div>
          <div class="recent-list">
            {#each recentIntakes as sip}
              <div class="recent-row">
                <div class="recent-info">
                  <span class="recent-title">{sip.title}</span>
                  <span class="recent-sub">{sip.standard} &middot; {formatDate(sip.createdAt)}</span>
                </div>
                <span class="recent-badge" style="background: {statusColor(sip.status)};">{sip.status}</span>
              </div>
            {/each}
          </div>
        </div>
      {/if}

      <!-- Navigation Cards -->
      <div class="nav-section">
        <h3>Manage</h3>
        <div class="nav-grid">
          <a href="/tenants/{tenantId}/users" class="nav-card users">
            <span class="nav-icon">👥</span>
            <div class="nav-content">
              <h4>Users</h4>
              <p>Manage users and role assignments</p>
            </div>
            <span class="nav-arrow">&rarr;</span>
          </a>
          <a href="/tenants/{tenantId}/archives" class="nav-card archives">
            <span class="nav-icon">📁</span>
            <div class="nav-content">
              <h4>Archives</h4>
              <p>Create and manage archival collections</p>
            </div>
            <span class="nav-arrow">&rarr;</span>
          </a>
          <a href="/tenants/{tenantId}/intakes" class="nav-card sips">
            <span class="nav-icon">📦</span>
            <div class="nav-content">
              <h4>Intakes</h4>
              <p>Intake packages</p>
            </div>
            <span class="nav-arrow">&rarr;</span>
          </a>
          <a href="/tenants/{tenantId}/preservations" class="nav-card aips">
            <span class="nav-icon">🏗️</span>
            <div class="nav-content">
              <h4>Preservations</h4>
              <p>Preservation packages</p>
            </div>
            <span class="nav-arrow">&rarr;</span>
          </a>
          <a href="/tenants/{tenantId}/releases" class="nav-card dips">
            <span class="nav-icon">📤</span>
            <div class="nav-content">
              <h4>Releases</h4>
              <p>Release packages</p>
            </div>
            <span class="nav-arrow">&rarr;</span>
          </a>
          <a href="/tenants/{tenantId}/documents" class="nav-card documents">
            <span class="nav-icon">📄</span>
            <div class="nav-content">
              <h4>Documents</h4>
              <p>Upload, browse, and download files</p>
            </div>
            <span class="nav-arrow">&rarr;</span>
          </a>
        </div>
      </div>
    </div>
  {:else}
    <div class="empty-state">
      <span class="empty-icon">🏢</span>
      <h3>Tenant Not Found</h3>
      <p>The requested tenant could not be found.</p>
      <a href="/admin/tenants" class="btn-back">Back to Tenants</a>
    </div>
  {/if}
</div>

<style>
  .tenant-detail-page {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    margin-bottom: 2rem;
  }


  .page-header h1 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    gap: 1rem;
  }

  /* Page loader — global .spinner chrome, scaled up for the empty page. */
  .spinner {
    border: 4px solid var(--arc-line-strong);
    border-top: 4px solid var(--arc-indigo, #6366f1);
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  /* Scoped animation outranks the global reduced-motion rule, so repeat it. */
  @media (prefers-reduced-motion: reduce) {
    .spinner {
      animation: none;
    }
  }

  .error {
    padding: 1rem;
  }

  .tenant-info {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .tenant-header-card {
    background: var(--arc-grad-dark, radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%));
    color: #cbd5e1;
    padding: 2rem;
    border-radius: 1rem;
    border: 1px solid var(--arc-line);
    display: flex;
    align-items: center;
    gap: 1.5rem;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .tenant-icon {
    font-size: 4rem;
  }

  .tenant-header-content {
    flex: 1;
  }

  .tenant-header-content h2 {
    margin: 0 0 0.5rem 0;
    font-size: 2rem;
    color: #f8fafc;
  }

  .tenant-domain {
    margin: 0 0 1rem 0;
    color: #cbd5e1;
    font-size: 1.125rem;
  }

  .tenant-badges {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .badge {
    padding: 0.375rem 0.875rem;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
    color: white;
  }

  /* Stats */
  .stats-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(130px, 1fr));
    gap: 1rem;
    margin-bottom: 2rem;
  }

  .stat-card {
    background: var(--arc-card);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 1.25rem;
    text-align: center;
    text-decoration: none;
    color: inherit;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  }

  .stat-card:hover {
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

  .grid-2col {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
    gap: 1.25rem;
    margin-bottom: 2rem;
  }

  .panel {
    background: var(--arc-card);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 1.5rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .panel h3 {
    margin: 0 0 1rem;
    font-size: 1rem;
    font-weight: 700;
    color: var(--arc-ink, #0f172a);
  }

  .panel-header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
  }

  .panel-header-row h3 { margin: 0; }

  .panel-link {
    font-size: 0.8rem;
    color: var(--arc-indigo, #6366f1);
    text-decoration: none;
    font-weight: 600;
  }

  .panel-link:hover { color: var(--arc-indigo-deep, #4f46e5); }

  .breakdown-list { display: flex; flex-direction: column; gap: 0.75rem; }

  .breakdown-item {
    display: grid;
    grid-template-columns: 0.6rem 1fr auto 5rem;
    align-items: center;
    gap: 0.6rem;
  }

  .breakdown-dot { width: 0.6rem; height: 0.6rem; border-radius: 50%; }
  .bd-label { font-size: 0.85rem; color: var(--arc-body); }
  .bd-val { font-size: 0.85rem; font-weight: 700; color: var(--arc-ink); text-align: right; }
  .bd-bar { height: 0.375rem; background: var(--arc-card-2); border-radius: 1rem; overflow: hidden; }
  .bd-fill { height: 100%; border-radius: 1rem; transition: width 0.5s ease; }

  .muted { color: var(--arc-faint); font-size: 0.85rem; margin: 0; }

  .recent-list { display: flex; flex-direction: column; gap: 0.5rem; }

  .recent-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.6rem 0.75rem;
    background: var(--arc-card-2);
    border-radius: 0.375rem;
  }

  .recent-info { display: flex; flex-direction: column; gap: 0.1rem; }
  .recent-title { font-weight: 600; color: var(--arc-ink); font-size: 0.875rem; }
  .recent-sub { color: var(--arc-muted); font-size: 0.75rem; }

  .recent-badge {
    padding: 0.15rem 0.6rem;
    border-radius: 9999px;
    font-size: 0.65rem;
    font-weight: 700;
    color: white;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .nav-section {
    margin-top: 2rem;
  }

  .nav-section h3 {
    margin: 0 0 1.25rem;
    color: var(--arc-muted);
    font-size: 1rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .nav-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 1rem;
  }

  .nav-card {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1.25rem 1.5rem;
    background: var(--arc-card);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    text-decoration: none;
    color: inherit;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .nav-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  .nav-card.users:hover    { border-color: #f59e0b; }
  .nav-card.archives:hover { border-color: #06b6d4; }
  .nav-card.sips:hover     { border-color: #ec4899; }
  .nav-card.aips:hover     { border-color: #6366f1; }
  .nav-card.dips:hover     { border-color: #f97316; }
  .nav-card.documents:hover { border-color: #8b5cf6; }

  .nav-icon {
    font-size: 2rem;
    flex-shrink: 0;
    line-height: 1;
  }

  .nav-content {
    flex: 1;
    min-width: 0;
  }

  .nav-content h4 {
    margin: 0 0 0.2rem;
    color: var(--arc-ink);
    font-size: 1.05rem;
  }

  .nav-content p {
    margin: 0;
    color: var(--arc-muted);
    font-size: 0.825rem;
    line-height: 1.4;
  }

  .nav-arrow {
    font-size: 1.25rem;
    color: var(--arc-faint);
    flex-shrink: 0;
    transition: color 0.2s, transform 0.2s;
  }

  .nav-card:hover .nav-arrow {
    color: var(--arc-indigo-deep, #4f46e5);
    transform: translateX(3px);
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-card);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .empty-icon {
    font-size: 5rem;
    margin-bottom: 1rem;
    display: block;
  }

  .empty-state h3 {
    margin: 0 0 1rem 0;
    color: var(--arc-ink, #0f172a);
  }

  .empty-state p {
    margin: 0 0 1.5rem 0;
    color: var(--arc-muted);
  }

  .btn-back {
    display: inline-block;
    padding: 0.75rem 1.5rem;
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    text-decoration: none;
    border-radius: 0.65rem;
    font-weight: 700;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
    transition: background 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
  }

  .btn-back:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  @media (max-width: 768px) {
    .tenant-detail-page {
      padding: 1rem;
    }

    .tenant-header-card {
      flex-direction: column;
      text-align: center;
    }
  }
</style>

