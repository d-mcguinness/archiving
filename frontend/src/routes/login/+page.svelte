<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import AuthShell from '$lib/components/AuthShell.svelte';

  const devmode = import.meta.env.VITE_DEVMODE === 'true';

  let username = '';
  let password = '';
  let loading = false;
  let error = '';

  async function quickLogin(user: string, pass: string) {
    username = user;
    password = pass;
    await handleLogin();
  }

  async function handleLogin() {
    if (!username || !password) {
      error = 'Please enter both username and password';
      return;
    }

    loading = true;
    error = '';

    try {
      console.group('🔐 Login Request');
      console.log('Username:', username);
      console.log('Login Started:', new Date().toLocaleTimeString());

      const startTime = performance.now();

      const response = await fetch(`${API_BASE}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username,
          password,
        }),
      });

      const duration = performance.now() - startTime;
      console.log('Response Status:', response.status, response.statusText);
      console.log('Response Duration:', duration.toFixed(2), 'ms');

      const result = await response.json();
      console.log('Response Body:', result);

      if (response.ok && result.success) {
        console.log('✅ Login Successful!');
        console.log('User:', result.user);
        console.log('Role:', result.role);
        console.log('Token:', result.token ? '(token received)' : '(no token)');
        console.log('TenantId:', result.tenantId || '(none)');
        console.groupEnd();

        // Store auth data (incl. the refresh token used to renew the session)
        auth.login(result.token, result.user, result.role, result.tenantId, result.refreshToken);

        toasts.success(`Welcome back, ${result.user.name}!`);

        // Redirect based on role
        if (result.role === 'ADMIN') {
          goto('/admin');
        } else if (result.role === 'TENANT') {
          if (result.tenantId) {
            goto(`/tenants/${result.tenantId}/archives`);
          } else {
            goto('/');
          }
        } else if (result.role === 'USER') {
          if (result.tenantId && result.user?.id) {
            goto(`/tenants/${result.tenantId}/users/${result.user.id}`);
          } else {
            goto('/');
          }
        } else {
          goto('/');
        }
      } else {
        console.error('❌ Login Failed!');
        console.error('Error:', result.error || result.message);
        console.groupEnd();

        error = result.error || result.message || 'Login failed';
        toasts.error(`Login failed: ${error}`);
      }
    } catch (e) {
      console.error('❌ Login Error!');
      console.error('Error:', e);
      console.groupEnd();

      error = e instanceof Error ? e.message : 'Network error';
      toasts.error(`Login error: ${error}`);
    } finally {
      loading = false;
    }
  }

  function handleKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      handleLogin();
    }
  }
</script>

<svelte:head>
  <title>Sign in — Arcana</title>
</svelte:head>

<AuthShell title="Welcome back" subtitle="Sign in to continue to your Arcana archive.">
  <form on:submit|preventDefault={handleLogin}>
    {#if error}
      <div class="auth-error" role="alert">
        <span aria-hidden="true">⚠</span>
        <span>{error}</span>
      </div>
    {/if}

    <div class="auth-field">
      <label for="username">Username</label>
      <input
        class="auth-input"
        type="text"
        id="username"
        bind:value={username}
        on:keypress={handleKeyPress}
        disabled={loading}
        placeholder="Enter your username"
        autocomplete="username"
      />
    </div>

    <div class="auth-field">
      <label for="password">Password</label>
      <input
        class="auth-input"
        type="password"
        id="password"
        bind:value={password}
        on:keypress={handleKeyPress}
        disabled={loading}
        placeholder="Enter your password"
        autocomplete="current-password"
      />
    </div>

    <button type="submit" class="auth-submit" disabled={loading}>
      {#if loading}
        <span class="auth-spinner" aria-hidden="true"></span>
        <span>Signing in…</span>
      {:else}
        <span>Sign In</span>
      {/if}
    </button>
  </form>

  {#if devmode}
    <div class="auth-demo">
      <p class="auth-demo-title">Demo accounts</p>
      <div class="auth-demo-grid">
        <button class="auth-demo-card" on:click={() => quickLogin('admin', 'admin123')} disabled={loading}>Admin</button>
        <button class="auth-demo-card" on:click={() => quickLogin('tenant', 'tenant123')} disabled={loading}>Tenant</button>
        <button class="auth-demo-card" on:click={() => quickLogin('user', 'user123')} disabled={loading}>User</button>
      </div>
    </div>
  {/if}

  <p class="auth-alt">New to Arcana? <a href="/register">Create an account</a></p>
</AuthShell>
