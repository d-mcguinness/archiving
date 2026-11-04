<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_PRESERVATION, GET_ALL_PRESERVATIONS, GET_INTAKES_BY_TENANT_V2, PREFILL_INTAKE_FIELDS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { standards } from '$lib/standards';
  import EntityForm from '$lib/components/EntityForm.svelte';
  import { getScheme, getStageEntity, emptyValues, validateRequired, toFieldInputs, deriveElementIdentifier, getStageDefaults, applyDefaults, type EntityDefinition, type SchemeDefinition } from '$lib/schemes';
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
  let schemaData: SchemeDefinition | null = null;
  let preservationEntityDef: EntityDefinition | null = null;
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
      schemaData = getScheme(selectedStandard.graphql) ?? null;
      if (!schemaData) throw new Error(`No scheme definition for ${selectedStandard.label}`);
      const entity = getStageEntity(selectedStandard.graphql, 'preservation');
      if (!entity) throw new Error(`Preservation entity "${selectedStandard.preservationEntity}" not found in schema`);
      preservationEntityDef = entity;
      fieldValues = emptyValues(entity);
    } catch (e) { error = e instanceof Error ? e.message : 'Failed to load schema'; preservationEntityDef = null; }
    finally { loadingSchema = false; }
  }

  function genId() { return `Preservation-${data.archiveId}-${Math.random().toString(36).substring(2, 8)}`; }


  function applyAllDefaults() {
    if (!selectedUserId && users.length > 0) selectedUserId = users[0].id;
    if (!aipTitle) aipTitle = `${selectedStandard?.label || 'Archive'} Preservation - ${new Date().toLocaleDateString()}`;
    if (!aipDescription) aipDescription = `Preservation package created using the ${selectedStandard?.label || 'selected'} standard.`;
    if (!preservationEntityDef?.fields || !selectedStandard) return;
    const user = users.find((u: any) => u.id === selectedUserId);
    const defaults = getStageDefaults(selectedStandard.key, 'preservation', {
      id: genId(),
      today: new Date().toISOString().split('T')[0],
      userName: user?.name || 'System',
      userEmail: user?.email,
      title: aipTitle,
      description: aipDescription
    });
    fieldValues = applyDefaults(preservationEntityDef, fieldValues, defaults);
  }


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
    const missing = validateRequired(preservationEntityDef, fieldValues);
    if (missing) return missing;
    return null;
  }

  async function handleSubmit() {
    const validationError = validateForm(); if (validationError) { error = validationError; return; }
    submitting = true; error = null;
    try {
      const aipContent = JSON.stringify({ aipType: selectedStandard!.preservationLabel, standard: selectedStandard!.key, entity: preservationEntityDef!.name, fields: fieldValues }, null, 2);
      const elementIdentifier = deriveElementIdentifier(fieldValues, `Preservation-${Date.now()}`);
      const fields = toFieldInputs(preservationEntityDef!, fieldValues);
      const input: any = { userId: selectedUserId, title: aipTitle, description: aipDescription || null, content: aipContent, standard: selectedStandard!.graphql, elementIdentifier, entityName: preservationEntityDef!.name, entityType: preservationEntityDef!.type, elementTitle: aipTitle, elementDescription: aipDescription || null, createdBy: selectedUserId, fields };
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

  <div class="aip-header"><div class="aip-header-top"><div><span class="eyebrow">Admin console</span><h1>Create Preservation package</h1>{#if archive}<p class="subtitle">Archive: {archive.title} ({archive.standard})</p>{/if}</div>
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
          <EntityForm entity={preservationEntityDef} bind:values={fieldValues} bind:showOptional={showOptionalFields} />
          {#if preservationEntityDef.children && preservationEntityDef.children.length > 0}<div class="children-info"><h3 class="fields-group-title">Available Child Entities</h3><p class="helper-text">Click to create Preservation and add a child element:</p><div class="children-tags">{#each preservationEntityDef.children as child}<button type="button" class="child-tag child-tag-clickable" on:click={() => handleCreateAndAddChild(child)} disabled={submitting}>{child}</button>{/each}</div></div>{/if}
        </section>
      {/if}

      <div class="form-actions"><button type="button" class="btn btn-secondary" on:click={handleCancel}>Cancel</button><button type="submit" class="btn btn-primary" disabled={submitting || !preservationEntityDef}>{submitting ? 'Creating Preservation...' : 'Create Preservation'}</button></div>
    </form>
  {/if}
</div>

<style>
  .aip-container { max-width: 960px; margin: 2rem auto; background: var(--arc-card, #fff); padding: 2.5rem; border-radius: 1rem; box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04)); border: 1px solid var(--arc-line, #e8edf3); }
  .aip-header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 1px solid var(--arc-line, #e8edf3); }
  .aip-header-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
  .aip-header h1 { margin: 0 0 0.5rem 0; color: var(--arc-ink, #0f172a); font-size: 1.75rem; font-weight: 700; }
  .subtitle { margin: 0; color: var(--arc-muted); font-size: 0.925rem; }
  .alert { padding: 0.875rem 1rem; border-radius: 0.6rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
  .alert-error { background: var(--arc-alert-red-bg); color: var(--arc-alert-red-ink); border: 1px solid var(--arc-alert-red-border); }
  .alert button { background: none; border: none; color: inherit; font-size: 1.25rem; cursor: pointer; padding: 0 0.25rem; box-shadow: none; }
  .alert button:hover { background: none; transform: none; box-shadow: none; }
  .loading-state { display: flex; flex-direction: column; align-items: center; padding: 3rem 2rem; gap: 1rem; }
  /* Spinner chrome + reduced-motion come from the global .spinner; only size/speed differ. */
  .loading-state .spinner { width: 2.5rem; height: 2.5rem; animation-duration: 1s; }
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
  /* Input chrome (border, radius, focus ring) comes from the global rules in app.css */
  .form-group textarea { resize: vertical; }
  .fields-group-title { margin: 0 0 1rem 0; color: var(--arc-body); font-size: 0.925rem; font-weight: 600; }
  .children-info { margin-top: 1.5rem; padding: 1rem; background: var(--arc-ground, #f8fafc); border: 1px solid var(--arc-line, #e8edf3); border-radius: 0.75rem; }
  .children-info .fields-group-title { margin-bottom: 0.5rem; }
  .helper-text { margin: 0 0 0.75rem 0; color: var(--arc-muted); font-size: 0.8rem; }
  .children-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
  .child-tag { padding: 0.25rem 0.75rem; background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); border-radius: 9999px; font-size: 0.75rem; font-weight: 600; box-shadow: none; }
  .child-tag-clickable { border: 1px solid var(--arc-hover-border); cursor: pointer; transition: all 0.18s ease; }
  .child-tag-clickable:hover:not(:disabled) { background: var(--arc-indigo, #6366f1); color: white; border-color: var(--arc-indigo, #6366f1); }
  .child-tag-clickable:disabled { opacity: 0.5; cursor: not-allowed; }
  /* .form-actions, .btn-primary and .btn-secondary come from the global kit in app.css;
     only the taller footer and this page's wider buttons are layered on top. */
  .form-actions { margin-top: 2rem; border-top-color: var(--arc-line, #e8edf3); }
  .btn { padding: 0.75rem 2rem; font-size: 0.925rem; }
  @media (max-width: 768px) { .aip-container { margin: 1rem; padding: 1.5rem; } .form-row { grid-template-columns: 1fr; } }
</style>