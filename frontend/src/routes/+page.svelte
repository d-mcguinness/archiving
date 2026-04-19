<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import RoleGate from '$lib/components/RoleGate.svelte';

  export let data: any;

  $: currentRole = $auth.role;
  $: currentUser = $auth.user;

  const standards = ['NOARK5', 'OAIS', 'PREMIS', 'Dublin Core', 'METS', 'EAD', 'BagIt', 'ISAD(G)', 'MODS', 'E-ARK'];
</script>

<svelte:head>
  <title>Arcana - Digital Preservation Platform</title>
</svelte:head>

{#if !currentRole}
  <!-- ═══════════════════════════════════════ -->
  <!--  PUBLIC LANDING PAGE                    -->
  <!-- ═══════════════════════════════════════ -->
  <div class="landing">
    <section class="hero">
      <div class="hero-badge">Open-Standard Archiving</div>
      <h1>Digital Preservation,<br/><span class="gradient-text">Simplified.</span></h1>
      <p class="hero-subtitle">
        Enterprise-grade archival management built on international standards.
        Ingest, preserve, and deliver your digital assets with confidence.
      </p>
      <div class="hero-actions">
        <a href="/login" class="btn-cta">Get Started</a>
        <a href="#how-it-works" class="btn-outline">How It Works</a>
      </div>
    </section>

    <section class="stats-banner">
      <div class="stat-item">
        <span class="stat-num">10</span>
        <span class="stat-label">Archival Standards</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num">3</span>
        <span class="stat-label">OAIS Workflows</span>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <span class="stat-num">100%</span>
        <span class="stat-label">Compliance Ready</span>
      </div>
    </section>

    <section id="how-it-works" class="workflow-section">
      <h2 class="section-title">The OAIS Workflow</h2>
      <p class="section-desc">From submission to access, every step follows the Open Archival Information System reference model.</p>
      <div class="workflow-grid">
        <a href="/ingest" class="workflow-card ingest">
          <div class="workflow-step">1</div>
          <div class="workflow-icon">INGEST</div>
          <h3>Submission Packages</h3>
          <p>Create and validate SIPs across 10 archival standards including NOARK5, OAIS, E-ARK, and more.</p>
          <span class="workflow-arrow">Explore &rarr;</span>
        </a>
        <a href="/preserve" class="workflow-card preserve">
          <div class="workflow-step">2</div>
          <div class="workflow-icon">PRESERVE</div>
          <h3>Long-Term Storage</h3>
          <p>Generate AIPs with full provenance tracking, integrity verification, and format migration support.</p>
          <span class="workflow-arrow">Explore &rarr;</span>
        </a>
        <a href="/deliver" class="workflow-card deliver">
          <div class="workflow-step">3</div>
          <div class="workflow-icon">DELIVER</div>
          <h3>Access Packages</h3>
          <p>Produce DIPs tailored to your users with standard-compliant metadata and granular access controls.</p>
          <span class="workflow-arrow">Explore &rarr;</span>
        </a>
      </div>
    </section>

    <section class="standards-section">
      <h2 class="section-title">Built on Standards You Trust</h2>
      <p class="section-desc">Full compliance with international archival and preservation standards.</p>
      <div class="standards-grid">
        {#each standards as std}
          <div class="standard-chip">{std}</div>
        {/each}
      </div>
    </section>

    <section class="cta-section">
      <h2>Ready to modernize your digital archive?</h2>
      <p>Start managing your preservation workflows in minutes.</p>
      <a href="/login" class="btn-cta">Sign In to Arcana</a>
    </section>
  </div>

{:else}
  <!-- ═══════════════════════════════════════ -->
  <!--  LOGGED-IN HOME                         -->
  <!-- ═══════════════════════════════════════ -->
  <div class="home">
    <div class="welcome-banner">
      <div class="welcome-text">
        <h1>Welcome back, {currentUser?.name || 'User'}</h1>
        <p>
          {#if currentRole === 'ADMIN'}
            System Administrator
          {:else if currentRole === 'TENANT'}
            Tenant Manager
          {:else}
            Contributor
          {/if}
        </p>
      </div>
    </div>

    <h2 class="nav-heading">Where would you like to go?</h2>

    <div class="nav-grid">
      <RoleGate roles={['ADMIN']}>
        <a href="/tenants" class="nav-card tenants">
          <span class="nav-icon">🏢</span>
          <h3>Tenants</h3>
          <p>Manage tenant organizations, plans, and access</p>
        </a>
      </RoleGate>

      <RoleGate roles={['ADMIN', 'TENANT']}>
        <a href="/users" class="nav-card users">
          <span class="nav-icon">👥</span>
          <h3>Users</h3>
          <p>Manage user accounts and role assignments</p>
        </a>
        <a href="/archives" class="nav-card archives">
          <span class="nav-icon">📁</span>
          <h3>Archives</h3>
          <p>Create and manage archival collections</p>
        </a>
        <a href="/sip" class="nav-card sips">
          <span class="nav-icon">📦</span>
          <h3>SIPs</h3>
          <p>Build Submission Information Packages</p>
        </a>
        <a href="/aip" class="nav-card aips">
          <span class="nav-icon">🏗️</span>
          <h3>AIPs</h3>
          <p>Manage Archival Information Packages</p>
        </a>
        <a href="/dip" class="nav-card dips">
          <span class="nav-icon">📤</span>
          <h3>DIPs</h3>
          <p>Create Dissemination Information Packages</p>
        </a>
      </RoleGate>

      <a href="/documents" class="nav-card documents">
        <span class="nav-icon">📄</span>
        <h3>Documents</h3>
        <p>Upload, browse, and download files</p>
      </a>

      <RoleGate roles={['ADMIN', 'TENANT']}>
        <a href="/ingest" class="nav-card ingest-nav">
          <span class="nav-icon">INGEST</span>
          <h3>Ingest Standards</h3>
          <p>Explore supported ingest standards</p>
        </a>
        <a href="/preserve" class="nav-card preserve-nav">
          <span class="nav-icon">PRESERVE</span>
          <h3>Preservation Standards</h3>
          <p>Explore supported preservation standards</p>
        </a>
        <a href="/deliver" class="nav-card deliver-nav">
          <span class="nav-icon">DELIVER</span>
          <h3>Delivery Standards</h3>
          <p>Explore supported delivery standards</p>
        </a>
      </RoleGate>
    </div>
  </div>
{/if}

<style>
  /* ══════════════════════════════════════════
     LANDING PAGE
     ══════════════════════════════════════════ */
  .landing {
    text-align: center;
  }

  .hero {
    padding: 5rem 2rem 3rem;
  }

  .hero-badge {
    display: inline-block;
    padding: 0.4rem 1rem;
    background: #eff6ff;
    color: #2563eb;
    border-radius: 2rem;
    font-size: 0.8rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 1.5rem;
  }

  .hero h1 {
    font-size: 3.25rem;
    font-weight: 800;
    color: #0f172a;
    margin: 0 0 1.25rem;
    line-height: 1.1;
    letter-spacing: -0.03em;
  }

  .gradient-text {
    background: linear-gradient(135deg, #3b82f6, #8b5cf6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .hero-subtitle {
    font-size: 1.15rem;
    color: #475569;
    max-width: 560px;
    margin: 0 auto 2.5rem;
    line-height: 1.7;
  }

  .hero-actions {
    display: flex;
    justify-content: center;
    gap: 1rem;
    flex-wrap: wrap;
  }

  .btn-cta {
    display: inline-block;
    padding: 0.9rem 2.25rem;
    background: linear-gradient(135deg, #3b82f6, #7c3aed);
    color: white;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 1rem;
    transition: transform 0.2s, box-shadow 0.2s;
  }

  .btn-cta:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(59, 130, 246, 0.35);
  }

  .btn-outline {
    display: inline-block;
    padding: 0.9rem 2.25rem;
    background: transparent;
    color: #1e293b;
    border: 2px solid #cbd5e1;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .btn-outline:hover {
    border-color: #64748b;
  }

  /* Stats banner */
  .stats-banner {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 2.5rem;
    padding: 2rem;
    margin: 0 auto 3rem;
    max-width: 600px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 1rem;
  }

  .stat-item {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .stat-num {
    font-size: 1.75rem;
    font-weight: 800;
    color: #0f172a;
  }

  .stat-label {
    font-size: 0.8rem;
    color: #64748b;
    font-weight: 500;
    margin-top: 0.15rem;
  }

  .stat-divider {
    width: 1px;
    height: 2.5rem;
    background: #e2e8f0;
  }

  /* Workflow section */
  .workflow-section {
    margin-bottom: 4rem;
  }

  .section-title {
    font-size: 1.75rem;
    font-weight: 800;
    color: #0f172a;
    margin: 0 0 0.5rem;
  }

  .section-desc {
    color: #64748b;
    font-size: 1rem;
    margin: 0 auto 2rem;
    max-width: 520px;
  }

  .workflow-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1.5rem;
    text-align: left;
  }

  .workflow-card {
    position: relative;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 2rem 1.75rem 1.75rem;
    text-decoration: none;
    color: inherit;
    transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
    overflow: hidden;
  }

  .workflow-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  }

  .workflow-card.ingest { border-top: 3px solid #3b82f6; }
  .workflow-card.ingest:hover { border-color: #3b82f6; }
  .workflow-card.preserve { border-top: 3px solid #10b981; }
  .workflow-card.preserve:hover { border-color: #10b981; }
  .workflow-card.deliver { border-top: 3px solid #8b5cf6; }
  .workflow-card.deliver:hover { border-color: #8b5cf6; }

  .workflow-step {
    position: absolute;
    top: 1rem;
    right: 1rem;
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    background: #f1f5f9;
    color: #94a3b8;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 800;
    font-size: 0.8rem;
  }

  .workflow-icon {
    font-size: 0.7rem;
    font-weight: 800;
    letter-spacing: 0.12em;
    color: #64748b;
    margin-bottom: 0.75rem;
  }

  .workflow-card h3 {
    margin: 0 0 0.5rem;
    color: #0f172a;
    font-size: 1.15rem;
  }

  .workflow-card p {
    margin: 0 0 1rem;
    color: #64748b;
    font-size: 0.9rem;
    line-height: 1.55;
  }

  .workflow-arrow {
    font-size: 0.85rem;
    font-weight: 600;
    color: #3b82f6;
  }

  /* Standards */
  .standards-section {
    margin-bottom: 4rem;
  }

  .standards-grid {
    display: flex;
    justify-content: center;
    flex-wrap: wrap;
    gap: 0.6rem;
    max-width: 600px;
    margin: 0 auto;
  }

  .standard-chip {
    padding: 0.45rem 1rem;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 2rem;
    font-size: 0.825rem;
    font-weight: 600;
    color: #334155;
    transition: border-color 0.2s, background 0.2s;
  }

  .standard-chip:hover {
    border-color: #3b82f6;
    background: #eff6ff;
    color: #1e40af;
  }

  /* CTA */
  .cta-section {
    background: linear-gradient(135deg, #1e293b, #334155);
    color: white;
    padding: 3.5rem 2rem;
    border-radius: 1rem;
    margin-bottom: 2rem;
  }

  .cta-section h2 {
    margin: 0 0 0.5rem;
    font-size: 1.75rem;
    font-weight: 800;
  }

  .cta-section p {
    margin: 0 0 2rem;
    color: #94a3b8;
    font-size: 1.05rem;
  }

  .cta-section .btn-cta {
    background: white;
    color: #1e293b;
  }

  .cta-section .btn-cta:hover {
    box-shadow: 0 8px 24px rgba(255, 255, 255, 0.15);
  }

  /* ══════════════════════════════════════════
     LOGGED-IN HOME
     ══════════════════════════════════════════ */
  .home {
    max-width: 1000px;
    margin: 0 auto;
  }

  .welcome-banner {
    background: linear-gradient(135deg, #1e293b, #334155);
    color: white;
    padding: 2.5rem;
    border-radius: 1rem;
    margin-bottom: 2.5rem;
  }

  .welcome-text h1 {
    margin: 0 0 0.35rem;
    font-size: 1.75rem;
    font-weight: 700;
  }

  .welcome-text p {
    margin: 0;
    color: #94a3b8;
    font-size: 1rem;
  }

  .nav-heading {
    font-size: 1.1rem;
    font-weight: 600;
    color: #64748b;
    margin: 0 0 1.25rem;
  }

  .nav-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1rem;
  }

  .nav-card {
    display: flex;
    flex-direction: column;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
    text-decoration: none;
    color: inherit;
    transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  }

  .nav-card:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }

  .nav-icon {
    font-size: 2rem;
    margin-bottom: 0.75rem;
    line-height: 1;
  }

  /* Text-style icons for standards cards */
  .nav-card.ingest-nav .nav-icon,
  .nav-card.preserve-nav .nav-icon,
  .nav-card.deliver-nav .nav-icon {
    font-size: 0.7rem;
    font-weight: 800;
    letter-spacing: 0.1em;
    color: #64748b;
    margin-bottom: 0.75rem;
  }

  .nav-card h3 {
    margin: 0 0 0.35rem;
    font-size: 1.05rem;
    color: #0f172a;
  }

  .nav-card p {
    margin: 0;
    font-size: 0.85rem;
    color: #64748b;
    line-height: 1.45;
  }

  /* Card accent colors */
  .nav-card.tenants:hover    { border-color: #10b981; }
  .nav-card.users:hover      { border-color: #f59e0b; }
  .nav-card.archives:hover   { border-color: #06b6d4; }
  .nav-card.sips:hover       { border-color: #ec4899; }
  .nav-card.aips:hover       { border-color: #6366f1; }
  .nav-card.dips:hover       { border-color: #f97316; }
  .nav-card.documents:hover  { border-color: #8b5cf6; }
  .nav-card.ingest-nav:hover   { border-color: #3b82f6; }
  .nav-card.preserve-nav:hover { border-color: #10b981; }
  .nav-card.deliver-nav:hover  { border-color: #8b5cf6; }

  /* ══════════════════════════════════════════
     RESPONSIVE
     ══════════════════════════════════════════ */
  @media (max-width: 640px) {
    .hero h1 {
      font-size: 2.25rem;
    }

    .stats-banner {
      flex-direction: column;
      gap: 1rem;
    }

    .stat-divider {
      width: 3rem;
      height: 1px;
    }
  }
</style>
