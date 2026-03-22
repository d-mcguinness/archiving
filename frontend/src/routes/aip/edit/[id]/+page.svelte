<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_AIP, GET_ALL_USERS, GET_ALL_AIPS } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { standardFileMap } from '$lib/standards';

  function getAipsPath() {
    const { role } = get(auth);
    if (role === 'ADMIN' || role === 'TENANT') return '/aip';
    return '/';
  }

  const UPDATE_AIP_STATUS = gql`
    mutation UpdateAipStatus($aipId: ID!, $status: AipStatus!) {
      updateAipStatus(aipId: $aipId, status: $status) {
        id
        status
        updatedAt
      }
    }
  `;

  const GENERATE_AIP = gql`
    mutation GenerateAip($aipId: ID!) {
      generateAip(aipId: $aipId)
    }
  `;

  const DELETE_AIP = gql`
    mutation DeleteAip($id: ID!) {
      deleteAip(id: $id)
    }
  `;

  const ADD_CHILD_ELEMENT = gql`
    mutation AddChildElement($parentElementId: ID!, $input: AddChildElementInput!) {
      addChildElement(parentElementId: $parentElementId, input: $input) {
        id
        elementIdentifier
        entityName
        entityType
        title
        description
        status
        createdAt
        createdBy
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

  const DELETE_ELEMENT = gql`
    mutation DeleteElement($id: ID!) {
      deleteElement(id: $id)
    }
  `;

  let aip: any = null;
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let updatingStatus = false;
  let generating = false;
  let deleting = false;

  // Schema data
  let schemaData: any = null;
  let schemaEntities: any[] = [];

  // Add child modal state
  let showAddChildModal = false;
  let addingChild = false;
  let addChildParentId: string | null = null;
  let addChildParentEntityName = '';
  let availableChildEntities: any[] = [];
  let selectedChildEntity: any = null;
  let childFieldValues: Record<string, string> = {};
  let childTitle = '';
  let childDescription = '';

  const aipStatuses = ['DRAFT', 'BUILDING', 'VALIDATED', 'STORED', 'REJECTED'];

  $: aipId = $page.params.id;

  function buildElementContent(element: any): any {
    if (!element) return null;
    const obj: any = {
      entityName: element.entityName,
      entityType: element.entityType,
      elementIdentifier: element.elementIdentifier,
      title: element.title,
    };
    if (element.description) obj.description = element.description;
    if (element.fields?.length > 0) {
      obj.fields = {};
      element.fields.forEach((f: any) => { if (f.value) obj.fields[f.label || f.name] = f.value; });
    }
    if (element.children?.length > 0) {
      obj.children = element.children.map(buildElementContent);
    }
    return obj;
  }

  $: computedContent = aip ? JSON.stringify(
    aip.rootElement
      ? { standard: aip.standard, status: aip.status, ...(aip.sourceSipId ? { sourceSipId: aip.sourceSipId } : {}), rootElement: buildElementContent(aip.rootElement) }
      : JSON.parse(aip.content || '{}'),
    null, 2
  ) : '';

  onMount(async () => {
    await Promise.all([loadAip(), loadUsers()]);

    // Auto-open add child modal if ?addChild= query param is present
    const addChildParam = $page.url.searchParams.get('addChild');
    if (addChildParam && aip?.rootElement) {
      const childDefs = getChildEntityDefs(aip.rootElement.entityName);
      const matchedEntity = childDefs.find((e: any) => e.name === addChildParam);
      if (matchedEntity) {
        openAddChildModal(aip.rootElement.id, aip.rootElement.entityName);
        selectChildEntity(matchedEntity);
      }
    }
  });

  async function loadAip() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_AIP,
        variables: { id: aipId },
        fetchPolicy: 'network-only'
      });
      aip = result?.data?.getAip;
      if (!aip) {
        error = 'AIP not found';
      } else {
        await loadSchema(aip.standard);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load AIP';
    } finally {
      loading = false;
    }
  }

  async function loadSchema(standard: string) {
    const file = standardFileMap[standard];
    if (!file) return;
    try {
      const response = await fetch(`/schemeDefintions/${file}`);
      if (!response.ok) return;
      schemaData = await response.json();
      schemaEntities = schemaData?.entities || [];
    } catch (e) {
      console.error('Failed to load schema:', e);
    }
  }

  async function loadUsers() {
    try {
      const result = await client.query({ query: GET_ALL_USERS });
      users = result?.data?.getAllUsers || [];
    } catch (e) {
      console.error('Failed to load users:', e);
    }
  }

  function getUserName(userId: string) {
    const user = users.find((u: any) => u.id === userId);
    return user ? user.name : `User #${userId}`;
  }

  function getEntityDef(entityName: string) {
    return schemaEntities.find((e: any) => e.name === entityName);
  }

  function getChildEntityDefs(parentEntityName: string): any[] {
    const parentDef = getEntityDef(parentEntityName);
    if (!parentDef?.children?.length) return [];
    return parentDef.children
      .map((name: string) => getEntityDef(name))
      .filter(Boolean);
  }

  // Add child handlers
  function openAddChildModal(parentId: string, parentEntityName: string) {
    addChildParentId = parentId;
    addChildParentEntityName = parentEntityName;
    availableChildEntities = getChildEntityDefs(parentEntityName);
    selectedChildEntity = null;
    childFieldValues = {};
    childTitle = '';
    childDescription = '';
    showAddChildModal = true;
  }

  function selectChildEntity(entity: any) {
    selectedChildEntity = entity;
    childFieldValues = {};
    childTitle = '';
    childDescription = '';
    if (entity.fields) {
      entity.fields.forEach((f: any) => { childFieldValues[f.name] = ''; });
    }
  }

  function closeAddChildModal() {
    showAddChildModal = false;
    addChildParentId = null;
    selectedChildEntity = null;
    childFieldValues = {};
  }

  function fillChildDefaults() {
    if (!selectedChildEntity) return;
    const today = new Date().toISOString().split('T')[0];
    const id = `${selectedChildEntity.name}-${Date.now().toString(36)}`;
    const user = users.find((u: any) => u.id === aip?.ownerId);
    const userName = user?.name || 'System';

    if (!childTitle) childTitle = `${selectedChildEntity.name} - ${new Date().toLocaleDateString()}`;
    if (!childDescription) childDescription = `${selectedChildEntity.name} element`;

    if (selectedChildEntity.fields) {
      for (const field of selectedChildEntity.fields) {
        if (childFieldValues[field.name]) continue;
        if (field.type === 'date') { childFieldValues[field.name] = today; continue; }
        const lname = field.name.toLowerCase();
        if (lname.includes('id') || lname.includes('identifier')) { childFieldValues[field.name] = id; }
        else if (lname.includes('title') || lname.includes('label') || lname.includes('name')) { childFieldValues[field.name] = childTitle; }
        else if (lname.includes('description')) { childFieldValues[field.name] = childDescription; }
        else if (lname.includes('creator') || lname.includes('createdby') || lname.includes('author')) { childFieldValues[field.name] = userName; }
        else if (lname.includes('version')) { childFieldValues[field.name] = '1.0'; }
        else if (lname.includes('status')) { childFieldValues[field.name] = 'Active'; }
        else if (lname.includes('type')) { childFieldValues[field.name] = selectedChildEntity.type || 'default'; }
        else if (lname.includes('lang')) { childFieldValues[field.name] = 'eng'; }
        else if (field.required) { childFieldValues[field.name] = field.label || field.name; }
      }
      childFieldValues = childFieldValues;
    }
  }

  async function handleAddChild() {
    if (!addChildParentId || !selectedChildEntity) return;

    // Validate required fields
    const requiredFields = selectedChildEntity.fields?.filter((f: any) => f.required) || [];
    for (const f of requiredFields) {
      if (!childFieldValues[f.name]?.trim()) {
        toasts.error(`Required field "${f.label}" is empty`);
        return;
      }
    }
    if (!childTitle.trim()) {
      toasts.error('Title is required');
      return;
    }

    addingChild = true;
    try {
      const identifier = childFieldValues['systemID'] || childFieldValues['packageID'] ||
        childFieldValues['objectIdentifierValue'] || childFieldValues['bagName'] ||
        childFieldValues['metsID'] || childFieldValues['eadID'] ||
        childFieldValues['descriptionID'] || childFieldValues['modsID'] ||
        childFieldValues['resourceIdentifier'] ||
        `${selectedChildEntity.name}-${Date.now()}`;

      const fields = selectedChildEntity.fields?.map((f: any) => ({
        name: f.name,
        label: f.label,
        type: f.type,
        value: childFieldValues[f.name] || ''
      })) || [];

      await client.mutate({
        mutation: ADD_CHILD_ELEMENT,
        variables: {
          parentElementId: addChildParentId,
          input: {
            elementIdentifier: identifier,
            entityName: selectedChildEntity.name,
            entityType: selectedChildEntity.type,
            title: childTitle,
            description: childDescription || null,
            createdBy: aip.ownerId?.toString() || 'system',
            fields
          }
        }
      });

      toasts.success(`${selectedChildEntity.name} added successfully`);
      closeAddChildModal();
      await loadAip();
    } catch (e) {
      toasts.error(`Failed to add child: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      addingChild = false;
    }
  }

  async function handleDeleteElement(elementId: string, elementTitle: string) {
    if (!confirm(`Delete element "${elementTitle}"?`)) return;
    try {
      await client.mutate({
        mutation: DELETE_ELEMENT,
        variables: { id: elementId }
      });
      toasts.success('Element deleted');
      await loadAip();
    } catch (e) {
      toasts.error(`Failed to delete: ${e instanceof Error ? e.message : 'Unknown error'}`);
    }
  }

  async function handleStatusChange(newStatus: string) {
    updatingStatus = true;
    try {
      const result = await client.mutate({
        mutation: UPDATE_AIP_STATUS,
        variables: { aipId: aip.id, status: newStatus }
      });
      aip = { ...aip, status: result.data.updateAipStatus.status, updatedAt: result.data.updateAipStatus.updatedAt };
      toasts.success(`Status updated to ${newStatus}`);
    } catch (e) {
      toasts.error(`Failed to update status: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      updatingStatus = false;
    }
  }

  async function handleGenerate() {
    generating = true;
    try {
      await client.mutate({
        mutation: GENERATE_AIP,
        variables: { aipId: aip.id }
      });
      toasts.success('AIP generated successfully');
      await loadAip();
    } catch (e) {
      toasts.error(`Failed to generate AIP: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      generating = false;
    }
  }

  async function handleDelete() {
    if (!confirm(`Are you sure you want to delete AIP "${aip.title}"?`)) return;
    deleting = true;
    try {
      await client.mutate({
        mutation: DELETE_AIP,
        variables: { id: aip.id },
        refetchQueries: [{ query: GET_ALL_AIPS }]
      });
      toasts.success('AIP deleted');
      goto(getAipsPath());
    } catch (e) {
      toasts.error(`Failed to delete AIP: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      deleting = false;
    }
  }

  function getStatusClass(status: string) {
    return status?.toLowerCase() || '';
  }

  function formatDate(dateStr: string) {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString();
  }
</script>

<svelte:head>
  <title>{aip ? `Edit AIP - ${aip.title}` : 'Edit AIP'}</title>
</svelte:head>

<div class="aip-edit-page">
  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading AIP...</p>
    </div>
  {:else if error}
    <div class="error-state">
      <p>{error}</p>
      <button class="btn btn-secondary" on:click={() => goto(getAipsPath())}>Back to AIPs</button>
    </div>
  {:else if aip}
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <button class="btn-back" on:click={() => goto(getAipsPath())}>← Back</button>
        <div>
          <h1>{aip.title}</h1>
          <div class="header-meta">
            <span class="badge standard-badge">{aip.standard}</span>
            <span class="badge {getStatusClass(aip.status)}">{aip.status}</span>
            <span class="meta-text">ID: #{aip.id}</span>
            {#if aip.sourceSipId}
              <span class="meta-text">Source SIP: #{aip.sourceSipId}</span>
            {/if}
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn btn-generate" on:click={handleGenerate} disabled={generating}>
          {generating ? 'Generating...' : 'Generate AIP'}
        </button>
        <button class="btn btn-danger" on:click={handleDelete} disabled={deleting}>
          {deleting ? 'Deleting...' : 'Delete'}
        </button>
      </div>
    </div>

    <div class="content-grid">
      <!-- Left: AIP Details -->
      <div class="panel">
        <h2 class="panel-title">AIP Details</h2>
        <div class="detail-row">
          <span class="detail-label">Title</span>
          <span class="detail-value">{aip.title}</span>
        </div>
        {#if aip.description}
          <div class="detail-row">
            <span class="detail-label">Description</span>
            <span class="detail-value">{aip.description}</span>
          </div>
        {/if}
        <div class="detail-row">
          <span class="detail-label">Owner</span>
          <span class="detail-value">{getUserName(aip.ownerId)}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Standard</span>
          <span class="detail-value">{aip.standard}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Created</span>
          <span class="detail-value">{formatDate(aip.createdAt)}</span>
        </div>
        {#if aip.updatedAt}
          <div class="detail-row">
            <span class="detail-label">Updated</span>
            <span class="detail-value">{formatDate(aip.updatedAt)}</span>
          </div>
        {/if}
        <div class="detail-row status-row">
          <span class="detail-label">Status</span>
          <div class="status-controls">
            <select value={aip.status} on:change={(e) => handleStatusChange(e.currentTarget.value)} disabled={updatingStatus}>
              {#each aipStatuses as status}
                <option value={status}>{status}</option>
              {/each}
            </select>
            {#if updatingStatus}
              <span class="updating-indicator">Updating...</span>
            {/if}
          </div>
        </div>
      </div>

      <!-- Right: Root Element & Fields -->
      <div class="panel">
        <h2 class="panel-title">Root Element</h2>
        {#if aip.rootElement}
          <div class="detail-row">
            <span class="detail-label">Entity</span>
            <span class="detail-value">
              {aip.rootElement.entityName}
              <span class="entity-type">({aip.rootElement.entityType})</span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Identifier</span>
            <span class="detail-value mono">{aip.rootElement.elementIdentifier}</span>
          </div>
          {#if aip.rootElement.title}
            <div class="detail-row">
              <span class="detail-label">Title</span>
              <span class="detail-value">{aip.rootElement.title}</span>
            </div>
          {/if}
          {#if aip.rootElement.fields && aip.rootElement.fields.length > 0}
            <h3 class="fields-title">Fields ({aip.rootElement.fields.length})</h3>
            <div class="fields-table">
              <table>
                <thead><tr><th>Name</th><th>Value</th><th>Type</th></tr></thead>
                <tbody>
                  {#each aip.rootElement.fields as field}
                    <tr>
                      <td class="field-name">{field.label || field.name}</td>
                      <td class="field-value">{field.value || '-'}</td>
                      <td class="field-type">{field.type}</td>
                    </tr>
                  {/each}
                </tbody>
              </table>
            </div>
          {/if}
        {:else}
          <p class="no-data">No root element defined</p>
        {/if}
      </div>
    </div>

    <!-- Element Tree -->
    {#if aip.rootElement}
      <div class="panel">
        <div class="panel-header-row">
          <h2 class="panel-title" style="border-bottom:none;padding-bottom:0;margin-bottom:0;">Child Elements</h2>
          {#if getChildEntityDefs(aip.rootElement.entityName).length > 0}
            <button class="btn btn-add-child" on:click={() => openAddChildModal(aip.rootElement.id, aip.rootElement.entityName)}>
              + Add Child
            </button>
          {/if}
        </div>

        {#if aip.rootElement.children && aip.rootElement.children.length > 0}
          <div class="element-tree">
            {#each aip.rootElement.children as child (child.id)}
              <div class="element-card">
                <div class="element-card-header">
                  <div class="element-card-info">
                    <span class="element-entity-badge">{child.entityName}</span>
                    <strong class="element-title">{child.title}</strong>
                    <span class="element-id">({child.elementIdentifier})</span>
                  </div>
                  <div class="element-card-actions">
                    {#if getChildEntityDefs(child.entityName).length > 0}
                      <button class="btn-sm btn-add" on:click={() => openAddChildModal(child.id, child.entityName)}>+ Add</button>
                    {/if}
                    <button class="btn-sm btn-delete" on:click={() => handleDeleteElement(child.id, child.title)}>Delete</button>
                  </div>
                </div>

                {#if child.description}
                  <p class="element-description">{child.description}</p>
                {/if}

                {#if child.fields && child.fields.length > 0}
                  <div class="element-fields">
                    {#each child.fields as field}
                      {#if field.value}
                        <div class="element-field">
                          <span class="ef-label">{field.label || field.name}:</span>
                          <span class="ef-value">{field.value}</span>
                        </div>
                      {/if}
                    {/each}
                  </div>
                {/if}

                <!-- Nested children (level 2) -->
                {#if child.children && child.children.length > 0}
                  <div class="nested-children">
                    {#each child.children as grandchild (grandchild.id)}
                      <div class="element-card nested">
                        <div class="element-card-header">
                          <div class="element-card-info">
                            <span class="element-entity-badge">{grandchild.entityName}</span>
                            <strong class="element-title">{grandchild.title}</strong>
                            <span class="element-id">({grandchild.elementIdentifier})</span>
                          </div>
                          <div class="element-card-actions">
                            <button class="btn-sm btn-delete" on:click={() => handleDeleteElement(grandchild.id, grandchild.title)}>Delete</button>
                          </div>
                        </div>
                        {#if grandchild.fields && grandchild.fields.length > 0}
                          <div class="element-fields">
                            {#each grandchild.fields as field}
                              {#if field.value}
                                <div class="element-field">
                                  <span class="ef-label">{field.label || field.name}:</span>
                                  <span class="ef-value">{field.value}</span>
                                </div>
                              {/if}
                            {/each}
                          </div>
                        {/if}
                      </div>
                    {/each}
                  </div>
                {/if}
              </div>
            {/each}
          </div>
        {:else}
          <p class="no-data" style="margin-top:1rem;">No child elements yet. Click "Add Child" to add one.</p>
        {/if}
      </div>
    {/if}

    <!-- Assigned Users -->
    {#if aip.assignedUsers && aip.assignedUsers.length > 0}
      <div class="panel">
        <h2 class="panel-title">Assigned Users</h2>
        <div class="user-chips">
          {#each aip.assignedUsers as user}
            <span class="user-chip">{user.name} ({user.email})</span>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Content (live JSON) -->
    {#if computedContent}
      <div class="panel">
        <h2 class="panel-title">Content</h2>
        <pre class="content-pre">{computedContent}</pre>
      </div>
    {/if}
  {/if}
</div>

<!-- Add Child Element Modal -->
{#if showAddChildModal}
  <div class="modal-overlay" on:click={closeAddChildModal} role="dialog" aria-modal="true">
    <div class="modal-content modal-lg" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>Add Child Element to {addChildParentEntityName}</h3>
        <button class="modal-close" on:click={closeAddChildModal} aria-label="Close">&times;</button>
      </div>
      <div class="modal-body">
        {#if !selectedChildEntity}
          <p class="modal-instruction">Select an entity type to add:</p>
          <div class="entity-options">
            {#each availableChildEntities as entity}
              <button class="entity-option" on:click={() => selectChildEntity(entity)}>
                <strong>{entity.name}</strong>
                <span class="entity-option-type">{entity.type}</span>
                {#if entity.description}
                  <p class="entity-option-desc">{entity.description}</p>
                {/if}
              </button>
            {/each}
          </div>
        {:else}
          <div class="selected-entity-header">
            <span class="element-entity-badge">{selectedChildEntity.name}</span>
            <button class="btn-sm btn-back-entity" on:click={() => { selectedChildEntity = null; childFieldValues = {}; }}>← Change type</button>
            <button class="btn-sm btn-fill-defaults" on:click={fillChildDefaults}>Fill Defaults</button>
          </div>

          {#if selectedChildEntity.description}
            <p class="entity-desc">{selectedChildEntity.description}</p>
          {/if}
          {#if selectedChildEntity.note}
            <div class="entity-note">{selectedChildEntity.note}</div>
          {/if}

          <div class="form-group">
            <label for="child-title">Title <span class="req">*</span></label>
            <input type="text" id="child-title" bind:value={childTitle} placeholder="Enter title" />
          </div>

          <div class="form-group">
            <label for="child-desc">Description</label>
            <textarea id="child-desc" bind:value={childDescription} rows="2" placeholder="Optional description"></textarea>
          </div>

          {#if selectedChildEntity.fields && selectedChildEntity.fields.length > 0}
            <h4 class="modal-section-title">Fields</h4>
            <div class="modal-fields-grid">
              {#each selectedChildEntity.fields as field}
                <div class="form-group" class:full-width={field.type === 'text'}>
                  <label for={`child-field-${field.name}`}>
                    {field.label}
                    {#if field.required}<span class="req">*</span>{/if}
                    <span class="field-type-tag">{field.type}</span>
                  </label>
                  {#if field.type === 'date'}
                    <input type="date" id={`child-field-${field.name}`}
                      bind:value={childFieldValues[field.name]} />
                  {:else if field.type === 'number'}
                    <input type="number" id={`child-field-${field.name}`}
                      bind:value={childFieldValues[field.name]}
                      placeholder={`Enter ${field.label}`} />
                  {:else}
                    <input type="text" id={`child-field-${field.name}`}
                      bind:value={childFieldValues[field.name]}
                      placeholder={`Enter ${field.label}`} />
                  {/if}
                </div>
              {/each}
            </div>
          {/if}
        {/if}
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" on:click={closeAddChildModal} disabled={addingChild}>Cancel</button>
        {#if selectedChildEntity}
          <button class="btn btn-primary" on:click={handleAddChild} disabled={addingChild || !childTitle.trim()}>
            {addingChild ? 'Adding...' : `Add ${selectedChildEntity.name}`}
          </button>
        {/if}
      </div>
    </div>
  </div>
{/if}

<style>
  .aip-edit-page {
    max-width: 1200px;
    margin: 0 auto;
    padding: 2rem;
  }

  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    gap: 1rem;
  }

  .spinner {
    border: 4px solid #f3f4f6;
    border-top: 4px solid #6366f1;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin { to { transform: rotate(360deg); } }

  .error-state {
    text-align: center;
    padding: 4rem 2rem;
    color: #991b1b;
  }

  /* Header */
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 2rem;
    gap: 1rem;
  }

  .header-left {
    display: flex;
    align-items: flex-start;
    gap: 1rem;
  }

  .btn-back {
    background: none;
    border: 1px solid #e2e8f0;
    padding: 0.5rem 0.75rem;
    border-radius: 0.375rem;
    color: #64748b;
    cursor: pointer;
    white-space: nowrap;
    font-size: 0.875rem;
  }

  .btn-back:hover { background: #f8fafc; color: #1e293b; }

  .page-header h1 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1.5rem;
  }

  .header-meta {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .meta-text {
    color: #94a3b8;
    font-size: 0.8rem;
    font-family: monospace;
  }

  .header-actions {
    display: flex;
    gap: 0.5rem;
    flex-shrink: 0;
  }

  /* Buttons */
  .btn {
    padding: 0.625rem 1.25rem;
    border: none;
    border-radius: 0.375rem;
    font-weight: 600;
    cursor: pointer;
    font-size: 0.85rem;
    transition: all 0.2s;
  }

  .btn:disabled { opacity: 0.5; cursor: not-allowed; }
  .btn-secondary { background: #e2e8f0; color: #475569; }
  .btn-secondary:hover:not(:disabled) { background: #cbd5e1; }
  .btn-primary { background: #6366f1; color: white; }
  .btn-primary:hover:not(:disabled) { background: #4f46e5; }
  .btn-generate { background: #10b981; color: white; }
  .btn-generate:hover:not(:disabled) { background: #059669; }
  .btn-danger { background: #ef4444; color: white; }
  .btn-danger:hover:not(:disabled) { background: #dc2626; }

  .btn-add-child {
    padding: 0.5rem 1rem;
    background: #6366f1;
    color: white;
    border: none;
    border-radius: 0.375rem;
    font-weight: 600;
    font-size: 0.8rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-add-child:hover { background: #4f46e5; }

  .btn-sm {
    padding: 0.25rem 0.5rem;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.7rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-add { background: #e0e7ff; color: #3730a3; }
  .btn-add:hover { background: #c7d2fe; }
  .btn-delete { background: #fee2e2; color: #991b1b; }
  .btn-delete:hover { background: #fecaca; }

  .btn-back-entity {
    background: #f1f5f9;
    color: #64748b;
    padding: 0.25rem 0.5rem;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    cursor: pointer;
  }

  .btn-back-entity:hover { background: #e2e8f0; }

  .btn-fill-defaults {
    background: #dcfce7;
    color: #166534;
    margin-left: auto;
  }

  .btn-fill-defaults:hover { background: #bbf7d0; }

  /* Badges */
  .badge {
    display: inline-block;
    padding: 0.2rem 0.6rem;
    border-radius: 0.375rem;
    font-size: 0.7rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .standard-badge { background: #e0e7ff; color: #3730a3; }
  .draft { background: #fef3c7; color: #92400e; }
  .building { background: #dbeafe; color: #1e40af; }
  .validated { background: #dcfce7; color: #166534; }
  .stored { background: #ede9fe; color: #5b21b6; }
  .rejected { background: #fee2e2; color: #991b1b; }

  /* Grid */
  .content-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
    margin-bottom: 1.5rem;
  }

  /* Panels */
  .panel {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    margin-bottom: 1.5rem;
  }

  .content-grid .panel { margin-bottom: 0; }

  .panel-title {
    margin: 0 0 1.25rem 0;
    font-size: 1.05rem;
    color: #1e293b;
    font-weight: 600;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid #f1f5f9;
  }

  .panel-header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px solid #f1f5f9;
  }

  /* Detail rows */
  .detail-row {
    display: flex;
    padding: 0.625rem 0;
    border-bottom: 1px solid #f8fafc;
  }

  .detail-row:last-child { border-bottom: none; }

  .detail-label {
    width: 120px;
    flex-shrink: 0;
    color: #64748b;
    font-size: 0.85rem;
    font-weight: 500;
  }

  .detail-value {
    color: #1e293b;
    font-size: 0.85rem;
    word-break: break-word;
  }

  .entity-type { color: #94a3b8; font-size: 0.8rem; }
  .mono { font-family: monospace; font-size: 0.8rem; color: #64748b; }

  .status-row { align-items: center; }

  .status-controls { display: flex; align-items: center; gap: 0.5rem; }

  .status-controls select {
    padding: 0.375rem 0.625rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    font-size: 0.85rem;
    cursor: pointer;
  }

  .status-controls select:focus {
    outline: none;
    border-color: #6366f1;
    box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
  }

  .updating-indicator { color: #94a3b8; font-size: 0.75rem; font-style: italic; }

  /* Fields table */
  .fields-title {
    margin: 1.25rem 0 0.75rem 0;
    font-size: 0.9rem;
    color: #334155;
    font-weight: 600;
  }

  .fields-table {
    overflow-x: auto;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
  }

  .fields-table table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.825rem;
  }

  .fields-table thead { background: #f8fafc; }

  .fields-table th {
    padding: 0.5rem 0.75rem;
    text-align: left;
    font-weight: 600;
    color: #64748b;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    border-bottom: 1px solid #e2e8f0;
  }

  .fields-table td {
    padding: 0.5rem 0.75rem;
    border-bottom: 1px solid #f1f5f9;
    color: #1e293b;
  }

  .fields-table tbody tr:last-child td { border-bottom: none; }
  .fields-table tbody tr:hover { background: #eef2ff; }

  .field-name { font-weight: 500; white-space: nowrap; }
  .field-value { word-break: break-word; max-width: 300px; }
  .field-type { color: #94a3b8; font-size: 0.75rem; font-style: italic; white-space: nowrap; }

  .no-data { color: #94a3b8; font-style: italic; font-size: 0.85rem; }

  /* Element Tree */
  .element-tree {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    margin-top: 1rem;
  }

  .element-card {
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    padding: 1rem;
    background: #fafbfc;
  }

  .element-card.nested {
    background: #f8fafc;
    border-color: #f1f5f9;
    margin-top: 0.5rem;
  }

  .element-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 0.5rem;
  }

  .element-card-info {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .element-entity-badge {
    display: inline-block;
    padding: 0.15rem 0.5rem;
    background: #e0e7ff;
    color: #3730a3;
    border-radius: 0.25rem;
    font-size: 0.7rem;
    font-weight: 600;
  }

  .element-title {
    font-size: 0.875rem;
    color: #1e293b;
  }

  .element-id {
    font-size: 0.75rem;
    color: #94a3b8;
    font-family: monospace;
  }

  .element-card-actions {
    display: flex;
    gap: 0.375rem;
    flex-shrink: 0;
  }

  .element-description {
    margin: 0.5rem 0 0;
    color: #64748b;
    font-size: 0.8rem;
  }

  .element-fields {
    display: flex;
    flex-wrap: wrap;
    gap: 0.375rem 1rem;
    margin-top: 0.625rem;
    padding-top: 0.625rem;
    border-top: 1px solid #e2e8f0;
  }

  .element-field {
    font-size: 0.75rem;
  }

  .ef-label { color: #64748b; }
  .ef-value { color: #1e293b; font-weight: 500; }

  .nested-children {
    margin-top: 0.75rem;
    padding-left: 1rem;
    border-left: 2px solid #e2e8f0;
  }

  /* Users */
  .user-chips { display: flex; flex-wrap: wrap; gap: 0.5rem; }

  .user-chip {
    padding: 0.375rem 0.75rem;
    background: #eef2ff;
    color: #3730a3;
    border-radius: 1rem;
    font-size: 0.8rem;
    font-weight: 500;
    border: 1px solid #c7d2fe;
  }

  /* Content */
  .content-pre {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    padding: 1rem;
    font-size: 0.8rem;
    overflow-x: auto;
    max-height: 300px;
    color: #334155;
    white-space: pre-wrap;
    word-break: break-word;
  }

  /* Modal */
  .modal-overlay {
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  .modal-content {
    background: white;
    border-radius: 0.75rem;
    max-width: 500px;
    width: 90%;
    max-height: 85vh;
    display: flex;
    flex-direction: column;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  }

  .modal-lg { max-width: 700px; }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.25rem 1.5rem;
    border-bottom: 1px solid #e2e8f0;
    flex-shrink: 0;
  }

  .modal-header h3 { margin: 0; color: #1e293b; font-size: 1.05rem; }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: #64748b;
    cursor: pointer;
  }

  .modal-body {
    padding: 1.5rem;
    overflow-y: auto;
    flex: 1;
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    padding: 1.25rem 1.5rem;
    border-top: 1px solid #e2e8f0;
    flex-shrink: 0;
  }

  .modal-instruction {
    margin: 0 0 1rem 0;
    color: #64748b;
    font-size: 0.875rem;
  }

  .entity-options {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .entity-option {
    display: block;
    width: 100%;
    text-align: left;
    padding: 1rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    background: #fafbfc;
    cursor: pointer;
    transition: all 0.15s;
  }

  .entity-option:hover {
    border-color: #6366f1;
    background: #eef2ff;
  }

  .entity-option strong {
    display: block;
    color: #1e293b;
    font-size: 0.9rem;
    margin-bottom: 0.125rem;
  }

  .entity-option-type {
    font-size: 0.7rem;
    color: #94a3b8;
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  .entity-option-desc {
    margin: 0.375rem 0 0;
    color: #64748b;
    font-size: 0.8rem;
    line-height: 1.4;
  }

  .selected-entity-header {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 1rem;
  }

  .entity-desc {
    margin: 0 0 0.75rem;
    color: #475569;
    font-size: 0.85rem;
    line-height: 1.5;
  }

  .entity-note {
    padding: 0.625rem 0.875rem;
    background: #fffbeb;
    border: 1px solid #fde68a;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    font-size: 0.8rem;
    color: #92400e;
    line-height: 1.5;
  }

  .modal-section-title {
    margin: 1.25rem 0 0.75rem;
    font-size: 0.9rem;
    color: #334155;
    font-weight: 600;
  }

  .modal-fields-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 1rem;
  }

  .modal-fields-grid .full-width {
    grid-column: 1 / -1;
  }

  /* Form */
  .form-group {
    margin-bottom: 1rem;
  }

  .form-group label {
    display: flex;
    align-items: center;
    gap: 0.375rem;
    margin-bottom: 0.375rem;
    color: #1e293b;
    font-weight: 500;
    font-size: 0.825rem;
  }

  .req { color: #ef4444; font-weight: 600; }

  .field-type-tag {
    font-size: 0.65rem;
    color: #94a3b8;
    font-weight: 400;
    font-style: italic;
    margin-left: auto;
  }

  .form-group input,
  .form-group textarea {
    width: 100%;
    padding: 0.5rem 0.625rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    font-size: 0.825rem;
    transition: border-color 0.2s;
    font-family: inherit;
  }

  .form-group input:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #6366f1;
    box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.1);
  }

  .form-group textarea { resize: vertical; }

  @media (max-width: 768px) {
    .aip-edit-page { padding: 1rem; }
    .content-grid { grid-template-columns: 1fr; }
    .page-header { flex-direction: column; }
    .header-actions { width: 100%; }
    .detail-row { flex-direction: column; gap: 0.25rem; }
    .detail-label { width: auto; }
    .modal-fields-grid { grid-template-columns: 1fr; }
  }
</style>
