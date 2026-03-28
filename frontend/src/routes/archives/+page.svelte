<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  $: if (browser && $auth.isLoggedIn && $auth.role === 'ADMIN') {
    goto('/admin/archives');
  }
  $: if (browser && $auth.isLoggedIn && $auth.role === 'TENANT' && $auth.tenantId) {
    goto(`/tenants/${$auth.tenantId}/archives`);
  }
</script>

<svelte:head>
  <title>Archives - Arcana</title>
</svelte:head>

{#if !$auth.isLoggedIn}
  <div class="public-page">
    <section class="hero">
      <div class="hero-badge">Digital Preservation</div>
      <h1>Archive Management</h1>
      <p class="hero-subtitle">
        Create, organize, and manage digital archives using international preservation standards.
        Arcana supports the full OAIS lifecycle from ingest to dissemination.
      </p>
      <div class="hero-actions">
        <a href="/login" class="btn-cta">Sign In</a>
        <a href="#capabilities" class="btn-outline">Learn More</a>
      </div>
    </section>

    <section id="capabilities" class="features-section">
      <h2 class="section-title">Archive Capabilities</h2>
      <div class="features-grid">
        <div class="feature-card">
          <span class="feature-icon">📁</span>
          <h3>Collection Management</h3>
          <p>Organize digital assets into structured archival collections with metadata and provenance tracking.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📦</span>
          <h3>OAIS Packages</h3>
          <p>Build SIPs, AIPs, and DIPs following the Open Archival Information System reference model.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">🔧</span>
          <h3>10 Standards</h3>
          <p>Support for NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), MODS, and E-ARK.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">🌳</span>
          <h3>Element Hierarchies</h3>
          <p>Build nested element trees with schema-driven fields, parent-child relationships, and recursive structures.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📄</span>
          <h3>Document Attachment</h3>
          <p>Upload and link documents to archives and packages with cloud storage and download support.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📥</span>
          <h3>Export & Extract</h3>
          <p>Export archives and packages as encrypted, portable files for transfer or offline storage.</p>
        </div>
      </div>
    </section>

    <section class="cta-section">
      <h2>Start preserving your digital assets</h2>
      <p>Sign in to create your first archive.</p>
      <a href="/login" class="btn-cta-inv">Get Started</a>
    </section>
  </div>
{/if}

<style>
  .public-page { text-align: center; }

  .hero { padding: 4rem 2rem 3rem; }

  .hero-badge {
    display: inline-block; padding: 0.4rem 1rem;
    background: #ecfeff; color: #0891b2;
    border-radius: 2rem; font-size: 0.8rem; font-weight: 700;
    text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 1.25rem;
  }

  .hero h1 { font-size: 3rem; font-weight: 800; color: #0f172a; margin: 0 0 1rem; letter-spacing: -0.03em; }

  .hero-subtitle { font-size: 1.1rem; color: #475569; max-width: 620px; margin: 0 auto 2.5rem; line-height: 1.7; }

  .hero-actions { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; }

  .btn-cta, .btn-cta-inv {
    display: inline-block; padding: 0.85rem 2rem;
    background: linear-gradient(135deg, #06b6d4, #0891b2);
    color: white; border-radius: 0.5rem; text-decoration: none;
    font-weight: 700; font-size: 0.95rem;
    transition: transform 0.2s, box-shadow 0.2s;
  }
  .btn-cta:hover, .btn-cta-inv:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(6, 182, 212, 0.35); }

  .btn-outline {
    display: inline-block; padding: 0.85rem 2rem;
    background: transparent; color: #1e293b; border: 2px solid #cbd5e1;
    border-radius: 0.5rem; text-decoration: none; font-weight: 700; font-size: 0.95rem;
    transition: border-color 0.2s;
  }
  .btn-outline:hover { border-color: #64748b; }

  .section-title { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0 0 2rem; }

  .features-section { margin-bottom: 4rem; }

  .features-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 1.25rem; text-align: left;
  }

  .feature-card {
    background: white; border: 1px solid #e2e8f0; border-radius: 0.75rem;
    padding: 1.5rem; transition: border-color 0.2s;
  }
  .feature-card:hover { border-color: #06b6d4; }

  .feature-icon { font-size: 2rem; display: block; margin-bottom: 0.75rem; }
  .feature-card h3 { margin: 0 0 0.5rem; color: #0f172a; font-size: 1.05rem; }
  .feature-card p { margin: 0; color: #64748b; font-size: 0.875rem; line-height: 1.55; }

  .cta-section {
    background: linear-gradient(135deg, #1e293b, #334155); color: white;
    padding: 3.5rem 2rem; border-radius: 1rem; margin-bottom: 2rem;
  }
  .cta-section h2 { margin: 0 0 0.5rem; font-size: 1.75rem; font-weight: 800; }
  .cta-section p { margin: 0 0 2rem; color: #94a3b8; font-size: 1.05rem; }
  .cta-section .btn-cta-inv { background: white; color: #1e293b; }
  .cta-section .btn-cta-inv:hover { box-shadow: 0 8px 24px rgba(255, 255, 255, 0.15); }

  @media (max-width: 640px) { .hero h1 { font-size: 2.25rem; } }
</style>
