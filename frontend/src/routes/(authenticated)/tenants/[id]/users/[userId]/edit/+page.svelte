<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_USER, GET_TENANT, UPDATE_USER } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

  interface PageData {
    tenantId: string;
    userId: string;
  }

  export let data: PageData;

  let user: any = null;
  let tenant: any = null;
  let loading = true;
  let saving = false;
  let error: string | null = null;

  let form = {
    name: '',
    email: '',
    age: null as number | null
  };

  onMount(async () => {
    const authState = get(auth);
    const currentRole = authState.role;
    const authUserId = authState.user?.id?.toString() ?? null;

    // Access: ADMIN, TENANT of this tenant, or USER editing their own profile
    if (currentRole === 'ADMIN' ||
        (currentRole === 'TENANT' && authState.tenantId?.toString() === data.tenantId) ||
        (currentRole === 'USER' && authUserId === data.userId)) {
      await Promise.all([loadUser(), loadTenant()]);
    } else {
      toasts.error('You do not have permission to edit this profile');
      goto('/');
    }
  });

  async function loadUser() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_USER,
        variables: { id: data.userId },
        fetchPolicy: 'network-only'
      });
      user = result?.data?.getUser || null;
      if (user) {
        form = { name: user.name, email: user.email, age: user.age };
      } else {
        error = 'User not found';
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load user';
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

  async function handleSave() {
    if (!form.name.trim()) {
      toasts.error('Name is required');
      return;
    }
    if (!form.email.trim()) {
      toasts.error('Email is required');
      return;
    }

    saving = true;
    try {
      const result = await client.mutate({
        mutation: UPDATE_USER,
        variables: {
          id: data.userId,
          input: {
            name: form.name.trim(),
            email: form.email.trim(),
            age: form.age
          }
        }
      });

      if (result.data?.updateUser) {
        user = result.data.updateUser;

        // If user edited their own profile, update the auth store
        const authState = get(auth);
        if (authState.user?.id?.toString() === data.userId) {
          const updatedUser = { ...authState.user, name: form.name.trim(), email: form.email.trim() };
          auth.login(
            localStorage.getItem('auth_token') || '',
            updatedUser,
            authState.role,
            authState.tenantId
          );
        }

        toasts.success('Profile updated successfully');
        goto(`/tenants/${data.tenantId}/users/${data.userId}`);
      }
    } catch (e) {
      toasts.error(e instanceof Error ? e.message : 'Failed to update profile');
    } finally {
      saving = false;
    }
  }
</script>

<svelte:head>
  <title>Edit Profile - Arcana</title>
</svelte:head>

<div class="edit-page">
  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading profile...</p>
    </div>
  {:else if error}
    <div class="error-msg">❌ {error}</div>
  {:else if user}
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name, userId: data.userId, userName: user?.name }}
      items={[{ label: 'Edit Profile' }]}
    />

    <div class="edit-card">
      <div class="card-header">
        <h1>Edit Profile</h1>
        <p>{user.name}</p>
      </div>

      <form on:submit|preventDefault={handleSave}>
        <div class="form-body">
          <div class="form-group">
            <label for="edit-name">Name *</label>
            <input
              id="edit-name"
              type="text"
              bind:value={form.name}
              placeholder="Full name"
              required
              disabled={saving}
            />
          </div>

          <div class="form-group">
            <label for="edit-email">Email *</label>
            <input
              id="edit-email"
              type="email"
              bind:value={form.email}
              placeholder="Email address"
              required
              disabled={saving}
            />
          </div>

          <div class="form-group">
            <label for="edit-age">Age</label>
            <input
              id="edit-age"
              type="number"
              bind:value={form.age}
              placeholder="Age"
              min="0"
              max="150"
              disabled={saving}
            />
          </div>
        </div>

        <div class="form-footer">
          <a href="/tenants/{data.tenantId}/users/{data.userId}" class="btn-secondary">Cancel</a>
          <button type="submit" class="btn-primary" disabled={saving}>
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </div>
  {/if}
</div>

<style>
  .edit-page {
    max-width: 600px;
    margin: 0 auto;
    padding: 2rem;
  }

  .loading {
    display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-height: 400px; gap: 1rem;
  }
  .spinner {
    border: 4px solid #f3f4f6; border-top: 4px solid #3b82f6;
    border-radius: 50%; width: 40px; height: 40px;
    animation: spin 1s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  .error-msg {
    background: #fee2e2; color: #991b1b; padding: 1rem;
    border-radius: 0.5rem; border: 1px solid #fca5a5;
  }

  .edit-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    overflow: hidden;
  }

  .card-header {
    padding: 1.5rem 2rem;
    border-bottom: 1px solid #f1f5f9;
  }

  .card-header h1 {
    margin: 0 0 0.25rem;
    font-size: 1.5rem;
    color: #0f172a;
  }

  .card-header p {
    margin: 0;
    color: #64748b;
    font-size: 0.9rem;
  }

  .form-body {
    padding: 2rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #475569;
    font-size: 0.875rem;
  }

  .form-group input {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 1rem;
    box-sizing: border-box;
    transition: border-color 0.2s;
  }

  .form-group input:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .form-group input:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .form-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    padding: 1.5rem 2rem;
    border-top: 1px solid #f1f5f9;
    background: #f8fafc;
  }

  .btn-primary, .btn-secondary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-weight: 600;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
    text-decoration: none;
    display: inline-block;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-primary:disabled {
    background: #94a3b8;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover {
    background: #cbd5e1;
  }
</style>
