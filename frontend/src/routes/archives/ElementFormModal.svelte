<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  export let show = false;
  export let schemes: any[] = [];
  export let selectedParent: any = null;

  const dispatch = createEventDispatcher();

  let selectedSchemeName = '';
  let selectedScheme: any = null;
  let previousSchemeName = '';

  let elementForm = {
    elementIdentifier: '',
    title: '',
    description: '',
    fieldValues: {} as Record<string, any>
  };

  // Reactive statement to update selectedScheme when selectedSchemeName changes
  $: if (selectedSchemeName && schemes.length > 0) {
    selectedScheme = schemes.find(s => s.name === selectedSchemeName) || null;

    // Initialize field values ONLY when scheme actually changes
    if (selectedScheme && selectedScheme.fields && selectedSchemeName !== previousSchemeName) {
      elementForm.fieldValues = {};
      selectedScheme.fields.forEach((field: any) => {
        elementForm.fieldValues[field.name] = '';
      });
      elementForm.fieldValues = elementForm.fieldValues;
      previousSchemeName = selectedSchemeName;
    }
  } else if (!selectedSchemeName) {
    selectedScheme = null;
    elementForm.fieldValues = {};
    previousSchemeName = '';
  }

  function getAvailableChildSchemes(): any[] {
    if (!selectedParent) {
      // No parent - only show root elements
      return schemes.filter(s => s.type === 'root');
    }

    // Find the parent's scheme definition to get allowed children
    const parentScheme = schemes.find(s => s.name === (selectedParent.entityName || selectedParent.name));
    if (!parentScheme) {
      console.warn('Parent scheme not found for', selectedParent);
      return [];
    }

    // Show only schemes that are allowed children of the parent
    const allowedChildren = parentScheme.children || [];
    return schemes.filter(s => allowedChildren.includes(s.name));
  }

  function cancel() {
    selectedSchemeName = '';
    selectedScheme = null;
    elementForm = {
      elementIdentifier: '',
      title: '',
      description: '',
      fieldValues: {}
    };
    dispatch('cancel');
  }

  function backToEntitySelection() {
    selectedSchemeName = '';
    selectedScheme = null;
    elementForm.fieldValues = {};
  }

  function addElement() {
    if (!selectedScheme) {
      return;
    }

    // Extract field values
    const fields = selectedScheme.fields ? selectedScheme.fields.map((fieldDef: any) => ({
      name: fieldDef.name,
      label: fieldDef.label,
      type: fieldDef.type,
      value: elementForm.fieldValues[fieldDef.name] || ''
    })) : [];

    dispatch('add', {
      scheme: selectedScheme,
      elementIdentifier: elementForm.fieldValues['systemID'] || `${selectedScheme.name}-${Date.now()}`,
      title: elementForm.fieldValues['title'] || selectedScheme.name,
      description: elementForm.fieldValues['description'] || '',
      fields: fields,
      fieldValues: elementForm.fieldValues
    });

    // Reset form
    cancel();
  }
</script>

