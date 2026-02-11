<script lang="ts">
  import '../app.css';
  import Toast from '$lib/components/Toast.svelte';
  import { page } from '$app/stores';

  // Reactive declaration ensures this updates whenever the route changes
  $: currentPath = $page.url.pathname;

  // Check if a path is active based on current route
  $: isActive = (path: string): boolean => {
    if (path === '/') {
      return currentPath === '/';
    }
    return currentPath === path || currentPath.startsWith(path + '/');
  };
</script>

<div class="app">
  <Toast />

  <header>
    <nav>
      <div class="nav-container">
        <h1><a href="/">Archiving System</a></h1>
        <ul class="nav-links">
          <li>
            <a
              href="/"
              class="dashboard-link"
              class:active={isActive('/')}
            >
              📊 Dashboard
            </a>
          </li>
          <li>
            <a
              href="/archives"
              class="archives-link"
              class:active={isActive('/archives')}
            >
              📁 Archives
            </a>
          </li>
          <li>
            <a
              href="/tenants"
              class="tenants-link"
              class:active={isActive('/tenants')}
            >
              🏢 Tenants
            </a>
          </li>
          <li>
            <a
              href="/users"
              class="users-link"
              class:active={isActive('/users')}
            >
              👥 Users
            </a>
          </li>
          <li>
            <a
              href="/admin"
              class="admin-link"
              class:active={isActive('/admin')}
            >
              🛡️ Admin
            </a>
          </li>
        </ul>
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
  }

  h1 a {
    color: white;
    text-decoration: none;
    font-size: 1.5rem;
  }

  .nav-links {
    display: flex;
    list-style: none;
    gap: 2rem;
    margin: 0;
    padding: 0;
  }

  .nav-links a {
    color: white;
    text-decoration: none;
    padding: 0.5rem 1rem;
    border-radius: 0.375rem;
    transition: all 0.2s;
    font-weight: 600;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  }

  .nav-links a:hover:not(.active) {
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
    transform: translateY(-1px);
  }

  .nav-links a.active {
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.6) !important;
    transform: scale(1.12) !important;
    border: 3px solid rgba(255, 255, 255, 0.8) !important;
    animation: pulse 2s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% {
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.6);
    }
    50% {
      box-shadow: 0 8px 20px rgba(255, 255, 255, 0.4);
    }
  }

  .nav-links a.dashboard-link {
    background: linear-gradient(135deg, #3b82f6, #2563eb);
  }

  .nav-links a.dashboard-link:hover:not(.active) {
    background: linear-gradient(135deg, #2563eb, #1d4ed8);
  }

  .nav-links a.dashboard-link.active {
    background: linear-gradient(135deg, #93c5fd, #60a5fa) !important;
  }

  .nav-links a.archives-link {
    background: linear-gradient(135deg, #06b6d4, #0891b2);
  }

  .nav-links a.archives-link:hover:not(.active) {
    background: linear-gradient(135deg, #0891b2, #0e7490);
  }

  .nav-links a.archives-link.active {
    background: linear-gradient(135deg, #67e8f9, #22d3ee) !important;
  }

  .nav-links a.tenants-link {
    background: linear-gradient(135deg, #10b981, #059669);
  }

  .nav-links a.tenants-link:hover:not(.active) {
    background: linear-gradient(135deg, #059669, #047857);
  }

  .nav-links a.tenants-link.active {
    background: linear-gradient(135deg, #6ee7b7, #34d399) !important;
  }

  .nav-links a.users-link {
    background: linear-gradient(135deg, #f59e0b, #d97706);
  }

  .nav-links a.users-link:hover:not(.active) {
    background: linear-gradient(135deg, #d97706, #b45309);
  }

  .nav-links a.users-link.active {
    background: linear-gradient(135deg, #fde68a, #fbbf24) !important;
  }

  .nav-links a.admin-link {
    background: linear-gradient(135deg, #8b5cf6, #6366f1);
  }

  .nav-links a.admin-link:hover:not(.active) {
    background: linear-gradient(135deg, #7c3aed, #4f46e5);
  }

  .nav-links a.admin-link.active {
    background: linear-gradient(135deg, #c4b5fd, #a78bfa) !important;
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
