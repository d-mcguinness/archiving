<script>
  import { client } from '$lib/apollo';
  import { CREATE_USER } from '$lib/graphql/queries';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';

  let newUser = { name: '', email: '', age: '' };
  let creating = false;
  let error = null;

  async function createUser() {
    if (!newUser.name || !newUser.email) return;
    try {
      creating = true;
      error = null;
      const result = await client.mutate({
        mutation: CREATE_USER,
        variables: {
          input: {
            name: newUser.name,
            email: newUser.email,
            age: newUser.age ? parseInt(newUser.age) : null
          }
        }
      });
      if (result.data.createUser) {
        toasts.add(`User "${newUser.name}" created successfully`, 'success');
        goto('/users');
        newUser = { name: '', email: '', age: '' };
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      toasts.add(`Failed to create user: ${error}`, 'error');
    } finally {
      creating = false;
    }
  }

  function handleCancel() {
    goto('/users');
  }
</script>

<div class="form-container">
  <div class="form-header">
    <h1>Create New User</h1>
    <p class="form-description">Add a new user to the system</p>
  </div>

  {#if error}
    <div class="alert alert-error">
      {error}
      <button on:click={() => error = null}>×</button>
    </div>
  {/if}

  <form on:submit|preventDefault={createUser}>
    <div class="form-row">
      <div class="form-group">
        <label for="name">Name *</label>
        <input
          type="text"
          id="name"
          bind:value={newUser.name}
          required
          disabled={creating}
          placeholder="Enter full name"
        />
      </div>
      <div class="form-group">
        <label for="email">Email *</label>
        <input
          type="email"
          id="email"
          bind:value={newUser.email}
          required
          disabled={creating}
          placeholder="user@example.com"
        />
      </div>
    </div>

    <div class="form-group">
      <label for="age">Age (optional)</label>
      <input
        type="number"
        id="age"
        bind:value={newUser.age}
        min="0"
        max="150"
        disabled={creating}
        placeholder="Enter age"
      />
    </div>

    <div class="form-actions">
      <button type="button" class="btn btn-secondary" on:click={handleCancel} disabled={creating}>
        Cancel
      </button>
      <button type="submit" class="btn btn-primary" disabled={creating || !newUser.name || !newUser.email}>
        {creating ? 'Creating...' : 'Create User'}
      </button>
    </div>
  </form>
</div>

<style>
  .form-container {
    max-width: 600px;
    margin: 2rem auto;
    background: white;
    padding: 2rem;
    border-radius: 1rem;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .form-header {
    margin-bottom: 2rem;
    text-align: center;
  }

  .form-header h1 {
    margin: 0 0 0.5rem 0;
    color: #1a202c;
    font-size: 2rem;
    font-weight: 700;
  }

  .form-description {
    margin: 0;
    color: #64748b;
    font-size: 1rem;
  }

  .alert {
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .alert-error {
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }

  .alert button {
    background: none;
    border: none;
    color: inherit;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    width: 1.5rem;
    height: 1.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  label {
    display: block;
    margin-bottom: 0.5rem;
    color: #1a202c;
    font-weight: 600;
    font-size: 0.875rem;
  }

  input, select, textarea {
    width: 100%;
    padding: 0.75rem 1rem;
    border-radius: 0.5rem;
    border: 1px solid #e2e8f0;
    font-size: 1rem;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  input:focus, select:focus, textarea:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  input:disabled, select:disabled, textarea:disabled {
    background-color: #f8fafc;
    cursor: not-allowed;
  }

  .form-actions {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    text-decoration: none;
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
    transform: translateY(-1px);
  }

  .btn-primary:disabled {
    background: #94a3b8;
    cursor: not-allowed;
    transform: none;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #cbd5e1;
  }

  .btn-secondary:disabled {
    background: #f1f5f9;
    color: #94a3b8;
    cursor: not-allowed;
  }

  @media (max-width: 768px) {
    .form-container {
      margin: 1rem;
      padding: 1.5rem;
    }

    .form-row {
      grid-template-columns: 1fr;
      gap: 0;
    }

    .form-actions {
      flex-direction: column-reverse;
    }

    .btn {
      width: 100%;
      justify-content: center;
    }
  }
</style>
