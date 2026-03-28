<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_TENANT } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';

  interface PageData {
    tenantId: string;
    archiveId: string;
  }

  export let data: PageData;

  let archive: any = null;
  let tenant: any = null;
  let loading = true;
  let extractPassword = '';
  let extracting = false;
  let extractError: string | null = null;

  const GET_ARCHIVE = gql`
    query GetArchive($id: ID!) {
      getArchive(id: $id) {
        id
        title
        description
        status
        standard
        createdAt
      }
    }
  `;

  onMount(async () => {
    const authState = get(auth);
    if (authState.role !== 'ADMIN' && authState.role !== 'TENANT') {
      toasts.error('You do not have permission to extract archives');
      goto('/');
      return;
    }
    await Promise.all([loadArchive(), loadTenant()]);
  });

  async function loadArchive() {
    try {
      const result = await client.query({
        query: GET_ARCHIVE,
        variables: { id: data.archiveId },
        fetchPolicy: 'network-only'
      });
      archive = result?.data?.getArchive || null;
    } catch (e) {
      console.error('Failed to load archive:', e);
      toasts.error('Failed to load archive');
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

  async function handleExtract() {
    if (!extractPassword) {
      extractError = 'Password is required';
      return;
    }

    extracting = true;
    extractError = null;

    try {
      const response = await fetch(`http://localhost:2020/api/archives/${data.archiveId}/extract`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password: extractPassword })
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || `Server error: ${response.status}`);
      }

      const contentDisposition = response.headers.get('Content-Disposition');
      const filenameMatch = contentDisposition?.match(/filename="?(.+?)"?$/);
      const filename = filenameMatch?.[1] || `archive_${data.archiveId}_export.json`;

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      toasts.success('Archive extracted successfully');
      extractPassword = '';
    } catch (e) {
      extractError = e instanceof Error ? e.message : 'Failed to extract archive';
    } finally {
      extracting = false;
    }
  }
</script>

<svelte:head>
  <title>Extract Archive - Arcana</title>
</svelte:head>

