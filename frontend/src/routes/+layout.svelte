<script lang="ts">
  import '../app.css';
  import Toast from '$lib/components/Toast.svelte';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { auth } from '$lib/stores/authStore';

  // Reactive declaration ensures this updates whenever the route changes
  $: currentPath = $page.url.pathname;

  // Check if a path is active based on current route
  $: isActive = (path: string): boolean => {
    if (path === '/') {
      return currentPath === '/';
    }
    return currentPath === path || currentPath.startsWith(path + '/');
  };

  onMount(() => {
    auth.init();
  });
</script>

<div class="app">
  <Toast />

  <header>
    <nav>
      <div class="nav-container">
        <div class="brand-section">
          <h1><a href="/">🏛️ Archiving System</a></h1>
        </div>

        <ul class="nav-links">

          {#if $auth.role === 'ADMIN'}
            <li>
              <a href="/tenants" class="tenants-link" class:active={isActive('/tenants')}>
                🏢 Tenants
              </a>
            </li>
          {/if}

          {#if $auth.role === 'ADMIN' || $auth.role === 'TENANT'}
            <li>
              <a href="/users" class="users-link" class:active={isActive('/users')}>
                👥 Users
              </a>
            </li>
            <li>
              <a href="/archives" class="archives-link" class:active={isActive('/archives')}>
                📁 Archives
              </a>
            </li>
            <li>
              <a href="/sip" class="sips-link" class:active={isActive('/sip')}>
                📦 SIPs
              </a>
            </li>
            <li>
              <a href="/aip" class="aips-link" class:active={isActive('/aip')}>
                🏗️ AIPs
              </a>
            </li>
            <li>
              <a href="/dip" class="dips-link" class:active={isActive('/dip')}>
                📤 DIPs
              </a>
            </li>
          {/if}

          {#if $auth.isLoggedIn}
            <li>
              <a href="/documents" class="documents-link" class:active={isActive('/documents')}>
                📄 Documents
              </a>
            </li>
          {/if}

        </ul>
        <div class="auth-section">
          {#if $auth.isLoggedIn && $auth.user}
            <span class="user-name-display">👤 {$auth.user.name}</span>
            <button class="logout-button" on:click={auth.logout}>
              🚪 Logout
            </button>
          {:else}
            <a href="/login" class="login-button">
              🔐 Login
            </a>
          {/if}
        </div>
      </div>
    </nav>
  </header>

  <main>
    <slot />
  </main>

  <footer>
    <p>&copy; 2025 Archiving System</p>
  </footer>
</div>

<style>
  .app {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  header {
    background: #1e293b;
    color: white;
    padding: 1rem 0;
  }

  .nav-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 1rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 2rem;
  }

  .brand-section {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  h1 {
    margin: 0;
  }

  h1 a {
    color: white;
    text-decoration: none;
    font-size: 1.5rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }


  .nav-links {
    display: flex;
    list-style: none;
    gap: 0.75rem;
    margin: 0;
    padding: 0;
    align-items: center;
  }

  .nav-links li {
    display: flex;
  }

  .nav-links a {
    color: white;
    text-decoration: none;
    padding: 0.625rem 1.25rem;
    border-radius: 0.5rem;
    transition: all 0.2s ease;
    font-weight: 600;
    font-size: 0.9rem;
    white-space: nowrap;
    border: 2px solid transparent;
  }

  .nav-links a:hover:not(.active) {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  }

  .nav-links a.active {
    border-color: rgba(255, 255, 255, 0.6);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
    transform: none;
  }


  .nav-links a.archives-link {
    background: linear-gradient(135deg, #06b6d4, #0891b2);
  }

  .nav-links a.archives-link:hover:not(.active) {
    background: linear-gradient(135deg, #0891b2, #0e7490);
  }

  .nav-links a.archives-link.active {
    background: linear-gradient(135deg, #22d3ee, #06b6d4);
  }

  .nav-links a.tenants-link {
    background: linear-gradient(135deg, #10b981, #059669);
  }

  .nav-links a.tenants-link:hover:not(.active) {
    background: linear-gradient(135deg, #059669, #047857);
  }

  .nav-links a.tenants-link.active {
    background: linear-gradient(135deg, #34d399, #10b981);
  }

  .nav-links a.users-link {
    background: linear-gradient(135deg, #f59e0b, #d97706);
  }

  .nav-links a.users-link:hover:not(.active) {
    background: linear-gradient(135deg, #d97706, #b45309);
  }

  .nav-links a.users-link.active {
    background: linear-gradient(135deg, #fbbf24, #f59e0b);
  }

  .nav-links a.sips-link {
    background: linear-gradient(135deg, #ec4899, #db2777);
  }

  .nav-links a.sips-link:hover:not(.active) {
    background: linear-gradient(135deg, #db2777, #be185d);
  }

  .nav-links a.sips-link.active {
    background: linear-gradient(135deg, #f472b6, #ec4899);
  }

  .nav-links a.aips-link {
    background: linear-gradient(135deg, #6366f1, #4f46e5);
  }

  .nav-links a.aips-link:hover:not(.active) {
    background: linear-gradient(135deg, #4f46e5, #4338ca);
  }

  .nav-links a.aips-link.active {
    background: linear-gradient(135deg, #818cf8, #6366f1);
  }

  .nav-links a.dips-link {
    background: linear-gradient(135deg, #f97316, #ea580c);
  }

  .nav-links a.dips-link:hover:not(.active) {
    background: linear-gradient(135deg, #ea580c, #c2410c);
  }

  .nav-links a.dips-link.active {
    background: linear-gradient(135deg, #fb923c, #f97316);
  }

  .nav-links a.documents-link {
    background: linear-gradient(135deg, #8b5cf6, #7c3aed);
  }

  .nav-links a.documents-link:hover:not(.active) {
    background: linear-gradient(135deg, #7c3aed, #6d28d9);
  }

  .nav-links a.documents-link.active {
    background: linear-gradient(135deg, #a78bfa, #8b5cf6);
  }

  .auth-section {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-left: auto;
  }

  .user-name-display {
    color: white;
    font-weight: 500;
    font-size: 0.9rem;
    white-space: nowrap;
  }

  .login-button,
  .logout-button {
    padding: 0.625rem 1.25rem;
    background: rgba(255, 255, 255, 0.15);
    color: white;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 0.9rem;
    display: inline-block;
    white-space: nowrap;
  }

  .login-button:hover,
  .logout-button:hover {
    background: rgba(255, 255, 255, 0.25);
    border-color: rgba(255, 255, 255, 0.5);
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  }

  .logout-button {
    background: rgba(239, 68, 68, 0.25);
    border-color: rgba(239, 68, 68, 0.5);
  }

  .logout-button:hover {
    background: rgba(239, 68, 68, 0.35);
    border-color: rgba(239, 68, 68, 0.7);
  }

  main {
    flex: 1;
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem 1rem;
    width: 100%;
  }

  footer {
    background: #f8fafc;
    text-align: center;
    padding: 1rem;
    color: #64748b;
  }
</style>
