<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_PRESERVATION, GET_ALL_PRESERVATIONS, GET_ARCHIVES_BY_TENANT, GET_TENANT, GET_INTAKES_BY_TENANT_V2, PREFILL_INTAKE_FIELDS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { standards } from '$lib/standards';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';
  import FileDropZone from '$lib/components/FileDropZone.svelte';

  interface PageData { tenantId: string; }
  export let data: PageData;

  let tenant: any = null;
  let archives: any[] = [];
  let sips: any[] = [];
  let users: any[] = [];
  let selectedArchiveId = '';
  let selectedArchive: any = null;
  let selectedIntakeId = '';
  let selectedStandard: typeof standards[0] | null = null;
  let schemaData: any = null;
  let preservationEntityDef: any = null;
  let fieldValues: Record<string, string> = {};
  let selectedUserId = '';
  let aipTitle = '';
  let aipDescription = '';
  let loading = false;
  let loadingSchema = false;
  let submitting = false;
  let error: string | null = null;
  let showOptionalFields = false;
  let pendingChildEntity: string | null = null;
  let droppedFiles: File[] = [];
  let prefilling = false;
  let prefilledFromFiles = false;

  let currentRole = '';
  let hasAccess = false;

  $: requiredFields = preservationEntityDef?.fields?.filter((f: any) => f.required) || [];
  $: optionalFields = preservationEntityDef?.fields?.filter((f: any) => !f.required) || [];
  $: filteredIntakes = selectedArchive
    ? sips.filter((s: any) => s.standard === selectedArchive.standard)
    : sips;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to create Preservations');
      goto(`/tenants/${data.tenantId}/preservations`);
      return;
    }

    loading = true;
    try {
      const [tenantResult, archivesResult, usersResult, sipsResult] = await Promise.all([
        client.query({ query: GET_TENANT, variables: { id: data.tenantId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ARCHIVES_BY_TENANT, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_USERS }),
        client.query({ query: GET_INTAKES_BY_TENANT_V2, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' })
      ]);
      tenant = tenantResult?.data?.getTenant;
      archives = archivesResult?.data?.getArchivesByTenant || [];
      users = usersResult?.data?.getAllUsers || [];
      sips = sipsResult?.data?.getIntakesByTenantV2 || [];
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load data';
    } finally {
      loading = false;
    }
  });

  async function onArchiveSelect() {
    if (!selectedArchiveId) {
      selectedArchive = null;
      selectedStandard = null;
      preservationEntityDef = null;
      schemaData = null;
      fieldValues = {};
      aipTitle = '';
      aipDescription = '';
      selectedUserId = '';
      selectedIntakeId = '';
      prefilledFromFiles = false;
      return;
    }

    selectedArchive = archives.find((a: any) => a.id === selectedArchiveId);
    if (!selectedArchive) return;

    aipTitle = selectedArchive.title ? `Preservation - ${selectedArchive.title}` : '';
    aipDescription = selectedArchive.description || '';

    const matchedStandard = standards.find(s => s.graphql === selectedArchive.standard);
    if (matchedStandard) {
      selectedStandard = matchedStandard;
      await loadSchema();
    }

    if (selectedArchive.ownerId) {
      const ownerMatch = users.find((u: any) => u.id === selectedArchive.ownerId);
      if (ownerMatch) selectedUserId = ownerMatch.id;
    }

    applyAllDefaults();
  }

  async function loadSchema() {
    if (!selectedStandard) return;
    loadingSchema = true;
    try {
      const response = await fetch(`/schemeDefintions/${selectedStandard.file}`);
      if (!response.ok) throw new Error(`Failed to load ${selectedStandard.file}`);
      schemaData = await response.json();

      const entity = schemaData.entities?.find((e: any) => e.name === selectedStandard!.preservationEntity);
      if (!entity) throw new Error(`Preservation entity "${selectedStandard.preservationEntity}" not found in schema`);

      preservationEntityDef = entity;
      fieldValues = {};
      if (entity.fields) {
        entity.fields.forEach((field: any) => { fieldValues[field.name] = ''; });
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load schema';
      preservationEntityDef = null;
    } finally {
      loadingSchema = false;
    }
  }

  function genId() {
    return `Preservation-${selectedArchiveId || Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
  }

  function getStandardDefaults(standardKey: string, title: string, description: string): Record<string, string> {
    const today = new Date().toISOString().split('T')[0];
    const id = genId();
    const user = users.find((u: any) => u.id === selectedUserId);
    const userName = user?.name || 'System';

    const map: Record<string, Record<string, string>> = {
      'NOARK5': { systemID: id, title: title || 'New Archive', description: description || '', archiveStatus: 'Created', documentMedium: 'Electronic archive', storageLocation: 'Default storage', createdDate: today, createdBy: userName, closedDate: today, closedBy: userName },
      'OAIS': { aipID: id, title: title || 'New Preservation package', description: description || '', creationDate: today, version: '1.0', accessRights: 'restricted', packageType: 'AIP' },
      'PREMIS': { objectIdentifierType: 'local', objectIdentifierValue: id, objectCategory: 'Representation', preservationLevelType: 'full', preservationLevelValue: 'full preservation', preservationLevelRole: 'requirement', preservationLevelRationale: 'Default preservation policy', preservationLevelDateAssigned: today, significantPropertiesType: 'content', significantPropertiesValue: 'All content preserved', originalName: title || 'Untitled Object' },
      'Dublin Core': { resourceIdentifier: id, resourceType: 'Dataset' },
      'METS': { metsID: id, objID: `OBJ-${id}`, label: title || 'New METS Document', type: 'digital object', profile: 'http://www.loc.gov/standards/mets/profiles' },
      'EAD': { eadID: id, audience: 'external', relatedEncoding: 'Dublin Core', lang: 'eng', script: 'Latn', base: '' },
      'BagIt': { bagName: (title || 'new-bag').toLowerCase().replace(/[^a-z0-9]+/g, '-'), payloadOxum: '0.0', bagSize: '0 KB', isComplete: 'true', isValid: 'true' },
      'ISAD(G)': { descriptionID: id, levelOfDescription: 'Fonds' },
      'MODS': { modsID: id, version: '3.8' },
      'E-ARK': { packageID: id, title: title || 'New Preservation package', description: description || '', profile: 'https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml', contentInformationType: 'MIXED', oaisPackageType: 'AIP', creationDate: today, creator: userName, preservationLevel: 'full', representationCount: '1' },
    };
    return map[standardKey] || {};
  }

  function applyAllDefaults() {
    if (!selectedUserId && users.length > 0) selectedUserId = users[0].id;
    if (!aipTitle) aipTitle = `${selectedStandard?.label || 'Archive'} Preservation - ${new Date().toLocaleDateString()}`;
    if (!aipDescription) aipDescription = `Preservation package created using the ${selectedStandard?.label || 'selected'} standard.`;

    if (!preservationEntityDef?.fields || !selectedStandard) return;
    const defaults = getStandardDefaults(selectedStandard.key, aipTitle, aipDescription);
    for (const field of preservationEntityDef.fields) {
      const val = defaults[field.name];
      if (val !== undefined) fieldValues[field.name] = val;
    }
    fieldValues = fieldValues;
  }

  function updateField(name: string, value: string) {
    fieldValues[name] = value;
    fieldValues = fieldValues;
  }

  async function handleFilesProcessed(e: CustomEvent<{ files: File[], metadata: any }>) {
    const { files, metadata } = e.detail;
    droppedFiles = files;
    if (!selectedStandard) return;

    prefilling = true;
    prefilledFromFiles = false;
    try {
      const user = users.find((u: any) => u.id === selectedUserId);
      const fileMetadata = { ...metadata, uploaderName: user?.name || 'System' };

      const result = await client.query({
        query: PREFILL_INTAKE_FIELDS,
        variables: { standard: selectedStandard.graphql, fileMetadata },
        fetchPolicy: 'network-only'
      });

      const prefilled = result?.data?.prefillIntakeFields || [];
      for (const { name, value } of prefilled) {
        if (preservationEntityDef?.fields?.some((f: any) => f.name === name)) {
          fieldValues[name] = value;
        }
      }
      fieldValues = fieldValues;

      if (files.length === 1 && metadata.filename) {
        const baseName = metadata.filename.replace(/\.[^.]+$/, '');
        if (!aipTitle || aipTitle.startsWith('Preservation -')) aipTitle = `Preservation - ${baseName}`;
      }

      prefilledFromFiles = true;
      toasts.add('Fields auto-populated from file metadata', 'success');
    } catch (err) {
      console.error('Prefill failed:', err);
      toasts.add('Failed to auto-populate fields from files', 'error');
    } finally {
      prefilling = false;
    }
  }

  function handleFilesCleared() {
    droppedFiles = [];
    prefilledFromFiles = false;
  }

  function validateForm(): string | null {
    if (!selectedArchiveId) return 'Please select an archive';
    if (!selectedUserId) return 'Please select an owner';
    if (!aipTitle.trim()) return 'Please enter a title';
    for (const field of requiredFields) {
      if (!fieldValues[field.name]?.trim()) return `Required field "${field.label}" is empty`;
    }
    return null;
  }

  async function handleSubmit() {
    const validationError = validateForm();
    if (validationError) { error = validationError; return; }

    submitting = true;
    error = null;

    try {
      const aipContent = JSON.stringify({
        aipType: selectedStandard!.preservationLabel,
        standard: selectedStandard!.key,
        entity: preservationEntityDef.name,
        fields: fieldValues
      }, null, 2);

      const elementIdentifier = fieldValues['systemID'] || fieldValues['aipID'] || fieldValues['packageID'] || fieldValues['objectIdentifierValue'] || fieldValues['bagName'] || fieldValues['metsID'] || fieldValues['eadID'] || fieldValues['descriptionID'] || fieldValues['modsID'] || fieldValues['resourceIdentifier'] || `Preservation-${Date.now()}`;

      const fields = preservationEntityDef.fields.map((fieldDef: any) => ({
        name: fieldDef.name, label: fieldDef.label, type: fieldDef.type, value: fieldValues[fieldDef.name] || ''
      }));

      const input: any = {
        userId: selectedUserId, title: aipTitle, description: aipDescription || null,
        content: aipContent, standard: selectedStandard!.graphql,
        elementIdentifier, entityName: preservationEntityDef.name, entityType: preservationEntityDef.type,
        elementTitle: aipTitle, elementDescription: aipDescription || null,
        createdBy: selectedUserId, fields
      };

      if (selectedIntakeId) {
        input.sourceIntakeId = selectedIntakeId;
      }

      const result = await client.mutate({
        mutation: CREATE_PRESERVATION,
        variables: { input },
        refetchQueries: [{ query: GET_ALL_PRESERVATIONS }],
        awaitRefetchQueries: true
      });

      const newPreservationId = result?.data?.createPreservation?.id;
      toasts.add(`Preservation "${aipTitle}" created successfully using ${selectedStandard!.label}`, 'success');

      if (pendingChildEntity && newPreservationId) {
        goto(`/preservation/edit/${newPreservationId}?addChild=${encodeURIComponent(pendingChildEntity)}`);
      } else {
        goto(`/tenants/${data.tenantId}/preservations`);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to create Preservation';
      toasts.add(`Failed to create Preservation: ${error}`, 'error');
    } finally {
      submitting = false;
    }
  }

  function handleCreateAndAddChild(childEntityName: string) {
    pendingChildEntity = childEntityName;
    handleSubmit();
  }

  function handleCancel() {
    goto(`/tenants/${data.tenantId}/preservations`);
  }
</script>

<svelte:head><title>Create Preservation - {tenant?.displayName || tenant?.name || 'Tenant'} - Arcana</title></svelte:head>

{#if !hasAccess && !loading}
  <div class="access-denied">
    <div class="access-denied-icon">🚫</div>
    <h1>Access Denied</h1>
    <p>You don't have permission to create Preservations.</p>
    <p class="redirect-message">Redirecting...</p>
  </div>
{:else}
  <div class="aip-container">
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[
        { label: 'Preservations', href: `/tenants/${data.tenantId}/preservations` },
        { label: 'Create' }
      ]}
    />

    <div class="aip-header">
      <div class="aip-header-top">
        <div>
          <span class="eyebrow">Preservation</span>
          <h1>Create Preservation package</h1>
          {#if tenant}<p class="subtitle">Tenant: {tenant.displayName || tenant.name}</p>{/if}
        </div>
        {#if selectedStandard}
          <button type="button" class="btn btn-defaults" on:click={applyAllDefaults} disabled={loadingSchema} title="Auto-fill all sections with sensible defaults for {selectedStandard.label}">
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

        <section class="form-section">
          <h2 class="section-title">1. Select Archive</h2>
          <div class="form-group">
            <label for="archiveSelect">Archive <span class="req">*</span></label>
            <select id="archiveSelect" bind:value={selectedArchiveId} on:change={onArchiveSelect} required>
              <option value="">-- Select an archive --</option>
              {#each archives as archive}
                <option value={archive.id}>#{archive.id} - {archive.title} ({archive.standard})</option>
              {/each}
            </select>
            <span class="field-hint">The standard and defaults will be derived from the selected archive.</span>
          </div>

          {#if selectedStandard && schemaData}
            <div class="standard-info">
              <strong>{schemaData.fullName}</strong>
              <span class="standard-ref">{schemaData.reference}</span>
              <p>{schemaData.description}</p>
            </div>
          {/if}
        </section>

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
                <label for="aipTitle">Preservation Title <span class="req">*</span></label>
                <input type="text" id="aipTitle" bind:value={aipTitle} required placeholder="Enter Preservation title" />
              </div>
            </div>
            <div class="form-group">
              <label for="aipDescription">Description</label>
              <textarea id="aipDescription" bind:value={aipDescription} rows="3" placeholder="Brief description of this archival package..."></textarea>
            </div>
            <div class="form-group">
              <label for="sourceIntake">Source Intake (optional)</label>
              <select id="sourceIntake" bind:value={selectedIntakeId}>
                <option value="">-- No source Intake --</option>
                {#each filteredIntakes as sip}
                  <option value={sip.id}>#{sip.id} - {sip.title} ({sip.status})</option>
                {/each}
              </select>
              <span class="field-hint">Optionally link this Preservation to a source Intake.</span>
            </div>
          </section>

          {#if selectedStandard}
            <section class="form-section">
              <h2 class="section-title">Upload Files</h2>
              <p class="section-hint">Drop files to auto-populate Preservation metadata fields from file properties.</p>
              <FileDropZone on:filesProcessed={handleFilesProcessed} on:filesCleared={handleFilesCleared} disabled={prefilling} />
              {#if prefilling}<p class="prefill-status">Auto-populating fields...</p>{/if}
              {#if prefilledFromFiles}<p class="prefill-success">Fields populated from file metadata. Review and edit below.</p>{/if}
            </section>
          {/if}

          {#if loadingSchema}
            <section class="form-section">
              <div class="loading-state">
                <div class="spinner"></div>
                <p>Loading {selectedStandard?.label} schema...</p>
              </div>
            </section>
          {:else if preservationEntityDef}
            <section class="form-section">
              <h2 class="section-title">3. {selectedStandard?.preservationLabel} Fields</h2>

              {#if preservationEntityDef.description}<p class="entity-description">{preservationEntityDef.description}</p>{/if}
              {#if preservationEntityDef.note}<div class="entity-note">{preservationEntityDef.note}</div>{/if}

              {#if requiredFields.length > 0}
                <div class="fields-group">
                  <h3 class="fields-group-title">Required Fields</h3>
                  <div class="fields-grid">
                    {#each requiredFields as field}
                      <div class="form-group" class:full-width={field.type === 'text'}>
                        <label for={`field-${field.name}`}>
                          {field.label} <span class="req">*</span>
                          <span class="field-type-tag">{field.type}</span>
                        </label>
                        {#if field.type === 'date'}
                          <input type="date" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} />
                        {:else if field.type === 'number'}
                          <input type="number" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />
                        {:else}
                          <input type="text" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />
                        {/if}
                      </div>
                    {/each}
                  </div>
                </div>
              {/if}

              {#if optionalFields.length > 0}
                <div class="fields-group optional-group">
                  <button type="button" class="fields-group-toggle" on:click={() => showOptionalFields = !showOptionalFields}>
                    <h3 class="fields-group-title">Optional Fields ({optionalFields.length})</h3>
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
                            <input type="date" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} />
                          {:else if field.type === 'number'}
                            <input type="number" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />
                          {:else}
                            <input type="text" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />
                          {/if}
                        </div>
                      {/each}
                    </div>
                  {/if}
                </div>
              {/if}

              {#if preservationEntityDef.children && preservationEntityDef.children.length > 0}
                <div class="children-info">
                  <h3 class="fields-group-title">Available Child Entities</h3>
                  <p class="helper-text">Click to create Preservation and add a child element:</p>
                  <div class="children-tags">
                    {#each preservationEntityDef.children as child}
                      <button type="button" class="child-tag child-tag-clickable" on:click={() => handleCreateAndAddChild(child)} disabled={submitting}>{child}</button>
                    {/each}
                  </div>
                </div>
              {/if}
            </section>
          {/if}
        {/if}

        <div class="form-actions">
          <button type="button" class="btn btn-secondary" on:click={handleCancel}>Cancel</button>
          <button type="submit" class="btn btn-primary" disabled={submitting || !selectedArchiveId || !preservationEntityDef}>
            {submitting ? 'Creating Preservation...' : 'Create Preservation'}
          </button>
        </div>
      </form>
    {/if}
  </div>
{/if}

<style>
  .access-denied { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 400px; text-align: center; padding: 3rem; }
  .access-denied-icon { font-size: 5rem; margin-bottom: 1.5rem; }
  .access-denied h1 { margin: 0 0 1rem 0; color: var(--arc-ink); font-size: 2rem; }
  .access-denied p { margin: 0.5rem 0; color: var(--arc-muted); font-size: 1.125rem; }
  .redirect-message { color: var(--arc-link); font-weight: 500; animation: pulse 1.5s ease-in-out infinite; }
  @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
  .aip-container { max-width: 960px; margin: 2rem auto; background: var(--arc-card); padding: 2.5rem; border-radius: 1rem; box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04)); border: 1px solid var(--arc-line); }
  .aip-header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 1px solid var(--arc-line); }
  .aip-header-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
  .aip-header h1 { margin: 0 0 0.5rem 0; color: var(--arc-ink); font-size: 1.75rem; font-weight: 700; }
  .subtitle { margin: 0; color: var(--arc-muted); font-size: 0.925rem; }
  .alert { padding: 0.875rem 1rem; border-radius: 0.6rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
  .alert-error { background: var(--arc-alert-red-bg); color: var(--arc-alert-red-ink); border: 1px solid var(--arc-alert-red-border); }
  .alert button { background: none; border: none; box-shadow: none; color: inherit; font-size: 1.25rem; cursor: pointer; padding: 0 0.25rem; }
  .alert button:hover { background: none; transform: none; box-shadow: none; }
  .loading-state { display: flex; flex-direction: column; align-items: center; padding: 3rem 2rem; gap: 1rem; }
  .loading-state .spinner { width: 2.5rem; height: 2.5rem; border: 3px solid var(--arc-line-strong); border-top-color: var(--arc-link); border-radius: 50%; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .loading-state p { color: var(--arc-muted); font-size: 0.875rem; }
  .form-section { margin-bottom: 2rem; padding-bottom: 2rem; border-bottom: 1px solid var(--arc-line); }
  .form-section:last-of-type { border-bottom: none; }
  .section-title { margin: 0 0 1.25rem 0; color: var(--arc-ink); font-size: 1.15rem; font-weight: 600; }
  .section-hint { margin: 0 0 1rem 0; color: var(--arc-muted); font-size: 0.85rem; }
  .prefill-status { margin: 0.75rem 0 0; color: var(--arc-link); font-size: 0.8rem; font-weight: 500; }
  .prefill-success { margin: 0.75rem 0 0; color: var(--arc-alert-green-ink); font-size: 0.8rem; font-weight: 500; }
  .btn-defaults { padding: 0.625rem 1.25rem; background: var(--arc-card); color: var(--arc-ink); font-size: 0.85rem; white-space: nowrap; flex-shrink: 0; border: 1.5px solid var(--arc-line-strong); border-radius: 0.65rem; cursor: pointer; font-weight: 600; box-shadow: none; transition: border-color 0.18s ease, color 0.18s ease; }
  .btn-defaults:hover:not(:disabled) { background: var(--arc-card); border-color: var(--arc-indigo); color: var(--arc-link); transform: none; box-shadow: none; }
  .btn-defaults:disabled { background: var(--arc-card-2); color: var(--arc-faint); border-color: var(--arc-line-strong); cursor: not-allowed; box-shadow: none; }
  .standard-info { padding: 1rem; background: var(--arc-alert-indigo-bg); border: 1px solid var(--arc-alert-indigo-border); border-radius: 0.6rem; border-left: 3px solid #6366f1; }
  .standard-info strong { display: block; color: var(--arc-ink); margin-bottom: 0.25rem; font-size: 0.925rem; }
  .standard-ref { display: block; color: var(--arc-muted); font-size: 0.75rem; margin-bottom: 0.5rem; }
  .standard-info p { margin: 0; color: var(--arc-body); font-size: 0.85rem; line-height: 1.5; }
  .field-hint { display: block; margin-top: 0.375rem; color: var(--arc-faint); font-size: 0.75rem; }
  .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
  .form-group { margin-bottom: 1.25rem; }
  .form-group label { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; color: var(--arc-body); font-weight: 600; font-size: 0.85rem; }
  .req { color: #ef4444; font-weight: 600; }
  .field-type-tag { font-size: 0.65rem; color: var(--arc-faint); font-weight: 400; font-style: italic; margin-left: auto; }
  /* Inputs, selects & textareas are styled by the global app.css rules */
  .form-group textarea { resize: vertical; }
  .entity-description { margin: 0 0 1rem 0; color: var(--arc-body); font-size: 0.875rem; line-height: 1.5; }
  .entity-note { padding: 0.75rem 1rem; background: var(--arc-alert-amber-bg); border: 1px solid var(--arc-alert-amber-border); border-radius: 0.375rem; margin-bottom: 1.5rem; font-size: 0.8rem; color: var(--arc-alert-amber-ink); line-height: 1.5; }
  .fields-group { margin-bottom: 1.5rem; }
  .fields-group-title { margin: 0 0 1rem 0; color: var(--arc-body); font-size: 0.925rem; font-weight: 600; }
  .fields-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 1rem; }
  .fields-grid .full-width { grid-column: 1 / -1; }
  .optional-group { border: 1px solid var(--arc-line); border-radius: 0.6rem; padding: 1rem; background: var(--arc-card-2); }
  .fields-group-toggle { display: flex; align-items: center; justify-content: space-between; width: 100%; background: none; border: none; box-shadow: none; cursor: pointer; padding: 0; margin-bottom: 0; color: inherit; }
  .fields-group-toggle:hover { background: none; transform: none; box-shadow: none; }
  .fields-group-toggle .fields-group-title { margin: 0; color: var(--arc-muted); }
  .toggle-icon { font-size: 1rem; color: var(--arc-faint); transition: transform 0.2s; }
  .children-info { margin-top: 1.5rem; padding: 1rem; background: var(--arc-card-2); border: 1px solid var(--arc-line); border-radius: 0.6rem; }
  .children-info .fields-group-title { margin-bottom: 0.5rem; }
  .helper-text { margin: 0 0 0.75rem 0; color: var(--arc-muted); font-size: 0.8rem; }
  .children-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
  .child-tag { padding: 0.25rem 0.75rem; background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); border-radius: 9999px; font-size: 0.75rem; font-weight: 500; box-shadow: none; }
  .child-tag-clickable { border: 1px solid var(--arc-hover-border); cursor: pointer; transition: all 0.15s; }
  .child-tag-clickable:hover:not(:disabled) { background: #6366f1; color: white; border-color: var(--arc-indigo); transform: none; box-shadow: none; }
  .child-tag-clickable:disabled { opacity: 0.5; cursor: not-allowed; }
  .form-actions { display: flex; justify-content: space-between; gap: 1rem; margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--arc-line); }
  .btn { padding: 0.75rem 2rem; border: none; border-radius: 0.65rem; font-weight: 700; cursor: pointer; transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, border-color 0.18s ease, color 0.18s ease; font-size: 0.875rem; }
  .btn-primary { background: var(--arc-grad-brand, linear-gradient(135deg, #6366f1, #8b5cf6)); color: white; box-shadow: 0 10px 30px -8px rgba(124, 58, 237, 0.6); }
  .btn-primary:hover:not(:disabled) { background: var(--arc-grad-brand-hover, linear-gradient(135deg, #4f46e5, #7c3aed)); transform: translateY(-2px); }
  .btn-primary:disabled { background: var(--arc-disabled-bg); cursor: not-allowed; transform: none; box-shadow: none; }
  .btn-secondary { background: var(--arc-card); color: var(--arc-ink); border: 1.5px solid var(--arc-line-strong); box-shadow: none; }
  .btn-secondary:hover { background: var(--arc-card); border-color: var(--arc-indigo); color: var(--arc-link); transform: none; box-shadow: none; }
  @media (max-width: 768px) { .aip-container { margin: 1rem; padding: 1.5rem; } .form-row, .fields-grid { grid-template-columns: 1fr; } }
</style>