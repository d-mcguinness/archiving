<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_DASHBOARD_STATS } from '$lib/graphql/queries';

  let stats = {
    users: 0,
    tenants: 0,
    archives: 0,
    activeArchives: 0,
    draftArchives: 0,
    archivedArchives: 0
  };
  let loading = true;
  let error: string | null = null;

  // File upload state
  let selectedFile: File | null = null;
  let uploading = false;
  let uploadMessage = '';
  let uploadError = '';

  function handleFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      selectedFile = input.files[0];
      uploadMessage = '';
      uploadError = '';
    }
  }

  async function handleUpload() {
    if (!selectedFile) {
      uploadError = 'Please select a file first';
      return;
    }

    uploading = true;
    uploadMessage = '';
    uploadError = '';

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);

      const response = await fetch('http://localhost:2020/api/upload', {
        method: 'POST',
        body: formData
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Upload failed: ${response.status}`);
      }

      const result = await response.json();
      uploadMessage = result.message || 'File uploaded successfully!';
      selectedFile = null;

      // Reset file input
      const fileInput = document.getElementById('file-upload') as HTMLInputElement;
      if (fileInput) fileInput.value = '';
    } catch (e) {
      uploadError = e instanceof Error ? e.message : 'Failed to upload file';
      console.error('Upload error:', e);
    } finally {
      uploading = false;
    }
  }

  onMount(async () => {
    try {
      // Fetch dashboard stats using single optimized query
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

    <div class="file-upload-section">
      <h2>Upload File</h2>
      <div class="upload-card">
        <div class="upload-area">
          <input
            type="file"
            id="file-upload"
            on:change={handleFileSelect}
            disabled={uploading}
            class="file-input"
          />
          <label for="file-upload" class="file-label">
            <span class="upload-icon">📁</span>
            <span class="upload-text">
              {selectedFile ? selectedFile.name : 'Choose a file to upload'}
            </span>
          </label>
        </div>

        {#if uploadMessage}
          <div class="upload-success">
            <span class="success-icon">✅</span>
            <span>{uploadMessage}</span>
          </div>
        {/if}

        {#if uploadError}
          <div class="upload-error">
            <span class="error-icon">❌</span>
            <span>{uploadError}</span>
          </div>
        {/if}

        <button
          class="upload-button"
          on:click={handleUpload}
          disabled={!selectedFile || uploading}
        >
          {uploading ? '⏳ Uploading...' : '📤 Upload File'}
        </button>
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
    font-size: 2rem;
  }

  .breakdown-content {
    flex: 1;
  }

  .breakdown-label {
    color: #64748b;
    font-size: 0.875rem;
    font-weight: 500;
    margin-bottom: 0.25rem;
  }

  .breakdown-number {
    font-size: 1.75rem;
    font-weight: 700;
    color: #1e293b;
  }

  .file-upload-section {
    margin-bottom: 3rem;
  }

  .file-upload-section h2 {
    margin-bottom: 1.5rem;
    color: #1e293b;
  }

  .upload-card {
    background: white;
    padding: 2rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .upload-area {
    margin-bottom: 1.5rem;
  }

  .file-input {
    display: none;
  }

  .file-label {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1.5rem;
    border: 2px dashed #cbd5e1;
    border-radius: 0.5rem;
    cursor: pointer;
    transition: all 0.2s;
    background: #f8fafc;
  }

  .file-label:hover {
    border-color: #3b82f6;
    background: #eff6ff;
  }

  .upload-icon {
    font-size: 2rem;
  }

  .upload-text {
    color: #475569;
    font-weight: 500;
  }

  .upload-success {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 1rem;
    background: #dcfce7;
    border: 1px solid #86efac;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    color: #166534;
  }

  .success-icon {
    font-size: 1.25rem;
  }

  .upload-error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 1rem;
    background: #fee2e2;
    border: 1px solid #fca5a5;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    color: #991b1b;
  }

  .error-icon {
    font-size: 1.25rem;
  }

  .upload-button {
    width: 100%;
    padding: 0.75rem 1.5rem;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .upload-button:hover:not(:disabled) {
    background: #2563eb;
  }

  .upload-button:disabled {
    background: #cbd5e1;
    cursor: not-allowed;
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
