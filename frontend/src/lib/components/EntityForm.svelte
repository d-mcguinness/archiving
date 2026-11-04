<script lang="ts">
  /**
   * Renders the fields of one scheme entity as a form section: the entity's
   * description and note, a grid of required fields, and a collapsible grid
   * of optional fields. Field inputs are typed from the definition (date,
   * number, text). Bind `values` to read the entered data.
   *
   * Every page that used to copy this markup (the stage create pages, the
   * add-child modals, the archive element designer) renders this instead.
   */
  import type { EntityDefinition } from '$lib/schemes';
  import { splitFields } from '$lib/schemes';

  export let entity: EntityDefinition;
  /** name → value; bind it. Missing keys are treated as empty. */
  export let values: Record<string, string> = {};
  /** Whether the optional group is expanded; bind to persist across re-renders. */
  export let showOptional = false;
  /** Prefix for input ids, so two forms on one page don't collide. */
  export let idPrefix = 'field';
  /** Show the entity's description and note above the fields. */
  export let showEntityInfo = true;
  /** Split into Required / Optional groups (false: one flat grid, required marked with *). */
  export let grouped = true;
  /** Mark required inputs as `required` for native form validation. */
  export let nativeRequired = true;
  export let disabled = false;

  $: ({ required, optional } = splitFields(entity));
  $: flat = entity?.fields ?? [];

  function update(name: string, value: string) {
    values[name] = value;
    values = values;
  }
</script>

{#if entity}
  {#if showEntityInfo && entity.description}
    <p class="entity-description">{entity.description}</p>
  {/if}
  {#if showEntityInfo && entity.note}
    <div class="entity-note">{entity.note}</div>
  {/if}

  {#if grouped}
    {#if required.length > 0}
      <div class="fields-group">
        <h3 class="fields-group-title">Required Fields</h3>
        <div class="fields-grid">
          {#each required as field (field.name)}
            <div class="form-group" class:full-width={field.type === 'text'}>
              <label for={`${idPrefix}-${field.name}`}>
                {field.label}
                <span class="req">*</span>
                <span class="field-type-tag">{field.type}</span>
              </label>
              <input
                type={field.type === 'date' ? 'date' : field.type === 'number' ? 'number' : 'text'}
                id={`${idPrefix}-${field.name}`}
                value={values[field.name] || ''}
                required={nativeRequired}
                {disabled}
                placeholder={field.type === 'date' ? undefined : `Enter ${field.label}`}
                on:input={(e) => update(field.name, e.currentTarget.value)}
              />
            </div>
          {/each}
        </div>
      </div>
    {/if}

    {#if optional.length > 0}
      <div class="fields-group optional-group">
        <button
          type="button"
          class="fields-group-toggle"
          aria-expanded={showOptional}
          on:click={() => (showOptional = !showOptional)}
        >
          <h3 class="fields-group-title">Optional Fields ({optional.length})</h3>
          <span class="toggle-icon">{showOptional ? 'v' : '>'}</span>
        </button>

        {#if showOptional}
          <div class="fields-grid">
            {#each optional as field (field.name)}
              <div class="form-group" class:full-width={field.type === 'text'}>
                <label for={`${idPrefix}-${field.name}`}>
                  {field.label}
                  <span class="field-type-tag">{field.type}</span>
                </label>
                <input
                  type={field.type === 'date' ? 'date' : field.type === 'number' ? 'number' : 'text'}
                  id={`${idPrefix}-${field.name}`}
                  value={values[field.name] || ''}
                  {disabled}
                  placeholder={field.type === 'date' ? undefined : `Enter ${field.label}`}
                  on:input={(e) => update(field.name, e.currentTarget.value)}
                />
              </div>
            {/each}
          </div>
        {/if}
      </div>
    {/if}
  {:else}
    <div class="fields-grid">
      {#each flat as field (field.name)}
        <div class="form-group" class:full-width={field.type === 'text'}>
          <label for={`${idPrefix}-${field.name}`}>
            {field.label || field.name}
            {#if field.required}<span class="req">*</span>{/if}
            <span class="field-type-tag">{field.type}</span>
          </label>
          <input
            type={field.type === 'date' ? 'date' : field.type === 'number' ? 'number' : 'text'}
            id={`${idPrefix}-${field.name}`}
            value={values[field.name] || ''}
            required={nativeRequired && field.required}
            {disabled}
            placeholder={field.type === 'date' ? undefined : `Enter ${field.label || field.name}`}
            on:input={(e) => update(field.name, e.currentTarget.value)}
          />
        </div>
      {/each}
    </div>
  {/if}
{/if}

<style>
  .entity-description {
    margin: 0 0 1rem 0;
    color: var(--arc-body);
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .entity-note {
    padding: 0.75rem 1rem;
    background: var(--arc-alert-amber-bg);
    border: 1px solid var(--arc-alert-amber-border);
    border-radius: 0.375rem;
    margin-bottom: 1.5rem;
    font-size: 0.8rem;
    color: var(--arc-alert-amber-ink);
    line-height: 1.5;
  }

  .fields-group {
    margin-bottom: 1.5rem;
  }

  .fields-group-title {
    margin: 0 0 1rem 0;
    color: var(--arc-body);
    font-size: 0.925rem;
    font-weight: 600;
  }

  .fields-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 1rem;
  }

  .fields-grid .full-width {
    grid-column: 1 / -1;
  }

  .form-group {
    margin-bottom: 1.25rem;
  }

  .form-group label {
    display: flex;
    align-items: center;
    gap: 0.375rem;
    margin-bottom: 0.5rem;
    color: var(--arc-ink);
    font-weight: 500;
    font-size: 0.85rem;
  }

  .req {
    color: #ef4444;
    font-weight: 600;
  }

  .field-type-tag {
    font-size: 0.65rem;
    color: var(--arc-faint);
    font-weight: 400;
    font-style: italic;
    margin-left: auto;
  }

  /* Input chrome (width, border, radius, focus ring) comes from app.css. */
  .form-group input {
    padding: 0.625rem 0.75rem;
    font-size: 0.875rem;
  }

  .optional-group {
    border: 1px solid var(--arc-line-strong);
    border-radius: 0.5rem;
    padding: 1rem;
    background: var(--arc-card-2);
  }

  .fields-group-toggle {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    margin-bottom: 0;
    color: inherit;
    box-shadow: none;
  }

  .fields-group-toggle:hover {
    background: none;
    transform: none;
    box-shadow: none;
  }

  .fields-group-toggle .fields-group-title {
    margin: 0;
    color: var(--arc-muted);
  }

  .toggle-icon {
    font-size: 1rem;
    color: var(--arc-faint);
    transition: transform 0.2s;
  }

  @media (max-width: 768px) {
    .fields-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
