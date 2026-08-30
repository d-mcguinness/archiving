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
    <button class="btn btn-edit btn-primary" on:click={handleEdit}>
      ✏️ Edit
    </button>
    <button class="btn btn-delete btn-danger" on:click={handleDelete}>
      🗑️ Delete
    </button>
  </div>
</div>

<style>
  .tenant-card {
    background: var(--arc-card, #fff);
    border: 1px solid var(--arc-line, #e8edf3);
    border-radius: 1rem;
    overflow: hidden;
    transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
    box-shadow: var(--arc-shadow-card, 0 1px 2px rgba(15, 23, 42, 0.04));
  }

  .tenant-card:hover {
    border-color: var(--arc-hover-border, #c7d2fe);
    box-shadow: var(--arc-shadow-lift, 0 18px 40px -16px rgba(15, 23, 42, 0.18));
    transform: translateY(-4px);
  }

  .card-header {
    padding: 1.5rem;
    border-bottom: 1px solid var(--arc-line, #e8edf3);
    background: var(--arc-ground, #f8fafc);
  }

  .title-section {
    margin-bottom: 1rem;
  }

  .tenant-name {
    margin: 0 0 0.25rem 0;
    font-size: 1.25rem;
    font-weight: 700;
    color: var(--arc-ink, #0f172a);
  }

  .tenant-domain {
    margin: 0;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
    font-family: monospace;
    background: var(--arc-card-2, #f1f5f9);
    padding: 0.25rem 0.5rem;
    border-radius: 0.25rem;
    display: inline-block;
  }

  .badges {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;
  }

  /* .badge base comes from the global kit; these tenant-specific
     status/plan hues are not part of the global status list. */
  .status-active { background: var(--arc-chip-green-bg, #dcfce7); color: var(--arc-chip-green-ink, #166534); }
  .status-inactive { background: var(--arc-chip-slate-bg, #f1f5f9); color: var(--arc-chip-slate-ink, #475569); }
  .status-suspended { background: var(--arc-chip-red-bg, #fee2e2); color: var(--arc-chip-red-ink, #991b1b); }
  .status-pending { background: var(--arc-chip-amber-bg, #fef3c7); color: var(--arc-chip-amber-ink, #92400e); }
  .status-trial { background: var(--arc-chip-indigo-bg, #e0e7ff); color: var(--arc-chip-indigo-ink, #4338ca); }
  .status-expired { background: var(--arc-chip-red-bg, #fecaca); color: var(--arc-chip-red-ink, #dc2626); }

  .plan-free { background: var(--arc-chip-slate-bg, #f1f5f9); color: var(--arc-chip-slate-ink, #475569); }
  .plan-basic { background: var(--arc-chip-indigo-bg, #e0e7ff); color: var(--arc-chip-indigo-ink, #4338ca); }
  .plan-professional { background: var(--arc-chip-green-bg, #dcfce7); color: var(--arc-chip-green-ink, #166534); }
  .plan-enterprise { background: var(--arc-chip-amber-bg, #fef3c7); color: var(--arc-chip-amber-ink, #92400e); }
  .plan-custom { background: var(--arc-chip-violet-bg, #ede9fe); color: var(--arc-chip-violet-ink, #6d28d9); }

  .card-body {
    padding: 1.5rem;
  }

  .description {
    margin: 0 0 1.5rem 0;
    color: var(--arc-body, #334155);
    line-height: 1.6;
  }

  .description.placeholder {
    color: var(--arc-faint, #94a3b8);
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
    border-bottom: 1px solid var(--arc-line, #f1f5f9);
  }

  .detail-item:last-child {
    border-bottom: none;
  }

  .label {
    font-weight: 600;
    color: var(--arc-muted, #64748b);
    font-size: 0.875rem;
  }

  .value {
    color: var(--arc-body, #334155);
    font-size: 0.875rem;
    text-align: right;
    word-break: break-word;
  }

  .settings-summary {
    border-top: 1px solid var(--arc-line, #e8edf3);
    padding-top: 1rem;
  }

  .settings-summary h4 {
    margin: 0 0 0.75rem 0;
    font-size: 0.875rem;
    font-weight: 700;
    color: var(--arc-muted, #64748b);
    text-transform: uppercase;
    letter-spacing: 0.08em;
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
    color: var(--arc-muted, #64748b);
    font-weight: 500;
  }

  .setting-value {
    color: var(--arc-body, #334155);
    font-weight: 600;
  }

  .card-actions {
    padding: 1rem 1.5rem;
    background: var(--arc-ground, #f8fafc);
    border-top: 1px solid var(--arc-line, #e8edf3);
    display: flex;
    gap: 0.75rem;
    justify-content: flex-end;
  }

  /* Colour/hover come from the global .btn-primary / .btn-danger kit;
     card-footer actions are just a size down from the default. */
  .btn {
    padding: 0.5rem 1rem;
    font-size: 0.875rem;
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
  }

  @media (prefers-reduced-motion: reduce) {
    .tenant-card, .btn {
      transition: none;
    }
    .tenant-card:hover, .card-actions .btn:hover {
      transform: none;
    }
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
