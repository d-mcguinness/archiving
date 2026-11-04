<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_INTAKE, GET_ALL_INTAKES_V2, GET_ARCHIVES_BY_TENANT, GET_TENANT, PREFILL_INTAKE_FIELDS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { standards } from '$lib/standards';
  import EntityForm from '$lib/components/EntityForm.svelte';
  import { getScheme, getStageEntity, emptyValues, validateRequired, toFieldInputs, deriveElementIdentifier, getStageDefaults, applyDefaults, type EntityDefinition, type SchemeDefinition } from '$lib/schemes';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';
  import FileDropZone from '$lib/components/FileDropZone.svelte';

  interface PageData { tenantId: string; }
  export let data: PageData;

  let tenant: any = null;
  let archives: any[] = [];
  let users: any[] = [];
  let selectedArchiveId = '';
  let selectedArchive: any = null;
  let selectedStandard: typeof standards[0] | null = null;
  let schemaData: SchemeDefinition | null = null;
  let intakeEntityDef: EntityDefinition | null = null;
  let fieldValues: Record<string, string> = {};
  let selectedUserId = '';
  let sipTitle = '';
  let sipDescription = '';
  let loading = false;
  let loadingSchema = false;
  let submitting = false;
  let error: string | null = null;
  let showOptionalFields = false;
  let pendingChildEntity: string | null = null;
  let droppedFiles: File[] = [];
  let prefilling = false;
  let prefilledFromFiles = false;

  // Access control
  let currentRole = '';
  let hasAccess = false;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    const tenantId = authState.tenantId?.toString() ?? null;

    // Guard: only ADMIN and TENANT (matching tenant) can create Intakes
    if (currentRole === 'ADMIN') {
      hasAccess = true;
    } else if (currentRole === 'TENANT' && tenantId === data.tenantId) {
      hasAccess = true;
    } else {
      hasAccess = false;
      loading = false;
      toasts.error('You do not have permission to create Intakes');
      goto(`/tenants/${data.tenantId}/intakes`);
      return;
    }

    loading = true;
    try {
      const [tenantResult, archivesResult, usersResult] = await Promise.all([
        client.query({ query: GET_TENANT, variables: { id: data.tenantId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ARCHIVES_BY_TENANT, variables: { tenantId: data.tenantId }, fetchPolicy: 'network-only' }),
        client.query({ query: GET_ALL_USERS })
      ]);
      tenant = tenantResult?.data?.getTenant;
      archives = archivesResult?.data?.getArchivesByTenant || [];
      users = usersResult?.data?.getAllUsers || [];
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
      intakeEntityDef = null;
      schemaData = null;
      fieldValues = {};
      sipTitle = '';
      sipDescription = '';
      selectedUserId = '';
      prefilledFromFiles = false;
      return;
    }

    selectedArchive = archives.find((a: any) => a.id === selectedArchiveId);
    if (!selectedArchive) return;

    sipTitle = selectedArchive.title ? `Intake - ${selectedArchive.title}` : '';
    sipDescription = selectedArchive.description || '';

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
      schemaData = getScheme(selectedStandard.graphql) ?? null;
      if (!schemaData) throw new Error(`No scheme definition for ${selectedStandard.label}`);
      const entity = getStageEntity(selectedStandard.graphql, 'intake');
      if (!entity) throw new Error(`Intake entity "${selectedStandard.intakeEntity}" not found in schema`);
      intakeEntityDef = entity;
      fieldValues = emptyValues(entity);
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load schema';
      intakeEntityDef = null;
    } finally {
      loadingSchema = false;
    }
  }

  function genId() {
    return `Intake-${selectedArchiveId || Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
  }


  function applyAllDefaults() {
    if (!selectedUserId && users.length > 0) selectedUserId = users[0].id;
    if (!sipTitle) sipTitle = `${selectedStandard?.label || 'Archive'} Intake - ${new Date().toLocaleDateString()}`;
    if (!sipDescription) sipDescription = `Intake package created using the ${selectedStandard?.label || 'selected'} standard.`;

    if (!intakeEntityDef?.fields || !selectedStandard) return;
    const user = users.find((u: any) => u.id === selectedUserId);
    const defaults = getStageDefaults(selectedStandard.key, 'intake', {
      id: genId(),
      today: new Date().toISOString().split('T')[0],
      userName: user?.name || 'System',
      userEmail: user?.email,
      title: sipTitle,
      description: sipDescription
    });
    fieldValues = applyDefaults(intakeEntityDef, fieldValues, defaults);
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
        if (intakeEntityDef?.fields?.some((f: any) => f.name === name)) {
          fieldValues[name] = value;
        }
      }
      fieldValues = fieldValues;

      if (files.length === 1 && metadata.filename) {
        const baseName = metadata.filename.replace(/\.[^.]+$/, '');
        if (!sipTitle || sipTitle.startsWith('Intake -')) sipTitle = `Intake - ${baseName}`;
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
    if (!sipTitle.trim()) return 'Please enter a title';
    const missing = validateRequired(intakeEntityDef, fieldValues);
    if (missing) return missing;
    return null;
  }

  async function handleSubmit() {
    const validationError = validateForm();
    if (validationError) { error = validationError; return; }

    submitting = true;
    error = null;

    try {
      const sipContent = JSON.stringify({
        sipType: selectedStandard!.intakeLabel,
        standard: selectedStandard!.key,
        entity: intakeEntityDef!.name,
        fields: fieldValues
      }, null, 2);

      const elementIdentifier = deriveElementIdentifier(fieldValues, `Intake-${Date.now()}`);

      const fields = toFieldInputs(intakeEntityDef!, fieldValues);

      const result = await client.mutate({
        mutation: CREATE_INTAKE,
        variables: {
          input: {
            userId: selectedUserId, archiveId: selectedArchiveId, title: sipTitle, description: sipDescription || null,
            content: sipContent, standard: selectedStandard!.graphql,
            elementIdentifier, entityName: intakeEntityDef!.name, entityType: intakeEntityDef!.type,
            elementTitle: sipTitle, elementDescription: sipDescription || null,
            createdBy: selectedUserId, fields
          }
        },
        refetchQueries: [{ query: GET_ALL_INTAKES_V2 }],
        awaitRefetchQueries: true
      });

      const newIntakeId = result?.data?.createIntakeV2?.id;
      toasts.add(`Intake "${sipTitle}" created successfully using ${selectedStandard!.label}`, 'success');

      if (pendingChildEntity && newIntakeId) {
        goto(`/intake/edit/${newIntakeId}?addChild=${encodeURIComponent(pendingChildEntity)}`);
      } else {
        goto(`/tenants/${data.tenantId}/intakes`);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to create Intake';
      toasts.add(`Failed to create Intake: ${error}`, 'error');
    } finally {
      submitting = false;
    }
  }

  function handleCreateAndAddChild(childEntityName: string) {
    pendingChildEntity = childEntityName;
    handleSubmit();
  }

  function handleCancel() {
    goto(`/tenants/${data.tenantId}/intakes`);
  }
</script>

<svelte:head><title>Create Intake - {tenant?.displayName || tenant?.name || 'Tenant'} - Arcana</title></svelte:head>

{#if !hasAccess && !loading}
  <div class="access-denied">
    <div class="access-denied-icon">🚫</div>
    <h1>Access Denied</h1>
    <p>You don't have permission to create Intakes.</p>
    <p class="redirect-message">Redirecting...</p>
  </div>
{:else}
  <div class="sip-container">
    <Breadcrumb
      context={{ tenantId: data.tenantId, tenantName: tenant?.displayName || tenant?.name }}
      items={[
        { label: 'Intakes', href: `/tenants/${data.tenantId}/intakes` },
        { label: 'Create' }
      ]}
    />

    <div class="sip-header">
      <div class="sip-header-top">
        <div>
          <span class="eyebrow">Intake</span>
          <h1>Create Intake package</h1>
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

        <!-- Section 1: Select Archive -->
        <section class="form-section">
          <h2 class="section-title">1. Select Archive</h2>
          <div class="form-group">
            <label for="archiveSelect">Archive for Submission <span class="req">*</span></label>
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
                <label for="sipTitle">Intake Title <span class="req">*</span></label>
                <input type="text" id="sipTitle" bind:value={sipTitle} required placeholder="Enter Intake title" />
              </div>
            </div>
            <div class="form-group">
              <label for="sipDescription">Description</label>
              <textarea id="sipDescription" bind:value={sipDescription} rows="3" placeholder="Brief description of this submission package..."></textarea>
            </div>
          </section>

          <!-- File Drop Zone -->
          {#if selectedStandard}
            <section class="form-section">
              <h2 class="section-title">Upload Files</h2>
              <p class="section-hint">Drop files to auto-populate Intake metadata fields from file properties.</p>
              <FileDropZone on:filesProcessed={handleFilesProcessed} on:filesCleared={handleFilesCleared} disabled={prefilling} />
              {#if prefilling}
                <p class="prefill-status">Auto-populating fields...</p>
              {/if}
              {#if prefilledFromFiles}
                <p class="prefill-success">Fields populated from file metadata. Review and edit below.</p>
              {/if}
            </section>
          {/if}

          <!-- Section 3: Standard-Specific Fields -->
          {#if loadingSchema}
            <section class="form-section">
              <div class="loading-state">
                <div class="spinner"></div>
                <p>Loading {selectedStandard?.label} schema...</p>
              </div>
            </section>
          {:else if intakeEntityDef}
            <section class="form-section">
              <h2 class="section-title">3. {selectedStandard?.intakeLabel} Fields</h2>

              <EntityForm entity={intakeEntityDef} bind:values={fieldValues} bind:showOptional={showOptionalFields} />

              {#if intakeEntityDef.children && intakeEntityDef.children.length > 0}
                <div class="children-info">
                  <h3 class="fields-group-title">Available Child Entities</h3>
                  <p class="helper-text">Click to create Intake and add a child element:</p>
                  <div class="children-tags">
                    {#each intakeEntityDef.children as child}
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
          <button type="submit" class="btn btn-primary" disabled={submitting || !selectedArchiveId || !intakeEntityDef}>
            {submitting ? 'Creating Intake...' : 'Create Intake'}
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
  .sip-container { max-width: 960px; margin: 2rem auto; background: var(--arc-card); padding: 2.5rem; border-radius: 1rem; box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04)); border: 1px solid var(--arc-line); }
  .sip-header { margin-bottom: 2rem; padding-bottom: 1.5rem; border-bottom: 1px solid var(--arc-line); }
  .sip-header-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; }
  .sip-header h1 { margin: 0 0 0.5rem 0; color: var(--arc-ink); font-size: 1.75rem; font-weight: 700; }
  .subtitle { margin: 0; color: var(--arc-muted); font-size: 0.925rem; }
  .alert { padding: 0.875rem 1rem; border-radius: 0.6rem; margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center; }
  .alert-error { background: var(--arc-alert-red-bg); color: var(--arc-alert-red-ink); border: 1px solid var(--arc-alert-red-border); }
  .alert button { background: none; border: none; box-shadow: none; color: inherit; font-size: 1.25rem; cursor: pointer; padding: 0 0.25rem; }
  .alert button:hover { background: none; transform: none; box-shadow: none; }
  .loading-state { display: flex; flex-direction: column; align-items: center; padding: 3rem 2rem; gap: 1rem; }
  /* Spinner chrome is the global .spinner; only the size, accent and slower
     sweep are local. */
  .loading-state .spinner { width: 2.5rem; height: 2.5rem; border-top-color: var(--arc-link); animation: spin 1s linear infinite; }
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
  /* Inputs, selects & textareas are styled by the global app.css rules */
  .form-group textarea { resize: vertical; }
  .fields-group-title { margin: 0 0 1rem 0; color: var(--arc-body); font-size: 0.925rem; font-weight: 600; }
  .children-info { margin-top: 1.5rem; padding: 1rem; background: var(--arc-card-2); border: 1px solid var(--arc-line); border-radius: 0.6rem; }
  .children-info .fields-group-title { margin-bottom: 0.5rem; }
  .helper-text { margin: 0 0 0.75rem 0; color: var(--arc-muted); font-size: 0.8rem; }
  .children-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
  .child-tag { padding: 0.25rem 0.75rem; background: var(--arc-chip-indigo-bg); color: var(--arc-chip-indigo-ink); border-radius: 9999px; font-size: 0.75rem; font-weight: 500; box-shadow: none; }
  .child-tag-clickable { border: 1px solid var(--arc-hover-border); cursor: pointer; transition: all 0.15s; }
  .child-tag-clickable:hover:not(:disabled) { background: #6366f1; color: white; border-color: var(--arc-indigo); transform: none; box-shadow: none; }
  .child-tag-clickable:disabled { opacity: 0.5; cursor: not-allowed; }
  /* .form-actions, .btn-primary and .btn-secondary come from the global kit
     (app.css); only this form's wider, smaller button size stays local. */
  .btn { padding: 0.75rem 2rem; font-size: 0.875rem; }
  @media (max-width: 768px) { .sip-container { margin: 1rem; padding: 1.5rem; } .form-row { grid-template-columns: 1fr; } }
</style>
