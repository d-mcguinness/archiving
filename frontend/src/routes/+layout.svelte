<script lang="ts">
  import '../app.css';
  import Toast from '$lib/components/Toast.svelte';
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { afterNavigate } from '$app/navigation';
  import { auth } from '$lib/stores/authStore';
  import { theme } from '$lib/stores/themeStore';
  import { navFor, isNavActive, roleHome, roleLabel } from '$lib/roles';

  export let data: any;

  // Reactive declaration ensures this updates whenever the route changes
  $: currentPath = $page.url.pathname;

  // Primary navigation is derived from the signed-in role (see $lib/roles).
  $: navGroups = navFor($auth);
  $: homeHref = $auth.isLoggedIn ? roleHome($auth) : '/';

  // Mimic state lives in localStorage; re-read it whenever the session changes.
  $: mimicking = $auth.isLoggedIn && auth.isMimicking();
  $: mimicOrigin = mimicking ? originalUserName() : '';

  function originalUserName(): string {
    try {
      return JSON.parse(localStorage.getItem('auth_original_user') || '{}')?.name || '';
    } catch {
      return '';
    }
  }

  $: initials = ($auth.user?.name || '?')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part: string) => part[0]?.toUpperCase())
    .join('');

  $: profileHref =
    $auth.tenantId && $auth.user?.id
      ? `/tenants/${$auth.tenantId}/users/${$auth.user.id}`
      : '';

  // Mobile nav state — the links/auth collapse behind a hamburger on small screens.
  let mobileOpen = false;
  const toggleMenu = () => (mobileOpen = !mobileOpen);
  const closeMenu = () => (mobileOpen = false);

  // Account dropdown (desktop). On mobile its contents render inline in the panel.
  let accountOpen = false;
  let accountEl: HTMLElement;
  let accountTrigger: HTMLButtonElement;

  function handleWindowClick(event: MouseEvent) {
    if (accountOpen && accountEl && !accountEl.contains(event.target as Node)) {
      accountOpen = false;
    }
  }

  function handleKeydown(event: KeyboardEvent) {
    if (event.key !== 'Escape') return;
    if (accountOpen) {
      accountOpen = false;
      accountTrigger?.focus();
    }
    closeMenu();
  }

  // Close the menus after any client-side navigation (e.g. tapping a link).
  afterNavigate(() => {
    mobileOpen = false;
    accountOpen = false;
  });

  function handleLogout() {
    mobileOpen = false;
    accountOpen = false;
    auth.logout();
  }

  function handleExitMimic() {
    mobileOpen = false;
    accountOpen = false;
    auth.exitMimic();
  }

  onMount(() => {
    auth.init();
  });
</script>

<svelte:window on:keydown={handleKeydown} on:click={handleWindowClick} />

