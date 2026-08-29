<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_TENANT } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

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
      const response = await fetch(`${API_BASE}/api/archives/${data.archiveId}/extract`, {
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
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[{ label: 'Archives', href: '/tenants/' + data.tenantId + '/archives' }, { label: archive.title, href: '/tenants/' + data.tenantId + '/archives/' + data.archiveId + '/update' }, { label: 'Extract' }]}
    />

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
    border: 4px solid var(--arc-line-strong); border-top: 4px solid var(--arc-indigo);
    border-radius: 50%; width: 40px; height: 40px;
    animation: spin 1s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .breadcrumb {
    display: flex; align-items: center; gap: 0.5rem;
    margin-bottom: 1.5rem; font-size: 0.875rem; flex-wrap: wrap;
  }
  .breadcrumb a { color: var(--arc-link); text-decoration: none; font-weight: 500; }
  .breadcrumb a:hover { color: var(--arc-eyebrow-ink); }
  .sep { color: var(--arc-faint); }
  .breadcrumb > span:last-child { color: var(--arc-muted); }

  .extract-card {
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card);
    overflow: hidden;
  }

  .card-header {
    display: flex; align-items: center; gap: 1rem;
    padding: 2rem;
    background: var(--arc-grad-dark, radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%));
    color: #cbd5e1;
  }

  .card-icon { font-size: 2.5rem; }

  .card-header h1 { margin: 0 0 0.25rem; font-size: 1.5rem; color: #f8fafc; }
  .card-subtitle { margin: 0; color: #cbd5e1; font-size: 0.9rem; }

  .archive-info {
    padding: 1.5rem 2rem;
    border-bottom: 1px solid var(--arc-line);
  }

  .info-row {
    display: flex; justify-content: space-between; align-items: center;
    padding: 0.5rem 0;
  }

  .info-row:not(:last-child) { border-bottom: 1px solid var(--arc-line); }

  .info-label {
    font-size: 0.8rem; font-weight: 600; color: var(--arc-muted);
    text-transform: uppercase; letter-spacing: 0.05em;
  }

  .info-value { color: var(--arc-ink); font-size: 0.9rem; }

  .badge {
    padding: 0.25rem 0.75rem; border-radius: 9999px;
    font-size: 0.72rem; font-weight: 700; letter-spacing: 0.04em; text-transform: uppercase;
    background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink);
  }

  .status-active { background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink); }
  .status-draft { background: var(--arc-chip-amber-bg); color: var(--arc-chip-amber-ink); }
  .status-archived { background: var(--arc-chip-slate-bg); color: var(--arc-chip-slate-ink); }

  .extract-form { padding: 2rem; }

  .form-desc {
    margin: 0 0 1.5rem; color: var(--arc-muted); font-size: 0.875rem; line-height: 1.5;
  }

  .alert-error {
    display: flex; align-items: center; gap: 0.5rem;
    padding: 0.75rem 1rem; background: var(--arc-alert-red-bg); color: var(--arc-alert-red-ink);
    border: 1px solid var(--arc-alert-red-border); border-radius: 0.5rem; margin-bottom: 1rem;
    font-size: 0.875rem;
  }

  .form-group { margin-bottom: 1.5rem; }
  .form-group label {
    display: block; margin-bottom: 0.5rem;
    font-weight: 600; color: var(--arc-ink); font-size: 0.875rem;
  }
  .form-group input:disabled { background: var(--arc-card-2); cursor: not-allowed; }

  .form-actions { display: flex; justify-content: flex-end; gap: 0.75rem; }

  .btn-primary, .btn-secondary {
    padding: 0.75rem 1.5rem; border: none; border-radius: 0.65rem;
    font-weight: 700; cursor: pointer;
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
    text-decoration: none; display: inline-block; font-size: 0.9rem;
  }

  .btn-primary {
    background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white;
    box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6);
  }
  .btn-primary:hover:not(:disabled) {
    background: linear-gradient(135deg, #4f46e5, #7c3aed);
    transform: translateY(-2px);
  }
  .btn-primary:disabled { background: var(--arc-disabled-bg); box-shadow: none; cursor: not-allowed; }

  .btn-secondary { background: var(--arc-card); border: 1.5px solid var(--arc-line-strong); color: var(--arc-ink); }
  .btn-secondary:hover { border-color: var(--arc-indigo); color: var(--arc-link); }

  @media (prefers-reduced-motion: reduce) {
    .spinner { animation: none; }
    .btn-primary, .btn-secondary { transition: none; }
    .btn-primary:hover:not(:disabled) { transform: none; }
  }

  .error-state {
    text-align: center; padding: 4rem 2rem;
  }
  .error-state h2 { margin: 0 0 0.5rem; color: var(--arc-ink); }
  .error-state p { margin: 0 0 1.5rem; color: var(--arc-muted); }
</style>
