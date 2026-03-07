<script lang="ts">
  import { onMount } from 'svelte';
  import { goto } from '$app/navigation';

  onMount(() => {
    const role = localStorage.getItem('auth_role');
    const tenantId = localStorage.getItem('auth_tenantId');

    if (role === 'ADMIN') {
      goto('/admin/archives', { replaceState: true });
    } else if (role === 'TENANT' && tenantId) {
      goto(`/tenants/${tenantId}/archives`, { replaceState: true });
    } else {
      goto('/login', { replaceState: true });
    }
  });
</script>

<div class="redirect">
  <p>Redirecting...</p>
</div>

<style>
  .redirect {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
    color: #64748b;
  }
</style>
