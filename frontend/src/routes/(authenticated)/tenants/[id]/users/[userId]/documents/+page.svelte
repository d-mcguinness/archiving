<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { authHeaders, API_BASE } from '$lib/api';
  import { GET_INTAKES_BY_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';
  import { auth } from '$lib/stores/authStore';
  import Breadcrumb from '$lib/components/Breadcrumb.svelte';

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
  let uploadIntakeId = '';
  let uploading = false;
  let uploadError: string | null = null;
  let sips: any[] = [];
  let fileInputEl: HTMLInputElement;
  let showMetadata = false;
  let metadata: Record<string, string> = {};

  // Standard-specific optional metadata field definitions
  type MetaField = { key: string; label: string; type: 'text' | 'select' | 'date' | 'textarea'; options?: string[]; placeholder?: string };

  const standardFields: Record<string, MetaField[]> = {
    NOARK5: [
      { key: 'dokumentmedium', label: 'Document Medium', type: 'select', options: ['Elektronisk arkiv', 'Fysisk arkiv', 'Blandet arkiv'] },
      { key: 'opprettetAv', label: 'Created By (opprettetAv)', type: 'text', placeholder: 'Name of creator' },
      { key: 'opprettetDato', label: 'Created Date (opprettetDato)', type: 'date' },
      { key: 'beskrivelse', label: 'Description (beskrivelse)', type: 'textarea', placeholder: 'Description of the document' },
      { key: 'kassasjon', label: 'Disposal (kassasjon)', type: 'select', options: ['Bevares', 'Kasseres', 'Vurderes senere'] },
      { key: 'skjerming', label: 'Screening (skjerming)', type: 'select', options: ['Ingen', 'Unntatt offentlighet', 'Strengt fortrolig'] },
    ],
    OAIS: [
      { key: 'producer', label: 'Producer', type: 'text', placeholder: 'Submitting organization or person' },
      { key: 'contentInformationType', label: 'Content Information Type', type: 'select', options: ['Simple', 'Complex', 'Physical', 'Digital'] },
      { key: 'preservationLevel', label: 'Preservation Level', type: 'select', options: ['Bit-level', 'Full', 'Logical'] },
      { key: 'accessRights', label: 'Access Rights', type: 'select', options: ['Open', 'Restricted', 'Closed', 'Embargoed'] },
      { key: 'submissionAgreementRef', label: 'Submission Agreement Ref', type: 'text', placeholder: 'Agreement reference ID' },
      { key: 'checksumAlgorithm', label: 'Checksum Algorithm', type: 'select', options: ['MD5', 'SHA-1', 'SHA-256', 'SHA-512'] },
    ],
    PREMIS: [
      { key: 'objectCategory', label: 'Object Category', type: 'select', options: ['file', 'bitstream', 'representation', 'intellectualEntity'] },
      { key: 'preservationLevel', label: 'Preservation Level', type: 'select', options: ['Bit-level', 'Full preservation', 'Logical preservation'] },
      { key: 'originalName', label: 'Original Name', type: 'text', placeholder: 'Original filename' },
      { key: 'messageDigestAlgorithm', label: 'Checksum Algorithm', type: 'select', options: ['MD5', 'SHA-1', 'SHA-256', 'SHA-512'] },
      { key: 'significantProperties', label: 'Significant Properties', type: 'text', placeholder: 'Properties to preserve' },
      { key: 'creatingApplicationName', label: 'Creating Application', type: 'text', placeholder: 'Software used to create' },
    ],
    DUBLIN_CORE: [
      { key: 'creator', label: 'Creator (dc:creator)', type: 'text', placeholder: 'Author or organization' },
      { key: 'subject', label: 'Subject (dc:subject)', type: 'text', placeholder: 'Topic keywords' },
      { key: 'description', label: 'Description (dc:description)', type: 'textarea', placeholder: 'Brief description' },
      { key: 'date', label: 'Date (dc:date)', type: 'date' },
      { key: 'type', label: 'Type (dc:type)', type: 'select', options: ['Text', 'Image', 'Dataset', 'Collection', 'Sound', 'MovingImage', 'Software', 'Event'] },
      { key: 'language', label: 'Language (dc:language)', type: 'text', placeholder: 'e.g. en, no, de' },
      { key: 'rights', label: 'Rights (dc:rights)', type: 'text', placeholder: 'Copyright or license' },
      { key: 'publisher', label: 'Publisher (dc:publisher)', type: 'text', placeholder: 'Publishing entity' },
    ],
    METS: [
      { key: 'createDate', label: 'Creation Date', type: 'date' },
      { key: 'recordStatus', label: 'Record Status', type: 'select', options: ['New', 'Revised', 'Deleted'] },
      { key: 'agentRole', label: 'Agent Role', type: 'select', options: ['CREATOR', 'EDITOR', 'ARCHIVIST', 'PRESERVATION', 'DISSEMINATOR', 'CUSTODIAN'] },
      { key: 'agentName', label: 'Agent Name', type: 'text', placeholder: 'Person or organization' },
      { key: 'label', label: 'Label', type: 'text', placeholder: 'Display label for package' },
      { key: 'profile', label: 'METS Profile', type: 'text', placeholder: 'Profile URI' },
    ],
    EAD: [
      { key: 'unitdate', label: 'Unit Date', type: 'text', placeholder: 'e.g. 1990-2020' },
      { key: 'origination', label: 'Origination (Creator)', type: 'text', placeholder: 'Creator of the materials' },
      { key: 'scopecontent', label: 'Scope and Content', type: 'textarea', placeholder: 'Summary of content and scope' },
      { key: 'physdesc', label: 'Physical Description', type: 'text', placeholder: 'e.g. 3 boxes, 1.5 linear feet' },
      { key: 'accessrestrict', label: 'Access Restrictions', type: 'text', placeholder: 'Access conditions' },
      { key: 'arrangement', label: 'Arrangement', type: 'text', placeholder: 'Organization of materials' },
    ],
    BAGIT: [
      { key: 'sourceOrganization', label: 'Source Organization', type: 'text', placeholder: 'Organization creating the bag' },
      { key: 'contactName', label: 'Contact Name', type: 'text', placeholder: 'Contact person' },
      { key: 'contactEmail', label: 'Contact Email', type: 'text', placeholder: 'Contact email address' },
      { key: 'externalDescription', label: 'External Description', type: 'textarea', placeholder: 'Brief description of the bag' },
      { key: 'baggingDate', label: 'Bagging Date', type: 'date' },
      { key: 'bagGroupIdentifier', label: 'Bag Group Identifier', type: 'text', placeholder: 'Group ID if part of a set' },
    ],
    ISADG: [
      { key: 'referenceCode', label: 'Reference Code (3.1.1)', type: 'text', placeholder: 'e.g. NO/RA/PA-0001' },
      { key: 'dateExpression', label: 'Date Expression (3.1.3)', type: 'text', placeholder: 'e.g. 1950-1980' },
      { key: 'levelOfDescription', label: 'Level of Description (3.1.4)', type: 'select', options: ['Fonds', 'Sub-fonds', 'Series', 'Sub-series', 'File', 'Item'] },
      { key: 'extentAndMedium', label: 'Extent and Medium (3.1.5)', type: 'text', placeholder: 'e.g. 5 boxes, paper records' },
      { key: 'nameOfCreator', label: 'Name of Creator (3.2.1)', type: 'text', placeholder: 'Creator entity name' },
      { key: 'scopeAndContent', label: 'Scope and Content (3.3.1)', type: 'textarea', placeholder: 'Summary of scope and content' },
    ],
    MODS: [
      { key: 'creator', label: 'Creator Name', type: 'text', placeholder: 'Author or creator' },
      { key: 'typeOfResource', label: 'Type of Resource', type: 'select', options: ['text', 'cartographic', 'notated music', 'sound recording', 'still image', 'moving image', 'three dimensional object', 'software, multimedia', 'mixed material'] },
      { key: 'genre', label: 'Genre', type: 'text', placeholder: 'Specific genre or form' },
      { key: 'dateCreated', label: 'Date Created', type: 'date' },
      { key: 'publisher', label: 'Publisher', type: 'text', placeholder: 'Publishing entity' },
      { key: 'languageCode', label: 'Language Code (ISO 639)', type: 'text', placeholder: 'e.g. eng, nor' },
      { key: 'abstract', label: 'Abstract', type: 'textarea', placeholder: 'Summary of content' },
      { key: 'subject', label: 'Subject', type: 'text', placeholder: 'Subject terms' },
    ],
  };

  // Get the selected Intake's standard
  $: selectedIntake = sips.find((s: any) => s.id === uploadIntakeId) || null;
  $: selectedStandard = selectedIntake?.standard || '';
  $: currentFields = selectedStandard ? (standardFields[selectedStandard] || []) : [];

  // Reset metadata when Intake changes
  $: if (uploadIntakeId) {
    metadata = {};
    showMetadata = false;
  }

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
    await Promise.all([loadDocuments(), loadIntakes()]);
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

  async function loadIntakes() {
    try {
      const result = await client.query({
        query: GET_INTAKES_BY_TENANT,
        variables: { tenantId: data.tenantId },
        fetchPolicy: 'network-only'
      });
      sips = result?.data?.getIntakesByTenant || [];
    } catch (e) {
      console.error('Failed to load Intakes:', e);
    }
  }

  function openUploadModal() {
    uploadFile = null;
    uploadTitle = '';
    uploadIntakeId = '';
    uploadError = null;
    metadata = {};
    showMetadata = false;
    showUploadModal = true;
  }

  function closeUploadModal() {
    showUploadModal = false;
    uploadFile = null;
    uploadTitle = '';
    uploadIntakeId = '';
    uploadError = null;
    metadata = {};
    showMetadata = false;
    uploading = false;
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

  async function handleUpload() {
    if (!uploadFile) {
      uploadError = 'Please select a file';
      return;
    }
    if (!uploadTitle.trim()) {
      uploadError = 'Please enter a title';
      return;
    }
    if (!uploadIntakeId) {
      uploadError = 'Please select a Intake to link this document to';
      return;
    }

    uploading = true;
    uploadError = null;

    try {
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('userId', data.userId);
      formData.append('tenantId', data.tenantId);
      formData.append('title', uploadTitle.trim());
      formData.append('intakeId', uploadIntakeId);

      // Build description from standard metadata if any fields were filled
      const filledMeta = Object.entries(metadata).filter(([_, v]) => v && v.trim());
      if (filledMeta.length > 0 && selectedStandard) {
        const metaLines = filledMeta.map(([k, v]) => {
          const field = currentFields.find(f => f.key === k);
          return `${field?.label || k}: ${v}`;
        });
        const description = `[${selectedStandard} Metadata]\n${metaLines.join('\n')}`;
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

      toasts.success('Document uploaded successfully');
      closeUploadModal();
      await loadDocuments();
    } catch (e) {
      uploadError = e instanceof Error ? e.message : 'Failed to upload document';
    } finally {
      uploading = false;
    }
  }

  function fillRandomMetadata() {
    const m: Record<string, string> = {};
    for (const field of currentFields) {
      if (field.type === 'select' && field.options && field.options.length > 0) {
        m[field.key] = field.options[Math.floor(Math.random() * field.options.length)];
      } else if (field.type === 'date') {
        const d = new Date();
        d.setDate(d.getDate() - Math.floor(Math.random() * 365));
        m[field.key] = d.toISOString().split('T')[0];
      } else if (field.type === 'textarea') {
        const texts: Record<string, string[]> = {
          beskrivelse: ['Saksdokument for intern bruk', 'Korrespondanse med ekstern part', 'Offisielt vedtak fra styremøte'],
          scopecontent: ['Records relating to administrative operations', 'Correspondence and reports from field offices', 'Financial records and audit documentation'],
          scopeAndContent: ['Fonds comprises administrative records', 'Series of case files from 1990-2020', 'Collection of official correspondence'],
          externalDescription: ['Research dataset from field survey', 'Digitized historical photographs', 'Annual report collection'],
          abstract: ['A comprehensive analysis of the subject matter', 'Survey results and statistical findings', 'Historical overview and primary source compilation'],
          description: ['Resource covering organizational records', 'Collection of primary source materials', 'Digital preservation package contents'],
        };
        const pool = texts[field.key] || ['Sample metadata value for archival submission', 'Detailed description of document contents', 'Contextual information for preservation'];
        m[field.key] = pool[Math.floor(Math.random() * pool.length)];
      } else {
        const vals: Record<string, string[]> = {
          opprettetAv: ['Kari Nordmann', 'Ola Hansen', 'Erik Johansen'],
          producer: ['National Archives', 'University Library', 'Research Institute'],
          submissionAgreementRef: ['SA-2024-001', 'SA-2025-042', 'SA-2023-117'],
          originalName: ['report_final.pdf', 'scan_001.tiff', 'dataset_v2.csv'],
          significantProperties: ['Page count, image resolution', 'Text content, layout', 'Color depth, dimensions'],
          creatingApplicationName: ['Microsoft Word 365', 'Adobe Acrobat Pro', 'LibreOffice Writer'],
          creator: ['Jane Smith', 'Olav Berg', 'Maria Garcia'],
          subject: ['Public administration', 'Environmental policy', 'Cultural heritage'],
          language: ['en', 'no', 'de'],
          rights: ['CC BY 4.0', 'All rights reserved', 'Public Domain'],
          publisher: ['National Archives', 'University Press', 'Government Publishing'],
          agentName: ['Archives Division', 'Digital Preservation Unit', 'Records Management Office'],
          label: ['Annual Report Package', 'Correspondence Series', 'Research Data Collection'],
          profile: ['http://www.loc.gov/METS/profiles/00000001.xml'],
          unitdate: ['1995-2010', '2000-2023', '1980-1999'],
          origination: ['Department of Records', 'Office of the Director', 'Field Survey Unit'],
          physdesc: ['3 boxes, 1.5 linear feet', '2 folders, 45 pages', '1 box, 200 photographs'],
          accessrestrict: ['Open access', 'Restricted for 25 years', 'Permission required'],
          arrangement: ['Chronological', 'Alphabetical by subject', 'Original order maintained'],
          sourceOrganization: ['National Library', 'State Archives', 'Research Council'],
          contactName: ['Jan Eriksen', 'Anna Svensson', 'Peter Müller'],
          contactEmail: ['archive@example.org', 'records@institution.no', 'preservation@lib.edu'],
          bagGroupIdentifier: ['BAG-GRP-2024-A', 'BAG-GRP-2025-B', 'BAG-GRP-2023-C'],
          referenceCode: ['NO/RA/PA-0001', 'NO/SA/S-1234', 'NO/KA/A-0567'],
          dateExpression: ['1950-1980', '2001-2015', '1975-1999'],
          extentAndMedium: ['5 boxes, paper records', '3 volumes, bound manuscripts', '12 folders, mixed media'],
          nameOfCreator: ['Ministry of Culture', 'County Administration', 'Municipal Archives'],
          genre: ['report', 'correspondence', 'dataset'],
          languageCode: ['eng', 'nor', 'swe'],
          typeOfResource: ['text', 'still image', 'mixed material'],
          dateCreated: [], // handled by date type
        };
        const pool = vals[field.key] || [`Sample ${field.label}`];
        m[field.key] = pool[Math.floor(Math.random() * pool.length)];
      }
    }
    metadata = m;
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
  <title>My Documents - Archiving System</title>
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

<!-- Upload Document Modal -->
{#if showUploadModal}
  <div class="modal-overlay" on:click={closeUploadModal} role="dialog" aria-modal="true">
    <div class="modal-content modal" on:click|stopPropagation role="document">
      <div class="modal-header">
        <h3>📄 Add Document</h3>
        <button class="modal-close" on:click={closeUploadModal} aria-label="Close">×</button>
      </div>
      <div class="modal-body">
        {#if uploadError}
          <div class="alert alert-error">
            <span>⚠️ {uploadError}</span>
          </div>
        {/if}

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
          <label for="uploadTitle">Title *</label>
          <input
            type="text"
            id="uploadTitle"
            bind:value={uploadTitle}
            placeholder="Document title"
            disabled={uploading}
          />
        </div>

        <!-- Intake selector (required) -->
        <div class="form-group">
          <label for="uploadIntake">Link to Intake <span class="required-marker">*</span></label>
          <select id="uploadIntake" bind:value={uploadIntakeId} disabled={uploading} required>
            <option value="">Select a Intake...</option>
            {#each sips as sip}
              <option value={sip.id}>{sip.title} ({sip.standard})</option>
            {/each}
          </select>
          {#if sips.length === 0}
            <p class="helper-text">No Intakes available. <a href="/intake/create">Create a Intake</a> first.</p>
          {/if}
        </div>

        <!-- Standard-specific metadata (shown when a Intake is selected) -->
        {#if selectedStandard && currentFields.length > 0}
          <div class="metadata-section">
            <div class="metadata-header">
              <button
                type="button"
                class="metadata-toggle"
                on:click={() => showMetadata = !showMetadata}
              >
                <span class="toggle-icon">{showMetadata ? '▾' : '▸'}</span>
                <span>{selectedStandard} Metadata</span>
                <span class="optional">(optional)</span>
              </button>
              {#if showMetadata}
                <button type="button" class="btn-fill" on:click|stopPropagation={fillRandomMetadata}>Fill Random</button>
              {/if}
            </div>

            {#if showMetadata}
              <div class="metadata-fields">
                <p class="metadata-hint">Fill in these fields for a more complete {selectedStandard} submission.</p>
                {#each currentFields as field (field.key)}
                  <div class="form-group form-group-sm">
                    <label for="meta_{field.key}">{field.label}</label>
                    {#if field.type === 'select'}
                      <select id="meta_{field.key}" bind:value={metadata[field.key]} disabled={uploading}>
                        <option value="">—</option>
                        {#each field.options || [] as opt}
                          <option value={opt}>{opt}</option>
                        {/each}
                      </select>
                    {:else if field.type === 'date'}
                      <input type="date" id="meta_{field.key}" bind:value={metadata[field.key]} disabled={uploading} />
                    {:else if field.type === 'textarea'}
                      <textarea id="meta_{field.key}" bind:value={metadata[field.key]} placeholder={field.placeholder || ''} disabled={uploading} rows="2"></textarea>
                    {:else}
                      <input type="text" id="meta_{field.key}" bind:value={metadata[field.key]} placeholder={field.placeholder || ''} disabled={uploading} />
                    {/if}
                  </div>
                {/each}
              </div>
            {/if}
          </div>
        {/if}
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" on:click={closeUploadModal} disabled={uploading}>Cancel</button>
        <button class="btn-primary" on:click={handleUpload} disabled={uploading || !uploadFile || !uploadTitle.trim()}>
          {uploading ? 'Uploading...' : '📤 Upload'}
        </button>
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
    max-width: 550px;
    width: 90%;
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

  .modal-body { padding: 1.5rem; }

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

  .optional {
    color: var(--arc-faint, #94a3b8);
    font-weight: 400;
    font-size: 0.8rem;
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

  /* Metadata Section */
  .metadata-section {
    border: 1px solid var(--arc-line-strong, #e2e8f0);
    border-radius: 0.5rem;
    overflow: hidden;
  }

  .metadata-header {
    display: flex;
    align-items: center;
    background: var(--arc-card-2, #f8fafc);
  }

  .metadata-toggle {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex: 1;
    padding: 0.75rem 1rem;
    background: none;
    border: none;
    box-shadow: none;
    border-radius: 0;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.9rem;
    color: var(--arc-body, #475569);
    transition: background 0.2s;
    text-align: left;
  }

  .metadata-toggle:hover { background: var(--arc-chip-slate-bg, #f1f5f9); transform: none; box-shadow: none; }

  .btn-fill {
    padding: 0.3rem 0.65rem;
    margin-right: 0.75rem;
    background: var(--arc-chip-indigo-bg, #e0e7ff);
    color: var(--arc-chip-indigo-ink, #4338ca);
    border: 1px solid var(--arc-chip-indigo-hover, #c7d2fe);
    border-radius: 0.375rem;
    box-shadow: none;
    font-weight: 600;
    font-size: 0.75rem;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
  }

  .btn-fill:hover {
    background: var(--arc-chip-indigo-hover, #c7d2fe);
    border-color: var(--arc-hover-border, #a5b4fc);
    transform: none;
    box-shadow: none;
  }

  .toggle-icon {
    font-size: 0.75rem;
    color: var(--arc-faint, #94a3b8);
    width: 1rem;
    text-align: center;
  }

  .metadata-fields {
    padding: 1rem;
    border-top: 1px solid var(--arc-line-strong, #e2e8f0);
    background: var(--arc-card-2, #fafbfc);
  }

  .metadata-hint {
    margin: 0 0 1rem 0;
    font-size: 0.8rem;
    color: var(--arc-muted, #64748b);
  }

  .form-group-sm { margin-bottom: 0.75rem; }

  .form-group-sm label {
    font-size: 0.8rem;
    margin-bottom: 0.25rem;
  }

  .form-group-sm input,
  .form-group-sm select,
  .form-group-sm textarea {
    padding: 0.5rem 0.75rem;
    font-size: 0.875rem;
  }

  .form-group textarea {
    resize: vertical;
  }

  .form-group textarea:disabled {
    background: var(--arc-card-2, #f1f5f9);
    cursor: not-allowed;
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

