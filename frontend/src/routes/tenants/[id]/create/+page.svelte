<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, GET_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { gql } from '@apollo/client/core';

  interface PageData {
    tenantId: string;
  }

  export let data: PageData;

  let newArchive = {
    tenantId: data.tenantId, // Set tenantId to tenant ID from route
    ownerId: '', // Will be set to selected user ID
    userId: '', // Creator user ID
    title: '',
    description: '',
    content: '',
    standard: 'NOARK5'
  };

  let tenant: any = null;
  let users: any[] = [];
  let creating = false;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;

  const standards = [
    'NOARK5',
    'OAIS',
    'PREMIS',
    'Dublin Core',
    'METS',
    'EAD',
    'BagIt',
    'ISAD(G)',
    'MODS'
  ];

  onMount(async () => {
    // Check authentication and role
    const role = localStorage.getItem('auth_role');
    const tenantId = localStorage.getItem('auth_tenantId');
    currentRole = role || '';

    // Check access - ADMIN can create for any tenant, TENANT can create for their own
    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else if (currentRole === 'USER') {
      // USER should not access this page
      hasAccess = false;
      toasts.error('You do not have permission to create archives for this tenant');
      goto('/');
      return;
    } else {
      hasAccess = false;
      goto('/login');
      return;
    }

    await Promise.all([loadTenant(), loadUsers()]);
  });

  async function loadTenant() {
    try {
      const result = await client.query({
        query: GET_TENANT,
        variables: { id: data.tenantId },
        fetchPolicy: 'network-only'
      });
      tenant = result?.data?.getTenant || null;
    } catch (e) {
      console.error('Failed to load tenant:', e);
      toasts.error('Failed to load tenant information');
    }
  }

  async function loadUsers() {
    try {
      const result = await client.query({
        query: GET_ALL_USERS,
        fetchPolicy: 'network-only'
      });
      users = result?.data?.getAllUsers || [];
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load users';
      console.error('Load users error:', e);
      toasts.error(`Failed to load users: ${error}`);
    }
  }

  async function createArchive() {
    if (!newArchive.userId || !newArchive.ownerId || !newArchive.title || !newArchive.content) {
      toasts.error('Please fill in all required fields');
      return;
    }

    try {
      creating = true;
      error = null;

      // Map UI standard names to GraphQL enum values
      const standardMap: Record<string, string> = {
        'NOARK5': 'NOARK5',
        'OAIS': 'OAIS',
        'PREMIS': 'PREMIS',
        'Dublin Core': 'DUBLIN_CORE',
        'METS': 'METS',
        'EAD': 'EAD',
        'BagIt': 'BAGIT',
        'ISAD(G)': 'ISADG',
        'MODS': 'MODS'
      };

      const graphqlStandard = standardMap[newArchive.standard] || newArchive.standard;

      // Create mutation with both tenantId and ownerId
      const CREATE_ARCHIVE_WITH_TENANT = gql`
        mutation CreateArchive($input: CreateArchiveInput!) {
          createArchive(input: $input) {
            id
            tenantId
            ownerId
            title
            description
            content
            createdAt
            updatedAt
            status
            standard
          }
        }
      `;

      const result = await client.mutate({
        mutation: CREATE_ARCHIVE_WITH_TENANT,
        variables: {
          input: {
            tenantId: parseInt(newArchive.tenantId), // Tenant ID (organization)
            ownerId: parseInt(newArchive.ownerId),   // Owner ID (user who owns it)
            userId: parseInt(newArchive.userId),     // Creator user ID
            title: newArchive.title,
            description: newArchive.description || null,
            content: newArchive.content,
            standard: graphqlStandard
          }
        }
      });

      if (result.data?.createArchive) {
        toasts.success(`Archive "${newArchive.title}" created successfully`);
        goto(`/tenants/${data.tenantId}/archives`);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Create archive error:', e);
      toasts.error(`Failed to create archive: ${error}`);
    } finally {
      creating = false;
    }
  }

  function fillRandom() {
    const titles = ['Annual Report Archive', 'Legal Documents', 'Project Files', 'Financial Records', 'HR Documentation', 'Technical Specs', 'Client Correspondence', 'Research Data'];
    const descs = ['Collection of important organizational documents', 'Archived records for compliance purposes', 'Historical data preservation', 'Critical business documentation'];
    const contents = ['Archived content ready for long-term preservation', 'Digital records maintained per regulatory requirements', 'Organizational knowledge base archive', 'Structured data collection for institutional memory'];
    newArchive.title = titles[Math.floor(Math.random() * titles.length)];
    newArchive.description = descs[Math.floor(Math.random() * descs.length)];
    newArchive.content = contents[Math.floor(Math.random() * contents.length)];
    newArchive.standard = standards[Math.floor(Math.random() * standards.length)];
    if (users.length > 0) {
      newArchive.ownerId = users[Math.floor(Math.random() * users.length)].id;
      newArchive.userId = users[Math.floor(Math.random() * users.length)].id;
    }
  }

  function handleCancel() {
    goto(`/tenants/${data.tenantId}/archives`);
  }
</script>

<svelte:head>
  <title>Create Archive - {tenant ? tenant.displayName || tenant.name : 'Tenant'} - Archiving System</title>
</svelte:head>

<div class="create-archive-page">
  {#if !hasAccess}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to create archives for this tenant.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <a href="/tenants/{data.tenantId}/archives">← Back to Archives</a>
    </div>

    <!-- Page Header -->
    <div class="page-header">
      <div class="header-content">
        <h1>📁 Create New Archive</h1>
        <button type="button" class="btn-fill" on:click={fillRandom}>Fill Random</button>
        {#if tenant}
          <div class="tenant-badge">
            <span class="tenant-icon">🏢</span>
            <span class="tenant-name">{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
      </div>
    </div>

    {#if error}
      <div class="error-banner">
        ❌ {error}
      </div>
    {/if}

    <div class="form-container">
      <form on:submit|preventDefault={createArchive}>
        <div class="form-section">
          <h3>Archive Information</h3>

          <div class="form-group">
            <label for="ownerId">Archive Owner *</label>
            <select
              id="ownerId"
              bind:value={newArchive.ownerId}
              required
              disabled={creating}
            >
              <option value="">Select archive owner</option>
              {#each users as user}
                <option value={user.id}>{user.name} ({user.email})</option>
              {/each}
            </select>
            <small class="field-hint">User who will own this archive</small>
          </div>

          <div class="form-group">
            <label for="userId">Creator / User *</label>
            <select
              id="userId"
              bind:value={newArchive.userId}
              required
              disabled={creating}
            >
              <option value="">Select creator</option>
              {#each users as user}
                <option value={user.id}>{user.name} ({user.email})</option>
              {/each}
            </select>
            <small class="field-hint">User who is creating this archive</small>
          </div>

          <div class="form-group">
            <label for="title">Title *</label>
            <input
              type="text"
              id="title"
              bind:value={newArchive.title}
              required
              disabled={creating}
              placeholder="Enter archive title"
            />
            <small class="field-hint">A descriptive title for the archive</small>
          </div>

          <div class="form-group">
            <label for="description">Description</label>
            <textarea
              id="description"
              bind:value={newArchive.description}
              disabled={creating}
              rows="4"
              placeholder="Enter archive description (optional)"
            ></textarea>
            <small class="field-hint">Optional description of the archive contents</small>
          </div>

          <div class="form-group">
            <label for="content">Content *</label>
            <textarea
              id="content"
              bind:value={newArchive.content}
              required
              disabled={creating}
              rows="6"
              placeholder="Enter archive content"
            ></textarea>
            <small class="field-hint">Main content or metadata of the archive</small>
          </div>

          <div class="form-group">
            <label for="standard">Archive Standard *</label>
            <select
              id="standard"
              bind:value={newArchive.standard}
              required
              disabled={creating}
            >
              {#each standards as standard}
                <option value={standard}>{standard}</option>
              {/each}
            </select>
            <small class="field-hint">Archival standard/format to use</small>
          </div>

          <div class="info-box">
            <div class="info-icon">ℹ️</div>
            <div class="info-content">
              <strong>Tenant Association:</strong> This archive will be owned by and associated with
              <strong>{tenant ? tenant.displayName || tenant.name : 'this tenant'}</strong>.
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="button"
            class="btn-secondary"
            on:click={handleCancel}
            disabled={creating}
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            disabled={creating || !newArchive.ownerId || !newArchive.userId || !newArchive.title || !newArchive.content}
          >
            {creating ? '⏳ Creating...' : '✅ Create Archive'}
          </button>
        </div>
      </form>
    </div>
  {/if}
</div>

<style>
  .create-archive-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 2rem;
  }

  /* Breadcrumb */
  .breadcrumb {
    margin-bottom: 1.5rem;
  }

  .breadcrumb a {
    color: #3b82f6;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s;
  }

  .breadcrumb a:hover {
    color: #2563eb;
  }

  /* Page Header */
  .page-header {
    margin-bottom: 2rem;
  }

  .header-content {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .page-header h1 {
    margin: 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .tenant-badge {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 1rem;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: 0.5rem;
    font-weight: 600;
    width: fit-content;
  }

  .btn-fill {
    padding: 0.5rem 1rem;
    background: #f0fdf4;
    color: #16a34a;
    border: 1px solid #bbf7d0;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-fill:hover {
    background: #dcfce7;
    border-color: #86efac;
  }

  .tenant-icon {
    font-size: 1.25rem;
  }

  /* Access Denied */
  .access-denied {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    text-align: center;
    padding: 3rem;
  }

  .access-denied-icon {
    font-size: 5rem;
    margin-bottom: 1.5rem;
  }

  .access-denied h1 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: #64748b;
    font-size: 1.125rem;
  }

  .redirect-message {
    color: #3b82f6;
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  .error-banner {
    background: #fee2e2;
    border: 1px solid #fca5a5;
    color: #991b1b;
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
    text-align: center;
  }

  .form-container {
    background: white;
    padding: 2rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .form-section {
    margin-bottom: 2rem;
  }

  .form-section h3 {
    margin: 0 0 1.5rem 0;
    color: #1e293b;
    font-size: 1.25rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #475569;
    font-weight: 600;
    font-size: 0.875rem;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 1rem;
    transition: border-color 0.2s, box-shadow 0.2s;
    font-family: inherit;
  }

  .form-group input:focus,
  .form-group select:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .form-group input:disabled,
  .form-group select:disabled,
  .form-group textarea:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .form-group textarea {
    resize: vertical;
    min-height: 100px;
  }

  .field-hint {
    display: block;
    margin-top: 0.375rem;
    color: #64748b;
    font-size: 0.75rem;
  }

  .info-box {
    display: flex;
    gap: 1rem;
    padding: 1rem;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
    border-radius: 0.5rem;
    margin-top: 1.5rem;
  }

  .info-icon {
    font-size: 1.5rem;
    flex-shrink: 0;
  }

  .info-content {
    color: #1e40af;
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 2rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn-primary,
  .btn-secondary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-primary:disabled {
    background: #94a3b8;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #cbd5e1;
  }

  .btn-secondary:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  @media (max-width: 768px) {
    .create-archive-page {
      padding: 1rem;
    }

    .form-actions {
      flex-direction: column-reverse;
    }

    .form-actions button {
      width: 100%;
    }
  }
</style>

