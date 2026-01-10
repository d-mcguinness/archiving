<script>
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS } from '$lib/graphql/queries';

  let users = [];
  let loading = true;
  let error = null;

  onMount(async () => {
    await loadUsers();
  });

  async function loadUsers() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_USERS,
        fetchPolicy: 'network-only'
      });
      users = result.data.getAllUsers || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head>
  <title>Users - Archiving System</title>
</svelte:head>

<div class="users-page">
  <div class="page-header">
    <h1>Users</h1>
    <a href="/users/create" class="add-user-btn">Add User</a>
  </div>

  {#if error}
    <div class="error">
      Error: {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
    </div>
  {:else}
    <div class="users-grid">
      {#each users as user (user.id)}
        <div class="user-card">
          <h3>{user.name}</h3>
          <p class="email">{user.email}</p>
          {#if user.age}
            <p class="age">Age: {user.age}</p>
          {/if}
          <div class="user-id">ID: {user.id}</div>
          <div class="user-actions">
            <a href={`/users/update?userId=${user.id}`} class="update-btn">Update</a>
            <a href={`/users/delete?userId=${user.id}`} class="delete-btn">Delete</a>
          </div>
        </div>
      {:else}
        <div class="empty-state">
          <p>No users found. Create your first user to get started!</p>
        </div>
      {/each}
    </div>
  {/if}
</div>

<style>
  .users-page {
    max-width: 1200px;
    margin: 0 auto;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
  }

  .page-header h1 {
    margin: 0;
    color: #1e293b;
  }

  .add-user-btn {
    background: #3b82f6;
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    text-decoration: none;
    font-weight: 500;
    transition: background 0.2s;
  }
  .add-user-btn:hover {
    background: #2563eb;
  }

  .users-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 1.5rem;
  }

  .user-card {
    background: white;
    padding: 1.5rem;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .user-card h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
  }

  .email {
    color: #3b82f6;
    margin: 0.5rem 0;
  }

  .age {
    color: #64748b;
    margin: 0.5rem 0;
  }

  .user-id {
    font-size: 0.75rem;
    color: #9ca3af;
    margin-top: 1rem;
    font-family: monospace;
  }

  .user-actions {
    margin-top: 1rem;
    display: flex;
    gap: 1rem;
  }

  .update-btn,
  .delete-btn {
    padding: 0.5rem 1rem;
    border-radius: 0.25rem;
    text-decoration: none;
    font-weight: 500;
    transition: background 0.2s;
  }

  .update-btn {
    background: #4caf50;
    color: white;
  }
  .update-btn:hover {
    background: #388e3c;
  }

  .delete-btn {
    background: #f44336;
    color: white;
  }
  .delete-btn:hover {
    background: #c62828;
  }

  .empty-state {
    grid-column: 1 / -1;
    text-align: center;
    padding: 3rem;
    color: #64748b;
  }
</style>
