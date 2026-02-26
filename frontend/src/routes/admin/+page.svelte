<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_ARCHIVES, GET_ALL_USERS, GET_ALL_TENANTS } from '$lib/graphql/queries';

  let archives: any[] = [];
  let users: any[] = [];
  let tenants: any[] = [];
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

  // Statistics
  let stats = {
    totalArchives: 0,
    totalUsers: 0,
    totalTenants: 0,
    activeArchives: 0,
    draftArchives: 0,
    archivedArchives: 0
  };

  onMount(async () => {
    // Check role first
    const role = localStorage.getItem('auth_role');
    const tenantId = localStorage.getItem('auth_tenantId');
    currentRole = role || '';

    // Only ADMIN can access this page
    if (currentRole !== 'ADMIN') {
      hasAccess = false;
      loading = false;

      // Redirect non-admin users to appropriate page
      if (currentRole === 'TENANT' && tenantId) {
        // Redirect TENANT to their tenant page
        goto(`/tenants/${tenantId}`);
      } else if (currentRole === 'USER' && tenantId) {
        // Redirect USER to their tenant users page
        goto(`/tenants/${tenantId}/users`);
      } else {
        // Redirect others to home
        goto('/');
      }
      return;
    }

    hasAccess = true;
    await loadAdminData();
  });

  async function loadAdminData() {
    try {
      loading = true;
      const [archivesResult, usersResult, tenantsResult] = await Promise.all([
        client.query({ query: GET_ALL_ARCHIVES, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_USERS, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_TENANTS, fetchPolicy: 'network-only' })
      ]);

      archives = archivesResult?.data?.getAllArchives || [];
      users = usersResult?.data?.getAllUsers || [];
      tenants = tenantsResult?.data?.getAllTenants || [];

      // Calculate statistics
      stats.totalArchives = archives.length;
      stats.totalUsers = users.length;
      stats.totalTenants = tenants.length;
      stats.activeArchives = archives.filter(a => a.status === 'PUBLISHED').length;
      stats.draftArchives = archives.filter(a => a.status === 'DRAFT').length;
      stats.archivedArchives = archives.filter(a => a.status === 'ARCHIVED').length;

      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load admin data error:', e);
    } finally {
      loading = false;
    }
  }

  function getStandardCounts() {
    const counts: Record<string, number> = {};
    archives.forEach(archive => {
      counts[archive.standard] = (counts[archive.standard] || 0) + 1;
    });
    return Object.entries(counts).sort((a, b) => b[1] - a[1]);
  }

  function getRecentArchives() {
    return [...archives]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);
  }
</script>

<svelte:head>
  <title>Admin - Archiving System</title>
</svelte:head>

