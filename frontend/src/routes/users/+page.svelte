<script lang="ts">
  import { onMount } from 'svelte';
  import { client } from '$lib/apollo';
  import { GET_ALL_USERS } from '$lib/graphql/queries';
  import { toasts } from '$lib/stores/toastStore';

  let users: any[] = [];
  let loading = true;
  let error: string | null = null;
  let fileInputs: { [key: number]: HTMLInputElement } = {};
  let uploadingUsers: Set<number> = new Set();

  onMount(async () => {
    await loadUsers();
  });

  async function loadUsers() {
    try {
      loading = true;
      const result = await client.query({
        query: GET_ALL_USERS,
        fetchPolicy: 'network-only'
      });
      users = result?.data?.getAllUsers || [];
      error = null;
    } catch (e) {
      error = e instanceof Error ? e.message : 'An unknown error occurred';
      console.error('Load users error:', e);
    } finally {
      loading = false;
    }
  }

  function triggerFileUpload(userId: number) {
    const input = fileInputs[userId];
    if (input) {
      input.click();
    }
  }

  async function handleFileSelect(event: Event, userId: number, userName: string) {
    const target = event.target as HTMLInputElement;
    const file = target.files?.[0];

    if (!file) {
      console.log('No file selected');
      return;
    }

    // Add user to uploading set
    uploadingUsers.add(userId);
    uploadingUsers = uploadingUsers; // Trigger reactivity

    const startTime = performance.now();

    try {
      // Log upload start
      console.group('📤 File Upload Round Trip Test');
      console.log('User ID:', userId);
      console.log('User Name:', userName);
      console.log('File Name:', file.name);
      console.log('File Size:', (file.size / 1024).toFixed(2), 'KB');
      console.log('File Type:', file.type);
      console.log('Upload Started:', new Date().toLocaleTimeString());

      // Create FormData to send file
      const formData = new FormData();
      formData.append('file', file);

      console.log('FormData created with file:', file.name);

      // Upload file to backend
      const uploadStartTime = performance.now();
      const response = await fetch(`http://localhost:2020/api/users/${userId}/upload`, {
        method: 'POST',
        body: formData,
      });
      const uploadDuration = performance.now() - uploadStartTime;

      console.log('Response Status:', response.status, response.statusText);
      console.log('Upload Duration:', uploadDuration.toFixed(2), 'ms');

      // Parse response
      let result;
      try {
        result = await response.json();
        console.log('Response Body:', result);
      } catch (parseError) {
        console.error('Failed to parse JSON response:', parseError);
        throw new Error('Invalid response from server');
      }

      // Check response
      if (response.ok && result.success) {
        const totalDuration = performance.now() - startTime;

        console.log('✅ Upload Successful!');
        console.log('Uploaded File Name:', result.filename);
        console.log('Original File Name:', result.originalFilename);
        console.log('File Path:', result.filePath);
        console.log('Upload Time:', result.uploadTime);
        console.log('Total Round Trip Duration:', totalDuration.toFixed(2), 'ms');
        console.groupEnd();

        // Show success toast
        toasts.success(`Document archived: ${file.name}`);

        // Verify file was saved
        console.log('✅ Backend confirmed file saved to:', result.filePath);
      } else {
        console.error('❌ Upload Failed!');
        console.error('Error:', result.error || result.message || 'Unknown error');
        console.groupEnd();
        throw new Error(result.error || result.message || 'Upload failed');
      }

      // Reset the input so the same file can be selected again if needed
      target.value = '';
    } catch (error) {
      const totalDuration = performance.now() - startTime;

      console.error('❌ Upload Error!');
      console.error('Error Type:', error instanceof Error ? error.constructor.name : typeof error);
      console.error('Error Message:', error instanceof Error ? error.message : String(error));
      console.error('Total Duration:', totalDuration.toFixed(2), 'ms');
      console.groupEnd();

      const errorMessage = error instanceof Error ? error.message : 'Failed to upload file';
      toasts.error(`Upload failed: ${errorMessage}`);
    } finally {
      // Remove user from uploading set
      uploadingUsers.delete(userId);
      uploadingUsers = uploadingUsers; // Trigger reactivity
    }
  }

  async function handleDownload(userId: number, userName: string) {
    try {
      console.group('⬇️ File Download Round Trip Test');
      console.log('User ID:', userId);
      console.log('User Name:', userName);
      console.log('Download Started:', new Date().toLocaleTimeString());

      const startTime = performance.now();

      // Fetch file from backend
      const response = await fetch(`http://localhost:2020/api/users/${userId}/download/latest`, {
        method: 'GET',
      });

      console.log('Response Status:', response.status, response.statusText);

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || errorData.message || 'Download failed');
      }

      // Get filename from Content-Disposition header
      const contentDisposition = response.headers.get('Content-Disposition');
      let filename = `user_${userId}_file.bin`;

      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        if (filenameMatch && filenameMatch[1]) {
          filename = filenameMatch[1].replace(/['"]/g, '');
        }
      }

      console.log('Filename from header:', filename);
      console.log('Content-Type:', response.headers.get('Content-Type'));

      // Convert response to blob
      const blob = await response.blob();
      const downloadDuration = performance.now() - startTime;

      console.log('File Size:', (blob.size / 1024).toFixed(2), 'KB');
      console.log('Download Duration:', downloadDuration.toFixed(2), 'ms');

      // Create download link and trigger download (shows in browser download bar)
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();

      // Cleanup
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      const totalDuration = performance.now() - startTime;

      console.log('✅ Download Successful!');
      console.log('File shown in browser download bar');
      console.log('Total Round Trip Duration:', totalDuration.toFixed(2), 'ms');
      console.groupEnd();

      toasts.success(`File downloaded: ${filename}`);

    } catch (error) {
      console.error('❌ Download Error!');
      console.error('Error Message:', error instanceof Error ? error.message : String(error));
      console.groupEnd();

      const errorMessage = error instanceof Error ? error.message : 'Failed to download file';
      toasts.error(`Download failed: ${errorMessage}`);
    }
  }
