<script>
  import '../app.css';
  import Toast from '$lib/components/Toast.svelte';
</script>

<div class="app">
  <Toast />

  <header>
    <nav>
      <div class="nav-container">
        <h1><a href="/">Archiving System</a></h1>
        <ul class="nav-links">
          <li><a href="/">Dashboard</a></li>
          <li><a href="/tenants">Tenants</a></li>
          <li><a href="/users">Users</a></li>
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
    transition: background-color 0.2s;
  }

  .nav-links a:hover {
    background: rgba(255, 255, 255, 0.1);
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
