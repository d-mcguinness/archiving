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
    background: #ecfdf5;
    color: #059669;
    border-radius: 2rem;
    font-size: 0.8rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 1.25rem;
  }

  .hero h1 {
    font-size: 3rem;
    font-weight: 800;
    color: #0f172a;
    margin: 0 0 1rem;
    letter-spacing: -0.03em;
  }

  .hero-subtitle {
    font-size: 1.1rem;
    color: #475569;
    max-width: 620px;
    margin: 0 auto 2.5rem;
    line-height: 1.7;
  }

  .hero-actions { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; }

  .btn-cta, .btn-cta-inv {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: linear-gradient(135deg, #10b981, #059669);
    color: white;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    transition: transform 0.2s, box-shadow 0.2s;
  }

  .btn-cta:hover, .btn-cta-inv:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(16, 185, 129, 0.35);
  }

  .btn-outline {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: transparent;
    color: #1e293b;
    border: 2px solid #cbd5e1;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    transition: border-color 0.2s;
  }

  .btn-outline:hover { border-color: #64748b; }

  .section-title { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0 0 0.5rem; }
  .section-desc { color: #64748b; font-size: 1rem; margin: 0 auto 2rem; max-width: 520px; }

  .features-section { margin-bottom: 4rem; }

  .features-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 1.25rem;
    text-align: left;
    margin-top: 2rem;
  }

  .feature-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
    transition: border-color 0.2s;
  }

  .feature-card:hover { border-color: #10b981; }

  .feature-icon { font-size: 2rem; display: block; margin-bottom: 0.75rem; }

  .feature-card h3 { margin: 0 0 0.5rem; color: #0f172a; font-size: 1.05rem; }
  .feature-card p { margin: 0; color: #64748b; font-size: 0.875rem; line-height: 1.55; }

  .plans-section { margin-bottom: 4rem; }

  .plans-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 1.25rem;
    margin-top: 2rem;
  }

  .plan-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 2rem 1.5rem;
    text-align: center;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .plan-card:hover { border-color: #10b981; }

  .plan-card.featured {
    border-color: #10b981;
    box-shadow: 0 0 0 1px #10b981;
  }

  .plan-card h3 { margin: 0 0 0.75rem; color: #0f172a; font-size: 1.25rem; font-weight: 700; }
  .plan-card p { margin: 0; color: #64748b; font-size: 0.875rem; line-height: 1.5; }

  .cta-section {
    background: linear-gradient(135deg, #1e293b, #334155);
    color: white;
    padding: 3.5rem 2rem;
    border-radius: 1rem;
    margin-bottom: 2rem;
  }

  .cta-section h2 { margin: 0 0 0.5rem; font-size: 1.75rem; font-weight: 800; }
  .cta-section p { margin: 0 0 2rem; color: #94a3b8; font-size: 1.05rem; }

  .cta-section .btn-cta-inv {
    background: white;
    color: #1e293b;
  }

  .cta-section .btn-cta-inv:hover {
    box-shadow: 0 8px 24px rgba(255, 255, 255, 0.15);
  }

  @media (max-width: 640px) {
    .hero h1 { font-size: 2.25rem; }
  }
</style>
