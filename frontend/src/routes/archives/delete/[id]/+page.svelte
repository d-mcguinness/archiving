<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { gql } from '@apollo/client/core';
  import { GET_ALL_ARCHIVES } from '$lib/graphql/queries';

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
      goto('/archives');
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to delete archive';
      console.error('Delete archive error:', e);
    } finally {
      deleting = false;
    }
  }

  function cancel() {
    goto('/archives');
  }
</script>

<svelte:head>
  <title>Delete Archive - Archiving System</title>
</svelte:head>

<div class="delete-page">
  <div class="page-header">
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
          <span class="badge standard-badge">{archive.standard}</span>
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
    color: #1e293b;
    font-size: 2rem;
  }

  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    border: 1px solid #fcc;
  }

  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
  }

  .loading p {
    margin-top: 1rem;
    color: #64748b;
  }

  .spinner {
    border: 4px solid #f3f4f6;
    border-top: 4px solid #3b82f6;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: #f9fafb;
    border-radius: 0.5rem;
    color: #64748b;
  }

  .confirmation-card {
    background: white;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
    padding: 2rem;
    text-align: center;
  }

  .warning-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
  }

  .confirmation-card h2 {
    margin: 0 0 1rem 0;
    color: #dc2626;
    font-size: 1.5rem;
  }

  .warning-text {
    color: #64748b;
    margin-bottom: 2rem;
    line-height: 1.6;
  }

  .archive-details {
    background: #f8fafc;
    border-radius: 0.375rem;
    padding: 1.5rem;
    margin-bottom: 2rem;
    text-align: left;
  }

  .archive-details h3 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 600;
  }

  .detail-item {
    display: flex;
    justify-content: space-between;
    padding: 0.5rem 0;
    border-bottom: 1px solid #e2e8f0;
  }

  .detail-item:last-child {
    border-bottom: none;
  }

  .detail-label {
    font-weight: 500;
    color: #64748b;
    font-size: 0.875rem;
  }

  .detail-value {
    color: #1e293b;
    font-size: 0.875rem;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
  }

  .badge.active,
  .badge.published {
    background: #dcfce7;
    color: #166534;
  }

  .badge.draft {
    background: #f3f4f6;
    color: #6b7280;
  }

  .badge.archived,
  .badge.deleted {
    background: #fef3c7;
    color: #92400e;
  }

  .standard-badge {
    background: #e0e7ff;
    color: #4338ca;
  }

  .actions {
    display: flex;
    justify-content: center;
    gap: 1rem;
  }

  .btn {
    padding: 0.75rem 2rem;
    border: none;
    border-radius: 0.25rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;
    font-size: 1rem;
  }

  .btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #cbd5e1;
  }

  .btn-danger {
    background: #dc2626;
    color: white;
  }

  .btn-danger:hover:not(:disabled) {
    background: #b91c1c;
  }
</style>

