<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
</script>

<svelte:head>
  <title>Ingest - Arcana</title>
</svelte:head>

<StandardsPage
  title="Ingest"
  oaisStep="OAIS Step 1"
  heroDescription="The ingest function accepts Submission Information Packages (SIPs) from producers, validates them against archival standards, and prepares content for long-term preservation."
  packageType="sip"
  createPath="/sip/create"
  listPath="/sip"
  accentColor="#3b82f6"
  steps={[
    { title: 'Select Standard', description: 'Choose from 10 international archival standards like NOARK5, OAIS, PREMIS, E-ARK, and more.' },
    { title: 'Build Package', description: 'Define metadata elements, attach documents, and structure your submission package using standard-compliant schemas.' },
    { title: 'Validate & Submit', description: 'Automated validation ensures compliance before submission. Packages are verified and queued for preservation.' },
  ]}
  capabilities={[
    'Schema-driven forms with dynamic field generation',
    'Document attachment and linking',
    'Nested hierarchical element structures',
    'Multi-tenant with role-based access control',
  ]}
/>
