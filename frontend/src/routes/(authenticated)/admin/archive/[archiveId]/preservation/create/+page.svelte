<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_PRESERVATION, GET_ALL_PRESERVATIONS, GET_INTAKES_BY_TENANT_V2, PREFILL_INTAKE_FIELDS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { standards } from '$lib/standards';
  import { gql } from '@apollo/client/core';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';
  import FileDropZone from '$lib/components/FileDropZone.svelte';

  interface PageData { archiveId: string; }
  export let data: PageData;

  const GET_ARCHIVE = gql`query GetArchive($id: ID!) { getArchive(id: $id) { id title description standard ownerId tenantId } }`;

  let archive: any = null;
  let sips: any[] = [];
  let selectedIntakeId = '';
  let selectedStandard: typeof standards[0] | null = null;
  let schemaData: any = null;
  let preservationEntityDef: any = null;
  let fieldValues: Record<string, string> = {};
  let users: any[] = [];
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

  $: requiredFields = preservationEntityDef?.fields?.filter((f: any) => f.required) || [];
  $: optionalFields = preservationEntityDef?.fields?.filter((f: any) => !f.required) || [];
  $: filteredIntakes = archive ? sips.filter((s: any) => s.standard === archive.standard) : sips;

  onMount(async () => {
    loading = true;
    try {
      const [archiveResult, usersResult] = await Promise.all([
        client.query({ query: GET_ARCHIVE, variables: { id: data.archiveId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_USERS })
      ]);
      archive = archiveResult?.data?.getArchive;
      users = usersResult?.data?.getAllUsers || [];

      if (archive?.tenantId) {
        const sipsResult = await client.query({ query: GET_INTAKES_BY_TENANT_V2, variables: { tenantId: archive.tenantId.toString() }, fetchPolicy: 'network-only' });
        sips = sipsResult?.data?.getIntakesByTenantV2 || [];
      }

      if (archive) await prefillFromArchive();
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load data';
    } finally { loading = false; }
  });

  async function prefillFromArchive() {
    aipTitle = archive.title ? `Preservation - ${archive.title}` : '';
    aipDescription = archive.description || '';
    const matchedStandard = standards.find(s => s.graphql === archive.standard);
    if (matchedStandard) { selectedStandard = matchedStandard; await loadSchema(); }
    if (archive.ownerId) { const m = users.find((u: any) => u.id === archive.ownerId); if (m) selectedUserId = m.id; }
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
      if (entity.fields) entity.fields.forEach((field: any) => { fieldValues[field.name] = ''; });
    } catch (e) { error = e instanceof Error ? e.message : 'Failed to load schema'; preservationEntityDef = null; }
    finally { loadingSchema = false; }
  }

  function genId() { return `Preservation-${data.archiveId}-${Math.random().toString(36).substring(2, 8)}`; }

  function getStandardDefaults(standardKey: string, title: string, description: string): Record<string, string> {
    const today = new Date().toISOString().split('T')[0]; const id = genId();
    const user = users.find((u: any) => u.id === selectedUserId); const userName = user?.name || 'System';
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
    for (const field of preservationEntityDef.fields) { const val = defaults[field.name]; if (val !== undefined) fieldValues[field.name] = val; }
    fieldValues = fieldValues;
  }

  function updateField(name: string, value: string) { fieldValues[name] = value; fieldValues = fieldValues; }

  async function handleFilesProcessed(e: CustomEvent<{ files: File[], metadata: any }>) {
    const { files, metadata } = e.detail; droppedFiles = files;
    if (!selectedStandard) return;
    prefilling = true; prefilledFromFiles = false;
    try {
      const user = users.find((u: any) => u.id === selectedUserId);
      const result = await client.query({ query: PREFILL_INTAKE_FIELDS, variables: { standard: selectedStandard.graphql, fileMetadata: { ...metadata, uploaderName: user?.name || 'System' } }, fetchPolicy: 'network-only' });
      for (const { name, value } of (result?.data?.prefillIntakeFields || [])) { if (preservationEntityDef?.fields?.some((f: any) => f.name === name)) fieldValues[name] = value; }
      fieldValues = fieldValues;
      if (files.length === 1 && metadata.filename) { const baseName = metadata.filename.replace(/\.[^.]+$/, ''); if (!aipTitle || aipTitle.startsWith('Preservation -')) aipTitle = `Preservation - ${baseName}`; }
      prefilledFromFiles = true; toasts.add('Fields auto-populated from file metadata', 'success');
    } catch (err) { console.error('Prefill failed:', err); toasts.add('Failed to auto-populate fields', 'error'); }
    finally { prefilling = false; }
  }

  function handleFilesCleared() { droppedFiles = []; prefilledFromFiles = false; }

  function validateForm(): string | null {
    if (!selectedUserId) return 'Please select an owner';
    if (!aipTitle.trim()) return 'Please enter a title';
    for (const field of requiredFields) { if (!fieldValues[field.name]?.trim()) return `Required field "${field.label}" is empty`; }
    return null;
  }

  async function handleSubmit() {
    const validationError = validateForm(); if (validationError) { error = validationError; return; }
    submitting = true; error = null;
    try {
      const aipContent = JSON.stringify({ aipType: selectedStandard!.preservationLabel, standard: selectedStandard!.key, entity: preservationEntityDef.name, fields: fieldValues }, null, 2);
      const elementIdentifier = fieldValues['systemID'] || fieldValues['aipID'] || fieldValues['packageID'] || fieldValues['objectIdentifierValue'] || fieldValues['bagName'] || fieldValues['metsID'] || fieldValues['eadID'] || fieldValues['descriptionID'] || fieldValues['modsID'] || fieldValues['resourceIdentifier'] || `Preservation-${Date.now()}`;
      const fields = preservationEntityDef.fields.map((fieldDef: any) => ({ name: fieldDef.name, label: fieldDef.label, type: fieldDef.type, value: fieldValues[fieldDef.name] || '' }));
      const input: any = { userId: selectedUserId, title: aipTitle, description: aipDescription || null, content: aipContent, standard: selectedStandard!.graphql, elementIdentifier, entityName: preservationEntityDef.name, entityType: preservationEntityDef.type, elementTitle: aipTitle, elementDescription: aipDescription || null, createdBy: selectedUserId, fields };
      if (selectedIntakeId) input.sourceIntakeId = selectedIntakeId;
      await client.mutate({ mutation: CREATE_PRESERVATION, variables: { input }, refetchQueries: [{ query: GET_ALL_PRESERVATIONS }], awaitRefetchQueries: true });
      toasts.add(`Preservation "${aipTitle}" created successfully using ${selectedStandard!.label}`, 'success');
      goto('/admin/archives');
    } catch (e) { error = e instanceof Error ? e.message : 'Failed to create Preservation'; toasts.add(`Failed to create Preservation: ${error}`, 'error'); }
    finally { submitting = false; }
  }

  function handleCreateAndAddChild(childEntityName: string) { pendingChildEntity = childEntityName; handleSubmit(); }
  function handleCancel() { goto('/admin/archives'); }
