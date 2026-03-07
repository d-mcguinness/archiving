<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';
  import { client } from '$lib/apollo';
  import { GET_SIPS_BY_TENANT } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

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
  let uploadSipId = '';
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

  // Get the selected SIP's standard
  $: selectedSip = sips.find((s: any) => s.id === uploadSipId) || null;
  $: selectedStandard = selectedSip?.standard || '';
  $: currentFields = selectedStandard ? (standardFields[selectedStandard] || []) : [];

  // Reset metadata when SIP changes
  $: if (uploadSipId) {
    metadata = {};
    showMetadata = false;
  }

  onMount(async () => {
    // Check authentication
    const role = localStorage.getItem('auth_role');
    const user = localStorage.getItem('auth_user');
    currentRole = role || '';

    if (user) {
      try {
        currentUser = JSON.parse(user);
      } catch (e) {
        console.error('Error parsing user:', e);
      }
    }

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
    await Promise.all([loadDocuments(), loadSips()]);
  });

  async function loadDocuments() {
    try {
      loading = true;

      // Construct query parameters
      const params = new URLSearchParams();
      params.append('role', currentRole);
      params.append('userId', data.userId);
      params.append('tenantId', data.tenantId);

      const response = await fetch(`http://localhost:2020/api/documents?${params.toString()}`);

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

  async function loadSips() {
    try {
      const result = await client.query({
        query: GET_SIPS_BY_TENANT,
        variables: { tenantId: data.tenantId },
        fetchPolicy: 'network-only'
      });
      sips = result?.data?.getSipsByTenant || [];
    } catch (e) {
      console.error('Failed to load SIPs:', e);
    }
  }

  function openUploadModal() {
    uploadFile = null;
    uploadTitle = '';
    uploadSipId = '';
    uploadError = null;
    metadata = {};
    showMetadata = false;
    showUploadModal = true;
  }

  function closeUploadModal() {
    showUploadModal = false;
    uploadFile = null;
    uploadTitle = '';
    uploadSipId = '';
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

    uploading = true;
    uploadError = null;

    try {
      const formData = new FormData();
      formData.append('file', uploadFile);
      formData.append('userId', data.userId);
      formData.append('tenantId', data.tenantId);
      formData.append('title', uploadTitle.trim());

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

      const response = await fetch('http://localhost:2020/api/documents/upload', {
        method: 'POST',
        body: formData
      });

      const result = await response.json();

      if (!result.success) {
        throw new Error(result.error || 'Upload failed');
      }

      // If a SIP was selected, associate the document with it
      if (uploadSipId && result.document?.id) {
        const assocResponse = await fetch(
          `http://localhost:2020/api/documents/${result.document.id}/associate-archive?archiveId=${uploadSipId}`,
          { method: 'POST' }
        );
        const assocResult = await assocResponse.json();
        if (!assocResult.success) {
          console.warn('Document uploaded but failed to associate with SIP:', assocResult.error);
        }
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
                      <span class="status status-{document.status?.toLowerCase() || 'unknown'}">
                        {document.status || 'Unknown'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
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
    <div class="modal-content" on:click|stopPropagation role="document">
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

        <!-- SIP selector (optional) -->
        <div class="form-group">
          <label for="uploadSip">Link to SIP <span class="optional">(optional)</span></label>
          <select id="uploadSip" bind:value={uploadSipId} disabled={uploading}>
            <option value="">None</option>
            {#each sips as sip}
              <option value={sip.id}>{sip.title} ({sip.standard})</option>
            {/each}
          </select>
        </div>

        <!-- Standard-specific metadata (shown when a SIP is selected) -->
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
    color: #1e293b;
    font-size: 2rem;
  }

  .subtitle {
    margin: 0;
    color: #64748b;
    font-size: 1rem;
  }

  .btn-add {
    padding: 0.75rem 1.5rem;
    background: #8b5cf6;
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
  }

  .btn-add:hover {
    background: #7c3aed;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
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
    color: #1e293b;
    font-size: 2rem;
  }

  .access-denied p {
    margin: 0.5rem 0;
    color: #64748b;
    font-size: 1.125rem;
  }

  .redirect-message {
    color: #3b82f6;
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

  /* Error */
  .error {
    background: #fee;
    color: #c00;
    padding: 2rem;
    border-radius: 0.5rem;
    text-align: center;
    border: 1px solid #fcc;
  }

  .error p {
    margin: 0 0 1rem 0;
    font-size: 1.125rem;
  }

  .btn-retry {
    padding: 0.5rem 1rem;
    background: #3b82f6;
    color: white;
    border: none;
    border-radius: 0.375rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;
  }

  .btn-retry:hover {
    background: #2563eb;
  }

  /* Documents Section */
  .documents-section {
    background: white;
    padding: 2rem;
    border-radius: 0.75rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    border: 1px solid #e2e8f0;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
    padding-bottom: 1rem;
    border-bottom: 2px solid #e2e8f0;
  }

  .section-header h2 {
    margin: 0;
    color: #1e293b;
    font-size: 1.5rem;
  }

  .document-count {
    color: #64748b;
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
    color: #1e293b;
  }

  .empty-state p {
    margin: 0;
    color: #64748b;
  }

  /* Documents Grid */
  .documents-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
    gap: 1.5rem;
  }

  .document-card {
    background: #f8fafc;
    padding: 1.5rem;
    border-radius: 0.75rem;
    border: 2px solid #e2e8f0;
    transition: all 0.2s;
  }

  .document-card:hover {
    border-color: #3b82f6;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
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
    color: #1e293b;
    font-size: 1.125rem;
    font-weight: 600;
  }

  .document-description {
    margin: 0;
    color: #64748b;
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
    color: #64748b;
    font-weight: 500;
  }

  .meta-value {
    color: #1e293b;
    font-weight: 400;
  }

  .status {
    padding: 0.25rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .status-active,
  .status-approved {
    background: #dcfce7;
    color: #166534;
  }

  .status-pending,
  .status-pending_review {
    background: #fef3c7;
    color: #92400e;
  }

  .status-rejected {
    background: #fee2e2;
    color: #991b1b;
  }

  .status-unknown {
    background: #f1f5f9;
    color: #64748b;
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
    max-width: 550px;
    width: 90%;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  }

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .modal-header h3 { margin: 0; color: #1e293b; }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    color: #64748b;
    cursor: pointer;
  }

  .modal-close:hover { color: #1e293b; }

  .modal-body { padding: 1.5rem; }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    padding: 1.5rem;
    border-top: 1px solid #e2e8f0;
  }

  .alert {
    padding: 0.75rem 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
  }

  .alert-error {
    background: #fee2e2;
    color: #991b1b;
    border: 1px solid #fca5a5;
  }

  /* Drop Zone */
  .drop-zone {
    border: 2px dashed #cbd5e1;
    border-radius: 0.75rem;
    padding: 2rem;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    margin-bottom: 1.25rem;
    background: #f8fafc;
  }

  .drop-zone:hover {
    border-color: #8b5cf6;
    background: #faf5ff;
  }

  .drop-zone.has-file {
    border-color: #8b5cf6;
    border-style: solid;
    background: #faf5ff;
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
    color: #475569;
    font-weight: 500;
  }

  .drop-hint {
    margin: 0;
    color: #94a3b8;
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
    color: #1e293b;
    font-size: 0.9rem;
    word-break: break-all;
  }

  .file-size {
    color: #64748b;
    font-size: 0.8rem;
  }

  .btn-remove-file {
    background: #fee2e2;
    color: #991b1b;
    border: none;
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

  .btn-remove-file:hover { background: #fca5a5; }

  /* Form */
  .form-group { margin-bottom: 1rem; }

  .form-group label {
    display: block;
    margin-bottom: 0.5rem;
    color: #1e293b;
    font-weight: 600;
    font-size: 0.9rem;
  }

  .optional {
    color: #94a3b8;
    font-weight: 400;
    font-size: 0.8rem;
  }

  .form-group input,
  .form-group select {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-family: inherit;
  }

  .form-group input:focus,
  .form-group select:focus {
    outline: none;
    border-color: #8b5cf6;
    box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
  }

  .form-group input:disabled,
  .form-group select:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  /* Metadata Section */
  .metadata-section {
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    overflow: hidden;
  }

  .metadata-header {
    display: flex;
    align-items: center;
    background: #f8fafc;
  }

  .metadata-toggle {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex: 1;
    padding: 0.75rem 1rem;
    background: none;
    border: none;
    cursor: pointer;
    font-weight: 600;
    font-size: 0.9rem;
    color: #475569;
    transition: background 0.2s;
    text-align: left;
  }

  .metadata-toggle:hover { background: #f1f5f9; }

  .btn-fill {
    padding: 0.3rem 0.65rem;
    margin-right: 0.75rem;
    background: #f0fdf4;
    color: #16a34a;
    border: 1px solid #bbf7d0;
    border-radius: 0.375rem;
    font-weight: 600;
    font-size: 0.75rem;
    cursor: pointer;
    transition: all 0.2s;
    white-space: nowrap;
  }

  .btn-fill:hover {
    background: #dcfce7;
    border-color: #86efac;
  }

  .toggle-icon {
    font-size: 0.75rem;
    color: #94a3b8;
    width: 1rem;
    text-align: center;
  }

  .metadata-fields {
    padding: 1rem;
    border-top: 1px solid #e2e8f0;
    background: #fafbfc;
  }

  .metadata-hint {
    margin: 0 0 1rem 0;
    font-size: 0.8rem;
    color: #64748b;
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
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    font-size: 1rem;
    font-family: inherit;
    resize: vertical;
  }

  .form-group textarea:focus {
    outline: none;
    border-color: #8b5cf6;
    box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1);
  }

  .form-group textarea:disabled {
    background: #f1f5f9;
    cursor: not-allowed;
  }

  .btn-secondary, .btn-primary {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.5rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn-secondary { background: #f1f5f9; color: #475569; }
  .btn-secondary:hover:not(:disabled) { background: #e2e8f0; }

  .btn-primary { background: #8b5cf6; color: white; }
  .btn-primary:hover:not(:disabled) { background: #7c3aed; }
  .btn-primary:disabled, .btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

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

