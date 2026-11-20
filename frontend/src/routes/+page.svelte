<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import RoleGate from '$lib/components/RoleGate.svelte';

  $: currentRole = $auth.role;
  $: currentUser = $auth.user;

  // Compliance-grade standards lead the list.
  const standards = ['NOARK 5.5', 'E-ARK', 'OAIS', 'PREMIS', 'METS', 'EAD', 'BagIt', 'Dublin Core', 'MODS', 'ISAD(G)'];

  const workflow = [
    {
      step: '01',
      stage: 'Intake',
      body: 'Assemble and validate submissions, with metadata captured at the door.',
      href: '/ingest',
      tone: 'ingest'
    },
    {
      step: '02',
      stage: 'Preservation',
      body: 'Seal records for the long term with provenance and fixity checks.',
      href: '/preserve',
      tone: 'preserve'
    },
    {
      step: '03',
      stage: 'Release',
      body: 'Produce access packages tailored to each audience.',
      href: '/deliver',
      tone: 'deliver'
    }
  ];

  const benefits = [
    {
      title: 'Ten standards, zero lock-in',
      body: 'Describe and package against ten open archival standards, mixed per collection. Change format without changing platform.'
    },
    {
      title: 'Compliance-grade conformance',
      body: 'Validated, regulator-ready NOARK 5.5 and E-ARK packages that pass conformance out of the box.'
    },
    {
      title: 'Secure by design',
      body: 'Short-lived signed sessions, rotating refresh tokens, and every request scoped to its tenant.'
    }
  ];

  const tiers = [
    {
      name: 'Free',
      price: '$0',
      cadence: 'forever',
      features: ['5 members · 10 archives', '100 MB storage', 'All 10 open standards'],
      cta: 'Start free',
      popular: false
    },
    {
      name: 'Basic',
      price: '$0',
      cadence: '+ usage',
      features: ['25 members · 100 archives', '1 GB storage', 'Pay-as-you-go premium packages'],
      cta: 'Choose Basic',
      popular: false
    },
    {
      name: 'Professional',
      price: '$249',
      cadence: '/month',
      features: ['100 members · 1,000 archives', '10 GB storage', '100 NOARK 5.5 / E-ARK packages a month'],
      cta: 'Choose Professional',
      popular: true
    },
    {
      name: 'Enterprise',
      price: '$1,499',
      cadence: '/month',
      features: ['Unlimited members & archives', 'Unlimited storage & conformance', 'Audit logging, SSO-ready'],
      cta: 'Contact sales',
      popular: false
    }
  ];
</script>

<svelte:head>
  <title>Arcana — Digital Preservation, Built on Standards</title>
  <meta
    name="description"
    content="Arcana is a digital preservation platform built on ten open archival standards, with compliance-grade NOARK 5.5 and E-ARK conformance. Ingest, preserve, and deliver — start free."
  />
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous" />
  <link
    href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600;700&display=swap"
    rel="stylesheet"
  />
</svelte:head>