{#if show}
  <div class="modal-overlay" on:click={cancel} role="dialog" aria-modal="true">
    <div class="modal-content" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>
          {#if selectedScheme}
            Add {selectedScheme.name}
          {:else}
            Add Element
          {/if}
        </h3>
        <button class="modal-close" on:click={cancel} aria-label="Close">×</button>
      </div>

      <div class="modal-body">
        {#if selectedParent}
          <div class="parent-info">
            Adding child to: <strong>{selectedParent.norwegianName || selectedParent.entityName}</strong> ({selectedParent.elementIdentifier})
          </div>
        {/if}

        <!-- Scheme Selection Dropdown -->
        {#if !selectedScheme}
          <div class="form-group">
            <label for="schemeSelect">Element Type *</label>
            <select
              id="schemeSelect"
              bind:value={selectedSchemeName}
              required
            >
              <option value="">
                {#if selectedParent}
                  Select child element type...
                {:else}
                  Select element type...
                {/if}
              </option>
              {#each getAvailableChildSchemes() as scheme}
                <option value={scheme.name}>
                  {scheme.name}
                  ({scheme.type})
                </option>
              {/each}
            </select>
            {#if selectedParent && getAvailableChildSchemes().length === 0}
              <p class="helper-text warning">
                ⚠️ No child elements are allowed for this element type.
              </p>
            {:else if selectedParent}
              <p class="helper-text">
                Only showing element types that can be children of <strong>{selectedParent.norwegianName || selectedParent.entityName}</strong>
              </p>
            {:else}
              <p class="helper-text">
                Only showing root-level element types
              </p>
            {/if}
          </div>
        {/if}

        <!-- Dynamic Fields Based on Selected Scheme -->
        {#if selectedScheme && selectedScheme.fields && selectedScheme.fields.length > 0}
          {#key selectedScheme.name}
            <div class="dynamic-fields-section">
              <h4>Entity Fields</h4>
              {#each selectedScheme.fields as field}
                <div class="form-group">
                  <label for={`field-${field.name}`}>
                    {field.label || field.name}
                    {#if field.required}
                      <span class="required-marker">*</span>
                    {/if}
                    {#if field.type}
                      <span class="field-type">({field.type})</span>
                    {/if}
                  </label>

                  {#if field.type === 'date'}
                    <input
                      type="date"
                      id={`field-${field.name}`}
                      value={elementForm.fieldValues[field.name] || ''}
                      required={field.required}
                      on:input={(e) => {
                        elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                        elementForm.fieldValues = elementForm.fieldValues;
                      }}
                      placeholder={`Enter ${field.label || field.name}`}
                    />
                  {:else if field.type === 'number'}
                    <input
                      type="number"
                      id={`field-${field.name}`}
                      value={elementForm.fieldValues[field.name] || ''}
                      required={field.required}
                      on:input={(e) => {
                        elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                        elementForm.fieldValues = elementForm.fieldValues;
                      }}
                      placeholder={`Enter ${field.label || field.name}`}
                    />
                  {:else}
                    <input
                      type="text"
                      id={`field-${field.name}`}
                      value={elementForm.fieldValues[field.name] || ''}
                      required={field.required}
                      on:input={(e) => {
                        elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                        elementForm.fieldValues = elementForm.fieldValues;
                      }}
                      placeholder={`Enter ${field.label || field.name}`}
                    />
                  {/if}
                </div>
              {/each}
            </div>
          {/key}
        {/if}
      </div>

      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" on:click={cancel}>
          Cancel
        </button>
        <div class="button-group">
          {#if selectedScheme}
            <button type="button" class="btn btn-secondary" on:click={backToEntitySelection}>
              Back
            </button>
          {/if}
          <button
            type="button"
            class="btn btn-primary"
            on:click={addElement}
            disabled={!selectedScheme}
          >
            Add Element
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}

<style>
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  .modal-content {
    background: white;
    border-radius: 0.5rem;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
    max-width: 600px;
    width: 90%;
    max-height: 90vh;
    overflow-y: auto;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .modal-header h3 {
    margin: 0;
    color: #1e293b;
    font-size: 1.25rem;
    font-weight: 600;
  }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: #64748b;
    cursor: pointer;
    padding: 0;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .modal-close:hover {
    color: #1e293b;
  }

  .modal-body {
    padding: 1.5rem;
  }

  .parent-info {
    background: #f1f5f9;
    padding: 0.75rem 1rem;
    border-radius: 0.25rem;
    margin-bottom: 1.5rem;
    font-size: 0.875rem;
    color: #475569;
  }

  .form-group {
    margin-bottom: 1.25rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #1e293b;
    font-size: 0.875rem;
  }

  .field-type {
    color: #94a3b8;
    font-weight: 400;
    font-size: 0.75rem;
    margin-left: 0.25rem;
  }

  .required-marker {
    color: #ef4444;
    font-weight: 600;
    margin-left: 0.125rem;
  }

  .form-group input,
  .form-group select {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.25rem;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .form-group input:focus,
  .form-group select:focus {
    outline: none;
    border-color: #3b82f6;
  }

  .helper-text {
    margin-top: 0.25rem;
    font-size: 0.75rem;
    color: #64748b;
    font-style: italic;
  }

  .helper-text.warning {
    color: #f59e0b;
  }

  .dynamic-fields-section {
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .dynamic-fields-section h4 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 600;
  }

  .modal-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .button-group {
    display: flex;
    gap: 0.5rem;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #1e293b;
  }

  .btn-secondary:hover {
    background: #cbd5e1;
  }
</style>

