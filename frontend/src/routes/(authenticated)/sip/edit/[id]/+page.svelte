<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_SIP, GET_ALL_USERS, GET_ALL_SIPS_V2 } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import { standardFileMap, graphqlToKey } from '$lib/standards';
  import ElementTree from '$lib/components/ElementTree.svelte';

  function getSipsPath() {
    const { role } = get(auth);
    if (role === 'ADMIN' || role === 'TENANT') return '/sip';
    return '/';
  }

  const UPDATE_SIP_STATUS = gql`
    mutation UpdateSipStatusV2($sipId: ID!, $status: SipStatus!) {
      updateSipStatusV2(sipId: $sipId, status: $status) {
        id
        status
        updatedAt
      }
    }
  `;

  const GENERATE_SIP = gql`
    mutation GenerateSip($sipId: ID!) {
      generateSip(sipId: $sipId)
    }
  `;

  const DELETE_SIP = gql`
    mutation DeleteSipV2($id: ID!) {
      deleteSipV2(id: $id)
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

  let sip: any = null;
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let updatingStatus = false;
  let generating = false;
  let deleting = false;

  // Documents state
  let sipDocuments: any[] = [];
  let loadingDocuments = false;
  let showDocumentModal = false;
  let documentModalMode: 'upload' | 'link' = 'upload';
  let uploadFile: File | null = null;
  let uploadTitle = '';
  let uploadDescription = '';
  let uploading = false;
  let unassociatedDocuments: any[] = [];
  let selectedDocumentId: string | null = null;
  let linking = false;

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

  const sipStatuses = ['DRAFT', 'SUBMITTED', 'VALIDATED', 'ACCEPTED', 'REJECTED'];

  $: sipId = $page.params.id || $page.params.sipId;

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

  $: computedContent = sip ? JSON.stringify(
    sip.rootElement
      ? { standard: sip.standard, status: sip.status, rootElement: buildElementContent(sip.rootElement) }
      : JSON.parse(sip.content || '{}'),
    null, 2
  ) : '';

  onMount(async () => {
    await Promise.all([loadSip(), loadUsers(), loadDocuments()]);

    // Auto-open add child modal if ?addChild= query param is present
    const addChildParam = $page.url.searchParams.get('addChild');
    if (addChildParam && sip?.rootElement) {
      const childDefs = getChildEntityDefs(sip.rootElement.entityName);
      const matchedEntity = childDefs.find((e: any) => e.name === addChildParam);
      if (matchedEntity) {
        openAddChildModal(sip.rootElement.id, sip.rootElement.entityName);
        selectChildEntity(matchedEntity);
      }
    }
  });

  async function loadSip() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_SIP,
        variables: { id: sipId },
        fetchPolicy: 'network-only'
      });
      sip = result?.data?.getSip;
      if (!sip) {
        error = 'SIP not found';
      } else {
        await loadSchema(sip.standard);
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load SIP';
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
    const user = users.find((u: any) => u.id === sip?.ownerId);
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
            createdBy: sip.ownerId?.toString() || 'system',
            fields
          }
        }
      });

      toasts.success(`${selectedChildEntity.name} added successfully`);
      closeAddChildModal();
      await loadSip();
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
      await loadSip();
    } catch (e) {
      toasts.error(`Failed to delete: ${e instanceof Error ? e.message : 'Unknown error'}`);
    }
  }

  async function handleStatusChange(newStatus: string) {
    updatingStatus = true;
    try {
      const result = await client.mutate({
        mutation: UPDATE_SIP_STATUS,
        variables: { sipId: sip.id, status: newStatus }
      });
      sip = { ...sip, status: result.data.updateSipStatusV2.status, updatedAt: result.data.updateSipStatusV2.updatedAt };
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
        mutation: GENERATE_SIP,
        variables: { sipId: sip.id }
      });
      toasts.success('SIP generated successfully');
      await loadSip();
    } catch (e) {
      toasts.error(`Failed to generate SIP: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      generating = false;
    }
  }

  async function handleDelete() {
    if (!confirm(`Are you sure you want to delete SIP "${sip.title}"?`)) return;
    deleting = true;
    try {
      await client.mutate({
        mutation: DELETE_SIP,
        variables: { id: sip.id },
        refetchQueries: [{ query: GET_ALL_SIPS_V2 }]
      });
      toasts.success('SIP deleted');
      goto(getSipsPath());
    } catch (e) {
      toasts.error(`Failed to delete SIP: ${e instanceof Error ? e.message : 'Unknown error'}`);
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

  function formatFileSize(bytes: number): string {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) { size /= 1024; i++; }
    return `${size.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
  }

  async function loadDocuments() {
    if (!sipId) return;
    loadingDocuments = true;
    try {
      const response = await fetch(`http://localhost:2020/api/documents?sipId=${sipId}`);
      const result = await response.json();
      if (result.success) {
        sipDocuments = result.documents || [];
      }
    } catch (e) {
      console.error('Failed to load documents:', e);
    } finally {
      loadingDocuments = false;
    }
  }

  async function loadUnassociatedDocuments() {
    try {
      const authState = get(auth);
      const params = new URLSearchParams();
      if (authState.role === 'ADMIN') {
        params.set('role', 'ADMIN');
      } else if (authState.tenantId) {
        params.set('role', 'TENANT');
        params.set('tenantId', authState.tenantId.toString());
      } else if (authState.userId) {
        params.set('userId', authState.userId.toString());
      }
      const response = await fetch(`http://localhost:2020/api/documents?${params.toString()}`);
      const result = await response.json();
      if (result.success) {
        unassociatedDocuments = (result.documents || []).filter((d: any) => !d.sipId && !d.archiveId);
      }
    } catch (e) {
      console.error('Failed to load unassociated documents:', e);
    }
  }

  function openDocumentModal(mode: 'upload' | 'link') {
    documentModalMode = mode;
    uploadFile = null;
    uploadTitle = '';
    uploadDescription = '';
    selectedDocumentId = null;
    if (mode === 'link') {
      loadUnassociatedDocuments();
    }
    showDocumentModal = true;
  }

  function closeDocumentModal() {
    showDocumentModal = false;
    uploadFile = null;
    uploadTitle = '';
    uploadDescription = '';
    selectedDocumentId = null;
  }

  function handleFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      uploadFile = input.files[0];
      if (!uploadTitle) uploadTitle = uploadFile.name;
    }
  }

  async function handleUploadDocument() {
    if (!uploadFile || !sip) return;
    uploading = true;
    try {
      const authState = get(auth);
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('userId', authState.userId?.toString() || sip.ownerId);
      if (sip.tenantId) formData.append('tenantId', sip.tenantId);
      if (uploadTitle) formData.append('title', uploadTitle);
      if (uploadDescription) formData.append('description', uploadDescription);

      const uploadResponse = await fetch('http://localhost:2020/api/documents/upload', {
        method: 'POST',
        body: formData
      });
      const uploadResult = await uploadResponse.json();

      if (uploadResult.success && uploadResult.document?.id) {
        await fetch(
          `http://localhost:2020/api/documents/${uploadResult.document.id}/associate-sip?sipId=${sipId}`,
          { method: 'POST' }
        );
        toasts.success('Document uploaded and associated with SIP');
        closeDocumentModal();
        await loadDocuments();
      } else {
        toasts.error(uploadResult.error || 'Failed to upload document');
      }
    } catch (e) {
      toasts.error(`Upload failed: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      uploading = false;
    }
  }

  async function handleLinkDocument() {
    if (!selectedDocumentId) return;
    linking = true;
    try {
      const response = await fetch(
        `http://localhost:2020/api/documents/${selectedDocumentId}/associate-sip?sipId=${sipId}`,
        { method: 'POST' }
      );
      const result = await response.json();
      if (result.success) {
        toasts.success('Document linked to SIP');
        closeDocumentModal();
        await loadDocuments();
      } else {
        toasts.error(result.error || 'Failed to link document');
      }
    } catch (e) {
      toasts.error(`Failed to link document: ${e instanceof Error ? e.message : 'Unknown error'}`);
    } finally {
      linking = false;
    }
  }

  async function handleRemoveDocument(docId: string) {
    if (!confirm('Remove this document from the SIP? (The document will not be deleted)')) return;
    try {
      const response = await fetch(
        `http://localhost:2020/api/documents/${docId}/disassociate-sip`,
        { method: 'POST' }
      );
      const result = await response.json();
      if (result.success) {
        toasts.success('Document removed from SIP');
        await loadDocuments();
      } else {
        toasts.error(result.error || 'Failed to remove document');
      }
    } catch (e) {
      toasts.error(`Failed to remove document: ${e instanceof Error ? e.message : 'Unknown error'}`);
    }
  }

  async function handleDownloadDocument(docId: string, fileName: string) {
    try {
      const response = await fetch(`http://localhost:2020/api/documents/${docId}/file`);
      if (!response.ok) throw new Error('Download failed');
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      toasts.error(`Download failed: ${e instanceof Error ? e.message : 'Unknown error'}`);
    }
  }
