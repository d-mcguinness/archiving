<script lang="ts">
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { page } from '$app/stores';
  import { auth } from '$lib/stores/authStore';

  function getArchivesPath() {
    const { role } = get(auth);
    if (role === 'ADMIN' || role === 'TENANT') return '/archives';
    return '/';
  }

  $: archivesPath = getArchivesPath();

  // URL parameters
  let archiveId: string | null = null;
  let archiveName: string = '';
  let standardFromUrl: string | null = null;

  // Available standards
  const standards = [
    { value: 'NOARK5', label: 'NOARK5 - Norwegian Records' },
    { value: 'OAIS', label: 'OAIS - Digital Preservation' },
    { value: 'PREMIS', label: 'PREMIS - Preservation Metadata' },
    { value: 'Dublin Core', label: 'Dublin Core - Simple Metadata' },
    { value: 'METS', label: 'METS - Structural Packaging' },
    { value: 'EAD', label: 'EAD - Archival Finding Aids' },
    { value: 'BagIt', label: 'BagIt - File Packaging' },
    { value: 'ISAD(G)', label: 'ISAD(G) - International Archival' },
    { value: 'MODS', label: 'MODS - Bibliographic Metadata' }
  ];

  let selectedStandard = 'Dublin Core';
  let selectedFiles: FileList | null = null;
  let uploading = false;
  let uploadProgress = 0;
  let uploadedDocuments: any[] = [];
  let error: string | null = null;

  // Metadata fields (minimal set)
  let metadata = {
    title: '',
    creator: '',
    description: '',
    date: new Date().toISOString().split('T')[0],
    type: 'document',
    format: '',
    identifier: '',
    rights: 'All rights reserved'
  };

  onMount(() => {
    // Get URL parameters
    const params = new URLSearchParams(window.location.search);
    archiveId = params.get('archiveId');
    standardFromUrl = params.get('standard');

    // If standard is provided in URL, use it and disable the dropdown
    if (standardFromUrl) {
      selectedStandard = standardFromUrl;
    }
  });

  function handleFileSelect(event: Event) {
    const input = event.target as HTMLInputElement;
    selectedFiles = input.files;

    if (selectedFiles && selectedFiles.length > 0) {
      const file = selectedFiles[0];
      // Auto-fill some metadata from file
      if (!metadata.title) {
        metadata.title = file.name.replace(/\.[^/.]+$/, ''); // Remove extension
      }
      metadata.format = file.type || 'application/octet-stream';
      metadata.identifier = `DOC-${Date.now()}`;
    }
  }

  async function handleUpload() {
    if (!selectedFiles || selectedFiles.length === 0) {
      error = 'Please select a file to upload';
      return;
    }

    if (!metadata.title || !metadata.creator) {
      error = 'Please fill in required fields (Title and Creator)';
      return;
    }

    uploading = true;
    error = null;
    uploadProgress = 0;

    try {
      const file = selectedFiles[0];

      // Simulate upload progress
      const progressInterval = setInterval(() => {
        uploadProgress += 10;
        if (uploadProgress >= 90) {
          clearInterval(progressInterval);
        }
      }, 200);

      // Simulate file upload (replace with actual API call)
      await new Promise(resolve => setTimeout(resolve, 2000));

      clearInterval(progressInterval);
      uploadProgress = 100;

      // Create document record
      const document = {
        id: metadata.identifier,
        archiveId: archiveId,
        fileName: file.name,
        fileSize: file.size,
        fileType: file.type,
        standard: selectedStandard,
        metadata: { ...metadata },
        uploadDate: new Date().toISOString(),
        status: 'uploaded'
      };

      uploadedDocuments = [...uploadedDocuments, document];

      // Reset form
      resetForm();

      // Show success message briefly
      setTimeout(() => {
        uploadProgress = 0;
      }, 2000);

    } catch (e) {
      error = e instanceof Error ? e.message : 'Upload failed';
      uploadProgress = 0;
    } finally {
      uploading = false;
    }
  }

  function resetForm() {
    selectedFiles = null;
    metadata = {
      title: '',
      creator: '',
      description: '',
      date: new Date().toISOString().split('T')[0],
      type: 'document',
      format: '',
      identifier: '',
      rights: 'All rights reserved'
    };
    const fileInput = document.getElementById('fileInput') as HTMLInputElement;
    if (fileInput) fileInput.value = '';
  }

  function deleteDocument(docId: string) {
    uploadedDocuments = uploadedDocuments.filter(doc => doc.id !== docId);
  }

  function formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