</script>

<svelte:head><title>Create Preservation - {archive?.title || 'Archive'} - Arcana</title></svelte:head>

<div class="aip-container">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Archives', href: '/admin/archives' }, { label: archive?.title || 'Archive' }, { label: 'Create Preservation' }]} />

  <div class="aip-header"><div class="aip-header-top"><div><h1>Create Preservation package</h1>{#if archive}<p class="subtitle">Archive: {archive.title} ({archive.standard})</p>{/if}</div>
    {#if selectedStandard}<button type="button" class="btn btn-defaults" on:click={applyAllDefaults} disabled={loadingSchema}>Fill All Defaults</button>{/if}
  </div></div>

  {#if error}<div class="alert alert-error">{error}<button on:click={() => error = null}>x</button></div>{/if}

  {#if loading}<div class="loading-state"><div class="spinner"></div><p>Loading...</p></div>
  {:else}
    <form on:submit|preventDefault={handleSubmit}>
      {#if selectedStandard && schemaData}<section class="form-section"><div class="standard-info"><strong>{schemaData.fullName}</strong><span class="standard-ref">{schemaData.reference}</span><p>{schemaData.description}</p></div></section>{/if}

      <section class="form-section">
        <h2 class="section-title">Basic Information</h2>
        <div class="form-row">
          <div class="form-group"><label for="userId">Owner <span class="req">*</span></label><select id="userId" bind:value={selectedUserId} required><option value="">Select an owner</option>{#each users as user}<option value={user.id}>{user.name} ({user.email})</option>{/each}</select></div>
          <div class="form-group"><label for="aipTitle">Preservation Title <span class="req">*</span></label><input type="text" id="aipTitle" bind:value={aipTitle} required placeholder="Enter Preservation title" /></div>
        </div>
        <div class="form-group"><label for="aipDescription">Description</label><textarea id="aipDescription" bind:value={aipDescription} rows="3" placeholder="Brief description of this archival package..."></textarea></div>
        <div class="form-group"><label for="sourceIntake">Source Intake (optional)</label><select id="sourceIntake" bind:value={selectedIntakeId}><option value="">-- No source Intake --</option>{#each filteredIntakes as sip}<option value={sip.id}>#{sip.id} - {sip.title} ({sip.status})</option>{/each}</select><span class="field-hint">Optionally link this Preservation to a source Intake.</span></div>
      </section>

      {#if selectedStandard}
        <section class="form-section"><h2 class="section-title">Upload Files</h2><p class="section-hint">Drop files to auto-populate Preservation metadata fields.</p><FileDropZone on:filesProcessed={handleFilesProcessed} on:filesCleared={handleFilesCleared} disabled={prefilling} />{#if prefilling}<p class="prefill-status">Auto-populating fields...</p>{/if}{#if prefilledFromFiles}<p class="prefill-success">Fields populated from file metadata.</p>{/if}</section>
      {/if}

      {#if loadingSchema}<section class="form-section"><div class="loading-state"><div class="spinner"></div><p>Loading {selectedStandard?.label} schema...</p></div></section>
      {:else if preservationEntityDef}
        <section class="form-section">
          <h2 class="section-title">{selectedStandard?.preservationLabel} Fields</h2>
          {#if preservationEntityDef.description}<p class="entity-description">{preservationEntityDef.description}</p>{/if}
          {#if preservationEntityDef.note}<div class="entity-note">{preservationEntityDef.note}</div>{/if}
          {#if requiredFields.length > 0}<div class="fields-group"><h3 class="fields-group-title">Required Fields</h3><div class="fields-grid">{#each requiredFields as field}<div class="form-group" class:full-width={field.type === 'text'}><label for={`field-${field.name}`}>{field.label} <span class="req">*</span><span class="field-type-tag">{field.type}</span></label>{#if field.type === 'date'}<input type="date" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} />{:else if field.type === 'number'}<input type="number" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{:else}<input type="text" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{/if}</div>{/each}</div></div>{/if}
          {#if optionalFields.length > 0}<div class="fields-group optional-group"><button type="button" class="fields-group-toggle" on:click={() => showOptionalFields = !showOptionalFields}><h3 class="fields-group-title">Optional Fields ({optionalFields.length})</h3><span class="toggle-icon">{showOptionalFields ? 'v' : '>'}</span></button>{#if showOptionalFields}<div class="fields-grid">{#each optionalFields as field}<div class="form-group" class:full-width={field.type === 'text'}><label for={`field-${field.name}`}>{field.label}<span class="field-type-tag">{field.type}</span></label>{#if field.type === 'date'}<input type="date" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} />{:else if field.type === 'number'}<input type="number" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{:else}<input type="text" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{/if}</div>{/each}</div>{/if}</div>{/if}
          {#if preservationEntityDef.children && preservationEntityDef.children.length > 0}<div class="children-info"><h3 class="fields-group-title">Available Child Entities</h3><p class="helper-text">Click to create Preservation and add a child element:</p><div class="children-tags">{#each preservationEntityDef.children as child}<button type="button" class="child-tag child-tag-clickable" on:click={() => handleCreateAndAddChild(child)} disabled={submitting}>{child}</button>{/each}</div></div>{/if}
        </section>
      {/if}

      <div class="form-actions"><button type="button" class="btn btn-secondary" on:click={handleCancel}>Cancel</button><button type="submit" class="btn btn-primary" disabled={submitting || !preservationEntityDef}>{submitting ? 'Creating Preservation...' : 'Create Preservation'}</button></div>
    </form>
  {/if}
</div>

<style>
  .aip-container { max-width: 960px; margin: 2rem auto; background: white; padding: 2.5rem; border-radius: 0.75rem; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1); border: 1px solid #e2e8f0; }
  .aip-header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 2px solid #e2e8f0; }
  .aip-header-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
  .aip-header h1 { margin: 0 0 0.5rem 0; color: #1e293b; font-size: 1.75rem; font-weight: 700; }
  .subtitle { margin: 0; color: #64748b; font-size: 0.925rem; }
  .alert { padding: 0.875rem 1rem; border-radius: 0.375rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
  .alert-error { background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; }
  .alert button { background: none; border: none; color: inherit; font-size: 1.25rem; cursor: pointer; padding: 0 0.25rem; }
  .loading-state { display: flex; flex-direction: column; align-items: center; padding: 3rem 2rem; gap: 1rem; }
  .loading-state .spinner { width: 2.5rem; height: 2.5rem; border: 3px solid #e2e8f0; border-top-color: #6366f1; border-radius: 50%; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .loading-state p { color: #64748b; font-size: 0.875rem; }
  .form-section { margin-bottom: 2rem; padding-bottom: 2rem; border-bottom: 1px solid #f1f5f9; }
  .form-section:last-of-type { border-bottom: none; }
  .section-title { margin: 0 0 1.25rem 0; color: #1e293b; font-size: 1.15rem; font-weight: 600; }
  .section-hint { margin: 0 0 1rem 0; color: #64748b; font-size: 0.85rem; }
  .prefill-status { margin: 0.75rem 0 0; color: #6366f1; font-size: 0.8rem; font-weight: 500; }
  .prefill-success { margin: 0.75rem 0 0; color: #059669; font-size: 0.8rem; font-weight: 500; }
  .btn-defaults { padding: 0.625rem 1.25rem; background: #10b981; color: white; font-size: 0.85rem; white-space: nowrap; flex-shrink: 0; border: none; border-radius: 0.375rem; cursor: pointer; font-weight: 600; }
  .btn-defaults:hover:not(:disabled) { background: #059669; }
  .btn-defaults:disabled { background: #94a3b8; cursor: not-allowed; }
  .standard-info { padding: 1rem; background: #eef2ff; border: 1px solid #c7d2fe; border-radius: 0.5rem; border-left: 4px solid #6366f1; }
  .standard-info strong { display: block; color: #1e293b; margin-bottom: 0.25rem; font-size: 0.925rem; }
  .standard-ref { display: block; color: #64748b; font-size: 0.75rem; margin-bottom: 0.5rem; }
  .standard-info p { margin: 0; color: #475569; font-size: 0.85rem; line-height: 1.5; }
  .field-hint { display: block; margin-top: 0.375rem; color: #94a3b8; font-size: 0.75rem; }
  .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
  .form-group { margin-bottom: 1.25rem; }
  .form-group label { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; color: #1e293b; font-weight: 500; font-size: 0.85rem; }
  .req { color: #ef4444; font-weight: 600; }
  .field-type-tag { font-size: 0.65rem; color: #94a3b8; font-weight: 400; font-style: italic; margin-left: auto; }
  .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 0.625rem 0.75rem; border: 1px solid #e2e8f0; border-radius: 0.375rem; font-size: 0.875rem; transition: border-color 0.2s; background: white; }
  .form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1); }
  .form-group textarea { resize: vertical; font-family: inherit; }
  .entity-description { margin: 0 0 1rem 0; color: #475569; font-size: 0.875rem; line-height: 1.5; }
  .entity-note { padding: 0.75rem 1rem; background: #fffbeb; border: 1px solid #fde68a; border-radius: 0.375rem; margin-bottom: 1.5rem; font-size: 0.8rem; color: #92400e; line-height: 1.5; }
  .fields-group { margin-bottom: 1.5rem; }
  .fields-group-title { margin: 0 0 1rem 0; color: #334155; font-size: 0.925rem; font-weight: 600; }
  .fields-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 1rem; }
  .fields-grid .full-width { grid-column: 1 / -1; }
  .optional-group { border: 1px solid #e2e8f0; border-radius: 0.5rem; padding: 1rem; background: #fafbfc; }
  .fields-group-toggle { display: flex; align-items: center; justify-content: space-between; width: 100%; background: none; border: none; cursor: pointer; padding: 0; color: inherit; }
  .fields-group-toggle .fields-group-title { margin: 0; color: #64748b; }
  .toggle-icon { font-size: 1rem; color: #94a3b8; }
  .children-info { margin-top: 1.5rem; padding: 1rem; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 0.5rem; }
  .children-info .fields-group-title { margin-bottom: 0.5rem; }
  .helper-text { margin: 0 0 0.75rem 0; color: #64748b; font-size: 0.8rem; }
  .children-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
  .child-tag { padding: 0.25rem 0.75rem; background: #e0e7ff; color: #3730a3; border-radius: 1rem; font-size: 0.75rem; font-weight: 500; }
  .child-tag-clickable { border: 1px solid #c7d2fe; cursor: pointer; transition: all 0.15s; }
  .child-tag-clickable:hover:not(:disabled) { background: #6366f1; color: white; border-color: #6366f1; }
  .child-tag-clickable:disabled { opacity: 0.5; cursor: not-allowed; }
  .form-actions { display: flex; justify-content: space-between; gap: 1rem; margin-top: 2rem; padding-top: 1.5rem; border-top: 2px solid #e2e8f0; }
  .btn { padding: 0.75rem 2rem; border: none; border-radius: 0.375rem; font-weight: 600; cursor: pointer; transition: all 0.2s; font-size: 0.875rem; }
  .btn-primary { background: #6366f1; color: white; }
  .btn-primary:hover:not(:disabled) { background: #4f46e5; }
  .btn-primary:disabled { background: #94a3b8; cursor: not-allowed; }
  .btn-secondary { background: #e2e8f0; color: #475569; }
  .btn-secondary:hover { background: #cbd5e1; }
  @media (max-width: 768px) { .aip-container { margin: 1rem; padding: 1.5rem; } .form-row, .fields-grid { grid-template-columns: 1fr; } }
</style>