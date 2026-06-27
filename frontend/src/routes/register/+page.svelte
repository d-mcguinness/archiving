<script lang="ts">
  import { API_BASE } from '$lib/api';
  import { goto } from '$app/navigation';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';

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
  <title>Register - Arcana</title>
</svelte:head>

<div class="register-page">
  <div class="register-container">
    <div class="register-header">
      <h1>Create your account</h1>
      <p>Start preserving on the Free plan — upgrade anytime.</p>
    </div>

    <form on:submit|preventDefault={handleRegister} class="register-form">
      {#if error}
        <div class="error-banner">
          <span class="error-icon">❌</span>
          <span>{error}</span>
        </div>
      {/if}

      <div class="form-group">
        <label for="name">Full name</label>
        <input id="name" type="text" bind:value={name} disabled={loading} placeholder="Ada Lovelace" autocomplete="name" />
      </div>

      <div class="form-group">
        <label for="organization">Organization</label>
        <input id="organization" type="text" bind:value={organization} disabled={loading} placeholder="Your archive or institution" autocomplete="organization" />
      </div>

      <div class="form-group">
        <label for="email">Email</label>
        <input id="email" type="email" bind:value={email} disabled={loading} placeholder="you@example.com" autocomplete="email" />
      </div>

      <div class="form-group">
        <label for="username">Username</label>
        <input id="username" type="text" bind:value={username} disabled={loading} placeholder="Choose a username" autocomplete="username" />
      </div>

      <div class="form-row">
        <div class="form-group">
          <label for="password">Password</label>
          <input id="password" type="password" bind:value={password} disabled={loading} placeholder="At least 8 characters" autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label for="confirmPassword">Confirm</label>
          <input id="confirmPassword" type="password" bind:value={confirmPassword} disabled={loading} placeholder="Re-enter password" autocomplete="new-password" />
        </div>
      </div>

      <button type="submit" class="primary-button" disabled={loading}>
        {loading ? 'Creating account…' : 'Create account'}
      </button>
    </form>

    <p class="signin-hint">Already have an account? <a href="/login">Sign in</a></p>
  </div>
</div>

<style>
  .register-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 2rem;
  }

  .register-container {
    background: white;
    border-radius: 1rem;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    padding: 3rem;
    max-width: 480px;
    width: 100%;
  }

  .register-header {
    text-align: center;
    margin-bottom: 2rem;
  }

  .register-header h1 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1.75rem;
  }

  .register-header p {
    margin: 0;
    color: #64748b;
    font-size: 1rem;
  }

  .register-form {
    margin-bottom: 1.25rem;
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
    margin-bottom: 1.25rem;
    flex: 1;
  }

  .form-row {
    display: flex;
    gap: 1rem;
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
    box-sizing: border-box;
  }

  .form-group input:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  }

  .primary-button {
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
    display: inline-block;
    text-align: center;
    text-decoration: none;
    box-sizing: border-box;
  }

  .primary-button:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 20px rgba(102, 126, 234, 0.3);
  }

  .signin-hint {
    text-align: center;
    margin: 0;
    color: #64748b;
    font-size: 0.9rem;
  }

  .signin-hint a {
    color: #667eea;
    font-weight: 600;
    text-decoration: none;
  }

  .signin-hint a:hover {
    text-decoration: underline;
  }

  @media (max-width: 640px) {
    .register-page {
      padding: 1rem;
    }

    .register-container {
      padding: 2rem 1.5rem;
    }

    .form-row {
      flex-direction: column;
      gap: 0;
    }
  }
</style>
