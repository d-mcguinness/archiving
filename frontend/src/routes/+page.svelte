<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import RoleGate from '$lib/components/RoleGate.svelte';

  $: currentRole = $auth.role;
  $: currentUser = $auth.user;

  const standards = ['NOARK5', 'OAIS', 'PREMIS', 'Dublin Core', 'METS', 'EAD', 'BagIt', 'ISAD(G)', 'MODS', 'E-ARK'];
  // Compliance-grade standards that generate validated, regulator-ready packages.
  const premiumStandards = ['NOARK5', 'E-ARK'];

  const features = [
    {
      icon: '◆',
      title: 'Ten standards, zero lock-in',
      body: 'Ingest, describe, and package against ten open archival standards. Mix and match them per collection — never re-platform to change format.'
    },
    {
      icon: '✦',
      title: 'Compliance-grade conformance',
      body: 'Emit validated, regulator-ready NOARK 5.5 and E-ARK packages that pass conformance out of the box — not an export afterthought.'
    },
    {
      icon: '⟳',
      title: 'The complete OAIS workflow',
      body: 'Carry every asset cleanly from Intake to Preservation to Release, with provenance, fixity, and integrity tracked at every step.'
    },
    {
      icon: '⛉',
      title: 'Secure by design',
      body: 'Signed, short-lived sessions with single-use refresh rotation, per-tenant isolation, and brute-force throttling baked in.'
    },
    {
      icon: '◧',
      title: 'Multi-tenant from day one',
      body: 'Every archive, user, and document is scoped to its organization — clean separation for internal teams or external clients.'
    },
    {
      icon: '⟠',
      title: 'Pay for what you preserve',
      body: 'Begin free, then grow with transparent usage-based pricing for storage and premium conformance. No surprise invoices.'
    }
  ];

  const workflow = [
    {
      stage: 'Intake',
      step: '01',
      kicker: 'Ingest',
      title: 'Submission packages',
      body: 'Assemble and validate Intake packages across all ten standards, with metadata captured at the door.',
      href: '/ingest',
      tone: 'ingest'
    },
    {
      stage: 'Preservation',
      step: '02',
      kicker: 'Preserve',
      title: 'Long-term storage',
      body: 'Seal Preservation packages with full provenance, fixity checks, and format-migration readiness.',
      href: '/preserve',
      tone: 'preserve'
    },
    {
      stage: 'Release',
      step: '03',
      kicker: 'Deliver',
      title: 'Access packages',
      body: 'Produce Release packages tailored to each audience, with standard-compliant metadata and access control.',
      href: '/deliver',
      tone: 'deliver'
    }
  ];

  const security = [
    'HMAC-signed, short-lived access tokens',
    'Single-use refresh rotation with reuse detection',
    'Per-tenant data isolation on every request',
    'Fail-closed storage quotas & usage metering',
    'Login, signup & refresh rate limiting'
  ];

  const tiers = [
    {
      name: 'Free',
      price: '$0',
      cadence: 'forever',
      tagline: 'Explore the full archival workflow',
      features: ['5 team members', '10 archives', '100 MB storage', 'All 10 open standards', 'Intake → Preservation → Release workflow'],
      cta: 'Start free',
      popular: false
    },
    {
      name: 'Basic',
      price: '$0',
      cadence: '+ usage',
      tagline: 'For small teams getting started',
      features: ['25 team members', '100 archives', '1 GB storage', 'Pay-as-you-go premium packages', 'Usage-based scaling'],
      cta: 'Choose Basic',
      popular: false
    },
    {
      name: 'Professional',
      price: 'Scale',
      cadence: 'compliance',
      tagline: 'Compliance at scale',
      features: ['100 team members', '1,000 archives', '10 GB storage', '100 NOARK 5.5 / E-ARK packages / mo', 'Priority support'],
      cta: 'Choose Professional',
      popular: true
    },
    {
      name: 'Enterprise',
      price: 'Custom',
      cadence: 'unlimited',
      tagline: 'Unlimited preservation',
      features: ['Unlimited members & archives', 'Unlimited storage', 'Unlimited premium conformance', 'Audit logging & SSO-ready', 'Dedicated support'],
      cta: 'Contact sales',
      popular: false
    }
  ];

  // Lightweight scroll-reveal: fade/slide elements in as they enter the viewport.
  function reveal(node: HTMLElement, delay = 0) {
    node.style.transitionDelay = `${delay}ms`;
    node.classList.add('reveal-init');
    if (typeof IntersectionObserver === 'undefined') {
      node.classList.add('reveal-in');
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (e.isIntersecting) {
            node.classList.add('reveal-in');
            io.unobserve(node);
          }
        }
      },
      { threshold: 0.12 }
    );
    io.observe(node);
    return { destroy: () => io.disconnect() };
  }
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
      <div class="hero-aurora" aria-hidden="true"></div>
      <div class="hero-grid-bg" aria-hidden="true"></div>
      <div class="container hero-inner">
        <div class="hero-copy" use:reveal>
          <span class="eyebrow">Open-standard digital preservation</span>
          <h1>
            Preserve anything.<br />
            <span class="grad">Prove everything.</span>
          </h1>
          <p class="lede">
            Arcana takes your digital records from submission to access on the standards regulators
            trust — with validated <strong>NOARK&nbsp;5.5</strong> and <strong>E-ARK</strong> conformance
            built in, not bolted on.
          </p>
          <div class="hero-actions">
            <a href="/register" class="btn btn-primary">Start free</a>
            <a href="#how" class="btn btn-ghost">See how it works</a>
          </div>
          <p class="hero-note">No credit card · Free forever tier · <a href="#pricing">Compare plans →</a></p>
        </div>

        <div class="hero-visual" use:reveal={120}>
          <div class="pipeline-card">
            <div class="pc-bar">
              <span class="pc-dot"></span><span class="pc-dot"></span><span class="pc-dot"></span>
              <span class="pc-bar-title">preservation pipeline</span>
            </div>
            <div class="pc-flow">
              <div class="pc-node n-ingest">
                <span class="pc-stage">Intake</span>
                <span class="pc-kicker">Ingest</span>
                <span class="pc-state">validated</span>
              </div>
              <div class="pc-conn"><span class="pc-pulse"></span></div>
              <div class="pc-node n-preserve">
                <span class="pc-stage">Preservation</span>
                <span class="pc-kicker">Preserve</span>
                <span class="pc-state">sealed</span>
              </div>
              <div class="pc-conn"><span class="pc-pulse" style="animation-delay:.8s"></span></div>
              <div class="pc-node n-deliver">
                <span class="pc-stage">Release</span>
                <span class="pc-kicker">Deliver</span>
                <span class="pc-state">ready</span>
              </div>
            </div>
            <div class="pc-foot">
              <span class="pc-tag">NOARK 5.5</span>
              <span class="pc-tag">E-ARK</span>
              <span class="pc-tag">OAIS</span>
              <span class="pc-conform">● conformant</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- TRUST BAR -->
    <section class="trustbar container" use:reveal>
      <span class="trustbar-label">Standards in the box</span>
      <div class="trustbar-marquee">
        <div class="trustbar-track">
          {#each [...standards, ...standards] as std}
            <span class="trustbar-item" class:premium={premiumStandards.includes(std)}>{std}</span>
          {/each}
        </div>
      </div>
    </section>

    <!-- STATS -->
    <section class="container stats" use:reveal>
      <div class="stat"><span class="stat-num">10</span><span class="stat-label">Archival standards</span></div>
      <div class="stat"><span class="stat-num">3</span><span class="stat-label">OAIS stages</span></div>
      <div class="stat"><span class="stat-num">2</span><span class="stat-label">Compliance-grade formats</span></div>
      <div class="stat"><span class="stat-num">$0</span><span class="stat-label">To get started</span></div>
    </section>

    <!-- FEATURES -->
    <section class="container section">
      <div class="section-head" use:reveal>
        <span class="eyebrow eyebrow-dark">Why Arcana</span>
        <h2>Everything a trustworthy archive needs</h2>
        <p class="section-sub">Built on the OAIS reference model and a decade of archival standards — engineered for the long term.</p>
      </div>
      <div class="feature-grid">
        {#each features as f, i}
          <div class="feature-card" use:reveal={i * 60}>
            <span class="feature-icon">{f.icon}</span>
            <h3>{f.title}</h3>
            <p>{f.body}</p>
          </div>
        {/each}
      </div>
    </section>

    <!-- HOW IT WORKS -->
    <section id="how" class="container section">
      <div class="section-head" use:reveal>
        <span class="eyebrow eyebrow-dark">The workflow</span>
        <h2>Submission to access, the OAIS way</h2>
        <p class="section-sub">Every record follows the Open Archival Information System reference model — nothing skipped, nothing lost.</p>
      </div>
      <div class="flow">
        {#each workflow as w, i}
          <a href={w.href} class="flow-card {w.tone}" use:reveal={i * 90}>
            <div class="flow-top">
              <span class="flow-stage">{w.stage}</span>
              <span class="flow-step">{w.step}</span>
            </div>
            <span class="flow-kicker">{w.kicker}</span>
            <h3>{w.title}</h3>
            <p>{w.body}</p>
            <span class="flow-link">Explore {w.kicker} →</span>
          </a>
        {/each}
      </div>
    </section>

    <!-- STANDARDS -->
    <section class="container section">
      <div class="section-head" use:reveal>
        <span class="eyebrow eyebrow-dark">Standards</span>
        <h2>Built on standards you trust</h2>
        <p class="section-sub">Ten archival standards out of the box — including compliance-grade <strong>NOARK&nbsp;5.5</strong> and <strong>E-ARK</strong> for regulated archives.</p>
      </div>
      <div class="chips" use:reveal>
        {#each standards as std}
          <span class="chip" class:premium={premiumStandards.includes(std)}>
            {std}{#if premiumStandards.includes(std)}<span class="star">★</span>{/if}
          </span>
        {/each}
      </div>
      <p class="chips-legend"><span class="star">★</span> Premium conformance — validated, regulator-ready NOARK&nbsp;5.5 / E-ARK packages</p>
    </section>

    <!-- SECURITY -->
    <section class="bleed security">
      <div class="container security-inner">
        <div class="security-copy" use:reveal>
          <span class="eyebrow">Trust &amp; security</span>
          <h2>Your archive, locked down by default</h2>
          <p>
            Preservation is a promise of integrity — so security isn't an add-on in Arcana, it's the
            foundation. Sessions are signed and short-lived, refresh tokens rotate on every use, and
            every request is scoped to its tenant.
          </p>
          <a href="/register" class="btn btn-primary">Create your secure archive</a>
        </div>
        <ul class="security-list" use:reveal={120}>
          {#each security as s}
            <li><span class="sec-check">✓</span>{s}</li>
          {/each}
        </ul>
      </div>
    </section>

    <!-- PRICING -->
    <section id="pricing" class="container section">
      <div class="section-head" use:reveal>
        <span class="eyebrow eyebrow-dark">Pricing</span>
        <h2>Plans that scale with your archive</h2>
        <p class="section-sub">Start free on open standards. Upgrade for compliance-grade conformance and room to grow.</p>
      </div>
      <div class="pricing-grid">
        {#each tiers as tier, i}
          <div class="tier" class:popular={tier.popular} use:reveal={i * 60}>
            {#if tier.popular}<div class="tier-badge">Most popular</div>{/if}
            <h3 class="tier-name">{tier.name}</h3>
            <div class="tier-price"><span class="tier-amount">{tier.price}</span><span class="tier-cadence">{tier.cadence}</span></div>
            <p class="tier-tagline">{tier.tagline}</p>
            <ul class="tier-features">
              {#each tier.features as f}<li>{f}</li>{/each}
            </ul>
            <a href="/register" class="tier-cta">{tier.cta}</a>
          </div>
        {/each}
      </div>
      <p class="pricing-usage">
        Usage-based add-ons: <strong>$0.18</strong> / GB-month for storage beyond your plan,
        <strong>$0.40</strong> per premium NOARK&nbsp;5.5 / E-ARK package.
      </p>
      <p class="pricing-custom">Need something bespoke? <a href="/register">Custom plans</a> with tailored limits and SLAs are available.</p>
    </section>

    <!-- FINAL CTA -->
    <section class="bleed cta">
      <div class="cta-aurora" aria-hidden="true"></div>
      <div class="container cta-inner" use:reveal>
        <h2>Start preserving in minutes</h2>
        <p>Spin up a free archive today — no credit card, no commitment. Upgrade when you need compliance-grade conformance.</p>
        <div class="hero-actions">
          <a href="/register" class="btn btn-light">Start free</a>
          <a href="/login" class="btn btn-ghost">Sign in</a>
        </div>
      </div>
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
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    color: #0f172a;
  }

  /* The full-bleed sections span 100vw; guard against the scrollbar-gutter
     overflowing the page horizontally while the landing is mounted. */
  :global(body) {
    overflow-x: hidden;
  }

  .landing h1,
  .landing h2,
  .landing h3,
  .landing .stat-num,
  .landing .tier-amount {
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    letter-spacing: -0.02em;
  }

  /* Full-bleed helper: break out of the centered 1200px <main> container. */
  .bleed {
    width: 100vw;
    margin-left: calc(50% - 50vw);
    margin-right: calc(50% - 50vw);
  }

  .container {
    max-width: 1100px;
    margin: 0 auto;
    padding: 0 1.5rem;
  }

  .eyebrow {
    display: inline-block;
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: #a5b4fc;
    margin-bottom: 1rem;
  }

  .eyebrow-dark {
    color: #7c3aed;
  }

  /* ── Hero ── */
  .hero {
    position: relative;
    overflow: hidden;
    background: radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%);
    color: #e2e8f0;
    margin-top: -2rem; /* cancel <main> top padding so it meets the nav */
    padding: 5rem 0 6rem;
  }

  .hero-aurora {
    position: absolute;
    inset: -30% -10% auto -10%;
    height: 80%;
    background:
      radial-gradient(40% 60% at 20% 30%, rgba(99, 102, 241, 0.45), transparent 70%),
      radial-gradient(40% 60% at 80% 20%, rgba(139, 92, 246, 0.4), transparent 70%),
      radial-gradient(35% 50% at 60% 60%, rgba(6, 182, 212, 0.3), transparent 70%);
    filter: blur(30px);
    pointer-events: none;
  }

  .hero-grid-bg {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(148, 163, 184, 0.07) 1px, transparent 1px),
      linear-gradient(90deg, rgba(148, 163, 184, 0.07) 1px, transparent 1px);
    background-size: 44px 44px;
    mask-image: radial-gradient(circle at 50% 30%, black, transparent 75%);
    pointer-events: none;
  }

  .hero-inner {
    position: relative;
    display: grid;
    grid-template-columns: 1.05fr 0.95fr;
    gap: 3rem;
    align-items: center;
  }

  .hero-copy h1 {
    font-size: clamp(2.6rem, 5vw, 4rem);
    font-weight: 700;
    line-height: 1.05;
    margin: 0 0 1.4rem;
    color: #f8fafc;
  }

  .grad {
    background: linear-gradient(110deg, #818cf8 0%, #c084fc 45%, #22d3ee 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .lede {
    font-size: 1.18rem;
    line-height: 1.7;
    color: #cbd5e1;
    max-width: 36rem;
    margin: 0 0 2.2rem;
  }

  .lede strong {
    color: #f1f5f9;
    font-weight: 600;
  }

  .hero-actions {
    display: flex;
    gap: 1rem;
    flex-wrap: wrap;
  }

  .btn {
    display: inline-block;
    padding: 0.9rem 1.9rem;
    border-radius: 0.65rem;
    font-weight: 700;
    font-size: 1rem;
    text-decoration: none;
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, border-color 0.18s ease;
    cursor: pointer;
  }

  .btn-primary {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6);
  }

  .btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 40px -8px rgba(124, 58, 237, 0.75);
  }

  .btn-ghost {
    background: rgba(255, 255, 255, 0.06);
    color: #e2e8f0;
    border: 1px solid rgba(148, 163, 184, 0.4);
  }

  .btn-ghost:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(226, 232, 240, 0.7);
    transform: translateY(-2px);
  }

  .btn-light {
    background: white;
    color: #1e293b;
    box-shadow: 0 10px 30px -8px rgba(0, 0, 0, 0.4);
  }

  .btn-light:hover {
    transform: translateY(-2px);
    box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.5);
  }

  .hero-note {
    margin: 1.4rem 0 0;
    font-size: 0.88rem;
    color: #94a3b8;
  }

  .hero-note a {
    color: #c4b5fd;
    font-weight: 600;
    text-decoration: none;
  }

  .hero-note a:hover {
    text-decoration: underline;
  }

  /* ── Hero pipeline visual ── */
  .hero-visual {
    perspective: 1200px;
  }

  .pipeline-card {
    background: linear-gradient(160deg, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.92));
    border: 1px solid rgba(148, 163, 184, 0.22);
    border-radius: 1.1rem;
    padding: 1.1rem 1.1rem 1.25rem;
    box-shadow: 0 30px 60px -20px rgba(0, 0, 0, 0.7);
    backdrop-filter: blur(8px);
    transform: rotateY(-8deg) rotateX(4deg);
    transition: transform 0.4s ease;
  }

  .hero-visual:hover .pipeline-card {
    transform: rotateY(0deg) rotateX(0deg);
  }

  .pc-bar {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    padding-bottom: 0.9rem;
    border-bottom: 1px solid rgba(148, 163, 184, 0.18);
    margin-bottom: 1.1rem;
  }

  .pc-dot {
    width: 0.6rem;
    height: 0.6rem;
    border-radius: 50%;
    background: #475569;
  }

  .pc-dot:nth-child(1) { background: #f87171; }
  .pc-dot:nth-child(2) { background: #fbbf24; }
  .pc-dot:nth-child(3) { background: #34d399; }

  .pc-bar-title {
    margin-left: 0.6rem;
    font-size: 0.72rem;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: #64748b;
    font-weight: 600;
  }

  .pc-flow {
    display: flex;
    align-items: stretch;
    gap: 0.3rem;
  }

  .pc-node {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.25rem;
    padding: 1rem 0.4rem;
    border-radius: 0.75rem;
    background: rgba(148, 163, 184, 0.06);
    border: 1px solid rgba(148, 163, 184, 0.16);
  }

  .pc-stage {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 1.35rem;
    font-weight: 700;
    color: #f8fafc;
  }

  .n-ingest .pc-stage { color: #60a5fa; }
  .n-preserve .pc-stage { color: #34d399; }
  .n-deliver .pc-stage { color: #c084fc; }

  .pc-kicker {
    font-size: 0.72rem;
    color: #94a3b8;
    font-weight: 500;
  }

  .pc-state {
    margin-top: 0.3rem;
    font-size: 0.62rem;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #34d399;
    background: rgba(52, 211, 153, 0.12);
    border-radius: 1rem;
    padding: 0.15rem 0.55rem;
    font-weight: 700;
  }

  .pc-conn {
    align-self: center;
    width: 1.4rem;
    height: 2px;
    background: rgba(148, 163, 184, 0.3);
    position: relative;
    overflow: hidden;
    border-radius: 2px;
  }

  .pc-pulse {
    position: absolute;
    top: 0;
    left: -40%;
    width: 40%;
    height: 100%;
    background: linear-gradient(90deg, transparent, #818cf8, transparent);
    animation: flow 1.8s linear infinite;
  }

  @keyframes flow {
    to { left: 110%; }
  }

  .pc-foot {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin-top: 1.1rem;
    padding-top: 0.9rem;
    border-top: 1px solid rgba(148, 163, 184, 0.18);
    flex-wrap: wrap;
  }

  .pc-tag {
    font-size: 0.66rem;
    font-weight: 700;
    color: #c4b5fd;
    background: rgba(139, 92, 246, 0.14);
    border: 1px solid rgba(139, 92, 246, 0.3);
    border-radius: 1rem;
    padding: 0.2rem 0.6rem;
  }

  .pc-conform {
    margin-left: auto;
    font-size: 0.7rem;
    font-weight: 700;
    color: #34d399;
  }

  /* ── Trust bar ── */
  .trustbar {
    margin: 3rem auto 0;
    display: flex;
    align-items: center;
    gap: 1.5rem;
  }

  .trustbar-label {
    flex-shrink: 0;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: #94a3b8;
  }

  .trustbar-marquee {
    position: relative;
    overflow: hidden;
    flex: 1;
    mask-image: linear-gradient(90deg, transparent, black 6%, black 94%, transparent);
  }

  .trustbar-track {
    display: flex;
    gap: 0.75rem;
    width: max-content;
    animation: marquee 34s linear infinite;
  }

  .trustbar:hover .trustbar-track {
    animation-play-state: paused;
  }

  @keyframes marquee {
    to { transform: translateX(-50%); }
  }

  .trustbar-item {
    flex-shrink: 0;
    font-size: 0.82rem;
    font-weight: 600;
    color: #475569;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    padding: 0.45rem 0.9rem;
  }

  .trustbar-item.premium {
    color: #6d28d9;
    border-color: #ddd6fe;
    background: #faf5ff;
  }

  /* ── Stats ── */
  .stats {
    margin: 3.5rem auto 0;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1rem;
    text-align: center;
  }

  .stat {
    padding: 1.5rem 1rem;
    background: white;
    border: 1px solid #e8edf3;
    border-radius: 1rem;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }

  .stat-num {
    display: block;
    font-size: 2.4rem;
    font-weight: 700;
    background: linear-gradient(135deg, #4f46e5, #8b5cf6);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    line-height: 1;
  }

  .stat-label {
    display: block;
    margin-top: 0.55rem;
    font-size: 0.82rem;
    color: #64748b;
    font-weight: 500;
  }

  /* ── Sections ── */
  .section {
    padding: 5rem 1.5rem 0;
  }

  .section-head {
    text-align: center;
    max-width: 40rem;
    margin: 0 auto 3rem;
  }

  .section-head h2 {
    font-size: clamp(1.8rem, 3.2vw, 2.5rem);
    font-weight: 700;
    color: #0f172a;
    margin: 0 0 0.85rem;
  }

  .section-sub {
    font-size: 1.05rem;
    color: #64748b;
    line-height: 1.6;
    margin: 0;
  }

  .section-sub strong {
    color: #334155;
  }

  /* ── Features ── */
  .feature-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1.25rem;
  }

  .feature-card {
    background: white;
    border: 1px solid #e8edf3;
    border-radius: 1rem;
    padding: 1.9rem 1.6rem;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .feature-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 18px 40px -16px rgba(79, 70, 229, 0.28);
    border-color: #c7d2fe;
  }

  .feature-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 3rem;
    height: 3rem;
    border-radius: 0.85rem;
    font-size: 1.4rem;
    color: white;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    margin-bottom: 1.1rem;
    box-shadow: 0 8px 20px -6px rgba(124, 58, 237, 0.5);
  }

  .feature-card h3 {
    font-size: 1.15rem;
    color: #0f172a;
    margin: 0 0 0.5rem;
  }

  .feature-card p {
    font-size: 0.92rem;
    color: #64748b;
    line-height: 1.6;
    margin: 0;
  }

  /* ── Workflow ── */
  .flow {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1.25rem;
  }

  .flow-card {
    position: relative;
    display: flex;
    flex-direction: column;
    background: white;
    border: 1px solid #e8edf3;
    border-radius: 1rem;
    padding: 1.9rem 1.6rem;
    text-decoration: none;
    color: inherit;
    overflow: hidden;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .flow-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
  }

  .flow-card.ingest::before { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
  .flow-card.preserve::before { background: linear-gradient(90deg, #10b981, #34d399); }
  .flow-card.deliver::before { background: linear-gradient(90deg, #8b5cf6, #c084fc); }

  .flow-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 18px 40px -16px rgba(15, 23, 42, 0.18);
  }

  .flow-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 0.9rem;
  }

  .flow-stage {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 1.5rem;
    font-weight: 700;
    color: #0f172a;
  }

  .flow-step {
    font-size: 0.8rem;
    font-weight: 700;
    color: #cbd5e1;
  }

  .flow-kicker {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #8b5cf6;
  }

  .flow-card h3 {
    font-size: 1.15rem;
    color: #0f172a;
    margin: 0.35rem 0 0.5rem;
  }

  .flow-card p {
    font-size: 0.92rem;
    color: #64748b;
    line-height: 1.6;
    margin: 0 0 1.1rem;
    flex: 1;
  }

  .flow-link {
    font-size: 0.85rem;
    font-weight: 700;
    color: #6366f1;
  }

  /* ── Chips ── */
  .chips {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.6rem;
    max-width: 42rem;
    margin: 0 auto;
  }

  .chip {
    padding: 0.5rem 1.1rem;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 2rem;
    font-size: 0.875rem;
    font-weight: 600;
    color: #334155;
    transition: border-color 0.2s, background 0.2s, transform 0.2s;
  }

  .chip:hover {
    border-color: #818cf8;
    background: #eef2ff;
    transform: translateY(-2px);
  }

  .chip.premium {
    border-color: #c4b5fd;
    background: linear-gradient(135deg, #faf5ff, #eef2ff);
    color: #6d28d9;
    font-weight: 700;
  }

  .star {
    color: #8b5cf6;
    margin-left: 0.3rem;
    font-size: 0.72rem;
    vertical-align: middle;
  }

  .chips-legend {
    text-align: center;
    margin: 1.4rem auto 0;
    font-size: 0.82rem;
    color: #64748b;
  }

  /* ── Security ── */
  .security {
    margin-top: 5rem;
    background: radial-gradient(120% 120% at 80% 0%, #1e1b4b 0%, #0f172a 60%, #0b1120 100%);
    color: #e2e8f0;
    padding: 5rem 0;
  }

  .security-inner {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 3rem;
    align-items: center;
  }

  .security-copy h2 {
    font-size: clamp(1.8rem, 3vw, 2.4rem);
    color: #f8fafc;
    margin: 0 0 1rem;
  }

  .security-copy p {
    color: #cbd5e1;
    font-size: 1.05rem;
    line-height: 1.7;
    margin: 0 0 2rem;
    max-width: 32rem;
  }

  .security-list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 0.75rem;
  }

  .security-list li {
    display: flex;
    align-items: center;
    gap: 0.85rem;
    background: rgba(148, 163, 184, 0.07);
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 0.75rem;
    padding: 0.9rem 1.1rem;
    font-size: 0.95rem;
    color: #e2e8f0;
    font-weight: 500;
  }

  .sec-check {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 1.5rem;
    height: 1.5rem;
    border-radius: 50%;
    background: linear-gradient(135deg, #34d399, #10b981);
    color: #052e16;
    font-size: 0.8rem;
    font-weight: 800;
  }

  /* ── Pricing ── */
  .pricing-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1.1rem;
    align-items: stretch;
  }

  .tier {
    position: relative;
    display: flex;
    flex-direction: column;
    background: white;
    border: 1px solid #e8edf3;
    border-radius: 1rem;
    padding: 1.9rem 1.5rem;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .tier:hover {
    transform: translateY(-4px);
    box-shadow: 0 18px 40px -16px rgba(15, 23, 42, 0.18);
  }

  .tier.popular {
    border-color: #8b5cf6;
    box-shadow: 0 18px 44px -18px rgba(139, 92, 246, 0.5);
  }

  .tier-badge {
    position: absolute;
    top: -0.75rem;
    left: 50%;
    transform: translateX(-50%);
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    font-size: 0.65rem;
    font-weight: 800;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    padding: 0.32rem 0.85rem;
    border-radius: 1rem;
    white-space: nowrap;
  }

  .tier-name {
    margin: 0 0 0.4rem;
    font-size: 1.15rem;
    font-weight: 700;
    color: #0f172a;
  }

  .tier-price {
    display: flex;
    align-items: baseline;
    gap: 0.4rem;
    margin-bottom: 0.9rem;
  }

  .tier-amount {
    font-size: 1.9rem;
    font-weight: 700;
    color: #0f172a;
  }

  .tier-cadence {
    font-size: 0.8rem;
    color: #94a3b8;
    font-weight: 600;
  }

  .tier-tagline {
    margin: 0 0 1.2rem;
    font-size: 0.85rem;
    color: #64748b;
    min-height: 2.4em;
  }

  .tier-features {
    list-style: none;
    padding: 0;
    margin: 0 0 1.6rem;
    flex: 1;
  }

  .tier-features li {
    position: relative;
    padding: 0.45rem 0 0.45rem 1.5rem;
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
    padding: 0.75rem 1rem;
    border-radius: 0.6rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.9rem;
    border: 1.5px solid #cbd5e1;
    color: #1e293b;
    transition: border-color 0.2s, background 0.2s, color 0.2s, box-shadow 0.2s;
  }

  .tier-cta:hover {
    border-color: #6366f1;
    color: #4f46e5;
  }

  .tier.popular .tier-cta {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    border-color: transparent;
  }

  .tier.popular .tier-cta:hover {
    box-shadow: 0 8px 22px -6px rgba(124, 58, 237, 0.6);
    color: white;
  }

  .pricing-usage {
    margin: 2rem auto 0;
    max-width: 40rem;
    text-align: center;
    font-size: 0.875rem;
    color: #475569;
    background: #f8fafc;
    border: 1px solid #e8edf3;
    border-radius: 0.7rem;
    padding: 0.9rem 1.25rem;
  }

  .pricing-usage strong {
    color: #0f172a;
  }

  .pricing-custom {
    text-align: center;
    margin: 1rem 0 0;
    font-size: 0.85rem;
    color: #64748b;
  }

  .pricing-custom a {
    color: #6366f1;
    font-weight: 600;
    text-decoration: none;
  }

  .pricing-custom a:hover {
    text-decoration: underline;
  }

  /* ── Final CTA ── */
  .cta {
    position: relative;
    overflow: hidden;
    margin-top: 5rem;
    margin-bottom: -2rem; /* meet the footer */
    background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #6d28d9 100%);
    color: white;
    padding: 5rem 0;
    text-align: center;
  }

  .cta-aurora {
    position: absolute;
    inset: 0;
    background:
      radial-gradient(40% 70% at 15% 20%, rgba(34, 211, 238, 0.35), transparent 70%),
      radial-gradient(40% 70% at 85% 80%, rgba(236, 72, 153, 0.3), transparent 70%);
    pointer-events: none;
  }

  .cta-inner {
    position: relative;
  }

  .cta h2 {
    font-size: clamp(1.9rem, 3.4vw, 2.7rem);
    margin: 0 0 0.85rem;
    color: white;
  }

  .cta p {
    margin: 0 auto 2.2rem;
    max-width: 34rem;
    font-size: 1.08rem;
    color: rgba(237, 233, 254, 0.95);
    line-height: 1.6;
  }

  .cta .hero-actions {
    justify-content: center;
  }

  /* ── Scroll reveal (classes added at runtime, so :global) ── */
  :global(.reveal-init) {
    opacity: 0;
    transform: translateY(26px);
    transition: opacity 0.7s ease, transform 0.7s ease;
    will-change: opacity, transform;
  }

  :global(.reveal-in) {
    opacity: 1;
    transform: none;
  }

  @media (prefers-reduced-motion: reduce) {
    :global(.reveal-init) {
      opacity: 1;
      transform: none;
      transition: none;
    }
    .pc-pulse,
    .trustbar-track {
      animation: none;
    }
  }

  /* ── Responsive ── */
  @media (max-width: 920px) {
    .hero-inner {
      grid-template-columns: 1fr;
      gap: 2.5rem;
    }
    .hero-visual {
      max-width: 28rem;
    }
    .pipeline-card {
      transform: none;
    }
    .feature-grid,
    .flow {
      grid-template-columns: repeat(2, 1fr);
    }
    .pricing-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .security-inner {
      grid-template-columns: 1fr;
      gap: 2rem;
    }
    .stats {
      grid-template-columns: repeat(2, 1fr);
    }
  }

  @media (max-width: 560px) {
    .feature-grid,
    .flow,
    .pricing-grid {
      grid-template-columns: 1fr;
    }
    .hero {
      padding: 3.5rem 0 4rem;
    }
    .trustbar {
      flex-direction: column;
      align-items: flex-start;
      gap: 0.75rem;
    }
    .trustbar-marquee {
      width: 100%;
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
</style>
