<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { authHeaders, apiFetch, API_BASE } from '$lib/api';
  import { GET_ARCHIVES_BY_USER_ASSIGNMENT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';
  import EntityForm from '$lib/components/EntityForm.svelte';
  import { validateRequired } from '$lib/schemes';
  import {
    documentFormSteps,
    describeForms,
    suggestValues,
    sha256Hex,
    type DocumentContext,
    type DocumentFormStep
  } from '$lib/schemes/documentForms';
  import { graphqlToKey } from '$lib/standards';

  interface PageData {
    tenantId: string;
    userId: string;
  }

  export let data: PageData;

  let documents: any[] = [];
  let loading = true;
  let error: string | null = null;
  let currentRole = '';
  let currentUser: any = null;
  let hasAccess = false;

  // Upload modal state
  let showUploadModal = false;
  let uploadFile: File | null = null;
  let uploadTitle = '';
  let uploadArchiveId = '';
  let uploading = false;
  let uploadError: string | null = null;
  let archives: any[] = [];
  let fileInputEl: HTMLInputElement;
  let modalBodyEl: HTMLElement;

  // Wizard: step 0 is the document itself (file, title, archive); steps 1..n
  // are the forms the selected archive's standard requires for a document.
  let wizardStep = 0;
  let preparing = false;
  /** entity name → field name → value */
  let formValues: Record<string, Record<string, string>> = {};
  let showOptional: Record<string, boolean> = {};
  /** What formValues was last suggested for; see prepareForms. */
  let preparedFor = '';

  $: selectedArchive = archives.find((a: any) => a.id === uploadArchiveId) || null;
  $: selectedStandard = selectedArchive?.standard || '';
  $: formSteps = documentFormSteps(selectedStandard);
  $: stepTitles = ['Document', ...formSteps.map((s) => s.title)];
  $: currentFormStep = wizardStep > 0 ? formSteps[wizardStep - 1] : null;

  const standardLabel = (standard: string) => graphqlToKey[standard] ?? standard;

  onMount(async () => {
    const authState = get(auth);
    currentRole = authState.role;
    currentUser = authState.user;

    // Check access - USER can only access their own documents
    if (currentRole === 'USER') {
      if (!currentUser || currentUser.id.toString() !== data.userId) {
        hasAccess = false;
        loading = false;
        toasts.error('You can only access your own documents');
        goto('/');
        return;
      }
    } else if (currentRole !== 'ADMIN' && currentRole !== 'TENANT') {
      // Non-authenticated or invalid role
      hasAccess = false;
      loading = false;
      goto('/login');
      return;
    }

    hasAccess = true;
    await Promise.all([loadDocuments(), loadArchives()]);
  });

  async function loadDocuments() {
    try {
      loading = true;

      // Construct query parameters
      const params = new URLSearchParams();
      params.append('role', currentRole);
      params.append('userId', data.userId);
      params.append('tenantId', data.tenantId);

      const response = await fetch(`${API_BASE}/api/documents?${params.toString()}`, {
        headers: { ...authHeaders() }
      });

      if (!response.ok) {
        throw new Error('Failed to load documents');
      }

      const result = await response.json();
      if (result.success) {
        documents = result.documents || [];
      } else {
        throw new Error(result.message || 'Failed to load documents');
      }

      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Failed to load documents';
      console.error('Load documents error:', e);
      toasts.error('Failed to load documents');
    } finally {
      loading = false;
    }
  }

  // Only archives this user owns or has been assigned to, and only within this
  // tenant: the upload is stored and billed under the tenant in the URL, so an
  // archive the user holds in another tenant must not be offered here.
  async function loadArchives() {
    try {
      const result = await client.query({
        query: GET_ARCHIVES_BY_USER_ASSIGNMENT,
        variables: { userId: data.userId },
        fetchPolicy: 'network-only'
      });
      archives = (result?.data?.getArchivesByUserAssignment || []).filter(
        (a: any) => String(a.tenantId) === String(data.tenantId)
      );
    } catch (e) {
      console.error('Failed to load archives:', e);
    }
  }

  function resetUpload() {
    uploadFile = null;
    uploadTitle = '';
    uploadArchiveId = '';
    uploadError = null;
    wizardStep = 0;
    formValues = {};
    showOptional = {};
    preparedFor = '';
    preparing = false;
    uploading = false;
  }

  function openUploadModal() {
    resetUpload();
    showUploadModal = true;
  }

  function closeUploadModal() {
    showUploadModal = false;
    resetUpload();
  }

  function handleFileSelect(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      uploadFile = target.files[0];
      if (!uploadTitle) {
        uploadTitle = uploadFile.name;
      }
    }
  }

  function handleDrop(event: DragEvent) {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      uploadFile = files[0];
      if (!uploadTitle) {
        uploadTitle = uploadFile.name;
      }
    }
  }

  function handleDragOver(event: DragEvent) {
    event.preventDefault();
  }

  function removeFile() {
    uploadFile = null;
    if (fileInputEl) fileInputEl.value = '';
  }

  function validateDocumentStep(): string | null {
    if (!uploadFile) return 'Please select a file';
    if (!uploadTitle.trim()) return 'Please enter a title';
    if (!uploadArchiveId) return 'Please select an archive to add this document to';
    return null;
  }

  function validateFormStep(step: DocumentFormStep): string | null {
    for (const entity of step.entities) {
      const problem = validateRequired(entity, formValues[entity.name] ?? {});
      if (problem) return step.entities.length > 1 ? `${entity.name}: ${problem}` : problem;
    }
    return null;
  }

  /**
   * Start the forms from what is already known (identifiers, dates, the
   * uploader, the file's name/size/format/checksum). Redone only when the
   * file, title or archive changed, so going Back keeps what was typed.
   */
  async function prepareForms() {
    if (!uploadFile) return;
    const key = [uploadArchiveId, uploadFile.name, uploadFile.size, uploadFile.lastModified, uploadTitle.trim()].join('|');
    if (key === preparedFor) return;
    preparing = true;
    try {
      const now = new Date();
      const ctx: DocumentContext = {
        id: `DOC-${now.getTime().toString(36).toUpperCase()}`,
        today: now.toISOString().split('T')[0],
        now: now.toISOString(),
        title: uploadTitle.trim(),
        userName: $auth.user?.name || $auth.user?.email || '',
        fileName: uploadFile.name,
        fileSize: uploadFile.size,
        format: uploadFile.type || uploadFile.name.split('.').pop() || '',
        sha256: await sha256Hex(uploadFile)
      };
      const values: Record<string, Record<string, string>> = {};
      for (const entity of formSteps.flatMap((s) => s.entities)) {
        values[entity.name] = suggestValues(entity, ctx);
      }
      formValues = values;
      showOptional = {};
      preparedFor = key;
    } finally {
      preparing = false;
    }
  }

  function goToStep(step: number) {
    uploadError = null;
    wizardStep = step;
    if (modalBodyEl) modalBodyEl.scrollTop = 0;
  }

  async function nextStep() {
    const problem = wizardStep === 0 ? validateDocumentStep() : validateFormStep(formSteps[wizardStep - 1]);
    if (problem) {
      uploadError = problem;
      return;
    }
    if (wizardStep === 0) await prepareForms();
    goToStep(wizardStep + 1);
  }

  /** Last step: check it, then upload. */
  async function finish() {
    const problem =
      validateDocumentStep() ?? (wizardStep > 0 ? validateFormStep(formSteps[wizardStep - 1]) : null);
    if (problem) {
      uploadError = problem;
      return;
    }
    await handleUpload();
  }

  async function handleUpload() {
    if (!uploadFile) return;

    uploading = true;
    uploadError = null;

    try {
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('userId', data.userId);
      formData.append('tenantId', data.tenantId);
      formData.append('title', uploadTitle.trim());

      // A readable copy of the forms stays with the document as its description;
      // the forms themselves are stored in the archive's tree (see below).
      const description = describeForms(standardLabel(selectedStandard), formSteps, formValues);
      if (description) {
        formData.append('description', description);
      }

      const uploadToken = typeof window !== 'undefined' ? localStorage.getItem('auth_token') : null;
      const response = await fetch(`${API_BASE}/api/documents/upload`, {
        method: 'POST',
        headers: { ...authHeaders(), ...(uploadToken ? { Authorization: uploadToken } : {}) },
        body: formData
      });

      const result = await response.json();

      if (!result.success) {
        throw new Error(result.error || 'Upload failed');
      }

      // The upload endpoint has no archive parameter; a second call links the new
      // document and stores the filled-in forms as elements of the archive's tree.
      const archiveTitle = selectedArchive?.title || 'the archive';
      const forms = formSteps
        .flatMap((s) => s.entities)
        .map((entity) => ({ entity: entity.name, values: formValues[entity.name] ?? {} }));
      let linked = false;
      let linkError = '';
      try {
        const linkResponse = await apiFetch(
          `/api/documents/${result.document.id}/associate-archive?archiveId=${encodeURIComponent(uploadArchiveId)}`,
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ forms })
          }
        );
        linked = linkResponse.ok;
        if (!linked) {
          linkError = (await linkResponse.json().catch(() => null))?.error ?? '';
        }
      } catch {
        linked = false;
      }

      if (linked) {
        toasts.success(`Document added to ${archiveTitle}`);
      } else {
        toasts.error(
          `Document uploaded, but it could not be added to ${archiveTitle}${linkError ? `: ${linkError}` : ''}`
        );
      }
      closeUploadModal();
      await loadDocuments();
    } catch (e) {
      uploadError = e instanceof Error ? e.message : 'Failed to upload document';
    } finally {
      uploading = false;
    }
  }

  function formatFileSize(bytes: number): string {
    if (!bytes) return 'Unknown';
    const mb = bytes / (1024 * 1024);
    if (mb > 1) {
      return `${mb.toFixed(2)} MB`;
    }
    const kb = bytes / 1024;
    return `${kb.toFixed(2)} KB`;
  }

  function formatDate(dateString: string): string {
    if (!dateString) return 'Unknown';
    return new Date(dateString).toLocaleString();
  }

  function getFileIcon(contentType: string): string {
    if (!contentType) return '📎';
    if (contentType.includes('pdf')) return '📄';
    if (contentType.includes('image')) return '🖼️';
    if (contentType.includes('video')) return '🎥';
    if (contentType.includes('word') || contentType.includes('document')) return '📝';
    if (contentType.includes('spreadsheet') || contentType.includes('excel')) return '📊';
    return '📎';
  }
