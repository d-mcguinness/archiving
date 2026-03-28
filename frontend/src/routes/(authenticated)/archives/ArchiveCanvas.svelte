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
    border: 2px solid #e2e8f0;
    border-radius: 0.5rem;
    overflow: hidden;
  }

  .canvas-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 1.5rem;
    background: #f8fafc;
    border-bottom: 2px solid #e2e8f0;
  }

  .canvas-header h3 {
    margin: 0;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 600;
  }

  .btn-add {
    padding: 0.5rem 1rem;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 0.375rem;
    font-weight: 500;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .btn-add:hover {
    background: #2563eb;
    box-shadow: 0 2px 4px rgba(59, 130, 246, 0.2);
  }

  /* Canvas */
  .canvas {
    min-height: 400px;
    padding: 2rem;
    background: white;
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
    color: #64748b;
    font-size: 1.125rem;
    font-weight: 600;
  }

  .canvas-empty p {
    margin: 0;
    color: #94a3b8;
    font-size: 0.875rem;
  }
</style>

