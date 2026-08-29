<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_RELEASE, GET_ALL_RELEASES, GET_PRESERVATIONS_BY_TENANT, PREFILL_INTAKE_FIELDS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { standards } from '$lib/standards';
  import { gql } from '@apollo/client/core';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';
  import FileDropZone from '$lib/components/FileDropZone.svelte';

  interface PageData { archiveId: string; }
  export let data: PageData;

  const GET_ARCHIVE = gql`query GetArchive($id: ID!) { getArchive(id: $id) { id title description standard ownerId tenantId } }`;

  let archive: any = null;
  let aips: any[] = [];
  let selectedPreservationId = '';
  let selectedStandard: typeof standards[0] | null = null;
  let schemaData: any = null;
  let releaseEntityDef: any = null;
  let fieldValues: Record<string, string> = {};
  let users: any[] = [];
  let selectedUserId = '';
  let dipTitle = '';
  let dipDescription = '';
  let loading = false;
  let loadingSchema = false;
  let submitting = false;
  let error: string | null = null;
  let showOptionalFields = false;
  let pendingChildEntity: string | null = null;
  let droppedFiles: File[] = [];
  let prefilling = false;
  let prefilledFromFiles = false;

  $: requiredFields = releaseEntityDef?.fields?.filter((f: any) => f.required) || [];
  $: optionalFields = releaseEntityDef?.fields?.filter((f: any) => !f.required) || [];
  $: filteredPreservations = archive ? aips.filter((a: any) => a.standard === archive.standard) : aips;

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
        const aipsResult = await client.query({ query: GET_PRESERVATIONS_BY_TENANT, variables: { tenantId: archive.tenantId.toString() }, fetchPolicy: 'network-only' });
        aips = aipsResult?.data?.getPreservationsByTenant || [];
      }

      if (archive) await prefillFromArchive();
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load data';
    } finally { loading = false; }
  });

  async function prefillFromArchive() {
    dipTitle = archive.title ? `Release - ${archive.title}` : '';
    dipDescription = archive.description || '';
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
      const entity = schemaData.entities?.find((e: any) => e.name === selectedStandard!.releaseEntity);
      if (!entity) throw new Error(`Release entity "${selectedStandard.releaseEntity}" not found in schema`);
      releaseEntityDef = entity;
      fieldValues = {};
      if (entity.fields) entity.fields.forEach((field: any) => { fieldValues[field.name] = ''; });
    } catch (e) { error = e instanceof Error ? e.message : 'Failed to load schema'; releaseEntityDef = null; }
    finally { loadingSchema = false; }
  }

  function genId() { return `Release-${data.archiveId}-${Math.random().toString(36).substring(2, 8)}`; }

  function getStandardDefaults(standardKey: string, title: string, description: string): Record<string, string> {
    const today = new Date().toISOString().split('T')[0]; const id = genId();
    const user = users.find((u: any) => u.id === selectedUserId); const userName = user?.name || 'System';
    const map: Record<string, Record<string, string>> = {
      'NOARK5': { systemID: id, title: title || 'New Archive', description: description || '', archiveStatus: 'Created', documentMedium: 'Electronic archive', storageLocation: 'Default storage', createdDate: today, createdBy: userName, closedDate: today, closedBy: userName },
      'OAIS': { dipID: id, title: title || 'New Release package', description: description || '', creationDate: today, requestDate: today, consumer: userName, accessRights: 'public', packageType: 'DIP' },
      'PREMIS': { objectIdentifierType: 'local', objectIdentifierValue: id, objectCategory: 'Representation', preservationLevelType: 'full', preservationLevelValue: 'full preservation', preservationLevelRole: 'requirement', preservationLevelRationale: 'Default preservation policy', preservationLevelDateAssigned: today, significantPropertiesType: 'content', significantPropertiesValue: 'All content preserved', originalName: title || 'Untitled Object' },
      'Dublin Core': { resourceIdentifier: id, resourceType: 'Dataset' },
      'METS': { metsID: id, objID: `OBJ-${id}`, label: title || 'New METS Document', type: 'digital object', profile: 'http://www.loc.gov/standards/mets/profiles' },
      'EAD': { eadID: id, audience: 'external', relatedEncoding: 'Dublin Core', lang: 'eng', script: 'Latn', base: '' },
      'BagIt': { bagName: (title || 'new-bag').toLowerCase().replace(/[^a-z0-9]+/g, '-'), payloadOxum: '0.0', bagSize: '0 KB', isComplete: 'true', isValid: 'true' },
      'ISAD(G)': { descriptionID: id, levelOfDescription: 'Fonds' },
      'MODS': { modsID: id, version: '3.8' },
      'E-ARK': { packageID: id, title: title || 'New Release package', description: description || '', profile: 'https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml', contentInformationType: 'MIXED', oaisPackageType: 'DIP', creationDate: today, creator: userName, preservationLevel: 'full', representationCount: '1' },
    };
    return map[standardKey] || {};
  }

  function applyAllDefaults() {
    if (!selectedUserId && users.length > 0) selectedUserId = users[0].id;
    if (!dipTitle) dipTitle = `${selectedStandard?.label || 'Archive'} Release - ${new Date().toLocaleDateString()}`;
    if (!dipDescription) dipDescription = `Release package created using the ${selectedStandard?.label || 'selected'} standard.`;
    if (!releaseEntityDef?.fields || !selectedStandard) return;
    const defaults = getStandardDefaults(selectedStandard.key, dipTitle, dipDescription);
    for (const field of releaseEntityDef.fields) { const val = defaults[field.name]; if (val !== undefined) fieldValues[field.name] = val; }
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
      for (const { name, value } of (result?.data?.prefillIntakeFields || [])) { if (releaseEntityDef?.fields?.some((f: any) => f.name === name)) fieldValues[name] = value; }
      fieldValues = fieldValues;
      if (files.length === 1 && metadata.filename) { const baseName = metadata.filename.replace(/\.[^.]+$/, ''); if (!dipTitle || dipTitle.startsWith('Release -')) dipTitle = `Release - ${baseName}`; }
      prefilledFromFiles = true; toasts.add('Fields auto-populated from file metadata', 'success');
    } catch (err) { console.error('Prefill failed:', err); toasts.add('Failed to auto-populate fields', 'error'); }
    finally { prefilling = false; }
  }

  function handleFilesCleared() { droppedFiles = []; prefilledFromFiles = false; }

  function validateForm(): string | null {
    if (!selectedUserId) return 'Please select an owner';
    if (!dipTitle.trim()) return 'Please enter a title';
    for (const field of requiredFields) { if (!fieldValues[field.name]?.trim()) return `Required field "${field.label}" is empty`; }
    return null;
  }

  async function handleSubmit() {
    const validationError = validateForm(); if (validationError) { error = validationError; return; }
    submitting = true; error = null;
    try {
      const dipContent = JSON.stringify({ dipType: selectedStandard!.releaseLabel, standard: selectedStandard!.key, entity: releaseEntityDef.name, fields: fieldValues }, null, 2);
      const elementIdentifier = fieldValues['systemID'] || fieldValues['dipID'] || fieldValues['packageID'] || fieldValues['objectIdentifierValue'] || fieldValues['bagName'] || fieldValues['metsID'] || fieldValues['eadID'] || fieldValues['descriptionID'] || fieldValues['modsID'] || fieldValues['resourceIdentifier'] || `Release-${Date.now()}`;
      const fields = releaseEntityDef.fields.map((fieldDef: any) => ({ name: fieldDef.name, label: fieldDef.label, type: fieldDef.type, value: fieldValues[fieldDef.name] || '' }));
      const input: any = { userId: selectedUserId, title: dipTitle, description: dipDescription || null, content: dipContent, standard: selectedStandard!.graphql, elementIdentifier, entityName: releaseEntityDef.name, entityType: releaseEntityDef.type, elementTitle: dipTitle, elementDescription: dipDescription || null, createdBy: selectedUserId, fields };
      if (selectedPreservationId) input.sourcePreservationId = selectedPreservationId;
      await client.mutate({ mutation: CREATE_RELEASE, variables: { input }, refetchQueries: [{ query: GET_ALL_RELEASES }], awaitRefetchQueries: true });
      toasts.add(`Release "${dipTitle}" created successfully using ${selectedStandard!.label}`, 'success');
      goto('/admin/archives');
    } catch (e) { error = e instanceof Error ? e.message : 'Failed to create Release'; toasts.add(`Failed to create Release: ${error}`, 'error'); }
    finally { submitting = false; }
  }

  function handleCreateAndAddChild(childEntityName: string) { pendingChildEntity = childEntityName; handleSubmit(); }
  function handleCancel() { goto('/admin/archives'); }