</script>

<div class="document-upload-page">
  <div class="page-header">
    <div class="header-left">
      <a href={archivesPath} class="back-button">← Back to Archives</a>
      <h1>📄 Document Upload</h1>
      {#if archiveId}
        <p class="archive-context">
          Uploading to archive using <strong>{selectedStandard}</strong> standard
        </p>
      {:else}
        <p>Upload documents with archival metadata</p>
      {/if}
    </div>
  </div>

  {#if error}
    <div class="alert alert-error">
      <span class="alert-icon">⚠️</span>
      <span>{error}</span>
      <button class="alert-close" on:click={() => error = null}>×</button>
    </div>
  {/if}

  <div class="upload-container">
    <!-- Standard Selection -->
    <div class="form-section">
      <h2>1. Select Standard</h2>
      <div class="form-group">
        <label for="standard">Archival Standard *</label>
        <select
          id="standard"
          bind:value={selectedStandard}
          disabled={archiveId !== null}
          class:readonly-field={archiveId !== null}
        >
          {#each standards as standard}
            <option value={standard.value}>{standard.label}</option>
          {/each}
        </select>
        {#if archiveId}
          <small class="helper-text info">
            ℹ️ Standard is set by the archive and cannot be changed
          </small>
        {:else}
          <small class="helper-text">Choose the metadata standard for this document</small>
        {/if}
      </div>
    </div>

    <!-- File Upload -->
    <div class="form-section">
      <h2>2. Select File</h2>
      <div class="form-group">
        <label for="fileInput">Document File *</label>
        <input
          type="file"
          id="fileInput"
          on:change={handleFileSelect}
          accept=".pdf,.doc,.docx,.txt,.jpg,.jpeg,.png,.tiff,.xml,.json"
          class="file-input"
        />
        {#if selectedFiles && selectedFiles.length > 0}
          <div class="file-preview">
            <span class="file-icon">📎</span>
            <div class="file-info">
              <div class="file-name">{selectedFiles[0].name}</div>
              <div class="file-size">{formatFileSize(selectedFiles[0].size)}</div>
            </div>
          </div>
        {/if}
        <small class="helper-text">Supported: PDF, DOC, DOCX, TXT, JPG, PNG, TIFF, XML, JSON</small>
      </div>
    </div>

    <!-- Metadata -->
    <div class="form-section">
      <h2>3. Add Metadata</h2>

      <div class="form-group">
        <label for="title">Title *</label>
        <input
          type="text"
          id="title"
          bind:value={metadata.title}
          placeholder="Document title"
          required
        />
      </div>

      <div class="form-group">
        <label for="creator">Creator *</label>
        <input
          type="text"
          id="creator"
          bind:value={metadata.creator}
          placeholder="Author or organization"
          required
        />
      </div>

      <div class="form-group">
        <label for="description">Description</label>
        <textarea
          id="description"
          bind:value={metadata.description}
          placeholder="Brief description of the document"
          rows="3"
        ></textarea>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label for="date">Date</label>
          <input
            type="date"
            id="date"
            bind:value={metadata.date}
          />
        </div>

        <div class="form-group">
          <label for="type">Type</label>
          <select id="type" bind:value={metadata.type}>
            <option value="document">Document</option>
            <option value="image">Image</option>
            <option value="audio">Audio</option>
            <option value="video">Video</option>
            <option value="dataset">Dataset</option>
            <option value="text">Text</option>
            <option value="report">Report</option>
            <option value="correspondence">Correspondence</option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label for="identifier">Identifier</label>
        <input
          type="text"
          id="identifier"
          bind:value={metadata.identifier}
          placeholder="Auto-generated"
          readonly
        />
        <small class="helper-text">Automatically assigned unique identifier</small>
      </div>

      <div class="form-group">
        <label for="rights">Rights Statement</label>
        <input
          type="text"
          id="rights"
          bind:value={metadata.rights}
          placeholder="Copyright or license information"
        />
      </div>
    </div>

    <!-- Upload Button -->
    <div class="form-actions">
      <button
        type="button"
        class="btn btn-secondary"
        on:click={resetForm}
        disabled={uploading}
      >
        Reset
      </button>
      <button
        type="button"
        class="btn btn-primary"
        on:click={handleUpload}
        disabled={uploading || !selectedFiles}
      >
        {uploading ? 'Uploading...' : '📤 Upload Document'}
      </button>
    </div>

    <!-- Progress Bar -->
    {#if uploading || uploadProgress > 0}
      <div class="progress-section">
        <div class="progress-bar">
          <div class="progress-fill" style="width: {uploadProgress}%"></div>
        </div>
        <div class="progress-text">{uploadProgress}%</div>
      </div>
    {/if}
  </div>

  <!-- Uploaded Documents List -->
  {#if uploadedDocuments.length > 0}
    <div class="documents-section">
      <h2>📚 Uploaded Documents ({uploadedDocuments.length})</h2>
      <div class="documents-list">
        {#each uploadedDocuments as doc}
          <div class="document-card">
            <div class="document-header">
              <div class="document-icon">📄</div>
              <div class="document-info">
                <h3>{doc.metadata.title}</h3>
                <div class="document-meta">
                  <span class="badge">{doc.standard}</span>
                  <span class="meta-item">📁 {doc.fileName}</span>
                  <span class="meta-item">💾 {formatFileSize(doc.fileSize)}</span>
                </div>
              </div>
              <button
                class="btn-delete"
                on:click={() => deleteDocument(doc.id)}
                title="Delete document"
              >
                🗑️
              </button>
            </div>
            <div class="document-details">
              <div class="detail-row">
                <strong>Creator:</strong> {doc.metadata.creator}
              </div>
              <div class="detail-row">
                <strong>Date:</strong> {doc.metadata.date}
              </div>
              <div class="detail-row">
                <strong>ID:</strong> {doc.id}
              </div>
              {#if doc.metadata.description}
                <div class="detail-row">
                  <strong>Description:</strong> {doc.metadata.description}
                </div>
              {/if}
            </div>
          </div>
        {/each}
      </div>
    </div>
  {/if}

  <!-- Bottom Navigation -->
  <div class="bottom-navigation">
    <a href={archivesPath} class="back-button-bottom">← Back to Archives</a>
  </div>
</div>

<style>
  .document-upload-page {
    max-width: 900px;
    margin: 0 auto;
    padding: 2rem;
  }

  .page-header {
    margin-bottom: 2rem;
  }

  .header-left {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }

  .back-button {
    display: inline-flex;
    align-items: center;
    color: #3b82f6;
    text-decoration: none;
    font-size: 0.875rem;
    font-weight: 500;
    transition: color 0.2s;
    width: fit-content;
  }

  .back-button:hover {
    color: #2563eb;
    text-decoration: underline;
  }

  .page-header h1 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 2rem;
    font-weight: 700;
  }

  .page-header p {
    margin: 0;
    color: #64748b;
    font-size: 1rem;
  }

  .archive-context {
    color: #3b82f6 !important;
    font-size: 0.95rem !important;
  }

  .archive-context strong {
    color: #1e40af;
  }

  /* Alert */
  .alert {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1.5rem;
  }

  .alert-error {
    background: #fef2f2;
    border: 1px solid #fecaca;
    color: #991b1b;
  }

  .alert-icon {
    font-size: 1.25rem;
  }

  .alert-close {
    margin-left: auto;
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: inherit;
    opacity: 0.6;
  }

  .alert-close:hover {
    opacity: 1;
  }

  /* Upload Container */
  .upload-container {
    background: white;
    border: 2px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 2rem;
    margin-bottom: 2rem;
  }

  .form-section {
    margin-bottom: 2rem;
    padding-bottom: 2rem;
    border-bottom: 1px solid #e2e8f0;
  }

  .form-section:last-of-type {
    border-bottom: none;
  }

  .form-section h2 {
    margin: 0 0 1rem 0;
    color: #334155;
    font-size: 1.125rem;
    font-weight: 600;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  .form-group:last-child {
    margin-bottom: 0;
  }

  .form-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
  }

  label {
    display: block;
    margin-bottom: 0.5rem;
    color: #475569;
    font-weight: 500;
    font-size: 0.875rem;
  }

  input[type="text"],
  input[type="date"],
  select,
  textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.375rem;
    font-size: 0.875rem;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  input:focus,
  select:focus,
  textarea:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  input[readonly] {
    background: #f1f5f9;
    color: #64748b;
  }

  select.readonly-field {
    background: #dbeafe;
    color: #1e40af;
    border-color: #93c5fd;
    cursor: not-allowed;
    font-weight: 500;
  }

  textarea {
    resize: vertical;
    font-family: inherit;
  }

  .file-input {
    width: 100%;
    padding: 0.75rem;
    border: 2px dashed #cbd5e1;
    border-radius: 0.375rem;
    cursor: pointer;
    transition: border-color 0.2s;
  }

  .file-input:hover {
    border-color: #3b82f6;
  }

  .file-preview {
    display: flex;
    align-items: center;
    gap: 1rem;
    margin-top: 1rem;
    padding: 1rem;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.375rem;
  }

  .file-icon {
    font-size: 2rem;
  }

  .file-info {
    flex: 1;
  }

  .file-name {
    font-weight: 500;
    color: #334155;
    margin-bottom: 0.25rem;
  }

  .file-size {
    font-size: 0.875rem;
    color: #64748b;
  }

  .helper-text {
    display: block;
    margin-top: 0.375rem;
    color: #64748b;
    font-size: 0.75rem;
  }

  .helper-text.info {
    color: #3b82f6;
    font-weight: 500;
  }

  /* Form Actions */
  .form-actions {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
    margin-top: 2rem;
    padding-top: 2rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn {
    padding: 0.75rem 1.5rem;
    border: none;
    border-radius: 0.375rem;
    font-weight: 500;
    font-size: 0.875rem;
    cursor: pointer;
    transition: all 0.2s;
  }

  .btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .btn-primary {
    background: #3b82f6;
    color: white;
  }

  .btn-primary:hover:not(:disabled) {
    background: #2563eb;
    box-shadow: 0 2px 4px rgba(59, 130, 246, 0.3);
  }

  .btn-secondary {
    background: #f1f5f9;
    color: #475569;
  }

  .btn-secondary:hover:not(:disabled) {
    background: #e2e8f0;
  }

  /* Progress */
  .progress-section {
    margin-top: 1.5rem;
  }

  .progress-bar {
    width: 100%;
    height: 8px;
    background: #e2e8f0;
    border-radius: 4px;
    overflow: hidden;
  }

  .progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #3b82f6, #2563eb);
    transition: width 0.3s ease;
  }

  .progress-text {
    text-align: center;
    margin-top: 0.5rem;
    color: #64748b;
    font-size: 0.875rem;
    font-weight: 500;
  }

  /* Documents Section */
  .documents-section {
    margin-top: 2rem;
  }

  .documents-section h2 {
    margin: 0 0 1rem 0;
    color: #1e293b;
    font-size: 1.25rem;
    font-weight: 600;
  }

  .documents-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }

  .document-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 0.5rem;
    padding: 1.5rem;
    transition: box-shadow 0.2s;
  }

  .document-card:hover {
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  }

  .document-header {
    display: flex;
    align-items: flex-start;
    gap: 1rem;
    margin-bottom: 1rem;
  }

  .document-icon {
    font-size: 2rem;
  }

  .document-info {
    flex: 1;
  }

  .document-info h3 {
    margin: 0 0 0.5rem 0;
    color: #1e293b;
    font-size: 1rem;
    font-weight: 600;
  }

  .document-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    align-items: center;
  }

  .badge {
    display: inline-block;
    padding: 0.25rem 0.625rem;
    background: #dbeafe;
    color: #1e40af;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 500;
  }

  .meta-item {
    color: #64748b;
    font-size: 0.875rem;
  }

  .btn-delete {
    background: none;
    border: none;
    font-size: 1.25rem;
    cursor: pointer;
    opacity: 0.5;
    transition: opacity 0.2s;
  }

  .btn-delete:hover {
    opacity: 1;
  }

  .document-details {
    padding-top: 1rem;
    border-top: 1px solid #f1f5f9;
  }

  .detail-row {
    margin-bottom: 0.5rem;
    color: #475569;
    font-size: 0.875rem;
  }

  .detail-row:last-child {
    margin-bottom: 0;
  }

  .detail-row strong {
    color: #334155;
    font-weight: 600;
  }

  /* Bottom Navigation */
  .bottom-navigation {
    margin-top: 3rem;
    padding-top: 2rem;
    border-top: 2px solid #e2e8f0;
    display: flex;
    justify-content: center;
  }

  .back-button-bottom {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem 1.5rem;
    background: #f1f5f9;
    color: #475569;
    text-decoration: none;
    font-weight: 500;
    border-radius: 0.375rem;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .back-button-bottom:hover {
    background: #e2e8f0;
    color: #1e293b;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  }

  /* Responsive */
  @media (max-width: 640px) {
    .document-upload-page {
      padding: 1rem;
    }

    .upload-container {
      padding: 1.5rem;
    }

    .form-row {
      grid-template-columns: 1fr;
    }

    .form-actions {
      flex-direction: column;
    }

    .btn {
      width: 100%;
    }
  }
</style>
