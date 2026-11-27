<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
  import ProductStagePage from '$lib/components/ProductStagePage.svelte';
  import { productStages } from '$lib/productStages';
  import { auth } from '$lib/stores/authStore';

  const preservation = productStages.find((s) => s.slug === 'preservation')!;
</script>

<svelte:head>
  {#if $auth.isLoggedIn}<title>Preserve - Arcana</title>{/if}
</svelte:head>

{#if !$auth.isLoggedIn}
  <!-- Signed out: the Preservation product page. -->
  <ProductStagePage stage={preservation} />
{:else}
  <!-- Signed in: pick a standard to create a Preservation package. -->
  <StandardsPage
    title="Preserve"
    oaisStep="Archival Packages"
    packageType="aip"
    createPath="/preservation/create"
    listPath="/preservation"
    accentColor="#6366f1"
  />
{/if}