<div class="extract-page">
  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading archive...</p>
    </div>
  {:else if archive}
    <div class="breadcrumb">
      <a href="/tenants/{data.tenantId}">Tenant</a>
      <span class="sep">/</span>
      <a href="/tenants/{data.tenantId}/archives">Archives</a>
      <span class="sep">/</span>
      <span>{archive.title}</span>
      <span class="sep">/</span>
      <span>Extract</span>
    </div>

    <div class="extract-card">
      <div class="card-header">
        <span class="card-icon">📥</span>
        <div>
          <h1>Extract Archive</h1>
          <p class="card-subtitle">Download an encrypted copy of this archive</p>
        </div>
      </div>

      <div class="archive-info">
        <div class="info-row">
          <span class="info-label">Archive</span>
          <span class="info-value">{archive.title}</span>
        </div>
        {#if archive.description}
          <div class="info-row">
            <span class="info-label">Description</span>
            <span class="info-value">{archive.description}</span>
          </div>
        {/if}
        <div class="info-row">
          <span class="info-label">Standard</span>
          <span class="info-value badge">{archive.standard}</span>
        </div>
        <div class="info-row">
          <span class="info-label">Status</span>
          <span class="info-value badge status-{archive.status?.toLowerCase()}">{archive.status}</span>
        </div>
        {#if tenant}
          <div class="info-row">
            <span class="info-label">Tenant</span>
            <span class="info-value">{tenant.displayName || tenant.name}</span>
          </div>
        {/if}
      </div>

      <div class="extract-form">
        <p class="form-desc">Enter a password to encrypt the extracted archive. You will need this password to open the file.</p>

        {#if extractError}
          <div class="alert-error">
            <span>❌</span>
            <span>{extractError}</span>
          </div>
        {/if}

        <div class="form-group">
          <label for="password">Password *</label>
          <input
            type="password"
            id="password"
            bind:value={extractPassword}
            placeholder="Enter extraction password"
            disabled={extracting}
            on:keydown={(e) => e.key === 'Enter' && handleExtract()}
          />
        </div>

        <div class="form-actions">
          <a href="/tenants/{data.tenantId}/archives" class="btn-secondary">Cancel</a>
          <button class="btn-primary" on:click={handleExtract} disabled={extracting || !extractPassword}>
            {extracting ? 'Extracting...' : '📥 Extract & Download'}
          </button>
        </div>
      </div>
    </div>
  {:else}
    <div class="error-state">
      <h2>Archive Not Found</h2>
      <p>The requested archive could not be found.</p>
      <a href="/tenants/{data.tenantId}/archives" class="btn-secondary">Back to Archives</a>
    </div>
  {/if}
</div>

<style>
  .extract-page {
    max-width: 700px;
    margin: 0 auto;
    padding: 2rem;
  }

  .loading {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; gap: 1rem;
  }
  .spinner {
    border: 4px solid #f3f4f6; border-top: 4px solid #06b6d4;
    border-radius: 50%; width: 40px; height: 40px;
    animation: spin 1s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .breadcrumb {
    display: flex; align-items: center; gap: 0.5rem;
    margin-bottom: 1.5rem; font-size: 0.875rem; flex-wrap: wrap;
  }
  .breadcrumb a { color: #3b82f6; text-decoration: none; font-weight: 500; }
  .breadcrumb a:hover { color: #2563eb; }
  .sep { color: #94a3b8; }
  .breadcrumb > span:last-child { color: #64748b; }

  .extract-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    overflow: hidden;
  }

  .card-header {
    display: flex; align-items: center; gap: 1rem;
    padding: 2rem;
    background: linear-gradient(135deg, #06b6d4, #0891b2);
    color: white;
  }

  .card-icon { font-size: 2.5rem; }

  .card-header h1 { margin: 0 0 0.25rem; font-size: 1.5rem; }
  .card-subtitle { margin: 0; opacity: 0.9; font-size: 0.9rem; }

  .archive-info {
    padding: 1.5rem 2rem;
    border-bottom: 1px solid #f1f5f9;
  }

  .info-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 0.5rem 0;
  }

  .info-row:not(:last-child) { border-bottom: 1px solid #f8fafc; }

  .info-label {
    font-size: 0.8rem; font-weight: 600; color: #64748b;
    text-transform: uppercase; letter-spacing: 0.05em;
  }

  .info-value { color: #1e293b; font-size: 0.9rem; }

  .badge {
    padding: 0.2rem 0.6rem; border-radius: 0.25rem;
    font-size: 0.7rem; font-weight: 600; text-transform: uppercase;
    background: #eef2ff; color: #3730a3;
  }

  .status-active { background: #dcfce7; color: #166534; }
  .status-draft { background: #fef3c7; color: #92400e; }
  .status-archived { background: #f3f4f6; color: #4b5563; }

  .extract-form { padding: 2rem; }

  .form-desc {
    margin: 0 0 1.5rem; color: #64748b; font-size: 0.875rem; line-height: 1.5;
  }

  .alert-error {
    display: flex; align-items: center; gap: 0.5rem;
    padding: 0.75rem 1rem; background: #fee2e2; color: #991b1b;
    border: 1px solid #fca5a5; border-radius: 0.5rem; margin-bottom: 1rem;
    font-size: 0.875rem;
  }

  .form-group { margin-bottom: 1.5rem; }
  .form-group label {
    display: block; margin-bottom: 0.5rem;
    font-weight: 600; color: #1e293b; font-size: 0.875rem;
  }
  .form-group input {
    width: 100%; padding: 0.75rem; border: 1px solid #cbd5e1;
    border-radius: 0.5rem; font-size: 1rem; box-sizing: border-box;
  }
  .form-group input:focus {
    outline: none; border-color: #06b6d4;
    box-shadow: 0 0 0 3px rgba(6, 182, 212, 0.1);
  }
  .form-group input:disabled { background: #f1f5f9; cursor: not-allowed; }

  .form-actions { display: flex; justify-content: flex-end; gap: 0.75rem; }

  .btn-primary, .btn-secondary {
    padding: 0.75rem 1.5rem; border: none; border-radius: 0.5rem;
    font-weight: 600; cursor: pointer; transition: all 0.2s;
    text-decoration: none; display: inline-block; font-size: 0.9rem;
  }

  .btn-primary { background: #06b6d4; color: white; }
  .btn-primary:hover:not(:disabled) { background: #0891b2; }
  .btn-primary:disabled { background: #94a3b8; cursor: not-allowed; }

  .btn-secondary { background: #e2e8f0; color: #475569; }
  .btn-secondary:hover { background: #cbd5e1; }

  .error-state {
    text-align: center; padding: 4rem 2rem;
  }
  .error-state h2 { margin: 0 0 0.5rem; color: #1e293b; }
  .error-state p { margin: 0 0 1.5rem; color: #64748b; }
</style>
