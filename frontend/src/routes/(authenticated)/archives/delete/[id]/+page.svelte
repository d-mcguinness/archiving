<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { gql } from '@apollo/client/core';
  import { GET_ALL_ARCHIVES } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';

  function getArchivesPath() {
    const { role } = get(auth);
    if (role === 'ADMIN' || role === 'TENANT') return '/archives';
    return '/';
  }

  let archive: any = null;
  let loading = true;
  let deleting = false;
  let error: string | null = null;

  // Get archive ID from route parameter
  $: archiveId = $page.params.id;

  onMount(async () => {

    await loadArchive();
  });

  async function loadArchive() {
    try {
      const GET_ARCHIVE = gql`
        query GetArchive($id: ID!) {
          getArchive(id: $id) {
            id
            title
            description
            status
            standard
            createdAt
            updatedAt
          }
        }
      `;

      const result = await client.query({
        query: GET_ARCHIVE,
        variables: { id: archiveId },
        fetchPolicy: 'network-only'
      });

      archive = result?.data?.getArchive;
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load archive';
      console.error('Load archive error:', e);
      toasts.add(`Failed to load archive: ${error}`, 'error');
    } finally {
      loading = false;
    }
  }

  async function deleteArchive() {
    if (!archive) return;

    try {
      deleting = true;
      const DELETE_ARCHIVE = gql`
        mutation DeleteArchive($id: ID!) {
          deleteArchive(id: $id)
        }
      `;

      await client.mutate({
        mutation: DELETE_ARCHIVE,
        variables: { id: archive.id },
        refetchQueries: [{ query: GET_ALL_ARCHIVES }],
        awaitRefetchQueries: true
      });

      // Navigate back to archives list
      toasts.add(`Archive "${archive.title}" deleted successfully`, 'success');
      goto(getArchivesPath());
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to delete archive';
      console.error('Delete archive error:', e);
      toasts.add(`Failed to delete archive: ${error}`, 'error');
    } finally {
      deleting = false;
    }
  }

  function cancel() {
    goto(getArchivesPath());
  }
</script>

<svelte:head>
  <title>Delete Archive - Archiving System</title>
</svelte:head>

<div class="delete-page">
  <div class="page-header">
    <span class="eyebrow">Archives</span>
    <h1>Delete Archive</h1>
  </div>

  {#if error}
    <div class="error">
      Error: {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading archive...</p>
    </div>
  {:else if !archive}
    <div class="empty-state">
      <p>Archive not found.</p>
      <button class="btn btn-secondary" on:click={cancel}>Go Back</button>
    </div>
  {:else}
    <div class="confirmation-card">
      <div class="warning-icon">⚠️</div>
      <h2>Are you sure you want to delete this archive?</h2>
      <p class="warning-text">This action cannot be undone. All data associated with this archive will be permanently deleted.</p>

      <div class="archive-details">
        <h3>Archive Details:</h3>
        <div class="detail-item">
          <span class="detail-label">ID:</span>
          <span class="detail-value">{archive.id}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Title:</span>
          <span class="detail-value">{archive.title}</span>
        </div>
        {#if archive.description}
          <div class="detail-item">
            <span class="detail-label">Description:</span>
            <span class="detail-value">{archive.description}</span>
          </div>
        {/if}
        <div class="detail-item">
          <span class="detail-label">Status:</span>
          <span class="badge {archive.status.toLowerCase()}">{archive.status}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Standard:</span>
          <span class="badge indigo">{archive.standard}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">Created:</span>
          <span class="detail-value">{new Date(archive.createdAt).toLocaleDateString()}</span>
        </div>
      </div>

      <div class="actions">
        <button class="btn btn-secondary" on:click={cancel} disabled={deleting}>
          Cancel
        </button>
        <button class="btn btn-danger" on:click={deleteArchive} disabled={deleting}>
          {deleting ? 'Deleting...' : 'Delete Archive'}
        </button>
      </div>
    </div>
  {/if}
</div>

<style>
  .delete-page {
    max-width: 700px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    margin-bottom: 2rem;
  }

  .page-header h1 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  /* Layout on top of the global .loading/.spinner kit */
  .loading {
    flex-direction: column;
    min-height: 400px;
  }

  .loading p {
    margin-top: 1rem;
    color: var(--arc-muted, #64748b);
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    color: var(--arc-muted, #64748b);
  }

  .confirmation-card {
    background: var(--arc-card, #fff);
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
    padding: 2rem;
    text-align: center;
  }

  .warning-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
  }

  .confirmation-card h2 {
    margin: 0 0 1rem 0;
    color: var(--arc-alert-red-ink, #dc2626);
    font-size: 1.5rem;
  }

  .warning-text {
    color: var(--arc-muted, #64748b);
    margin-bottom: 2rem;
    line-height: 1.6;
  }

  .archive-details {
    background: var(--arc-ground, #f8fafc);
    border-radius: 0.75rem;
    padding: 1.5rem;
    margin-bottom: 2rem;
    text-align: left;
  }

  .archive-details h3 {
    margin: 0 0 1rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1rem;
    font-weight: 600;
  }

  .detail-item {
    display: flex;
    justify-content: space-between;
    padding: 0.5rem 0;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .detail-item:last-child {
    border-bottom: none;
  }

  .detail-label {
    font-weight: 500;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
  }

  .detail-value {
    color: var(--arc-ink, #0f172a);
    font-size: 0.875rem;
  }

  /* Archive statuses the global .badge kit does not cover
     (published is green here, not the global indigo). */
  .badge.published {
    background: var(--arc-chip-green-bg, #dcfce7);
    color: var(--arc-chip-green-ink, #166534);
  }

  .badge.archived,
  .badge.deleted {
    background: var(--arc-chip-amber-bg, #fef3c7);
    color: var(--arc-chip-amber-ink, #92400e);
  }

  .actions {
    display: flex;
    justify-content: center;
    gap: 1rem;
  }

  /* Wider, larger confirmation buttons on top of the global
     .btn-secondary / .btn-danger kit. */
  .btn {
    padding: 0.75rem 2rem;
    font-size: 1rem;
  }

  .btn:disabled {
    opacity: 0.6;
  }
</style>

