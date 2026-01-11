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
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    padding: 0.75rem;
    margin-bottom: 0.75rem;
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
    color: #1e293b;
    margin-bottom: 0.25rem;
  }

  .element-meta {
    display: flex;
    gap: 0.75rem;
    font-size: 0.75rem;
  }

  .element-id {
    color: #3b82f6;
    font-family: monospace;
  }

  .element-type {
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .element-status {
    padding: 0.125rem 0.5rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .element-status.status-opprettet {
    background: #dbeafe;
    color: #1e40af;
  }

  .element-status.status-avsluttet {
    background: #dcfce7;
    color: #166534;
  }

  .element-description {
    margin-top: 0.5rem;
    padding-top: 0.5rem;
    border-top: 1px solid #f1f5f9;
    color: #64748b;
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
    background: #f1f5f9;
    border: 1px solid #e2e8f0;
    border-radius: 0.25rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
    font-size: 1rem;
  }

  .btn-icon:hover {
    background: #e2e8f0;
  }

  .btn-icon.btn-edit:hover {
    background: #dbeafe;
    border-color: #bfdbfe;
  }

  .btn-icon.btn-danger:hover {
    background: #fef2f2;
    border-color: #fecaca;
  }

  .element-children {
    margin-top: 0.75rem;
    padding-top: 0.75rem;
    border-top: 1px solid #f1f5f9;
  }
</style>

