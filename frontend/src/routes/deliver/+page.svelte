<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
</script>

<svelte:head>
  <title>Deliver - Arcana</title>
</svelte:head>

<StandardsPage
  title="Deliver"
  oaisStep="OAIS Step 3"
  heroDescription="The delivery function produces Dissemination Information Packages (DIPs) tailored to end users, with standard-compliant metadata and granular access controls for secure distribution."
  packageType="dip"
  createPath="/dip/create"
  listPath="/dip"
  accentColor="#f97316"
  steps={[
    { title: 'Select Standard', description: 'Choose the archival standard that matches your dissemination requirements.' },
    { title: 'Configure Access', description: 'Define access restrictions, user permissions, and delivery format for the target audience.' },
    { title: 'Generate & Distribute', description: 'Produce DIPs with compliant metadata, ready for download or integration with external systems.' },
  ]}
  capabilities={[
    'User-tailored access packages',
    'Granular access controls and permissions',
    'Standard-compliant delivery metadata',
    'Multi-format export support',
  ]}
/>
