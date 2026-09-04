<script lang="ts">
  // Public marketing page — renders for everyone, no auth gating, no auth import.
  // Final synthesis: tri-column hero with the animated OAIS pipeline visual,
  // a Trustworthy/Compliant/Future-proof outcomes band, three full alternating
  // product feature sections, an at-a-glance comparison strip, the secure-by-design
  // proof section, the standards chips, and a final CTA.

  const standards = ['NOARK5', 'OAIS', 'PREMIS', 'Dublin Core', 'METS', 'EAD', 'BagIt', 'ISAD(G)', 'MODS', 'E-ARK'];
  // Compliance-grade standards that generate validated, regulator-ready packages.
  const premiumStandards = ['NOARK5', 'E-ARK'];

  // The three pillars, introduced together in the hero as clickable entry points.
  const pillars = [
    {
      tone: 'ingest',
      kicker: 'Ingest',
      stage: 'Intake',
      pkg: 'Intake package',
      abbr: 'SIP',
      href: '/ingest',
      blurb: 'Accept submissions from producers across ten standards.'
    },
    {
      tone: 'preserve',
      kicker: 'Preserve',
      stage: 'Preservation',
      pkg: 'Preservation package',
      abbr: 'AIP',
      href: '/preserve',
      blurb: 'Seal records for the long term with provenance & fixity.'
    },
    {
      tone: 'release',
      kicker: 'Release',
      stage: 'Release',
      pkg: 'Release package',
      abbr: 'DIP',
      href: '/deliver',
      blurb: 'Disseminate tailored access packages to every audience.'
    }
  ];

  // The promise, before the mechanism: outcomes shared across all three products.
  // Accents mirror the product band exactly — blue / indigo / orange.
  const outcomes = [
    {
      tone: 'ingest',
      icon: '⛉',
      title: 'Trustworthy',
      body: 'Provenance, fixity, and integrity tracked at every step — so years from now you can still prove exactly what you hold, and that it has not changed.'
    },
    {
      tone: 'preserve',
      icon: '✦',
      title: 'Compliant',
      body: 'Validated, regulator-ready NOARK 5.5 and E-ARK packages that pass conformance out of the box — audits become a formality, not a fire drill.'
    },
    {
      tone: 'release',
      icon: '⟳',
      title: 'Future-proof',
      body: 'Built on ten open standards with format-migration readiness, so your archive outlives the tools, formats, and vendors of today.'
    }
  ];

  // Full alternating feature sections. Capabilities are lifted from the deep-dive
  // pages, minus the two facts true of ALL three (10 standards, multi-tenant RBAC) —
  // those are carried once by the trustbar/standards and the security section.
  const products = [
    {
      tone: 'ingest',
      kicker: 'Ingest',
      stage: 'Intake',
      pkg: 'Intake package',
      abbr: 'SIP',
      headline: 'Accept submissions, not just files',
      lede:
        'The ingest workflow accepts Intake packages from producers and turns them into structured, standard-compliant records ready for preservation. Pick from all ten archival standards and capture metadata at the door — never re-key it later.',
      capabilities: [
        'Schema-driven forms with per-standard field definitions',
        'Document attachment and cloud storage linking',
        'Nested hierarchical element structures',
        'Standard-specific validation and defaults'
      ],
      note: 'Build to NOARK 5.5 and E-ARK conformance from the first submission — validation runs per standard, not as an afterthought.',
      state: 'validated',
      href: '/ingest',
      cta: 'Explore Ingest'
    },
    {
      tone: 'preserve',
      kicker: 'Preserve',
      stage: 'Preservation',
      pkg: 'Preservation package',
      abbr: 'AIP',
      headline: 'Storage that proves its own integrity',
      lede:
        'The preservation workflow seals Preservation packages for long-term storage with provenance tracking, integrity verification, and format-migration readiness. Every preservation action is captured, so the chain of custody is never in doubt.',
      capabilities: [
        'Provenance tracking and audit trails',
        'Integrity verification with fixity metadata',
        'Format migration and representation support',
        'Standard-specific preservation metadata'
      ],
      note: 'Fixity, provenance, and audit trails are first-class — the foundations of a trustworthy repository under the OAIS reference model.',
      state: 'sealed',
      href: '/preserve',
      cta: 'Explore Preserve'
    },
    {
      tone: 'release',
      kicker: 'Release',
      stage: 'Release',
      pkg: 'Release package',
      abbr: 'DIP',
      headline: 'Deliver to every audience',
      lede:
        'The release workflow produces Release packages for end-user access, with metadata and access controls tailored to each audience. Disseminate the same archived record many ways without ever touching the preserved original.',
      capabilities: [
        'User-tailored access packages',
        'Granular access controls and permissions',
        'Standard-compliant delivery metadata',
        'Multi-format export support'
      ],
      note: 'Release packages carry standard-compliant metadata, ready for download, integration, or distribution to external systems.',
      state: 'ready',
      href: '/deliver',
      cta: 'Explore Release'
    }
  ];

  // At-a-glance comparison strip summarizing the three.
  const glance = [
    {
      tone: 'ingest',
      kicker: 'Ingest',
      stage: 'Intake',
      pkg: 'Intake package',
      produces: 'Validated submissions',
      key: 'Schema-driven capture across 10 standards',
      href: '/ingest'
    },
    {
      tone: 'preserve',
      kicker: 'Preserve',
      stage: 'Preservation',
      pkg: 'Preservation package',
      produces: 'Sealed long-term records',
      key: 'Provenance, fixity & format migration',
      href: '/preserve'
    },
    {
      tone: 'release',
      kicker: 'Release',
      stage: 'Release',
      pkg: 'Release package',
      produces: 'Tailored access copies',
      key: 'Access control & multi-format export',
      href: '/deliver'
    }
  ];

  // Secure-by-design proof points — mirrors the canonical landing page.
  const security = [
    'HMAC-signed, short-lived access tokens',
    'Single-use refresh rotation with reuse detection',
    'Per-tenant data isolation on every request',
    'Multi-tenant, role-based access control',
    'Fail-closed storage quotas & usage metering',
    'Login, signup & refresh rate limiting'
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
  <title>Arcana Product — Ingest, Preserve & Release on Open Standards</title>
  <meta
    name="description"
    content="One platform, three products: Ingest submissions, Preserve them long-term, and Release tailored access packages — trustworthy, compliant, and future-proof. Built on ten open archival standards with NOARK 5.5 and E-ARK conformance. Start free."
  />
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous" />
  <link
    href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600;700&display=swap"
    rel="stylesheet"
  />
</svelte:head>

<div class="product">
  <!-- ═══════════════════════════════════════ -->
  <!--  HERO — three pillars + animated pipeline -->
  <!-- ═══════════════════════════════════════ -->
  <section class="bleed hero">
    <div class="hero-aurora" aria-hidden="true"></div>
    <div class="hero-grid-bg" aria-hidden="true"></div>
    <div class="container hero-inner">
      <div class="hero-copy" use:reveal>
        <span class="eyebrow">One platform · The complete OAIS workflow</span>
        <h1>
          Archives you can<br />
          <span class="grad">trust for decades.</span>
        </h1>
        <p class="lede">
          Arcana carries every record from submission to access on the standards regulators trust —
          <strong>Ingest</strong>, <strong>Preserve</strong>, and <strong>Release</strong>, with validated
          <strong>NOARK&nbsp;5.5</strong> and <strong>E-ARK</strong> conformance built in, not bolted on.
        </p>
        <div class="hero-actions">
          <a href="/register" class="btn btn-primary">Start free</a>
          <a href="/login" class="btn btn-ghost">Sign in</a>
        </div>
        <p class="hero-note">No credit card · Free forever tier · <a href="#glance">Compare the three →</a></p>
      </div>

      <!-- Animated OAIS pipeline — the canonical landing motif -->
      <div class="hero-visual" use:reveal={120}>
        <div class="pipeline-card">
          <div class="pc-bar">
            <span class="pc-dot" aria-hidden="true"></span>
            <span class="pc-dot" aria-hidden="true"></span>
            <span class="pc-dot" aria-hidden="true"></span>
            <span class="pc-bar-title">preservation pipeline</span>
          </div>
          <div class="pc-flow">
            <div class="pc-node n-ingest">
              <span class="pc-stage">Intake</span>
              <span class="pc-kicker">Ingest</span>
              <span class="pc-state">validated</span>
            </div>
            <div class="pc-conn" aria-hidden="true"><span class="pc-pulse"></span></div>
            <div class="pc-node n-preserve">
              <span class="pc-stage">Preservation</span>
              <span class="pc-kicker">Preserve</span>
              <span class="pc-state">sealed</span>
            </div>
            <div class="pc-conn" aria-hidden="true"><span class="pc-pulse" style="animation-delay:.8s"></span></div>
            <div class="pc-node n-release">
              <span class="pc-stage">Release</span>
              <span class="pc-kicker">Release</span>
              <span class="pc-state">ready</span>
            </div>
          </div>
          <div class="pc-foot">
            <span class="pc-tag">NOARK 5.5</span>
            <span class="pc-tag">E-ARK</span>
            <span class="pc-tag">OAIS</span>
            <span class="pc-conform"><span aria-hidden="true">●</span> conformant</span>
          </div>
        </div>
      </div>
    </div>

    <!-- The three pillars, side by side, each a deep-dive link -->
    <div class="container">
      <div class="pillars" use:reveal={180}>
        {#each pillars as p, i}
          <a href={p.href} class="pillar {p.tone}">
            <span class="pillar-step" aria-hidden="true">0{i + 1}</span>
            <span class="pillar-stage">{p.stage}</span>
            <span class="pillar-kicker">{p.kicker}</span>
            <span class="pillar-pkg">{p.pkg} <span class="pillar-abbr">{p.abbr}</span></span>
            <p class="pillar-blurb">{p.blurb}</p>
            <span class="pillar-link">Deep dive <span aria-hidden="true">→</span></span>
          </a>
          {#if i < pillars.length - 1}
            <span class="pillar-arrow" aria-hidden="true">→</span>
          {/if}
        {/each}
      </div>
    </div>
  </section>

  <!-- TRUST BAR -->
  <section class="trustbar container" use:reveal>
    <span class="trustbar-label">Standards in the box</span>
    <div class="trustbar-marquee" aria-hidden="true">
      <div class="trustbar-track">
        {#each [...standards, ...standards] as std}
          <span class="trustbar-item" class:premium={premiumStandards.includes(std)}>{std}</span>
        {/each}
      </div>
    </div>
  </section>

  <!-- ═══════════════════════════════════════ -->
  <!--  OUTCOMES — the promise, before the how -->
  <!-- ═══════════════════════════════════════ -->
  <section class="container section">
    <div class="section-head" use:reveal>
      <span class="eyebrow eyebrow-dark">What you get</span>
      <h2>Preservation is a promise. We help you keep it.</h2>
      <p class="section-sub">
        Every team that runs an archive answers to someone — a regulator, a court, a future colleague.
        Arcana is engineered so the answer is always the same: <strong>yes, it's intact, and here's the proof.</strong>
      </p>
    </div>
    <div class="outcome-grid">
      {#each outcomes as o, i}
        <div class="outcome-card o-{o.tone}" use:reveal={i * 70}>
          <span class="outcome-icon" aria-hidden="true">{o.icon}</span>
          <h3>{o.title}</h3>
          <p>{o.body}</p>
        </div>
      {/each}
    </div>
  </section>

  <!-- ═══════════════════════════════════════ -->
  <!--  ALTERNATING PRODUCT FEATURE SECTIONS   -->
  <!-- ═══════════════════════════════════════ -->
  <div class="band">
    <div class="band-head container" use:reveal>
      <span class="eyebrow eyebrow-dark">How it works</span>
      <h2>One platform, three products</h2>
      <p class="section-sub">
        Those outcomes don't come from a single feature — they come from carrying every record cleanly
        through the OAIS workflow: <strong>Intake → Preservation → Release</strong>. Each step is a product in its own right.
      </p>
    </div>

    {#each products as prod, i}
      <section class="container section product-section {prod.tone}" class:flip={i % 2 === 1}>
        <!-- Visual side: lighter "console" card so the page is not a stack of dark slabs -->
        <div class="ps-visual" use:reveal={i % 2 === 1 ? 120 : 0}>
          <div class="ps-card">
            <div class="ps-card-bar">
              <span class="ps-dot" aria-hidden="true"></span>
              <span class="ps-dot" aria-hidden="true"></span>
              <span class="ps-dot" aria-hidden="true"></span>
              <span class="ps-card-title">{prod.kicker.toLowerCase()} workflow</span>
            </div>
            <div class="ps-card-body">
              <div class="ps-stage-row">
                <div class="ps-stage-meta">
                  <span class="ps-stage-label">{prod.stage}</span>
                  <span class="ps-pkg">{prod.pkg} <span class="ps-abbr">{prod.abbr}</span></span>
                </div>
                <span class="ps-state">{prod.state}</span>
              </div>
              <ul class="ps-mini">
                {#each prod.capabilities as c}
                  <li><span class="ps-mini-dot" aria-hidden="true"></span>{c}</li>
                {/each}
              </ul>
              <div class="ps-card-foot">
                <span class="ps-tag">NOARK 5.5</span>
                <span class="ps-tag">E-ARK</span>
                <span class="ps-conform"><span aria-hidden="true">●</span> conformant</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Copy side -->
        <div class="ps-copy" use:reveal={i % 2 === 1 ? 0 : 120}>
          <span class="ps-eyebrow">Step 0{i + 1} · {prod.kicker}</span>
          <h3 class="ps-headline">{prod.headline}</h3>
          <p class="ps-lede">{prod.lede}</p>
          <ul class="ps-caps">
            {#each prod.capabilities as c}
              <li><span class="ps-check" aria-hidden="true">✓</span>{c}</li>
            {/each}
          </ul>
          <p class="ps-note"><span class="ps-note-label">Compliance</span>{prod.note}</p>
          <a href={prod.href} class="ps-cta">{prod.cta} <span aria-hidden="true">→</span></a>
        </div>
      </section>
    {/each}
  </div>

  <!-- ═══════════════════════════════════════ -->
  <!--  AT-A-GLANCE COMPARISON STRIP           -->
  <!-- ═══════════════════════════════════════ -->
  <section id="glance" class="container section">
    <div class="section-head" use:reveal>
      <span class="eyebrow eyebrow-dark">At a glance</span>
      <h2>Three products, one workflow</h2>
      <p class="section-sub">
        Intake → Preservation → Release. Each product produces its own package type — the same record,
        handled correctly at every step of the OAIS reference model.
      </p>
    </div>

    <div class="glance" use:reveal>
      <div class="glance-head" aria-hidden="true">
        <span>Product</span>
        <span>Package type</span>
        <span>What it produces</span>
        <span>Key capability</span>
      </div>
      {#each glance as g}
        <a href={g.href} class="glance-row {g.tone}">
          <span class="glance-cell glance-product">
            <span class="glance-kicker">{g.kicker}</span>
            <span class="glance-stage">{g.stage}</span>
          </span>
          <span class="glance-cell" data-label="Package type">{g.pkg}</span>
          <span class="glance-cell" data-label="What it produces">{g.produces}</span>
          <span class="glance-cell" data-label="Key capability">{g.key}</span>
        </a>
      {/each}
    </div>

    <p class="glance-legend">
      All three products support every one of the ten open standards — including compliance-grade
      <strong>NOARK&nbsp;5.5</strong> and <strong>E-ARK</strong>.
    </p>
  </section>

  <!-- ═══════════════════════════════════════ -->
  <!--  SECURE BY DESIGN — the trust pillar    -->
  <!-- ═══════════════════════════════════════ -->
  <section class="bleed security">
    <div class="container security-inner">
      <div class="security-copy" use:reveal>
        <span class="eyebrow">Trust &amp; security</span>
        <h2>Your archive, locked down by default</h2>
        <p>
          Compliance-grade conformance means nothing if the data underneath can be tampered with — so
          security isn't an add-on in Arcana, it's the foundation. Sessions are signed and short-lived,
          refresh tokens rotate on every use, and every request is scoped to its tenant.
        </p>
        <a href="/register" class="btn btn-primary">Create your secure archive</a>
      </div>
      <ul class="security-list" use:reveal={120}>
        {#each security as s}
          <li><span class="sec-check" aria-hidden="true">✓</span>{s}</li>
        {/each}
      </ul>
    </div>
  </section>

  <!-- ═══════════════════════════════════════ -->
  <!--  STANDARDS TRUST                        -->
  <!-- ═══════════════════════════════════════ -->
  <section class="container section">
    <div class="section-head" use:reveal>
      <span class="eyebrow eyebrow-dark">Standards</span>
      <h2>Every product, built on standards you trust</h2>
      <p class="section-sub">
        Ingest, Preserve, and Release all speak the same ten archival standards out of the box —
        including compliance-grade <strong>NOARK&nbsp;5.5</strong> and <strong>E-ARK</strong> for regulated archives.
      </p>
    </div>
    <div class="chips" use:reveal>
      {#each standards as std}
        <span class="chip" class:premium={premiumStandards.includes(std)}>
          {std}{#if premiumStandards.includes(std)}<span class="star" aria-hidden="true">★</span>{/if}
        </span>
      {/each}
    </div>
    <p class="chips-legend">
      <span class="star" aria-hidden="true">★</span> Premium conformance — validated, regulator-ready NOARK&nbsp;5.5 / E-ARK packages
    </p>
  </section>

  <!-- ═══════════════════════════════════════ -->
  <!--  FINAL CTA                              -->
  <!-- ═══════════════════════════════════════ -->
  <section class="bleed cta">
    <div class="cta-aurora" aria-hidden="true"></div>
    <div class="container cta-inner" use:reveal>
      <h2>One platform, end to end</h2>
      <p>Ingest, preserve, and release on open standards. Spin up a free archive today — upgrade when you need compliance-grade conformance.</p>
      <div class="hero-actions">
        <a href="/register" class="btn btn-light">Start free</a>
        <a href="/login" class="btn btn-ghost">Sign in</a>
      </div>
    </div>
  </section>
</div>

<style>
  /* ══════════════════════════════════════════
     PRODUCT PAGE — Final synthesis
     ══════════════════════════════════════════ */
  .product {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    color: var(--arc-ink);
  }

  /* Full-bleed sections span 100vw; guard against the scrollbar-gutter
     overflowing the page horizontally while this page is mounted. */
  :global(body) {
    overflow-x: hidden;
  }

  .product h1,
  .product h2,
  .product h3 {
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
    color: var(--arc-eyebrow-ink);
  }

  /* ── Hero ── */
  .hero {
    position: relative;
    overflow: hidden;
    background: radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%);
    color: #e2e8f0;
    margin-top: -2rem; /* cancel <main> top padding so it meets the nav */
    padding: 5rem 0 5.5rem;
    /* Stays dark in both themes; hairline keeps it from dissolving into a dark ground. */
    border-bottom: 1px solid var(--arc-line);
  }

  .hero-aurora {
    position: absolute;
    inset: -30% -10% auto -10%;
    height: 80%;
    /* Retuned to the three product accents — blue / indigo / orange. */
    background:
      radial-gradient(40% 60% at 18% 30%, rgba(59, 130, 246, 0.42), transparent 70%),
      radial-gradient(40% 60% at 52% 18%, rgba(99, 102, 241, 0.42), transparent 70%),
      radial-gradient(38% 55% at 84% 28%, rgba(249, 115, 22, 0.32), transparent 70%);
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
    mask-image: radial-gradient(circle at 50% 25%, black, transparent 75%);
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
    background: linear-gradient(110deg, #60a5fa 0%, #818cf8 48%, #fb923c 100%);
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

  /* ── Hero animated pipeline visual (canonical landing motif) ── */
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
    font-size: 1.2rem;
    font-weight: 700;
    color: #f8fafc;
    text-align: center;
  }

  /* Pipeline node accents mirror the three product accents. */
  .n-ingest .pc-stage { color: #60a5fa; }
  .n-preserve .pc-stage { color: #818cf8; }
  .n-release .pc-stage { color: #fb923c; }

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

  /* ── Tri-column pillars ── */
  .pillars {
    position: relative;
    margin-top: 3.5rem;
    display: grid;
    grid-template-columns: 1fr auto 1fr auto 1fr;
    align-items: stretch;
    gap: 0;
  }

  .pillar {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    text-align: left;
    background: linear-gradient(160deg, rgba(30, 41, 59, 0.85), rgba(15, 23, 42, 0.9));
    border: 1px solid rgba(148, 163, 184, 0.2);
    border-radius: 1.1rem;
    padding: 1.7rem 1.5rem 1.5rem;
    text-decoration: none;
    color: inherit;
    overflow: hidden;
    backdrop-filter: blur(8px);
    transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  }

  .pillar::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
  }

  .pillar.ingest::before { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
  .pillar.preserve::before { background: linear-gradient(90deg, #6366f1, #818cf8); }
  .pillar.release::before { background: linear-gradient(90deg, #f97316, #fb923c); }

  .pillar:hover {
    transform: translateY(-5px);
    border-color: rgba(226, 232, 240, 0.4);
    box-shadow: 0 26px 50px -22px rgba(0, 0, 0, 0.8);
  }

  .pillar-step {
    position: absolute;
    top: 0.9rem;
    right: 1.1rem;
    font-family: 'Space Grotesk', sans-serif;
    font-size: 1.6rem;
    font-weight: 700;
    color: rgba(148, 163, 184, 0.22);
  }

  .pillar-stage {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 1.5rem;
    font-weight: 700;
    color: #f8fafc;
    line-height: 1.1;
  }

  .pillar.ingest .pillar-stage { color: #60a5fa; }
  .pillar.preserve .pillar-stage { color: #818cf8; }
  .pillar.release .pillar-stage { color: #fb923c; }

  .pillar-kicker {
    margin-top: 0.35rem;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.12em;
    text-transform: uppercase;
    color: #94a3b8;
  }

  .pillar-pkg {
    margin-top: 0.9rem;
    font-size: 0.92rem;
    font-weight: 600;
    color: #e2e8f0;
  }

  .pillar-abbr {
    display: inline-block;
    margin-left: 0.2rem;
    font-size: 0.62rem;
    font-weight: 800;
    letter-spacing: 0.06em;
    color: #c4b5fd;
    background: rgba(139, 92, 246, 0.16);
    border: 1px solid rgba(139, 92, 246, 0.32);
    border-radius: 1rem;
    padding: 0.1rem 0.45rem;
    vertical-align: middle;
  }

  .pillar.ingest .pillar-abbr { color: #93c5fd; background: rgba(59, 130, 246, 0.16); border-color: rgba(59, 130, 246, 0.34); }
  .pillar.release .pillar-abbr { color: #fdba74; background: rgba(249, 115, 22, 0.16); border-color: rgba(249, 115, 22, 0.34); }

  .pillar-blurb {
    margin: 0.7rem 0 1.2rem;
    font-size: 0.88rem;
    line-height: 1.55;
    color: #94a3b8;
    flex: 1;
  }

  .pillar-link {
    font-size: 0.82rem;
    font-weight: 700;
    color: #c4b5fd;
  }

  .pillar.ingest .pillar-link { color: #93c5fd; }
  .pillar.release .pillar-link { color: #fdba74; }

  .pillar-arrow {
    align-self: center;
    padding: 0 0.7rem;
    font-size: 1.5rem;
    font-weight: 700;
    color: rgba(148, 163, 184, 0.45);
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
    color: var(--arc-faint);
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
    color: var(--arc-body);
    background: var(--arc-card);
    border: 1px solid var(--arc-line-strong);
    border-radius: 0.5rem;
    padding: 0.45rem 0.9rem;
  }

  .trustbar-item.premium {
    color: var(--arc-chip-violet-ink);
    border-color: var(--arc-chip-violet-hover);
    background: var(--arc-chip-violet-bg);
  }

  /* ── Sections ── */
  .section {
    padding: 5rem 1.5rem 0;
  }

  .section-head {
    text-align: center;
    max-width: 42rem;
    margin: 0 auto 3rem;
  }

  .section-head h2 {
    font-size: clamp(1.8rem, 3.2vw, 2.5rem);
    font-weight: 700;
    color: var(--arc-ink);
    margin: 0 0 0.85rem;
  }

  .section-sub {
    font-size: 1.05rem;
    color: var(--arc-muted);
    line-height: 1.6;
    margin: 0;
  }

  .section-sub strong {
    color: var(--arc-body);
  }

  /* ── Outcomes ── */
  .outcome-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1.25rem;
  }

  .outcome-card {
    position: relative;
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
    padding: 2rem 1.7rem;
    overflow: hidden;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .outcome-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
  }

  .o-ingest::before { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
  .o-preserve::before { background: linear-gradient(90deg, #6366f1, #818cf8); }
  .o-release::before { background: linear-gradient(90deg, #f97316, #fb923c); }

  .outcome-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--arc-shadow-lift);
  }

  .o-ingest:hover { border-color: #bfdbfe; }
  .o-preserve:hover { border-color: var(--arc-hover-border); }
  .o-release:hover { border-color: #fed7aa; }

  .outcome-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 3rem;
    height: 3rem;
    border-radius: 0.85rem;
    font-size: 1.4rem;
    color: white;
    margin-bottom: 1.1rem;
  }

  .o-ingest .outcome-icon { background: linear-gradient(135deg, #3b82f6, #60a5fa); box-shadow: 0 8px 20px -6px rgba(59, 130, 246, 0.5); }
  .o-preserve .outcome-icon { background: linear-gradient(135deg, #6366f1, #818cf8); box-shadow: 0 8px 20px -6px rgba(99, 102, 241, 0.5); }
  .o-release .outcome-icon { background: linear-gradient(135deg, #f97316, #fb923c); box-shadow: 0 8px 20px -6px rgba(249, 115, 22, 0.5); }

  .outcome-card h3 {
    font-size: 1.3rem;
    color: var(--arc-ink);
    margin: 0 0 0.6rem;
  }

  .outcome-card p {
    font-size: 0.95rem;
    color: var(--arc-muted);
    line-height: 1.65;
    margin: 0;
  }

  /* ── Product band ── */
  .band {
    margin-top: 5rem;
    padding: 4.5rem 0 0.5rem;
    background:
      radial-gradient(80% 60% at 50% 0%, var(--arc-card-2) 0%, transparent 70%),
      var(--arc-ground);
  }

  .band-head {
    text-align: center;
    max-width: 42rem;
    margin: 0 auto;
  }

  .band-head h2 {
    font-size: clamp(1.8rem, 3.2vw, 2.5rem);
    font-weight: 700;
    color: var(--arc-ink);
    margin: 0 0 0.85rem;
  }

  /* ── Alternating product feature sections ── */
  .product-section {
    display: grid;
    grid-template-columns: 0.95fr 1.05fr;
    gap: 3.5rem;
    align-items: center;
    padding-top: 4rem;
  }

  /* Odd section (Preserve) flips the visual to the right. */
  .product-section.flip .ps-visual {
    order: 2;
  }

  .ps-copy {
    min-width: 0;
  }

  .ps-eyebrow {
    display: inline-block;
    font-size: 0.74rem;
    font-weight: 700;
    letter-spacing: 0.13em;
    text-transform: uppercase;
    margin-bottom: 0.9rem;
  }

  .ingest .ps-eyebrow { color: #2563eb; }
  .preserve .ps-eyebrow { color: #4f46e5; }
  .release .ps-eyebrow { color: #ea580c; }

  .ps-headline {
    font-size: clamp(1.7rem, 3vw, 2.3rem);
    font-weight: 700;
    color: var(--arc-ink);
    margin: 0 0 1rem;
    line-height: 1.12;
  }

  .ps-lede {
    font-size: 1.05rem;
    line-height: 1.7;
    color: var(--arc-body);
    margin: 0 0 1.75rem;
  }

  .ps-caps {
    list-style: none;
    margin: 0 0 1.75rem;
    padding: 0;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.6rem 1.25rem;
  }

  .ps-caps li {
    display: flex;
    align-items: flex-start;
    gap: 0.6rem;
    font-size: 0.92rem;
    line-height: 1.45;
    color: var(--arc-body);
    font-weight: 500;
  }

  .ps-check {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 1.3rem;
    height: 1.3rem;
    margin-top: 0.05rem;
    border-radius: 50%;
    color: white;
    font-size: 0.7rem;
    font-weight: 800;
  }

  .ingest .ps-check { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
  .preserve .ps-check { background: linear-gradient(135deg, #818cf8, #6366f1); }
  .release .ps-check { background: linear-gradient(135deg, #fb923c, #f97316); }

  .ps-note {
    position: relative;
    margin: 0 0 1.75rem;
    padding: 0.95rem 1.1rem 0.95rem 1.15rem;
    border-radius: 0.7rem;
    font-size: 0.88rem;
    line-height: 1.55;
    color: var(--arc-body);
    background: var(--arc-card-2);
    border: 1px solid var(--arc-line);
    border-left-width: 3px;
  }

  .ingest .ps-note { border-left-color: #3b82f6; }
  .preserve .ps-note { border-left-color: #6366f1; }
  .release .ps-note { border-left-color: #f97316; }

  .ps-note-label {
    display: inline-block;
    margin-right: 0.55rem;
    font-size: 0.66rem;
    font-weight: 800;
    letter-spacing: 0.09em;
    text-transform: uppercase;
    vertical-align: 1px;
  }

  .ingest .ps-note-label { color: #2563eb; }
  .preserve .ps-note-label { color: #4f46e5; }
  .release .ps-note-label { color: #ea580c; }

  .ps-cta {
    display: inline-block;
    padding: 0.8rem 1.6rem;
    border-radius: 0.65rem;
    font-weight: 700;
    font-size: 0.95rem;
    text-decoration: none;
    color: white;
    transition: transform 0.18s ease, box-shadow 0.18s ease;
  }

  .ingest .ps-cta { background: linear-gradient(135deg, #3b82f6, #2563eb); box-shadow: 0 10px 28px -10px rgba(59, 130, 246, 0.65); }
  .preserve .ps-cta { background: linear-gradient(135deg, #6366f1, #4f46e5); box-shadow: 0 10px 28px -10px rgba(99, 102, 241, 0.65); }
  .release .ps-cta { background: linear-gradient(135deg, #f97316, #ea580c); box-shadow: 0 10px 28px -10px rgba(249, 115, 22, 0.65); }

  .ps-cta:hover {
    transform: translateY(-2px);
  }

  .ingest .ps-cta:hover { box-shadow: 0 16px 36px -10px rgba(59, 130, 246, 0.8); }
  .preserve .ps-cta:hover { box-shadow: 0 16px 36px -10px rgba(99, 102, 241, 0.8); }
  .release .ps-cta:hover { box-shadow: 0 16px 36px -10px rgba(249, 115, 22, 0.8); }

  /* ── Product visual card (light "console" treatment) ── */
  .ps-visual {
    perspective: 1200px;
  }

  .ps-card {
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1.1rem;
    box-shadow: 0 30px 60px -28px rgba(15, 23, 42, 0.35);
    overflow: hidden;
    transform: rotateY(6deg) rotateX(3deg);
    transition: transform 0.4s ease;
  }

  .product-section.flip .ps-card {
    transform: rotateY(-6deg) rotateX(3deg);
  }

  .ps-visual:hover .ps-card {
    transform: rotateY(0deg) rotateX(0deg);
  }

  .ps-card-bar {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.9rem 1.1rem;
    background: var(--arc-card-2);
    border-bottom: 1px solid var(--arc-line);
  }

  .ps-dot {
    width: 0.6rem;
    height: 0.6rem;
    border-radius: 50%;
    background: #cbd5e1;
  }

  .ps-dot:nth-child(1) { background: #f87171; }
  .ps-dot:nth-child(2) { background: #fbbf24; }
  .ps-dot:nth-child(3) { background: #34d399; }

  .ps-card-title {
    margin-left: 0.6rem;
    font-size: 0.72rem;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: var(--arc-faint);
    font-weight: 600;
  }

  .ps-card-body {
    padding: 1.4rem 1.4rem 1.3rem;
  }

  .ps-stage-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 1.2rem;
    padding-bottom: 1.1rem;
    border-bottom: 1px solid var(--arc-line);
  }

  .ps-stage-meta {
    display: flex;
    flex-direction: column;
    gap: 0.3rem;
    min-width: 0;
  }

  .ps-stage-label {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 1.55rem;
    font-weight: 700;
    line-height: 1;
  }

  .ingest .ps-stage-label { color: #2563eb; }
  .preserve .ps-stage-label { color: #4f46e5; }
  .release .ps-stage-label { color: #ea580c; }

  .ps-pkg {
    font-size: 0.82rem;
    font-weight: 600;
    color: var(--arc-muted);
  }

  .ps-abbr {
    display: inline-block;
    margin-left: 0.2rem;
    font-size: 0.6rem;
    font-weight: 800;
    letter-spacing: 0.06em;
    color: var(--arc-chip-violet-ink);
    background: var(--arc-chip-violet-bg);
    border: 1px solid var(--arc-chip-violet-hover);
    border-radius: 1rem;
    padding: 0.08rem 0.42rem;
    vertical-align: middle;
  }

  .ps-state {
    flex-shrink: 0;
    font-size: 0.62rem;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--arc-alert-green-ink);
    background: var(--arc-alert-green-bg);
    border: 1px solid var(--arc-alert-green-border);
    border-radius: 1rem;
    padding: 0.2rem 0.6rem;
    font-weight: 700;
  }

  .ps-mini {
    list-style: none;
    margin: 0 0 1.2rem;
    padding: 0;
    display: grid;
    gap: 0.5rem;
  }

  .ps-mini li {
    display: flex;
    align-items: center;
    gap: 0.65rem;
    font-size: 0.82rem;
    color: var(--arc-body);
    background: var(--arc-card-2);
    border: 1px solid var(--arc-line);
    border-radius: 0.6rem;
    padding: 0.55rem 0.75rem;
  }

  .ps-mini-dot {
    flex-shrink: 0;
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 50%;
  }

  .ingest .ps-mini-dot { background: #3b82f6; }
  .preserve .ps-mini-dot { background: #6366f1; }
  .release .ps-mini-dot { background: #f97316; }

  .ps-card-foot {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    padding-top: 1rem;
    border-top: 1px solid var(--arc-line);
    flex-wrap: wrap;
  }

  .ps-tag {
    font-size: 0.66rem;
    font-weight: 700;
    color: var(--arc-chip-violet-ink);
    background: var(--arc-chip-violet-bg);
    border: 1px solid var(--arc-chip-violet-hover);
    border-radius: 1rem;
    padding: 0.2rem 0.6rem;
  }

  .ps-conform {
    margin-left: auto;
    font-size: 0.7rem;
    font-weight: 700;
    color: var(--arc-alert-green-ink);
  }

  /* ── At-a-glance comparison ── */
  .glance {
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
    overflow: hidden;
    background: var(--arc-card);
    box-shadow: var(--arc-shadow-card);
  }

  .glance-head,
  .glance-row {
    display: grid;
    grid-template-columns: 0.9fr 1.3fr 1fr 1.4fr;
    align-items: center;
  }

  .glance-head {
    background: var(--arc-card-2);
    border-bottom: 1px solid var(--arc-line);
    padding: 0.85rem 1.4rem;
  }

  .glance-head span {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--arc-faint);
  }

  .glance-row {
    position: relative;
    padding: 1.25rem 1.4rem;
    text-decoration: none;
    color: inherit;
    border-bottom: 1px solid var(--arc-line);
    transition: background 0.2s ease;
  }

  .glance-row:last-child {
    border-bottom: none;
  }

  .glance-row::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
  }

  .glance-row.ingest::before { background: linear-gradient(180deg, #3b82f6, #60a5fa); }
  .glance-row.preserve::before { background: linear-gradient(180deg, #6366f1, #818cf8); }
  .glance-row.release::before { background: linear-gradient(180deg, #f97316, #fb923c); }

  .glance-row.ingest:hover { background: #eff6ff; }
  .glance-row.preserve:hover { background: var(--arc-chip-soft-indigo-bg); }
  .glance-row.release:hover { background: var(--arc-chip-orange-bg); }

  .glance-cell {
    font-size: 0.92rem;
    color: var(--arc-body);
    line-height: 1.4;
  }

  .glance-product {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
  }

  .glance-kicker {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--arc-ink);
  }

  .glance-row.ingest .glance-kicker { color: #2563eb; }
  .glance-row.preserve .glance-kicker { color: #4f46e5; }
  .glance-row.release .glance-kicker { color: #ea580c; }

  .glance-stage {
    font-size: 0.72rem;
    font-weight: 600;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--arc-faint);
  }

  .glance-legend {
    text-align: center;
    margin: 1.5rem auto 0;
    max-width: 42rem;
    font-size: 0.85rem;
    color: var(--arc-muted);
    line-height: 1.6;
  }

  .glance-legend strong {
    color: var(--arc-chip-violet-ink);
  }

  /* ── Security ── */
  .security {
    margin-top: 5rem;
    background: radial-gradient(120% 120% at 80% 0%, #1e1b4b 0%, #0f172a 60%, #0b1120 100%);
    color: #e2e8f0;
    padding: 5rem 0;
    /* Stays dark in both themes; hairlines keep it from dissolving into a dark ground. */
    border-top: 1px solid var(--arc-line);
    border-bottom: 1px solid var(--arc-line);
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
    background: var(--arc-card);
    border: 1px solid var(--arc-line-strong);
    border-radius: 2rem;
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--arc-body);
    transition: border-color 0.2s, background 0.2s, transform 0.2s;
  }

  .chip:hover {
    border-color: var(--arc-hover-border);
    background: var(--arc-chip-soft-indigo-bg);
    transform: translateY(-2px);
  }

  .chip.premium {
    border-color: #c4b5fd;
    background: linear-gradient(135deg, var(--arc-chip-violet-bg), var(--arc-chip-soft-indigo-bg));
    color: var(--arc-chip-violet-ink);
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
    color: var(--arc-muted);
  }

  /* ── Final CTA ── */
  .cta {
    position: relative;
    overflow: hidden;
    margin-top: 5rem;
    margin-bottom: -2rem; /* meet the footer */
    /* Unchanged from the landing so the page rhymes with the homepage. */
    background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #6d28d9 100%);
    color: white;
    padding: 5rem 0;
    text-align: center;
  }

  .cta-aurora {
    position: absolute;
    inset: 0;
    background:
      radial-gradient(40% 70% at 15% 20%, rgba(59, 130, 246, 0.35), transparent 70%),
      radial-gradient(40% 70% at 85% 80%, rgba(249, 115, 22, 0.32), transparent 70%);
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
    max-width: 36rem;
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
    .pipeline-card,
    .ps-card,
    .product-section.flip .ps-card {
      transform: none;
    }
  }

  /* ── Responsive ── */
  @media (max-width: 920px) {
    .hero-inner {
      grid-template-columns: 1fr;
      gap: 2.5rem;
    }
    /* Let the single column shrink to the container so hero text wraps
       instead of being forced wide by the pipeline's intrinsic width. */
    .hero-copy,
    .hero-visual {
      min-width: 0;
    }
    .hero-visual {
      max-width: 28rem;
    }
    .pipeline-card {
      transform: none;
    }
    .pillars {
      grid-template-columns: 1fr;
      gap: 1rem;
      max-width: 30rem;
      margin-left: auto;
      margin-right: auto;
    }
    .pillar-arrow {
      transform: rotate(90deg);
      padding: 0.2rem 0;
    }
    .outcome-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .outcome-grid .outcome-card:last-child {
      grid-column: 1 / -1;
    }
    .product-section {
      grid-template-columns: 1fr;
      gap: 2.25rem;
    }
    /* Keep the visual above the copy regardless of flip when stacked. */
    .product-section.flip .ps-visual {
      order: 0;
    }
    .ps-visual {
      max-width: 30rem;
      width: 100%;
    }
    .glance-head {
      display: none;
    }
    .glance-row {
      grid-template-columns: 1fr;
      gap: 0.5rem;
      padding: 1.25rem 1.4rem 1.25rem 1.6rem;
    }
    .glance-cell[data-label]::before {
      content: attr(data-label);
      display: block;
      font-size: 0.66rem;
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      color: var(--arc-faint);
      margin-bottom: 0.15rem;
    }
    .security-inner {
      grid-template-columns: 1fr;
      gap: 2rem;
    }
  }

  @media (max-width: 560px) {
    .hero {
      padding: 3.5rem 0 4rem;
    }
    /* Stack the pipeline so its three nodes never force horizontal overflow. */
    .pc-flow {
      flex-direction: column;
      align-items: stretch;
      gap: 0.45rem;
    }
    .pc-conn {
      width: 2px;
      height: 0.9rem;
      align-self: center;
    }
    .outcome-grid {
      grid-template-columns: 1fr;
    }
    .outcome-grid .outcome-card:last-child {
      grid-column: auto;
    }
    .ps-caps {
      grid-template-columns: 1fr;
    }
    .trustbar {
      flex-direction: column;
      align-items: flex-start;
      gap: 0.75rem;
    }
    .trustbar-marquee {
      width: 100%;
    }
    .hero-actions {
      width: 100%;
    }
    .hero-actions .btn {
      flex: 1;
      text-align: center;
    }
  }

  /* ── Dark-theme accent adjustments (no tokens exist for the per-product
     blue/orange accents; keep the hue, lift it for a dark ground) ── */
  :global(html[data-theme='dark']) .ingest .ps-eyebrow,
  :global(html[data-theme='dark']) .ingest .ps-note-label,
  :global(html[data-theme='dark']) .ingest .ps-stage-label,
  :global(html[data-theme='dark']) .glance-row.ingest .glance-kicker {
    color: #60a5fa;
  }

  :global(html[data-theme='dark']) .preserve .ps-eyebrow,
  :global(html[data-theme='dark']) .preserve .ps-note-label,
  :global(html[data-theme='dark']) .preserve .ps-stage-label,
  :global(html[data-theme='dark']) .glance-row.preserve .glance-kicker {
    color: #818cf8;
  }

  :global(html[data-theme='dark']) .release .ps-eyebrow,
  :global(html[data-theme='dark']) .release .ps-note-label,
  :global(html[data-theme='dark']) .release .ps-stage-label,
  :global(html[data-theme='dark']) .glance-row.release .glance-kicker {
    color: #fb923c;
  }

  :global(html[data-theme='dark']) .o-ingest:hover { border-color: rgba(59, 130, 246, 0.45); }
  :global(html[data-theme='dark']) .o-release:hover { border-color: rgba(249, 115, 22, 0.45); }

  :global(html[data-theme='dark']) .glance-row.ingest:hover { background: rgba(59, 130, 246, 0.12); }

  :global(html[data-theme='dark']) .chip.premium { border-color: rgba(139, 92, 246, 0.45); }
</style>