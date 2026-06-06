<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_TENANT, GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData { tenantId: string; }
  export let data: PageData;

  let tenant: any = null;
  let users: any[] = [];
  let selectedUserId = '';
  let title = '';
  let description = '';
  let selectedFile: File | null = null;
  let uploading = false;
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let hasAccess = false;
  let fileInput: HTMLInputElement;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to upload documents');
      goto(`/tenants/${data.tenantId}/documents`);
      return;
    }

    try {
      const [tenantResult, usersResult] = await Promise.all([
        client.query({ query: GET_TENANT, variables: { id: data.tenantId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_USERS })
      ]);
      tenant = tenantResult?.data?.getTenant;
      users = usersResult?.data?.getAllUsers || [];

      // Default to current user if available
      const authUser = authState.user;
      if (authUser?.id) selectedUserId = authUser.id.toString();
    } catch (e) {
      console.error('Failed to load data:', e);
    } finally {
      loading = false;
    }
  });

  function handleFileSelect(e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      selectedFile = input.files[0];
      if (!title) title = selectedFile.name.replace(/\.[^.]+$/, '');
    }
  }

  function formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  async function handleUpload() {
    if (!selectedFile || !selectedUserId || !title.trim()) return;

    uploading = true;
    error = null;

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('title', title);
      formData.append('description', description || '');
      // tenantId is a claim; the server validates it against the caller's membership.
      formData.append('tenantId', data.tenantId);

      // Identity is taken from the auth token, not request params.
      const token = typeof window !== 'undefined' ? localStorage.getItem('auth_token') : null;
      const response = await fetch('http://localhost:2020/api/documents/upload', {
        method: 'POST',
        headers: token ? { Authorization: token } : {},
        body: formData
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `Upload failed: ${response.status}`);
      }

      toasts.add(`Document "${title}" uploaded successfully`, 'success');
      goto(`/tenants/${data.tenantId}/documents`);
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to upload document';
      toasts.add(`Failed to upload: ${error}`, 'error');
    } finally {
      uploading = false;
    }
  }

  function handleCancel() {
    goto(`/tenants/${data.tenantId}/documents`);
  }
</script>

<svelte:head><title>Upload Document - {tenant?.displayName || tenant?.name || 'Tenant'} - Arcana</title></svelte:head>

