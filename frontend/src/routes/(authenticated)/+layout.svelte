<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';
  import { loadSchemes } from '$lib/schemes';

  // Every page under here reads the archive standard definitions synchronously;
  // fetch them once up front so pages never see an empty cache.
  const schemesReady = loadSchemes();

  $: if (browser && !$auth.isLoggedIn) {
    goto('/');
  }
</script>

{#if $auth.isLoggedIn}
  {#await schemesReady}
    <div class="schemes-loading" aria-busy="true"><div class="spinner"></div></div>
  {:then}
    <slot />
  {:catch e}
    <div class="schemes-error" role="alert">
      <p>Could not load the archive standard definitions: {e instanceof Error ? e.message : 'unknown error'}</p>
      <button type="button" class="btn-secondary" on:click={() => location.reload()}>Retry</button>
    </div>
  {/await}
{/if}

<style>
  .schemes-loading { display: flex; justify-content: center; padding: 4rem 1rem; }
  .schemes-loading .spinner { width: 2.5rem; height: 2.5rem; }
  .schemes-error { max-width: 36rem; margin: 3rem auto; padding: 1.5rem; text-align: center; color: var(--arc-alert-red-ink); background: var(--arc-alert-red-bg); border: 1px solid var(--arc-alert-red-border); border-radius: 0.75rem; }
</style>
