<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_USER, GET_TENANT, UPDATE_USER } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
    userId: string;
  }

  export let data: PageData;

  let user: any = null;
  let tenant: any = null;
  let loading = true;
  let error: string | null = null;
  let hasAccess = false;

  // Edit state
  let editing = false;
  let saving = false;
  let editForm = {
    name: '',
    email: '',
    age: null as number | null
  };

  // Documents state
  let documents: any[] = [];
  let loadingDocuments = false;

  onMount(async () => {
    const authState = get(auth);
    const currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    const authUserId = authState.user?.id?.toString() ?? null;

    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else if (currentRole === 'USER' && authUserId === data.userId) {
      hasAccess = true;
    } else {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to view this page');
      goto('/');
      return;
    }

    await Promise.all([loadUser(), loadTenant(), loadDocuments()]);
  });

  async function loadUser() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_USER,
        variables: { id: data.userId },
        fetchPolicy: 'network-only'
      });
      user = result?.data?.getUser || null;
      if (user) {
        editForm = { name: user.name, email: user.email, age: user.age };
      } else {
        error = 'User not found';
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load user';
    } finally {
      loading = false;
    }
  }

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
    }
  }

  async function loadDocuments() {
    loadingDocuments = true;
    try {
      const params = new URLSearchParams();
      params.append('userId', data.userId);
      params.append('tenantId', data.tenantId);
      const response = await fetch(`http://localhost:2020/api/documents?${params.toString()}`);
      if (response.ok) {
        const result = await response.json();
        if (result.success) {
          documents = result.documents || [];
        }
      }
    } catch (e) {
      console.error('Failed to load documents:', e);
    } finally {
      loadingDocuments = false;
    }
  }

  function startEdit() {
    editForm = { name: user.name, email: user.email, age: user.age };
    editing = true;
  }

  function cancelEdit() {
    editing = false;
    editForm = { name: user.name, email: user.email, age: user.age };
  }

  async function saveEdit() {
    if (!editForm.name.trim() || !editForm.email.trim()) {
      toasts.error('Name and email are required');
      return;
    }
    saving = true;
    try {
      const result = await client.mutate({
        mutation: UPDATE_USER,
        variables: {
          id: data.userId,
          input: {
            name: editForm.name.trim(),
            email: editForm.email.trim(),
            age: editForm.age
          }
        }
      });
      if (result.data?.updateUser) {
        user = result.data.updateUser;
        editing = false;
        toasts.success('User updated successfully');
      }
    } catch (e) {
      toasts.error(e instanceof Error ? e.message : 'Failed to update user');
    } finally {
      saving = false;
    }
  }

  async function mimicUser() {
    if (!user) return;
    const token = 'Bearer_mimic_' + user.id + '_USER_' + Date.now();
    const mimicData = {
      id: user.id,
      username: user.name?.toLowerCase().replace(/\s+/g, '') || 'user',
      name: user.name,
      email: user.email,
      role: 'USER',
    };
    auth.login(token, mimicData, 'USER', parseInt(data.tenantId, 10));
    toasts.success(`Now viewing as ${user.name}`);
    goto(`/tenants/${data.tenantId}/users/${user.id}`);
  }

  function formatFileSize(bytes: number): string {
    if (!bytes) return 'Unknown';
    const mb = bytes / (1024 * 1024);
    if (mb > 1) return `${mb.toFixed(2)} MB`;
    return `${(bytes / 1024).toFixed(2)} KB`;
  }

  function formatDate(dateString: string): string {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleDateString();
  }
</script>

<svelte:head>
  <title>{user ? `${user.name} - ` : ''}User Detail - Archiving System</title>
</svelte:head>

