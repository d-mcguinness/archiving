<script lang="ts">
  // Re-use the archive update page component
  import ArchiveUpdatePage from '../../../../../archives/update/[id]/+page.svelte';

  export let data: { tenantId: string; id: string };
</script>

<ArchiveUpdatePage {data} />
