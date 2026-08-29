<script lang="ts">
import { page } from '$app/stores';
import { goto } from '$app/navigation';
import { client } from '$lib/apollo';
import { authHeaders, API_BASE } from '$lib/api';
import { GET_USER, UPDATE_USER } from '$lib/graphql/queries';
import { onMount } from 'svelte';
import { get } from 'svelte/store';
import { toasts } from '$lib/stores/toastStore';

let userId = '';
let user: any = null;
let loading = true;
let error: string | null = null;
let updating = false;

let form = { name: '', email: '', age: '' };

// File upload state
let selectedFile: File | null = null;
let uploading = false;
let uploadMessage = '';
let uploadError = '';

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files.length > 0) {
    selectedFile = input.files[0];
    uploadMessage = '';
    uploadError = '';
  }
}

async function handleUpload() {
  if (!selectedFile) {
    uploadError = 'Please select a file first';
    return;
  }

  if (!userId) {
    uploadError = 'No user ID available';
    return;
  }

  uploading = true;
  uploadMessage = '';
  uploadError = '';

  try {
    const formData = new FormData();
    formData.append('file', selectedFile);
    formData.append('userId', userId);

    const response = await fetch(`${API_BASE}/api/upload/user`, {
      method: 'POST',
      headers: { ...authHeaders() },
      body: formData
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `Upload failed: ${response.status}`);
    }

    const result = await response.json();
    uploadMessage = result.message || 'File uploaded successfully!';
    selectedFile = null;

    // Reset file input
    const fileInput = document.getElementById('user-file-upload') as HTMLInputElement;
    if (fileInput) fileInput.value = '';

    toasts.add(`File uploaded for user "${user?.name}"`, 'success');
  } catch (e) {
    uploadError = e instanceof Error ? e.message : 'Failed to upload file';
    console.error('Upload error:', e);
    toasts.add(`File upload failed: ${uploadError}`, 'error');
  } finally {
    uploading = false;
  }
}

onMount(async () => {
  const url = new URL(get(page).url);
  userId = url.searchParams.get('userId') || '';
  if (userId) {
    try {
      const result = await client.query({
        query: GET_USER,
        variables: { id: userId },
        fetchPolicy: 'network-only'
      });
      user = result.data.getUser;
      if (user) {
        form.name = user.name;
        form.email = user.email;
        form.age = user.age ?? '';
      }
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      toasts.add(`Failed to load user: ${error}`, 'error');
    } finally {
      loading = false;
    }
  } else {
    loading = false;
    error = 'No userId provided.';
  }
});

async function updateUser() {
  if (!form.name || !form.email) return;
  try {
    updating = true;
    await client.mutate({
      mutation: UPDATE_USER,
      variables: {
        id: userId,
        input: {
          name: form.name,
          email: form.email,
          age: form.age ? parseInt(form.age) : null
        }
      }
    });
    toasts.add(`User "${form.name}" updated successfully`, 'success');
    goto('/users');
  } catch (e) {
    error = e instanceof Error ? e.message : 'An unknown error occurred';
    toasts.add(`Failed to update user: ${error}`, 'error');
  } finally {
    updating = false;
  }
}
</script>

<span class="eyebrow">Users</span>
<h2>Update User</h2>
{#if loading}
  <div class="loading">Loading...</div>
{:else if error}
  <div class="error">Error: {error}</div>
{:else if user}
  <form on:submit|preventDefault={updateUser} class="form-container">
    <div class="form-group">
      <label for="name">Name *</label>
      <input type="text" id="name" bind:value={form.name} required disabled={updating} />
    </div>
    <div class="form-group">
      <label for="email">Email *</label>
      <input type="email" id="email" bind:value={form.email} required disabled={updating} />
    </div>
    <div class="form-group">
      <label for="age">Age</label>
      <input type="number" id="age" bind:value={form.age} min="0" max="150" disabled={updating} />
    </div>
    <button type="submit" disabled={updating || !form.name || !form.email}>
      {updating ? 'Updating...' : 'Update User'}
    </button>
  </form>

  <!-- File Upload Section -->
  <div class="upload-section">
    <h3>Upload File for User</h3>
    <div class="upload-card">
      <div class="upload-area">
        <input
          type="file"
          id="user-file-upload"
          on:change={handleFileSelect}
          disabled={uploading}
          class="file-input"
        />
        <label for="user-file-upload" class="file-label">
          <span class="upload-icon">📁</span>
          <span class="upload-text">
            {selectedFile ? selectedFile.name : 'Choose a file to upload'}
          </span>
        </label>
      </div>

      {#if uploadMessage}
        <div class="upload-success">
          <span class="success-icon">✅</span>
          <span>{uploadMessage}</span>
        </div>
      {/if}

      {#if uploadError}
        <div class="upload-error">
          <span class="error-icon">❌</span>
          <span>{uploadError}</span>
        </div>
      {/if}

      <button
        type="button"
        class="upload-button"
        on:click={handleUpload}
        disabled={!selectedFile || uploading}
      >
        {uploading ? '⏳ Uploading...' : '📤 Upload File'}
      </button>
    </div>
  </div>
{/if}

<style>
  h2 {
    color: var(--arc-ink, #0f172a);
  }
  .form-container {
    max-width: 400px;
    margin: 2rem auto;
    background: var(--arc-card, #fff);
    padding: 2rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }
  .form-group {
    margin-bottom: 1.5rem;
  }
  .loading {
    text-align: center;
    margin: 2rem;
    color: var(--arc-muted, #64748b);
  }
  .error {
    margin: 2rem 0;
    text-align: center;
  }

  .upload-section {
    max-width: 400px;
    margin: 2rem auto;
  }

  .upload-section h3 {
    margin-bottom: 1rem;
    color: var(--arc-ink, #0f172a);
    font-size: 1.25rem;
  }

  .upload-card {
    background: var(--arc-card, #fff);
    padding: 1.5rem;
    border-radius: 1rem;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
    border: 1px solid var(--arc-line, #e8edf3);
  }

  .upload-area {
    margin-bottom: 1rem;
  }

  .file-input {
    display: none;
  }

  .file-label {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
    border: 2px dashed var(--arc-line-strong, #cbd5e1);
    border-radius: 0.5rem;
    cursor: pointer;
    transition: all 0.2s;
    background: var(--arc-card-2, #f8fafc);
  }

  .file-label:hover {
    border-color: var(--arc-indigo, #6366f1);
    background: var(--arc-chip-soft-indigo-bg, #eef2ff);
  }

  .upload-icon {
    font-size: 1.5rem;
  }

  .upload-text {
    color: var(--arc-body, #475569);
    font-weight: 500;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .upload-success {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem;
    background: var(--arc-alert-green-bg, #dcfce7);
    border: 1px solid var(--arc-alert-green-border, #86efac);
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    color: var(--arc-alert-green-ink, #166534);
    font-size: 0.875rem;
  }

  .success-icon {
    font-size: 1.125rem;
  }

  .upload-error {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.75rem;
    background: var(--arc-alert-red-bg, #fee2e2);
    border: 1px solid var(--arc-alert-red-border, #fca5a5);
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    color: var(--arc-alert-red-ink, #991b1b);
    font-size: 0.875rem;
  }

  .error-icon {
    font-size: 1.125rem;
  }

  .upload-button {
    width: 100%;
  }
</style>
