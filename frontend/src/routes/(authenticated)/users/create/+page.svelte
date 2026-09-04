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

  function fillRandom() {
    const firstNames = ['Alice', 'Bob', 'Charlie', 'Diana', 'Erik', 'Fiona', 'George', 'Hannah'];
    const lastNames = ['Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis'];
    const first = firstNames[Math.floor(Math.random() * firstNames.length)];
    const last = lastNames[Math.floor(Math.random() * lastNames.length)];
    newUser.name = `${first} ${last}`;
    newUser.email = `${first.toLowerCase()}.${last.toLowerCase()}@example.com`;
    newUser.age = String(Math.floor(Math.random() * 50) + 20);
  }

  function handleCancel() {
    goto('/users');
  }
</script>

<div class="form-container">
  <div class="form-header">
    <span class="eyebrow">Users</span>
    <h1>Create New User</h1>
    <p class="form-description">Add a new user to the system</p>
    <button type="button" class="btn btn-fill" on:click={fillRandom}>Fill Random</button>
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
  /* card chrome comes from the global .form-container; only sizing is page-specific */
  .form-container {
    max-width: 600px;
    margin: 2rem auto;
  }

  .form-header {
    margin-bottom: 2rem;
    text-align: center;
  }

  .form-header h1 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
    font-weight: 700;
  }

  .form-description {
    margin: 0;
    color: var(--arc-muted, #64748b);
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
    background: var(--arc-alert-red-bg, #fef2f2);
    color: var(--arc-alert-red-ink, #991b1b);
    border: 1px solid var(--arc-alert-red-border, #fecaca);
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
    box-shadow: none;
  }

  .alert button:hover {
    background: none;
    transform: none;
    box-shadow: none;
  }

  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  input:disabled, select:disabled, textarea:disabled {
    background-color: var(--arc-card-2, #f8fafc);
    cursor: not-allowed;
  }

  .form-actions {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  /* .btn-primary / .btn-secondary come from the global kit.
     .btn-fill is a page-specific soft-outline chip. */
  .btn-fill {
    padding: 0.5rem 1rem;
    background: var(--arc-chip-soft-indigo-bg, #eef2ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
    border: 1px solid var(--arc-chip-indigo-hover, #c7d2fe);
    border-radius: 0.65rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s ease;
    margin-top: 0.75rem;
    box-shadow: none;
  }

  .btn-fill:hover {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    border-color: var(--arc-hover-border, #a5b4fc);
    transform: none;
    box-shadow: none;
  }

  @media (prefers-reduced-motion: reduce) {
    .btn, .btn-fill {
      transition: none;
    }
    .btn-primary:hover:not(:disabled) {
      transform: none;
    }
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
