<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_DASHBOARD_STATS, GET_TENANT_DASHBOARD_STATS, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

  let stats = {
    users: 0,
    tenants: 0,
    archives: 0,
    activeArchives: 0,
    draftArchives: 0,
    archivedArchives: 0
  };
  let tenantInfo = {
    tenantId: null as number | null,
    tenantName: '',
    tenantStatus: '',
    tenantPlan: ''
  };
  let loading = true;
  let error: string | null = null;

  // Get current user role
  let currentRole = '';
  let currentUser: any = null;
  let currentTenantId: number | null = null;

  // Users list for USER role
  let allUsers: any[] = [];
  let loadingUsers = false;


  // User documents state for USER role
  let userDocuments: any[] = [];
  let loadingDocuments = false;

  onMount(() => {
    // Check user role
    const role = localStorage.getItem('auth_role');
    const user = localStorage.getItem('auth_user');
    const tenantId = localStorage.getItem('auth_tenantId');

    currentRole = role || '';
    if (user) {
      currentUser = JSON.parse(user);
    }
    if (tenantId) {
      currentTenantId = parseInt(tenantId, 10);
    }

    // Load stats based on role
    if (currentRole === 'ADMIN') {
      loadAdminDashboardStats();
    } else if (currentRole === 'TENANT' && currentTenantId) {
      loadTenantDashboardStats(currentTenantId);
    } else if (currentRole === 'USER') {
      // Load user's documents for USER role
      loadUserDocuments();
    } else {
      loading = false;
    }
  });

  async function loadAdminDashboardStats() {
    try {
      loading = true;
      // Fetch combined dashboard stats for ADMIN
      const result = await client.query({
        query: GET_DASHBOARD_STATS,
        fetchPolicy: 'network-only'
      });

      const data = result?.data?.getDashboardStats;
      if (data) {
        stats = {
          users: data.totalUsers || 0,
          tenants: data.totalTenants || 0,
          archives: data.totalArchives || 0,
          activeArchives: data.activeArchives || 0,
          draftArchives: data.draftArchives || 0,
          archivedArchives: data.archivedArchives || 0
        };
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Admin dashboard error:', e);
    } finally {
      loading = false;
    }
  }

  async function loadTenantDashboardStats(tenantId: number) {
    try {
      loading = true;
      // Fetch tenant-specific dashboard stats
      const result = await client.query({
        query: GET_TENANT_DASHBOARD_STATS,
        variables: { tenantId: tenantId.toString() },
        fetchPolicy: 'network-only'
      });

      const data = result?.data?.getTenantDashboardStats;
      if (data) {
        // Store tenant info
        tenantInfo = {
          tenantId: data.tenantId ? parseInt(data.tenantId, 10) : null,
          tenantName: data.tenantName || '',
          tenantStatus: data.tenantStatus || '',
          tenantPlan: data.tenantPlan || ''
        };

        // Store stats (no tenants count for TENANT role)
        stats = {
          users: data.totalUsers || 0,
          tenants: 0, // Not relevant for tenant role
          archives: data.totalArchives || 0,
          activeArchives: data.activeArchives || 0,
          draftArchives: data.draftArchives || 0,
          archivedArchives: data.archivedArchives || 0
        };
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Tenant dashboard error:', e);
    } finally {
      loading = false;
    }
  }

  async function loadUserDocuments() {
    if (!currentUser?.id) return;

    loadingDocuments = true;
    try {
      const params = new URLSearchParams();
      params.append('role', 'USER');
      params.append('userId', currentUser.id.toString());

      const response = await fetch(`http://localhost:2020/api/documents?${params.toString()}`);

      if (!response.ok) {
        throw new Error('Failed to load documents');
      }

      const data = await response.json();
      if (data.success) {
        userDocuments = data.documents || [];
      }
    } catch (e) {
      console.error('Error loading documents:', e);
      toasts.error('Failed to load your documents');
    } finally {
      loadingDocuments = false;
      loading = false;
    }
  }


  function formatFileSize(bytes: number): string {
    if (!bytes) return 'Unknown';
    const mb = bytes / (1024 * 1024);
    if (mb > 1) {
      return `${mb.toFixed(2)} MB`;
    }
    const kb = bytes / 1024;
    return `${kb.toFixed(2)} KB`;
  }

  function formatDate(dateString: string): string {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleString();
  }
</script>

<svelte:head>
  <title>Dashboard - Archiving System</title>
</svelte:head>

<div class="dashboard">
  <h1>Dashboard</h1>

  {#if currentRole === 'USER'}
    <!-- USER ROLE - My Documents (View Only) -->
    <div class="user-dashboard">
      <div class="welcome-message">
        <h2>👤 Welcome, {currentUser?.name || 'User'}!</h2>
        <p>View your submitted documents</p>
      </div>

      {#if loading || loadingDocuments}
        <div class="loading">
          <div class="spinner"></div>
          <p>Loading your documents...</p>
        </div>
      {:else}
        <!-- My Documents List -->
        <div class="my-documents-section">
          <div class="section-header">
            <h2>📄 My Documents</h2>
            <span class="document-count">{userDocuments.length} document{userDocuments.length !== 1 ? 's' : ''}</span>
          </div>

          {#if userDocuments.length === 0}
            <div class="empty-state">
              <span class="empty-icon">📭</span>
              <h3>No documents yet</h3>
              <p>You haven't uploaded any documents</p>
            </div>
          {:else}
            <div class="documents-grid">
              {#each userDocuments as document}
                <div class="document-card">
                  <div class="document-icon">
                    {#if document.contentType?.includes('pdf')}
                      📄
                    {:else if document.contentType?.includes('image')}
                      🖼️
                    {:else if document.contentType?.includes('video')}
                      🎥
                    {:else if document.contentType?.includes('word') || document.contentType?.includes('document')}
                      📝
                    {:else if document.contentType?.includes('spreadsheet') || document.contentType?.includes('excel')}
                      📊
                    {:else}
                      📎
                    {/if}
                  </div>
                  <div class="document-info">
                    <h3 class="document-title">{document.title}</h3>
                    {#if document.description}
                      <p class="document-description">{document.description}</p>
                    {/if}
                    <div class="document-meta">
                      <p><strong>File:</strong> {document.fileName}</p>
                      <p><strong>Size:</strong> {formatFileSize(document.fileSize)}</p>
                      <p><strong>Type:</strong> {document.contentType || 'Unknown'}</p>
                      <p><strong>Uploaded:</strong> {formatDate(document.uploadedAt)}</p>
                      <p><strong>Status:</strong> <span class="status status-{document.status.toLowerCase()}">{document.status}</span></p>
                    </div>
                  </div>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      {/if}
    </div>
  {:else if currentRole === 'ADMIN' || currentRole === 'TENANT'}
    <!-- ADMIN & TENANT ROLES - Full Dashboard -->
    {#if currentRole === 'TENANT' && tenantInfo.tenantName}
      <div class="tenant-info-banner">
        <div class="tenant-banner-content">
          <h2>🏢 {tenantInfo.tenantName}</h2>
          <div class="tenant-badges">
            <span class="badge-status badge-{tenantInfo.tenantStatus.toLowerCase()}">{tenantInfo.tenantStatus}</span>
            <span class="badge-plan badge-{tenantInfo.tenantPlan.toLowerCase()}">{tenantInfo.tenantPlan}</span>
          </div>
        </div>
      </div>
    {/if}

    {#if loading}
      <div class="loading">
        <div class="spinner"></div>
      </div>
    {:else if error}
      <div class="error">
        Error loading dashboard: {error}
      </div>
    {:else}
      <div class="stats-grid">
        <!-- Show Users stat for ADMIN and TENANT -->
        {#if currentRole === 'ADMIN' || currentRole === 'TENANT'}
          <div class="stat-card">
            <h3>Users</h3>
            <div class="stat-number">{stats.users}</div>
            <a href="/admin/users" class="stat-link">Manage Users</a>
          </div>
        {/if}

        <!-- Show Tenants stat for ADMIN only -->
        {#if currentRole === 'ADMIN'}
          <div class="stat-card">
            <h3>Tenants</h3>
            <div class="stat-number">{stats.tenants}</div>
            <a href="/admin/tenants" class="stat-link">Manage Tenants</a>
          </div>
        {/if}

        <!-- Show Archives for ADMIN and TENANT -->
        {#if currentRole === 'ADMIN'}
          <div class="stat-card">
            <h3>Archives</h3>
            <div class="stat-number">{stats.archives}</div>
            <a href="/admin/archives" class="stat-link">Manage Archives</a>
          </div>
        {:else if currentRole === 'TENANT' && currentTenantId}
          <div class="stat-card">
            <h3>Archives</h3>
            <div class="stat-number">{stats.archives}</div>
            <a href="/tenants/{currentTenantId}/archives" class="stat-link">Manage Archives</a>
          </div>
        {:else if currentRole === 'ADMIN'}
          <div class="stat-card">
            <h3>Archives</h3>
            <div class="stat-number">{stats.archives}</div>
            <a href="/admin/archives" class="stat-link">Manage Archives</a>
          </div>
        {/if}
      </div>

      <div class="archive-breakdown">
        <h2>Archive Status Breakdown</h2>
        <div class="breakdown-grid">
          <div class="breakdown-card active">
            <div class="breakdown-icon">✅</div>
            <div class="breakdown-content">
              <div class="breakdown-label">Active</div>
              <div class="breakdown-number">{stats.activeArchives}</div>
            </div>
          </div>

          <div class="breakdown-card draft">
            <div class="breakdown-icon">📝</div>
            <div class="breakdown-content">
              <div class="breakdown-label">Draft</div>
              <div class="breakdown-number">{stats.draftArchives}</div>
            </div>
          </div>

          <div class="breakdown-card archived">
            <div class="breakdown-icon">📦</div>
            <div class="breakdown-content">
              <div class="breakdown-label">Archived</div>
              <div class="breakdown-number">{stats.archivedArchives}</div>
            </div>
          </div>
        </div>
      </div>


    <div class="quick-actions">
      <h2>Quick Actions</h2>
      <div class="action-grid">
        <a href="/users/create" class="action-card">
          <h4>Create User</h4>
          <p>Add a new user to the system</p>
        </a>

        <a href="/admin/tenants/create" class="action-card">
          <h4>Create Tenant</h4>
          <p>Set up a new tenant organization</p>
        </a>

        <a href="/archives/create" class="action-card">
          <h4>Create Archive</h4>
          <p>Start a new archive document</p>
        </a>
      </div>
    </div>
    {/if}
  {:else}
    <!-- Not logged in or role not set -->
    <div class="welcome-guest">
      <h2>Welcome to Archiving System</h2>
      <p>Please <a href="/login">login</a> to access the dashboard.</p>
    </div>
  {/if}
</div>

<style>
  .dashboard {
    max-width: 1200px;
    margin: 0 auto;
  }

  h1 {
    margin-bottom: 2rem;
    color: #1e293b;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    margin-bottom: 3rem;
  }

  .stat-card {
    background: white;
    padding: 2rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    text-align: center;
    border: 1px solid #e2e8f0;
  }

  .stat-card h3 {
    margin: 0 0 1rem 0;
    color: #64748b;
    font-size: 1rem;
    font-weight: 500;
  }

  .stat-number {
    font-size: 3rem;
    font-weight: bold;
    color: #1e293b;
    margin-bottom: 1rem;
  }

  .stat-link {
    color: #3b82f6;
    text-decoration: none;
    font-weight: 500;
  }

  .stat-link:hover {
    text-decoration: underline;
  }

  /* Tenant Info Banner */
  .tenant-info-banner {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 2rem;
    border-radius: 0.75rem;
    margin-bottom: 2rem;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  }

  .tenant-banner-content h2 {
    margin: 0 0 1rem 0;
    font-size: 1.75rem;
  }

  .tenant-badges {
    display: flex;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .badge-status,
  .badge-plan {
    padding: 0.375rem 0.875rem;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
  }

  .badge-status {
    background: rgba(255, 255, 255, 0.25);
    color: white;
  }

  .badge-status.badge-active {
    background: #10b981;
  }

  .badge-status.badge-inactive {
    background: #6b7280;
  }

  .badge-status.badge-suspended {
    background: #ef4444;
  }

  .badge-status.badge-trial {
    background: #3b82f6;
  }

  .badge-plan {
    background: rgba(255, 255, 255, 0.2);
    color: white;
  }

  .badge-plan.badge-enterprise {
    background: #8b5cf6;
  }

  .badge-plan.badge-professional {
    background: #6366f1;
  }

  .badge-plan.badge-basic {
    background: #3b82f6;
  }

  .badge-plan.badge-free {
    background: #9ca3af;
  }

  .archive-breakdown {
    margin-bottom: 3rem;
  }

  .archive-breakdown h2 {
    margin-bottom: 1.5rem;
    color: #1e293b;
  }

  .breakdown-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1.5rem;
  }

  .breakdown-card {
    background: white;
    padding: 1.5rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .breakdown-card.active {
    border-left: 4px solid #10b981;
  }

  .breakdown-card.draft {
    border-left: 4px solid #f59e0b;
  }

  .breakdown-card.archived {
    border-left: 4px solid #64748b;
  }

  .breakdown-icon {
    margin-bottom: 1.5rem;
    color: #1e293b;
  }

  .action-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1.5rem;
  }

  .action-card {
    background: white;
    padding: 1.5rem;
    border-radius: 0.5rem;
    text-decoration: none;
    color: inherit;
    border: 1px solid #e2e8f0;
    transition: all 0.2s;
  }

  .action-card:hover {
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    border-color: #3b82f6;
  }

  .action-card h4 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
  }

  .action-card p {
    margin: 0;
    color: #64748b;
    font-size: 0.875rem;
  }

  /* USER Role Specific Styles */
  .user-dashboard {
    max-width: 800px;
    margin: 0 auto;
  }

  .welcome-message {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 2rem;
    border-radius: 0.75rem;
    margin-bottom: 2rem;
    text-align: center;
  }

  .welcome-message h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.75rem;
  }

  .welcome-message p {
    margin: 0;
    opacity: 0.9;
    font-size: 1.125rem;
  }


  .welcome-guest {
    text-align: center;
    padding: 4rem 2rem;
    background: white;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .welcome-guest h2 {
    margin: 0 0 1rem 0;
    color: #1e293b;
  }

  .welcome-guest p {
    margin: 0;
    color: #64748b;
  }

  .welcome-guest a {
    color: #3b82f6;
    font-weight: 600;
    text-decoration: none;
  }

  .welcome-guest a:hover {
    text-decoration: underline;
  }

  /* User Documents Section */
  .my-documents-section {
    margin-top: 3rem;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
  }

  .section-header h2 {
    margin: 0;
    color: #1e293b;
  }

  .document-count {
    background: #eff6ff;
    color: #1e40af;
    padding: 0.5rem 1rem;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.875rem;
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: white;
    border-radius: 0.75rem;
    border: 2px dashed #cbd5e1;
  }

  .empty-icon {
    font-size: 4rem;
    display: block;
    margin-bottom: 1rem;
  }

  .empty-state h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
  }

  .empty-state p {
    margin: 0;
    color: #64748b;
  }

  .documents-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .document-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
    transition: all 0.2s;
  }

  .document-card:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .document-icon {
    font-size: 3rem;
    text-align: center;
    margin-bottom: 1rem;
  }

  .document-info {
    margin-bottom: 1rem;
  }

  .document-title {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1.125rem;
    font-weight: 600;
  }

  .document-description {
    color: #64748b;
    margin: 0 0 1rem 0;
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .document-meta {
    font-size: 0.875rem;
    color: #64748b;
  }

  .document-meta p {
    margin: 0.25rem 0;
  }

  .status {
    display: inline-block;
    padding: 0.25rem 0.5rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .status-active {
    background-color: #dcfce7;
    color: #166534;
  }

  .status-archived {
    background-color: #f3f4f6;
    color: #4b5563;
  }

  .status-pending_review {
    background-color: #fef3c7;
    color: #92400e;
  }

  .status-approved {
    background-color: #dbeafe;
    color: #1e40af;
  }

  .status-rejected {
    background-color: #fee2e2;
    color: #991b1b;
  }
</style>
