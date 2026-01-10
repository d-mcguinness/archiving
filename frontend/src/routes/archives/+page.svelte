<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_ARCHIVES, GET_ALL_USERS, ASSIGN_USER_TO_ARCHIVE } from '$lib/graphql/queries';
  import { gql } from '@apollo/client/core';
  import ElementNode from './ElementNode.svelte';

  let archives: any[] = [];
  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let showAssignModal = false;
  let showStructureModal = false;
  let selectedArchive: any | null = null;
  let archiveElements: any[] = [];
  let loadingStructure = false;

  let assignUser: {
    userId: string;
    role: string;
  } = {
    userId: '',
    role: 'VIEWER'
  };

  const userRoles = ['OWNER', 'EDITOR', 'REVIEWER', 'VIEWER'];

  onMount(async () => {
    await Promise.all([loadArchives(), loadUsers()]);
  });

  async function loadArchives() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_ARCHIVES,
        fetchPolicy: 'network-only' // Always fetch fresh data
      });
      archives = result?.data?.getAllArchives || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load archives error:', e);
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

  async function assignUserToArchive() {
    if (!assignUser.userId || !selectedArchive) return;

    try {
      const result = await client.mutate({
        mutation: ASSIGN_USER_TO_ARCHIVE,
        variables: {
          input: {
            archiveId: selectedArchive.id,
            userId: assignUser.userId,
            role: assignUser.role
          }
        }
      });

      if (result.data.assignUserToArchive) {
        // Update the archive in the list
        const updatedArchive = result.data.assignUserToArchive;
        archives = archives.map((a: any) => a.id === updatedArchive.id ? updatedArchive : a);
        showAssignModal = false;
        assignUser = { userId: '', role: 'VIEWER' };
        selectedArchive = null;
      }
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
    }
  }

  function openAssignModal(archive: any) {
    selectedArchive = archive;
    showAssignModal = true;
  }

  function closeAssignModal() {
    showAssignModal = false;
    selectedArchive = null;
    assignUser = { userId: '', role: 'VIEWER' };
  }

  function getStatusBadgeClass(status: string) {
    return status.toLowerCase();
  }

  function getUserName(userId: string) {
    const user = users.find((u: any) => u.id === userId);
    return user ? user.name : 'Unknown User';
  }

  async function openStructureModal(archive: any) {
    selectedArchive = archive;
    showStructureModal = true;
    await loadArchiveStructure(archive.id);
  }

  function closeStructureModal() {
    showStructureModal = false;
    selectedArchive = null;
    archiveElements = [];
  }

  async function loadArchiveStructure(archiveId: string) {
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

      // Build hierarchy from flat list
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
</script>

<svelte:head>
  <title>Archives - Archiving System</title>
</svelte:head>

<div class="archives-page">
  <div class="page-header">
    <h1>Archives</h1>
    <a href="/archives/create" class="add-archive-btn">Add Archive</a>
  </div>

  {#if error}
    <div class="error">
      Error: {error}
    </div>
  {/if}

  {#if loading}
    <div class="loading">
      <div class="spinner"></div>
    </div>
  {:else if archives.length === 0}
    <div class="empty-state">
      <p>No archives found. Create your first archive to get started!</p>
    </div>
  {:else}
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Title</th>
            <th>Status</th>
            <th>Standard</th>
            <th>Owner</th>
            <th>Created</th>
            <th>Updated</th>
            <th>Assigned Users</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {#each archives as archive (archive.id)}
            <tr>
              <td class="id-cell">{archive.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="archive-title">{archive.title}</div>
                  {#if archive.description}
                    <div class="archive-description">{archive.description}</div>
                  {/if}
                  {#if archive.content}
                    <div class="content-preview">
                      {archive.content.substring(0, 100)}{archive.content.length > 100 ? '...' : ''}
                    </div>
                  {/if}
                </div>
              </td>
              <td class="status-cell">
                <span class="badge {getStatusBadgeClass(archive.status)}">{archive.status}</span>
              </td>
              <td class="standard-cell">
                <span class="badge standard-badge">{archive.standard}</span>
              </td>
              <td class="owner-cell">{getUserName(archive.ownerId)}</td>
              <td class="date-cell">{new Date(archive.createdAt).toLocaleDateString()}</td>
              <td class="date-cell">{new Date(archive.updatedAt).toLocaleDateString()}</td>
              <td class="assigned-cell">
                {#if archive.assignedUsers && archive.assignedUsers.length > 0}
                  <div class="assigned-users-compact">
                    {#each archive.assignedUsers.slice(0, 2) as assignment}
                      <span class="user-badge">{getUserName(assignment.userId)}</span>
                    {/each}
                    {#if archive.assignedUsers.length > 2}
                      <span class="more-badge">+{archive.assignedUsers.length - 2}</span>
                    {/if}
                  </div>
                {:else}
                  <span class="no-users">None</span>
                {/if}
              </td>
              <td class="actions-cell">
                <button class="btn-action btn-structure" on:click={() => openStructureModal(archive)}>
                  📋 Structure
                </button>
                <button class="btn-action btn-assign" on:click={() => openAssignModal(archive)}>
                  Assign
                </button>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
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
    aria-labelledby="modal-title"
  >
    <div
      class="modal"
      on:click|stopPropagation
      on:keydown|stopPropagation
      role="document"
    >
      <div class="modal-header">
        <h3 id="modal-title">Assign User to Archive</h3>
        <button
          class="close-btn"
          on:click={closeAssignModal}
          aria-label="Close modal"
        >&times;</button>
      </div>

      <div class="modal-content">
        <p>Archive: <strong>{selectedArchive?.title}</strong></p>

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

<!-- Archive Structure Modal -->
{#if showStructureModal}
  <div
    class="modal-overlay"
    on:click={closeStructureModal}
    on:keydown={(e) => e.key === 'Escape' && closeStructureModal()}
    role="dialog"
    aria-modal="true"
    aria-labelledby="structure-modal-title"
  >
    <div
      class="modal structure-modal"
      on:click|stopPropagation
      on:keydown|stopPropagation
      role="document"
    >
      <div class="modal-header">
        <h3 id="structure-modal-title">Archive Structure: {selectedArchive?.title}</h3>
        <button
          class="close-btn"
          on:click={closeStructureModal}
          aria-label="Close modal"
        >&times;</button>
      </div>

      <div class="modal-content">
        {#if loadingStructure}
          <div class="loading-structure">
            <div class="spinner"></div>
            <p>Loading archive structure...</p>
          </div>
        {:else if archiveElements.length === 0}
          <div class="empty-structure">
            <p>📋 No elements found in this archive.</p>
            <p class="hint">Elements will appear here once they are added to the archive.</p>
          </div>
        {:else}
          <div class="structure-info">
            <div class="info-badge">
              <span class="info-label">Standard:</span>
              <span class="info-value">{selectedArchive?.standard}</span>
            </div>
            <div class="info-badge">
              <span class="info-label">Elements:</span>
              <span class="info-value">{archiveElements.length} root element(s)</span>
            </div>
          </div>

          <div class="structure-tree">
            {#each archiveElements as element}
              <div class="element-tree-item">
                <ElementNode elementNode={element} level={0} readonly={true} />
              </div>
            {/each}
          </div>
        {/if}
      </div>

      <div class="modal-actions">
        <button class="btn btn-secondary" on:click={closeStructureModal}>Close</button>
      </div>
    </div>
  </div>
{/if}


<style>
  .archives-page {
    max-width: 1600px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 2rem;
  }

  .page-header h1 {
    margin: 0;
    color: #1e293b;
    font-size: 2rem;
  }

  .add-archive-btn {
    background: #3b82f6;
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.375rem;
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .add-archive-btn:hover {
    background: #2563eb;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
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
    justify-content: center;
    align-items: center;
    min-height: 400px;
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

  /* Table Styles */
  .table-container {
    background: white;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow-x: auto;
    border: 1px solid #e2e8f0;
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
    min-width: 1400px;
  }

  .data-table thead {
    background: #f8fafc;
    border-bottom: 2px solid #e2e8f0;
  }

  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    color: #475569;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    white-space: nowrap;
  }

  .data-table tbody tr {
    border-bottom: 1px solid #e2e8f0;
    transition: background-color 0.15s;
  }

  .data-table tbody tr:hover {
    background: #f8fafc;
  }

  .data-table tbody tr:last-child {
    border-bottom: none;
  }

  .data-table td {
    padding: 1rem;
    color: #1e293b;
  }

  .id-cell {
    font-family: 'Monaco', 'Courier New', monospace;
    color: #64748b;
    font-size: 0.875rem;
    width: 60px;
  }

  .title-cell {
    min-width: 300px;
    max-width: 400px;
  }

  .title-wrapper {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .archive-title {
    font-weight: 500;
    color: #1e293b;
  }

  .archive-description {
    font-size: 0.875rem;
    color: #64748b;
    line-height: 1.4;
  }

  .content-preview {
    font-size: 0.75rem;
    color: #94a3b8;
    font-family: 'Monaco', 'Courier New', monospace;
    line-height: 1.4;
    margin-top: 0.25rem;
    max-height: 2.8em;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .status-cell,
  .standard-cell {
    white-space: nowrap;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.025em;
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

  .owner-cell {
    color: #64748b;
    font-size: 0.875rem;
    white-space: nowrap;
  }

  .date-cell {
    color: #64748b;
    font-size: 0.875rem;
    white-space: nowrap;
    width: 120px;
  }

  .assigned-cell {
    min-width: 150px;
  }

  .assigned-users-compact {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
  }

  .user-badge {
    display: inline-block;
    padding: 0.125rem 0.5rem;
    background: #dbeafe;
    color: #1e40af;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 500;
  }

  .more-badge {
    display: inline-block;
    padding: 0.125rem 0.5rem;
    background: #f1f5f9;
    color: #64748b;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .no-users {
    color: #cbd5e1;
    font-style: italic;
    font-size: 0.875rem;
  }

  .actions-cell {
    text-align: right;
    white-space: nowrap;
    width: 250px;
  }

  .btn-action {
    display: inline-block;
    padding: 0.5rem 1rem;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    font-weight: 500;
    transition: all 0.2s;
    margin-left: 0.5rem;
    border: none;
    cursor: pointer;
  }

  .btn-structure {
    background: #3b82f6;
    color: white;
  }

  .btn-structure:hover {
    background: #2563eb;
  }

  .btn-assign {
    background: #10b981;
    color: white;
  }

  .btn-assign:hover {
    background: #059669;
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

  .structure-modal {
    min-width: 600px;
    max-width: 800px;
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

  /* Structure Modal Styles */
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

  .info-label {
    font-size: 0.75rem;
    color: #64748b;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .info-value {
    font-size: 1rem;
    color: #1e293b;
    font-weight: 600;
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

  @media (max-width: 768px) {
    .modal {
      min-width: 300px;
      margin: 1rem;
    }

    .structure-modal {
      min-width: 300px;
    }
  }
</style>
