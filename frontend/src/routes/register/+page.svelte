<script lang="ts">
  let name = '';
  let organization = '';
  let email = '';
  let username = '';
  let password = '';
  let confirmPassword = '';
  let error = '';
  let submitted = false;

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

  function handleRegister() {
    error = validate();
    if (error) {
      return;
    }
    // No self-service backend in this demo: login authenticates a fixed set of
    // demo accounts, so we don't POST to a non-existent endpoint. Acknowledge the
    // request and route activation through an administrator.
    submitted = true;
  }
</script>

<svelte:head>
  <title>Register - Arcana</title>
</svelte:head>

<div class="register-page">
  <div class="register-container">
    {#if submitted}
      <div class="success">
        <div class="success-icon">✅</div>
        <h1>Thanks, {name}!</h1>
        <p>
          We've received your request to register <strong>{organization}</strong>.
          In this demo, accounts are provisioned by an administrator — they'll
          activate your access shortly.
        </p>
        <a href="/login" class="primary-button">Back to Sign In</a>
      </div>
    {:else}
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
          <input id="name" type="text" bind:value={name} placeholder="Ada Lovelace" autocomplete="name" />
        </div>

        <div class="form-group">
          <label for="organization">Organization</label>
          <input id="organization" type="text" bind:value={organization} placeholder="Your archive or institution" autocomplete="organization" />
        </div>

        <div class="form-group">
          <label for="email">Email</label>
          <input id="email" type="email" bind:value={email} placeholder="you@example.com" autocomplete="email" />
        </div>

        <div class="form-group">
          <label for="username">Username</label>
          <input id="username" type="text" bind:value={username} placeholder="Choose a username" autocomplete="username" />
        </div>

        <div class="form-row">
          <div class="form-group">
            <label for="password">Password</label>
            <input id="password" type="password" bind:value={password} placeholder="At least 8 characters" autocomplete="new-password" />
          </div>
          <div class="form-group">
            <label for="confirmPassword">Confirm</label>
            <input id="confirmPassword" type="password" bind:value={confirmPassword} placeholder="Re-enter password" autocomplete="new-password" />
          </div>
        </div>

        <button type="submit" class="primary-button">Create account</button>
      </form>

      <p class="signin-hint">Already have an account? <a href="/login">Sign in</a></p>
    {/if}
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

  .success {
    text-align: center;
  }

  .success-icon {
    font-size: 3rem;
    margin-bottom: 0.5rem;
  }

  .success h1 {
    margin: 0 0 0.75rem 0;
    color: #1e293b;
    font-size: 1.6rem;
  }

  .success p {
    margin: 0 0 2rem;
    color: #475569;
    line-height: 1.6;
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