</script>

<svelte:head><title>Create Release - {archive?.title || 'Archive'} - Arcana</title></svelte:head>

<div class="dip-container">
  <Breadcrumb items={[{ label: 'Admin', href: '/admin' }, { label: 'Archives', href: '/admin/archives' }, { label: archive?.title || 'Archive' }, { label: 'Create Release' }]} />

  <div class="dip-header"><div class="dip-header-top"><div><span class="eyebrow">Admin console</span><h1>Create Release package</h1>{#if archive}<p class="subtitle">Archive: {archive.title} ({archive.standard})</p>{/if}</div>
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
          <div class="form-group"><label for="dipTitle">Release Title <span class="req">*</span></label><input type="text" id="dipTitle" bind:value={dipTitle} required placeholder="Enter Release title" /></div>
        </div>
        <div class="form-group"><label for="dipDescription">Description</label><textarea id="dipDescription" bind:value={dipDescription} rows="3" placeholder="Brief description of this dissemination package..."></textarea></div>
        <div class="form-group"><label for="sourcePreservation">Source Preservation (optional)</label><select id="sourcePreservation" bind:value={selectedPreservationId}><option value="">-- No source Preservation --</option>{#each filteredPreservations as aip}<option value={aip.id}>#{aip.id} - {aip.title} ({aip.status})</option>{/each}</select><span class="field-hint">Optionally link this Release to a source Preservation.</span></div>
      </section>

      {#if selectedStandard}
        <section class="form-section"><h2 class="section-title">Upload Files</h2><p class="section-hint">Drop files to auto-populate Release metadata fields.</p><FileDropZone on:filesProcessed={handleFilesProcessed} on:filesCleared={handleFilesCleared} disabled={prefilling} />{#if prefilling}<p class="prefill-status">Auto-populating fields...</p>{/if}{#if prefilledFromFiles}<p class="prefill-success">Fields populated from file metadata.</p>{/if}</section>
      {/if}

      {#if loadingSchema}<section class="form-section"><div class="loading-state"><div class="spinner"></div><p>Loading {selectedStandard?.label} schema...</p></div></section>
      {:else if releaseEntityDef}
        <section class="form-section">
          <h2 class="section-title">{selectedStandard?.releaseLabel} Fields</h2>
          {#if releaseEntityDef.description}<p class="entity-description">{releaseEntityDef.description}</p>{/if}
          {#if releaseEntityDef.note}<div class="entity-note">{releaseEntityDef.note}</div>{/if}
          {#if requiredFields.length > 0}<div class="fields-group"><h3 class="fields-group-title">Required Fields</h3><div class="fields-grid">{#each requiredFields as field}<div class="form-group" class:full-width={field.type === 'text'}><label for={`field-${field.name}`}>{field.label} <span class="req">*</span><span class="field-type-tag">{field.type}</span></label>{#if field.type === 'date'}<input type="date" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} />{:else if field.type === 'number'}<input type="number" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{:else}<input type="text" id={`field-${field.name}`} value={fieldValues[field.name] || ''} required on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{/if}</div>{/each}</div></div>{/if}
          {#if optionalFields.length > 0}<div class="fields-group optional-group"><button type="button" class="fields-group-toggle" on:click={() => showOptionalFields = !showOptionalFields}><h3 class="fields-group-title">Optional Fields ({optionalFields.length})</h3><span class="toggle-icon">{showOptionalFields ? 'v' : '>'}</span></button>{#if showOptionalFields}<div class="fields-grid">{#each optionalFields as field}<div class="form-group" class:full-width={field.type === 'text'}><label for={`field-${field.name}`}>{field.label}<span class="field-type-tag">{field.type}</span></label>{#if field.type === 'date'}<input type="date" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} />{:else if field.type === 'number'}<input type="number" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{:else}<input type="text" id={`field-${field.name}`} value={fieldValues[field.name] || ''} on:input={(e) => updateField(field.name, e.currentTarget?.value || '')} placeholder={`Enter ${field.label}`} />{/if}</div>{/each}</div>{/if}</div>{/if}
          {#if releaseEntityDef.children && releaseEntityDef.children.length > 0}<div class="children-info"><h3 class="fields-group-title">Available Child Entities</h3><p class="helper-text">Click to create Release and add a child element:</p><div class="children-tags">{#each releaseEntityDef.children as child}<button type="button" class="child-tag child-tag-clickable" on:click={() => handleCreateAndAddChild(child)} disabled={submitting}>{child}</button>{/each}</div></div>{/if}
        </section>
      {/if}

      <div class="form-actions"><button type="button" class="btn btn-secondary" on:click={handleCancel}>Cancel</button><button type="submit" class="btn btn-primary" disabled={submitting || !releaseEntityDef}>{submitting ? 'Creating Release...' : 'Create Release'}</button></div>
    </form>
  {/if}
</div>

<style>
  .dip-container { max-width: 960px; margin: 2rem auto; background: var(--arc-card, #fff); padding: 2.5rem; border-radius: 1rem; box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04)); border: 1px solid var(--arc-line, #e8edf3); }
  .dip-header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 1px solid var(--arc-line, #e8edf3); }
  .dip-header-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
  .dip-header h1 { margin: 0 0 0.5rem 0; color: var(--arc-ink, #0f172a); font-size: 1.75rem; font-weight: 700; }
  .subtitle { margin: 0; color: var(--arc-muted); font-size: 0.925rem; }
  .alert { padding: 0.875rem 1rem; border-radius: 0.6rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
  .alert-error { background: var(--arc-alert-red-bg); color: var(--arc-alert-red-ink); border: 1px solid var(--arc-alert-red-border); }
  .alert button { background: none; border: none; color: inherit; font-size: 1.25rem; cursor: pointer; padding: 0 0.25rem; box-shadow: none; }
  .alert button:hover { background: none; transform: none; box-shadow: none; }
  .loading-state { display: flex; flex-direction: column; align-items: center; padding: 3rem 2rem; gap: 1rem; }
  .loading-state .spinner { width: 2.5rem; height: 2.5rem; border: 3px solid var(--arc-line-strong); border-top-color: var(--arc-indigo, #6366f1); border-radius: 50%; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  @media (prefers-reduced-motion: reduce) { .loading-state .spinner { animation: none; } }
  .loading-state p { color: var(--arc-muted); font-size: 0.875rem; }
  .form-section { margin-bottom: 2rem; padding-bottom: 2rem; border-bottom: 1px solid var(--arc-line, #e8edf3); }
  .form-section:last-of-type { border-bottom: none; }
  .section-title { margin: 0 0 1.25rem 0; color: var(--arc-ink, #0f172a); font-size: 1.15rem; font-weight: 600; }
  .section-hint { margin: 0 0 1rem 0; color: var(--arc-muted); font-size: 0.85rem; }
  .prefill-status { margin: 0.75rem 0 0; color: var(--arc-indigo, #6366f1); font-size: 0.8rem; font-weight: 500; }
  .prefill-success { margin: 0.75rem 0 0; color: var(--arc-alert-green-ink); font-size: 0.8rem; font-weight: 500; }
  .btn-defaults { padding: 0.625rem 1.25rem; background: var(--arc-chip-soft-indigo-bg); color: var(--arc-chip-indigo-ink); font-size: 0.85rem; white-space: nowrap; flex-shrink: 0; border: 1.5px solid var(--arc-hover-border); border-radius: 0.65rem; cursor: pointer; font-weight: 600; box-shadow: none; }
  .btn-defaults:hover:not(:disabled) { background: var(--arc-chip-indigo-bg); border-color: var(--arc-indigo, #6366f1); color: var(--arc-indigo-deep, #4f46e5); box-shadow: none; }
  .btn-defaults:disabled { background: var(--arc-card-2); color: var(--arc-faint); cursor: not-allowed; }
  .standard-info { padding: 1rem; background: var(--arc-alert-indigo-bg); border: 1px solid var(--arc-alert-indigo-border); border-radius: 0.6rem; border-left: 4px solid var(--arc-indigo, #6366f1); }
  .standard-info strong { display: block; color: var(--arc-ink); margin-bottom: 0.25rem; font-size: 0.925rem; }
  .standard-ref { display: block; color: var(--arc-muted); font-size: 0.75rem; margin-bottom: 0.5rem; }
  .standard-info p { margin: 0; color: var(--arc-body); font-size: 0.85rem; line-height: 1.5; }
  .field-hint { display: block; margin-top: 0.375rem; color: var(--arc-faint); font-size: 0.75rem; }
  .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
  .form-group { margin-bottom: 1.25rem; }
  .form-group label { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; color: var(--arc-body, #334155); font-weight: 600; font-size: 0.85rem; }
  .req { color: #ef4444; font-weight: 600; }
  .field-type-tag { font-size: 0.65rem; color: var(--arc-faint); font-weight: 400; font-style: italic; margin-left: auto; }
  /* Input chrome (border, radius, focus ring) comes from the global rules in app.css */
  .form-group textarea { resize: vertical; }
  .entity-description { margin: 0 0 1rem 0; color: var(--arc-body); font-size: 0.875rem; line-height: 1.5; }
  .entity-note { padding: 0.75rem 1rem; background: var(--arc-alert-amber-bg); border: 1px solid var(--arc-alert-amber-border); border-radius: 0.375rem; margin-bottom: 1.5rem; font-size: 0.8rem; color: var(--arc-alert-amber-ink); line-height: 1.5; }
  .fields-group { margin-bottom: 1.5rem; }
  .fields-group-title { margin: 0 0 1rem 0; color: var(--arc-body); font-size: 0.925rem; font-weight: 600; }
  .fields-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 1rem; }
  .fields-grid .full-width { grid-column: 1 / -1; }
  .optional-group { border: 1px solid var(--arc-line, #e8edf3); border-radius: 0.75rem; padding: 1rem; background: var(--arc-ground, #f8fafc); }
  .fields-group-toggle { display: flex; align-items: center; justify-content: space-between; width: 100%; background: none; border: none; cursor: pointer; padding: 0; color: inherit; box-shadow: none; }
  .fields-group-toggle:hover { background: none; transform: none; box-shadow: none; }
  .fields-group-toggle .fields-group-title { margin: 0; color: var(--arc-muted); }
  .toggle-icon { font-size: 1rem; color: var(--arc-faint); }
  .children-info { margin-top: 1.5rem; padding: 1rem; background: var(--arc-ground, #f8fafc); border: 1px solid var(--arc-line, #e8edf3); border-radius: 0.75rem; }
  .children-info .fields-group-title { margin-bottom: 0.5rem; }
  .helper-text { margin: 0 0 0.75rem 0; color: var(--arc-muted); font-size: 0.8rem; }
  .children-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
  /* Amber chips kept as the subtle per-entity (release) accent */
  .child-tag { padding: 0.25rem 0.75rem; background: var(--arc-chip-amber-bg); color: var(--arc-chip-amber-ink); border-radius: 9999px; font-size: 0.75rem; font-weight: 600; box-shadow: none; }
  .child-tag-clickable { border: 1px solid var(--arc-chip-amber-hover); cursor: pointer; transition: all 0.18s ease; }
  .child-tag-clickable:hover:not(:disabled) { background: #f59e0b; color: white; border-color: #f59e0b; }
  .child-tag-clickable:disabled { opacity: 0.5; cursor: not-allowed; }
  /* Primary button styling (brand gradient) comes from the global button rules in app.css */
  .form-actions { display: flex; justify-content: space-between; gap: 1rem; margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--arc-line, #e8edf3); }
  .btn { padding: 0.75rem 2rem; font-size: 0.925rem; }
  .btn-secondary { background: var(--arc-card); border: 1.5px solid var(--arc-line-strong); color: var(--arc-ink); box-shadow: none; }
  .btn-secondary:hover { background: var(--arc-card); border-color: var(--arc-indigo, #6366f1); color: var(--arc-indigo-deep, #4f46e5); box-shadow: none; }
  @media (max-width: 768px) { .dip-container { margin: 1rem; padding: 1.5rem; } .form-row, .fields-grid { grid-template-columns: 1fr; } }
</style>
