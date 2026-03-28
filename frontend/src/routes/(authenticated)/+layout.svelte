<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  $: if (browser && !$auth.isLoggedIn) {
    goto('/');
  }
</script>

{#if $auth.isLoggedIn}
  <slot />
{/if}
