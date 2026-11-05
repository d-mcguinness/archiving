<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, CREATE_INTAKE, GET_ALL_INTAKES_V2, GET_ALL_ARCHIVES, GET_ARCHIVES_BY_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { standards, graphqlToKey } from '$lib/standards';
  import EntityForm from '$lib/components/EntityForm.svelte';
  import { getScheme, getStageEntity, emptyValues, validateRequired, toFieldInputs, deriveElementIdentifier, getStageDefaults, applyDefaults, type EntityDefinition, type SchemeDefinition } from '$lib/schemes';

  function getIntakesPath() {
    const { role } = get(auth);
    if (role === 'ADMIN' || role === 'TENANT') return '/intake';
    return '/';
  }

  let selectedStandardKey = '';
  let selectedStandard: typeof standards[0] | null = null;
  let schemaData: SchemeDefinition | null = null;
  let intakeEntityDef: EntityDefinition | null = null;
  let fieldValues: Record<string, string> = {};
  let users: any[] = [];
  let archives: any[] = [];
  let selectedUserId = '';
  let selectedArchiveId = '';
  let sipTitle = '';
  let sipDescription = '';
  let sourceArchiveId = '';
  let loading = false;
  let loadingSchema = false;
  let submitting = false;
  let error: string | null = null;
  let showOptionalFields = false;
  let prefilled = false;
  let pendingChildEntity: string | null = null;
  let filterStandard: string | null = null;

  // Computed field groups

  // Filter archives by the standard from the URL query param
  $: filteredArchives = filterStandard
    ? archives.filter((a: any) => {
        const match = standards.find(s => s.key === filterStandard);
        return match ? a.standard === match.graphql : true;
      })
    : archives;

  async function onArchiveSelect() {
    if (!selectedArchiveId) {
      // Reset everything when clearing archive selection
      sourceArchiveId = '';
      selectedStandardKey = '';
      selectedStandard = null;
      intakeEntityDef = null;
      schemaData = null;
      fieldValues = {};
      sipTitle = '';
      sipDescription = '';
      selectedUserId = '';
      prefilled = false;
      return;
    }
    const archive = archives.find((a: any) => a.id === selectedArchiveId);
    if (!archive) return;

    sourceArchiveId = archive.id;
    sipTitle = archive.title ? `Intake - ${archive.title}` : '';
    sipDescription = archive.description || '';

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
    const qArchiveId = params.get('archiveId');

    if (qUserId) selectedUserId = qUserId;
    if (qTitle) sipTitle = qTitle;
    if (qDescription) sipDescription = qDescription;

    // Auto-select archive from query param
    if (qArchiveId) {
      const match = archives.find((a: any) => a.id === qArchiveId);
      if (match) {
        selectedArchiveId = qArchiveId;
        await onArchiveSelect();
      }
    }

    // Pre-select standard from query param (e.g. from /ingest page)
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
    intakeEntityDef = null;
    fieldValues = {};
    error = null;

    if (!selectedStandard) return;

    loadingSchema = true;
    try {
      schemaData = getScheme(selectedStandard.graphql) ?? null;
      if (!schemaData) throw new Error(`No scheme definition for ${selectedStandard.label}`);
      const entity = getStageEntity(selectedStandard.graphql, 'intake');
      if (!entity) throw new Error(`No intake entity defined for ${selectedStandard.label}`);
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
    return `Intake-${sourceArchiveId || Date.now()}-${Math.random().toString(36).substring(2, 8)}`;
  }


  function applyAllDefaults() {
    // Fill basic info if empty
    if (!selectedUserId && users.length > 0) {
      selectedUserId = users[0].id;
    }
    if (!sipTitle) {
      const standardLabel = selectedStandard?.label || 'Archive';
      sipTitle = `${standardLabel} Intake - ${new Date().toLocaleDateString()}`;
    }
    if (!sipDescription) {
      sipDescription = `Intake package created using the ${selectedStandard?.label || 'selected'} standard.`;
    }

    // Fill standard-specific fields
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
    if (validationError) {
      error = validationError;
      return;
    }

    submitting = true;
    error = null;

    try {
      // Build content from field values
      const sipContent = JSON.stringify({
        sipType: selectedStandard!.intakeLabel,
        standard: selectedStandard!.key,
        entity: intakeEntityDef!.name,
        fields: fieldValues
      }, null, 2);

      // Derive element identifier from standard-specific ID fields
      const elementIdentifier = deriveElementIdentifier(fieldValues, `Intake-${Date.now()}`);

      // Build fields array for the root element
      const fields = toFieldInputs(intakeEntityDef!, fieldValues);

      // Single mutation: creates Intake + root Element + Fields
      const result = await client.mutate({
        mutation: CREATE_INTAKE,
        variables: {
          input: {
            userId: selectedUserId,
            title: sipTitle,
            description: sipDescription || null,
            content: sipContent,
            standard: selectedStandard!.graphql,
            elementIdentifier,
            entityName: intakeEntityDef!.name,
            entityType: intakeEntityDef!.type,
            elementTitle: sipTitle,
            elementDescription: sipDescription || null,
            createdBy: selectedUserId,
            fields
          }
        },
        refetchQueries: [{ query: GET_ALL_INTAKES_V2 }],
        awaitRefetchQueries: true
      });

      const newIntakeId = result?.data?.createIntakeV2?.id;
      toasts.add(`Intake "${sipTitle}" created successfully using ${selectedStandard!.label}`, 'success');

      if (pendingChildEntity && newIntakeId) {
        goto(`/intake/edit/${newIntakeId}?addChild=${encodeURIComponent(pendingChildEntity)}`);
      } else if (newIntakeId) {
        goto(`/intake/edit/${newIntakeId}`);
      } else {
        goto(getIntakesPath());
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
    goto(getIntakesPath());
  }
</script>

<div class="sip-container">
  <div class="sip-header">
    <div class="sip-header-top">
      <div>
        <span class="eyebrow">Intake</span>
        <h1>Create Intake package</h1>
        <p class="subtitle">Build a Intake using any supported archiving standard</p>
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
          <label for="archiveSelect">Archive for Submission <span class="req">*</span></label>
          <select id="archiveSelect" bind:value={selectedArchiveId} on:change={onArchiveSelect} required>
            <option value="">-- Select an archive --</option>
            {#each filteredArchives as archive}
              <option value={archive.id}>
                #{archive.id} - {archive.title} ({archive.standard})
              </option>
            {/each}
          </select>
          {#if filterStandard}
            <span class="field-hint">Showing archives using the {filterStandard} standard. <button type="button" class="link-btn" on:click={() => filterStandard = null}>Show all</button></span>
          {:else}
            <span class="field-hint">The standard, title, and description will be derived from the selected archive.</span>
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
            <h2 class="section-title">
              3. {selectedStandard?.intakeLabel} Fields
            </h2>

            <EntityForm entity={intakeEntityDef} bind:values={fieldValues} bind:showOptional={showOptionalFields} />

            <!-- Children preview -->
            {#if intakeEntityDef.children && intakeEntityDef.children.length > 0}
              <div class="children-info">
                <h3 class="fields-group-title">Available Child Entities</h3>
                <p class="helper-text">Click to create Intake and add a child element:</p>
                <div class="children-tags">
                  {#each intakeEntityDef.children as child}
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
          disabled={submitting || !selectedArchiveId || !intakeEntityDef}
        >
          {submitting ? 'Creating Intake...' : 'Create Intake'}
        </button>
      </div>
    </form>
  {/if}
</div>

<style>
  .sip-container {
    max-width: 960px;
    margin: 2rem auto;
    background: var(--arc-card, #fff);
    padding: 2.5rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .sip-header {
    margin-bottom: 2rem;
    padding-bottom: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .sip-header-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
  }

  .sip-header h1 {
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
    background: var(--arc-alert-red-bg, #fef2f2);
    color: var(--arc-alert-red-ink, #dc2626);
    border: 1px solid var(--arc-alert-red-border, #fecaca);
  }

  .alert-info {
    background: var(--arc-alert-indigo-bg, #eef2ff);
    color: var(--arc-alert-indigo-ink, #4338ca);
    border: 1px solid var(--arc-alert-indigo-border, #c7d2fe);
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
    border-bottom: 1px solid var(--arc-line, #f1f5f9);
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
    background: var(--arc-chip-soft-indigo-bg, #eef2ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
    border: 1px solid var(--arc-hover-border, #c7d2fe);
    font-size: 0.85rem;
    white-space: nowrap;
    flex-shrink: 0;
    box-shadow: none;
  }

  .btn-defaults:hover:not(:disabled) {
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    transform: translateY(-2px);
    box-shadow: none;
  }

  .btn-defaults:disabled {
    background: var(--arc-disabled-bg, #c7cdd8);
    cursor: not-allowed;
    transform: none;
  }

  .standard-info {
    padding: 1rem;
    background: var(--arc-alert-indigo-bg, #eef2ff);
    border: 1px solid var(--arc-alert-indigo-border, #c7d2fe);
    border-radius: 0.6rem;
    border-left: 4px solid var(--arc-indigo, #6366f1);
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

  /* Input chrome (width, border, radius, focus ring) comes from the global
     rules in app.css; only this page's tighter scale is layered on top. */
  .form-group input,
  .form-group select,
  .form-group textarea {
    padding: 0.625rem 0.75rem;
    font-size: 0.875rem;
  }

  .form-group textarea {
    resize: vertical;
  }

  .fields-group-title {
    margin: 0 0 1rem 0;
    color: var(--arc-body);
    font-size: 0.925rem;
    font-weight: 600;
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
    background: var(--arc-chip-pink-bg);
    color: var(--arc-chip-pink-ink);
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
    box-shadow: none;
  }

  .child-tag-clickable {
    border: 1px solid var(--arc-chip-pink-hover);
    cursor: pointer;
    transition: all 0.15s ease;
  }

  .child-tag-clickable:hover:not(:disabled) {
    background: var(--arc-chip-pink-hover);
    color: var(--arc-chip-pink-ink);
    border-color: #ec4899;
    transform: none;
    box-shadow: none;
  }

  .child-tag-clickable:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .empty-state {
    text-align: center;
    color: var(--arc-faint);
    padding: 2rem;
    font-style: italic;
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
    .sip-container {
      margin: 1rem;
      padding: 1.5rem;
    }

    .form-row {
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
