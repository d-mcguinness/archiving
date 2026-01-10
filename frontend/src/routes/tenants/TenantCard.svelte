<script>
  import { createEventDispatcher } from 'svelte';

  export let tenant;

  const dispatch = createEventDispatcher();

  function handleEdit() {
    dispatch('edit', tenant);
  }

  function handleDelete() {
    dispatch('delete', tenant.id);
  }

  function getStatusColor(status) {
    const colors = {
      'ACTIVE': 'status-active',
      'INACTIVE': 'status-inactive',
      'SUSPENDED': 'status-suspended',
      'PENDING_ACTIVATION': 'status-pending',
      'TRIAL': 'status-trial',
      'EXPIRED': 'status-expired'
    };
    return colors[status] || 'status-default';
  }

  function getPlanColor(plan) {
    const colors = {
      'FREE': 'plan-free',
      'BASIC': 'plan-basic',
      'PROFESSIONAL': 'plan-professional',
      'ENTERPRISE': 'plan-enterprise',
      'CUSTOM': 'plan-custom'
    };
    return colors[plan] || 'plan-default';
  }

  function formatDate(dateString) {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
</script>

<div class="tenant-card">
  <div class="card-header">
    <div class="title-section">
      <h3 class="tenant-name">{tenant.displayName}</h3>
      <p class="tenant-domain">@{tenant.domain}</p>
    </div>
    <div class="badges">
      <span class="badge status {getStatusColor(tenant.status)}">
        {tenant.status.replace('_', ' ')}
      </span>
      <span class="badge plan {getPlanColor(tenant.plan)}">
        {tenant.plan}
      </span>
    </div>
  </div>

  <div class="card-body">
    {#if tenant.description}
      <p class="description">{tenant.description}</p>
    {:else}
      <p class="description placeholder">No description provided</p>
    {/if}

    <div class="details">
      <div class="detail-item">
        <span class="label">Name:</span>
        <span class="value">{tenant.name}</span>
      </div>
      <div class="detail-item">
        <span class="label">Owner ID:</span>
        <span class="value">{tenant.ownerId}</span>
      </div>
      <div class="detail-item">
        <span class="label">Created:</span>
        <span class="value">{formatDate(tenant.createdAt)}</span>
      </div>
      <div class="detail-item">
        <span class="label">Updated:</span>
        <span class="value">{formatDate(tenant.updatedAt)}</span>
      </div>
    </div>

    {#if tenant.settings}
      <div class="settings-summary">
        <h4>Settings</h4>
        <div class="settings-grid">
          {#if tenant.settings.maxUsers}
            <div class="setting-item">
              <span class="setting-label">Max Users:</span>
              <span class="setting-value">{tenant.settings.maxUsers}</span>
            </div>
          {/if}
          {#if tenant.settings.maxArchives}
            <div class="setting-item">
              <span class="setting-label">Max Archives:</span>
              <span class="setting-value">{tenant.settings.maxArchives}</span>
            </div>
          {/if}
          {#if tenant.settings.timezone}
            <div class="setting-item">
              <span class="setting-label">Timezone:</span>
              <span class="setting-value">{tenant.settings.timezone}</span>
            </div>
          {/if}
          {#if tenant.settings.defaultLanguage}
            <div class="setting-item">
              <span class="setting-label">Language:</span>
              <span class="setting-value">{tenant.settings.defaultLanguage}</span>
            </div>
          {/if}
        </div>
      </div>
    {/if}
  </div>

  <div class="card-actions">
    <button class="btn btn-edit" on:click={handleEdit}>
      ✏️ Edit
    </button>
    <button class="btn btn-delete" on:click={handleDelete}>
      🗑️ Delete
    </button>
  </div>
</div>

<style>
  .tenant-card {
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 1rem;
    overflow: hidden;
    transition: all 0.2s;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .tenant-card:hover {
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }

  .card-header {
    padding: 1.5rem;
    border-bottom: 1px solid #e2e8f0;
    background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  }

  .title-section {
    margin-bottom: 1rem;
  }

  .tenant-name {
    margin: 0 0 0.25rem 0;
    font-size: 1.25rem;
    font-weight: 700;
    color: #1a202c;
  }

  .tenant-domain {
    margin: 0;
    color: #64748b;
    font-size: 0.875rem;
    font-family: monospace;
    background: #f1f5f9;
    padding: 0.25rem 0.5rem;
    border-radius: 0.25rem;
    display: inline-block;
  }

  .badges {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  .badge {
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .status-active { background: #dcfce7; color: #166534; }
  .status-inactive { background: #f3f4f6; color: #374151; }
  .status-suspended { background: #fed7d7; color: #c53030; }
  .status-pending { background: #fef3c7; color: #92400e; }
  .status-trial { background: #dbeafe; color: #1d4ed8; }
  .status-expired { background: #fecaca; color: #dc2626; }

  .plan-free { background: #f3f4f6; color: #374151; }
  .plan-basic { background: #dbeafe; color: #1e40af; }
  .plan-professional { background: #dcfce7; color: #166534; }
  .plan-enterprise { background: #fef3c7; color: #92400e; }
  .plan-custom { background: #e9d5ff; color: #7c2d12; }

  .card-body {
    padding: 1.5rem;
  }

  .description {
    margin: 0 0 1.5rem 0;
    color: #4b5563;
    line-height: 1.6;
  }

  .description.placeholder {
    color: #9ca3af;
    font-style: italic;
  }

  .details {
    display: grid;
    gap: 0.75rem;
    margin-bottom: 1.5rem;
  }

  .detail-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0.5rem 0;
    border-bottom: 1px solid #f1f5f9;
  }

  .detail-item:last-child {
    border-bottom: none;
  }

  .label {
    font-weight: 600;
    color: #6b7280;
    font-size: 0.875rem;
  }

  .value {
    color: #1f2937;
    font-size: 0.875rem;
    text-align: right;
    word-break: break-word;
  }

  .settings-summary {
    border-top: 1px solid #e5e7eb;
    padding-top: 1rem;
  }

  .settings-summary h4 {
    margin: 0 0 0.75rem 0;
    font-size: 0.875rem;
    font-weight: 600;
    color: #374151;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .settings-grid {
    display: grid;
    gap: 0.5rem;
  }

  .setting-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.75rem;
  }

  .setting-label {
    color: #6b7280;
    font-weight: 500;
  }

  .setting-value {
    color: #1f2937;
    font-weight: 600;
  }

  .card-actions {
    padding: 1rem 1.5rem;
    background: #f8fafc;
    border-top: 1px solid #e2e8f0;
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
  }

  .btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
  }

  .btn-edit {
    background: #3b82f6;
    color: white;
  }

  .btn-edit:hover {
    background: #2563eb;
    transform: translateY(-1px);
  }

  .btn-delete {
    background: #ef4444;
    color: white;
  }

  .btn-delete:hover {
    background: #dc2626;
    transform: translateY(-1px);
  }

  @media (max-width: 480px) {
    .detail-item {
      flex-direction: column;
      align-items: flex-start;
      gap: 0.25rem;
    }

    .value {
      text-align: left;
    }

    .card-actions {
      flex-direction: column;
    }
  }
</style>
