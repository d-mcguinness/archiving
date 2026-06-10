<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import RoleGate from '$lib/components/RoleGate.svelte';

  $: currentRole = $auth.role;
  $: currentUser = $auth.user;

  const standards = ['NOARK5', 'OAIS', 'PREMIS', 'Dublin Core', 'METS', 'EAD', 'BagIt', 'ISAD(G)', 'MODS', 'E-ARK'];
  // Compliance-grade standards that generate validated, regulator-ready packages.
  const premiumStandards = ['NOARK5', 'E-ARK'];

  const tiers = [
    {
      name: 'Free',
      tagline: 'Explore the full archival workflow',
      features: ['5 team members', '10 archives', '100 MB storage', 'All 10 open standards', 'SIP → AIP → DIP workflow'],
      cta: 'Start free',
      popular: false
    },
    {
      name: 'Basic',
      tagline: 'For small teams getting started',
      features: ['25 team members', '100 archives', '1 GB storage', 'Pay-as-you-go premium packages', 'Usage-based scaling'],
      cta: 'Choose Basic',
      popular: false
    },
    {
      name: 'Professional',
      tagline: 'Compliance at scale',
      features: ['100 team members', '1,000 archives', '10 GB storage', '100 NOARK 5.5 / E-ARK packages / mo', 'Priority support'],
      cta: 'Choose Professional',
      popular: true
    },
    {
      name: 'Enterprise',
      tagline: 'Unlimited preservation',
      features: ['Unlimited members & archives', 'Unlimited storage', 'Unlimited premium conformance', 'Audit logging & SSO-ready', 'Dedicated support'],
      cta: 'Contact sales',
      popular: false
    }
  ];
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
        Archival management built on international standards — from a free tier to
        unlimited enterprise. Ingest, preserve, and deliver your digital assets
        with compliance-grade NOARK&nbsp;5.5 and E-ARK conformance.
      </p>
      <div class="hero-actions">
        <a href="/login" class="btn-cta">Get Started Free</a>
        <a href="#how-it-works" class="btn-outline">How It Works</a>
      </div>
      <p class="hero-note">No credit card to start · <a href="#pricing">Compare plans &rarr;</a></p>
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
      <p class="section-desc">Ten archival standards out of the box — including compliance-grade <strong>NOARK&nbsp;5.5</strong> and <strong>E-ARK</strong> conformance for regulated archives.</p>
      <div class="standards-grid">
        {#each standards as std}
          <div class="standard-chip" class:premium={premiumStandards.includes(std)}>
            {std}{#if premiumStandards.includes(std)}<span class="premium-star">★</span>{/if}
          </div>
        {/each}
      </div>
      <p class="standards-legend"><span class="premium-star">★</span> Premium conformance — validated, regulator-ready NOARK&nbsp;5.5 / E-ARK packages</p>
    </section>

    <section id="pricing" class="pricing-section">
      <h2 class="section-title">Plans That Scale With Your Archive</h2>
      <p class="section-desc">Start free on open standards. Upgrade for compliance-grade conformance and room to grow.</p>
      <div class="pricing-grid">
        {#each tiers as tier}
          <div class="tier-card" class:popular={tier.popular}>
            {#if tier.popular}<div class="tier-badge">Most Popular</div>{/if}
            <h3 class="tier-name">{tier.name}</h3>
            <p class="tier-tagline">{tier.tagline}</p>
            <ul class="tier-features">
              {#each tier.features as f}
                <li>{f}</li>
              {/each}
            </ul>
            <a href="/register" class="tier-cta">{tier.cta}</a>
          </div>
        {/each}
      </div>
      <p class="pricing-usage">
        Usage-based add-ons: <strong>$0.18</strong> / GB-month for storage beyond your plan,
        <strong>$0.40</strong> per premium NOARK&nbsp;5.5 / E-ARK package.
      </p>
      <p class="pricing-custom">Need something bespoke? <a href="/login">Custom plans</a> with tailored limits and SLAs are available.</p>
    </section>

    <section class="cta-section">
      <h2>Ready to modernize your digital archive?</h2>
      <p>Start free in minutes — no credit card required. Upgrade when you need compliance-grade conformance.</p>
      <a href="/login" class="btn-cta">Get Started Free</a>
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

  .standard-chip.premium {
    border-color: #c4b5fd;
    background: linear-gradient(135deg, #faf5ff, #eff6ff);
    color: #6d28d9;
    font-weight: 700;
  }

  .standard-chip.premium:hover {
    border-color: #8b5cf6;
    background: #f5f3ff;
    color: #5b21b6;
  }

  .premium-star {
    color: #8b5cf6;
    margin-left: 0.3rem;
    font-size: 0.7rem;
    vertical-align: middle;
  }

  .standards-legend {
    margin: 1.25rem auto 0;
    font-size: 0.8rem;
    color: #64748b;
  }

  /* Hero note */
  .hero-note {
    margin: 1.25rem 0 0;
    font-size: 0.85rem;
    color: #64748b;
  }

  .hero-note a {
    color: #3b82f6;
    font-weight: 600;
    text-decoration: none;
  }

  .hero-note a:hover {
    text-decoration: underline;
  }

  /* Pricing */
  .pricing-section {
    margin-bottom: 4rem;
  }

  .pricing-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1.25rem;
    text-align: left;
    align-items: stretch;
  }

  .tier-card {
    position: relative;
    display: flex;
    flex-direction: column;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.85rem;
    padding: 1.75rem 1.5rem;
    transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  }

  .tier-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  }

  .tier-card.popular {
    border-color: #8b5cf6;
    box-shadow: 0 8px 28px rgba(139, 92, 246, 0.18);
  }

  .tier-badge {
    position: absolute;
    top: -0.7rem;
    left: 50%;
    transform: translateX(-50%);
    background: linear-gradient(135deg, #3b82f6, #7c3aed);
    color: white;
    font-size: 0.65rem;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    padding: 0.3rem 0.8rem;
    border-radius: 1rem;
    white-space: nowrap;
  }

  .tier-name {
    margin: 0 0 0.25rem;
    font-size: 1.2rem;
    font-weight: 800;
    color: #0f172a;
  }

  .tier-tagline {
    margin: 0 0 1.1rem;
    font-size: 0.85rem;
    color: #64748b;
    min-height: 2.4em;
  }

  .tier-features {
    list-style: none;
    padding: 0;
    margin: 0 0 1.5rem;
    flex: 1;
  }

  .tier-features li {
    position: relative;
    padding: 0.4rem 0 0.4rem 1.5rem;
    font-size: 0.875rem;
    color: #334155;
    border-bottom: 1px solid #f1f5f9;
  }

  .tier-features li::before {
    content: '✓';
    position: absolute;
    left: 0;
    color: #10b981;
    font-weight: 800;
  }

  .tier-cta {
    display: block;
    text-align: center;
    padding: 0.7rem 1rem;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.9rem;
    border: 1.5px solid #cbd5e1;
    color: #1e293b;
    transition: border-color 0.2s, background 0.2s, color 0.2s;
  }

  .tier-cta:hover {
    border-color: #64748b;
  }

  .tier-card.popular .tier-cta {
    background: linear-gradient(135deg, #3b82f6, #7c3aed);
    color: white;
    border-color: transparent;
  }

  .tier-card.popular .tier-cta:hover {
    box-shadow: 0 6px 18px rgba(124, 58, 237, 0.35);
  }

  .pricing-usage {
    margin: 1.75rem auto 0;
    max-width: 620px;
    font-size: 0.875rem;
    color: #475569;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.6rem;
    padding: 0.85rem 1.25rem;
  }

  .pricing-usage strong {
    color: #0f172a;
  }

  .pricing-custom {
    margin: 0.9rem 0 0;
    font-size: 0.85rem;
    color: #64748b;
  }

  .pricing-custom a {
    color: #3b82f6;
    font-weight: 600;
    text-decoration: none;
  }

  .pricing-custom a:hover {
    text-decoration: underline;
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