<div class="admin-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to access the admin panel.</p>
      <p class="redirect-message">Redirecting to dashboard...</p>
      <a href="/" class="btn-home">Go to Dashboard</a>
    </div>
  {:else if loading}
    <!-- Loading State -->
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading admin panel...</p>
    </div>
  {:else if error}
    <!-- Error State -->
    <div class="error-state">
      <div class="error-icon">❌</div>
      <h2>Error Loading Data</h2>
      <p>{error}</p>
      <button on:click={loadAdminData} class="btn-retry">Try Again</button>
    </div>
  {:else}
    <!-- Admin Dashboard -->
  <div class="page-header">
    <div class="header-content">
      <h1>🛡️ Admin Dashboard</h1>
      <p class="subtitle">System overview and management</p>
    </div>
  </div>

  <!-- Stats Grid -->
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
    </div>
  {:else}
    <!-- Quick Actions & Statistics -->
    <div class="combined-grid">
      <a href="/admin/tenants" class="combined-card">
        <div class="card-icon">🏢</div>
        <div class="card-content">
          <div class="card-title">Manage Tenants</div>
          <div class="card-value">{stats.totalTenants}</div>
        </div>
      </a>

      <a href="/admin/users" class="combined-card">
        <div class="card-icon">👥</div>
        <div class="card-content">
          <div class="card-title">Manage Users</div>
          <div class="card-value">{stats.totalUsers}</div>
        </div>
      </a>

      <a href="/admin/archives" class="combined-card">
        <div class="card-icon">📁</div>
        <div class="card-content">
          <div class="card-title">Manage Archives</div>
          <div class="card-value">{stats.totalArchives}</div>
        </div>
      </a>

      <div class="combined-card stat-only">
        <div class="card-icon">✅</div>
        <div class="card-content">
          <div class="card-title">Active Archives</div>
          <div class="card-value">{stats.activeArchives}</div>
        </div>
      </div>
    </div>

    <!-- Archive Status Breakdown -->
    <div class="section">
      <h2>Archive Status Breakdown</h2>
      <div class="status-grid">
        <div class="status-item">
          <div class="status-label">
            <span class="status-dot status-active"></span>
            Active
          </div>
          <div class="status-count">{stats.activeArchives}</div>
        </div>
        <div class="status-item">
          <div class="status-label">
            <span class="status-dot status-draft"></span>
            Draft
          </div>
          <div class="status-count">{stats.draftArchives}</div>
        </div>
        <div class="status-item">
          <div class="status-label">
            <span class="status-dot status-archived"></span>
            Archived
          </div>
          <div class="status-count">{stats.archivedArchives}</div>
        </div>
      </div>
    </div>

    <!-- Archive Standards Distribution -->
    <div class="section">
      <h2>Archive Standards Distribution</h2>
      <div class="standards-list">
        {#each getStandardCounts() as [standard, count]}
          <div class="standard-item">
            <div class="standard-name">{standard}</div>
            <div class="standard-bar">
              <div
                class="standard-fill"
                style="width: {(count / stats.totalArchives) * 100}%"
              ></div>
            </div>
            <div class="standard-count">{count}</div>
          </div>
        {/each}
      </div>
    </div>

    <!-- Recent Archives -->
    <div class="section">
      <h2>Recent Archives</h2>
      <div class="recent-archives">
        {#each getRecentArchives() as archive}
          <div class="archive-item">
            <div class="archive-info">
              <div class="archive-title">{archive.title}</div>
              <div class="archive-meta">
                <span class="badge badge-{archive.status.toLowerCase()}">{archive.status}</span>
                <span class="badge badge-standard">{archive.standard}</span>
                <span class="archive-date">{new Date(archive.createdAt).toLocaleDateString()}</span>
              </div>
            </div>
            <a href="/admin/archives" class="view-link">View →</a>
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>

<style>
  .admin-page {
    max-width: 1600px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    margin-bottom: 2rem;
  }

  .header-content h1 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 2.5rem;
    font-weight: 700;
  }

  .subtitle {
    margin: 0;
    color: #64748b;
    font-size: 1.125rem;
  }

  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    border: 1px solid #fcc;
  }

  .loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
  }

  .spinner {
    border: 4px solid #f3f4f6;
    border-top: 4px solid #3b82f6;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .combined-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    margin-bottom: 2rem;
  }

  .combined-card {
    background: white;
    border-radius: 0.75rem;
    padding: 1.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 2px solid #e2e8f0;
    display: flex;
    align-items: center;
    gap: 1rem;
    transition: all 0.2s;
    text-decoration: none;
    cursor: pointer;
  }

  .combined-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    border-color: #3b82f6;
  }

  .combined-card.stat-only {
    cursor: default;
  }

  .combined-card.stat-only:hover {
    border-color: #e2e8f0;
  }

  .card-icon {
    font-size: 2.5rem;
  }

  .card-content {
    flex: 1;
  }

  .card-title {
    color: #64748b;
    font-size: 0.875rem;
    font-weight: 500;
    margin-bottom: 0.25rem;
  }

  .card-value {
    color: #1e293b;
    font-size: 2rem;
    font-weight: 700;
  }

  .section {
    background: white;
    border-radius: 0.75rem;
    padding: 1.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
    margin-bottom: 1.5rem;
  }

  .section h2 {
    margin: 0 0 1.5rem 0;
    color: #1e293b;
    font-size: 1.5rem;
    font-weight: 600;
  }

  .status-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1.5rem;
  }

  .status-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    background: #f8fafc;
    border-radius: 0.5rem;
  }

  .status-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: #475569;
    font-weight: 500;
  }

  .status-dot {
    width: 12px;
    height: 12px;
    border-radius: 50%;
  }

  .status-dot.status-active {
    background: #10b981;
  }

  .status-dot.status-draft {
    background: #f59e0b;
  }

  .status-dot.status-archived {
    background: #64748b;
  }

  .status-count {
    color: #1e293b;
    font-size: 1.5rem;
    font-weight: 700;
  }

  .standards-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .standard-item {
    display: grid;
    grid-template-columns: 150px 1fr 60px;
    align-items: center;
    gap: 1rem;
  }

  .standard-name {
    color: #1e293b;
    font-weight: 500;
  }

  .standard-bar {
    height: 24px;
    background: #f1f5f9;
    border-radius: 0.25rem;
    overflow: hidden;
  }

  .standard-fill {
    height: 100%;
    background: linear-gradient(90deg, #3b82f6, #8b5cf6);
    transition: width 0.3s;
  }

  .standard-count {
    color: #64748b;
    font-weight: 600;
    text-align: right;
  }

  .recent-archives {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .archive-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem;
    background: #f8fafc;
    border-radius: 0.5rem;
    transition: background 0.2s;
  }

  .archive-item:hover {
    background: #f1f5f9;
  }

  .archive-info {
    flex: 1;
  }

  .archive-title {
    color: #1e293b;
    font-weight: 600;
    margin-bottom: 0.5rem;
  }

  .archive-meta {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .badge-active {
    background: #dcfce7;
    color: #166534;
  }

  .badge-draft {
    background: #fef3c7;
    color: #92400e;
  }

  .badge-archived {
    background: #f3f4f6;
    color: #6b7280;
  }

  .badge-standard {
    background: #dbeafe;
    color: #1e40af;
  }

  .archive-date {
    color: #64748b;
    font-size: 0.875rem;
  }

  .view-link {
    color: #3b82f6;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s;
  }

  .view-link:hover {
    color: #2563eb;
  }

  /* Access Denied Styles */
  .access-denied {
    text-align: center;
    padding: 4rem 2rem;
    max-width: 600px;
    margin: 4rem auto;
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .access-denied-icon {
    font-size: 5rem;
    margin-bottom: 1rem;
  }

  .access-denied h1 {
    color: #ef4444;
    margin-bottom: 1rem;
    font-size: 2rem;
  }

  .access-denied p {
    color: #64748b;
    margin-bottom: 1rem;
    font-size: 1.125rem;
  }

  .redirect-message {
    color: #3b82f6;
    font-weight: 500;
  }

  .btn-home {
    display: inline-block;
    margin-top: 1.5rem;
    padding: 0.75rem 2rem;
    background: #3b82f6;
    color: white;
    text-decoration: none;
    border-radius: 0.5rem;
    font-weight: 600;
    transition: background 0.2s;
  }

  .btn-home:hover {
    background: #2563eb;
  }

  /* Loading Styles */
  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    gap: 1rem;
  }

  .spinner {
    width: 3rem;
    height: 3rem;
    border: 4px solid #f3f4f6;
    border-top-color: #ef4444;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  /* Error State Styles */
  .error-state {
    text-align: center;
    padding: 4rem 2rem;
    max-width: 600px;
    margin: 4rem auto;
    background: #fee2e2;
    border-radius: 0.75rem;
    border: 1px solid #fca5a5;
  }

  .error-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
  }

  .error-state h2 {
    color: #991b1b;
    margin-bottom: 1rem;
  }

  .error-state p {
    color: #7f1d1d;
    margin-bottom: 1.5rem;
  }

  .btn-retry {
    padding: 0.75rem 2rem;
    background: #ef4444;
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-retry:hover {
    background: #dc2626;
  }

  @media (max-width: 768px) {
    .admin-page {
      padding: 1rem;
    }

    .combined-grid {
      grid-template-columns: 1fr;
    }

    .standard-item {
      grid-template-columns: 1fr;
      gap: 0.5rem;
    }

    .standard-bar {
      order: 3;
    }

    .standard-count {
      text-align: left;
    }
  }
</style>