{#if !currentRole}
  <!-- ═══════════════════════════════════════ -->
  <!--  PUBLIC LANDING PAGE                    -->
  <!-- ═══════════════════════════════════════ -->
  <div class="landing">
    <!-- HERO -->
    <section class="bleed hero">
      <div class="container hero-inner">
        <span class="eyebrow">Open-standard digital preservation</span>
        <h1>
          Preserve anything.<br />
          <span class="grad">Prove everything.</span>
        </h1>
        <p class="lede">
          Arcana takes your records from submission to access on the standards regulators trust,
          with NOARK&nbsp;5.5 and E-ARK conformance built in.
        </p>
        <div class="actions">
          <a href="/register" class="btn btn-primary">Start free</a>
          <a href="#how" class="btn btn-ghost">See how it works</a>
        </div>
        <p class="hero-note">No credit card · Free forever tier</p>

        <div class="hero-standards">
          <span class="hero-standards-label">Ten open standards, built in</span>
          <ul>
            {#each standards as std}<li>{std}</li>{/each}
          </ul>
        </div>
      </div>
    </section>

    <!-- HOW IT WORKS -->
    <section id="how" class="container section">
      <div class="section-head">
        <span class="eyebrow eyebrow-dark">How it works</span>
        <h2>Submission to access, in three steps</h2>
      </div>
      <div class="panel steps">
        {#each workflow as w}
          <a href={w.href} class="step {w.tone}">
            <span class="step-num">{w.step}</span>
            <h3>{w.stage}</h3>
            <p>{w.body}</p>
            <span class="more">Learn more →</span>
          </a>
        {/each}
      </div>
    </section>

    <!-- WHY ARCANA -->
    <section class="container section">
      <div class="section-head">
        <span class="eyebrow eyebrow-dark">Why Arcana</span>
        <h2>What a trustworthy archive needs</h2>
      </div>
      <div class="benefits">
        {#each benefits as b}
          <div class="benefit">
            <h3>{b.title}</h3>
            <p>{b.body}</p>
          </div>
        {/each}
      </div>
      <p class="section-foot"><a href="/product">Explore the product →</a></p>
    </section>

    <!-- PRICING -->
    <section id="pricing" class="container section">
      <div class="section-head">
        <span class="eyebrow eyebrow-dark">Pricing</span>
        <h2>Start free, grow when you need to</h2>
      </div>
      <div class="panel tiers">
        {#each tiers as tier}
          <div class="tier" class:popular={tier.popular}>
            <h3>
              {tier.name}
              {#if tier.popular}<span class="tier-badge">Popular</span>{/if}
            </h3>
            <div class="tier-price">
              <span class="tier-amount">{tier.price}</span><span class="tier-cadence">{tier.cadence}</span>
            </div>
            <ul>
              {#each tier.features as f}<li>{f}</li>{/each}
            </ul>
            <a href="/register" class="tier-cta">{tier.cta}</a>
          </div>
        {/each}
      </div>
      <p class="section-foot">
        Beyond your plan: $0.18 per GB-month of storage, $0.40 per premium NOARK&nbsp;5.5 / E-ARK package.
        <a href="/register">Custom plans</a> are available.
      </p>
    </section>
  </div>

{:else}
  <!-- ═══════════════════════════════════════ -->
  <!--  LOGGED-IN HOME                         -->
  <!-- ═══════════════════════════════════════ -->
  <div class="home">
    <div class="welcome-banner">
      <div class="banner-aurora" aria-hidden="true"></div>
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
        <a href="/intake" class="nav-card sips">
          <span class="nav-icon">📦</span>
          <h3>Intakes</h3>
          <p>Build Intake packages</p>
        </a>
        <a href="/preservation" class="nav-card aips">
          <span class="nav-icon">🏗️</span>
          <h3>Preservations</h3>
          <p>Manage Preservation packages</p>
        </a>
        <a href="/release" class="nav-card dips">
          <span class="nav-icon">📤</span>
          <h3>Releases</h3>
          <p>Create Release packages</p>
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
    font-family: var(--arc-font-body);
    color: var(--arc-ink);
    padding-bottom: 3rem;
  }

  /* The full-bleed hero spans 100vw; guard against the scrollbar-gutter
     overflowing the page horizontally while the landing is mounted. */
  :global(body) {
    overflow-x: hidden;
  }

  .landing h1,
  .landing h2,
  .landing h3,
  .landing .step-num,
  .landing .tier-amount {
    font-family: var(--arc-font-display);
    letter-spacing: -0.02em;
  }

  /* Full-bleed helper: break out of the centered 1200px <main> container. */
  .bleed {
    width: 100vw;
    margin-left: calc(50% - 50vw);
    margin-right: calc(50% - 50vw);
  }

  .container {
    max-width: 1040px;
    margin: 0 auto;
    padding: 0 1.5rem;
  }

  .eyebrow {
    display: inline-block;
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: var(--arc-panel-accent);
    margin-bottom: 1rem;
  }

  .eyebrow-dark {
    color: var(--arc-eyebrow-ink);
  }

  /* ── Hero ── */
  .hero {
    background:
      radial-gradient(60% 55% at 50% 0%, rgba(99, 102, 241, calc(0.32 * var(--arc-panel-glow))), transparent 70%),
      var(--arc-panel-bg);
    color: var(--arc-panel-body);
    margin-top: -2rem; /* cancel <main> top padding so it meets the nav */
    padding: 6.5rem 0 4.5rem;
    text-align: center;
  }

  .hero h1 {
    font-size: clamp(2.6rem, 6vw, 4.25rem);
    font-weight: 700;
    line-height: 1.05;
    margin: 0 0 1.5rem;
    color: var(--arc-panel-ink);
  }

  .grad {
    background: var(--arc-grad-text);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .lede {
    font-size: 1.18rem;
    line-height: 1.65;
    color: var(--arc-panel-body);
    max-width: 35rem;
    margin: 0 auto 2.25rem;
  }

  .actions {
    display: flex;
    justify-content: center;
    gap: 0.85rem;
    flex-wrap: wrap;
  }

  .btn {
    display: inline-block;
    padding: 0.85rem 1.75rem;
    border-radius: 0.65rem;
    font-weight: 600;
    font-size: 1rem;
    text-decoration: none;
    transition: background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
  }

  .btn-primary {
    background: var(--arc-grad-brand);
    color: white;
    box-shadow: 0 8px 24px -10px rgba(124, 58, 237, 0.7);
  }

  .btn-primary:hover {
    background: var(--arc-grad-brand-hover);
  }

  .btn-ghost {
    color: var(--arc-panel-ink);
    border: 1px solid var(--arc-ghost-border);
  }

  .btn-ghost:hover {
    background: var(--arc-tint-strong);
    border-color: var(--arc-ghost-border-strong);
  }

  .hero-note {
    margin: 1.25rem 0 0;
    font-size: 0.88rem;
    color: var(--arc-panel-muted);
  }

  .hero-standards {
    margin-top: 4.5rem;
    padding-top: 2rem;
    border-top: 1px solid var(--arc-panel-line);
  }

  .hero-standards-label {
    display: block;
    font-size: 0.72rem;
    font-weight: 600;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #64748b;
    margin-bottom: 0.9rem;
  }

  .hero-standards ul {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.5rem 2rem;
    font-size: 0.95rem;
    font-weight: 500;
    color: var(--arc-panel-muted);
  }

  /* ── Sections ── */
  .section {
    padding-top: 6rem;
  }

  .section-head {
    text-align: center;
    margin: 0 auto 2.75rem;
  }

  .section-head .eyebrow {
    margin-bottom: 0.75rem;
  }

  .section-head h2 {
    font-size: clamp(1.7rem, 3.2vw, 2.3rem);
    font-weight: 700;
    color: var(--arc-ink);
    margin: 0;
  }

  .section-foot {
    text-align: center;
    margin: 2rem auto 0;
    max-width: 44rem;
    font-size: 0.9rem;
    color: var(--arc-muted);
  }

  .section-foot a,
  .more {
    color: var(--arc-link);
    font-weight: 600;
    text-decoration: none;
  }

  .section-foot a:hover {
    text-decoration: underline;
  }

  /* One bordered surface split into cells by hairlines: the 1px grid gap
     shows the panel's line-colored background between the cells. */
  .panel {
    display: grid;
    gap: 1px;
    background: var(--arc-line-strong);
    border: 1px solid var(--arc-line-strong);
    border-radius: 1rem;
    overflow: hidden;
  }

  .panel > * {
    background: var(--arc-card);
  }

  /* ── How it works ── */
  .steps {
    grid-template-columns: repeat(3, 1fr);
  }

  .step {
    display: flex;
    flex-direction: column;
    padding: 2rem 1.75rem;
    text-decoration: none;
    color: inherit;
    transition: background 0.18s ease;
  }

  .step:hover {
    background: var(--arc-card-2);
  }

  .step-num {
    font-size: 0.95rem;
    font-weight: 700;
    margin-bottom: 1.25rem;
  }

  .step.ingest .step-num { color: #3b82f6; }
  .step.preserve .step-num { color: #10b981; }
  .step.deliver .step-num { color: #8b5cf6; }

  .step h3,
  .benefit h3 {
    font-size: 1.2rem;
    color: var(--arc-ink);
    margin: 0 0 0.5rem;
  }

  .step p,
  .benefit p {
    font-size: 0.95rem;
    color: var(--arc-muted);
    line-height: 1.6;
    margin: 0;
  }

  .step p {
    flex: 1;
    margin-bottom: 1.25rem;
  }

  .more {
    font-size: 0.875rem;
  }

  /* ── Why Arcana ── */
  .benefits {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 2.5rem;
  }

  .benefit {
    padding-top: 1.25rem;
    border-top: 2px solid var(--arc-line-strong);
  }

  /* ── Pricing ── */
  .tiers {
    grid-template-columns: repeat(4, 1fr);
  }

  .tier {
    display: flex;
    flex-direction: column;
    padding: 1.75rem 1.5rem;
  }

  .tier h3 {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin: 0 0 0.6rem;
    font-size: 1rem;
    font-weight: 600;
    color: var(--arc-ink);
  }

  .tier-badge {
    font-family: var(--arc-font-body);
    font-size: 0.65rem;
    font-weight: 700;
    letter-spacing: 0.04em;
    text-transform: uppercase;
    color: var(--arc-chip-violet-ink);
    background: var(--arc-chip-violet-bg);
    border-radius: 1rem;
    padding: 0.15rem 0.55rem;
  }

  .tier-price {
    display: flex;
    align-items: baseline;
    gap: 0.4rem;
    margin-bottom: 1.25rem;
  }

  .tier-amount {
    font-size: 2rem;
    font-weight: 700;
    line-height: 1.1;
    color: var(--arc-ink);
  }

  .tier-cadence {
    font-size: 0.8rem;
    color: var(--arc-muted);
  }

  .tier ul {
    list-style: none;
    padding: 0;
    margin: 0 0 1.75rem;
    flex: 1;
    display: grid;
    gap: 0.5rem;
    align-content: start;
    font-size: 0.875rem;
    line-height: 1.45;
    color: var(--arc-body);
  }

  .tier-cta {
    display: block;
    text-align: center;
    padding: 0.65rem 1rem;
    border-radius: 0.6rem;
    text-decoration: none;
    font-weight: 600;
    font-size: 0.9rem;
    border: 1px solid var(--arc-line-strong);
    color: var(--arc-ink);
    transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
  }

  .tier-cta:hover {
    border-color: var(--arc-indigo);
    color: var(--arc-link);
  }

  .tier.popular .tier-cta {
    background: var(--arc-grad-brand);
    color: white;
    border-color: transparent;
  }

  .tier.popular .tier-cta:hover {
    background: var(--arc-grad-brand-hover);
    color: white;
  }

  /* ── Responsive ── */
  @media (max-width: 860px) {
    .steps,
    .benefits {
      grid-template-columns: 1fr;
    }
    .benefits {
      gap: 2rem;
    }
    .tiers {
      grid-template-columns: repeat(2, 1fr);
    }
    .section {
      padding-top: 4.5rem;
    }
  }

  @media (max-width: 560px) {
    .tiers {
      grid-template-columns: 1fr;
    }
    .hero {
      padding: 4rem 0 3rem;
    }
    .hero-standards {
      margin-top: 3rem;
    }
  }

  /* ══════════════════════════════════════════
     LOGGED-IN HOME
     ══════════════════════════════════════════ */
  .home {
    max-width: 1000px;
    margin: 0 auto;
  }

  .welcome-banner {
    position: relative;
    overflow: hidden;
    background: var(--arc-panel-bg);
    color: var(--arc-panel-body);
    padding: 2.75rem 2.5rem;
    border-radius: 1rem;
    border: 1px solid var(--arc-panel-line);
    margin-bottom: 2.5rem;
  }

  .banner-aurora {
    position: absolute;
    inset: -40% -10% auto -10%;
    height: 95%;
    background:
      radial-gradient(40% 60% at 18% 30%, rgba(99, 102, 241, 0.45), transparent 70%),
      radial-gradient(40% 60% at 80% 20%, rgba(139, 92, 246, 0.4), transparent 70%),
      radial-gradient(35% 50% at 60% 65%, rgba(6, 182, 212, 0.3), transparent 70%);
    filter: blur(30px);
    opacity: var(--arc-panel-glow);
    pointer-events: none;
  }

  .welcome-text {
    position: relative;
  }

  .welcome-text h1 {
    margin: 0 0 0.35rem;
    font-size: 1.9rem;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: var(--arc-panel-ink);
  }

  .welcome-text p {
    margin: 0;
    color: var(--arc-panel-body);
    font-size: 1rem;
  }

  .nav-heading {
    font-size: 1.1rem;
    font-weight: 600;
    color: var(--arc-muted);
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
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    padding: 1.5rem;
    text-decoration: none;
    color: inherit;
    box-shadow: var(--arc-shadow-card);
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .nav-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border);
    box-shadow: var(--arc-shadow-lift);
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
    letter-spacing: 0.14em;
    color: var(--arc-eyebrow-ink);
    margin-bottom: 0.75rem;
  }

  .nav-card h3 {
    margin: 0 0 0.35rem;
    font-size: 1.05rem;
    color: var(--arc-ink);
  }

  .nav-card p {
    margin: 0;
    font-size: 0.85rem;
    color: var(--arc-muted);
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
</style>
