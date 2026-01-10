<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, GET_ALL_TENANTS, GET_ALL_ARCHIVES } from '$lib/graphql/queries';

  let stats = {
    users: 0,
    tenants: 0,
    archives: 0
  };
  let loading = true;
  let error: string | null = null;

  onMount(async () => {
    try {
      // Fetch stats from all modules
      const [usersResult, tenantsResult, archivesResult] = await Promise.all([
        client.query({ query: GET_ALL_USERS }),
        client.query({ query: GET_ALL_TENANTS }),
        client.query({ query: GET_ALL_ARCHIVES })
      ]);

      stats = {
        users: usersResult?.data?.getAllUsers?.length || 0,
        tenants: tenantsResult?.data?.getAllTenants?.length || 0,
        archives: archivesResult?.data?.getAllArchives?.length || 0
      };
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Dashboard error:', e);
    } finally {
      loading = false;
    }
  });
</script>

<svelte:head>
  <title>Dashboard - Archiving System</title>
</svelte:head>

<div class="dashboard">
  <h1>Dashboard</h1>

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
      <div class="stat-card">
        <h3>Users</h3>
        <div class="stat-number">{stats.users}</div>
        <a href="/users" class="stat-link">Manage Users</a>
      </div>

      <div class="stat-card">
        <h3>Tenants</h3>
        <div class="stat-number">{stats.tenants}</div>
        <a href="/tenants" class="stat-link">Manage Tenants</a>
      </div>

      <div class="stat-card">
        <h3>Archives</h3>
        <div class="stat-number">{stats.archives}</div>
        <a href="/archives" class="stat-link">Manage Archives</a>
      </div>
    </div>

    <div class="quick-actions">
      <h2>Quick Actions</h2>
      <div class="action-grid">
        <a href="/users/create" class="action-card">
          <h4>Create User</h4>
          <p>Add a new user to the system</p>
        </a>

        <a href="/tenants/create" class="action-card">
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

  .quick-actions h2 {
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
</style>
