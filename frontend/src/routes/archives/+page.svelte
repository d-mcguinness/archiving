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
  {:else}
    <div class="archives-grid">
      {#each archives as archive (archive.id)}
        <div class="archive-card">
          <div class="archive-header">
            <h3>{archive.title}</h3>
            <div class="badges">
              <span class="badge {getStatusBadgeClass(archive.status)}">{archive.status}</span>
              <span class="badge standard-badge">{archive.standard}</span>
            </div>
          </div>

          {#if archive.description}
            <p class="description">{archive.description}</p>
          {/if}

          <div class="content-preview">
            {archive.content.substring(0, 100)}{archive.content.length > 100 ? '...' : ''}
          </div>

          <div class="archive-details">
            <div class="detail-item">
              <span class="label">Owner:</span>
              <span class="value">{getUserName(archive.ownerId)}</span>
            </div>
            <div class="detail-item">
              <span class="label">Created:</span>
              <span class="value">{new Date(archive.createdAt).toLocaleDateString()}</span>
            </div>
            <div class="detail-item">
              <span class="label">Updated:</span>
              <span class="value">{new Date(archive.updatedAt).toLocaleDateString()}</span>
            </div>
            {#if archive.assignedUsers && archive.assignedUsers.length > 0}
              <div class="detail-item">
                <span class="label">Assigned:</span>
                <span class="value">{archive.assignedUsers.length} user(s)</span>
              </div>
            {/if}
          </div>

          {#if archive.assignedUsers && archive.assignedUsers.length > 0}
            <div class="assigned-users">
              {#each archive.assignedUsers as assignment}
                <div class="user-assignment">
                  <span class="user-name">{getUserName(assignment.userId)}</span>
                  <span class="role-badge">{assignment.role}</span>
                </div>
              {/each}
            </div>
          {/if}

          <div class="archive-id">ID: {archive.id}</div>

          <div class="archive-actions">
            <button class="view-structure-btn" on:click={() => openStructureModal(archive)}>
              📋 View Structure
            </button>
            <button class="assign-btn" on:click={() => openAssignModal(archive)}>Assign User</button>
          </div>
        </div>
      {:else}
        <div class="empty-state">
          <p>No archives found. Create your first archive to get started!</p>
        </div>
      {/each}
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
    max-width: 1200px;
    margin: 0 auto;
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
  }

  .add-archive-btn {
    background: #3b82f6;
    color: white;
    padding: 0.5rem 1.5rem;
    border-radius: 0.25rem;
    text-decoration: none;
    font-weight: 500;
    transition: background 0.2s;
  }

  .add-archive-btn:hover {
    background: #2563eb;
  }

  .error {
    background: #fee;
    border: 1px solid #fcc;
    padding: 1rem;
    border-radius: 0.25rem;
    color: #c00;
    margin-bottom: 1rem;
  }

  .loading {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 200px;
  }

  .spinner {
    width: 40px;
    height: 40px;
    border: 4px solid #e2e8f0;
    border-top-color: #3b82f6;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  .archives-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .archive-card {
    background: white;
    padding: 1.5rem;
    border-radius: 0.5rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .archive-header {
    margin-bottom: 1rem;
  }

  .archive-header h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
  }

  .badges {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .badge {
    padding: 0.25rem 0.75rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
  }

  .badge.active {
    background: #dcfce7;
    color: #166534;
  }

  .badge.draft {
    background: #f3f4f6;
    color: #6b7280;
  }

  .badge.archived {
    background: #fef3c7;
    color: #92400e;
  }

  .badge.pending {
    background: #dbeafe;
    color: #1e40af;
  }

  .standard-badge {
    background: #e0e7ff;
    color: #4338ca;
  }

  .description {
    color: #64748b;
    margin: 0.75rem 0;
    font-size: 0.875rem;
    line-height: 1.4;
  }

  .content-preview {
    background: #f8fafc;
    padding: 0.75rem;
    border-radius: 0.25rem;
    font-size: 0.875rem;
    color: #475569;
    margin: 0.75rem 0;
    font-family: 'Monaco', 'Menlo', monospace;
    line-height: 1.5;
  }

  .archive-details {
    margin: 1rem 0;
    padding: 1rem;
    background: #f8fafc;
    border-radius: 0.25rem;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.5rem;
    font-size: 0.875rem;
  }

  .detail-item {
    display: flex;
    flex-direction: column;
  }

  .detail-item .label {
    color: #64748b;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .detail-item .value {
    color: #1e293b;
    font-weight: 500;
    margin-top: 0.25rem;
  }

  .assigned-users {
    margin: 1rem 0;
    padding: 0.75rem;
    background: #f8fafc;
    border-radius: 0.25rem;
  }

  .user-assignment {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem;
    margin-bottom: 0.25rem;
    background: white;
    border-radius: 0.25rem;
    font-size: 0.875rem;
  }

  .user-assignment:last-child {
    margin-bottom: 0;
  }

  .user-name {
    color: #1e293b;
    font-weight: 500;
  }

  .role-badge {
    padding: 0.125rem 0.5rem;
    border-radius: 0.25rem;
    font-size: 0.75rem;
    font-weight: 600;
    background: #dbeafe;
    color: #1e40af;
  }

  .archive-id {
    font-size: 0.75rem;
    color: #9ca3af;
    margin-top: 1rem;
    font-family: monospace;
  }

  .archive-actions {
    margin-top: 1rem;
    display: flex;
    gap: 1rem;
  }

  .view-structure-btn {
    padding: 0.5rem 1rem;
    border-radius: 0.25rem;
    font-weight: 500;
    transition: background 0.2s;
    flex: 1;
    text-align: center;
    background: #3b82f6;
    color: white;
    border: none;
    cursor: pointer;
  }

  .view-structure-btn:hover {
    background: #2563eb;
  }

  .assign-btn {
    padding: 0.5rem 1rem;
    border-radius: 0.25rem;
    font-weight: 500;
    transition: background 0.2s;
    flex: 1;
    text-align: center;
    background: #4caf50;
    color: white;
    border: none;
    cursor: pointer;
  }

  .assign-btn:hover {
    background: #388e3c;
  }

  .empty-state {
    grid-column: 1 / -1;
    text-align: center;
    padding: 3rem;
    color: #64748b;
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

  @media (max-width: 768px) {
    .archives-grid {
      grid-template-columns: 1fr;
    }

    .archive-details {
      grid-template-columns: 1fr;
    }

    .modal {
      min-width: 300px;
      margin: 1rem;
    }

    .structure-modal {
      min-width: 300px;
    }
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
</style>
