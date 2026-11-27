<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
  import ProductStagePage from '$lib/components/ProductStagePage.svelte';
  import { productStages } from '$lib/productStages';
  import { auth } from '$lib/stores/authStore';

  const intake = productStages.find((s) => s.slug === 'intake')!;
</script>

<svelte:head>
  {#if $auth.isLoggedIn}<title>Ingest - Arcana</title>{/if}
</svelte:head>

{#if !$auth.isLoggedIn}
  <!-- Signed out: the Intake product page. -->
  <ProductStagePage stage={intake} />
{:else}
  <!-- Signed in: pick a standard to create an Intake package. -->
  <StandardsPage
    title="Ingest"
    oaisStep="Submission Packages"
    packageType="sip"
    createPath="/intake/create"
    listPath="/intake"
    accentColor="#ec4899"
  />
{/if}
