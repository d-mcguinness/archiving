<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  /** Allowed roles. Empty array = any authenticated user. */
  export let roles: string[] = [];
  /** What to do when role doesn't match: hide content, show fallback slot, or redirect. */
  export let fallback: 'hide' | 'slot' | 'redirect' = 'hide';
  /** Redirect target when fallback is 'redirect'. */
  export let redirectTo = '/login';
  /** If true, require user to be logged in (default true). */
  export let requireAuth = true;

  $: allowed = requireAuth
    ? $auth.isLoggedIn && (roles.length === 0 || roles.includes($auth.role))
    : roles.length === 0 || roles.includes($auth.role);

  $: if (browser && !allowed && fallback === 'redirect') {
    goto(redirectTo);
  }
</script>

{#if allowed}
  <slot />
{:else if fallback === 'slot'}
  <slot name="fallback" />
{/if}
