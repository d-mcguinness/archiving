<script>
import { page } from '$app/stores';
import { goto } from '$app/navigation';
import { client } from '$lib/apollo';
import { GET_USER, UPDATE_USER } from '$lib/graphql/queries';
import { onMount } from 'svelte';
import { get } from 'svelte/store';

let userId = '';
let user = null;
let loading = true;
let error = null;
let updating = false;

let form = { name: '', email: '', age: '' };

onMount(async () => {
  const url = new URL(get(page).url);
  userId = url.searchParams.get('userId') || '';
  if (userId) {
    try {
      const result = await client.query({
        query: GET_USER,
        variables: { id: userId },
        fetchPolicy: 'network-only'
      });
      user = result.data.getUser;
      if (user) {
        form.name = user.name;
        form.email = user.email;
        form.age = user.age ?? '';
      }
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
    } finally {
      loading = false;
    }
  } else {
    loading = false;
    error = 'No userId provided.';
  }
});

async function updateUser() {
  if (!form.name || !form.email) return;
  try {
    updating = true;
    await client.mutate({
      mutation: UPDATE_USER,
      variables: {
        id: userId,
        input: {
          name: form.name,
          email: form.email,
          age: form.age ? parseInt(form.age) : null
        }
      }
    });
    goto('/users');
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
  } finally {
    updating = false;
  }
}
</script>

<h2>Update User</h2>
{#if loading}
  <div class="loading">Loading...</div>
{:else if error}
  <div class="error">Error: {error}</div>
{:else if user}
  <form on:submit|preventDefault={updateUser} class="form-container">
    <div class="form-group">
      <label for="name">Name *</label>
      <input type="text" id="name" bind:value={form.name} required disabled={updating} />
    </div>
    <div class="form-group">
      <label for="email">Email *</label>
      <input type="email" id="email" bind:value={form.email} required disabled={updating} />
    </div>
    <div class="form-group">
      <label for="age">Age</label>
      <input type="number" id="age" bind:value={form.age} min="0" max="150" disabled={updating} />
    </div>
    <button type="submit" disabled={updating || !form.name || !form.email}>
      {updating ? 'Updating...' : 'Update User'}
    </button>
  </form>
{/if}

<style>
  .form-container {
    max-width: 400px;
    margin: 2rem auto;
    background: #fff;
    padding: 2rem;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    border: 1px solid #e2e8f0;
  }
  .form-group {
    margin-bottom: 1.5rem;
  }
  label {
    display: block;
    margin-bottom: 0.5rem;
    color: #1e293b;
    font-weight: 500;
  }
  input {
    width: 100%;
    padding: 0.5rem;
    border-radius: 0.25rem;
    border: 1px solid #e2e8f0;
    font-size: 1rem;
  }
  button[type="submit"] {
    background: #4caf50;
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    font-weight: 500;
    border: none;
    cursor: pointer;
    transition: background 0.2s;
  }
  button[type="submit"]:disabled {
    background: #a5d6a7;
    cursor: not-allowed;
  }
  .loading {
    text-align: center;
    margin: 2rem;
    color: #64748b;
  }
  .error {
    color: #f44336;
    margin: 2rem 0;
    text-align: center;
  }
</style>
