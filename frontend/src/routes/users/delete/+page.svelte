<script>
import { page } from '$app/stores';
import { goto } from '$app/navigation';
import { client } from '$lib/apollo';
import { DELETE_USER } from '$lib/graphql/queries';
import { onMount } from 'svelte';
import { get } from 'svelte/store';

let userId = '';
let deleting = false;
let error = null;

onMount(() => {
  const url = new URL(get(page).url);
  userId = url.searchParams.get('userId') || '';
});

async function handleDelete() {
  if (!userId) return;
  try {
    deleting = true;
    await client.mutate({
      mutation: DELETE_USER,
      variables: { id: userId }
    });
    goto('/users');
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
  } finally {
    deleting = false;
  }
}

function handleCancel() {
  goto('/users');
}
</script>

<h2>Delete User</h2>
{#if userId}
  <p>Are you sure you want to delete this user?</p>
  {#if error}
    <div class="error">Error: {error}</div>
  {/if}
  <div class="actions">
    <button on:click={handleDelete} disabled={deleting} class="delete-btn">
      {deleting ? 'Deleting...' : 'Delete'}
    </button>
    <button on:click={handleCancel} disabled={deleting} class="cancel-btn">Cancel</button>
  </div>
{:else}
  <p>No user selected for deletion.</p>
{/if}

<style>
  .actions {
    margin-top: 2rem;
    display: flex;
    gap: 1rem;
  }
  .delete-btn {
    background: #f44336;
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    font-weight: 500;
    border: none;
    cursor: pointer;
    transition: background 0.2s;
  }
  .delete-btn:disabled {
    background: #e57373;
    cursor: not-allowed;
  }
  .delete-btn:hover:not(:disabled) {
    background: #c62828;
  }
  .cancel-btn {
    background: #e0e0e0;
    color: #333;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    font-weight: 500;
    border: none;
    cursor: pointer;
    transition: background 0.2s;
  }
  .cancel-btn:hover:not(:disabled) {
    background: #bdbdbd;
  }
  .error {
    color: #f44336;
    margin-top: 1rem;
  }
</style>
