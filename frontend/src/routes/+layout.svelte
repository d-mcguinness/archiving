<script lang="ts">
  import '../app.css';
  import Toast from '$lib/components/Toast.svelte';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';

  // Reactive declaration ensures this updates whenever the route changes
  $: currentPath = $page.url.pathname;

  // Check if a path is active based on current route
  $: isActive = (path: string): boolean => {
    if (path === '/') {
      return currentPath === '/';
    }
    return currentPath === path || currentPath.startsWith(path + '/');
  };

  // Auth state
  let isLoggedIn = false;
  let currentUser: any = null;
  let currentRole = '';
  let currentTenantId: number | null = null;
  let isBrowser = false;

  // Check if we're in browser (not SSR)
  $: isBrowser = typeof window !== 'undefined';

  // Reactive statement to check auth on route changes (only in browser)
  $: if (isBrowser) {
    // Re-check auth status whenever the page changes
    $page;
    checkAuthStatus();
  }

  onMount(() => {
    checkAuthStatus();

    // Listen for storage changes (e.g., login in another tab)
    window.addEventListener('storage', checkAuthStatus);

    return () => {
      window.removeEventListener('storage', checkAuthStatus);
    };
  });

  function checkAuthStatus() {
    // Only run in browser
    if (typeof window === 'undefined' || typeof localStorage === 'undefined') {
      return;
    }

    try {
      const token = localStorage.getItem('auth_token');
      const user = localStorage.getItem('auth_user');
      const role = localStorage.getItem('auth_role');
      const tenantId = localStorage.getItem('auth_tenantId');

      if (token && user) {
        isLoggedIn = true;
        try {
          currentUser = JSON.parse(user);
        } catch (e) {
          console.error('Error parsing user data:', e);
          currentUser = null;
          isLoggedIn = false;
        }
        currentRole = role || '';
        currentTenantId = tenantId ? parseInt(tenantId, 10) : null;
      } else {
        isLoggedIn = false;
        currentUser = null;
        currentRole = '';
        currentTenantId = null;
      }
    } catch (e) {
      console.error('Error checking auth status:', e);
      isLoggedIn = false;
      currentUser = null;
      currentRole = '';
      currentTenantId = null;
    }
  }

  function handleLogout() {
    // Only run in browser
    if (typeof window === 'undefined' || typeof localStorage === 'undefined') {
      return;
    }

    try {
      // Clear auth data
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_user');
      localStorage.removeItem('auth_role');
      localStorage.removeItem('auth_tenantId');

      // Update state immediately
      isLoggedIn = false;
      currentUser = null;
      currentRole = '';
      currentTenantId = null;

      // Redirect to login
      goto('/login');
    } catch (e) {
      console.error('Error during logout:', e);
    }
  }
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

          <!-- Tenants - ADMIN shows all tenants list, TENANT shows their tenant page -->
          {#if currentRole === 'ADMIN'}
            <li>
              <a
                href="/admin/tenants"
                class="tenants-link"
                class:active={isActive('/admin/tenants')}
              >
                🏢 Tenants
              </a>
            </li>
          {:else if currentRole === 'TENANT' && currentTenantId}
            <li>
              <a
                href="/tenants/{currentTenantId}"
                class="tenants-link"
                class:active={isActive('/tenants/' + currentTenantId)}
              >
                🏢 My Tenant
              </a>
            </li>
          {/if}

          <!-- Users - ADMIN shows all users, TENANT shows their tenant's users -->
          {#if currentRole === 'ADMIN'}
            <li>
              <a
                href="/admin/users"
                class="users-link"
                class:active={isActive('/admin/users')}
              >
                👥 Users
              </a>
            </li>
          {:else if currentRole === 'TENANT' && currentTenantId}
            <li>
              <a
                href="/tenants/{currentTenantId}/users"
                class="users-link"
                class:active={isActive('/tenants/' + currentTenantId + '/users')}
              >
                👥 Users
              </a>
            </li>
          {/if}

          <!-- Archives - ADMIN shows all archives, TENANT shows their tenant's archives -->
          {#if currentRole === 'ADMIN'}
            <li>
              <a
                href="/admin/archives"
                class="archives-link"
                class:active={isActive('/admin/archives')}
              >
                📁 Archives
              </a>
            </li>
          {:else if currentRole === 'TENANT' && currentTenantId}
            <li>
              <a
                href="/tenants/{currentTenantId}/archives"
                class="archives-link"
                class:active={isActive('/tenants/' + currentTenantId + '/archives')}
              >
                📁 Archives
              </a>
            </li>
          {/if}

          <!-- Documents - role-based navigation -->
          {#if isLoggedIn}
            <li>
              {#if currentRole === 'ADMIN'}
                <a
                  href="/admin/documents"
                  class="documents-link"
                  class:active={isActive('/admin/documents')}
                >
                  📄 Documents
                </a>
              {:else if currentRole === 'USER' && currentTenantId && currentUser?.id}
                <a
                  href="/tenants/{currentTenantId}/users/{currentUser.id}/documents"
                  class="documents-link"
                  class:active={isActive('/tenants/' + currentTenantId + '/users/' + currentUser.id + '/documents')}
                >
                  📄 Documents
                </a>
              {:else if currentRole === 'TENANT' && currentTenantId}
                <a
                  href="/tenants/{currentTenantId}/documents"
                  class="documents-link"
                  class:active={isActive('/tenants/' + currentTenantId + '/documents')}
                >
                  📄 Documents
                </a>
              {:else}
                <a
                  href="/documents"
                  class="documents-link"
                  class:active={isActive('/documents')}
                >
                  📄 Documents
                </a>
              {/if}
            </li>
          {/if}
        </ul>
        <div class="auth-section">
          {#if isLoggedIn && currentUser}
            <span class="user-name-display">👤 {currentUser.name}</span>
            <button class="logout-button" on:click={handleLogout}>
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