</script>

<svelte:head>
  <title>{sip ? `Edit SIP - ${sip.title}` : 'Edit SIP'}</title>
</svelte:head>

<div class="sip-edit-page">
  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading SIP...</p>
    </div>
  {:else if error}
    <div class="error-state">
      <p>{error}</p>
      <button class="btn btn-secondary" on:click={() => goto(getSipsPath())}>Back to SIPs</button>
    </div>
  {:else if sip}
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <button class="btn-back" on:click={() => goto(getSipsPath())}>← Back</button>
        <div>
          <h1>{sip.title}</h1>
          <div class="header-meta">
            <span class="badge standard-badge">{sip.standard}</span>
            <span class="badge {getStatusClass(sip.status)}">{sip.status}</span>
            <span class="meta-text">ID: #{sip.id}</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <div class="btn-group">
          <button class="btn btn-generate" on:click={handleGenerate} disabled={generating}>
            {generating ? 'Generating...' : 'Generate SIP'}
          </button>
          <a href="/sip/create?standard={encodeURIComponent(graphqlToKey[sip.standard] || sip.standard)}" class="btn btn-generate btn-group-right" title="Create a new {graphqlToKey[sip.standard] || sip.standard} SIP">
            +
          </a>
        </div>
        <button class="btn btn-danger" on:click={handleDelete} disabled={deleting}>
          {deleting ? 'Deleting...' : 'Delete'}
        </button>
      </div>
    </div>

    <div class="content-grid">
      <!-- Left: SIP Details -->
      <div class="panel">
        <h2 class="panel-title">SIP Details</h2>
        <div class="detail-row">
          <span class="detail-label">Title</span>
          <span class="detail-value">{sip.title}</span>
        </div>
        {#if sip.description}
          <div class="detail-row">
            <span class="detail-label">Description</span>
            <span class="detail-value">{sip.description}</span>
          </div>
        {/if}
        <div class="detail-row">
          <span class="detail-label">Owner</span>
          <span class="detail-value">{getUserName(sip.ownerId)}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Standard</span>
          <span class="detail-value">{sip.standard}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Created</span>
          <span class="detail-value">{formatDate(sip.createdAt)}</span>
        </div>
        {#if sip.updatedAt}
          <div class="detail-row">
            <span class="detail-label">Updated</span>
            <span class="detail-value">{formatDate(sip.updatedAt)}</span>
          </div>
        {/if}
        <div class="detail-row status-row">
          <span class="detail-label">Status</span>
          <div class="status-controls">
            <select value={sip.status} on:change={(e) => handleStatusChange(e.currentTarget.value)} disabled={updatingStatus}>
              {#each sipStatuses as status}
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
        {#if sip.rootElement}
          <div class="detail-row">
            <span class="detail-label">Entity</span>
            <span class="detail-value">
              {sip.rootElement.entityName}
              <span class="entity-type">({sip.rootElement.entityType})</span>
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Identifier</span>
            <span class="detail-value mono">{sip.rootElement.elementIdentifier}</span>
          </div>
          {#if sip.rootElement.title}
            <div class="detail-row">
              <span class="detail-label">Title</span>
              <span class="detail-value">{sip.rootElement.title}</span>
            </div>
          {/if}
          {#if sip.rootElement.fields && sip.rootElement.fields.length > 0}
            <h3 class="fields-title">Fields ({sip.rootElement.fields.length})</h3>
            <div class="fields-table">
              <table>
                <thead><tr><th>Name</th><th>Value</th><th>Type</th></tr></thead>
                <tbody>
                  {#each sip.rootElement.fields as field}
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
    {#if sip.rootElement}
      <div class="panel">
        <div class="panel-header-row">
          <h2 class="panel-title" style="border-bottom:none;padding-bottom:0;margin-bottom:0;">Child Elements</h2>
          {#if getChildEntityDefs(sip.rootElement.entityName).length > 0}
            <button class="btn btn-add-child" on:click={() => openAddChildModal(sip.rootElement.id, sip.rootElement.entityName)}>
              + Add Child
            </button>
          {/if}
        </div>

        {#if sip.rootElement.children && sip.rootElement.children.length > 0}
          <div class="element-tree">
            <ElementTree
              elements={sip.rootElement.children}
              {getChildEntityDefs}
              on:addChild={(e) => openAddChildModal(e.detail.parentId, e.detail.parentEntityName)}
              on:deleteElement={(e) => handleDeleteElement(e.detail.elementId, e.detail.title)}
            />
          </div>
        {:else}
          <p class="no-data" style="margin-top:1rem;">No child elements yet. Click "Add Child" to add one.</p>
        {/if}
      </div>
    {/if}

    <!-- Assigned Users -->
    {#if sip.assignedUsers && sip.assignedUsers.length > 0}
      <div class="panel">
        <h2 class="panel-title">Assigned Users</h2>
        <div class="user-chips">
          {#each sip.assignedUsers as user}
            <span class="user-chip">{user.name} ({user.email})</span>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Documents -->
    <div class="panel">
      <div class="panel-header-row">
        <h2 class="panel-title" style="border-bottom:none;padding-bottom:0;margin-bottom:0;">Documents</h2>
        <div class="doc-actions">
          <button class="btn btn-add-child" on:click={() => openDocumentModal('upload')}>Upload New</button>
          <button class="btn-sm btn-add" on:click={() => openDocumentModal('link')}>Link Existing</button>
        </div>
      </div>

      {#if loadingDocuments}
        <p class="no-data">Loading documents...</p>
      {:else if sipDocuments.length > 0}
        <div class="documents-list">
          {#each sipDocuments as doc (doc.id)}
            <div class="document-card">
              <div class="document-info">
                <strong class="document-title">{doc.title}</strong>
                <div class="document-meta">
                  <span>{doc.fileName}</span>
                  <span class="doc-separator">|</span>
                  <span>{formatFileSize(doc.fileSize)}</span>
                  <span class="doc-separator">|</span>
                  <span>{doc.contentType}</span>
                  <span class="doc-separator">|</span>
                  <span class="badge {doc.status?.toLowerCase()}">{doc.status}</span>
                </div>
              </div>
              <div class="document-actions">
                <button class="btn-sm btn-add" on:click={() => handleDownloadDocument(doc.id, doc.fileName)}>Download</button>
                <button class="btn-sm btn-delete" on:click={() => handleRemoveDocument(doc.id)}>Remove</button>
              </div>
            </div>
          {/each}
        </div>
      {:else}
        <p class="no-data" style="margin-top:1rem;">No documents associated with this SIP.</p>
      {/if}
    </div>

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

<!-- Document Modal -->
{#if showDocumentModal}
  <div class="modal-overlay" on:click={closeDocumentModal} role="dialog" aria-modal="true">
    <div class="modal-content" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>{documentModalMode === 'upload' ? 'Upload New Document' : 'Link Existing Document'}</h3>
        <button class="modal-close" on:click={closeDocumentModal} aria-label="Close">&times;</button>
      </div>
      <div class="modal-body">
        {#if documentModalMode === 'upload'}
          <div class="form-group">
            <label for="doc-file">File <span class="req">*</span></label>
            <input type="file" id="doc-file" on:change={handleFileSelect} />
          </div>
          {#if uploadFile}
            <p class="file-info-text">Selected: {uploadFile.name} ({formatFileSize(uploadFile.size)})</p>
          {/if}
          <div class="form-group">
            <label for="doc-title">Title</label>
            <input type="text" id="doc-title" bind:value={uploadTitle} placeholder="Document title" />
          </div>
          <div class="form-group">
            <label for="doc-desc">Description</label>
            <textarea id="doc-desc" bind:value={uploadDescription} rows="2" placeholder="Optional description"></textarea>
          </div>
        {:else}
          <div class="form-group">
            <label for="doc-select">Select a document</label>
            <select id="doc-select" bind:value={selectedDocumentId}>
              <option value={null}>-- Choose a document --</option>
              {#each unassociatedDocuments as doc}
                <option value={doc.id}>{doc.title} ({doc.fileName}, {formatFileSize(doc.fileSize)})</option>
              {/each}
            </select>
            {#if unassociatedDocuments.length === 0}
              <p class="no-data" style="margin-top:0.5rem;">No unassociated documents available.</p>
            {/if}
          </div>
        {/if}
      </div>
      <div class="modal-footer">
        <button class="btn btn-secondary" on:click={closeDocumentModal}>Cancel</button>
        {#if documentModalMode === 'upload'}
          <button class="btn btn-primary" on:click={handleUploadDocument} disabled={uploading || !uploadFile}>
            {uploading ? 'Uploading...' : 'Upload & Associate'}
          </button>
        {:else}
          <button class="btn btn-primary" on:click={handleLinkDocument} disabled={linking || !selectedDocumentId}>
            {linking ? 'Linking...' : 'Link Document'}
          </button>
        {/if}
      </div>
    </div>
  </div>
{/if}

<style>
  .sip-edit-page {
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
    border-top: 4px solid #ec4899;
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
  .btn-primary { background: #ec4899; color: white; }
  .btn-primary:hover:not(:disabled) { background: #db2777; }
  .btn-group { display: flex; }
  .btn-group .btn { border-radius: 0; }
  .btn-group .btn:first-child { border-radius: 0.375rem 0 0 0.375rem; }
  .btn-group .btn-group-right {
    border-radius: 0 0.375rem 0.375rem 0;
    border-left: 1px solid rgba(255, 255, 255, 0.3);
    padding: 0.5rem 0.75rem;
    text-decoration: none;
    font-weight: 700;
  }
  .btn-generate { background: #10b981; color: white; }
  .btn-generate:hover:not(:disabled) { background: #059669; }
  .btn-danger { background: #ef4444; color: white; }
  .btn-danger:hover:not(:disabled) { background: #dc2626; }

  .btn-add-child {
    padding: 0.5rem 1rem;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 0.375rem;
    font-weight: 600;
    font-size: 0.8rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-add-child:hover { background: #2563eb; }

  .btn-sm {
    padding: 0.25rem 0.5rem;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.7rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-add { background: #dbeafe; color: #1e40af; }
  .btn-add:hover { background: #bfdbfe; }
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

  .standard-badge { background: #fce7f3; color: #9d174d; }
  .draft { background: #fef3c7; color: #92400e; }
  .submitted { background: #dbeafe; color: #1e40af; }
  .validated { background: #dcfce7; color: #166534; }
  .accepted { background: #d1fae5; color: #065f46; }
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
    border-color: #ec4899;
    box-shadow: 0 0 0 2px rgba(236, 72, 153, 0.1);
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
  .fields-table tbody tr:hover { background: #fdf2f8; }

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
    background: #e0f2fe;
    color: #0369a1;
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
    background: #f0f9ff;
    color: #0369a1;
    border-radius: 1rem;
    font-size: 0.8rem;
    font-weight: 500;
    border: 1px solid #bae6fd;
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
    border-color: #3b82f6;
    background: #eff6ff;
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
    border-color: #3b82f6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  .form-group textarea { resize: vertical; }

  /* Documents */
  .doc-actions {
    display: flex;
    gap: 0.5rem;
    align-items: center;
  }

  .documents-list {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-top: 1rem;
  }

  .document-card {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem 1rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    background: #fafbfc;
    gap: 1rem;
  }

  .document-info {
    flex: 1;
    min-width: 0;
  }

  .document-title {
    display: block;
    font-size: 0.875rem;
    color: #1e293b;
    margin-bottom: 0.25rem;
  }

  .document-meta {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 0.25rem;
    font-size: 0.75rem;
    color: #64748b;
  }

  .doc-separator {
    color: #cbd5e1;
  }

  .document-actions {
    display: flex;
    gap: 0.375rem;
    flex-shrink: 0;
  }

  .file-info-text {
    font-size: 0.8rem;
    color: #64748b;
    margin: -0.5rem 0 0.75rem 0;
  }

  .form-group select {
    width: 100%;
    padding: 0.5rem 0.625rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    font-size: 0.825rem;
    font-family: inherit;
  }

  .form-group select:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
  }

  @media (max-width: 768px) {
    .sip-edit-page { padding: 1rem; }
    .content-grid { grid-template-columns: 1fr; }
    .page-header { flex-direction: column; }
    .header-actions { width: 100%; }
    .detail-row { flex-direction: column; gap: 0.25rem; }
    .detail-label { width: auto; }
    .modal-fields-grid { grid-template-columns: 1fr; }
    .document-card { flex-direction: column; align-items: flex-start; }
  }
</style>
