<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
</script>

<svelte:head>
  <title>Deliver - Arcana</title>
</svelte:head>

<StandardsPage
  title="Deliver"
  oaisStep="Dissemination Packages"
  heroDescription="The delivery workflow produces Release packages (DIPs) for end-user access. Create Releases using any of 10 archival standards with tailored metadata, access controls, and multi-format export."
  packageType="dip"
  createPath="/release/create"
  listPath="/release"
  accentColor="#f97316"
  steps={[
    { title: 'Select Standard', description: 'Choose the archival standard that matches your dissemination requirements. Each standard defines its own access metadata and delivery structure.' },
    { title: 'Configure Access', description: 'Define access restrictions, delivery format, and target audience using standard-compliant entity fields.' },
    { title: 'Generate & Distribute', description: 'Produce Releases with compliant metadata, ready for download, integration, or distribution to external systems.' },
  ]}
  capabilities={[
    'All 10 archival standards supported',
    'User-tailored access packages',
    'Granular access controls and permissions',
    'Standard-compliant delivery metadata',
    'Multi-format export support',
    'Multi-tenant with role-based access control',
  ]}
/>
