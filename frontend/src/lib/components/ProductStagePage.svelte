<script lang="ts">
  // Public marketing page for one stage of the workflow — renders for everyone,
  // no auth gating. /product/intake, /product/preservation and /product/release
  // all render this component, so the three pages look alike and differ only in
  // their content (see $lib/productStages).
  import { productStages, standards, premiumStandards, type ProductStage } from '$lib/productStages';

  export let stage: ProductStage;

  $: index = productStages.findIndex((s) => s.slug === stage.slug);
  $: previous = productStages[index - 1];
  $: next = productStages[index + 1];

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
  <title>{stage.kicker} — Arcana Product</title>
  <meta name="description" content={stage.description} />
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous" />
  <link
    href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600;700&display=swap"
    rel="stylesheet"
  />
</svelte:head>

<div class="product">
  <!-- HERO — stage tabs, this stage's promise, and where it sits in the pipeline -->
  <section class="bleed hero">
    <div class="hero-aurora" aria-hidden="true"></div>
    <div class="hero-grid-bg" aria-hidden="true"></div>
    <div class="container">
      <nav class="stage-tabs" aria-label="Product stages">
        {#each productStages as s, i}
          <a
            href="/product/{s.slug}"
            class="stage-tab {s.tone}"
            class:current={s.slug === stage.slug}
            aria-current={s.slug === stage.slug ? 'page' : undefined}
          >
            <span class="stage-tab-step" aria-hidden="true">0{i + 1}</span>{s.stage}
          </a>
        {/each}
      </nav>
    </div>
    <div class="container hero-inner">
      <div class="hero-copy">
        <span class="eyebrow">Step 0{index + 1} · {stage.kicker} · {stage.pkg}</span>
        <h1>{stage.headline}</h1>
        <p class="lede">{stage.lede}</p>
        <div class="hero-actions">
          <a href="/register" class="btn btn-primary">Start free</a>
          <a href={stage.href} class="btn btn-ghost">{stage.cta}</a>
        </div>
      </div>

      <div class="hero-visual">
        <div class="pipeline-card">
          <div class="pc-bar">
            <span class="pc-dot" aria-hidden="true"></span>
            <span class="pc-dot" aria-hidden="true"></span>
            <span class="pc-dot" aria-hidden="true"></span>
            <span class="pc-bar-title">preservation pipeline</span>
          </div>
          <div class="pc-flow">
            {#each productStages as s, i}
              <div class="pc-node n-{s.tone}" class:dim={s.slug !== stage.slug}>
                <span class="pc-stage">{s.stage}</span>
                <span class="pc-kicker">{s.kicker}</span>
                <span class="pc-state">{s.state}</span>
              </div>
              {#if i < productStages.length - 1}
                <div class="pc-conn" aria-hidden="true">
                  <span class="pc-pulse" style="animation-delay:{i * 0.8}s"></span>
                </div>
              {/if}
            {/each}
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
  </section>

  <!-- WHAT THIS STAGE DOES -->
  <section class="container section product-section {stage.tone}">
    <div class="ps-visual" use:reveal>
      <div class="ps-card">
        <div class="ps-card-bar">
          <span class="ps-dot" aria-hidden="true"></span>
          <span class="ps-dot" aria-hidden="true"></span>
          <span class="ps-dot" aria-hidden="true"></span>
          <span class="ps-card-title">{stage.kicker.toLowerCase()} workflow</span>
        </div>
        <div class="ps-card-body">
          <div class="ps-stage-row">
            <div class="ps-stage-meta">
              <span class="ps-stage-label">{stage.stage}</span>
              <span class="ps-pkg">{stage.pkg} <span class="ps-abbr">{stage.abbr}</span></span>
            </div>
            <span class="ps-state">{stage.state}</span>
          </div>
          <dl class="ps-facts">
            <div>
              <dt>Produces</dt>
              <dd>{stage.produces}</dd>
            </div>
            <div>
              <dt>Key capability</dt>
              <dd>{stage.key}</dd>
            </div>
          </dl>
          <div class="ps-card-foot">
            <span class="ps-tag">NOARK 5.5</span>
            <span class="ps-tag">E-ARK</span>
            <span class="ps-conform"><span aria-hidden="true">●</span> conformant</span>
          </div>
        </div>
      </div>
    </div>

    <div class="ps-copy" use:reveal={120}>
      <span class="ps-eyebrow">What {stage.kicker} does</span>
      <h2 class="ps-headline">{stage.subhead}</h2>
      <ul class="ps-caps">
        {#each stage.capabilities as c}
          <li><span class="ps-check" aria-hidden="true">✓</span>{c}</li>
        {/each}
      </ul>
      <p class="ps-note"><span class="ps-note-label">Compliance</span>{stage.note}</p>
    </div>
  </section>

  <!-- STANDARDS -->
  <section class="container section">
    <div class="section-head" use:reveal>
      <span class="eyebrow eyebrow-dark">Standards</span>
      <h2>{stage.kicker} on standards you trust</h2>
      <p class="section-sub">
        {stage.stage} speaks ten archival standards out of the box, including compliance-grade
        <strong>NOARK&nbsp;5.5</strong> and <strong>E-ARK</strong> for regulated archives.
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

  <!-- PREVIOUS / NEXT STAGE -->
  <nav class="container stage-pager" aria-label="Other product stages">
    {#if previous}
      <a href="/product/{previous.slug}" class="previous">
        <span class="pager-label"><span aria-hidden="true">←</span> Previous step</span>
        <span class="pager-title">{previous.stage}</span>
      </a>
    {/if}
    {#if next}
      <a href="/product/{next.slug}" class="next">
        <span class="pager-label">Next step <span aria-hidden="true">→</span></span>
        <span class="pager-title">{next.stage}</span>
      </a>
    {/if}
  </nav>
</div>

<style>
  /* ══════════════════════════════════════════
     PRODUCT STAGE PAGE — shared by /product/intake, /preservation, /release
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
  .product h2 {
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

  .lede {
    font-size: 1.18rem;
    line-height: 1.7;
    color: #cbd5e1;
    max-width: 36rem;
    margin: 0 0 2.2rem;
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

  /* ── Alternating product feature sections ── */
  .product-section {
    display: grid;
    grid-template-columns: 0.95fr 1.05fr;
    gap: 3.5rem;
    align-items: center;
    padding-top: 4rem;
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

  .ps-facts {
    display: grid;
    gap: 0.5rem;
    margin: 0 0 1.2rem;
  }

  .ps-facts div {
    background: var(--arc-card-2);
    border: 1px solid var(--arc-line);
    border-radius: 0.6rem;
    padding: 0.6rem 0.8rem;
  }

  .ps-facts dt {
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--arc-muted);
    margin-bottom: 0.15rem;
  }

  .ps-facts dd {
    margin: 0;
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--arc-body);
  }

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

  /* ── Stage tabs: the three product pages, switchable from the hero ── */
  .stage-tabs {
    position: relative;
    display: inline-flex;
    flex-wrap: wrap;
    gap: 0.35rem;
    padding: 0.3rem;
    margin-bottom: 2.5rem;
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(148, 163, 184, 0.22);
    border-radius: 999px;
  }

  .stage-tab {
    display: inline-flex;
    align-items: baseline;
    gap: 0.5rem;
    padding: 0.5rem 1.1rem;
    border-radius: 999px;
    font-size: 0.9rem;
    font-weight: 600;
    color: #cbd5e1;
    text-decoration: none;
    transition: background 0.18s ease, color 0.18s ease;
  }

  .stage-tab:hover {
    background: rgba(255, 255, 255, 0.08);
    color: white;
  }

  .stage-tab-step {
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: #94a3b8;
  }

  .stage-tab.current {
    background: rgba(255, 255, 255, 0.14);
    color: white;
  }

  .stage-tab.current.ingest .stage-tab-step { color: #60a5fa; }
  .stage-tab.current.preserve .stage-tab-step { color: #818cf8; }
  .stage-tab.current.release .stage-tab-step { color: #fb923c; }

  /* The pipeline shows all three steps; the ones this page is not about recede. */
  .pc-node.dim {
    opacity: 0.45;
  }

  /* ── Previous / next stage ── */
  .stage-pager {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    padding-top: 4rem;
    padding-bottom: 3rem;
  }

  .stage-pager a {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    padding: 1rem 1.25rem;
    min-width: 12rem;
    border: 1px solid var(--arc-line);
    border-radius: 0.8rem;
    background: var(--arc-card);
    text-decoration: none;
    transition: border-color 0.18s ease, transform 0.18s ease;
  }

  .stage-pager a:hover {
    border-color: var(--arc-hover-border);
    transform: translateY(-2px);
  }

  .stage-pager .next {
    margin-left: auto;
    text-align: right;
  }

  .pager-label {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: var(--arc-muted);
  }

  .pager-title {
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--arc-ink);
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
    .pc-pulse {
      animation: none;
    }
    .pipeline-card,
    .ps-card {
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
    .product-section {
      grid-template-columns: 1fr;
      gap: 2.25rem;
    }
    .ps-visual {
      max-width: 30rem;
      width: 100%;
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
    .ps-caps {
      grid-template-columns: 1fr;
    }
    /* The tabs wrap to two rows here; a full pill radius would look lopsided. */
    .stage-tabs {
      border-radius: 1.4rem;
    }
    .stage-pager {
      flex-direction: column;
    }
    .stage-pager .next {
      margin-left: 0;
      text-align: left;
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
  :global(html[data-theme='dark']) .ingest .ps-stage-label {
    color: #60a5fa;
  }

  :global(html[data-theme='dark']) .preserve .ps-eyebrow,
  :global(html[data-theme='dark']) .preserve .ps-note-label,
  :global(html[data-theme='dark']) .preserve .ps-stage-label {
    color: #818cf8;
  }

  :global(html[data-theme='dark']) .release .ps-eyebrow,
  :global(html[data-theme='dark']) .release .ps-note-label,
  :global(html[data-theme='dark']) .release .ps-stage-label {
    color: #fb923c;
  }

  :global(html[data-theme='dark']) .chip.premium { border-color: rgba(139, 92, 246, 0.45); }
</style>
