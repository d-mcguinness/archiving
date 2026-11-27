<script lang="ts">
  // Public marketing page for one stage of the workflow. /ingest and /preserve
  // (signed out) and /product/release all render this component, so the
  // three pages look alike and differ only in their content (see $lib/productStages).
  import { productStages, standards, premiumStandards, type ProductStage } from '$lib/productStages';

  export let stage: ProductStage;

  $: index = productStages.findIndex((s) => s.slug === stage.slug);
  $: previous = productStages[index - 1];
  $: next = productStages[index + 1];

  // Line icons (24×24, stroked) for the steps and capabilities, keyed by name.
  const icons: Record<string, string[]> = {
    layers: ['M12 3 3 8l9 5 9-5-9-5z', 'M3 13l9 5 9-5'],
    form: ['M5 4h14v16H5z', 'M8 9h8M8 13h8M8 17h5'],
    check: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M8 12.5l2.8 2.8L16 9.5'],
    clip: ['M8 12.5l6.2-6.2a3 3 0 0 1 4.2 4.2l-7.6 7.6a5 5 0 0 1-7-7L10 4.8'],
    tree: ['M10 3h4v4h-4zM4 17h4v4H4zM16 17h4v4h-4z', 'M12 7v5M6 17v-5h12v5'],
    lock: ['M6 11h12v9H6z', 'M9 11V8a3 3 0 0 1 6 0v3'],
    shield: ['M12 3l8 3v6c0 4.5-3.2 8-8 9-4.8-1-8-4.5-8-9V6l8-3z', 'M9 12l2 2 4-4'],
    history: ['M4 12a8 8 0 1 0 2.5-5.8M4 4v4h4', 'M12 8v4l3 2'],
    refresh: ['M20 11a8 8 0 0 0-14.5-4M4 4v3.5h3.5', 'M4 13a8 8 0 0 0 14.5 4M20 20v-3.5h-3.5'],
    tag: ['M3 12V4h8l10 10-8 8L3 12z', 'M7.5 8.5h.01'],
    users: ['M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM2.5 20a6.5 6.5 0 0 1 13 0', 'M16 4.5a3.5 3.5 0 0 1 0 6.5M18 14.5a6.5 6.5 0 0 1 3.5 5.5'],
    key: ['M8 15a4 4 0 1 1 3.9-5H21v3h-2v2.5h-3V13h-4.1A4 4 0 0 1 8 15z'],
    download: ['M12 4v11M7.5 11l4.5 4.5 4.5-4.5', 'M4 20h16'],
    box: ['M3 8l9-5 9 5v8l-9 5-9-5V8z', 'M3 8l9 5 9-5M12 13v8']
  };

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
            href={s.path}
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
          <a href="/login" class="btn btn-ghost">Sign in</a>
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

  <!-- HOW IT WORKS — three steps as a visual flow, then what you get -->
  <section class="container section {stage.tone}">
    <div class="section-head" use:reveal>
      <span class="eyebrow eyebrow-dark">How it works</span>
      <h2>{stage.subhead}</h2>
    </div>

    <ol class="flow" use:reveal>
      {#each stage.steps as step, i}
        <li class="flow-step">
          <span class="flow-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              {#each icons[step.icon] ?? [] as d}<path {d} />{/each}
            </svg>
          </span>
          <span class="flow-num" aria-hidden="true">0{i + 1}</span>
          <h3>{step.title}</h3>
          <p>{step.text}</p>
        </li>
      {/each}
    </ol>

    <ul class="tiles" use:reveal={120}>
      {#each stage.capabilities as c}
        <li class="tile">
          <span class="tile-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              {#each icons[c.icon] ?? [] as d}<path {d} />{/each}
            </svg>
          </span>
          {c.label}
        </li>
      {/each}
    </ul>
  </section>

  <!-- STANDARDS -->
  <section class="container section">
    <div class="section-head" use:reveal>
      <span class="eyebrow eyebrow-dark">Standards</span>
      <h2>Ten standards, out of the box</h2>
    </div>
    <div class="chips" use:reveal>
      {#each standards as std}
        <span class="chip" class:premium={premiumStandards.includes(std)}>
          {std}{#if premiumStandards.includes(std)}<span class="star" aria-hidden="true">★</span>{/if}
        </span>
      {/each}
    </div>
    <p class="chips-legend">
      <span class="star" aria-hidden="true">★</span> Validated, regulator-ready conformance
    </p>
  </section>

  <!-- PREVIOUS / NEXT STAGE -->
  <nav class="container stage-pager" aria-label="Other product stages">
    {#if previous}
      <a href={previous.path} class="previous">
        <span class="pager-label"><span aria-hidden="true">←</span> Previous step</span>
        <span class="pager-title">{previous.stage}</span>
      </a>
    {/if}
    {#if next}
      <a href={next.path} class="next">
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
    color: var(--arc-panel-accent);
    margin-bottom: 1rem;
  }

  .eyebrow-dark {
    color: var(--arc-eyebrow-ink);
  }

  /* ── Hero ── */
  .hero {
    /* Hero visuals (pipeline mock): light cards by default, dark under the dark theme. */
    --pv-card: rgba(255, 255, 255, 0.86);
    --pv-shadow: 0 30px 60px -24px rgba(15, 23, 42, 0.28);
    --pv-blue: #2563eb;
    --pv-indigo: #4f46e5;
    --pv-orange: #c2410c;
    position: relative;
    overflow: hidden;
    background: var(--arc-panel-bg);
    color: var(--arc-panel-body);
    margin-top: -2rem; /* cancel <main> top padding so it meets the nav */
    padding: 5rem 0 5.5rem;
    /* Hairline keeps it from dissolving into the page ground. */
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
    opacity: var(--arc-panel-glow);
    pointer-events: none;
  }

  .hero-grid-bg {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(var(--arc-panel-line) 1px, transparent 1px),
      linear-gradient(90deg, var(--arc-panel-line) 1px, transparent 1px);
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
    color: var(--arc-panel-ink);
  }

  .lede {
    font-size: 1.18rem;
    line-height: 1.7;
    color: var(--arc-panel-body);
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
    background: var(--arc-tint);
    color: var(--arc-panel-ink);
    border: 1px solid var(--arc-ghost-border);
  }

  .btn-ghost:hover {
    background: var(--arc-tint-strong);
    border-color: var(--arc-ghost-border-strong);
    transform: translateY(-2px);
  }

  /* ── Hero animated pipeline visual (canonical landing motif) ── */
  .hero-visual {
    perspective: 1200px;
  }

  .pipeline-card {
    background: var(--pv-card);
    border: 1px solid var(--arc-panel-line);
    border-radius: 1.1rem;
    padding: 1.1rem 1.1rem 1.25rem;
    box-shadow: var(--pv-shadow);
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
    border-bottom: 1px solid var(--arc-panel-line);
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
    color: var(--arc-panel-muted);
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
    background: var(--arc-tint);
    border: 1px solid var(--arc-panel-line);
  }

  .pc-stage {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 1.2rem;
    font-weight: 700;
    color: var(--arc-panel-ink);
    text-align: center;
  }

  /* Pipeline node accents mirror the three product accents. */
  .n-ingest .pc-stage { color: var(--pv-blue); }
  .n-preserve .pc-stage { color: var(--pv-indigo); }
  .n-release .pc-stage { color: var(--pv-orange); }

  .pc-kicker {
    font-size: 0.72rem;
    color: var(--arc-panel-muted);
    font-weight: 500;
  }

  .pc-state {
    margin-top: 0.3rem;
    font-size: 0.62rem;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: var(--arc-chip-green-ink);
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
    border-top: 1px solid var(--arc-panel-line);
    flex-wrap: wrap;
  }

  .pc-tag {
    font-size: 0.66rem;
    font-weight: 700;
    color: var(--arc-chip-violet-ink);
    background: rgba(139, 92, 246, 0.14);
    border: 1px solid rgba(139, 92, 246, 0.3);
    border-radius: 1rem;
    padding: 0.2rem 0.6rem;
  }

  .pc-conform {
    margin-left: auto;
    font-size: 0.7rem;
    font-weight: 700;
    color: var(--arc-chip-green-ink);
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

  /* ── Per-stage accent (blue / indigo / orange) ── */
  .ingest { --tone: #3b82f6; }
  .preserve { --tone: #6366f1; }
  .release { --tone: #f97316; }

  /* ── How it works: three steps joined by arrows ── */
  .flow {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 2.75rem;
  }

  .flow-step {
    position: relative;
    padding: 1.75rem 1.5rem 1.5rem;
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1.1rem;
    box-shadow: var(--arc-shadow-card);
    text-align: center;
  }

  /* Arrow to the next step, drawn in the gap between cards. */
  .flow-step:not(:last-child)::after {
    content: '→';
    position: absolute;
    top: 50%;
    right: -2.75rem;
    width: 2.75rem;
    transform: translateY(-50%);
    text-align: center;
    font-size: 1.4rem;
    color: var(--tone);
  }

  .flow-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 4rem;
    height: 4rem;
    border-radius: 1.1rem;
    color: white;
    background: linear-gradient(135deg, color-mix(in srgb, var(--tone) 75%, white), var(--tone));
    box-shadow: 0 14px 28px -12px var(--tone);
  }

  .flow-num {
    display: block;
    margin-top: 1.1rem;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.12em;
    color: var(--tone);
  }

  .flow-step h3 {
    margin: 0.3rem 0 0.45rem;
    font-size: 1.2rem;
    font-weight: 700;
    color: var(--arc-ink);
  }

  .flow-step p {
    margin: 0;
    font-size: 0.92rem;
    line-height: 1.5;
    color: var(--arc-muted);
  }

  /* ── Capabilities as icon tiles ── */
  .tiles {
    list-style: none;
    margin: 2rem 0 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 0.9rem;
  }

  .tile {
    display: flex;
    align-items: center;
    gap: 0.7rem;
    padding: 0.85rem 1rem;
    font-size: 0.92rem;
    font-weight: 600;
    color: var(--arc-body);
    background: var(--arc-card-2);
    border: 1px solid var(--arc-line);
    border-radius: 0.8rem;
  }

  .tile-icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 2.2rem;
    height: 2.2rem;
    border-radius: 0.6rem;
    color: var(--tone);
    background: color-mix(in srgb, var(--tone) 14%, transparent);
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
    background: var(--arc-tint);
    border: 1px solid var(--arc-panel-line);
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
    color: var(--arc-panel-body);
    text-decoration: none;
    transition: background 0.18s ease, color 0.18s ease;
  }

  .stage-tab:hover {
    background: var(--arc-tint);
    color: var(--arc-panel-ink);
  }

  .stage-tab-step {
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    color: var(--arc-panel-muted);
  }

  .stage-tab.current {
    background: var(--arc-tint-strong);
    color: var(--arc-panel-ink);
  }

  .stage-tab.current.ingest .stage-tab-step { color: var(--pv-blue); }
  .stage-tab.current.preserve .stage-tab-step { color: var(--pv-indigo); }
  .stage-tab.current.release .stage-tab-step { color: var(--pv-orange); }

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
    .pipeline-card {
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
    .flow {
      grid-template-columns: 1fr;
      gap: 2.5rem;
      max-width: 26rem;
      margin: 0 auto;
    }
    .flow-step:not(:last-child)::after {
      content: '↓';
      top: auto;
      right: 0;
      bottom: -2.5rem;
      width: 100%;
      height: 2.5rem;
      line-height: 2.5rem;
      transform: none;
    }
    .tiles {
      grid-template-columns: repeat(2, 1fr);
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
    .tiles {
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
  :global(html[data-theme='dark']) .hero {
    --pv-card: linear-gradient(160deg, rgba(30, 41, 59, 0.9), rgba(15, 23, 42, 0.92));
    --pv-shadow: 0 30px 60px -20px rgba(0, 0, 0, 0.7);
    --pv-blue: #60a5fa;
    --pv-indigo: #818cf8;
    --pv-orange: #fb923c;
  }

  :global(html[data-theme='dark']) .chip.premium { border-color: rgba(139, 92, 246, 0.45); }
</style>
