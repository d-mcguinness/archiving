<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  // If admin, redirect to management page
  $: if (browser && $auth.isLoggedIn && $auth.role === 'ADMIN') {
    goto('/admin/tenants');
  }
  // If tenant, redirect to their tenant page
  $: if (browser && $auth.isLoggedIn && $auth.role === 'TENANT' && $auth.tenantId) {
    goto(`/tenants/${$auth.tenantId}`);
  }
</script>

<svelte:head>
  <title>Multi-Tenancy - Arcana</title>
</svelte:head>

{#if !$auth.isLoggedIn}
  <div class="public-page">
    <section class="hero">
      <div class="hero-badge">Enterprise Feature</div>
      <h1>Multi-Tenant Architecture</h1>
      <p class="hero-subtitle">
        Arcana supports isolated tenant environments, each with their own users, archives, and preservation workflows.
        Data is securely partitioned with role-based access controls at every level.
      </p>
      <div class="hero-actions">
        <a href="/login" class="btn-cta">Sign In</a>
        <a href="#features" class="btn-outline">Learn More</a>
      </div>
    </section>

    <section id="features" class="features-section">
      <h2 class="section-title">Tenant Capabilities</h2>
      <div class="features-grid">
        <div class="feature-card">
          <span class="feature-icon">🏢</span>
          <h3>Isolated Environments</h3>
          <p>Each tenant operates in a fully isolated space with their own archives, users, and configurations.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">👥</span>
          <h3>User Management</h3>
          <p>Tenant admins manage their own users with role assignments and permission controls.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📁</span>
          <h3>Dedicated Archives</h3>
          <p>Create and manage archives scoped to your organization with full OAIS workflow support.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📦</span>
          <h3>Intake / Preservation / Release</h3>
          <p>Build submission, archival, and dissemination packages within your tenant boundary.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📄</span>
          <h3>Document Storage</h3>
          <p>Upload and manage documents with tenant-scoped access. Attach files to any package type.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">🔒</span>
          <h3>Access Control</h3>
          <p>Three-tier role system: Admin oversees all tenants, Tenant managers run their org, Users contribute.</p>
        </div>
      </div>
    </section>

    <section class="plans-section">
      <h2 class="section-title">Tenant Plans</h2>
      <p class="section-desc">Choose the plan that fits your organization's archival needs.</p>
      <div class="plans-grid">
        <div class="plan-card">
          <h3>Free</h3>
          <p>For individuals and small teams getting started with digital preservation.</p>
        </div>
        <div class="plan-card featured">
          <h3>Professional</h3>
          <p>For organizations with active archival workflows and multiple users.</p>
        </div>
        <div class="plan-card">
          <h3>Enterprise</h3>
          <p>For large institutions with complex compliance and preservation requirements.</p>
        </div>
      </div>
    </section>

    <section class="cta-section">
      <h2>Ready to set up your tenant?</h2>
      <p>Sign in to create and manage your organization's archival environment.</p>
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
    background: var(--arc-chip-green-bg);
    color: var(--arc-chip-green-ink);
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

  .section-title { font-size: 1.75rem; font-weight: 700; color: var(--arc-ink, #0f172a); margin: 0 0 0.5rem; }
  .section-desc { color: var(--arc-muted, #64748b); font-size: 1rem; margin: 0 auto 2rem; max-width: 520px; }

  .features-section { margin-bottom: 4rem; }

  .features-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 1.25rem;
    text-align: left;
    margin-top: 2rem;
  }

  .feature-card {
    background: var(--arc-card, white);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 1.5rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .feature-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border, #c7d2fe);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  .feature-icon { font-size: 2rem; display: block; margin-bottom: 0.75rem; }

  .feature-card h3 { margin: 0 0 0.5rem; color: var(--arc-ink, #0f172a); font-size: 1.05rem; }
  .feature-card p { margin: 0; color: var(--arc-muted, #64748b); font-size: 0.875rem; line-height: 1.55; }

  .plans-section { margin-bottom: 4rem; }

  .plans-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 1.25rem;
    margin-top: 2rem;
  }

  .plan-card {
    background: var(--arc-card, white);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 2rem 1.5rem;
    text-align: center;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  }

  .plan-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border, #c7d2fe);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  .plan-card.featured {
    border-color: var(--arc-violet, #8b5cf6);
    box-shadow: 0 18px 44px -18px rgba(139, 92, 246, 0.5);
  }

  .plan-card.featured:hover { border-color: var(--arc-violet, #8b5cf6); }

  .plan-card h3 { margin: 0 0 0.75rem; color: var(--arc-ink, #0f172a); font-size: 1.25rem; font-weight: 700; }
  .plan-card p { margin: 0; color: var(--arc-muted, #64748b); font-size: 0.875rem; line-height: 1.5; }

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

  .cta-section .btn-cta-inv {
    background: white;
    color: #1e293b;
    box-shadow: 0 10px 30px -8px rgba(0, 0, 0, 0.4);
  }

  .cta-section .btn-cta-inv:hover {
    box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.5);
  }

  @media (max-width: 640px) {
    .hero h1 { font-size: 2.25rem; }
  }

  @media (prefers-reduced-motion: reduce) {
    .btn-cta, .btn-cta-inv, .btn-outline, .feature-card, .plan-card { transition: none; }
    .btn-cta:hover, .btn-cta-inv:hover, .feature-card:hover, .plan-card:hover { transform: none; }
  }
</style>
