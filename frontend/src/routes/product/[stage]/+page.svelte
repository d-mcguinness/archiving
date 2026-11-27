<script lang="ts">
  import ProductStagePage from '$lib/components/ProductStagePage.svelte';
  import type { ProductStage } from '$lib/productStages';

  export let data: { stage: ProductStage };
</script>

<ProductStagePage stage={data.stage} />
