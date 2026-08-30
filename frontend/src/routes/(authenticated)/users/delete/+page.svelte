<script>
import { page } from '$app/stores';
import { goto } from '$app/navigation';
import { client } from '$lib/apollo';
import { DELETE_USER } from '$lib/graphql/queries';
import { onMount } from 'svelte';
import { get } from 'svelte/store';
import { toasts } from '$lib/stores/toastStore';

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
    toasts.add('User deleted successfully', 'success');
    goto('/users');
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
    toasts.add(`Failed to delete user: ${error}`, 'error');
  } finally {
    deleting = false;
  }
}

function handleCancel() {
  goto('/users');
}
</script>

<span class="eyebrow">Users</span>
<h2>Delete User</h2>
{#if userId}
  <p>Are you sure you want to delete this user?</p>
  {#if error}
    <div class="error">Error: {error}</div>
  {/if}
  <div class="actions">
    <button on:click={handleDelete} disabled={deleting} class="delete-btn btn-danger">
      {deleting ? 'Deleting...' : 'Delete'}
    </button>
    <button on:click={handleCancel} disabled={deleting} class="cancel-btn btn-secondary">Cancel</button>
  </div>
{:else}
  <p>No user selected for deletion.</p>
{/if}

<style>
  /* Buttons use the global .btn-danger / .btn-secondary kit. */
  .actions {
    margin-top: 2rem;
    display: flex;
    gap: 1rem;
  }
  @media (prefers-reduced-motion: reduce) {
    .delete-btn, .cancel-btn {
      transition: none;
    }
    .delete-btn:hover:not(:disabled) {
      transform: none;
    }
  }
</style>