</script>

<svelte:head>
  <title>My Documents - Arcana</title>
</svelte:head>

<div class="user-documents-page">
  {#if !hasAccess && !loading}
    <!-- Access Denied -->
    <div class="access-denied">
      <div class="access-denied-icon">🚫</div>
      <h1>Access Denied</h1>
      <p>You don't have permission to access these documents.</p>
      <p class="redirect-message">Redirecting...</p>
    </div>
  {:else}
    <Breadcrumb
      context={{ tenantId: data.tenantId, userId: data.userId, userName: 'User' }}
      items={[{ label: 'Documents' }]}
    />
    <div class="page-header">
      <div>
        <h1>📄 My Documents</h1>
        <p class="subtitle">View your submitted documents</p>
      </div>
      <button class="btn-add" on:click={openUploadModal}>+ Add Document</button>
    </div>

    {#if loading}
      <div class="loading">
        <div class="spinner"></div>
        <p>Loading your documents...</p>
      </div>
    {:else if error}
      <div class="error">
        <p>❌ {error}</p>
        <button on:click={loadDocuments} class="btn-retry">Try Again</button>
      </div>
    {:else}
      <div class="documents-section">
        <div class="section-header">
          <h2>Documents</h2>
          <span class="document-count">
            {documents.length} document{documents.length !== 1 ? 's' : ''}
          </span>
        </div>

        {#if documents.length === 0}
          <div class="empty-state">
            <span class="empty-icon">📭</span>
            <h3>No documents yet</h3>
            <p>You haven't uploaded any documents</p>
          </div>
        {:else}
          <div class="documents-grid">
            {#each documents as document}
              <a href="/tenants/{data.tenantId}/users/{data.userId}/documents/{document.id}" class="document-card-link">
                <div class="document-card">
                  <div class="document-icon-large">
                    {getFileIcon(document.contentType)}
                  </div>
                  <div class="document-info">
                    <h3 class="document-title">{document.title}</h3>
                    {#if document.description}
                      <p class="document-description">{document.description}</p>
                    {/if}
                    <div class="document-meta">
                      <div class="meta-item">
                        <span class="meta-label">File:</span>
                        <span class="meta-value">{document.fileName}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Size:</span>
                        <span class="meta-value">{formatFileSize(document.fileSize)}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Type:</span>
                        <span class="meta-value">{document.contentType || 'Unknown'}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Uploaded:</span>
                        <span class="meta-value">{formatDate(document.uploadedAt)}</span>
                      </div>
                      <div class="meta-item">
                        <span class="meta-label">Status:</span>
                        <span class="status badge status-{document.status?.toLowerCase() || 'unknown'}">
                          {document.status || 'Unknown'}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              </a>
            {/each}
          </div>
        {/if}
      </div>
    {/if}
  {/if}
</div>

<!-- Add Document wizard: the document itself, then the forms its archive's standard requires -->
{#if showUploadModal}
  <div class="modal-overlay" on:click={closeUploadModal} role="dialog" aria-modal="true" aria-labelledby="uploadModalTitle">
    <div class="modal-content modal" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3 id="uploadModalTitle">📄 Add Document</h3>
        <button class="modal-close" on:click={closeUploadModal} aria-label="Close">×</button>
      </div>

      {#if formSteps.length > 0}
        <ol class="wizard-steps" aria-label="Steps">
          {#each stepTitles as title, i}
            <li>
              <button
                type="button"
                class="wizard-step"
                class:current={i === wizardStep}
                class:done={i < wizardStep}
                aria-current={i === wizardStep ? 'step' : undefined}
                disabled={uploading || preparing || i >= wizardStep}
                on:click={() => goToStep(i)}
              >
                <span class="step-index">{i < wizardStep ? '✓' : i + 1}</span>
                {title}
              </button>
            </li>
          {/each}
        </ol>
      {/if}

      <div class="modal-body" bind:this={modalBodyEl}>
        {#if uploadError}
          <div class="alert alert-error" role="alert">
            <span>⚠️ {uploadError}</span>
          </div>
        {/if}

        {#if wizardStep === 0}
          <!-- File Drop Zone -->
          <div
            class="drop-zone"
            class:has-file={uploadFile}
            on:drop={handleDrop}
            on:dragover={handleDragOver}
            role="button"
            tabindex="0"
            on:click={() => fileInputEl.click()}
            on:keydown={(e) => e.key === 'Enter' && fileInputEl.click()}
          >
            {#if uploadFile}
              <div class="file-preview">
                <span class="file-icon">{getFileIcon(uploadFile.type)}</span>
                <div class="file-details">
                  <span class="file-name">{uploadFile.name}</span>
                  <span class="file-size">{formatFileSize(uploadFile.size)}</span>
                </div>
                <button class="btn-remove-file" on:click|stopPropagation={removeFile} aria-label="Remove file">×</button>
              </div>
            {:else}
              <div class="drop-zone-content">
                <span class="drop-icon">📁</span>
                <p class="drop-text">Drop file here or click to browse</p>
                <p class="drop-hint">Max 50MB</p>
              </div>
            {/if}
          </div>
          <input
            type="file"
            bind:this={fileInputEl}
            on:change={handleFileSelect}
            style="display: none"
          />

          <!-- Title -->
          <div class="form-group">
            <label for="uploadTitle">Title <span class="required-marker">*</span></label>
            <input
              type="text"
              id="uploadTitle"
              bind:value={uploadTitle}
              placeholder="Document title"
              disabled={uploading}
            />
          </div>

          <!-- Archive selector (required): the archives this user owns or is assigned to -->
          <div class="form-group">
            <label for="uploadArchive">Archive <span class="required-marker">*</span></label>
            <select id="uploadArchive" bind:value={uploadArchiveId} disabled={uploading || archives.length === 0} required>
              <option value="">{archives.length === 0 ? 'No archives available' : 'Select an archive…'}</option>
              {#each archives as archive (archive.id)}
                <option value={archive.id}>{archive.title} ({standardLabel(archive.standard)})</option>
              {/each}
            </select>
            {#if archives.length === 0}
              <p class="helper-text">
                {#if $auth.role === 'USER'}
                  You haven't been assigned to an archive yet. Ask your tenant manager to assign you to one.
                {:else}
                  This user isn't assigned to any archive yet.
                  <a href="/tenants/{data.tenantId}/archives">Assign them from an archive's page</a>.
                {/if}
              </p>
            {/if}
          </div>

          {#if formSteps.length > 0}
            <p class="forms-hint">
              {standardLabel(selectedStandard)} asks for {formSteps.length} form{formSteps.length === 1 ? '' : 's'}
              with each document: {formSteps.map((s) => s.title).join(', ')}. What the file already tells us is filled in for you.
            </p>
          {/if}
        {:else if currentFormStep}
          <p class="step-intro">
            Required by {standardLabel(selectedStandard)} for “{uploadTitle.trim()}” in {selectedArchive?.title}.
          </p>
          {#each currentFormStep.entities as entity (entity.name)}
            <section class="entity-block">
              {#if currentFormStep.entities.length > 1}
                <h4 class="entity-title">{entity.name}</h4>
              {/if}
              <EntityForm
                {entity}
                bind:values={formValues[entity.name]}
                bind:showOptional={showOptional[entity.name]}
                idPrefix={`doc-${wizardStep}-${entity.name.replace(/\W+/g, '-')}`}
                nativeRequired={false}
                disabled={uploading}
              />
            </section>
          {/each}
        {/if}
      </div>

      <div class="modal-footer">
        <button class="btn-secondary" on:click={closeUploadModal} disabled={uploading}>Cancel</button>
        <span class="footer-spacer"></span>
        {#if wizardStep > 0}
          <button class="btn-secondary" on:click={() => goToStep(wizardStep - 1)} disabled={uploading}>← Back</button>
        {/if}
        {#if wizardStep < formSteps.length}
          <button
            class="btn-primary"
            on:click={nextStep}
            disabled={preparing || !uploadFile || !uploadTitle.trim() || !uploadArchiveId}
          >
            {preparing ? 'Reading file…' : `Continue to ${formSteps[wizardStep].title} →`}
          </button>
        {:else}
          <button
            class="btn-primary"
            on:click={finish}
            disabled={uploading || !uploadFile || !uploadTitle.trim() || !uploadArchiveId}
          >
            {uploading ? 'Uploading...' : '📤 Upload'}
          </button>
        {/if}
      </div>
    </div>
  </div>
{/if}

<style>
  .user-documents-page {
    max-width: 1400px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 2rem;
    gap: 1rem;
  }

  .page-header h1 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  .subtitle {
    margin: 0;
    color: var(--arc-muted, #64748b);
    font-size: 1rem;
  }

  /* .btn-add inherits the global brand-gradient button styling from app.css */
  .btn-add {
    white-space: nowrap;
  }

  /* Access Denied */
  .access-denied {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    text-align: center;
    padding: 3rem;
  }

  .access-denied-icon {
    font-size: 5rem;
    margin-bottom: 1.5rem;
  }

  .access-denied h1 {
    margin: 0 0 1rem 0;
    color: var(--arc-ink, #0f172a);
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: var(--arc-muted, #64748b);
    font-size: 1.125rem;
  }

  .redirect-message {
    color: var(--arc-indigo, #6366f1);
    font-weight: 500;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
  }

  /* Loading */
  .loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    gap: 1rem;
  }

  /* .spinner comes from the global loading pattern in app.css */

  /* Error — panel colors come from the global .error kit; only layout is local */
  .error {
    padding: 2rem;
    text-align: center;
  }

  .error p {
    margin: 0 0 1rem 0;
    font-size: 1.125rem;
  }

  /* .btn-retry inherits the global brand-gradient button styling from app.css */

  /* Documents Section */
  .documents-section {
    background: var(--arc-card, #fff);
    padding: 2rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .section-header h2 {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.5rem;
  }

  .document-count {
    color: var(--arc-muted, #64748b);
    font-weight: 600;
    font-size: 0.875rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  /* Empty State */
  .empty-state {
    text-align: center;
    padding: 4rem 2rem;
  }

  .empty-icon {
    font-size: 5rem;
    display: block;
    margin-bottom: 1rem;
  }

  .empty-state h3 {
    margin: 0 0 0.5rem 0;
    color: var(--arc-ink, #0f172a);
  }

  .empty-state p {
    margin: 0;
    color: var(--arc-muted, #64748b);
  }

  /* Documents Grid */
  .documents-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .document-card-link {
    text-decoration: none;
    color: inherit;
    display: block;
  }

  .document-card {
    background: var(--arc-card, #fff);
    padding: 1.5rem;
    border-radius: 1rem;
    border: 1px solid var(--arc-line, #e8edf3);
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
  }

  .document-card:hover {
    transform: translateY(-4px);
    border-color: var(--arc-hover-border, #c7d2fe);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
  }

  @media (prefers-reduced-motion: reduce) {
    .document-card {
      transition: none;
    }
    .document-card:hover {
      transform: none;
    }
  }

  .document-icon-large {
    font-size: 3rem;
    text-align: center;
    margin-bottom: 1rem;
  }

  .document-info {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .document-title {
    margin: 0;
    color: var(--arc-ink, #0f172a);
    font-size: 1.125rem;
    font-weight: 600;
  }

  .document-description {
    /* The description can hold a document's standard forms; show the start only. */
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    white-space: pre-line;
    margin: 0;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    line-height: 1.5;
  }

  .document-meta {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    margin-top: 0.5rem;
  }

  .meta-item {
    display: flex;
    justify-content: space-between;
    font-size: 0.875rem;
  }

  .meta-label {
    color: var(--arc-muted, #64748b);
    font-weight: 500;
  }

  .meta-value {
    color: var(--arc-ink, #1e293b);
    font-weight: 400;
  }

  /* .status pill chrome comes from the global .badge kit; the hues below cover
     document statuses the global kit doesn't define. */
  .status-active,
  .status-approved {
    background: var(--arc-chip-green-bg, #dcfce7);
    color: var(--arc-chip-green-ink, #166534);
  }

  .status-pending,
  .status-pending_review {
    background: var(--arc-chip-amber-bg, #fef3c7);
    color: var(--arc-chip-amber-ink, #92400e);
  }

  .status-rejected {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #991b1b);
  }

  .status-unknown {
    background: var(--arc-chip-slate-bg, #f1f5f9);
    color: var(--arc-chip-slate-ink, #64748b);
  }

  /* Modal — surface comes from the global .modal-overlay / .modal kit in app.css */
  .modal-content {
    padding: 0;
    max-width: 680px;
    width: 92%;
    max-height: 90vh;
    display: flex;
    flex-direction: column;
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
  }

  .modal-header h3 { margin: 0; color: var(--arc-ink, #0f172a); }

  .modal-close {
    background: none;
    border: none;
    box-shadow: none;
    padding: 0;
    font-size: 1.5rem;
    color: var(--arc-muted, #64748b);
    cursor: pointer;
  }

  .modal-close:hover { color: var(--arc-ink, #0f172a); transform: none; box-shadow: none; }

  .modal-body { padding: 1.5rem; overflow-y: auto; }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    padding: 1.5rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  .alert {
    padding: 0.75rem 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
  }

  .alert-error {
    background: var(--arc-alert-red-bg, #fee2e2);
    color: var(--arc-alert-red-ink, #991b1b);
    border: 1px solid var(--arc-alert-red-border, #fca5a5);
  }

  /* Drop Zone */
  .drop-zone {
    border: 2px dashed var(--arc-line-strong, #cbd5e1);
    border-radius: 0.75rem;
    padding: 2rem;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    margin-bottom: 1.25rem;
    background: var(--arc-card-2, #f8fafc);
  }

  .drop-zone:hover {
    border-color: var(--arc-violet, #8b5cf6);
    background: var(--arc-chip-violet-bg, #faf5ff);
  }

  .drop-zone.has-file {
    border-color: var(--arc-violet, #8b5cf6);
    border-style: solid;
    background: var(--arc-chip-violet-bg, #faf5ff);
    padding: 1rem;
  }

  .drop-zone-content { pointer-events: none; }

  .drop-icon {
    font-size: 2.5rem;
    display: block;
    margin-bottom: 0.5rem;
  }

  .drop-text {
    margin: 0 0 0.25rem 0;
    color: var(--arc-body, #475569);
    font-weight: 500;
  }

  .drop-hint {
    margin: 0;
    color: var(--arc-faint, #94a3b8);
    font-size: 0.8rem;
  }

  .file-preview {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .file-icon { font-size: 2rem; }

  .file-details {
    display: flex;
    flex-direction: column;
    flex: 1;
    text-align: left;
  }

  .file-name {
    font-weight: 600;
    color: var(--arc-ink, #1e293b);
    font-size: 0.9rem;
    word-break: break-all;
  }

  .file-size {
    color: var(--arc-muted, #64748b);
    font-size: 0.8rem;
  }

  .btn-remove-file {
    background: var(--arc-chip-red-bg, #fee2e2);
    color: var(--arc-chip-red-ink, #991b1b);
    border: none;
    box-shadow: none;
    padding: 0;
    border-radius: 50%;
    width: 28px;
    height: 28px;
    font-size: 1.1rem;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s;
  }

  .btn-remove-file:hover { background: var(--arc-chip-red-hover, #fca5a5); transform: none; box-shadow: none; }

  /* Form */
  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: var(--arc-ink, #0f172a);
    font-weight: 600;
    font-size: 0.9rem;
  }


  .required-marker {
    color: #ef4444;
    font-weight: 700;
  }

  .helper-text {
    margin: 0.5rem 0 0;
    font-size: 0.8rem;
    color: var(--arc-faint, #94a3b8);
  }

  .helper-text a {
    color: var(--arc-link, #4f46e5);
    text-decoration: none;
  }

  .helper-text a:hover {
    text-decoration: underline;
  }

  /* inputs and selects inherit the global Arcana input styling from app.css */

  .form-group input:disabled,
  .form-group select:disabled {
    background: var(--arc-card-2, #f1f5f9);
    cursor: not-allowed;
  }

  /* Wizard */
  .wizard-steps {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem 0.5rem;
    list-style: none;
    margin: 0;
    padding: 0.75rem 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
    background: var(--arc-card-2, #f8fafc);
  }

  .wizard-step {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.25rem 0.5rem 0.25rem 0.25rem;
    background: none;
    border: none;
    box-shadow: none;
    border-radius: 999px;
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--arc-muted, #64748b);
    cursor: pointer;
  }

  .wizard-step:hover:not(:disabled) {
    background: var(--arc-chip-slate-bg, #f1f5f9);
    transform: none;
    box-shadow: none;
  }

  .wizard-step:disabled {
    cursor: default;
    opacity: 1;
  }

  .wizard-step .step-index {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 1.4rem;
    height: 1.4rem;
    border-radius: 50%;
    font-size: 0.72rem;
    background: var(--arc-chip-slate-bg, #e2e8f0);
    color: var(--arc-body, #475569);
  }

  .wizard-step.done .step-index {
    background: var(--arc-chip-green-bg, #dcfce7);
    color: var(--arc-chip-green-ink, #166534);
  }

  .wizard-step.current {
    color: var(--arc-ink, #0f172a);
  }

  .wizard-step.current .step-index {
    background: var(--arc-indigo, #6366f1);
    color: #fff;
  }

  .forms-hint {
    margin: 1rem 0 0;
    padding: 0.75rem 1rem;
    border-radius: 0.5rem;
    font-size: 0.85rem;
    color: var(--arc-chip-indigo-ink, #4338ca);
    background: var(--arc-chip-indigo-bg, #e0e7ff);
  }

  .step-intro {
    margin: 0 0 1rem;
    font-size: 0.85rem;
    color: var(--arc-muted, #64748b);
  }

  .entity-block + .entity-block {
    margin-top: 1.25rem;
    padding-top: 1.25rem;
    border-top: 1px solid var(--arc-line, #e8edf3);
  }

  .entity-title {
    margin: 0 0 0.5rem;
    font-size: 1rem;
    color: var(--arc-ink, #0f172a);
  }

  .footer-spacer {
    flex: 1;
  }

  /* .btn-primary and .btn-secondary come from the global button kit in app.css */

  @media (max-width: 768px) {
    .user-documents-page {
      padding: 1rem;
    }

    .page-header {
      flex-direction: column;
    }

    .documents-grid {
      grid-template-columns: 1fr;
    }
  }
</style>

