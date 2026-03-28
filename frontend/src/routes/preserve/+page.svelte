<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
</script>

<svelte:head>
  <title>Preserve - Arcana</title>
</svelte:head>

<StandardsPage
  title="Preserve"
  oaisStep="OAIS Step 2"
  heroDescription="The preservation function ensures long-term storage and integrity of Archival Information Packages (AIPs), maintaining provenance, format migration support, and fixity verification."
  packageType="aip"
  createPath="/aip/create"
  listPath="/aip"
  accentColor="#6366f1"
  steps={[
    { title: 'Select Standard', description: 'Choose from 10 archival standards to define the structure of your Archival Information Package.' },
    { title: 'Define Preservation Metadata', description: 'Capture provenance, rights, technical metadata, and preservation actions using standard-compliant schemas.' },
    { title: 'Store & Verify', description: 'AIPs are stored with integrity checks, fixity verification, and full audit trails for long-term access.' },
  ]}
  capabilities={[
    'Provenance tracking and audit trails',
    'Integrity verification and fixity checks',
    'Format migration support',
    'Standard-compliant preservation metadata',
  ]}
/>