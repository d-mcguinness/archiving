<script lang="ts">
  export let label = '';
  export let fullName = '';
  export let description = '';
  export let reference = '';
  export let version = '';
  export let href = '';
  export let typeLabel = '';
  /** Accent color for hover state. */
  export let accentColor = '#3b82f6';
</script>

<a {href} class="standard-card" style="--accent: {accentColor}">
  <div class="card-header">
    <span class="standard-badge">{label}</span>
    {#if version}
      <span class="version">v{version}</span>
    {/if}
  </div>
  <h2>{fullName || label}</h2>
  <p class="card-description">{description}</p>
  {#if reference}
    <span class="card-reference">{reference}</span>
  {/if}
  <div class="card-footer">
    <span class="type-label">{typeLabel}</span>
    <span class="card-arrow">&rarr;</span>
  </div>
</a>

<style>
  .standard-card {
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
    padding: 1.5rem;
    text-decoration: none;
    color: inherit;
    display: flex;
    flex-direction: column;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
  }

  .standard-card:hover {
    border-color: color-mix(in srgb, var(--accent) 45%, var(--arc-card));
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
    transform: translateY(-4px);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
  }

  .standard-badge {
    font-size: 0.72rem;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--accent);
    background: color-mix(in srgb, var(--accent) 10%, var(--arc-card));
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
  }

  .version {
    font-size: 0.7rem;
    color: var(--arc-faint);
    font-weight: 600;
  }

  h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.1rem;
    font-weight: 700;
    color: var(--arc-ink);
    line-height: 1.3;
  }

  .card-description {
    margin: 0 0 0.75rem 0;
    color: var(--arc-muted);
    font-size: 0.85rem;
    line-height: 1.5;
    flex: 1;
  }

  .card-reference {
    display: block;
    font-size: 0.7rem;
    color: var(--arc-faint);
    margin-bottom: 1rem;
    font-family: monospace;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 0.75rem;
    border-top: 1px solid var(--arc-line);
  }

  .type-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: var(--arc-body);
  }

  .card-arrow {
    font-size: 1.25rem;
    color: var(--arc-faint);
    transition: color 0.2s, transform 0.2s;
  }

  .standard-card:hover .card-arrow {
    color: var(--accent);
    transform: translateX(4px);
  }

  @media (prefers-reduced-motion: reduce) {
    .standard-card, .card-arrow {
      transition: none;
    }
    .standard-card:hover,
    .standard-card:hover .card-arrow {
      transform: none;
    }
  }
</style>