<div class="app">
  <a class="skip-link" href="#main-content">Skip to main content</a>
  <Toast />

  <header>
    <nav aria-label="Primary">
      <div class="nav-container">
        <div class="brand-section">
          <a class="brand" href={homeHref}>Arcana</a>
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
              <a
                href="/product"
                class="product-link"
                class:active={currentPath === '/product'}
                aria-current={currentPath === '/product' ? 'page' : undefined}
              >
                Product
              </a>
            </li>
          </ul>
        {:else}
          <ul class="nav-links">
            {#each navGroups as group, groupIndex}
              {#each group as item, itemIndex}
                {@const active = isNavActive(item, currentPath)}
                <li class:group-start={groupIndex > 0 && itemIndex === 0}>
                  <a href={item.href} class:active aria-current={active ? 'page' : undefined}>
                    {item.label}
                  </a>
                </li>
              {/each}
            {/each}
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
            <div class="account" bind:this={accountEl}>
              <button
                class="account-trigger"
                bind:this={accountTrigger}
                aria-haspopup="true"
                aria-expanded={accountOpen}
                aria-controls="account-menu"
                on:click={() => (accountOpen = !accountOpen)}
              >
                <span class="avatar" aria-hidden="true">{initials}</span>
                <span class="account-id">
                  <span class="account-name">{$auth.user?.name}</span>
                  <span class="account-role">{roleLabel($auth.role)}</span>
                </span>
                <svg class="caret" viewBox="0 0 12 12" width="12" height="12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M2.5 4.5 6 8l3.5-3.5" />
                </svg>
              </button>

              <div class="account-menu" class:open={accountOpen} id="account-menu">
                <div class="account-summary">
                  <span class="summary-name">{$auth.user?.name}</span>
                  {#if $auth.user?.email}
                    <span class="summary-email">{$auth.user.email}</span>
                  {/if}
                  <span class="role-chip role-{($auth.role || '').toLowerCase()}">{roleLabel($auth.role)}</span>
                </div>
                {#if profileHref && $auth.role !== 'ADMIN'}
                  <a class="account-item" href={profileHref}>My profile</a>
                {/if}
                {#if mimicking}
                  <button class="account-item" on:click={handleExitMimic}>Stop viewing as this user</button>
                {/if}
                <button class="account-item" on:click={handleLogout}>Sign out</button>
              </div>
            </div>
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
    {#if mimicking}
      <div class="mimic-banner" role="status">
        <div class="mimic-inner">
          <p>
            <strong>Viewing as {$auth.user?.name}</strong>
            <span class="mimic-detail">
              {roleLabel($auth.role)}. Anything you do here is done as this account{mimicOrigin ? `, not as ${mimicOrigin}` : ''}.
            </span>
          </p>
          <button class="mimic-exit" on:click={handleExitMimic}>Stop viewing as {$auth.user?.name}</button>
        </div>
      </div>
    {/if}
  </header>

  {#if mobileOpen}
    <button class="nav-backdrop" aria-label="Close navigation menu" on:click={closeMenu}></button>
  {/if}

  <main id="main-content" tabindex="-1">
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
    padding: 0.75rem 0;
    position: sticky;
    top: 0;
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
    gap: 1.25rem;
    position: relative;
    flex-wrap: wrap; /* fallback: wrap rather than overflow on very long navs */
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

  .brand {
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
  .brand::before {
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
    gap: 0.15rem;
    margin: 0;
    padding: 0;
    align-items: center;
  }

  /* Divider between navigation groups (e.g. people | archive pipeline). */
  .nav-links li.group-start {
    margin-left: 0.55rem;
    padding-left: 0.7rem;
    border-left: 1px solid rgba(148, 163, 184, 0.3);
  }

  .nav-links li {
    display: flex;
  }

  /* Unified brand pills — quiet ghost links, indigo→violet gradient when active. */
  .nav-links a {
    color: #e2e8f0;
    text-decoration: none;
    padding: 0.5rem 0.75rem;
    border-radius: 0.6rem;
    transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease,
      box-shadow 0.18s ease;
    font-weight: 600;
    font-size: 0.9rem;
    white-space: nowrap;
    border: 1px solid transparent;
    background: transparent;
  }

  .nav-links a:hover:not(.active) {
    background: rgba(255, 255, 255, 0.08);
    color: white;
  }

  .nav-links a.active {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    border-color: transparent;
    box-shadow: 0 8px 22px -8px rgba(124, 58, 237, 0.7);
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
    gap: 0.75rem;
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

  .login-button {
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

  .login-button:hover {
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

  /* ── Account menu: who is signed in, at what level, and how to leave ── */
  .account {
    position: relative;
  }

  .account-trigger {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.3rem 0.6rem 0.3rem 0.3rem;
    background: rgba(255, 255, 255, 0.06);
    color: #e2e8f0;
    border: 1px solid rgba(148, 163, 184, 0.4);
    border-radius: 999px;
    font-family: inherit;
    cursor: pointer;
    box-shadow: none;
    transition: background 0.18s ease, border-color 0.18s ease;
  }

  .account-trigger:hover,
  .account-trigger[aria-expanded='true'] {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(226, 232, 240, 0.7);
    transform: none;
    box-shadow: none;
  }

  .avatar {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    font-size: 0.75rem;
    font-weight: 700;
    letter-spacing: 0.02em;
    flex-shrink: 0;
  }

  .account-id {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    line-height: 1.2;
    text-align: left;
  }

  .account-name {
    font-size: 0.85rem;
    font-weight: 600;
    color: #f8fafc;
    white-space: nowrap;
    max-width: 11rem;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .account-role {
    font-size: 0.7rem;
    font-weight: 500;
    color: #a5b4fc;
    white-space: nowrap;
  }

  .caret {
    color: #94a3b8;
    transition: transform 0.18s ease;
  }

  .account-trigger[aria-expanded='true'] .caret {
    transform: rotate(180deg);
  }

  .account-menu {
    display: none;
    position: absolute;
    top: calc(100% + 0.5rem);
    right: 0;
    min-width: 15rem;
    padding: 0.4rem;
    background: var(--arc-card);
    border: 1px solid var(--arc-line-strong);
    border-radius: 0.8rem;
    box-shadow: var(--arc-shadow-lift);
    z-index: 70;
  }

  .account-menu.open {
    display: block;
  }

  .account-summary {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.15rem;
    padding: 0.6rem 0.7rem 0.75rem;
    margin-bottom: 0.3rem;
    border-bottom: 1px solid var(--arc-line);
  }

  .summary-name {
    font-weight: 700;
    color: var(--arc-ink);
    font-size: 0.92rem;
  }

  .summary-email {
    color: var(--arc-muted);
    font-size: 0.8rem;
    word-break: break-all;
  }

  .role-chip {
    margin-top: 0.4rem;
    padding: 0.15rem 0.6rem;
    border-radius: 999px;
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.03em;
    background: var(--arc-chip-slate-bg);
    color: var(--arc-chip-slate-ink);
  }

  .role-chip.role-admin { background: var(--arc-chip-violet-bg); color: var(--arc-chip-violet-ink); }
  .role-chip.role-tenant { background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); }
  .role-chip.role-user { background: var(--arc-chip-cyan-bg); color: var(--arc-chip-cyan-ink); }

  .account-item {
    display: block;
    width: 100%;
    padding: 0.55rem 0.7rem;
    background: transparent;
    color: var(--arc-body);
    border: none;
    border-radius: 0.5rem;
    font-family: inherit;
    font-size: 0.88rem;
    font-weight: 600;
    text-align: left;
    text-decoration: none;
    cursor: pointer;
    box-shadow: none;
    transition: background 0.15s ease, color 0.15s ease;
  }

  .account-item:hover {
    background: var(--arc-card-2);
    color: var(--arc-ink);
    transform: none;
    box-shadow: none;
  }

  /* ── Mimic banner: impossible to miss while acting as someone else ── */
  .mimic-banner {
    margin: 0.75rem 0 -0.75rem;
    background: var(--arc-alert-amber-bg);
    border-bottom: 1px solid var(--arc-alert-amber-border);
    color: var(--arc-alert-amber-ink);
  }

  .mimic-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 0.55rem 1rem;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
  }

  .mimic-inner p {
    margin: 0;
    font-size: 0.88rem;
    line-height: 1.4;
  }

  .mimic-detail {
    margin-left: 0.4rem;
  }

  .mimic-exit {
    padding: 0.4rem 0.9rem;
    background: transparent;
    color: inherit;
    border: 1.5px solid currentColor;
    border-radius: 0.55rem;
    font-size: 0.85rem;
    font-weight: 700;
    white-space: nowrap;
    box-shadow: none;
  }

  .mimic-exit:hover {
    background: var(--arc-alert-amber-border);
    transform: none;
    box-shadow: none;
  }

  /* Keyboard users: jump past the navigation. Visible only when focused. */
  .skip-link {
    position: absolute;
    left: 1rem;
    top: -4rem;
    z-index: 200;
    padding: 0.6rem 1rem;
    background: var(--arc-card);
    color: var(--arc-link);
    border: 2px solid var(--arc-indigo);
    border-radius: 0.6rem;
    font-weight: 700;
    text-decoration: none;
  }

  .skip-link:focus {
    top: 0.75rem;
  }

  /* The nav is dark in both themes, so its focus ring needs a light hue. */
  header :global(:focus-visible) {
    outline-color: #c7d2fe;
  }

  header .account-menu :global(:focus-visible) {
    outline-color: var(--arc-indigo);
  }

  main:focus {
    outline: none;
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
  @media (max-width: 1040px) {
    .nav-container {
      flex-wrap: nowrap;
    }

    .nav-toggle {
      display: flex;
    }

    .nav-collapse {
      display: none;
      position: absolute;
      top: calc(100% + 0.75rem);
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
      width: 100%;
      padding: 0.7rem 0.9rem;
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

    .nav-links li.group-start {
      margin: 0.35rem 0 0;
      padding: 0.6rem 0 0;
      border-left: none;
      border-top: 1px solid rgba(255, 255, 255, 0.12);
    }

    .login-button,
    .register-button {
      width: 100%;
      text-align: center;
    }

    /* The account dropdown becomes a plain inline block inside the panel. */
    .account-trigger {
      width: 100%;
      border-radius: 0.6rem;
      cursor: default;
      pointer-events: none;
    }

    .caret {
      display: none;
    }

    .account-menu {
      display: block;
      position: static;
      min-width: 0;
      margin-top: 0.6rem;
      background: transparent;
      border: none;
      box-shadow: none;
      padding: 0;
    }

    .account-summary {
      display: none;
    }

    .account-item {
      color: #e2e8f0;
      text-align: center;
      padding: 0.7rem;
      border: 1px solid rgba(148, 163, 184, 0.4);
      margin-top: 0.5rem;
    }

    .account-item:hover {
      background: rgba(255, 255, 255, 0.12);
      color: white;
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
