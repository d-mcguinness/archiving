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
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
    text-decoration: none;
    color: inherit;
    display: flex;
    flex-direction: column;
    transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  }

  .standard-card:hover {
    border-color: var(--accent);
    box-shadow: 0 4px 12px color-mix(in srgb, var(--accent) 12%, transparent);
    transform: translateY(-2px);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.75rem;
  }

  .standard-badge {
    font-size: 0.7rem;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: var(--accent);
    background: color-mix(in srgb, var(--accent) 10%, white);
    padding: 0.25rem 0.625rem;
    border-radius: 0.25rem;
  }

  .version {
    font-size: 0.7rem;
    color: #94a3b8;
    font-weight: 600;
  }

  h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.1rem;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.3;
  }

  .card-description {
    margin: 0 0 0.75rem 0;
    color: #64748b;
    font-size: 0.85rem;
    line-height: 1.5;
    flex: 1;
  }

  .card-reference {
    display: block;
    font-size: 0.7rem;
    color: #94a3b8;
    margin-bottom: 1rem;
    font-family: monospace;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 0.75rem;
    border-top: 1px solid #f1f5f9;
  }

  .type-label {
    font-size: 0.75rem;
    font-weight: 600;
    color: #475569;
  }

  .card-arrow {
    font-size: 1.25rem;
    color: #cbd5e1;
    transition: color 0.2s, transform 0.2s;
  }

  .standard-card:hover .card-arrow {
    color: var(--accent);
    transform: translateX(4px);
  }
</style>
