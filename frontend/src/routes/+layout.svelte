<script lang="ts">
  import '../app.css';
  import Toast from '$lib/components/Toast.svelte';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { afterNavigate } from '$app/navigation';
  import { auth } from '$lib/stores/authStore';
  import { theme } from '$lib/stores/themeStore';

  export let data: any;

  // Reactive declaration ensures this updates whenever the route changes
  $: currentPath = $page.url.pathname;

  // Mobile nav state — the links/auth collapse behind a hamburger on small screens.
  let mobileOpen = false;
  const toggleMenu = () => (mobileOpen = !mobileOpen);
  const closeMenu = () => (mobileOpen = false);

  // Close the menu after any client-side navigation (e.g. tapping a link).
  afterNavigate(() => {
    mobileOpen = false;
  });

  function handleLogout() {
    mobileOpen = false;
    auth.logout();
  }

  function handleExitMimic() {
    mobileOpen = false;
    auth.exitMimic();
  }

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

<svelte:window on:keydown={(e) => e.key === 'Escape' && closeMenu()} />

<div class="app">
  <Toast />

  <header>
    <nav>
      <div class="nav-container">
        <div class="brand-section">
          <h1><a href="/">Arcana</a></h1>
        </div>

        <button
          class="nav-toggle"
          class:open={mobileOpen}
          aria-label="Toggle navigation menu"
          aria-expanded={mobileOpen}
          aria-controls="nav-menu"
          on:click={toggleMenu}
        >
          <span class="nav-toggle-bar"></span>
          <span class="nav-toggle-bar"></span>
          <span class="nav-toggle-bar"></span>
        </button>

        <div class="nav-collapse" class:open={mobileOpen} id="nav-menu">
        {#if !$auth.isLoggedIn}
          <ul class="nav-links">
            <li>
              <a href="/product" class="product-link" class:active={isActive('/product')}>
                Product
              </a>
            </li>
          </ul>
        {:else}
          <ul class="nav-links">

            {#if $auth.role === 'ADMIN'}
              <li>
                <a href="/admin" class="tenants-link" class:active={isActive('/admin') && !isActive('/admin/')}>
                  Dashboard
                </a>
              </li>
              <li>
                <a href="/admin/tenants" class="tenants-link" class:active={isActive('/admin/tenants') || isActive('/tenants/')}>
                  🏢 Tenants
                </a>
              </li>
              <li>
                <a href="/admin/archives" class="archives-link" class:active={isActive('/admin/archives')}>
                  📁 Archives
                </a>
              </li>
              <li>
                <a href="/admin/intake" class="intakes-link" class:active={isActive('/admin/intake')}>
                  📦 Intakes
                </a>
              </li>
              <li>
                <a href="/admin/preservation" class="preservations-link" class:active={isActive('/admin/preservation')}>
                  🏗️ Preservations
                </a>
              </li>
              <li>
                <a href="/admin/release" class="releases-link" class:active={isActive('/admin/release')}>
                  📤 Releases
                </a>
              </li>
              <li>
                <a href="/admin/users" class="users-link" class:active={isActive('/admin/users')}>
                  👥 Users
                </a>
              </li>
              <li>
                <a href="/admin/documents" class="documents-link" class:active={isActive('/admin/documents')}>
                  📄 Documents
                </a>
              </li>

            {:else if $auth.role === 'TENANT'}
              <li>
                <a href="/tenants/{$auth.tenantId}/archives" class="archives-link" class:active={isActive(`/tenants/${$auth.tenantId}/archives`)}>
                  📁 Archives
                </a>
              </li>
              <li>
                <a href="/tenants/{$auth.tenantId}/intakes" class="intakes-link" class:active={isActive(`/tenants/${$auth.tenantId}/intakes`)}>
                  📦 Intakes
                </a>
              </li>
              <li>
                <a href="/tenants/{$auth.tenantId}/preservations" class="preservations-link" class:active={isActive(`/tenants/${$auth.tenantId}/preservations`)}>
                  🏗️ Preservations
                </a>
              </li>
              <li>
                <a href="/tenants/{$auth.tenantId}/releases" class="releases-link" class:active={isActive(`/tenants/${$auth.tenantId}/releases`)}>
                  📤 Releases
                </a>
              </li>
              <li>
                <a href="/tenants/{$auth.tenantId}/users" class="users-link" class:active={isActive(`/tenants/${$auth.tenantId}/users`)}>
                  👥 Users
                </a>
              </li>
              <li>
                <a href="/tenants/{$auth.tenantId}/documents" class="documents-link" class:active={isActive(`/tenants/${$auth.tenantId}/documents`)}>
                  📄 Documents
                </a>
              </li>

            {:else if $auth.role === 'USER'}
              <!-- USER: Documents only -->
              <li>
                <a
                  href={$auth.tenantId && $auth.user?.id ? `/tenants/${$auth.tenantId}/users/${$auth.user.id}/edit` : '/'}
                  class="users-link"
                  class:active={isActive(`/tenants/${$auth.tenantId}/users/${$auth.user?.id}`) && !isActive(`/tenants/${$auth.tenantId}/users/${$auth.user?.id}/documents`)}
                >
                  ✏️ Profile
                </a>
              </li>
              <li>
                <a
                  href={$auth.tenantId && $auth.user?.id ? `/tenants/${$auth.tenantId}/users/${$auth.user.id}/documents` : '/documents'}
                  class="documents-link"
                  class:active={isActive(`/tenants/${$auth.tenantId}/users/${$auth.user?.id}/documents`)}
                >
                  📄 Documents
                </a>
              </li>
            {/if}

          </ul>
        {/if}

        <div class="auth-section">
          <button
            class="theme-toggle"
            on:click={() => theme.toggle()}
            aria-label="Switch to {$theme === 'dark' ? 'light' : 'dark'} theme"
            title="Switch to {$theme === 'dark' ? 'light' : 'dark'} theme"
          >
            {#if $theme === 'dark'}
              <!-- sun -->
              <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
                <circle cx="12" cy="12" r="4.5" />
                <path d="M12 2.5v2.4M12 19.1v2.4M2.5 12h2.4M19.1 12h2.4M5.3 5.3l1.7 1.7M17 17l1.7 1.7M18.7 5.3L17 7M7 17l-1.7 1.7" />
              </svg>
            {:else}
              <!-- moon -->
              <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M20.5 14.5A8.5 8.5 0 0 1 9.5 3.5a8.5 8.5 0 1 0 11 11z" />
              </svg>
            {/if}
          </button>
          {#if $auth.isLoggedIn}
            <span class="user-name-display">👤 {$auth.user?.name}</span>
            {#if auth.isMimicking()}
              <button class="exit-mimic-button" on:click={handleExitMimic}>
                🎭 Exit Mimic
              </button>
            {/if}
            <button class="logout-button" on:click={handleLogout}>
              Logout
            </button>
          {:else}
            <a href="/login" class="login-button">
              Sign In
            </a>
            <a href="/register" class="register-button">
              Register
            </a>
          {/if}
        </div>
        </div>
      </div>
    </nav>
  </header>

  {#if mobileOpen}
    <button class="nav-backdrop" aria-label="Close navigation menu" on:click={closeMenu}></button>
  {/if}

  <main>
    <slot />
  </main>

  <footer>
    <p>&copy; 2026 Arcana</p>
  </footer>
</div>

<style>
  .app {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  header {
    background: var(--arc-nav-bg);
    color: white;
    padding: 1rem 0;
    position: relative;
    z-index: 50;
    border-bottom: 1px solid rgba(148, 163, 184, 0.14);
    transition: background-color 0.25s ease;
  }

  .nav-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0 1rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 2rem;
    position: relative;
    flex-wrap: wrap; /* heavy authenticated navs wrap instead of overflowing */
  }

  /* Hamburger toggle — hidden on desktop, shown below the breakpoint. */
  .nav-toggle {
    display: none;
    flex-direction: column;
    justify-content: center;
    gap: 5px;
    width: 44px;
    height: 44px;
    padding: 0;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.2);
    border-radius: 0.5rem;
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease;
  }

  .nav-toggle:hover {
    background: rgba(255, 255, 255, 0.15);
    border-color: rgba(255, 255, 255, 0.4);
  }

  .nav-toggle-bar {
    display: block;
    width: 22px;
    height: 2px;
    margin: 0 auto;
    background: white;
    border-radius: 2px;
    transition: transform 0.2s ease, opacity 0.2s ease;
  }

  .nav-toggle.open .nav-toggle-bar:nth-child(1) {
    transform: translateY(7px) rotate(45deg);
  }
  .nav-toggle.open .nav-toggle-bar:nth-child(2) {
    opacity: 0;
  }
  .nav-toggle.open .nav-toggle-bar:nth-child(3) {
    transform: translateY(-7px) rotate(-45deg);
  }

  /* On desktop the wrapper is transparent to the flex layout. */
  .nav-collapse {
    display: contents;
  }

  .nav-backdrop {
    display: none;
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
    color: #f8fafc;
    text-decoration: none;
    font-size: 1.5rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    font-weight: 700;
    letter-spacing: -0.02em;
  }

  /* Brand gradient dot beside the wordmark — echoes the indigo→violet→cyan accent. */
  h1 a::before {
    content: '';
    width: 0.6rem;
    height: 0.6rem;
    border-radius: 50%;
    background: linear-gradient(135deg, #6366f1, #8b5cf6 55%, #22d3ee);
    box-shadow: 0 0 12px rgba(139, 92, 246, 0.8);
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

  /* Unified brand pills — quiet ghost links, indigo→violet gradient when active. */
  .nav-links a {
    color: #e2e8f0;
    text-decoration: none;
    padding: 0.6rem 1.1rem;
    border-radius: 0.6rem;
    transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease,
      transform 0.18s ease, box-shadow 0.18s ease;
    font-weight: 600;
    font-size: 0.9rem;
    white-space: nowrap;
    border: 1px solid transparent;
    background: transparent;
  }

  .nav-links a:hover:not(.active) {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(148, 163, 184, 0.35);
    color: white;
    transform: translateY(-2px);
  }

  .nav-links a.active {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    border-color: transparent;
    box-shadow: 0 8px 22px -8px rgba(124, 58, 237, 0.7);
    transform: none;
  }

  /* Single public "Product" entry point — echoes the brand indigo→violet→cyan gradient. */
  .nav-links a.product-link {
    border-color: rgba(148, 163, 184, 0.4);
  }

  .nav-links a.product-link.active {
    background: linear-gradient(135deg, #6366f1, #8b5cf6 55%, #06b6d4);
    border-color: transparent;
  }

  .auth-section {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-left: auto;
  }

  /* Sun/moon theme switch — quiet ghost circle, matches the nav pills. */
  .theme-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    background: rgba(255, 255, 255, 0.06);
    color: #e2e8f0;
    border: 1px solid rgba(148, 163, 184, 0.4);
    border-radius: 50%;
    cursor: pointer;
    transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
    box-shadow: none;
    flex-shrink: 0;
  }

  .theme-toggle:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(226, 232, 240, 0.7);
    color: white;
    transform: translateY(-2px);
    box-shadow: none;
  }

  .user-name-display {
    color: white;
    font-weight: 500;
    font-size: 0.9rem;
    white-space: nowrap;
  }

  .login-button,
  .logout-button {
    padding: 0.6rem 1.25rem;
    background: rgba(255, 255, 255, 0.06);
    color: #e2e8f0;
    border: 1px solid rgba(148, 163, 184, 0.4);
    border-radius: 0.6rem;
    text-decoration: none;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 0.9rem;
    display: inline-block;
    white-space: nowrap;
    box-shadow: none;
    font-family: inherit;
  }

  .login-button:hover,
  .logout-button:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(226, 232, 240, 0.7);
    color: white;
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  }

  .register-button {
    padding: 0.6rem 1.25rem;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    border: 1px solid transparent;
    border-radius: 0.6rem;
    text-decoration: none;
    font-weight: 700;
    transition: all 0.2s ease;
    font-size: 0.9rem;
    display: inline-block;
    white-space: nowrap;
    box-shadow: 0 8px 22px -8px rgba(124, 58, 237, 0.7);
  }

  .register-button:hover {
    background: linear-gradient(135deg, #4f46e5, #7c3aed);
    transform: translateY(-2px);
    box-shadow: 0 12px 28px -8px rgba(124, 58, 237, 0.8);
  }

  .logout-button {
    background: rgba(239, 68, 68, 0.18);
    border-color: rgba(239, 68, 68, 0.45);
  }

  .logout-button:hover {
    background: rgba(239, 68, 68, 0.3);
    border-color: rgba(239, 68, 68, 0.7);
  }

  .exit-mimic-button {
    padding: 0.6rem 1.25rem;
    background: rgba(139, 92, 246, 0.3);
    color: white;
    border: 1px solid rgba(139, 92, 246, 0.6);
    border-radius: 0.6rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    font-size: 0.9rem;
    white-space: nowrap;
    box-shadow: none;
    font-family: inherit;
  }

  .exit-mimic-button:hover {
    background: rgba(139, 92, 246, 0.5);
    border-color: rgba(139, 92, 246, 0.8);
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  }

  main {
    flex: 1;
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem 1rem;
    width: 100%;
  }

  footer {
    background: var(--arc-ground);
    border-top: 1px solid var(--arc-line);
    text-align: center;
    padding: 1rem;
    color: var(--arc-muted);
    font-size: 0.9rem;
    transition: background-color 0.25s ease, border-color 0.25s ease;
  }

  /* ── Responsive nav: collapse links + auth behind the hamburger ── */
  @media (max-width: 900px) {
    .nav-container {
      flex-wrap: nowrap;
    }

    .nav-toggle {
      display: flex;
    }

    .nav-collapse {
      display: none;
      position: absolute;
      top: calc(100% + 1rem);
      left: 0;
      right: 0;
      flex-direction: column;
      align-items: stretch;
      gap: 1rem;
      background: var(--arc-nav-bg);
      border-top: 1px solid rgba(255, 255, 255, 0.12);
      padding: 1.25rem 1rem;
      box-shadow: 0 20px 36px -14px rgba(0, 0, 0, 0.65);
      z-index: 60;
      max-height: calc(100vh - 5rem);
      overflow-y: auto;
    }

    .nav-collapse.open {
      display: flex;
    }

    .nav-links {
      flex-direction: column;
      align-items: stretch;
      gap: 0.5rem;
      width: 100%;
    }

    .nav-links a {
      display: block;
      text-align: center;
    }

    .auth-section {
      flex-direction: column;
      align-items: stretch;
      gap: 0.6rem;
      width: 100%;
      margin-left: 0;
      padding-top: 1rem;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
    }

    .user-name-display {
      text-align: center;
    }

    .login-button,
    .register-button,
    .logout-button,
    .exit-mimic-button {
      width: 100%;
      text-align: center;
    }

    .theme-toggle {
      width: 100%;
      border-radius: 0.6rem;
    }

    .nav-backdrop {
      display: block;
      position: fixed;
      inset: 0;
      width: 100%;
      height: 100%;
      background: rgba(2, 6, 23, 0.5);
      border: none;
      padding: 0;
      margin: 0;
      cursor: default;
      z-index: 40;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .nav-toggle-bar {
      transition: none;
    }
  }
</style>
