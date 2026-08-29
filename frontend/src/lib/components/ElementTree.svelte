<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  /** Array of element objects to render. */
  export let elements: any[] = [];
  /** Current nesting depth (0 = top level). */
  export let depth = 0;
  /** Function that returns available child entity definitions for a given entity name. */
  export let getChildEntityDefs: (entityName: string) => any[] = () => [];
  /** Maximum recursion depth. */
  export let maxDepth = 10;

  const dispatch = createEventDispatcher();

  function onAdd(parentId: string, parentEntityName: string) {
    dispatch('addChild', { parentId, parentEntityName });
  }

  function onDelete(elementId: string, title: string) {
    dispatch('deleteElement', { elementId, title });
  }
</script>

{#each elements as element (element.id)}
  <div class="element-card" class:nested={depth > 0}>
    <div class="element-card-header">
      <div class="element-card-info">
        <span class="element-entity-badge">{element.entityName}</span>
        <strong class="element-title">{element.title}</strong>
        <span class="element-id">({element.elementIdentifier})</span>
      </div>
      <div class="element-card-actions">
        {#if depth < maxDepth && getChildEntityDefs(element.entityName).length > 0}
          <button class="btn-sm btn-add" on:click={() => onAdd(element.id, element.entityName)}>+ Add</button>
        {/if}
        <button class="btn-sm btn-delete" on:click={() => onDelete(element.id, element.title)}>Delete</button>
      </div>
    </div>

    {#if element.description}
      <p class="element-description">{element.description}</p>
    {/if}

    {#if element.fields?.length > 0}
      <div class="element-fields">
        {#each element.fields as field}
          {#if field.value}
            <div class="element-field">
              <span class="ef-label">{field.label || field.name}:</span>
              <span class="ef-value">{field.value}</span>
            </div>
          {/if}
        {/each}
      </div>
    {/if}

    {#if element.children?.length > 0 && depth < maxDepth}
      <div class="nested-children">
        <svelte:self
          elements={element.children}
          depth={depth + 1}
          {getChildEntityDefs}
          {maxDepth}
          on:addChild
          on:deleteElement
        />
      </div>
    {/if}
  </div>
{/each}

<style>
  .element-card {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 0.75rem;
    padding: 1rem;
    margin-bottom: 0.75rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .element-card.nested {
    background: var(--arc-ground, #f8fafc);
    border-color: var(--arc-line-strong, #e2e8f0);
    box-shadow: none;
  }

  .element-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .element-card-info {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .element-entity-badge {
    font-size: 0.65rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--arc-chip-indigo-ink, #4338ca);
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    padding: 0.2rem 0.6rem;
    border-radius: 9999px;
  }

  .element-title {
    color: var(--arc-ink, #0f172a);
    font-size: 0.95rem;
  }

  .element-id {
    color: var(--arc-faint, #94a3b8);
    font-size: 0.75rem;
  }

  .element-card-actions {
    display: flex;
    gap: 0.35rem;
  }

  .btn-sm {
    padding: 0.25rem 0.6rem;
    font-size: 0.75rem;
    border: none;
    border-radius: 0.4rem;
    box-shadow: none;
    transform: none;
    cursor: pointer;
    font-weight: 600;
    transition: background 0.18s ease;
  }

  .btn-add {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
  }

  .btn-add:hover {
    background: var(--arc-chip-indigo-hover, #c7d2fe);
  }

  .btn-delete {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #991b1b);
  }

  .btn-delete:hover {
    background: var(--arc-chip-red-hover, #fecaca);
  }

  .element-description {
    margin: 0.5rem 0 0;
    color: var(--arc-muted, #64748b);
    font-size: 0.825rem;
    line-height: 1.4;
  }

  .element-fields {
    margin-top: 0.5rem;
    display: flex;
    flex-wrap: wrap;
    gap: 0.35rem 1rem;
  }

  .element-field {
    font-size: 0.8rem;
  }

  .ef-label {
    color: var(--arc-muted, #64748b);
    font-weight: 600;
  }

  .ef-value {
    color: var(--arc-ink, #0f172a);
  }

  .nested-children {
    margin-top: 0.75rem;
    padding-left: 1rem;
    border-left: 2px solid var(--arc-hover-border, #c7d2fe);
  }
</style>
