<script lang="ts">
  export let elementNode: any;
  export let level: number = 0;
  export let onAddChild: ((parent: any) => void) | null = null;
  export let onDelete: ((id: string) => void) | null = null;
  export let onEdit: ((element: any) => void) | null = null;
  export let readonly: boolean = false;
</script>

<div class="element-node" style="margin-left: {level * 2}rem">
  <div class="element-header">
    <div class="element-info">
      <span class="element-icon">
        {#if elementNode.children && elementNode.children.length > 0}
          📁
        {:else}
          📄
        {/if}
      </span>
      <div class="element-details">
        <div class="element-title">{elementNode.title}</div>
        <div class="element-meta">
          <span class="element-id">{elementNode.elementIdentifier}</span>
          <span class="element-type">
            {elementNode.norwegianName || elementNode.entityName || elementNode.scheme?.norwegianName || elementNode.scheme?.entityName}
          </span>
          {#if elementNode.status}
            <span class="element-status status-{elementNode.status.toLowerCase()}">{elementNode.status}</span>
          {/if}
        </div>
      </div>
    </div>
    {#if !readonly && (onAddChild || onDelete || onEdit)}
      <div class="element-actions">
        {#if onEdit}
          <button
            class="btn-icon btn-edit"
            on:click={() => onEdit(elementNode)}
            title="Edit element fields"
          >
            ✏️
          </button>
        {/if}
        {#if onAddChild}
          <button
            class="btn-icon"
            on:click={() => onAddChild(elementNode)}
            title="Add child element"
          >
            ➕
          </button>
        {/if}
        {#if onDelete}
          <button
            class="btn-icon btn-danger"
            on:click={() => onDelete(elementNode.id || elementNode.tempId)}
            title="Delete element"
          >
            🗑️
          </button>
        {/if}
      </div>
    {/if}
  </div>
  {#if elementNode.description}
    <div class="element-description">{elementNode.description}</div>
  {/if}
  {#if elementNode.children && elementNode.children.length > 0}
    <div class="element-children">
      {#each elementNode.children as child}
        <svelte:self elementNode={child} level={level + 1} {onAddChild} {onDelete} {onEdit} {readonly} />
      {/each}
    </div>
  {/if}
</div>

<style>
  .element-node {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 0.75rem;
    padding: 0.75rem;
    margin-bottom: 0.75rem;
    transition: border-color 0.18s ease;
  }

  .element-node:hover {
    border-color: var(--arc-hover-border, #c7d2fe);
  }

  .element-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 0.75rem;
  }

  .element-info {
    display: flex;
    gap: 0.75rem;
    align-items: flex-start;
    flex: 1;
  }

  .element-icon {
    font-size: 1.25rem;
    flex-shrink: 0;
  }

  .element-details {
    flex: 1;
  }

  .element-title {
    font-weight: 600;
    color: var(--arc-ink, #0f172a);
    margin-bottom: 0.25rem;
  }

  .element-meta {
    display: flex;
    gap: 0.75rem;
    font-size: 0.75rem;
  }

  .element-id {
    color: var(--arc-indigo-deep, #4f46e5);
    font-family: monospace;
  }

  .element-type {
    color: var(--arc-muted, #64748b);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .element-status {
    padding: 0.125rem 0.6rem;
    border-radius: 9999px;
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }

  .element-status.status-opprettet {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
  }

  .element-status.status-avsluttet {
    background: var(--arc-chip-green-bg, #dcfce7);
    color: var(--arc-chip-green-ink, #166534);
  }

  .element-description {
    margin-top: 0.5rem;
    padding-top: 0.5rem;
    border-top: 1px solid var(--arc-line, #f1f5f9);
    color: var(--arc-muted, #64748b);
    font-size: 0.8125rem;
    line-height: 1.4;
  }

  .element-actions {
    display: flex;
    gap: 0.5rem;
  }

  .btn-icon {
    width: 2rem;
    height: 2rem;
    padding: 0;
    background: var(--arc-ground, #f8fafc);
    border: 1px solid var(--arc-line-strong, #e2e8f0);
    border-radius: 0.5rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.18s ease;
    font-size: 1rem;
    box-shadow: none;
  }

  .btn-icon:hover {
    background: var(--arc-chip-soft-indigo-bg, #eef2ff);
    border-color: var(--arc-hover-border, #c7d2fe);
    transform: none;
  }

  .btn-icon.btn-edit:hover {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    border-color: var(--arc-hover-border, #c7d2fe);
  }

  .btn-icon.btn-danger:hover {
    background: var(--arc-alert-red-bg, #fef2f2);
    border-color: var(--arc-alert-red-border, #fecaca);
  }

  .element-children {
    margin-top: 0.75rem;
    padding-top: 0.75rem;
    border-top: 1px solid var(--arc-line, #f1f5f9);
  }
</style>

