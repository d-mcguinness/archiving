<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_RELEASE, GET_ALL_RELEASES, GET_ALL_PRESERVATIONS, GET_PRESERVATIONS_BY_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { standards, graphqlToKey, type StandardDefinition } from '$lib/standards';

  function getReleasesPath() {
    const { role } = get(auth);
    if (role === 'ADMIN' || role === 'TENANT') return '/release';
    return '/';
  }

  let selectedStandardKey = '';
  let selectedStandard: StandardDefinition | null = null;
  let schemaData: any = null;
  let releaseEntityDef: any = null;
  let fieldValues: Record<string, string> = {};
  let users: any[] = [];
  let aips: any[] = [];
  let selectedUserId = '';
  let selectedPreservationId = '';
  let dipTitle = '';
  let dipDescription = '';
  let sourcePreservationId = '';
  let loading = false;
  let loadingSchema = false;
  let submitting = false;
  let error: string | null = null;
  let showOptionalFields = false;
  let prefilled = false;
  let pendingChildEntity: string | null = null;
  let filterStandard: string | null = null;

  // Computed field groups
  $: requiredFields = releaseEntityDef?.fields?.filter((f: any) => f.required) || [];
  $: optionalFields = releaseEntityDef?.fields?.filter((f: any) => !f.required) || [];

  // Filter Preservations by the standard from the URL query param
  $: filteredPreservations = filterStandard
    ? aips.filter((a: any) => {
        const match = standards.find(s => s.key === filterStandard);
        return match ? a.standard === match.graphql : true;
      })
    : aips;

  async function onPreservationSelect() {
    if (!selectedPreservationId) {
      sourcePreservationId = '';
      selectedStandardKey = '';
      selectedStandard = null;
      releaseEntityDef = null;
      schemaData = null;
      fieldValues = {};
      dipTitle = '';
      dipDescription = '';
      selectedUserId = '';
      prefilled = false;
      return;
    }
    const aip = aips.find((a: any) => a.id === selectedPreservationId);
    if (!aip) return;

    sourcePreservationId = aip.id;
    dipTitle = aip.title ? `Release - ${aip.title}` : '';
    dipDescription = aip.description || '';

    // Derive standard from the Preservation
    const matchedStandard = standards.find(s => s.graphql === aip.standard);
    if (matchedStandard) {
      selectedStandardKey = matchedStandard.key;
      await onStandardChange();
    }

    // Set owner to Preservation owner if we have a matching user
    if (aip.ownerId) {
      const ownerMatch = users.find((u: any) => u.id === aip.ownerId);
      if (ownerMatch) {
        selectedUserId = ownerMatch.id;
      }
    }

    prefilled = true;
    applyAllDefaults();
  }

  onMount(async () => {
    loading = true;
    try {
      const authState = get(auth);
      const [usersResult, aipsResult] = await Promise.all([
        client.query({ query: GET_ALL_USERS }),
        authState.role === 'TENANT' && authState.tenantId
          ? client.query({ query: GET_PRESERVATIONS_BY_TENANT, variables: { tenantId: authState.tenantId.toString() } })
          : client.query({ query: GET_ALL_PRESERVATIONS })
      ]);
      users = usersResult?.data?.getAllUsers || [];
      aips = (authState.role === 'TENANT' && authState.tenantId)
        ? aipsResult?.data?.getPreservationsByTenant || []
        : aipsResult?.data?.getAllPreservations || [];
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load data';
    } finally {
      loading = false;
    }

    // Read query params and pre-fill form
    const params = $page.url.searchParams;
    const qUserId = params.get('userId');
    const qTitle = params.get('title');
    const qDescription = params.get('description');
    const qPreservationId = params.get('sourcePreservationId');

    if (qUserId) selectedUserId = qUserId;
    if (qTitle) dipTitle = qTitle;
    if (qDescription) dipDescription = qDescription;

    // Auto-select Preservation from query param
    if (qPreservationId) {
      const match = aips.find((a: any) => a.id === qPreservationId);
      if (match) {
        selectedPreservationId = qPreservationId;
        await onPreservationSelect();
      }
    }

    // Pre-select standard from query param (e.g. from /deliver page)
    const qStandard = params.get('standard');
    if (qStandard) {
      filterStandard = qStandard;
      if (!selectedStandardKey) {
        const match = standards.find(s => s.key === qStandard);
        if (match) {
          selectedStandardKey = match.key;
          await onStandardChange();
        }
      }
    }
  });

  async function onStandardChange() {
    selectedStandard = standards.find(s => s.key === selectedStandardKey) || null;
    releaseEntityDef = null;
    fieldValues = {};
    error = null;

    if (!selectedStandard) return;

    loadingSchema = true;
    try {
      const response = await fetch(`/schemeDefintions/${selectedStandard.file}`);
      if (!response.ok) throw new Error(`Failed to load ${selectedStandard.file}`);
      schemaData = await response.json();

      // Find the Release root entity
      const entity = schemaData.entities?.find((e: any) => e.name === selectedStandard!.releaseEntity);
      if (!entity) throw new Error(`Release entity "${selectedStandard.releaseEntity}" not found in schema`);

      releaseEntityDef = entity;

      // Initialize field values
      fieldValues = {};
      if (entity.fields) {
        entity.fields.forEach((field: any) => {
          fieldValues[field.name] = '';
        });
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load schema';
      releaseEntityDef = null;
    } finally {
      loadingSchema = false;
    }
  }

  function genId() {
    return `Release-${sourcePreservationId || Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
  }

  function getStandardDefaults(standardKey: string, title: string, description: string): Record<string, string> {
    const today = new Date().toISOString().split('T')[0];
    const id = genId();
    const user = users.find((u: any) => u.id === selectedUserId);
    const userName = user?.name || 'System';

    const map: Record<string, Record<string, string>> = {
      'E-ARK': {
        packageID: id,
        title: title || 'New Release package',
        description: description || '',
        profile: 'https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml',
        contentInformationType: 'MIXED',
        oaisPackageType: 'DIP',
        creationDate: today,
        creator: userName,
        accessRights: 'Public',
        preservationLevel: 'reference',
        representationCount: '1',
      },
      'NOARK5': {
        systemID: id,
        title: title || 'New Archive',
        description: description || '',
        archiveStatus: 'Created',
        documentMedium: 'Electronic archive',
        storageLocation: 'Default storage',
        createdDate: today,
        createdBy: userName,
      },
      'OAIS': {
        aipID: id,
        title: title || 'New Release package',
        description: description || '',
        creationDate: today,
        version: '1.0',
        accessRights: 'Public',
        packageType: 'DIP',
      },
      'PREMIS': {
        objectIdentifierType: 'local',
        objectIdentifierValue: id,
        objectCategory: 'Representation',
        preservationLevelType: 'reference',
        preservationLevelValue: 'reference copy',
        preservationLevelRole: 'dissemination',
        preservationLevelDateAssigned: today,
        originalName: title || 'Untitled Object',
      },
      'Dublin Core': {
        resourceIdentifier: id,
        resourceType: 'Dataset',
      },
      'METS': {
        metsID: id,
        objID: `OBJ-${id}`,
        label: title || 'New METS Document',
        type: 'digital object',
        profile: 'http://www.loc.gov/standards/mets/profiles',
      },
      'EAD': {
        eadID: id,
        audience: 'external',
        relatedEncoding: 'Dublin Core',
        lang: 'eng',
        script: 'Latn',
      },
      'BagIt': {
        bagName: (title || 'new-bag').toLowerCase().replace(/[^a-z0-9]+/g, '-'),
        payloadOxum: '0.0',
        bagSize: '0 KB',
        isComplete: 'true',
        isValid: 'true',
      },
      'ISAD(G)': {
        descriptionID: id,
        levelOfDescription: 'Fonds',
      },
      'MODS': {
        modsID: id,
        version: '3.8',
      },
    };

    return map[standardKey] || {};
  }

  function applyAllDefaults() {
    if (!selectedUserId && users.length > 0) {
      selectedUserId = users[0].id;
    }
    if (!dipTitle) {
      const standardLabel = selectedStandard?.label || 'Archive';
      dipTitle = `${standardLabel} Release - ${new Date().toLocaleDateString()}`;
    }
    if (!dipDescription) {
      dipDescription = `Release package created using the ${selectedStandard?.label || 'selected'} standard.`;
    }

    if (!releaseEntityDef?.fields || !selectedStandard) return;

    const defaults = getStandardDefaults(selectedStandard.key, dipTitle, dipDescription);

    for (const field of releaseEntityDef.fields) {
      const val = defaults[field.name];
      if (val !== undefined) {
        fieldValues[field.name] = val;
      }
    }

    fieldValues = fieldValues;
  }

  function updateField(name: string, value: string) {
    fieldValues[name] = value;
    fieldValues = fieldValues;
  }

  function validateForm(): string | null {
    if (!selectedPreservationId) return 'Please select a source Preservation';
    if (!selectedUserId) return 'Please select an owner';
    if (!selectedStandard) return 'Please select a standard';
    if (!dipTitle.trim()) return 'Please enter a title';

    for (const field of requiredFields) {
      if (!fieldValues[field.name]?.trim()) {
        return `Required field "${field.label}" is empty`;
      }
    }
    return null;
  }

  async function handleSubmit() {
    const validationError = validateForm();
    if (validationError) {
      error = validationError;
      return;
    }

    submitting = true;
    error = null;

    try {
      const dipContent = JSON.stringify({
        dipType: selectedStandard!.releaseLabel,
        standard: selectedStandard!.key,
        entity: releaseEntityDef.name,
        fields: fieldValues
      }, null, 2);

      const elementIdentifier = fieldValues['packageID'] || fieldValues['systemID'] || fieldValues['aipID'] || fieldValues['objectIdentifierValue'] || fieldValues['bagName'] || fieldValues['metsID'] || fieldValues['eadID'] || fieldValues['descriptionID'] || fieldValues['modsID'] || fieldValues['resourceIdentifier'] || `Release-${Date.now()}`;

      const fields = releaseEntityDef.fields.map((fieldDef: any) => ({
        name: fieldDef.name,
        label: fieldDef.label,
        type: fieldDef.type,
        value: fieldValues[fieldDef.name] || ''
      }));

      const input: Record<string, any> = {
        userId: selectedUserId,
        title: dipTitle,
        description: dipDescription || null,
        content: dipContent,
        standard: selectedStandard!.graphql,
        sourcePreservationId: sourcePreservationId || null,
        elementIdentifier,
        entityName: releaseEntityDef.name,
        entityType: releaseEntityDef.type,
        elementTitle: dipTitle,
        elementDescription: dipDescription || null,
        createdBy: selectedUserId,
        fields
      };

      const result = await client.mutate({
        mutation: CREATE_RELEASE,
        variables: { input },
        refetchQueries: [{ query: GET_ALL_RELEASES }],
        awaitRefetchQueries: true
      });

      const newReleaseId = result?.data?.createRelease?.id;
      toasts.add(`Release "${dipTitle}" created successfully using ${selectedStandard!.label}`, 'success');

      if (pendingChildEntity && newReleaseId) {
        goto(`/release/edit/${newReleaseId}?addChild=${encodeURIComponent(pendingChildEntity)}`);
      } else if (newReleaseId) {
        goto(`/release/edit/${newReleaseId}`);
      } else {
        goto(getReleasesPath());
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to create Release';
      toasts.add(`Failed to create Release: ${error}`, 'error');
    } finally {
      submitting = false;
    }
  }

  function handleCreateAndAddChild(childEntityName: string) {
    pendingChildEntity = childEntityName;
    handleSubmit();
  }

  function handleCancel() {
    goto(getReleasesPath());
  }
</script>

<div class="dip-container">
  <div class="dip-header">
    <div class="dip-header-top">
      <div>
        <span class="eyebrow">Release</span>
        <h1>Create Release package</h1>
        <p class="subtitle">Build a Release by selecting a source Preservation</p>
      </div>
      {#if selectedStandard}
        <button
          type="button"
          class="btn btn-defaults"
          on:click={applyAllDefaults}
          disabled={loadingSchema}
          title="Auto-fill all sections with sensible defaults for {selectedStandard.label}"
        >
          Fill All Defaults
        </button>
      {/if}
    </div>
  </div>

  {#if error}
    <div class="alert alert-error">
      {error}
      <button on:click={() => error = null}>x</button>
    </div>
  {/if}

  {#if loading}
    <div class="loading-state">
      <div class="spinner"></div>
      <p>Loading...</p>
    </div>
  {:else}
    <form on:submit|preventDefault={handleSubmit}>

      <!-- Section 1: Select Source Preservation -->
      <section class="form-section">
        <h2 class="section-title">1. Select Source Preservation</h2>

        <div class="form-group">
          <label for="aipSelect">Source Preservation <span class="req">*</span></label>
          <select id="aipSelect" bind:value={selectedPreservationId} on:change={onPreservationSelect} required>
            <option value="">-- Select a source Preservation --</option>
            {#each filteredPreservations as aip}
              <option value={aip.id}>
                #{aip.id} - {aip.title} ({aip.standard})
              </option>
            {/each}
          </select>
          {#if filterStandard}
            <span class="field-hint">Showing Preservations using the {filterStandard} standard. <button type="button" class="link-btn" on:click={() => filterStandard = null}>Show all</button></span>
          {:else}
            <span class="field-hint">The standard, title, and description will be derived from the selected Preservation.</span>
          {/if}
        </div>

        {#if selectedStandard && schemaData}
          <div class="standard-info">
            <strong>{schemaData.fullName}</strong>
            <span class="standard-ref">{schemaData.reference}</span>
            <p>{schemaData.description}</p>
          </div>
        {/if}
      </section>

      <!-- Section 2: Basic Info (only when Preservation selected) -->
      {#if selectedPreservationId}
        <section class="form-section">
          <h2 class="section-title">2. Basic Information</h2>

          <div class="form-row">
            <div class="form-group">
              <label for="userId">Owner <span class="req">*</span></label>
              <select id="userId" bind:value={selectedUserId} required>
                <option value="">Select an owner</option>
                {#each users as user}
                  <option value={user.id}>{user.name} ({user.email})</option>
                {/each}
              </select>
            </div>
            <div class="form-group">
              <label for="dipTitle">Release Title <span class="req">*</span></label>
              <input type="text" id="dipTitle" bind:value={dipTitle} required placeholder="Enter Release title" />
            </div>
          </div>

          <div class="form-group">
            <label for="dipDescription">Description</label>
            <textarea id="dipDescription" bind:value={dipDescription} rows="3" placeholder="Brief description of this dissemination information package..."></textarea>
          </div>

          <div class="form-group">
            <label for="sourcePreservationId">Source Preservation ID</label>
            <input type="text" id="sourcePreservationId" bind:value={sourcePreservationId} placeholder="Link to originating Preservation" disabled />
          </div>
        </section>

        <!-- Section 3: Standard-Specific Fields -->
        {#if loadingSchema}
          <section class="form-section">
            <div class="loading-state">
              <div class="spinner"></div>
              <p>Loading {selectedStandard?.label} schema...</p>
            </div>
          </section>
        {:else if releaseEntityDef}
          <section class="form-section">
            <h2 class="section-title">
              3. {selectedStandard?.releaseLabel} Fields
            </h2>

            {#if releaseEntityDef.description}
              <p class="entity-description">{releaseEntityDef.description}</p>
            {/if}

            {#if releaseEntityDef.note}
              <div class="entity-note">
                {releaseEntityDef.note}
              </div>
            {/if}

            <!-- Required Fields -->
            {#if requiredFields.length > 0}
              <div class="fields-group">
                <h3 class="fields-group-title">Required Fields</h3>
                <div class="fields-grid">
                  {#each requiredFields as field}
                    <div class="form-group" class:full-width={field.type === 'text'}>
                      <label for={`field-${field.name}`}>
                        {field.label}
                        <span class="req">*</span>
                        <span class="field-type-tag">{field.type}</span>
                      </label>
                      {#if field.type === 'date'}
                        <input
                          type="date"
                          id={`field-${field.name}`}
                          value={fieldValues[field.name] || ''}
                          required
                          on:input={(e) => updateField(field.name, e.currentTarget?.value || '')}
                        />
                      {:else if field.type === 'number'}
                        <input
                          type="number"
                          id={`field-${field.name}`}
                          value={fieldValues[field.name] || ''}
                          required
                          on:input={(e) => updateField(field.name, e.currentTarget?.value || '')}
                          placeholder={`Enter ${field.label}`}
                        />
                      {:else}
                        <input
                          type="text"
                          id={`field-${field.name}`}
                          value={fieldValues[field.name] || ''}
                          required
                          on:input={(e) => updateField(field.name, e.currentTarget?.value || '')}
                          placeholder={`Enter ${field.label}`}
                        />
                      {/if}
                    </div>
                  {/each}
                </div>
              </div>
            {/if}

            <!-- Optional Fields (collapsible) -->
            {#if optionalFields.length > 0}
              <div class="fields-group optional-group">
                <button
                  type="button"
                  class="fields-group-toggle"
                  on:click={() => showOptionalFields = !showOptionalFields}
                >
                  <h3 class="fields-group-title">
                    Optional Fields ({optionalFields.length})
                  </h3>
                  <span class="toggle-icon">{showOptionalFields ? 'v' : '>'}</span>
                </button>

                {#if showOptionalFields}
                  <div class="fields-grid">
                    {#each optionalFields as field}
                      <div class="form-group" class:full-width={field.type === 'text'}>
                        <label for={`field-${field.name}`}>
                          {field.label}
                          <span class="field-type-tag">{field.type}</span>
                        </label>
                        {#if field.type === 'date'}
                          <input
                            type="date"
                            id={`field-${field.name}`}
                            value={fieldValues[field.name] || ''}
                            on:input={(e) => updateField(field.name, e.currentTarget?.value || '')}
                          />
                        {:else if field.type === 'number'}
                          <input
                            type="number"
                            id={`field-${field.name}`}
                            value={fieldValues[field.name] || ''}
                            on:input={(e) => updateField(field.name, e.currentTarget?.value || '')}
                            placeholder={`Enter ${field.label}`}
                          />
                        {:else}
                          <input
                            type="text"
                            id={`field-${field.name}`}
                            value={fieldValues[field.name] || ''}
                            on:input={(e) => updateField(field.name, e.currentTarget?.value || '')}
                            placeholder={`Enter ${field.label}`}
                          />
                        {/if}
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
            {/if}

            <!-- Children preview -->
            {#if releaseEntityDef.children && releaseEntityDef.children.length > 0}
              <div class="children-info">
                <h3 class="fields-group-title">Available Child Entities</h3>
                <p class="helper-text">After creating this Release, you can add the following child elements:</p>
                <div class="children-tags">
                  {#each releaseEntityDef.children as child}
                    <button
                      type="button"
                      class="child-tag child-tag-clickable"
                      on:click={() => handleCreateAndAddChild(child)}
                      disabled={submitting}
                    >{child}</button>
                  {/each}
                </div>
              </div>
            {/if}
          </section>
        {/if}
      {/if}

      <!-- Actions -->
      <div class="form-actions">
        <button type="button" class="btn btn-secondary" on:click={handleCancel}>
          Cancel
        </button>
        <button
          type="submit"
          class="btn btn-primary"
          disabled={submitting || !selectedPreservationId || !releaseEntityDef}
        >
          {submitting ? 'Creating Release...' : 'Create Release'}
        </button>
      </div>
    </form>
  {/if}
</div>

<style>
  .dip-container {
    max-width: 960px;
    margin: 2rem auto;
    background: var(--arc-card, #fff);
    padding: 2.5rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .dip-header {
    margin-bottom: 2rem;
    padding-bottom: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .dip-header-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
  }

  .dip-header h1 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.75rem;
    font-weight: 700;
  }

  .subtitle {
    margin: 0;
    color: var(--arc-muted);
    font-size: 0.925rem;
  }

  /* Alert */
  .alert {
    padding: 0.875rem 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1.5rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .alert-error {
    background: var(--arc-alert-red-bg);
    color: var(--arc-alert-red-ink);
    border: 1px solid var(--arc-alert-red-border);
  }

  .alert button {
    background: none;
    border: none;
    color: inherit;
    font-size: 1.25rem;
    cursor: pointer;
    padding: 0 0.25rem;
    box-shadow: none;
  }

  .alert button:hover {
    background: none;
    transform: none;
    box-shadow: none;
  }

  /* Loading */
  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 3rem 2rem;
    gap: 1rem;
  }

  /* .spinner chrome (ring, colors, keyframes) comes from the global kit in
     app.css; this page only scales it up and slows it down slightly. */
  .loading-state .spinner {
    width: 2.5rem;
    height: 2.5rem;
    animation: spin 1s linear infinite;
  }

  .loading-state p {
    color: var(--arc-muted);
    font-size: 0.875rem;
  }

  /* Sections */
  .form-section {
    margin-bottom: 2rem;
    padding-bottom: 2rem;
    border-bottom: 1px solid var(--arc-line);
  }

  .form-section:last-of-type {
    border-bottom: none;
  }

  .section-title {
    margin: 0 0 1.25rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.15rem;
    font-weight: 600;
  }

  .btn-defaults {
    padding: 0.625rem 1.25rem;
    background: var(--arc-chip-soft-indigo-bg);
    color: var(--arc-chip-indigo-ink);
    border: 1px solid var(--arc-hover-border);
    font-size: 0.85rem;
    white-space: nowrap;
    flex-shrink: 0;
    box-shadow: none;
  }

  .btn-defaults:hover:not(:disabled) {
    background: var(--arc-chip-indigo-bg);
    transform: translateY(-2px);
    box-shadow: none;
  }

  .btn-defaults:disabled {
    background: var(--arc-disabled-bg);
    cursor: not-allowed;
    transform: none;
  }

  .standard-info {
    padding: 1rem;
    background: var(--arc-chip-orange-bg);
    border: 1px solid var(--arc-chip-orange-hover);
    border-radius: 0.6rem;
    border-left: 4px solid #f97316;
  }

  .standard-info strong {
    display: block;
    color: var(--arc-ink);
    margin-bottom: 0.25rem;
    font-size: 0.925rem;
  }

  .standard-ref {
    display: block;
    color: var(--arc-muted);
    font-size: 0.75rem;
    margin-bottom: 0.5rem;
  }

  .standard-info p {
    margin: 0;
    color: var(--arc-body);
    font-size: 0.85rem;
    line-height: 1.5;
  }

  /* Form Fields */
  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
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

  .field-hint {
    display: block;
    margin-top: 0.375rem;
    color: var(--arc-faint);
    font-size: 0.75rem;
  }

  .link-btn {
    background: none;
    border: none;
    color: var(--arc-indigo, #6366f1);
    font-size: 0.75rem;
    cursor: pointer;
    padding: 0;
    text-decoration: underline;
    box-shadow: none;
  }

  .link-btn:hover {
    background: none;
    color: var(--arc-indigo-deep, #4f46e5);
    transform: none;
    box-shadow: none;
  }

  .field-type-tag {
    font-size: 0.65rem;
    color: var(--arc-faint);
    font-weight: 400;
    font-style: italic;
    margin-left: auto;
  }

  /* Input chrome (width, border, radius, focus ring) comes from the global
     rules in app.css; only this page's tighter scale is layered on top. */
  .form-group input,
  .form-group select,
  .form-group textarea {
    padding: 0.625rem 0.75rem;
    font-size: 0.875rem;
  }

  .form-group input:disabled {
    background: var(--arc-card-2);
    color: var(--arc-faint);
  }

  .form-group textarea {
    resize: vertical;
  }

  /* Entity info */
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

  /* Field Groups */
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

  /* Optional fields toggle */
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

  /* Children info */
  .children-info {
    margin-top: 1.5rem;
    padding: 1rem;
    background: var(--arc-card-2);
    border: 1px solid var(--arc-line-strong);
    border-radius: 0.5rem;
  }

  .children-info .fields-group-title {
    margin-bottom: 0.5rem;
  }

  .helper-text {
    margin: 0 0 0.75rem 0;
    color: var(--arc-muted);
    font-size: 0.8rem;
  }

  .children-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .child-tag {
    padding: 0.25rem 0.75rem;
    background: var(--arc-chip-orange-bg);
    color: var(--arc-chip-orange-ink);
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
    border: 1px solid transparent;
    box-shadow: none;
  }

  .child-tag-clickable {
    cursor: pointer;
    transition: all 0.18s ease;
  }

  .child-tag-clickable:hover:not(:disabled) {
    background: var(--arc-chip-orange-hover);
    color: var(--arc-chip-orange-ink);
    border-color: #f97316;
    transform: none;
    box-shadow: none;
  }

  .child-tag-clickable:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* Actions — .form-actions, .btn-primary and .btn-secondary come from the
     global kit in app.css; only the taller footer and this page's wider
     buttons are layered on top. */
  .form-actions {
    margin-top: 2rem;
    border-top-color: var(--arc-line, #e8edf3);
  }

  .btn {
    padding: 0.75rem 2rem;
    font-size: 0.875rem;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .dip-container {
      margin: 1rem;
      padding: 1.5rem;
    }

    .form-row,
    .fields-grid {
      grid-template-columns: 1fr;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .spinner {
      animation: none;
    }

    .btn,
    .child-tag-clickable {
      transition: none;
    }

    .btn-primary:hover:not(:disabled),
    .btn-defaults:hover:not(:disabled) {
      transform: none;
    }
  }
</style>
