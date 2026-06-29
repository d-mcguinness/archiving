<script lang="ts">
  // Shared brand chrome for the /login and /register pages so both match the
  // landing/product marketing system (Space Grotesk + Inter, slate-dark hero,
  // aurora, indigo→violet→cyan accents). Pages supply their form via the slot;
  // branded form-control classes (.auth-field, .auth-input, .auth-submit, …)
  // are defined here and applied to slotted markup via scoped :global.

  /** Heading on the form side, e.g. "Welcome back". */
  export let title = '';
  /** Supporting line under the heading. */
  export let subtitle = '';

  const pipeline = [
    { stage: 'Intake', kicker: 'Ingest' },
    { stage: 'Preservation', kicker: 'Preserve' },
    { stage: 'Release', kicker: 'Release' }
  ];

  const highlights = [
    'Validated NOARK 5.5 & E-ARK conformance, built in',
    'Ingest → Preservation → Release in one workflow',
    'Secure by design — signed, per-tenant sessions'
  ];
</script>

<svelte:head>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin="anonymous" />
  <link
    href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600;700&display=swap"
    rel="stylesheet"
  />
</svelte:head>

<div class="auth-shell">
  <!-- ── Brand / marketing panel ── -->
  <aside class="auth-brand">
    <div class="auth-aurora" aria-hidden="true"></div>
    <div class="auth-grid-bg" aria-hidden="true"></div>

    <div class="auth-brand-inner">
      <a href="/" class="auth-wordmark">Arcana</a>

      <div class="auth-brand-body">
        <span class="auth-eyebrow">Open-standard digital preservation</span>
        <h2 class="auth-brand-head">
          Preserve anything.<br />
          <span class="auth-grad">Prove everything.</span>
        </h2>

        <ul class="auth-highlights">
          {#each highlights as h}
            <li><span class="auth-check" aria-hidden="true">✓</span>{h}</li>
          {/each}
        </ul>

        <div class="auth-pipeline" aria-hidden="true">
          {#each pipeline as p, i}
            <div class="auth-pnode n-{i}">
              <span class="auth-pstage">{p.stage}</span>
              <span class="auth-pkicker">{p.kicker}</span>
            </div>
            {#if i < pipeline.length - 1}<span class="auth-pconn">→</span>{/if}
          {/each}
        </div>
      </div>

      <div class="auth-brand-foot">
        <span class="auth-tag">NOARK 5.5</span>
        <span class="auth-tag">E-ARK</span>
        <span class="auth-tag">OAIS</span>
        <span class="auth-conform"><span aria-hidden="true">●</span> conformant</span>
      </div>
    </div>
  </aside>

  <!-- ── Form panel ── -->
  <main class="auth-main">
    <div class="auth-card">
      <a href="/" class="auth-mobile-wordmark">Arcana</a>
      <h1 class="auth-title">{title}</h1>
      {#if subtitle}<p class="auth-subtitle">{subtitle}</p>{/if}
      <slot />
    </div>
  </main>
</div>

<style>
  /* The full-bleed shell spans the viewport and fills the space between the
     app nav and footer; guard against scrollbar-gutter horizontal overflow. */
  :global(body) {
    overflow-x: hidden;
  }

  .auth-shell {
    width: 100vw;
    margin-left: calc(50% - 50vw);
    margin-right: calc(50% - 50vw);
    margin-top: -2rem; /* cancel <main> top padding so it meets the nav */
    margin-bottom: -2rem; /* meet the footer */
    min-height: calc(100vh - 7rem);
    display: grid;
    grid-template-columns: 1.02fr 0.98fr;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  }

  /* ── Brand panel ── */
  .auth-brand {
    position: relative;
    overflow: hidden;
    background: radial-gradient(120% 120% at 30% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%);
    color: #e2e8f0;
    display: flex;
  }

  .auth-aurora {
    position: absolute;
    inset: -20% -10% auto -10%;
    height: 90%;
    background:
      radial-gradient(40% 60% at 22% 28%, rgba(99, 102, 241, 0.45), transparent 70%),
      radial-gradient(40% 60% at 75% 18%, rgba(139, 92, 246, 0.42), transparent 70%),
      radial-gradient(35% 50% at 60% 70%, rgba(6, 182, 212, 0.3), transparent 70%);
    filter: blur(30px);
    pointer-events: none;
  }

  .auth-grid-bg {
    position: absolute;
    inset: 0;
    background-image:
      linear-gradient(rgba(148, 163, 184, 0.07) 1px, transparent 1px),
      linear-gradient(90deg, rgba(148, 163, 184, 0.07) 1px, transparent 1px);
    background-size: 44px 44px;
    mask-image: radial-gradient(circle at 35% 30%, black, transparent 78%);
    pointer-events: none;
  }

  .auth-brand-inner {
    position: relative;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    gap: 2.5rem;
    padding: 3rem 3.25rem;
    width: 100%;
    max-width: 30rem;
    margin-left: auto;
  }

  .auth-wordmark {
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    font-size: 1.5rem;
    font-weight: 700;
    color: #f8fafc;
    text-decoration: none;
    letter-spacing: -0.02em;
  }

  .auth-eyebrow {
    display: inline-block;
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
    color: #a5b4fc;
    margin-bottom: 1rem;
  }

  .auth-brand-head {
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    font-size: clamp(2rem, 3vw, 2.7rem);
    font-weight: 700;
    line-height: 1.06;
    letter-spacing: -0.02em;
    margin: 0 0 1.75rem;
    color: #f8fafc;
  }

  .auth-grad {
    background: linear-gradient(110deg, #818cf8 0%, #c084fc 45%, #22d3ee 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .auth-highlights {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    gap: 0.85rem;
  }

  .auth-highlights li {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    font-size: 0.95rem;
    color: #cbd5e1;
    font-weight: 500;
  }

  .auth-check {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 1.4rem;
    height: 1.4rem;
    border-radius: 50%;
    background: linear-gradient(135deg, #34d399, #10b981);
    color: #052e16;
    font-size: 0.75rem;
    font-weight: 800;
  }

  .auth-pipeline {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin-top: 2rem;
  }

  .auth-pnode {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    padding: 0.7rem 0.6rem;
    border-radius: 0.65rem;
    background: rgba(148, 163, 184, 0.07);
    border: 1px solid rgba(148, 163, 184, 0.16);
  }

  .auth-pstage {
    font-family: 'Space Grotesk', sans-serif;
    font-size: 0.95rem;
    font-weight: 700;
    color: #f8fafc;
  }

  .auth-pnode.n-0 .auth-pstage { color: #60a5fa; }
  .auth-pnode.n-1 .auth-pstage { color: #818cf8; }
  .auth-pnode.n-2 .auth-pstage { color: #fb923c; }

  .auth-pkicker {
    font-size: 0.68rem;
    color: #94a3b8;
    font-weight: 500;
  }

  .auth-pconn {
    color: #475569;
    font-weight: 700;
    flex-shrink: 0;
  }

  .auth-brand-foot {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    flex-wrap: wrap;
  }

  .auth-tag {
    font-size: 0.66rem;
    font-weight: 700;
    color: #c4b5fd;
    background: rgba(139, 92, 246, 0.14);
    border: 1px solid rgba(139, 92, 246, 0.3);
    border-radius: 1rem;
    padding: 0.2rem 0.6rem;
  }

  .auth-conform {
    margin-left: auto;
    font-size: 0.7rem;
    font-weight: 700;
    color: #34d399;
  }

  /* ── Form panel ── */
  .auth-main {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 3rem 1.5rem;
    background: #f8fafc;
  }

  .auth-card {
    width: 100%;
    max-width: 27rem;
    background: white;
    border: 1px solid #e8edf3;
    border-radius: 1.1rem;
    box-shadow: 0 24px 60px -28px rgba(15, 23, 42, 0.35);
    padding: 2.75rem;
  }

  .auth-mobile-wordmark {
    display: none;
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    font-size: 1.4rem;
    font-weight: 700;
    color: #0f172a;
    text-decoration: none;
    letter-spacing: -0.02em;
    margin-bottom: 1.5rem;
  }

  .auth-title {
    font-family: 'Space Grotesk', 'Inter', sans-serif;
    font-size: 1.7rem;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #0f172a;
    margin: 0 0 0.5rem;
  }

  .auth-subtitle {
    margin: 0 0 1.9rem;
    color: #64748b;
    font-size: 0.98rem;
    line-height: 1.55;
  }

  /* ── Branded form controls (applied to slotted page markup) ── */
  .auth-shell :global(.auth-field) {
    margin-bottom: 1.25rem;
  }

  .auth-shell :global(.auth-row) {
    display: flex;
    gap: 1rem;
  }

  .auth-shell :global(.auth-row .auth-field) {
    flex: 1;
  }

  .auth-shell :global(.auth-field label) {
    display: block;
    margin-bottom: 0.45rem;
    font-weight: 600;
    font-size: 0.875rem;
    color: #334155;
  }

  .auth-shell :global(.auth-input) {
    width: 100%;
    padding: 0.75rem 1rem;
    border: 1.5px solid #e2e8f0;
    border-radius: 0.6rem;
    font-size: 0.97rem;
    font-family: inherit;
    color: #0f172a;
    background: #fff;
    transition: border-color 0.18s ease, box-shadow 0.18s ease;
    box-sizing: border-box;
  }

  .auth-shell :global(.auth-input::placeholder) {
    color: #94a3b8;
  }

  .auth-shell :global(.auth-input:focus) {
    outline: none;
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.16);
  }

  .auth-shell :global(.auth-input:disabled) {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .auth-shell :global(.auth-error) {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.85rem 1rem;
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 0.6rem;
    color: #991b1b;
    font-size: 0.9rem;
    margin-bottom: 1.4rem;
  }

  .auth-shell :global(.auth-submit) {
    width: 100%;
    padding: 0.85rem 1.5rem;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: white;
    border: none;
    border-radius: 0.65rem;
    font-size: 1rem;
    font-weight: 700;
    font-family: inherit;
    cursor: pointer;
    transition: transform 0.18s ease, box-shadow 0.18s ease;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    box-shadow: 0 10px 30px -10px rgba(124, 58, 237, 0.6);
  }

  .auth-shell :global(.auth-submit:hover:not(:disabled)) {
    transform: translateY(-2px);
    box-shadow: 0 16px 40px -10px rgba(124, 58, 237, 0.7);
  }

  .auth-shell :global(.auth-submit:disabled) {
    opacity: 0.7;
    cursor: not-allowed;
  }

  .auth-shell :global(.auth-spinner) {
    width: 1.15rem;
    height: 1.15rem;
    border: 3px solid rgba(255, 255, 255, 0.35);
    border-top-color: white;
    border-radius: 50%;
    animation: auth-spin 0.8s linear infinite;
  }

  @keyframes -global-auth-spin {
    to { transform: rotate(360deg); }
  }

  .auth-shell :global(.auth-alt) {
    text-align: center;
    margin: 1.6rem 0 0;
    color: #64748b;
    font-size: 0.9rem;
  }

  .auth-shell :global(.auth-alt a) {
    color: #6366f1;
    font-weight: 600;
    text-decoration: none;
  }

  .auth-shell :global(.auth-alt a:hover) {
    text-decoration: underline;
  }

  /* ── Demo accounts (login, dev only) ── */
  .auth-shell :global(.auth-demo) {
    margin-top: 1.6rem;
    padding-top: 1.5rem;
    border-top: 1px solid #eef2f7;
  }

  .auth-shell :global(.auth-demo-title) {
    margin: 0 0 0.75rem;
    text-align: center;
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: #94a3b8;
  }

  .auth-shell :global(.auth-demo-grid) {
    display: flex;
    gap: 0.5rem;
  }

  .auth-shell :global(.auth-demo-card) {
    flex: 1;
    background: #f8fafc;
    padding: 0.6rem;
    border-radius: 0.5rem;
    border: 1px solid #e2e8f0;
    transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
    cursor: pointer;
    text-align: center;
    font-family: inherit;
    font-size: 0.8rem;
    font-weight: 700;
    color: #475569;
  }

  .auth-shell :global(.auth-demo-card:hover:not(:disabled)) {
    border-color: #818cf8;
    background: #eef2ff;
    color: #4f46e5;
    transform: translateY(-1px);
  }

  .auth-shell :global(.auth-demo-card:disabled) {
    opacity: 0.6;
    cursor: not-allowed;
  }

  /* ── Responsive: stack, brand becomes a slim top band ── */
  @media (max-width: 860px) {
    .auth-shell {
      grid-template-columns: 1fr;
      min-height: calc(100vh - 7rem);
    }
    .auth-brand {
      display: none;
    }
    .auth-main {
      padding: 2.5rem 1.25rem;
    }
    .auth-mobile-wordmark {
      display: inline-block;
    }
  }

  @media (max-width: 480px) {
    .auth-card {
      padding: 2rem 1.4rem;
      box-shadow: none;
      border-color: #e8edf3;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .auth-shell :global(.auth-spinner) {
      animation: none;
    }
  }
</style>