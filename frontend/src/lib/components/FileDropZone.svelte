<script lang="ts">
  import { createEventDispatcher } from 'svelte';

  export let disabled = false;

  const dispatch = createEventDispatcher();

  let dragOver = false;
  let files: File[] = [];
  let computing = false;
  let fileInput: HTMLInputElement;

  async function computeSha256(file: File): Promise<string> {
    const buffer = await file.arrayBuffer();
    const hash = await crypto.subtle.digest('SHA-256', buffer);
    return Array.from(new Uint8Array(hash)).map(b => b.toString(16).padStart(2, '0')).join('');
  }

  async function processFiles(fileList: FileList | File[]) {
    const selected = Array.from(fileList);
    if (selected.length === 0) return;

    files = selected;
    computing = true;

    try {
      const firstFile = selected[0];
      const checksum = await computeSha256(firstFile);
      const totalSize = selected.reduce((sum, f) => sum + f.size, 0);

      const metadata = {
        filename: selected.length === 1 ? firstFile.name : `${selected.length} files`,
        contentType: firstFile.type || 'application/octet-stream',
        fileSize: totalSize,
        checksum,
        uploadedAt: new Date().toISOString().split('T')[0],
        uploaderName: '',
        fileCount: selected.length
      };

      dispatch('filesProcessed', { files: selected, metadata });
    } catch (e) {
      console.error('Failed to process files:', e);
    } finally {
      computing = false;
    }
  }

  function handleDrop(e: DragEvent) {
    e.preventDefault();
    dragOver = false;
    if (disabled || !e.dataTransfer?.files) return;
    processFiles(e.dataTransfer.files);
  }

  function handleDragOver(e: DragEvent) {
    e.preventDefault();
    if (!disabled) dragOver = true;
  }

  function handleDragLeave() { dragOver = false; }

  function handleClick() {
    if (!disabled) fileInput?.click();
  }

  function handleInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    if (input.files) processFiles(input.files);
  }

  function removeFile(index: number) {
    files = files.filter((_, i) => i !== index);
    if (files.length === 0) dispatch('filesCleared');
  }

  function formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
  }
</script>

<div
  class="drop-zone"
  class:drag-over={dragOver}
  class:disabled
  class:has-files={files.length > 0}
  on:drop={handleDrop}
  on:dragover={handleDragOver}
  on:dragleave={handleDragLeave}
  on:click={handleClick}
  on:keydown={(e) => e.key === 'Enter' && handleClick()}
  role="button"
  tabindex="0"
>
  <input
    bind:this={fileInput}
    type="file"
    multiple
    class="file-input"
    on:change={handleInputChange}
    {disabled}
  />

  {#if computing}
    <div class="drop-content">
      <div class="spinner"></div>
      <p class="drop-text">Processing files...</p>
    </div>
  {:else if files.length > 0}
    <div class="file-list" on:click|stopPropagation on:keydown|stopPropagation>
      {#each files as file, i}
        <div class="file-item">
          <span class="file-icon">📄</span>
          <span class="file-name">{file.name}</span>
          <span class="file-size">{formatSize(file.size)}</span>
          <button class="file-remove" on:click={() => removeFile(i)} title="Remove">&times;</button>
        </div>
      {/each}
    </div>
  {:else}
    <div class="drop-content">
      <span class="drop-icon">📁</span>
      <p class="drop-text">Drop files here or click to browse</p>
      <p class="drop-hint">File metadata will auto-populate Intake fields</p>
    </div>
  {/if}
</div>

<style>
  .drop-zone {
    border: 2px dashed #cbd5e1;
    border-radius: 0.5rem;
    padding: 1.5rem;
    text-align: center;
    cursor: pointer;
    transition: all 0.2s;
    background: #f8fafc;
    min-height: 100px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .drop-zone:hover:not(.disabled) { border-color: #93c5fd; background: #eff6ff; }
  .drop-zone.drag-over { border-color: #3b82f6; background: #dbeafe; border-style: solid; }
  .drop-zone.disabled { opacity: 0.5; cursor: not-allowed; }
  .drop-zone.has-files { cursor: default; text-align: left; }
  .file-input { display: none; }
  .drop-content { display: flex; flex-direction: column; align-items: center; gap: 0.25rem; }
  .drop-icon { font-size: 2rem; }
  .drop-text { margin: 0; color: #475569; font-weight: 500; font-size: 0.9rem; }
  .drop-hint { margin: 0; color: #94a3b8; font-size: 0.75rem; }
  .spinner { width: 1.5rem; height: 1.5rem; border: 2px solid #e2e8f0; border-top-color: #3b82f6; border-radius: 50%; animation: spin 1s linear infinite; }
  @keyframes spin { to { transform: rotate(360deg); } }
  .file-list { width: 100%; display: flex; flex-direction: column; gap: 0.5rem; }
  .file-item { display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0.75rem; background: white; border: 1px solid #e2e8f0; border-radius: 0.375rem; }
  .file-icon { font-size: 1rem; }
  .file-name { flex: 1; font-size: 0.85rem; font-weight: 500; color: #1e293b; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .file-size { font-size: 0.75rem; color: #94a3b8; white-space: nowrap; }
  .file-remove { background: none; border: none; color: #94a3b8; font-size: 1.25rem; cursor: pointer; padding: 0 0.25rem; line-height: 1; }
  .file-remove:hover { color: #ef4444; }
</style>