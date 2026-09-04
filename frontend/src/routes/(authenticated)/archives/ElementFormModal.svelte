<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  export let show = false;
  export let schemes: any[] = [];
  export let selectedParent: any = null;
  export let devmode = false;

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
      // No parent - show root elements
      const rootElements = schemes.filter(s => s.type === 'root');
      if (rootElements.length > 0) return rootElements;

      // Fallback: if no entity has type 'root', show entities that are not
      // listed as children of any other entity (i.e. top-level entities)
      const allChildNames = new Set<string>();
      for (const s of schemes) {
        if (s.children) {
          for (const c of s.children) {
            allChildNames.add(c);
          }
        }
      }
      const topLevel = schemes.filter(s => !allChildNames.has(s.name));
      return topLevel.length > 0 ? topLevel : schemes;
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

  function fillDefaults() {
    if (!selectedScheme?.fields) return;

    const now = new Date().toISOString().split('T')[0];
    const id = Math.random().toString(36).substring(2, 8).toUpperCase();

    for (const field of selectedScheme.fields) {
      // Only fill required fields (and ID/title fields)
      if (!field.required && !['systemID', 'title', 'description'].includes(field.name)) continue;
      // Skip if already has a value
      if (elementForm.fieldValues[field.name]) continue;

      const name = (field.name || '').toLowerCase();
      const type = field.type || 'text';

      if (name.includes('systemid') || name.includes('identifier') || name.includes('id')) {
        elementForm.fieldValues[field.name] = `${selectedScheme.name}-${id}`;
      } else if (name === 'title' || name === 'tittel') {
        elementForm.fieldValues[field.name] = `${selectedScheme.name} ${id}`;
      } else if (name === 'description' || name === 'beskrivelse') {
        elementForm.fieldValues[field.name] = `Auto-generated ${selectedScheme.name}`;
      } else if (type === 'date') {
        elementForm.fieldValues[field.name] = now;
      } else if (type === 'number') {
        elementForm.fieldValues[field.name] = String(Math.floor(Math.random() * 100) + 1);
      } else if (name.includes('status') || name.includes('tilstand')) {
        elementForm.fieldValues[field.name] = 'Active';
      } else if (name.includes('language') || name.includes('spraak')) {
        elementForm.fieldValues[field.name] = 'en';
      } else if (name.includes('format')) {
        elementForm.fieldValues[field.name] = 'application/pdf';
      } else if (name.includes('algorithm') || name.includes('checksum')) {
        elementForm.fieldValues[field.name] = 'SHA-256';
      } else if (name.includes('creator') || name.includes('opprettetav') || name.includes('author')) {
        elementForm.fieldValues[field.name] = 'System';
      } else if (name.includes('level')) {
        elementForm.fieldValues[field.name] = 'Full';
      } else if (name.includes('rights') || name.includes('access')) {
        elementForm.fieldValues[field.name] = 'Open';
      } else if (name.includes('type') || name.includes('category')) {
        elementForm.fieldValues[field.name] = 'Default';
      } else {
        elementForm.fieldValues[field.name] = `${field.label || field.name} ${id}`;
      }
    }

    elementForm.fieldValues = elementForm.fieldValues;
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
    <div class="modal modal-content" on:click|stopPropagation role="document">
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
        {#if devmode && selectedScheme}
          <button type="button" class="btn btn-defaults" on:click={fillDefaults}>
            Fill Defaults
          </button>
        {/if}
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
  /* Global .modal-overlay / .modal kit, widened for the field list and
     unpadded because the header/body/footer carry their own padding. */
  .modal-overlay {
    z-index: 1000;
  }

  .modal-content {
    max-width: 600px;
    width: 90%;
    padding: 0;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .modal-header h3 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.25rem;
    font-weight: 600;
  }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: var(--arc-muted, #64748b);
    cursor: pointer;
    padding: 0;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.5rem;
    box-shadow: none;
  }

  .modal-close:hover {
    color: var(--arc-ink, #0f172a);
    background: var(--arc-card-2, #f1f5f9);
    transform: none;
    box-shadow: none;
  }

  .modal-body {
    padding: 1.5rem;
  }

  .parent-info {
    background: var(--arc-alert-indigo-bg, #eef2ff);
    border: 1px solid var(--arc-alert-indigo-border, #c7d2fe);
    padding: 0.75rem 1rem;
    border-radius: 0.6rem;
    margin-bottom: 1.5rem;
    font-size: 0.875rem;
    color: var(--arc-alert-indigo-ink, #4338ca);
  }

  .form-group {
    margin-bottom: 1.25rem;
  }

  .field-type {
    color: var(--arc-faint, #94a3b8);
    font-weight: 400;
    font-size: 0.75rem;
    margin-left: 0.25rem;
  }

  .required-marker {
    color: #ef4444;
    font-weight: 600;
    margin-left: 0.125rem;
  }

  .helper-text {
    margin-top: 0.25rem;
    font-size: 0.75rem;
    color: var(--arc-muted, #64748b);
    font-style: italic;
  }

  .helper-text.warning {
    color: #f59e0b;
  }

  .dynamic-fields-section {
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  .dynamic-fields-section h4 {
    margin: 0 0 1rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1rem;
    font-weight: 600;
  }

  .modal-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  .button-group {
    display: flex;
    gap: 0.5rem;
  }

  /* Slightly smaller type than the global button kit. */
  .btn {
    font-size: 0.875rem;
  }

  .btn:disabled {
    opacity: 0.5;
  }

  .btn-defaults {
    background: #f59e0b;
    color: white;
    margin-right: auto;
    box-shadow: none;
  }

  .btn-defaults:hover {
    background: #d97706;
  }
</style>

