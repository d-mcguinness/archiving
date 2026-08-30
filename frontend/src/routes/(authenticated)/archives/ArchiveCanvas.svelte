<script lang="ts">
  import { createEventDispatcher } from 'svelte';
  import ElementNode from './ElementNode.svelte';

  export let elements: any[] = [];
  export let readonly = false;
  export let onAddChild: ((parent: any) => void) | null = null;
  export let onDelete: ((elementId: string) => void) | null = null;
  export let onEdit: ((element: any) => void) | null = null;

  const dispatch = createEventDispatcher();

  function handleAddElement() {
    dispatch('addElement');
  }

  function handleAddChild(parent: any) {
    if (onAddChild) {
      onAddChild(parent);
    } else {
      dispatch('addChild', parent);
    }
  }

  function handleDelete(elementId: string) {
    if (onDelete) {
      onDelete(elementId);
    } else {
      dispatch('delete', elementId);
    }
  }

  function handleEdit(element: any) {
    if (onEdit) {
      onEdit(element);
    } else {
      dispatch('edit', element);
    }
  }
</script>

<div class="canvas-container">
  <div class="canvas-header">
    <h3>Archive Canvas ({elements.length} element{elements.length !== 1 ? 's' : ''})</h3>
    {#if !readonly}
      <button type="button" class="btn btn-add" on:click={handleAddElement}>
        ➕ Add Element
      </button>
    {/if}
  </div>

  <div class="canvas">
    {#if elements.length === 0}
      <div class="canvas-empty">
        <div class="empty-icon">📋</div>
        <h4>No Elements Yet</h4>
        <p>
          {#if readonly}
            This archive has no elements
          {:else}
            Click "Add Element" to start designing your archive structure
          {/if}
        </p>
      </div>
    {:else}
      <div class="canvas-content">
        {#each elements as element}
          <ElementNode
            elementNode={element}
            level={0}
            {readonly}
            onAddChild={handleAddChild}
            onDelete={handleDelete}
            onEdit={handleEdit}
          />
        {/each}
      </div>
    {/if}
  </div>
</div>

<style>
  /* Canvas Container */
  .canvas-container {
    margin-bottom: 2rem;
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    overflow: hidden;
    background: var(--arc-card, #fff);
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .canvas-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 1.5rem;
    background: var(--arc-ground, #f8fafc);
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .canvas-header h3 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1rem;
    font-weight: 600;
  }

  /* Compact icon+label version of the global gradient button. */
  .btn-add {
    padding: 0.5rem 1rem;
    font-size: 0.875rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6);
  }

  /* Canvas */
  .canvas {
    min-height: 400px;
    padding: 2rem;
    background: var(--arc-card, #fff);
  }

  .canvas-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 350px;
    text-align: center;
  }

  .empty-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
    opacity: 0.3;
  }

  .canvas-empty h4 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-muted, #64748b);
    font-size: 1.125rem;
    font-weight: 600;
  }

  .canvas-empty p {
    margin: 0;
    color: var(--arc-faint, #94a3b8);
    font-size: 0.875rem;
  }
</style>

