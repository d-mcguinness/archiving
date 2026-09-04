<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import AuthShell from '$lib/components/AuthShell.svelte';

  let name = '';
  let organization = '';
  let email = '';
  let username = '';
  let password = '';
  let confirmPassword = '';
  let error = '';
  let loading = false;

  function validate(): string {
    if (!name || !organization || !email || !username || !password || !confirmPassword) {
      return 'Please fill in all fields';
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return 'Please enter a valid email address';
    }
    if (password.length < 8) {
      return 'Password must be at least 8 characters';
    }
    if (password !== confirmPassword) {
      return 'Passwords do not match';
    }
    return '';
  }

  async function handleRegister() {
    error = validate();
    if (error) {
      return;
    }
    loading = true;
    try {
      const response = await fetch(`${API_BASE}/api/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, organization, email, username, password })
      });
      const result = await response.json();

      if (response.ok && result.success) {
        // Signup creates a FREE-plan tenant the user owns and logs them straight in.
        auth.login(result.token, result.user, result.role, result.tenantId, result.refreshToken);
        toasts.success(`Welcome to Arcana, ${result.user.name}!`);
        if (result.tenantId) {
          goto(`/tenants/${result.tenantId}/archives`);
        } else {
          goto('/');
        }
      } else {
        error = result.error || result.message || 'Registration failed';
        toasts.error(`Registration failed: ${error}`);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Network error';
      toasts.error(`Registration error: ${error}`);
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head>
  <title>Create your account — Arcana</title>
</svelte:head>

<AuthShell title="Create your account" subtitle="Start preserving on the Free plan — upgrade anytime.">
  <form on:submit|preventDefault={handleRegister}>
    {#if error}
      <div class="auth-error" role="alert">
        <span aria-hidden="true">⚠</span>
        <span>{error}</span>
      </div>
    {/if}

    <div class="auth-field">
      <label for="name">Full name</label>
      <input class="auth-input" id="name" type="text" bind:value={name} disabled={loading} placeholder="Ada Lovelace" autocomplete="name" />
    </div>

    <div class="auth-field">
      <label for="organization">Organization</label>
      <input class="auth-input" id="organization" type="text" bind:value={organization} disabled={loading} placeholder="Your archive or institution" autocomplete="organization" />
    </div>

    <div class="auth-field">
      <label for="email">Email</label>
      <input class="auth-input" id="email" type="email" bind:value={email} disabled={loading} placeholder="you@example.com" autocomplete="email" />
    </div>

    <div class="auth-field">
      <label for="username">Username</label>
      <input class="auth-input" id="username" type="text" bind:value={username} disabled={loading} placeholder="Choose a username" autocomplete="username" />
    </div>

    <div class="auth-row">
      <div class="auth-field">
        <label for="password">Password</label>
        <input class="auth-input" id="password" type="password" bind:value={password} disabled={loading} placeholder="At least 8 characters" autocomplete="new-password" />
      </div>
      <div class="auth-field">
        <label for="confirmPassword">Confirm</label>
        <input class="auth-input" id="confirmPassword" type="password" bind:value={confirmPassword} disabled={loading} placeholder="Re-enter password" autocomplete="new-password" />
      </div>
    </div>

    <button type="submit" class="auth-submit" disabled={loading}>
      {#if loading}
        <span class="auth-spinner" aria-hidden="true"></span>
        <span>Creating account…</span>
      {:else}
        <span>Create account</span>
      {/if}
    </button>
  </form>

  <p class="auth-alt">Already have an account? <a href="/login">Sign in</a></p>
</AuthShell>
