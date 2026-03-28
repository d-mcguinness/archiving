<script lang="ts">
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';

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

      const response = await fetch('http://localhost:2020/api/auth/login', {
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

        // Store auth data
        auth.login(result.token, result.user, result.role, result.tenantId);

        toasts.success(`Welcome back, ${result.user.name}!`);

        // Redirect based on role
        if (result.role === 'ADMIN') {
          goto('/admin');
        } else if (result.role === 'TENANT') {
          if (result.tenantId) {
            goto(`/tenants/${result.tenantId}`);
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
  <title>Login - Archiving System</title>
</svelte:head>

<div class="login-page">
  <div class="login-container">
    <div class="login-header">
      <h1>🔐 Archiving System</h1>
      <p>Sign in to continue</p>
    </div>

    <form on:submit|preventDefault={handleLogin} class="login-form">
      {#if error}
        <div class="error-banner">
          <span class="error-icon">❌</span>
          <span>{error}</span>
        </div>
      {/if}

      <div class="form-group">
        <label for="username">Username</label>
        <input
          type="text"
          id="username"
          bind:value={username}
          on:keypress={handleKeyPress}
          disabled={loading}
          placeholder="Enter your username"
          autocomplete="username"
        />
      </div>

      <div class="form-group">
        <label for="password">Password</label>
        <input
          type="password"
          id="password"
          bind:value={password}
          on:keypress={handleKeyPress}
          disabled={loading}
          placeholder="Enter your password"
          autocomplete="current-password"
        />
      </div>

      <button type="submit" class="login-button" disabled={loading}>
        {#if loading}
          <span class="spinner"></span>
          <span>Signing in...</span>
        {:else}
          <span>Sign In</span>
        {/if}
      </button>
    </form>

    {#if devmode}
      <div class="demo-credentials">
        <p class="demo-title">Demo Accounts</p>
        <div class="demo-grid">
          <button class="demo-card" on:click={() => quickLogin('admin', 'admin123')} disabled={loading}>
            <span class="demo-role">Admin</span>
          </button>
          <button class="demo-card" on:click={() => quickLogin('tenant', 'tenant123')} disabled={loading}>
            <span class="demo-role">Tenant</span>
          </button>
          <button class="demo-card" on:click={() => quickLogin('user', 'user123')} disabled={loading}>
            <span class="demo-role">User</span>
          </button>
        </div>
      </div>
    {/if}

  </div>
</div>

<style>
  .login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 2rem;
  }

  .login-container {
    background: white;
    border-radius: 1rem;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    padding: 3rem;
    max-width: 450px;
    width: 100%;
  }

  .login-header {
    text-align: center;
    margin-bottom: 2rem;
  }

  .login-header h1 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .login-header p {
    margin: 0;
    color: #64748b;
    font-size: 1rem;
  }

  .login-form {
    margin-bottom: 2rem;
  }

  .error-banner {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 1rem;
    background: #fee2e2;
    border: 1px solid #fca5a5;
    border-radius: 0.5rem;
    color: #991b1b;
    margin-bottom: 1.5rem;
  }

  .error-icon {
    font-size: 1.25rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #1e293b;
  }

  .form-group input {
    width: 100%;
    padding: 0.75rem 1rem;
    border: 2px solid #e2e8f0;
    border-radius: 0.5rem;
    font-size: 1rem;
    transition: all 0.2s;
  }

  .form-group input:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  }

  .form-group input:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .login-button {
    width: 100%;
    padding: 0.875rem 1.5rem;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
  }

  .login-button:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
  }

  .login-button:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }

  .spinner {
    width: 1.25rem;
    height: 1.25rem;
    border: 3px solid rgba(255, 255, 255, 0.3);
    border-top-color: white;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .demo-credentials {
    background: #f8fafc;
    border-radius: 0.5rem;
    padding: 1.25rem;
    border: 1px solid #e2e8f0;
  }

  .demo-title {
    margin: 0 0 0.75rem 0;
    font-weight: 600;
    color: #475569;
    text-align: center;
    font-size: 0.85rem;
  }

  .demo-grid {
    display: flex;
    gap: 0.5rem;
  }

  .demo-card {
    flex: 1;
    background: white;
    padding: 0.625rem;
    border-radius: 0.375rem;
    border: 1px solid #e2e8f0;
    transition: all 0.2s;
    cursor: pointer;
    text-align: center;
    font-family: inherit;
  }

  .demo-card:hover:not(:disabled) {
    border-color: #667eea;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
    transform: translateY(-1px);
  }

  .demo-card:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .demo-role {
    font-size: 0.8rem;
    font-weight: 600;
    color: #334155;
  }

  @media (max-width: 640px) {
    .login-page {
      padding: 1rem;
    }

    .login-container {
      padding: 2rem 1.5rem;
    }

    .login-header h1 {
      font-size: 1.5rem;
    }
  }
</style>
