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
  /** Create path prefix: /sip/create, /aip/create, /dip/create */
  export let createPath = '/sip/create';
  /** List path: /sip, /aip, /dip */
  export let listPath = '/sip';
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
    sip: 'sipLabel',
    aip: 'aipLabel',
    dip: 'dipLabel',
  };

  function getTypeLabel(std: StandardDefinition): string {
    return (std[typeLabelKey[packageType]] as string) || '';
  }

  const packageNames: Record<string, string> = {
    sip: 'Submission Information Package',
    aip: 'Archival Information Package',
    dip: 'Dissemination Information Package',
  };

  const packageAbbr: Record<string, string> = {
    sip: 'SIP',
    aip: 'AIP',
    dip: 'DIP',
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
      {#if oaisStep}
        <div class="hero-badge" style="color: {accentColor}; background: color-mix(in srgb, {accentColor} 10%, white);">{oaisStep}</div>
      {/if}
      <h1>{title}</h1>
      <p class="hero-subtitle">{heroDescription}</p>
      <div class="hero-actions">
        <a href="/login" class="btn-cta">Sign In to Create {packageAbbr[packageType]}s</a>
        <a href="#standards" class="btn-outline">View Standards</a>
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
                <span class="badge" style="color: {accentColor}; background: color-mix(in srgb, {accentColor} 10%, white);">{std.label}</span>
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
        <h1>{title}</h1>
        <p class="page-subtitle">Select an archival standard to create a {packageNames[packageType]} ({packageAbbr[packageType]}).</p>
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

  .hero { padding: 4rem 2rem 3rem; }

  .hero-badge {
    display: inline-block;
    padding: 0.4rem 1rem;
    border-radius: 2rem;
    font-size: 0.8rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    margin-bottom: 1.25rem;
  }

  .hero h1 {
    font-size: 3rem;
    font-weight: 800;
    color: #0f172a;
    margin: 0 0 1rem;
    letter-spacing: -0.03em;
  }

  .hero-subtitle {
    font-size: 1.1rem;
    color: #475569;
    max-width: 620px;
    margin: 0 auto 2.5rem;
    line-height: 1.7;
  }

  .hero-actions { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; }

  .btn-cta, .btn-cta-inv {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: linear-gradient(135deg, #3b82f6, #7c3aed);
    color: white;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    transition: transform 0.2s, box-shadow 0.2s;
  }

  .btn-cta:hover, .btn-cta-inv:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(59, 130, 246, 0.35);
  }

  .btn-outline {
    display: inline-block;
    padding: 0.85rem 2rem;
    background: transparent;
    color: #1e293b;
    border: 2px solid #cbd5e1;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 700;
    font-size: 0.95rem;
    transition: border-color 0.2s;
  }

  .btn-outline:hover { border-color: #64748b; }

  .section-title { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0 0 0.5rem; }
  .section-desc { color: #64748b; font-size: 1rem; margin: 0 auto 2rem; max-width: 520px; }

  .how-it-works { margin-bottom: 4rem; }

  .steps-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 1.5rem;
    text-align: left;
    margin-top: 2rem;
  }

  .step-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.75rem;
  }

  .step-number {
    width: 2.25rem; height: 2.25rem;
    border-radius: 50%;
    color: white;
    display: flex; align-items: center; justify-content: center;
    font-weight: 800; font-size: 0.9rem;
    margin-bottom: 1rem;
  }

  .step-card h3 { margin: 0 0 0.5rem; color: #0f172a; font-size: 1.05rem; }
  .step-card p { margin: 0; color: #64748b; font-size: 0.875rem; line-height: 1.55; }

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
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    color: #334155;
  }

  .cap-icon {
    flex-shrink: 0;
    width: 1.5rem; height: 1.5rem;
    background: #dcfce7; color: #16a34a;
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
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
    transition: border-color 0.2s;
  }

  .standard-info-card:hover { border-color: #93c5fd; }

  .card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }

  .badge {
    font-size: 0.7rem; font-weight: 800;
    letter-spacing: 0.08em; text-transform: uppercase;
    padding: 0.25rem 0.625rem; border-radius: 0.25rem;
  }

  .ver { font-size: 0.7rem; color: #94a3b8; font-weight: 600; }

  .standard-info-card h3 { margin: 0 0 0.5rem; font-size: 1rem; font-weight: 700; color: #0f172a; line-height: 1.3; }
  .standard-info-card p { margin: 0 0 0.5rem; color: #64748b; font-size: 0.825rem; line-height: 1.5; }
  .ref { display: block; font-size: 0.7rem; color: #94a3b8; font-family: monospace; }

  .cta-section {
    background: linear-gradient(135deg, #1e293b, #334155);
    color: white;
    padding: 3.5rem 2rem;
    border-radius: 1rem;
    margin-bottom: 2rem;
  }

  .cta-section h2 { margin: 0 0 0.5rem; font-size: 1.75rem; font-weight: 800; }
  .cta-section p { margin: 0 0 2rem; color: #94a3b8; font-size: 1.05rem; }

  .cta-section .btn-cta-inv {
    background: white;
    color: #1e293b;
  }

  .cta-section .btn-cta-inv:hover {
    box-shadow: 0 8px 24px rgba(255, 255, 255, 0.15);
  }

  /* ── Authenticated page ── */
  .manage-page { max-width: 1200px; margin: 0 auto; }

  .page-header {
    display: flex; justify-content: space-between; align-items: flex-start;
    margin-bottom: 2.5rem; gap: 1rem;
  }

  .page-header h1 { margin: 0 0 0.5rem; color: #0f172a; font-size: 2rem; font-weight: 800; }
  .page-subtitle { margin: 0; color: #64748b; font-size: 1.05rem; }

  .btn-secondary {
    display: inline-block;
    padding: 0.65rem 1.25rem;
    background: #f1f5f9; color: #475569;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    text-decoration: none;
    font-weight: 600; font-size: 0.875rem;
    transition: background 0.2s;
    white-space: nowrap;
  }

  .btn-secondary:hover { background: #e2e8f0; }

  .standards-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 1.25rem;
  }

  /* ── Shared ── */
  .loading { display: flex; flex-direction: column; align-items: center; padding: 4rem 2rem; gap: 1rem; }

  .spinner {
    width: 2.5rem; height: 2.5rem;
    border: 3px solid #e2e8f0;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }

  .loading p { color: #64748b; font-size: 0.875rem; }

  @media (max-width: 768px) {
    .standards-grid, .standards-info-grid { grid-template-columns: 1fr; }
    .page-header { flex-direction: column; }
    .hero h1 { font-size: 2.25rem; }
  }
</style>
