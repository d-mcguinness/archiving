<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
</script>

<svelte:head>
  <title>Preserve - Arcana</title>
</svelte:head>

<StandardsPage
  title="Preserve"
  oaisStep="Archival Packages"
  heroDescription="The preservation workflow creates Archival Information Packages (AIPs) for long-term storage. Build AIPs using any of 10 archival standards with provenance tracking, integrity verification, and format migration support."
  packageType="aip"
  createPath="/aip/create"
  listPath="/aip"
  accentColor="#6366f1"
  steps={[
    { title: 'Select Standard', description: 'Choose the archival standard for your AIP. Each standard defines preservation metadata, provenance models, and structural requirements.' },
    { title: 'Define Preservation Metadata', description: 'Capture provenance, rights, technical metadata, and preservation actions using standard-compliant entity schemas.' },
    { title: 'Store & Verify', description: 'AIPs are structured with integrity checks and full audit trails, ready for long-term archival storage.' },
  ]}
  capabilities={[
    'All 10 archival standards supported',
    'Provenance tracking and audit trails',
    'Integrity verification with fixity metadata',
    'Format migration and representation support',
    'Standard-specific preservation metadata',
    'Multi-tenant with role-based access control',
  ]}
/>
