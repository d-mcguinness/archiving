<script lang="ts">
  import { onMount } from 'svelte';
  import { auth } from '$lib/stores/authStore';
  import { standards, type StandardDefinition } from '$lib/standards';
  import StandardCard from './StandardCard.svelte';

  /** Page title: "Ingest", "Preserve", "Deliver" */
  export let title = '';
  /** OAIS step badge text */
  export let oaisStep = '';
  /** Description for unauthenticated hero */
  export let heroDescription = '';
  /** Package type key to derive labels and links */
  export let packageType: 'sip' | 'aip' | 'dip' = 'sip';
  /** Create path prefix: /intake/create, /preservation/create, /release/create */
  export let createPath = '/intake/create';
  /** List path: /intake, /preservation, /release */
  export let listPath = '/intake';
  /** Accent color */
  export let accentColor = '#3b82f6';
  /** How-it-works steps for public page */
  export let steps: { title: string; description: string }[] = [];
  /** Capabilities for public page */
  export let capabilities: string[] = [];

  interface SchemaInfo {
    fullName: string;
    description: string;
    reference: string;
    version: string;
  }

  let schemas: Record<string, SchemaInfo> = {};
  let loading = true;

  const typeLabelKey: Record<string, keyof StandardDefinition> = {
    sip: 'intakeLabel',
    aip: 'preservationLabel',
    dip: 'releaseLabel',
  };

  function getTypeLabel(std: StandardDefinition): string {
    return (std[typeLabelKey[packageType]] as string) || '';
  }

  const packageNames: Record<string, string> = {
    sip: 'Intake package',
    aip: 'Preservation package',
    dip: 'Release package',
  };

  const packageAbbr: Record<string, string> = {
    sip: 'Intake',
    aip: 'Preservation',
    dip: 'Release',
  };

  onMount(async () => {
    const entries = await Promise.all(
      standards.map(async (s) => {
        try {
          const res = await fetch(`/schemeDefintions/${s.file}`);
          if (!res.ok) return null;
          const data = await res.json();
          return [s.key, {
            fullName: data.fullName,
            description: data.description,
            reference: data.reference,
            version: data.version,
          }] as [string, SchemaInfo];
        } catch {
          return null;
        }
      })
    );
    for (const entry of entries) {
      if (entry) schemas[entry[0]] = entry[1];
    }
    schemas = schemas;
    loading = false;
  });
</script>