{#if !hasAccess && !loading}
  <div class="access-denied"><div class="access-denied-icon">🚫</div><h1>Access Denied</h1><p>You don't have permission to upload documents.</p><p class="redirect-message">Redirecting...</p></div>
{:else}
  <div class="upload-container">
    <Breadcrumb context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }} items={[{ label: 'Documents', href: `/tenants/${data.tenantId}/documents` }, { label: 'Upload' }]} />

    <div class="form-header">
      <h1>Upload Document</h1>
      {#if tenant}<p class="subtitle">Tenant: {tenant.displayName || tenant.name}</p>{/if}
    </div>

    {#if error}<div class="alert alert-error">{error}<button on:click={() => error = null}>x</button></div>{/if}

    {#if loading}
      <div class="loading-state"><div class="spinner"></div><p>Loading...</p></div>
    {:else}
      <form on:submit|preventDefault={handleUpload}>
        <section class="form-section">
          <h2 class="section-title">File</h2>
          <div class="drop-zone" on:click={() => fileInput?.click()} on:keydown={(e) => e.key === 'Enter' && fileInput?.click()} role="button" tabindex="0">
            <input bind:this={fileInput} type="file" class="file-input" on:change={handleFileSelect} />
            {#if selectedFile}
              <div class="file-preview">
                <span class="file-icon">📄</span>
                <div class="file-details">
                  <span class="file-name">{selectedFile.name}</span>
                  <span class="file-size">{formatSize(selectedFile.size)} - {selectedFile.type || 'Unknown type'}</span>
                </div>
              </div>
            {:else}
              <div class="drop-content">
                <span class="drop-icon">📁</span>
                <p class="drop-text">Click to select a file</p>
              </div>
            {/if}
          </div>
        </section>

        <section class="form-section">
          <h2 class="section-title">Details</h2>
          <div class="form-group">
            <label for="docTitle">Title <span class="req">*</span></label>
            <input type="text" id="docTitle" bind:value={title} required placeholder="Enter document title" />
          </div>
          <div class="form-group">
            <label for="docDescription">Description</label>
            <textarea id="docDescription" bind:value={description} rows="3" placeholder="Brief description of this document..."></textarea>
          </div>
          <div class="form-group">
            <label for="docUser">Uploader <span class="req">*</span></label>
            <select id="docUser" bind:value={selectedUserId} required>
              <option value="">Select a user</option>
              {#each users as user}
                <option value={user.id}>{user.name} ({user.email})</option>
              {/each}
            </select>
          </div>
        </section>

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" on:click={handleCancel}>Cancel</button>
          <button type="submit" class="btn btn-primary" disabled={uploading || !selectedFile || !selectedUserId || !title.trim()}>
            {uploading ? 'Uploading...' : 'Upload Document'}
          </button>
        </div>
      </form>
    {/if}
  </div>
{/if}

<style>
  .access-denied { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; text-align: center; padding: 3rem; }
  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0 0 1rem 0; color: #1e293b; font-size: 2rem; }
  .access-denied p { margin: 0.5rem 0; color: #64748b; font-size: 1.125rem; }
  .redirect-message { color: #3b82f6; font-weight: 500; animation: pulse 1.5s ease-in-out infinite; }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
  .upload-container { max-width: 700px; margin: 2rem auto; background: white; padding: 2.5rem; border-radius: 0.75rem; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0; }
  .form-header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 2px solid #e2e8f0; }
  .form-header h1 { margin: 0 0 0.5rem 0; color: #1e293b; font-size: 1.75rem; font-weight: 700; }
  .subtitle { margin: 0; color: #64748b; font-size: 0.925rem; }
  .alert { padding: 0.875rem 1rem; border-radius: 0.375rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
  .alert-error { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
  .alert button { background: none; border: none; color: inherit; font-size: 1.25rem; cursor: pointer; }
  .loading-state { display: flex; flex-direction: column; align-items: center; padding: 3rem 2rem; gap: 1rem; }
  .loading-state .spinner { width: 2.5rem; height: 2.5rem; border: 3px solid #e2e8f0; border-top-color: #8b5cf6; border-radius: 50%; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .loading-state p { color: #64748b; font-size: 0.875rem; }
  .form-section { margin-bottom: 2rem; padding-bottom: 2rem; border-bottom: 1px solid #f1f5f9; }
  .form-section:last-of-type { border-bottom: none; }
  .section-title { margin: 0 0 1.25rem 0; color: #1e293b; font-size: 1.15rem; font-weight: 600; }
  .drop-zone { border: 2px dashed #cbd5e1; border-radius: 0.5rem; padding: 2rem; text-align: center; cursor: pointer; transition: all 0.2s; background: #f8fafc; }
  .drop-zone:hover { border-color: #a78bfa; background: #faf5ff; }
  .file-input { display: none; }
  .drop-content { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
  .drop-icon { font-size: 2.5rem; }
  .drop-text { margin: 0; color: #475569; font-weight: 500; }
  .file-preview { display: flex; align-items: center; gap: 1rem; text-align: left; }
  .file-icon { font-size: 2rem; }
  .file-details { display: flex; flex-direction: column; gap: 0.25rem; }
  .file-name { font-weight: 600; color: #1e293b; font-size: 0.9rem; }
  .file-size { color: #64748b; font-size: 0.8rem; }
  .form-group { margin-bottom: 1.25rem; }
  .form-group label { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; color: #1e293b; font-weight: 500; font-size: 0.85rem; }
  .req { color: #ef4444; font-weight: 600; }
  .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 0.625rem 0.75rem; border: 1px solid #e2e8f0; border-radius: 0.375rem; font-size: 0.875rem; transition: border-color 0.2s; background: white; }
  .form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: #8b5cf6; box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1); }
  .form-group textarea { resize: vertical; font-family: inherit; }
  .form-actions { display: flex; justify-content: space-between; gap: 1rem; padding-top: 1.5rem; border-top: 2px solid #e2e8f0; }
  .btn { padding: 0.75rem 2rem; border: none; border-radius: 0.375rem; font-weight: 600; cursor: pointer; transition: all 0.2s; font-size: 0.875rem; }
  .btn-primary { background: #8b5cf6; color: white; }
  .btn-primary:hover:not(:disabled) { background: #7c3aed; }
  .btn-primary:disabled { background: #94a3b8; cursor: not-allowed; }
  .btn-secondary { background: #e2e8f0; color: #475569; }
  .btn-secondary:hover { background: #cbd5e1; }
</style>
