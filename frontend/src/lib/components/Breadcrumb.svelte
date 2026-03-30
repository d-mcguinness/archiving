<script lang="ts">
  import { auth } from '$lib/stores/authStore';

  /**
   * Context object — provide IDs/names for each level in the hierarchy.
   * The component builds the breadcrumb chain based on the user's role
   * and which context fields are present.
   *
   * Hierarchy: Admin > Tenants > {Tenant} > [section] > {entity} > ...
   * ADMIN sees everything. TENANT sees from their tenant down. USER sees from their profile down.
   */
  export let context: {
    tenantId?: string;
    tenantName?: string;
    userId?: string;
    userName?: string;
  } = {};

  /** Additional items appended to the end (e.g. "Documents", "Extract"). Last item = current page. */
  export let items: { label: string; href?: string }[] = [];

  $: role = $auth.role;

  $: segments = buildSegments(role, context, items);

  function buildSegments(
    role: string,
    ctx: typeof context,
    extraItems: typeof items
  ): { label: string; href?: string }[] {
    const segs: { label: string; href?: string }[] = [];

    if (role === 'ADMIN') {
      segs.push({ label: 'Admin', href: '/admin' });

      if (ctx.tenantId) {
        segs.push({ label: 'Tenants', href: '/admin/tenants' });
        segs.push({ label: ctx.tenantName || 'Tenant', href: `/tenants/${ctx.tenantId}` });
      }
    } else if (role === 'TENANT') {
      if (ctx.tenantId) {
        segs.push({ label: ctx.tenantName || 'Home', href: `/tenants/${ctx.tenantId}` });
      }
    }
    // USER role: start from user level (no tenant/admin prefix)

    if (ctx.userId) {
      if (role !== 'USER') {
        // ADMIN and TENANT see the Users list link
        const usersHref = ctx.tenantId ? `/tenants/${ctx.tenantId}/users` : undefined;
        segs.push({ label: 'Users', href: usersHref });
      }
      const userHref = ctx.tenantId ? `/tenants/${ctx.tenantId}/users/${ctx.userId}` : undefined;
      segs.push({ label: ctx.userName || 'User', href: userHref });
    }

    // Append extra items
    for (const item of extraItems) {
      segs.push({ ...item });
    }

    return segs;
  }
</script>

{#if segments.length > 0}
  <nav class="breadcrumb" aria-label="Breadcrumb">
    {#each segments as seg, i}
      {#if i > 0}
        <span class="sep">/</span>
      {/if}
      {#if seg.href && i < segments.length - 1}
        <a href={seg.href}>{seg.label}</a>
      {:else}
        <span class="current">{seg.label}</span>
      {/if}
    {/each}
  </nav>
{/if}

<style>
  .breadcrumb {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 1.5rem;
    font-size: 0.875rem;
    flex-wrap: wrap;
  }

  .breadcrumb a {
    color: #3b82f6;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.2s;
  }

  .breadcrumb a:hover {
    color: #2563eb;
  }

  .sep {
    color: #94a3b8;
  }

  .current {
    color: #64748b;
    font-weight: 600;
  }
</style>
