<script lang="ts">
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, GET_ALL_ARCHIVES } from '$lib/graphql/queries';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import { gql } from '@apollo/client/core';
  import ArchiveCanvas from '../ArchiveCanvas.svelte';

  // Step tracking
  let currentStep = 1;
  const totalSteps = 2;

  let newArchive = {
    userId: '',
    title: '',
    description: '',
    content: '',
    standard: 'NOARK5'
  };

  // Scheme structure
  let schemes: any[] = [];
  let designedElements: any[] = [];
  let showElementForm = false;
  let selectedScheme: any = null;
  let selectedSchemeName: string = ''; // Add this to track the selected scheme name
  let selectedParent: any = null;

  let elementForm = {
    elementIdentifier: '',
    title: '',
    description: '',
    fieldValues: {} as Record<string, any> // Store field values dynamically
  };

  let users: any[] = [];
  let creating = false;
  let error: string | null = null;
  let loadingSchemes = false;
  let previousSchemeName = '';

  onMount(async () => {
    try {
      const result = await client.query({ query: GET_ALL_USERS });
      users = result?.data?.getAllUsers || [];

      // Load initial scheme data
      await loadSchemeDefinition(newArchive.standard);
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load users';
      console.error('Load users error:', e);
    }
  });

  // Reactive statement to update selectedScheme when selectedSchemeName changes
  $: if (selectedSchemeName && schemes.length > 0) {
    selectedScheme = schemes.find(s => s.entityName === selectedSchemeName) || null;

    console.log('🔍 Scheme selected:', {
      selectedSchemeName,
      previousSchemeName,
      selectedScheme,
      hasFields: selectedScheme?.fields,
      fieldCount: selectedScheme?.fields?.length,
      fields: selectedScheme?.fields
    });

    // Initialize field values ONLY when scheme actually changes
    if (selectedScheme && selectedScheme.fields && selectedSchemeName !== previousSchemeName) {
      console.log('🔄 Scheme changed, initializing fields...');
      // Clear existing fieldValues and add new ones
      elementForm.fieldValues = {};
      selectedScheme.fields.forEach((field: any) => {
        elementForm.fieldValues[field.name] = '';
      });
      // Trigger reactivity by reassigning
      elementForm.fieldValues = elementForm.fieldValues;
      console.log('✅ Field values initialized:', elementForm.fieldValues);
      previousSchemeName = selectedSchemeName;
    }
  } else if (!selectedSchemeName) {
    selectedScheme = null;
    // Clear field values when no scheme selected
    elementForm.fieldValues = {};
    previousSchemeName = '';
  }

  async function loadSchemeDefinition(standard: string) {
    try {
      loadingSchemes = true;

      // Fetch the JSON file based on standard
      const fileNameMap: Record<string, string> = {
        'NOARK5': 'noark5.json',
        'OAIS': 'oais.json',
        'PREMIS': 'premis.json',
        'Dublin Core': 'dublincore.json',
        'METS': 'mets.json',
        'EAD': 'ead.json',
        'BagIt': 'bagit.json',
        'ISAD(G)': 'isadg.json',
        'MODS': 'mods.json'
      };
      const fileName = fileNameMap[standard] || 'noark5.json';
      const response = await fetch(`/schemeDefintions/${fileName}`);

      if (!response.ok) {
        throw new Error(`Failed to fetch ${fileName}`);
      }

      const schemeData = await response.json();

      console.log('📦 Loaded scheme data:', {
        standard,
        entitiesCount: schemeData.entities?.length,
        firstEntity: schemeData.entities?.[0],
        firstEntityFields: schemeData.entities?.[0]?.fields
      });

      // Transform entities to schemes array
      if (schemeData.entities && Array.isArray(schemeData.entities)) {
        schemes = schemeData.entities.map((entity: any) => ({
          id: entity.name, // Use name as temporary ID
          entityName: entity.name,
          norwegianName: entity.norwegianName || null,
          englishName: entity.englishName || null,
          entityType: entity.type,
          description: entity.description,
          isRoot: entity.parent === null,
          children: entity.children || [], // Store allowed children
          fields: entity.fields || [] // Store field definitions
        }));

        console.log('✅ Schemes loaded:', {
          count: schemes.length,
          schemes: schemes.map(s => ({ name: s.entityName, fieldCount: s.fields?.length }))
        });
      }

      designedElements = [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load scheme definition';
      schemes = [];
    } finally {
      loadingSchemes = false;
    }
  }

  async function loadSchemes(standard: string) {
    await loadSchemeDefinition(standard);
  }

  function nextStep() {
    if (currentStep === 1) {
      // Validate step 1
      if (!newArchive.userId || !newArchive.title || !newArchive.content) {
        error = 'Please fill in all required fields';
        return;
      }
      // Load schemes for step 2
      loadSchemes(newArchive.standard);
      currentStep = 2;
      error = null;
    }
  }

  function previousStep() {
    if (currentStep > 1) {
      currentStep--;
      error = null;
    }
  }

  function openElementForm(scheme: any = null, parent: any = null) {
    selectedScheme = scheme;
    selectedSchemeName = scheme ? scheme.entityName : '';
    selectedParent = parent;

    // Initialize field values based on scheme
    const fieldValues: Record<string, any> = {};
    if (scheme && scheme.fields) {
      scheme.fields.forEach((field: any) => {
        fieldValues[field.name] = '';
      });
    }

    elementForm = {
      elementIdentifier: '',
      title: '',
      description: '',
      fieldValues
    };
    showElementForm = true;
  }

  function cancelElementForm() {
    showElementForm = false;
    selectedScheme = null;
    selectedSchemeName = '';
    selectedParent = null;
    elementForm = {
      elementIdentifier: '',
      title: '',
      description: '',
      fieldValues: {}
    };
  }

  function backToEntitySelection() {
    selectedScheme = null;
    selectedSchemeName = '';
    elementForm.fieldValues = {};
  }

  function addElement() {
    console.log('addElement called', {
      elementForm,
      selectedScheme,
      selectedParent,
      schemes: schemes.length
    });

    if (!selectedScheme) {
      error = 'Please select an element type';
      console.error('Validation failed: no scheme selected');
      return;
    }

    // Use systemID from field values as element identifier, or generate one
    const elementIdentifier = elementForm.fieldValues['systemID'] || `${selectedScheme.entityName}-${Date.now()}`;
    // Use title from field values, or use entity name as fallback
    const title = elementForm.fieldValues['title'] || selectedScheme.entityName;

    const newElement = {
      tempId: `temp-${Date.now()}-${Math.random()}`,
      scheme: selectedScheme,
      parent: selectedParent,
      elementIdentifier: elementIdentifier,
      title: title,
      description: elementForm.fieldValues['description'] || '',
      fieldValues: elementForm.fieldValues, // Include dynamic field values
      children: []
    };

    console.log('✨ Creating new element:', {
      newElement,
      elementFormFieldValues: elementForm.fieldValues,
      elementFormFieldValuesKeys: Object.keys(elementForm.fieldValues),
      elementFormFieldValuesSample: {
        systemID: elementForm.fieldValues['systemID'],
        title: elementForm.fieldValues['title']
      }
    });

    if (selectedParent) {
      // Add as child to parent
      const parentInList = findElementById(designedElements, selectedParent.tempId);
      if (parentInList) {
        console.log('Adding as child to parent:', selectedParent.tempId);
        parentInList.children.push(newElement);
        // Trigger reactivity by reassigning the array
        designedElements = [...designedElements];
      } else {
        console.error('Parent not found in list:', selectedParent.tempId);
      }
    } else {
      // Add as root element
      console.log('Adding as root element');
      designedElements = [...designedElements, newElement];
    }

    console.log('Updated designedElements:', designedElements);

    cancelElementForm();
    error = null;
  }

  function findElementById(elements: any[], id: string): any {
    for (const element of elements) {
      if (element.tempId === id) return element;
      if (element.children && element.children.length > 0) {
        const found = findElementById(element.children, id);
        if (found) return found;
      }
    }
    return null;
  }

  function deleteElement(elementId: string) {
    designedElements = removeElementById(designedElements, elementId);
  }

  function removeElementById(elements: any[], id: string): any[] {
    return elements.filter(el => {
      if (el.tempId === id) return false;
      if (el.children && el.children.length > 0) {
        el.children = removeElementById(el.children, id);
      }
      return true;
    });
  }

  function getAvailableChildSchemes(): any[] {
    if (!selectedParent) {
      // No parent - only show root elements
      return schemes.filter(s => s.isRoot);
    }

    // Show only schemes that are allowed children of the parent
    const allowedChildren = selectedParent.scheme.children || [];
    return schemes.filter(s => allowedChildren.includes(s.entityName));
  }


  async function createArchive() {
    if (!newArchive.userId || !newArchive.title || !newArchive.content) return;

    try {
      creating = true;
      error = null;

      // Map display standard names to GraphQL enum values
      const standardMap: Record<string, string> = {
        'NOARK5': 'NOARK5',
        'OAIS': 'OAIS',
        'PREMIS': 'PREMIS',
        'Dublin Core': 'DUBLIN_CORE',
        'METS': 'METS',
        'EAD': 'EAD',
        'BagIt': 'BAGIT',
        'ISAD(G)': 'ISADG',
        'MODS': 'MODS'
      };

      const graphqlStandard = standardMap[newArchive.standard] || newArchive.standard;

      // Step 1: Create the archive
      const CREATE_ARCHIVE_MUTATION = gql`
        mutation CreateArchive($input: CreateArchiveInput!) {
          createArchive(input: $input) {
            id
            title
            standard
            createdAt
          }
        }
      `;

      const archiveResult = await client.mutate({
        mutation: CREATE_ARCHIVE_MUTATION,
        variables: {
          input: {
            userId: newArchive.userId,
            title: newArchive.title,
            description: newArchive.description || null,
            content: newArchive.content,
            standard: graphqlStandard  // Use mapped enum value
          }
        },
        // Refetch archives list to update the cache
        refetchQueries: [{ query: GET_ALL_ARCHIVES }],
        awaitRefetchQueries: true
      });

      const archiveId = archiveResult.data.createArchive.id;

      // Step 2: Create all elements in hierarchy order
      let rootElementId = null;
      if (designedElements.length > 0) {
        rootElementId = await createElementsRecursively(archiveId, designedElements, null);
      }

      // Step 3: Set the root element on the archive if elements were created
      if (rootElementId) {
        const SET_ROOT_ELEMENT_MUTATION = gql`
          mutation SetArchiveRootElement($archiveId: ID!, $rootElementId: ID!) {
            setArchiveRootElement(archiveId: $archiveId, rootElementId: $rootElementId) {
              id
              rootElement {
                id
                elementIdentifier
                title
              }
            }
          }
        `;

        await client.mutate({
          mutation: SET_ROOT_ELEMENT_MUTATION,
          variables: {
            archiveId: archiveId,
            rootElementId: rootElementId
          },
          // Refetch archives list again to include the updated rootElement
          refetchQueries: [{ query: GET_ALL_ARCHIVES }],
          awaitRefetchQueries: true
        });
      }

      // Navigate to archives list - the cache is now updated with the new archive
      goto('/archives');
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Create archive error:', e);
    } finally {
      creating = false;
    }
  }


  async function createElementsRecursively(archiveId: string, elements: any[], parentId: string | null): Promise<string | null> {
    const CREATE_ELEMENT_MUTATION = gql`
      mutation CreateElement($input: CreateElementInput!) {
        createElement(input: $input) {
          id
          fields {
            id
            name
            label
            type
            value
          }
        }
      }
    `;

    let firstElementId = null;

    for (const element of elements) {
      // Transform fieldValues object to fields array
      const fields = element.scheme.fields ? element.scheme.fields.map((fieldDef: any) => ({
        name: fieldDef.name,
        label: fieldDef.label,
        type: fieldDef.type,
        value: element.fieldValues?.[fieldDef.name] || ''
      })) : [];

      console.log('🔍 Creating element with fields:', {
        elementIdentifier: element.elementIdentifier,
        title: element.title,
        elementFieldValues: element.fieldValues,
        transformedFields: fields,
        schemeFields: element.scheme.fields
      });

      // Create element using the scheme data stored in the element
      const result = await client.mutate({
        mutation: CREATE_ELEMENT_MUTATION,
        variables: {
          input: {
            archiveId: archiveId,
            parentElementId: parentId,
            elementIdentifier: element.elementIdentifier,
            entityName: element.scheme.entityName,
            entityType: element.scheme.entityType,
            norwegianName: element.scheme.norwegianName,
            englishName: element.scheme.englishName,
            title: element.title,
            description: element.description || null,
            createdBy: newArchive.userId, // Use the archive owner as creator
            fields: fields // Include the fields
          }
        }
      });

      const createdElementId = result.data.createElement.id;

      // Track the first root element
      if (!parentId && !firstElementId) {
        firstElementId = createdElementId;
      }

      // Create children
      if (element.children && element.children.length > 0) {
        await createElementsRecursively(archiveId, element.children, createdElementId);
      }
    }

    return firstElementId;
  }

  function handleCancel() {
    goto('/archives');
  }
</script>

<div class="form-container">
  <div class="form-header">
    <h1>Create New Archive</h1>
    <p class="form-description">Step {currentStep} of {totalSteps}</p>
  </div>

  <!-- Progress Steps -->
  <div class="progress-steps">
    <div class="step" class:active={currentStep === 1} class:completed={currentStep > 1}>
      <div class="step-number">1</div>
      <div class="step-label">Archive Details</div>
    </div>
    <div class="step-connector" class:completed={currentStep > 1}></div>
    <div class="step" class:active={currentStep === 2}>
      <div class="step-number">2</div>
      <div class="step-label">Design Scheme</div>
    </div>
  </div>

  {#if error}
    <div class="alert alert-error">
      {error}
      <button on:click={() => error = null}>×</button>
    </div>
  {/if}

  <!-- Step 1: Archive Details -->
  {#if currentStep === 1}
    <form on:submit|preventDefault={nextStep}>
      <div class="form-row">
        <div class="form-group">
          <label for="userId">Owner *</label>
          <select
            id="userId"
            bind:value={newArchive.userId}
            required
          >
            <option value="">Select an owner</option>
            {#each users as user}
              <option value={user.id}>{user.name} ({user.email})</option>
            {/each}
          </select>
        </div>
      <div class="form-group">
        <label for="standard">Archive Standard *</label>
        <select
          id="standard"
          bind:value={newArchive.standard}
          on:change={() => loadSchemeDefinition(newArchive.standard)}
          required
        >
          <option value="NOARK5">NOARK5</option>
          <option value="OAIS">OAIS</option>
          <option value="PREMIS">PREMIS</option>
          <option value="Dublin Core">Dublin Core</option>
          <option value="METS">METS</option>
          <option value="EAD">EAD</option>
          <option value="BagIt">BagIt</option>
          <option value="ISAD(G)">ISAD(G)</option>
          <option value="MODS">MODS</option>
        </select>
      </div>
      </div>

      <div class="form-group">
        <label for="title">Title *</label>
        <input
          type="text"
          id="title"
          bind:value={newArchive.title}
          required
          placeholder="Enter archive title"
        />
      </div>

      <div class="form-group">
        <label for="description">Description (optional)</label>
        <textarea
          id="description"
          bind:value={newArchive.description}
          rows="3"
          placeholder="Brief description of the archive..."
        ></textarea>
      </div>

      <div class="form-group">
        <label for="content">Content *</label>
        <textarea
          id="content"
          bind:value={newArchive.content}
          required
          rows="8"
          placeholder="Enter the archive content..."
        ></textarea>
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" on:click={handleCancel}>
          Cancel
        </button>
        <button type="submit" class="btn btn-primary">
          Next: Design Scheme
        </button>
      </div>
    </form>
  {/if}

  <!-- Step 2: Design Element Hierarchy -->
  {#if currentStep === 2}
    <div class="scheme-designer">
      {#if loadingSchemes}
        <div class="loading-scheme">
          <div class="spinner"></div>
          <p>Loading {newArchive.standard} scheme definitions...</p>
        </div>
      {:else}
        <div class="scheme-info">
          <h2>Design Element Hierarchy</h2>
          <p>Create elements for your archive based on the {newArchive.standard} standard. Build your archive structure by adding elements and organizing them hierarchically.</p>
        </div>

        <!-- Canvas with Add Element Button -->
        <ArchiveCanvas
          elements={designedElements}
          readonly={false}
          onAddChild={(parent) => openElementForm(null, parent)}
          onDelete={deleteElement}
          on:addElement={() => openElementForm(null)}
        />
      {/if}

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" on:click={previousStep}>
          Back
        </button>
        <button
          type="button"
          class="btn btn-primary"
          on:click={createArchive}
          disabled={creating}
        >
          {creating ? 'Creating...' : 'Create Archive'}
        </button>
      </div>
    </div>
  {/if}
</div>

<!-- Element Form Modal -->
{#if showElementForm}
  <div class="modal-overlay" on:click={cancelElementForm} role="dialog" aria-modal="true">
    <div class="modal-content" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>
          {#if selectedScheme}
            Add {selectedScheme.norwegianName || selectedScheme.englishName || selectedScheme.entityName}
          {:else}
            Add Element
          {/if}
        </h3>
        <button class="modal-close" on:click={cancelElementForm} aria-label="Close">×</button>
      </div>

      <div class="modal-body">
        {#if selectedParent}
          <div class="parent-info">
            Adding to: <strong>{selectedParent.title}</strong> ({selectedParent.elementIdentifier})
          </div>
        {/if}

        <!-- Scheme Selection Dropdown (only if scheme not pre-selected) -->
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
                <option value={scheme.entityName}>
                  {scheme.norwegianName || scheme.englishName || scheme.entityName}
                  ({scheme.entityType})
                </option>
              {/each}
            </select>
            {#if selectedParent && getAvailableChildSchemes().length === 0}
              <p class="helper-text warning">
                ⚠️ No child elements are allowed for this element type.
              </p>
            {:else if selectedParent}
              <p class="helper-text">
                Only showing element types that can be children of <strong>{selectedParent.scheme.norwegianName || selectedParent.scheme.entityName}</strong>
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
          {#key selectedScheme.entityName}
            <div class="dynamic-fields-section">
              <h4>Entity Fields</h4>
              <p class="helper-text" style="margin-bottom: 1rem;">
                Debug: {JSON.stringify(Object.keys(elementForm.fieldValues))}
              </p>
              {#each selectedScheme.fields as field}
                <div class="form-group">
                  <label for={`field-${field.name}`}>
                    {field.label || field.name}
                    {#if field.type === 'string' || field.type === 'date' || field.type === 'number'}
                      <span class="field-type">({field.type})</span>
                    {/if}
                  </label>

                  {#if field.type === 'date'}
                    <input
                      type="date"
                      id={`field-${field.name}`}
                      value={elementForm.fieldValues[field.name] || ''}
                      on:input={(e) => {
                        elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                        elementForm.fieldValues = elementForm.fieldValues;
                        console.log(`📝 Updated ${field.name}:`, e.currentTarget?.value);
                      }}
                      placeholder={`Enter ${field.label || field.name}`}
                    />
                  {:else if field.type === 'number'}
                    <input
                      type="number"
                      id={`field-${field.name}`}
                      value={elementForm.fieldValues[field.name] || ''}
                      on:input={(e) => {
                        elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                        elementForm.fieldValues = elementForm.fieldValues;
                        console.log(`📝 Updated ${field.name}:`, e.currentTarget?.value);
                      }}
                      placeholder={`Enter ${field.label || field.name}`}
                    />
                  {:else}
                    <input
                      type="text"
                      id={`field-${field.name}`}
                      value={elementForm.fieldValues[field.name] || ''}
                      on:input={(e) => {
                        elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                        elementForm.fieldValues = elementForm.fieldValues;
                        console.log(`📝 Updated ${field.name}:`, e.currentTarget?.value);
                      }}
                      placeholder={`Enter ${field.label || field.name}`}
                    />
                  {/if}
                  <small class="helper-text">Value: {elementForm.fieldValues[field.name] || '(empty)'}</small>
                </div>
              {/each}
            </div>
          {/key}
        {/if}
      </div>

      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" on:click={cancelElementForm}>
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
  .form-container {
    max-width: 900px;
    margin: 2rem auto;
    background: white;
    padding: 2rem;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .form-header {
    margin-bottom: 2rem;
    text-align: center;
  }

  .form-header h1 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1.875rem;
    font-weight: 700;
  }

  .form-description {
    margin: 0;
    color: #64748b;
    font-size: 0.875rem;
  }

  /* Progress Steps */
  .progress-steps {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 2rem;
    padding: 1.5rem 0;
  }

  .step {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.5rem;
  }

  .step-number {
    width: 2.5rem;
    height: 2.5rem;
    border-radius: 50%;
    background: #e2e8f0;
    color: #64748b;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    transition: all 0.3s;
  }

  .step.active .step-number {
    background: #3b82f6;
    color: white;
  }

  .step.completed .step-number {
    background: #10b981;
    color: white;
  }

  .step-label {
    font-size: 0.875rem;
    color: #64748b;
    font-weight: 500;
  }

  .step.active .step-label {
    color: #1e293b;
    font-weight: 600;
  }

  .step-connector {
    width: 4rem;
    height: 2px;
    background: #e2e8f0;
    margin: 0 1rem;
    transition: all 0.3s;
  }

  .step-connector.completed {
    background: #10b981;
  }

  /* Alert */
  .alert {
    padding: 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1.5rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .alert-error {
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }

  .alert button {
    background: none;
    border: none;
    color: inherit;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    width: 1.5rem;
    height: 1.5rem;
  }

  /* Form Fields */
  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
    margin-bottom: 1rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #1e293b;
    font-weight: 500;
    font-size: 0.875rem;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    width: 100%;
    padding: 0.625rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    transition: border-color 0.2s;
  }

  .form-group input:focus,
  .form-group select:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
  }

  .form-group textarea {
    resize: vertical;
    font-family: inherit;
  }

  /* Scheme Designer */
  .scheme-designer {
    min-height: 400px;
  }

  .loading-scheme {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 4rem 2rem;
    gap: 1rem;
  }

  .loading-scheme .spinner {
    width: 3rem;
    height: 3rem;
    border: 4px solid #e2e8f0;
    border-top-color: #3b82f6;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .loading-scheme p {
    color: #64748b;
    font-size: 0.875rem;
  }

  .scheme-info {
    margin-bottom: 2rem;
    padding: 1rem;
    background: #f8fafc;
    border-radius: 0.375rem;
    border-left: 4px solid #3b82f6;
  }

  .scheme-info h2 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1.25rem;
  }

  .scheme-info p {
    margin: 0;
    color: #64748b;
    font-size: 0.875rem;
  }


  /* Modal */
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
    max-width: 500px;
    width: 90%;
    max-height: 90vh;
    overflow: auto;
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
    border-radius: 0.25rem;
    transition: all 0.2s;
  }

  .modal-close:hover {
    background: #f1f5f9;
    color: #1e293b;
  }

  .modal-body {
    padding: 1.5rem;
  }

  .parent-info {
    padding: 0.75rem;
    background: #f0f9ff;
    border: 1px solid #bae6fd;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    font-size: 0.875rem;
    color: #0c4a6e;
  }

  .helper-text {
    margin-top: 0.5rem;
    margin-bottom: 0;
    font-size: 0.75rem;
    color: #64748b;
    line-height: 1.4;
  }

  .helper-text.warning {
    color: #f59e0b;
    font-weight: 500;
  }

  /* Dynamic Fields Section */
  .dynamic-fields-section {
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 2px solid #e2e8f0;
  }

  .dynamic-fields-section h4 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 600;
  }

  .field-type {
    font-size: 0.75rem;
    color: #64748b;
    font-weight: 400;
    font-style: italic;
  }

  .modal-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 0.75rem;
    padding: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .button-group {
    display: flex;
    gap: 0.75rem;
  }

  /* Form Actions */
  .form-actions {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn {
    padding: 0.625rem 1.5rem;
    border: none;
    border-radius: 0.375rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 0.875rem;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-primary:disabled {
    background: #94a3b8;
    cursor: not-allowed;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #475569;
  }

  .btn-secondary:hover {
    background: #cbd5e1;
  }

  @media (max-width: 768px) {
    .form-container {
      margin: 1rem;
      padding: 1.5rem;
    }

    .form-row {
      grid-template-columns: 1fr;
    }

    .progress-steps {
      padding: 1rem 0;
    }

    .step-connector {
      width: 2rem;
      margin: 0 0.5rem;
    }

    .step-label {
      font-size: 0.75rem;
    }

    .entity-children {
      padding-left: 1rem;
    }
  }
</style>
