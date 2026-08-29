<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { authHeaders, API_BASE } from '$lib/api';
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
      const response = await fetch(`${API_BASE}/api/documents?${params.toString()}`, { headers: { ...authHeaders() } });
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
        <span class="eyebrow">User profile</span>
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
  .access-denied h1 { margin: 0 0 1rem; color: var(--arc-ink, #0f172a); }
  .access-denied p { color: var(--arc-muted, #64748b); }

  .loading {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; gap: 1rem;
  }
  .spinner {
    border: 4px solid var(--arc-line-strong, #e2e8f0); border-top: 4px solid var(--arc-indigo, #6366f1);
    border-radius: 50%; width: 40px; height: 40px;
    animation: spin 1s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .error {
    background: var(--arc-alert-red-bg, #fee2e2); color: var(--arc-alert-red-ink, #991b1b); padding: 1rem;
    border-radius: 0.5rem; border: 1px solid var(--arc-alert-red-border, #fca5a5);
  }

  .breadcrumb {
    display: flex; align-items: center; gap: 0.5rem;
    margin-bottom: 1.5rem; font-size: 0.875rem;
  }
  .breadcrumb a { color: var(--arc-link, #4f46e5); text-decoration: none; font-weight: 500; }
  .breadcrumb a:hover { color: var(--arc-eyebrow-ink, #7c3aed); }
  .sep { color: var(--arc-faint, #94a3b8); }
  .breadcrumb span:last-child { color: var(--arc-muted, #64748b); }

  .page-header {
    display: flex; justify-content: space-between; align-items: flex-start;
    margin-bottom: 2rem; gap: 1rem;
  }
  .header-content { flex: 1; }
  .page-header h1 { margin: 0 0 0.5rem; color: var(--arc-ink, #0f172a); font-size: 2rem; }

  .tenant-badge {
    display: inline-flex; align-items: center; gap: 0.4rem;
    padding: 0.3rem 0.8rem; background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca); border-radius: 9999px; font-weight: 700; font-size: 0.72rem;
    text-transform: uppercase; letter-spacing: 0.04em;
  }

  .header-actions { display: flex; gap: 0.5rem; }

  .btn-edit-profile {
    padding: 0.6rem 1.2rem; background: var(--arc-card, #fff); color: var(--arc-ink, #1e293b);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    border-radius: 0.65rem; font-weight: 600; text-decoration: none;
    transition: border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
  }
  .btn-edit-profile:hover {
    border-color: var(--arc-indigo, #6366f1); color: var(--arc-link, #4f46e5);
    transform: translateY(-2px);
  }

  .btn-mimic {
    padding: 0.6rem 1.2rem; background: var(--arc-chip-violet-bg, #ede9fe); color: var(--arc-chip-violet-ink, #6d28d9);
    border: none; border-radius: 0.65rem; font-weight: 600;
    cursor: pointer; transition: background 0.18s ease, transform 0.18s ease;
    box-shadow: none;
  }
  .btn-mimic:hover { background: var(--arc-chip-violet-hover, #ddd6fe); box-shadow: none; }

  .btn-docs {
    display: inline-block;
    padding: 0.6rem 1.2rem; background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    border-radius: 0.65rem; font-weight: 700; text-decoration: none;
    box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6);
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
  }
  .btn-docs:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
  }

  .panel {
    background: var(--arc-card, #fff); border: 1px solid var(--arc-line, #e8edf3); border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    padding: 1.5rem; margin-bottom: 1.5rem;
  }

  .panel-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 1.25rem; padding-bottom: 1rem; border-bottom: 1px solid var(--arc-line, #e8edf3);
  }
  .panel-header h2 { margin: 0; font-size: 1.15rem; color: var(--arc-ink, #0f172a); }

  .btn-edit {
    padding: 0.4rem 0.9rem; background: var(--arc-chip-indigo-bg, #e0e7ff); color: var(--arc-chip-indigo-ink, #4338ca);
    border: none; border-radius: 0.5rem; font-weight: 600;
    font-size: 0.8rem; cursor: pointer; transition: background 0.2s ease;
    box-shadow: none;
  }
  .btn-edit:hover { background: var(--arc-chip-indigo-hover, #c7d2fe); box-shadow: none; }

  .btn-link {
    color: var(--arc-link, #4f46e5); text-decoration: none; font-weight: 600;
    font-size: 0.85rem;
  }
  .btn-link:hover { color: var(--arc-eyebrow-ink, #7c3aed); }

  .info-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1.25rem;
  }
  .info-item { display: flex; flex-direction: column; gap: 0.25rem; }
  .info-label { font-size: 0.75rem; font-weight: 600; color: var(--arc-muted, #64748b); text-transform: uppercase; letter-spacing: 0.05em; }
  .info-value { font-size: 1rem; color: var(--arc-ink, #0f172a); }
  .mono { font-family: monospace; color: var(--arc-muted, #64748b); }

  .form-grid {
    display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 1rem;
  }
  .form-group { display: flex; flex-direction: column; gap: 0.35rem; }
  .form-group label { font-size: 0.8rem; font-weight: 600; color: var(--arc-body, #475569); margin-bottom: 0; }
  /* inputs inherit the global Arcana input styling from app.css */
  .form-group input:disabled { background: var(--arc-card-2, #f1f5f9); cursor: not-allowed; }

  .form-actions { display: flex; gap: 0.5rem; margin-top: 1.25rem; }
  .btn-primary, .btn-secondary {
    padding: 0.6rem 1.2rem; border-radius: 0.65rem;
    font-weight: 600; cursor: pointer;
  }
  /* .btn-primary inherits the global brand-gradient button styling from app.css */
  .btn-secondary {
    background: var(--arc-card, #fff); color: var(--arc-ink, #1e293b); border: 1.5px solid var(--arc-line-strong, #cbd5e1); box-shadow: none;
    transition: border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
  }
  .btn-secondary:hover:not(:disabled) {
    background: var(--arc-card, #fff); border-color: var(--arc-indigo, #6366f1); color: var(--arc-link, #4f46e5);
  }

  .muted { color: var(--arc-faint, #94a3b8); font-size: 0.875rem; margin: 0; }

  .doc-list { display: flex; flex-direction: column; gap: 0.5rem; }
  .doc-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 0.75rem; background: var(--arc-ground, #f8fafc);
    border: 1px solid var(--arc-line, #e8edf3); border-radius: 0.6rem;
  }
  .doc-info { display: flex; flex-direction: column; gap: 0.15rem; }
  .doc-title { font-weight: 500; color: var(--arc-ink, #0f172a); font-size: 0.9rem; }
  .doc-meta { font-size: 0.75rem; color: var(--arc-faint, #94a3b8); }

  .doc-status {
    padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.7rem;
    font-weight: 700; text-transform: uppercase;
  }
  .status-active { background: var(--arc-chip-green-bg, #dcfce7); color: var(--arc-chip-green-ink, #166534); }
  .status-archived { background: var(--arc-chip-slate-bg, #f1f5f9); color: var(--arc-chip-slate-ink, #475569); }
  .status-pending_review { background: var(--arc-chip-amber-bg, #fef3c7); color: var(--arc-chip-amber-ink, #92400e); }
  .status-approved { background: var(--arc-chip-indigo-bg, #e0e7ff); color: var(--arc-chip-indigo-ink, #4338ca); }
  .status-rejected { background: var(--arc-chip-red-bg, #fee2e2); color: var(--arc-chip-red-ink, #991b1b); }

  @media (max-width: 640px) {
    .page-header { flex-direction: column; }
    .info-grid, .form-grid { grid-template-columns: 1fr; }
  }
</style>