</script>

<svelte:head>
  <title>Users - Archiving System</title>
</svelte:head>

<div class="users-page">
  <div class="page-header">
    <h1>Users</h1>
    <a href="/users/create" class="add-user-btn">Add User</a>
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
  {:else if users.length === 0}
    <div class="empty-state">
      <p>No users found. Create your first user to get started!</p>
    </div>
  {:else}
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>User</th>
            <th>Age</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {#each users as user (user.id)}
            <tr>
              <td class="id-cell">{user.id}</td>
              <td class="title-cell">
                <div class="title-wrapper">
                  <div class="user-title">{user.name}</div>
                  <div class="user-email">{user.email}</div>
                </div>
              </td>
              <td class="age-cell">{user.age || '-'}</td>
              <td class="actions-cell">
                <!-- Hidden file input -->
                <input
                  type="file"
                  bind:this={fileInputs[user.id]}
                  on:change={(e) => handleFileSelect(e, user.id, user.name)}
                  style="display: none;"
                  accept="*/*"
                />
                <button
                  class="btn-action btn-upload"
                  class:uploading={uploadingUsers.has(user.id)}
                  on:click={() => triggerFileUpload(user.id)}
                  disabled={uploadingUsers.has(user.id)}
                  title={uploadingUsers.has(user.id) ? 'Uploading...' : 'Upload file'}
                >
                  {uploadingUsers.has(user.id) ? '⏳ Uploading...' : '📁 Upload'}
                </button>
                <button
                  class="btn-action btn-download"
                  on:click={() => handleDownload(user.id, user.name)}
                  title="Download latest file"
                >
                  ⬇️ Download
                </button>
                <a href="/users/update?userId={user.id}" class="btn-action btn-edit">
                  ✏️ Edit
                </a>
                <a href="/users/delete?userId={user.id}" class="btn-action btn-delete">
                  🗑️ Delete
                </a>
              </td>
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
  {/if}
</div>

<style>
  .users-page {
    max-width: 1400px;
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

  .add-user-btn {
    background: #3b82f6;
    color: white;
    padding: 0.75rem 1.5rem;
    border-radius: 0.375rem;
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .add-user-btn:hover {
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
    min-width: 800px;
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

  .user-title {
    font-weight: 500;
    color: #1e293b;
  }

  .user-email {
    font-size: 0.875rem;
    color: #64748b;
    line-height: 1.4;
  }

  .age-cell {
    color: #64748b;
    font-size: 0.875rem;
    width: 100px;
  }

  .actions-cell {
    text-align: right;
    white-space: nowrap;
    width: 350px;
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
    text-decoration: none;
  }

  .btn-upload {
    background: #10b981;
    color: white;
  }

  .btn-upload:hover:not(:disabled) {
    background: #059669;
  }

  .btn-upload.uploading {
    background: #6b7280;
    cursor: wait;
    animation: pulse-upload 1.5s ease-in-out infinite;
  }

  .btn-upload:disabled {
    opacity: 0.7;
    cursor: not-allowed;
  }

  @keyframes pulse-upload {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.6;
    }
  }

  .btn-download {
    background: #0891b2;
    color: white;
  }

  .btn-download:hover {
    background: #0e7490;
  }

  .btn-edit {
    background: #3b82f6;
    color: white;
  }

  .btn-edit:hover {
    background: #2563eb;
  }

  .btn-delete {
    background: #dc2626;
    color: white;
  }

  .btn-delete:hover {
    background: #b91c1c;
  }
</style>


