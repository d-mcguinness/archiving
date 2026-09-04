<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  $: if (browser && $auth.isLoggedIn && $auth.role === 'ADMIN') {
    goto('/admin');
  }
  $: if (browser && $auth.isLoggedIn && $auth.role === 'TENANT' && $auth.tenantId) {
    goto(`/tenants/${$auth.tenantId}/users`);
  }
  $: if (browser && $auth.isLoggedIn && $auth.role === 'USER' && $auth.tenantId && $auth.user?.id) {
    goto(`/tenants/${$auth.tenantId}/users/${$auth.user.id}`);
  }
</script>

<svelte:head>
  <title>User Management - Arcana</title>
</svelte:head>

{#if !$auth.isLoggedIn}
  <div class="public-page">
    <section class="hero">
      <div class="hero-badge">Role-Based Access</div>
      <h1>User Management</h1>
      <p class="hero-subtitle">
        Arcana provides a three-tier role system for managing access to archival resources.
        Administrators, tenant managers, and contributors each have tailored permissions and workflows.
      </p>
      <div class="hero-actions">
        <a href="/login" class="btn-cta">Sign In</a>
        <a href="#roles" class="btn-outline">View Roles</a>
      </div>
    </section>

    <section id="roles" class="roles-section">
      <h2 class="section-title">Role Hierarchy</h2>
      <div class="roles-grid">
        <div class="role-card admin">
          <div class="role-icon">👑</div>
          <h3>Administrator</h3>
          <p>Full system access. Manages all tenants, users, archives, and preservation workflows across the platform.</p>
          <ul>
            <li>Create and manage tenants</li>
            <li>Assign users to tenants</li>
            <li>Access all archives and packages</li>
            <li>System-wide configuration</li>
          </ul>
        </div>
        <div class="role-card tenant">
          <div class="role-icon">🏢</div>
          <h3>Tenant Manager</h3>
          <p>Organization-level management. Oversees users, archives, and packages within their tenant scope.</p>
          <ul>
            <li>Manage tenant users</li>
            <li>Create and manage archives</li>
            <li>Build Intakes, Preservations, and Releases</li>
            <li>Upload and manage documents</li>
          </ul>
        </div>
        <div class="role-card user">
          <div class="role-icon">👤</div>
          <h3>Contributor</h3>
          <p>Individual access. Upload documents and view assigned resources within their tenant.</p>
          <ul>
            <li>View personal profile</li>
            <li>Upload documents</li>
            <li>View assigned packages</li>
            <li>Download files</li>
          </ul>
        </div>
      </div>
    </section>

    <section class="features-section">
      <h2 class="section-title">Key Features</h2>
      <div class="features-grid">
        <div class="feature-item">
          <span class="check">&#10003;</span>
          <span>Role-based navigation and page access</span>
        </div>
        <div class="feature-item">
          <span class="check">&#10003;</span>
          <span>Tenant-scoped data isolation</span>
        </div>
        <div class="feature-item">
          <span class="check">&#10003;</span>
          <span>User assignment to multiple tenants</span>
        </div>
        <div class="feature-item">
          <span class="check">&#10003;</span>
          <span>Admin impersonation for support</span>
        </div>
        <div class="feature-item">
          <span class="check">&#10003;</span>
          <span>Bulk user import via file upload</span>
        </div>
        <div class="feature-item">
          <span class="check">&#10003;</span>
          <span>Inline user profile editing</span>
        </div>
      </div>
    </section>

    <section class="cta-section">
      <h2>Ready to manage your team?</h2>
      <p>Sign in to start managing users and permissions.</p>
      <a href="/login" class="btn-cta-inv">Get Started</a>
    </section>
  </div>
{/if}

<style>
  .public-page { text-align: center; }

  .hero { padding: 4rem 2rem 3rem; }

  .hero-badge {
    display: inline-block;
    padding: 0.4rem 1rem;
    background: var(--arc-chip-amber-bg);
    color: var(--arc-chip-amber-ink);
    border-radius: 9999px;
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin-bottom: 1.25rem;
  }

  .hero h1 {
    font-size: 3rem;
    font-weight: 700;
    color: var(--arc-ink, #0f172a);
    margin: 0 0 1rem;
  }

  .hero-subtitle {
    font-size: 1.1rem;
    color: var(--arc-muted, #64748b);
    max-width: 620px;
    margin: 0 auto 2.5rem;
    line-height: 1.7;
  }

  .hero-actions { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; }

  .btn-cta, .btn-cta-inv {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
  }

  .btn-cta:hover, .btn-cta-inv:hover {
    transform: translateY(-2px);
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    box-shadow: var(--arc-shadow-btn-hover, 0 16px 40px -8px rgba(124, 58, 237, 0.75));
  }

  .btn-outline {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: var(--arc-card, white);
    color: var(--arc-ink, #1e293b);
    border: 1.5px solid var(--arc-line-strong, #cbd5e1);
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    transition: border-color 0.18s ease, color 0.18s ease;
  }

  .btn-outline:hover { border-color: var(--arc-indigo, #6366f1); color: var(--arc-indigo-deep, #4f46e5); }

  .section-title { font-size: 1.75rem; font-weight: 700; color: var(--arc-ink, #0f172a); margin: 0 0 2rem; }

  .roles-section { margin-bottom: 4rem; }

  .roles-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1.5rem;
    text-align: left;
  }

  .role-card {
    background: var(--arc-card, white);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 2rem 1.5rem;
    border-top: 3px solid var(--arc-line-strong, #e2e8f0);
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, box-shadow 0.2s ease;
  }

  .role-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  .role-card.admin { border-top-color: #f59e0b; }
  .role-card.tenant { border-top-color: #10b981; }
  .role-card.user { border-top-color: #8b5cf6; }

  .role-icon { font-size: 2.5rem; margin-bottom: 0.75rem; }

  .role-card h3 { margin: 0 0 0.5rem; color: var(--arc-ink, #0f172a); font-size: 1.15rem; }
  .role-card p { margin: 0 0 1rem; color: var(--arc-muted, #64748b); font-size: 0.875rem; line-height: 1.55; }

  .role-card ul {
    list-style: none;
    padding: 0;
    margin: 0;
  }

  .role-card li {
    padding: 0.3rem 0;
    color: var(--arc-body, #475569);
    font-size: 0.825rem;
  }

  .role-card li::before {
    content: '→ ';
    color: var(--arc-faint, #94a3b8);
  }

  .features-section { margin-bottom: 4rem; }

  .features-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1rem;
    text-align: left;
  }

  .feature-item {
    display: flex;
    gap: 0.75rem;
    align-items: center;
    padding: 1rem;
    background: var(--arc-card, white);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 0.75rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    font-size: 0.875rem;
    color: var(--arc-body, #334155);
  }

  .check {
    flex-shrink: 0;
    width: 1.5rem;
    height: 1.5rem;
    background: var(--arc-chip-green-bg);
    color: var(--arc-chip-green-ink);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.75rem;
    font-weight: 800;
  }

  .cta-section {
    background: var(--arc-grad-dark, radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%));
    color: white;
    border: 1px solid var(--arc-line, #e8edf3);
    padding: 3.5rem 2rem;
    border-radius: 1rem;
    margin-bottom: 2rem;
  }

  .cta-section h2 { margin: 0 0 0.5rem; font-size: 1.75rem; font-weight: 700; color: #f8fafc; }
  .cta-section p { margin: 0 0 2rem; color: #cbd5e1; font-size: 1.05rem; }

  .cta-section .btn-cta-inv { background: white; color: #1e293b; box-shadow: 0 10px 30px -8px rgba(0, 0, 0, 0.4); }
  .cta-section .btn-cta-inv:hover { box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.5); }

  @media (max-width: 640px) {
    .hero h1 { font-size: 2.25rem; }
  }

  @media (prefers-reduced-motion: reduce) {
    .btn-cta, .btn-cta-inv, .btn-outline, .role-card { transition: none; }
    .btn-cta:hover, .btn-cta-inv:hover, .role-card:hover { transform: none; }
  }
</style>
