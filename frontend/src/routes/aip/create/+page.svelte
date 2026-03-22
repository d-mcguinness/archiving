<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_AIP, GET_ALL_AIPS, GET_ALL_ARCHIVES, GET_ARCHIVES_BY_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { standards, graphqlToKey, type StandardDefinition } from '$lib/standards';

  function getAipsPath() {
    const { role } = get(auth);
    if (role === 'ADMIN' || role === 'TENANT') return '/aip';
    return '/';
  }

  let selectedStandardKey = '';
  let selectedStandard: StandardDefinition | null = null;
  let schemaData: any = null;
  let aipEntityDef: any = null;
  let fieldValues: Record<string, string> = {};
  let users: any[] = [];
  let archives: any[] = [];
  let selectedUserId = '';
  let selectedArchiveId = '';
  let aipTitle = '';
  let aipDescription = '';
  let sourceArchiveId = '';
  let sourceSipId = '';
  let loading = false;
  let loadingSchema = false;
  let submitting = false;
  let error: string | null = null;
  let showOptionalFields = false;
  let prefilled = false;
  let pendingChildEntity: string | null = null;

  // Computed field groups
  $: requiredFields = aipEntityDef?.fields?.filter((f: any) => f.required) || [];
  $: optionalFields = aipEntityDef?.fields?.filter((f: any) => !f.required) || [];

  async function onArchiveSelect() {
    if (!selectedArchiveId) {
      // Reset everything when clearing archive selection
      sourceArchiveId = '';
      selectedStandardKey = '';
      selectedStandard = null;
      aipEntityDef = null;
      schemaData = null;
      fieldValues = {};
      aipTitle = '';
      aipDescription = '';
      selectedUserId = '';
      prefilled = false;
      return;
    }
    const archive = archives.find((a: any) => a.id === selectedArchiveId);
    if (!archive) return;

    sourceArchiveId = archive.id;
    aipTitle = archive.title ? `AIP - ${archive.title}` : '';
    aipDescription = archive.description || '';

    // Derive standard from the archive
    const matchedStandard = standards.find(s => s.graphql === archive.standard);
    if (matchedStandard) {
      selectedStandardKey = matchedStandard.key;
      await onStandardChange();
    }

    // Set owner to archive owner if we have a matching user
    if (archive.ownerId) {
      const ownerMatch = users.find((u: any) => u.id === archive.ownerId);
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
      const [usersResult, archivesResult] = await Promise.all([
        client.query({ query: GET_ALL_USERS }),
        authState.role === 'TENANT' && authState.tenantId
          ? client.query({ query: GET_ARCHIVES_BY_TENANT, variables: { tenantId: authState.tenantId.toString() } })
          : client.query({ query: GET_ALL_ARCHIVES })
      ]);
      users = usersResult?.data?.getAllUsers || [];
      archives = (authState.role === 'TENANT' && authState.tenantId)
        ? archivesResult?.data?.getArchivesByTenant || []
        : archivesResult?.data?.getAllArchives || [];
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
    const qSipId = params.get('sourceSipId');
    const qArchiveId = params.get('archiveId');

    if (qUserId) selectedUserId = qUserId;
    if (qTitle) aipTitle = qTitle;
    if (qDescription) aipDescription = qDescription;
    if (qSipId) sourceSipId = qSipId;

    // Auto-select archive from query param
    if (qArchiveId) {
      const match = archives.find((a: any) => a.id === qArchiveId);
      if (match) {
        selectedArchiveId = qArchiveId;
        await onArchiveSelect();
      }
    }
  });

  async function onStandardChange() {
    selectedStandard = standards.find(s => s.key === selectedStandardKey) || null;
    aipEntityDef = null;
    fieldValues = {};
    error = null;

    if (!selectedStandard) return;

    loadingSchema = true;
    try {
      const response = await fetch(`/schemeDefintions/${selectedStandard.file}`);
      if (!response.ok) throw new Error(`Failed to load ${selectedStandard.file}`);
      schemaData = await response.json();

      // Find the AIP root entity
      const entity = schemaData.entities?.find((e: any) => e.name === selectedStandard!.aipEntity);
      if (!entity) throw new Error(`AIP entity "${selectedStandard.aipEntity}" not found in schema`);

      aipEntityDef = entity;

      // Initialize field values
      fieldValues = {};
      if (entity.fields) {
        entity.fields.forEach((field: any) => {
          fieldValues[field.name] = '';
        });
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load schema';
      aipEntityDef = null;
    } finally {
      loadingSchema = false;
    }
  }

  function genId() {
    return `AIP-${sourceArchiveId || sourceSipId || Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
  }

  function getStandardDefaults(standardKey: string, title: string, description: string): Record<string, string> {
    const today = new Date().toISOString().split('T')[0];
    const id = genId();
    const user = users.find((u: any) => u.id === selectedUserId);
    const userName = user?.name || 'System';

    const map: Record<string, Record<string, string>> = {
      'E-ARK': {
        packageID: id,
        title: title || 'New Archival Information Package',
        description: description || '',
        profile: 'https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml',
        contentInformationType: 'MIXED',
        oaisPackageType: 'AIP',
        creationDate: today,
        creator: userName,
        preservationLevel: 'full',
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
        title: title || 'New Archival Information Package',
        description: description || '',
        creationDate: today,
        version: '1.0',
        accessRights: 'Restricted',
        packageType: 'AIP',
      },
      'PREMIS': {
        objectIdentifierType: 'local',
        objectIdentifierValue: id,
        objectCategory: 'Representation',
        preservationLevelType: 'full',
        preservationLevelValue: 'full preservation',
        preservationLevelRole: 'requirement',
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
    if (!aipTitle) {
      const standardLabel = selectedStandard?.label || 'Archive';
      aipTitle = `${standardLabel} AIP - ${new Date().toLocaleDateString()}`;
    }
    if (!aipDescription) {
      aipDescription = `Archival Information Package created using the ${selectedStandard?.label || 'selected'} standard.`;
    }

    if (!aipEntityDef?.fields || !selectedStandard) return;

    const defaults = getStandardDefaults(selectedStandard.key, aipTitle, aipDescription);

    for (const field of aipEntityDef.fields) {
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
    if (!selectedArchiveId) return 'Please select an archive';
    if (!selectedUserId) return 'Please select an owner';
    if (!selectedStandard) return 'Please select a standard';
    if (!aipTitle.trim()) return 'Please enter a title';

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
      const aipContent = JSON.stringify({
        aipType: selectedStandard!.aipLabel,
        standard: selectedStandard!.key,
        entity: aipEntityDef.name,
        fields: fieldValues
      }, null, 2);

      const elementIdentifier = fieldValues['packageID'] || fieldValues['systemID'] || fieldValues['aipID'] || fieldValues['objectIdentifierValue'] || fieldValues['bagName'] || fieldValues['metsID'] || fieldValues['eadID'] || fieldValues['descriptionID'] || fieldValues['modsID'] || fieldValues['resourceIdentifier'] || `AIP-${Date.now()}`;

      const fields = aipEntityDef.fields.map((fieldDef: any) => ({
        name: fieldDef.name,
        label: fieldDef.label,
        type: fieldDef.type,
        value: fieldValues[fieldDef.name] || ''
      }));

      const input: Record<string, any> = {
        userId: selectedUserId,
        title: aipTitle,
        description: aipDescription || null,
        content: aipContent,
        standard: selectedStandard!.graphql,
        elementIdentifier,
        entityName: aipEntityDef.name,
        entityType: aipEntityDef.type,
        elementTitle: aipTitle,
        elementDescription: aipDescription || null,
        createdBy: selectedUserId,
        fields
      };

      if (sourceSipId) {
        input.sourceSipId = sourceSipId;
      }

      const result = await client.mutate({
        mutation: CREATE_AIP,
        variables: { input },
        refetchQueries: [{ query: GET_ALL_AIPS }],
        awaitRefetchQueries: true
      });

      const newAipId = result?.data?.createAip?.id;
      toasts.add(`AIP "${aipTitle}" created successfully using ${selectedStandard!.label}`, 'success');

      if (pendingChildEntity && newAipId) {
        goto(`/aip/edit/${newAipId}?addChild=${encodeURIComponent(pendingChildEntity)}`);
      } else if (newAipId) {
        goto(`/aip/edit/${newAipId}`);
      } else {
        goto(getAipsPath());
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to create AIP';
      toasts.add(`Failed to create AIP: ${error}`, 'error');
    } finally {
      submitting = false;
    }
  }

  function handleCreateAndAddChild(childEntityName: string) {
    pendingChildEntity = childEntityName;
    handleSubmit();
  }

  function handleCancel() {
    goto(getAipsPath());
  }
</script>

<div class="aip-container">
  <div class="aip-header">
    <div class="aip-header-top">
      <div>
        <h1>Create Archival Information Package</h1>
        <p class="subtitle">Build an AIP by selecting an archive</p>
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

      <!-- Section 1: Select Archive -->
      <section class="form-section">
        <h2 class="section-title">1. Select Archive</h2>

        <div class="form-group">
          <label for="archiveSelect">Archive for AIP <span class="req">*</span></label>
          <select id="archiveSelect" bind:value={selectedArchiveId} on:change={onArchiveSelect} required>
            <option value="">-- Select an archive --</option>
            {#each archives as archive}
              <option value={archive.id}>
                #{archive.id} - {archive.title} ({archive.standard})
              </option>
            {/each}
          </select>
          <span class="field-hint">The standard, title, and description will be derived from the selected archive.</span>
        </div>

        {#if selectedStandard && schemaData}
          <div class="standard-info">
            <strong>{schemaData.fullName}</strong>
            <span class="standard-ref">{schemaData.reference}</span>
            <p>{schemaData.description}</p>
          </div>
        {/if}
      </section>

      <!-- Section 2: Basic Info (only when archive selected) -->
      {#if selectedArchiveId}
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
              <label for="aipTitle">AIP Title <span class="req">*</span></label>
              <input type="text" id="aipTitle" bind:value={aipTitle} required placeholder="Enter AIP title" />
            </div>
          </div>

          <div class="form-group">
            <label for="aipDescription">Description</label>
            <textarea id="aipDescription" bind:value={aipDescription} rows="3" placeholder="Brief description of this archival information package..."></textarea>
          </div>

          <div class="form-group">
            <label for="sourceSipId">Source SIP ID (optional)</label>
            <input type="text" id="sourceSipId" bind:value={sourceSipId} placeholder="Link to originating SIP (optional)" />
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
        {:else if aipEntityDef}
          <section class="form-section">
            <h2 class="section-title">
              3. {selectedStandard?.aipLabel} Fields
            </h2>

            {#if aipEntityDef.description}
              <p class="entity-description">{aipEntityDef.description}</p>
            {/if}

            {#if aipEntityDef.note}
              <div class="entity-note">
                {aipEntityDef.note}
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
            {#if aipEntityDef.children && aipEntityDef.children.length > 0}
              <div class="children-info">
                <h3 class="fields-group-title">Available Child Entities</h3>
                <p class="helper-text">After creating this AIP, you can add the following child elements:</p>
                <div class="children-tags">
                  {#each aipEntityDef.children as child}
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
          disabled={submitting || !selectedArchiveId || !aipEntityDef}
        >
          {submitting ? 'Creating AIP...' : 'Create AIP'}
        </button>
      </div>
    </form>
  {/if}
</div>

<style>
  .aip-container {
    max-width: 960px;
    margin: 2rem auto;
    background: white;
    padding: 2.5rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .aip-header {
    margin-bottom: 2rem;
    padding-bottom: 1.5rem;
    border-bottom: 2px solid #e2e8f0;
  }

  .aip-header-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
  }

  .aip-header h1 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1.75rem;
    font-weight: 700;
  }

  .subtitle {
    margin: 0;
    color: #64748b;
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
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }

  .alert button {
    background: none;
    border: none;
    color: inherit;
    font-size: 1.25rem;
    cursor: pointer;
    padding: 0 0.25rem;
  }

  /* Loading */
  .loading-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 3rem 2rem;
    gap: 1rem;
  }

  .loading-state .spinner {
    width: 2.5rem;
    height: 2.5rem;
    border: 3px solid #e2e8f0;
    border-top-color: #6366f1;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .loading-state p {
    color: #64748b;
    font-size: 0.875rem;
  }

  /* Sections */
  .form-section {
    margin-bottom: 2rem;
    padding-bottom: 2rem;
    border-bottom: 1px solid #f1f5f9;
  }

  .form-section:last-of-type {
    border-bottom: none;
  }

  .section-title {
    margin: 0 0 1.25rem 0;
    color: #1e293b;
    font-size: 1.15rem;
    font-weight: 600;
  }

  .btn-defaults {
    padding: 0.625rem 1.25rem;
    background: #10b981;
    color: white;
    font-size: 0.85rem;
    white-space: nowrap;
    flex-shrink: 0;
  }

  .btn-defaults:hover:not(:disabled) {
    background: #059669;
  }

  .btn-defaults:disabled {
    background: #94a3b8;
    cursor: not-allowed;
  }

  .standard-info {
    padding: 1rem;
    background: #eef2ff;
    border: 1px solid #c7d2fe;
    border-radius: 0.5rem;
    border-left: 4px solid #6366f1;
  }

  .standard-info strong {
    display: block;
    color: #1e293b;
    margin-bottom: 0.25rem;
    font-size: 0.925rem;
  }

  .standard-ref {
    display: block;
    color: #64748b;
    font-size: 0.75rem;
    margin-bottom: 0.5rem;
  }

  .standard-info p {
    margin: 0;
    color: #475569;
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
    color: #1e293b;
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
    color: #94a3b8;
    font-size: 0.75rem;
  }

  .field-type-tag {
    font-size: 0.65rem;
    color: #94a3b8;
    font-weight: 400;
    font-style: italic;
    margin-left: auto;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    width: 100%;
    padding: 0.625rem 0.75rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    transition: border-color 0.2s;
    background: white;
  }

  .form-group input:focus,
  .form-group select:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
  }

  .form-group textarea {
    resize: vertical;
    font-family: inherit;
  }

  /* Entity info */
  .entity-description {
    margin: 0 0 1rem 0;
    color: #475569;
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .entity-note {
    padding: 0.75rem 1rem;
    background: #fffbeb;
    border: 1px solid #fde68a;
    border-radius: 0.375rem;
    margin-bottom: 1.5rem;
    font-size: 0.8rem;
    color: #92400e;
    line-height: 1.5;
  }

  /* Field Groups */
  .fields-group {
    margin-bottom: 1.5rem;
  }

  .fields-group-title {
    margin: 0 0 1rem 0;
    color: #334155;
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
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    padding: 1rem;
    background: #fafbfc;
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
  }

  .fields-group-toggle:hover {
    background: none;
  }

  .fields-group-toggle .fields-group-title {
    margin: 0;
    color: #64748b;
  }

  .toggle-icon {
    font-size: 1rem;
    color: #94a3b8;
    transition: transform 0.2s;
  }

  /* Children info */
  .children-info {
    margin-top: 1.5rem;
    padding: 1rem;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
  }

  .children-info .fields-group-title {
    margin-bottom: 0.5rem;
  }

  .helper-text {
    margin: 0 0 0.75rem 0;
    color: #64748b;
    font-size: 0.8rem;
  }

  .children-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  .child-tag {
    padding: 0.25rem 0.75rem;
    background: #e0e7ff;
    color: #3730a3;
    border-radius: 1rem;
    font-size: 0.75rem;
    font-weight: 500;
    border: 1px solid transparent;
  }

  .child-tag-clickable {
    cursor: pointer;
    transition: all 0.2s;
  }

  .child-tag-clickable:hover:not(:disabled) {
    background: #c7d2fe;
    border-color: #6366f1;
  }

  .child-tag-clickable:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  /* Actions */
  .form-actions {
    display: flex;
    justify-content: space-between;
    gap: 1rem;
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 2px solid #e2e8f0;
  }

  .btn {
    padding: 0.75rem 2rem;
    border: none;
    border-radius: 0.375rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 0.875rem;
  }

  .btn-primary {
    background: #6366f1;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #4f46e5;
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

  /* Responsive */
  @media (max-width: 768px) {
    .aip-container {
      margin: 1rem;
      padding: 1.5rem;
    }

    .form-row,
    .fields-grid {
      grid-template-columns: 1fr;
    }
  }
</style>
