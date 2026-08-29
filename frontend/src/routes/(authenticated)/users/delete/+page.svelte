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
    <button on:click={handleDelete} disabled={deleting} class="delete-btn">
      {deleting ? 'Deleting...' : 'Delete'}
    </button>
    <button on:click={handleCancel} disabled={deleting} class="cancel-btn">Cancel</button>
  </div>
{:else}
  <p>No user selected for deletion.</p>
{/if}

<style>
  h2 {
    color: var(--arc-ink, #0f172a);
  }
  .actions {
    margin-top: 2rem;
    display: flex;
    gap: 1rem;
  }
  .delete-btn {
    background: linear-gradient(135deg, #ef4444, #dc2626);
    color: white;
    padding: 0.65rem 1.5rem;
    border-radius: 0.65rem;
    font-weight: 700;
    border: none;
    cursor: pointer;
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
    box-shadow: 0 10px 30px -8px rgba(220, 38, 38, 0.45);
  }
  .delete-btn:disabled {
    background: #fca5a5;
    box-shadow: none;
    cursor: not-allowed;
    transform: none;
  }
  .delete-btn:hover:not(:disabled) {
    background: linear-gradient(135deg, #dc2626, #b91c1c);
    transform: translateY(-2px);
  }
  .cancel-btn {
    background: var(--arc-card, #fff);
    color: var(--arc-ink, #1e293b);
    padding: 0.65rem 1.5rem;
    border-radius: 0.65rem;
    font-weight: 700;
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    cursor: pointer;
    transition: border-color 0.18s ease, color 0.18s ease;
    box-shadow: none;
  }
  .cancel-btn:hover:not(:disabled) {
    background: var(--arc-card, #fff);
    border-color: var(--arc-indigo, #6366f1);
    color: var(--arc-indigo-deep, #4f46e5);
    transform: none;
  }
  .cancel-btn:disabled {
    background: var(--arc-card-2, #f1f5f9);
    border-color: var(--arc-line-strong, #e2e8f0);
    color: var(--arc-faint, #94a3b8);
    cursor: not-allowed;
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