<div class="user-detail-page">
  {#if !hasAccess && !loading}
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to view this user.</p>
    </div>
  {:else if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading user...</p>
    </div>
  {:else if error}
    <div class="error">❌ {error}</div>
  {:else if user}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name, userId: data.userId, userName: user?.name }}
      items={[]}
    />

    <div class="page-header">
      <div class="header-content">
        <h1>👤 {user.name}</h1>
        {#if tenant}
          <div class="tenant-badge">
            <span>🏢</span>
            <span>{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
      </div>
      <div class="header-actions">
        <a href="/tenants/{data.tenantId}/users/{data.userId}/edit" class="btn-edit-profile">✏️ Edit Profile</a>
        <button class="btn-mimic" on:click={mimicUser}>🎭 Mimic</button>
        <a href="/tenants/{data.tenantId}/users/{data.userId}/documents" class="btn-docs">📄 Documents</a>
      </div>
    </div>

    <!-- User Info -->
    <div class="panel">
      <div class="panel-header">
        <h2>User Information</h2>
        {#if !editing}
          <button class="btn-edit" on:click={startEdit}>✏️ Edit</button>
        {/if}
      </div>

      {#if editing}
        <form on:submit|preventDefault={saveEdit}>
          <div class="form-grid">
            <div class="form-group">
              <label for="edit-name">Name *</label>
              <input id="edit-name" type="text" bind:value={editForm.name} disabled={saving} required />
            </div>
            <div class="form-group">
              <label for="edit-email">Email *</label>
              <input id="edit-email" type="email" bind:value={editForm.email} disabled={saving} required />
            </div>
            <div class="form-group">
              <label for="edit-age">Age</label>
              <input id="edit-age" type="number" bind:value={editForm.age} min="0" max="150" disabled={saving} />
            </div>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
            <button type="button" class="btn-secondary" on:click={cancelEdit} disabled={saving}>Cancel</button>
          </div>
        </form>
      {:else}
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">ID</span>
            <span class="info-value mono">{user.id}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Name</span>
            <span class="info-value">{user.name}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Email</span>
            <span class="info-value">{user.email}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Age</span>
            <span class="info-value">{user.age || '-'}</span>
          </div>
        </div>
      {/if}
    </div>

    <!-- Recent Documents -->
    <div class="panel">
      <div class="panel-header">
        <h2>Recent Documents</h2>
        <a href="/tenants/{data.tenantId}/users/{data.userId}/documents" class="btn-link">View All →</a>
      </div>

      {#if loadingDocuments}
        <p class="muted">Loading documents...</p>
      {:else if documents.length === 0}
        <p class="muted">No documents found for this user.</p>
      {:else}
        <div class="doc-list">
          {#each documents.slice(0, 5) as doc}
            <div class="doc-row">
              <div class="doc-info">
                <span class="doc-title">{doc.title || doc.fileName}</span>
                <span class="doc-meta">{formatFileSize(doc.fileSize)} &middot; {formatDate(doc.uploadedAt)}</span>
              </div>
              <span class="doc-status status-{doc.status?.toLowerCase()}">{doc.status}</span>
            </div>
          {/each}
        </div>
        {#if documents.length > 5}
          <p class="muted" style="margin-top:0.75rem;">+ {documents.length - 5} more documents</p>
        {/if}
      {/if}
    </div>
  {/if}
</div>

<style>
  .user-detail-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 2rem;
  }

  .access-denied {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; text-align: center;
  }
  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0 0 1rem; color: #1e293b; }
  .access-denied p { color: #64748b; }

  .loading {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; gap: 1rem;
  }
  .spinner {
    border: 4px solid #f3f4f6; border-top: 4px solid #f59e0b;
    border-radius: 50%; width: 40px; height: 40px;
    animation: spin 1s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .error {
    background: #fee2e2; color: #991b1b; padding: 1rem;
    border-radius: 0.5rem; border: 1px solid #fca5a5;
  }

  .breadcrumb {
    display: flex; align-items: center; gap: 0.5rem;
    margin-bottom: 1.5rem; font-size: 0.875rem;
  }
  .breadcrumb a { color: #3b82f6; text-decoration: none; font-weight: 500; }
  .breadcrumb a:hover { color: #2563eb; }
  .sep { color: #94a3b8; }
  .breadcrumb span:last-child { color: #64748b; }

  .page-header {
    display: flex; justify-content: space-between; align-items: flex-start;
    margin-bottom: 2rem; gap: 1rem;
  }
  .header-content { flex: 1; }
  .page-header h1 { margin: 0 0 0.5rem; color: #1e293b; font-size: 2rem; }

  .tenant-badge {
    display: inline-flex; align-items: center; gap: 0.4rem;
    padding: 0.4rem 0.8rem; background: linear-gradient(135deg, #f59e0b, #d97706);
    color: white; border-radius: 0.375rem; font-weight: 600; font-size: 0.8rem;
  }

  .header-actions { display: flex; gap: 0.5rem; }

  .btn-edit-profile {
    padding: 0.6rem 1.2rem; background: #f59e0b; color: white;
    border-radius: 0.5rem; font-weight: 600; text-decoration: none;
    transition: background 0.2s;
  }
  .btn-edit-profile:hover { background: #d97706; }

  .btn-mimic {
    padding: 0.6rem 1.2rem; background: #8b5cf6; color: white;
    border: none; border-radius: 0.5rem; font-weight: 600;
    cursor: pointer; transition: background 0.2s;
  }
  .btn-mimic:hover { background: #7c3aed; }

  .btn-docs {
    padding: 0.6rem 1.2rem; background: #3b82f6; color: white;
    border-radius: 0.5rem; font-weight: 600; text-decoration: none;
    transition: background 0.2s;
  }
  .btn-docs:hover { background: #2563eb; }

  .panel {
    background: white; border: 1px solid #e2e8f0; border-radius: 0.75rem;
    padding: 1.5rem; margin-bottom: 1.5rem;
  }

  .panel-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 1.25rem; padding-bottom: 1rem; border-bottom: 1px solid #f1f5f9;
  }
  .panel-header h2 { margin: 0; font-size: 1.15rem; color: #1e293b; }

  .btn-edit {
    padding: 0.4rem 0.9rem; background: #dbeafe; color: #1e40af;
    border: none; border-radius: 0.375rem; font-weight: 600;
    font-size: 0.8rem; cursor: pointer; transition: background 0.2s;
  }
  .btn-edit:hover { background: #bfdbfe; }

  .btn-link {
    color: #3b82f6; text-decoration: none; font-weight: 600;
    font-size: 0.85rem;
  }
  .btn-link:hover { color: #2563eb; }

  .info-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1.25rem;
  }
  .info-item { display: flex; flex-direction: column; gap: 0.25rem; }
  .info-label { font-size: 0.75rem; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
  .info-value { font-size: 1rem; color: #1e293b; }
  .mono { font-family: monospace; color: #64748b; }

  .form-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1rem;
  }
  .form-group { display: flex; flex-direction: column; gap: 0.35rem; }
  .form-group label { font-size: 0.8rem; font-weight: 600; color: #475569; }
  .form-group input {
    padding: 0.6rem; border: 1px solid #cbd5e1; border-radius: 0.375rem; font-size: 0.95rem;
  }
  .form-group input:focus { outline: none; border-color: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.1); }
  .form-group input:disabled { background: #f1f5f9; cursor: not-allowed; }

  .form-actions { display: flex; gap: 0.5rem; margin-top: 1.25rem; }
  .btn-primary, .btn-secondary {
    padding: 0.6rem 1.2rem; border: none; border-radius: 0.375rem;
    font-weight: 600; cursor: pointer; transition: all 0.2s;
  }
  .btn-primary { background: #3b82f6; color: white; }
  .btn-primary:hover:not(:disabled) { background: #2563eb; }
  .btn-primary:disabled { background: #94a3b8; cursor: not-allowed; }
  .btn-secondary { background: #e2e8f0; color: #475569; }
  .btn-secondary:hover:not(:disabled) { background: #cbd5e1; }

  .muted { color: #94a3b8; font-size: 0.875rem; margin: 0; }

  .doc-list { display: flex; flex-direction: column; gap: 0.5rem; }
  .doc-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 0.75rem; background: #f8fafc; border-radius: 0.375rem;
  }
  .doc-info { display: flex; flex-direction: column; gap: 0.15rem; }
  .doc-title { font-weight: 500; color: #1e293b; font-size: 0.9rem; }
  .doc-meta { font-size: 0.75rem; color: #94a3b8; }

  .doc-status {
    padding: 0.2rem 0.6rem; border-radius: 0.25rem; font-size: 0.7rem;
    font-weight: 600; text-transform: uppercase;
  }
  .status-active { background: #dcfce7; color: #166534; }
  .status-archived { background: #f3f4f6; color: #4b5563; }
  .status-pending_review { background: #fef3c7; color: #92400e; }
  .status-approved { background: #dbeafe; color: #1e40af; }
  .status-rejected { background: #fee2e2; color: #991b1b; }

  @media (max-width: 640px) {
    .page-header { flex-direction: column; }
    .info-grid, .form-grid { grid-template-columns: 1fr; }
  }
</style>