{#if !$auth.isLoggedIn}
  <!-- PUBLIC INFO PAGE -->
  <div class="public-page">
    <section class="hero">
      <div class="hero-aurora" aria-hidden="true"></div>
      <div class="hero-inner">
        {#if oaisStep}
          <div class="hero-badge" style="color: color-mix(in srgb, {accentColor} 45%, white); background: color-mix(in srgb, {accentColor} 22%, transparent);">{oaisStep}</div>
        {/if}
        <h1>{title}</h1>
        <p class="hero-subtitle">{heroDescription}</p>
        <div class="hero-actions">
          <a href="/login" class="btn-cta">Sign In to Create {packageAbbr[packageType]}s</a>
          <a href="#standards" class="btn-outline">View Standards</a>
        </div>
      </div>
    </section>

    {#if steps.length > 0}
      <section class="how-it-works">
        <h2 class="section-title">How {title} Works</h2>
        <div class="steps-grid">
          {#each steps as step, i}
            <div class="step-card">
              <div class="step-number" style="background: linear-gradient(135deg, {accentColor}, color-mix(in srgb, {accentColor} 70%, #7c3aed));">{i + 1}</div>
              <h3>{step.title}</h3>
              <p>{step.description}</p>
            </div>
          {/each}
        </div>
      </section>
    {/if}

    {#if capabilities.length > 0}
      <section class="capabilities">
        <div class="cap-grid">
          {#each capabilities as cap}
            <div class="cap-item">
              <span class="cap-icon">&#10003;</span>
              <span>{cap}</span>
            </div>
          {/each}
        </div>
      </section>
    {/if}

    <section id="standards" class="standards-section">
      <h2 class="section-title">Supported Standards</h2>
      <p class="section-desc">Arcana supports all major archival standards for {packageNames[packageType]} creation.</p>

      {#if loading}
        <div class="loading"><div class="spinner" style="border-top-color: {accentColor};"></div></div>
      {:else}
        <div class="standards-info-grid">
          {#each standards as std}
            {@const info = schemas[std.key]}
            <div class="standard-info-card">
              <div class="card-top">
                <span class="badge" style="color: {accentColor}; background: color-mix(in srgb, {accentColor} 10%, var(--arc-card));">{std.label}</span>
                {#if info?.version}<span class="ver">v{info.version}</span>{/if}
              </div>
              <h3>{info?.fullName || std.label}</h3>
              <p>{info?.description || ''}</p>
              {#if info?.reference}<span class="ref">{info.reference}</span>{/if}
            </div>
          {/each}
        </div>
      {/if}
    </section>

    <section class="cta-section">
      <h2>Ready to start?</h2>
      <p>Sign in to create your first {packageNames[packageType]}.</p>
      <a href="/login" class="btn-cta-inv">Get Started</a>
    </section>
  </div>

{:else}
  <!-- AUTHENTICATED MANAGEMENT PAGE -->
  <div class="manage-page">
    <div class="page-header">
      <div>
        {#if oaisStep}<span class="eyebrow">{oaisStep}</span>{/if}
        <h1>{title}</h1>
        <p class="page-subtitle">Select an archival standard to create {packageNames[packageType] === 'Intake package' ? 'an' : 'a'} {packageNames[packageType]}.</p>
      </div>
      <a href={listPath} class="btn-secondary">View All {packageAbbr[packageType]}s</a>
    </div>

    {#if loading}
      <div class="loading">
        <div class="spinner" style="border-top-color: {accentColor};"></div>
        <p>Loading standards...</p>
      </div>
    {:else}
      <div class="standards-grid">
        {#each standards as std}
          {@const info = schemas[std.key]}
          <StandardCard
            label={std.label}
            fullName={info?.fullName || std.label}
            description={info?.description || `Create a ${packageAbbr[packageType]} using the ${std.label} standard.`}
            reference={info?.reference || ''}
            version={info?.version || ''}
            href="{createPath}?standard={encodeURIComponent(std.key)}"
            typeLabel={getTypeLabel(std)}
            {accentColor}
          />
        {/each}
      </div>
    {/if}
  </div>
{/if}

<style>
  /* ── Public page ── */
  .public-page { text-align: center; }

  .hero {
    position: relative;
    overflow: hidden;
    background: var(--arc-grad-dark, radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%));
    border: 1px solid var(--arc-line);
    border-radius: 1.25rem;
    padding: 4.5rem 2rem 4rem;
    margin-bottom: 3rem;
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

  .hero-inner { position: relative; }

  .hero-badge {
    display: inline-block;
    padding: 0.4rem 1rem;
    border-radius: 9999px;
    font-size: 0.78rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.14em;
    margin-bottom: 1.25rem;
  }

  .hero h1 {
    font-size: 3rem;
    font-weight: 700;
    color: #f8fafc;
    margin: 0 0 1rem;
  }

  .hero-subtitle {
    font-size: 1.1rem;
    color: #cbd5e1;
    max-width: 620px;
    margin: 0 auto 2.5rem;
    line-height: 1.7;
  }

  .hero-actions { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; }

  .btn-cta, .btn-cta-inv {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6));
    color: white;
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    box-shadow: var(--arc-shadow-btn, 0 10px 30px -8px rgba(124, 58, 237, 0.6));
    transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
  }

  .btn-cta:hover, .btn-cta-inv:hover {
    background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed));
    transform: translateY(-2px);
    box-shadow: var(--arc-shadow-btn-hover, 0 16px 40px -8px rgba(124, 58, 237, 0.75));
  }

  .btn-outline {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: rgba(255, 255, 255, 0.06);
    color: #e2e8f0;
    border: 1px solid rgba(148, 163, 184, 0.4);
    border-radius: 0.65rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
  }

  .btn-outline:hover {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(226, 232, 240, 0.7);
    transform: translateY(-2px);
  }

  .section-title { font-size: 1.75rem; font-weight: 700; color: var(--arc-ink); margin: 0 0 0.5rem; }
  .section-desc { color: var(--arc-muted); font-size: 1rem; margin: 0 auto 2rem; max-width: 520px; }

  .how-it-works { margin-bottom: 4rem; }

  .steps-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 1.5rem;
    text-align: left;
    margin-top: 2rem;
  }

  .step-card {
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
    padding: 1.75rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  }

  .step-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  .step-number {
    width: 2.25rem; height: 2.25rem;
    border-radius: 50%;
    color: white;
    display: flex; align-items: center; justify-content: center;
    font-weight: 800; font-size: 0.9rem;
    margin-bottom: 1rem;
  }

  .step-card h3 { margin: 0 0 0.5rem; color: var(--arc-ink); font-size: 1.05rem; }
  .step-card p { margin: 0; color: var(--arc-muted); font-size: 0.875rem; line-height: 1.55; }

  .capabilities { margin-bottom: 4rem; }

  .cap-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 1rem;
    text-align: left;
  }

  .cap-item {
    display: flex; gap: 0.75rem; align-items: center;
    padding: 1rem;
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 0.75rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    font-size: 0.875rem;
    color: var(--arc-body);
  }

  .cap-icon {
    flex-shrink: 0;
    width: 1.5rem; height: 1.5rem;
    background: var(--arc-chip-green-bg); color: var(--arc-chip-green-ink);
    border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    font-size: 0.75rem; font-weight: 800;
  }

  .standards-section { margin-bottom: 4rem; }

  .standards-info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 1.25rem;
    text-align: left;
  }

  .standard-info-card {
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
    padding: 1.5rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  }

  .standard-info-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  .card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }

  /* Global .badge kit + wider tracking for the standards pills. */
  .badge { letter-spacing: 0.08em; }

  .ver { font-size: 0.7rem; color: var(--arc-faint); font-weight: 600; }

  .standard-info-card h3 { margin: 0 0 0.5rem; font-size: 1rem; font-weight: 700; color: var(--arc-ink); line-height: 1.3; }
  .standard-info-card p { margin: 0 0 0.5rem; color: var(--arc-muted); font-size: 0.825rem; line-height: 1.5; }
  .ref { display: block; font-size: 0.7rem; color: var(--arc-faint); font-family: monospace; }

  .cta-section {
    background: var(--arc-grad-dark, radial-gradient(120% 120% at 50% -10%, #1e293b 0%, #0b1120 55%, #070b16 100%));
    color: #f8fafc;
    padding: 3.5rem 2rem;
    border: 1px solid var(--arc-line);
    border-radius: 1.25rem;
    margin-bottom: 2rem;
  }

  .cta-section h2 { margin: 0 0 0.5rem; font-size: 1.75rem; font-weight: 700; color: #f8fafc; }
  .cta-section p { margin: 0 0 2rem; color: #cbd5e1; font-size: 1.05rem; }

  .cta-section .btn-cta-inv {
    background: white;
    color: #1e293b;
    box-shadow: 0 10px 30px -8px rgba(0, 0, 0, 0.4);
  }

  .cta-section .btn-cta-inv:hover {
    background: white;
    box-shadow: 0 16px 40px -8px rgba(0, 0, 0, 0.5);
  }

  /* ── Authenticated page ── */
  .manage-page { max-width: 1200px; margin: 0 auto; }

  .page-header {
    display: flex; justify-content: space-between; align-items: flex-start;
    margin-bottom: 2.5rem; gap: 1rem;
  }

  .page-header h1 { margin: 0 0 0.5rem; color: var(--arc-ink); font-size: 2rem; font-weight: 700; }
  .page-subtitle { margin: 0; color: var(--arc-muted); font-size: 1.05rem; }

  /* Global .btn-secondary kit + a compact header-sized variant. */
  .btn-secondary {
    padding: 0.65rem 1.25rem;
    font-size: 0.875rem;
    white-space: nowrap;
  }

  .standards-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 1.25rem;
  }

  /* ── Shared ── */
  /* Global .loading / .spinner kit + this page's stacked, larger, slower variant.
     The spinner's border-top colour is set inline from accentColor. */
  .loading { flex-direction: column; padding: 4rem 2rem; gap: 1rem; }

  .spinner { width: 2.5rem; height: 2.5rem; animation-duration: 1s; }

  .loading p { color: var(--arc-muted); font-size: 0.875rem; }

  @media (max-width: 768px) {
    .standards-grid, .standards-info-grid { grid-template-columns: 1fr; }
    .page-header { flex-direction: column; }
    .hero h1 { font-size: 2.25rem; }
  }

  @media (prefers-reduced-motion: reduce) {
    .btn-cta, .btn-cta-inv, .btn-outline, .btn-secondary,
    .step-card, .standard-info-card {
      transition: none;
    }
    .btn-cta:hover, .btn-cta-inv:hover, .btn-outline:hover, .btn-secondary:hover,
    .step-card:hover, .standard-info-card:hover {
      transform: none;
    }
  }
</style>
