<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS, ASSIGN_USER_TO_ARCHIVE } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import ElementNode from '../../ElementNode.svelte';
  import ElementFormModal from '../../ElementFormModal.svelte';
  import ArchiveCanvas from '../../ArchiveCanvas.svelte';

  let archive: any = null;
  let users: any[] = [];
  let archiveElements: any[] = [];
  let loading = true;
  let loadingStructure = false;
  let error: string | null = null;
  let showAssignModal = false;
  let showElementModal = false;
  let editingElement: any = null;
  let selectedParent: any = null;
  let schemes: any[] = [];
  let isEditMode = false;
  let saving = false;

  // Editable archive fields
  let editableArchive = {
    title: '',
    description: '',
    content: '',
    status: 'DRAFT'
  };

  let elementForm = {
    fieldValues: {} as Record<string, any>
  };

  let assignUser: {
    userId: string;
    role: string;
  } = {
    userId: '',
    role: 'VIEWER'
  };

  const userRoles = ['OWNER', 'EDITOR', 'REVIEWER', 'VIEWER'];

  // Get archive ID from route parameter
  $: archiveId = $page.params.id;

  onMount(async () => {

    await Promise.all([loadArchive(), loadUsers(), loadArchiveStructure()]);
  });

  async function loadArchive() {
    try {
      const GET_ARCHIVE = gql`
        query GetArchive($id: ID!) {
          getArchive(id: $id) {
            id
            title
            description
            content
            status
            standard
            createdAt
            updatedAt
            ownerId
            assignedUsers {
              id
              userId
              role
              assignedAt
            }
          }
        }
      `;

      const result = await client.query({
        query: GET_ARCHIVE,
        variables: { id: archiveId },
        fetchPolicy: 'network-only'
      });

      archive = result?.data?.getArchive;
      error = null;

      // Initialize editable fields
      if (archive) {
        editableArchive = {
          title: archive.title || '',
          description: archive.description || '',
          content: archive.content || '',
          status: archive.status || 'DRAFT'
        };
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load archive';
      console.error('Load archive error:', e);
    } finally {
      loading = false;
    }
  }

  async function loadUsers() {
    try {
      const result = await client.query({ query: GET_ALL_USERS });
      users = result.data.getAllUsers || [];
    } catch (e) {
      console.error('Failed to load users:', e instanceof Error ? e.message : 'Unknown error');
    }
  }

  async function loadSchemeDefinition(standard: string) {
    try {
      const fileName = standard === 'NOARK5' ? 'noark5.json' : 'oais.json';
      const response = await fetch(`/schemeDefintions/${fileName}`);
      if (!response.ok) throw new Error(`Failed to fetch ${fileName}`);
      const data = await response.json();
      schemes = data.entities || [];
    } catch (e) {
      console.error('Failed to load scheme:', e);
      schemes = [];
    }
  }

  async function loadArchiveStructure() {
    try {
      loadingStructure = true;
      const GET_ARCHIVE_ELEMENTS = gql`
        query GetArchiveElements($archiveId: ID!) {
          getElementsByArchive(archiveId: $archiveId) {
            id
            elementIdentifier
            title
            description
            status
            createdAt
            isRoot
            entityName
            entityType
            norwegianName
            englishName
            parent {
              id
            }
            children {
              id
              elementIdentifier
              title
              entityName
              entityType
            }
          }
        }
      `;

      const result = await client.query({
        query: GET_ARCHIVE_ELEMENTS,
        variables: { archiveId },
        fetchPolicy: 'network-only'
      });

      console.log('Archive elements loaded:', result?.data?.getElementsByArchive);
      archiveElements = buildHierarchy(result?.data?.getElementsByArchive || []);
      console.log('Built hierarchy:', archiveElements);
    } catch (e) {
      console.error('Failed to load archive structure:', e);
      archiveElements = [];
    } finally {
      loadingStructure = false;
    }
  }

  function buildHierarchy(elements: any[]): any[] {
    const elementMap = new Map();
    const roots: any[] = [];

    // First pass: create map
    elements.forEach(el => {
      elementMap.set(el.id, { ...el, children: [] });
    });

    // Second pass: build hierarchy
    elements.forEach(el => {
      const element = elementMap.get(el.id);
      if (el.parent) {
        const parent = elementMap.get(el.parent.id);
        if (parent) {
          parent.children.push(element);
        }
      } else if (el.isRoot) {
        roots.push(element);
      }
    });

    return roots;
  }

  function getUserName(userId: string) {
    const user = users.find((u: any) => u.id === userId);
    return user ? user.name : 'Unknown User';
  }

  function openAssignModal() {
    showAssignModal = true;
  }

  function closeAssignModal() {
    showAssignModal = false;
    assignUser = { userId: '', role: 'VIEWER' };
  }

  async function assignUserToArchive() {
    if (!assignUser.userId || !archive) return;

    try {
      const result = await client.mutate({
        mutation: ASSIGN_USER_TO_ARCHIVE,
        variables: {
          input: {
            archiveId: archive.id,
            userId: assignUser.userId,
            role: assignUser.role
          }
        }
      });

      if (result.data.assignUserToArchive) {
        // Update the archive with new assignment
        archive = result.data.assignUserToArchive;
        showAssignModal = false;
        assignUser = { userId: '', role: 'VIEWER' };
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
    }
  }

  function goBack() {
    goto('/archives');
  }

  function enableEditMode() {
    isEditMode = true;
    // Reset editable fields to current archive values
    if (archive) {
      editableArchive = {
        title: archive.title || '',
        description: archive.description || '',
        content: archive.content || '',
        status: archive.status || 'DRAFT'
      };
    }
  }

  function cancelEdit() {
    isEditMode = false;
    error = null;
  }

  async function saveArchive() {
    if (!archive || !editableArchive.title) {
      error = 'Title is required';
      return;
    }

    try {
      saving = true;
      error = null;

      console.log('Saving archive with data:', {
        id: archive.id,
        input: {
          title: editableArchive.title,
          description: editableArchive.description || '',
          content: editableArchive.content,
          status: editableArchive.status
        }
      });

      const UPDATE_ARCHIVE = gql`
        mutation UpdateArchive($id: ID!, $input: UpdateArchiveInput!) {
          updateArchive(id: $id, input: $input) {
            id
            title
            description
            content
            status
            updatedAt
          }
        }
      `;

      const result = await client.mutate({
        mutation: UPDATE_ARCHIVE,
        variables: {
          id: archive.id,
          input: {
            title: editableArchive.title,
            description: editableArchive.description || '',
            content: editableArchive.content,
            status: editableArchive.status
          }
        },
        refetchQueries: ['GetArchive'],
        awaitRefetchQueries: true
      });

      console.log('Update result:', result);

      if (result.data?.updateArchive) {
        // Update the archive with new data from server
        archive = { ...archive, ...result.data.updateArchive };
        console.log('Archive updated successfully:', archive);
        isEditMode = false;
        // Reload the archive to ensure we have the latest data
        await loadArchive();
      } else {
        console.error('No data returned from mutation');
        error = 'Failed to save: No data returned from server';
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to save archive';
      console.error('Save archive error:', e);
    } finally {
      saving = false;
    }
  }

  async function openEditElement(element: any) {
    if (!archive) return;

    // Load scheme definitions if not already loaded
    if (schemes.length === 0) {
      await loadSchemeDefinition(archive.standard);
    }

    editingElement = element;
    selectedParent = null;

    // Find the scheme for this element (element.entityName matches scheme.name)
    const scheme = schemes.find(s => s.name === element.entityName);

    // Initialize form with element's current field values
    elementForm.fieldValues = {};
    if (scheme && scheme.fields) {
      scheme.fields.forEach((field: any) => {
        elementForm.fieldValues[field.name] = '';
      });
    }

    // Load element's fields from backend
    await loadElementFields(element.id);

    showElementModal = true;
  }

  async function loadElementFields(elementId: string) {
    try {
      const GET_ELEMENT_FIELDS = gql`
        query GetElement($id: ID!) {
          getElement(id: $id) {
            id
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

      const result = await client.query({
        query: GET_ELEMENT_FIELDS,
        variables: { id: elementId },
        fetchPolicy: 'network-only'
      });

      const element = result?.data?.getElement;
      if (element && element.fields) {
        element.fields.forEach((field: any) => {
          elementForm.fieldValues[field.name] = field.value || '';
        });
      }
    } catch (e) {
      console.error('Failed to load element fields:', e);
    }
  }


  async function saveElementFields() {
    if (!editingElement) return;

    try {
      // Fix: Use s.name instead of s.entityName to match the template logic
      const scheme = schemes.find(s => s.name === editingElement.entityName);

      console.log('Saving element fields:', {
        elementId: editingElement.id,
        entityName: editingElement.entityName,
        scheme: scheme,
        fieldValues: elementForm.fieldValues
      });

      const fields = scheme?.fields ? scheme.fields.map((fieldDef: any) => ({
        name: fieldDef.name,
        label: fieldDef.label,
        type: fieldDef.type,
        value: elementForm.fieldValues[fieldDef.name] || ''
      })) : [];

      console.log('Mapped fields to save:', fields);

      const UPDATE_ELEMENT = gql`
        mutation UpdateElement($id: ID!, $input: UpdateElementInput!, $fields: [FieldInput!]) {
          updateElement(id: $id, input: $input, fields: $fields) {
            id
            title
            description
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

      const result = await client.mutate({
        mutation: UPDATE_ELEMENT,
        variables: {
          id: editingElement.id,
          input: {
            title: editingElement.title,
            description: editingElement.description || '',
            updatedBy: 'system'
          },
          fields: fields
        },
        refetchQueries: ['GetArchiveElements'],
        awaitRefetchQueries: true
      });

      console.log('Element update result:', result);

      closeElementModal();
      await loadArchiveStructure();
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to save element fields';
      console.error('Save element fields error:', e);
    }
  }

  async function deleteElement(elementId: string) {
    if (!confirm('Are you sure you want to delete this element and all its children?')) {
      return;
    }

    try {
      const DELETE_ELEMENT = gql`
        mutation DeleteElement($id: ID!) {
          deleteElement(id: $id)
        }
      `;

      await client.mutate({
        mutation: DELETE_ELEMENT,
        variables: { id: elementId },
        refetchQueries: [
          {
            query: gql`
              query GetArchiveElements($archiveId: ID!) {
                getElementsByArchive(archiveId: $archiveId) {
                  id
                  elementIdentifier
                  title
                  description
                  status
                  createdAt
                  isRoot
                  entityName
                  entityType
                  norwegianName
                  englishName
                  parent {
                    id
                  }
                  children {
                    id
                    elementIdentifier
                    title
                    entityName
                    entityType
                  }
                }
              }
            `,
            variables: { archiveId: archive?.id }
          }
        ],
        awaitRefetchQueries: true
      });

      await loadArchiveStructure();
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to delete element';
      console.error('Delete element error:', e);
    }
  }

  async function openAddChildElement(parent: any) {
    if (!archive) return;

    // Load scheme definitions if not already loaded
    if (schemes.length === 0) {
      await loadSchemeDefinition(archive.standard);
    }

    editingElement = null;
    selectedParent = parent;
    showElementModal = true;
  }

  async function openAddRootElement() {
    if (!archive) return;

    // Load scheme definitions if not already loaded
    if (schemes.length === 0) {
      await loadSchemeDefinition(archive.standard);
    }

    editingElement = null;
    selectedParent = null; // No parent for root elements
    showElementModal = true;
  }

  function closeElementModal() {
    showElementModal = false;
    editingElement = null;
    selectedParent = null;
    elementForm.fieldValues = {};
  }

  async function handleElementAdd(event: CustomEvent) {
    const { scheme, elementIdentifier, title, description, fields } = event.detail;

    if (!archive) {
      error = 'Missing archive';
      return;
    }

    // Allow creating root elements (no parent) or child elements (with parent)
    if (!selectedParent && scheme.type !== 'root') {
      error = 'Only root-level elements can be added without a parent';
      return;
    }

    try {
      const CREATE_ELEMENT = gql`
        mutation CreateElement($input: CreateElementInput!) {
          createElement(input: $input) {
            id
            elementIdentifier
            title
            description
            entityName
            entityType
            norwegianName
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

      await client.mutate({
        mutation: CREATE_ELEMENT,
        variables: {
          input: {
            archiveId: archive.id,
            parentElementId: selectedParent?.id || null, // Null for root elements
            elementIdentifier: elementIdentifier,
            entityName: scheme.name,
            entityType: scheme.type,
            norwegianName: scheme.name,
            englishName: scheme.name,
            title: title,
            description: description || null,
            createdBy: 'system',
            fields: fields
          }
        }
      });

      closeElementModal();
      await loadArchiveStructure();
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to create element';
      console.error('Create element error:', e);
    }
  }
</script>

<svelte:head>
  <title>Update Archive - Archiving System</title>
</svelte:head>

<div class="update-page">
  <div class="page-header">
    <div>
      <button class="back-btn" on:click={goBack}>← Back to Archives</button>
      <h1>Archive Details</h1>
    </div>
  </div>

  {#if error}
    <div class="error">
      Error: {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
      <p>Loading archive...</p>
    </div>
  {:else if !archive}
    <div class="empty-state">
      <p>Archive not found.</p>
    </div>
  {:else}
    <div class="content-grid">
      <!-- Archive Info Card -->
      <div class="card info-card">
        <div class="card-header-with-actions">
          <h2>Archive Information</h2>
          {#if !isEditMode}
            <button class="btn btn-edit-toggle" on:click={enableEditMode}>
              ✏️ Edit
            </button>
          {/if}
        </div>

        {#if isEditMode}
          <!-- Editable Form -->
          <div class="edit-form">
            <div class="form-group">
              <label for="edit-title">Title *</label>
              <input
                type="text"
                id="edit-title"
                bind:value={editableArchive.title}
                required
                placeholder="Enter archive title"
              />
            </div>

            <div class="form-group">
              <label for="edit-status">Status *</label>
              <select id="edit-status" bind:value={editableArchive.status}>
                <option value="DRAFT">Draft</option>
                <option value="ACTIVE">Active</option>
                <option value="PUBLISHED">Published</option>
                <option value="ARCHIVED">Archived</option>
                <option value="DELETED">Deleted</option>
              </select>
            </div>

            <div class="form-group">
              <label for="edit-description">Description</label>
              <textarea
                id="edit-description"
                bind:value={editableArchive.description}
                rows="3"
                placeholder="Enter archive description"
              ></textarea>
            </div>

            <div class="form-group">
              <label for="edit-content">Content *</label>
              <textarea
                id="edit-content"
                bind:value={editableArchive.content}
                rows="6"
                required
                placeholder="Enter archive content"
              ></textarea>
            </div>

            <div class="form-actions-inline">
              <button class="btn btn-secondary" on:click={cancelEdit} disabled={saving}>
                Cancel
              </button>
              <button class="btn btn-primary" on:click={saveArchive} disabled={saving || !editableArchive.title || !editableArchive.content}>
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </div>
        {:else}
          <!-- Read-only View -->
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">ID:</span>
              <span class="info-value">{archive.id}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Title:</span>
              <span class="info-value">{archive.title}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Status:</span>
              <span class="badge {archive.status.toLowerCase()}">{archive.status}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Standard:</span>
              <span class="badge standard-badge">{archive.standard}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Owner:</span>
              <span class="info-value">{getUserName(archive.ownerId)}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Created:</span>
              <span class="info-value">{new Date(archive.createdAt).toLocaleDateString()}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Updated:</span>
              <span class="info-value">{new Date(archive.updatedAt).toLocaleDateString()}</span>
            </div>
            {#if archive.description}
              <div class="info-item full-width">
                <span class="info-label">Description:</span>
                <span class="info-value">{archive.description}</span>
              </div>
            {/if}
            {#if archive.content}
              <div class="info-item full-width">
                <span class="info-label">Content:</span>
                <span class="info-value content-text">{archive.content}</span>
              </div>
            {/if}
          </div>
        {/if}
      </div>

      <!-- Assigned Users Card -->
      <div class="card users-card">
        <div class="card-header">
          <h2>Assigned Users</h2>
          <button class="btn btn-primary btn-sm" on:click={openAssignModal}>
            + Assign User
          </button>
        </div>

        {#if archive.assignedUsers && archive.assignedUsers.length > 0}
          <div class="users-list">
            {#each archive.assignedUsers as assignment}
              <div class="user-item">
                <div class="user-info">
                  <span class="user-name">{getUserName(assignment.userId)}</span>
                  <span class="user-role">{assignment.role}</span>
                </div>
                <span class="assigned-date">
                  Assigned: {new Date(assignment.assignedAt).toLocaleDateString()}
                </span>
              </div>
            {/each}
          </div>
        {:else}
          <div class="empty-users">
            <p>No users assigned to this archive.</p>
            <p class="hint">Click "Assign User" to add users.</p>
          </div>
        {/if}
      </div>

      <!-- Archive Structure Card -->
      <div class="card structure-card">
        <div class="card-header">
          <h2>Archive Structure</h2>
          <button class="btn btn-primary btn-sm" on:click={openAddRootElement}>
            + Add Root Element
          </button>
        </div>

        {#if loadingStructure}
          <div class="loading-structure">
            <div class="spinner"></div>
            <p>Loading archive structure...</p>
          </div>
        {:else}
          <div class="structure-info">
            <div class="info-badge">
              <span class="info-label">Standard:</span>
              <span class="info-value">{archive.standard}</span>
            </div>
            <div class="info-badge">
              <span class="info-label">Elements:</span>
              <span class="info-value">{archiveElements.length} root element(s)</span>
            </div>
          </div>

          <ArchiveCanvas
            elements={archiveElements}
            readonly={false}
            onAddChild={openAddChildElement}
            onDelete={deleteElement}
            onEdit={openEditElement}
            on:addElement={openAddRootElement}
          />
        {/if}
      </div>
    </div>
  {/if}

  <!-- Back Button at Bottom -->
  {#if archive}
    <div class="bottom-actions">
      <button class="btn btn-secondary" on:click={goBack}>
        ← Back to Archives
      </button>
    </div>
  {/if}
</div>

<!-- Assign User Modal -->
{#if showAssignModal}
  <div
    class="modal-overlay"
    on:click={closeAssignModal}
    on:keydown={(e) => e.key === 'Escape' && closeAssignModal()}
    role="dialog"
    aria-modal="true"
  >
    <div
      class="modal"
      on:click|stopPropagation
      on:keydown|stopPropagation
      role="document"
    >
      <div class="modal-header">
        <h3>Assign User to Archive</h3>
        <button
          class="close-btn"
          on:click={closeAssignModal}
          aria-label="Close modal"
        >&times;</button>
      </div>

      <div class="modal-content">
        <p>Archive: <strong>{archive?.title}</strong></p>

        <div class="form-group">
          <label for="assignUserId">User</label>
          <select id="assignUserId" bind:value={assignUser.userId}>
            <option value="">Select a user</option>
            {#each users as user}
              <option value={user.id}>{user.name} ({user.email})</option>
            {/each}
          </select>
        </div>

        <div class="form-group">
          <label for="assignRole">Role</label>
          <select id="assignRole" bind:value={assignUser.role}>
            {#each userRoles as role}
              <option value={role}>{role}</option>
            {/each}
          </select>
        </div>
      </div>

      <div class="modal-actions">
        <button class="btn btn-secondary" on:click={closeAssignModal}>Cancel</button>
        <button class="btn btn-primary" on:click={assignUserToArchive} disabled={!assignUser.userId}>
          Assign User
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- Edit Element Modal (for editing existing elements) -->
{#if showElementModal && editingElement}
  <div
    class="modal-overlay"
    on:click={closeElementModal}
    on:keydown={(e) => e.key === 'Escape' && closeElementModal()}
    role="dialog"
    aria-modal="true"
  >
    <div
      class="modal element-modal"
      on:click|stopPropagation
      on:keydown|stopPropagation
      role="document"
    >
      <div class="modal-header">
        <h3>
          Edit {editingElement.norwegianName || editingElement.entityName}: {editingElement.elementIdentifier}
        </h3>
        <button
          class="close-btn"
          on:click={closeElementModal}
          aria-label="Close modal"
        >&times;</button>
      </div>

      <div class="modal-content">
        <p class="modal-description">
          Edit the field values for this element. Changes will be saved to the database.
        </p>

        <!-- Core Element Fields -->
        <div class="form-fields">
          <div class="form-group">
            <label for="edit-element-identifier">
              Element Identifier
              <span class="field-type">(read-only)</span>
            </label>
            <input
              type="text"
              id="edit-element-identifier"
              value={editingElement.elementIdentifier}
              disabled
              class="readonly-field"
            />
          </div>

          <div class="form-group">
            <label for="edit-element-title">
              Title *
            </label>
            <input
              type="text"
              id="edit-element-title"
              bind:value={editingElement.title}
              required
              placeholder="Enter element title"
            />
          </div>

          <div class="form-group">
            <label for="edit-element-description">
              Description
            </label>
            <textarea
              id="edit-element-description"
              bind:value={editingElement.description}
              rows="3"
              placeholder="Enter element description"
            ></textarea>
          </div>
        </div>

        <!-- Dynamic Schema Fields -->
        {#if schemes.length > 0}
          {@const scheme = schemes.find(s => s.name === editingElement.entityName)}
          {#if scheme && scheme.fields && scheme.fields.length > 0}
            <div class="schema-fields-section">
              <h4>Schema Fields</h4>
              <div class="form-fields">
                {#each scheme.fields as field}
                  <div class="form-group">
                    <label for={`edit-field-${field.name}`}>
                      {field.label || field.name}
                      {#if field.type}
                        <span class="field-type">({field.type})</span>
                      {/if}
                    </label>

                    {#if field.type === 'date'}
                      <input
                        type="date"
                        id={`edit-field-${field.name}`}
                        value={elementForm.fieldValues[field.name] || ''}
                        on:input={(e) => {
                          elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                          elementForm.fieldValues = elementForm.fieldValues;
                        }}
                        placeholder={`Enter ${field.label || field.name}`}
                      />
                    {:else if field.type === 'number'}
                      <input
                        type="number"
                        id={`edit-field-${field.name}`}
                        value={elementForm.fieldValues[field.name] || ''}
                        on:input={(e) => {
                          elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                          elementForm.fieldValues = elementForm.fieldValues;
                        }}
                        placeholder={`Enter ${field.label || field.name}`}
                      />
                    {:else}
                      <input
                        type="text"
                        id={`edit-field-${field.name}`}
                        value={elementForm.fieldValues[field.name] || ''}
                        on:input={(e) => {
                          elementForm.fieldValues[field.name] = e.currentTarget?.value || '';
                          elementForm.fieldValues = elementForm.fieldValues;
                        }}
                        placeholder={`Enter ${field.label || field.name}`}
                      />
                    {/if}
                  </div>
                {/each}
              </div>
            </div>
          {:else}
            <p class="no-fields">No additional schema fields available for this element type.</p>
          {/if}
        {:else}
          <div class="loading-fields">
            <div class="spinner"></div>
            <p>Loading schema fields...</p>
          </div>
        {/if}
      </div>

      <div class="modal-actions">
        <button class="btn btn-secondary" on:click={closeElementModal}>Cancel</button>
        <button class="btn btn-primary" on:click={saveElementFields}>
          Save Changes
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- Add Child Element Modal (using reusable component) -->
<!-- Shows for both root elements (selectedParent = null) and child elements (selectedParent !== null) -->
<ElementFormModal
  show={showElementModal && !editingElement}
  {schemes}
  {selectedParent}
  on:add={handleElementAdd}
  on:cancel={closeElementModal}
/>

<style>
  .update-page {
    max-width: 1400px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    margin-bottom: 2rem;
  }

  .back-btn {
    background: none;
    border: none;
    color: #3b82f6;
    cursor: pointer;
    font-size: 0.875rem;
    padding: 0.5rem 0;
    margin-bottom: 0.5rem;
    transition: color 0.2s;
  }

  .back-btn:hover {
    color: #2563eb;
  }

  .page-header h1 {
    margin: 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .error {
    background: #fee;
    color: #c00;
    padding: 1rem;
    border-radius: 0.375rem;
    margin-bottom: 1rem;
    border: 1px solid #fcc;
  }

  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
  }

  .loading p {
    margin-top: 1rem;
    color: #64748b;
  }

  .spinner {
    border: 4px solid #f3f4f6;
    border-top: 4px solid #3b82f6;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
    background: #f9fafb;
    border-radius: 0.5rem;
    color: #64748b;
  }

  .content-grid {
    display: grid;
    gap: 2rem;
    grid-template-columns: 1fr;
  }

  .bottom-actions {
    display: flex;
    justify-content: center;
    margin-top: 3rem;
    padding-top: 2rem;
    border-top: 1px solid #e2e8f0;
  }

  .card {
    background: white;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
    padding: 1.5rem;
  }

  .card h2 {
    margin: 0 0 1.5rem 0;
    color: #1e293b;
    font-size: 1.25rem;
    font-weight: 600;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
  }

  .card-header h2 {
    margin: 0;
  }

  .card-header-with-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
  }

  .card-header-with-actions h2 {
    margin: 0;
    color: #1e293b;
    font-size: 1.25rem;
    font-weight: 600;
  }

  .btn-edit-toggle {
    padding: 0.5rem 1rem;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-edit-toggle:hover {
    background: #2563eb;
  }

  .edit-form {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .form-group label {
    font-size: 0.875rem;
    font-weight: 500;
    color: #1e293b;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    padding: 0.75rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.25rem;
    font-size: 1rem;
    transition: border-color 0.2s;
    font-family: inherit;
  }

  .form-group input:focus,
  .form-group select:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
  }

  .form-group textarea {
    resize: vertical;
    min-height: 80px;
  }

  .form-actions-inline {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
    margin-top: 0.5rem;
    padding-top: 1rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn-sm {
    padding: 0.5rem 1rem;
    font-size: 0.8125rem;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
  }

  .btn-secondary {
    background: #e2e8f0;
    color: #1e293b;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #cbd5e1;
  }

  .info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1rem;
  }

  .info-item {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .info-item.full-width {
    grid-column: 1 / -1;
  }

  .info-label {
    font-size: 0.75rem;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    font-weight: 600;
  }

  .info-value {
    font-size: 1rem;
    color: #1e293b;
  }

  .content-text {
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 0.875rem;
    line-height: 1.6;
    color: #475569;
    background: #f8fafc;
    padding: 0.75rem;
    border-radius: 0.25rem;
    white-space: pre-wrap;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
    width: fit-content;
  }

  .badge.active,
  .badge.published {
    background: #dcfce7;
    color: #166534;
  }

  .badge.draft {
    background: #f3f4f6;
    color: #6b7280;
  }

  .badge.archived,
  .badge.deleted {
    background: #fef3c7;
    color: #92400e;
  }

  .standard-badge {
    background: #e0e7ff;
    color: #4338ca;
  }

  .users-list {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .user-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.75rem;
    background: #f8fafc;
    border-radius: 0.375rem;
    border: 1px solid #e2e8f0;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .user-name {
    font-weight: 500;
    color: #1e293b;
  }

  .user-role {
    display: inline-block;
    padding: 0.125rem 0.5rem;
    background: #dbeafe;
    color: #1e40af;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .assigned-date {
    font-size: 0.75rem;
    color: #64748b;
  }

  .empty-users {
    text-align: center;
    padding: 2rem;
    color: #64748b;
  }

  .empty-users .hint {
    font-size: 0.875rem;
    color: #94a3b8;
    margin-top: 0.5rem;
  }

  .loading-structure {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 3rem;
  }

  .loading-structure p {
    margin-top: 1rem;
    color: #64748b;
  }

  .empty-structure {
    text-align: center;
    padding: 3rem;
    color: #64748b;
  }

  .empty-structure .hint {
    font-size: 0.875rem;
    color: #94a3b8;
    margin-top: 0.5rem;
  }

  .structure-info {
    display: flex;
    gap: 1rem;
    margin-bottom: 1.5rem;
    padding: 1rem;
    background: #f8fafc;
    border-radius: 0.375rem;
  }

  .info-badge {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .structure-tree {
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
    padding: 1rem;
    background: #fafafa;
    max-height: 60vh;
    overflow-y: auto;
  }

  .element-tree-item {
    margin-bottom: 0.5rem;
  }

  .element-tree-item:last-child {
    margin-bottom: 0;
  }

  /* Modal styles */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }

  .modal {
    background: white;
    border-radius: 0.5rem;
    min-width: 400px;
    max-width: 90vw;
    max-height: 90vh;
    overflow: auto;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .modal-header h3 {
    margin: 0;
    color: #1e293b;
    font-weight: 600;
  }

  .close-btn {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    color: #94a3b8;
    width: 2rem;
    height: 2rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.25rem;
    transition: all 0.2s;
  }

  .close-btn:hover {
    color: #1e293b;
    background: #f1f5f9;
  }

  .modal-content {
    padding: 1.5rem;
  }

  .modal-content p {
    margin: 0 0 1.5rem 0;
    color: #64748b;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #1e293b;
    font-weight: 500;
    font-size: 0.875rem;
  }

  .form-group select {
    width: 100%;
    padding: 0.75rem;
    border-radius: 0.25rem;
    border: 1px solid #e2e8f0;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .form-group select:focus {
    outline: none;
    border-color: #3b82f6;
  }

  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    padding: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn {
    padding: 0.5rem 1.5rem;
    border: none;
    border-radius: 0.25rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-sm {
    padding: 0.375rem 1rem;
    font-size: 0.875rem;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover {
    background: #2563eb;
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

  .element-modal {
    min-width: 500px;
    max-width: 700px;
  }

  .modal-description {
    margin: 0 0 1.5rem 0;
    color: #64748b;
    font-size: 0.875rem;
    line-height: 1.6;
  }

  .form-fields {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .form-group input[type="text"],
  .form-group input[type="date"],
  .form-group input[type="number"],
  .form-group textarea {
    width: 100%;
    padding: 0.75rem;
    border-radius: 0.25rem;
    border: 1px solid #e2e8f0;
    font-size: 1rem;
    transition: border-color 0.2s;
    font-family: inherit;
  }

  .form-group textarea {
    resize: vertical;
    min-height: 80px;
  }

  .form-group input:focus,
  .form-group textarea:focus {
    outline: none;
    border-color: #3b82f6;
  }

  .form-group input.readonly-field {
    background: #f8fafc;
    color: #64748b;
    cursor: not-allowed;
  }

  .schema-fields-section {
    margin-top: 2rem;
    padding-top: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .schema-fields-section h4 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 600;
  }

  .field-type {
    color: #94a3b8;
    font-weight: 400;
    font-size: 0.75rem;
    margin-left: 0.25rem;
  }

  .helper-text {
    display: block;
    margin-top: 0.25rem;
    font-size: 0.75rem;
    color: #64748b;
    font-style: italic;
  }

  .no-fields {
    text-align: center;
    padding: 2rem;
    color: #94a3b8;
    font-style: italic;
  }

  .loading-fields {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 2rem;
  }

  .loading-fields p {
    margin-top: 1rem;
    color: #64748b;
  }

  @media (max-width: 768px) {
    .modal {
      min-width: 300px;
      margin: 1rem;
    }

    .element-modal {
      min-width: 300px;
    }
  }
</style>

