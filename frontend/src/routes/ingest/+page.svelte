<script lang="ts">
  import StandardsPage from '$lib/components/StandardsPage.svelte';
</script>

<svelte:head>
  <title>Ingest - Arcana</title>
</svelte:head>

<StandardsPage
  title="Ingest"
  oaisStep="Submission Packages"
  heroDescription="The ingest workflow accepts Intake packages (SIPs) from producers. Choose from 10 archival standards — NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), MODS, and E-ARK — to create standard-compliant packages ready for preservation."
  packageType="sip"
  createPath="/intake/create"
  listPath="/intake"
  accentColor="#ec4899"
  steps={[
    { title: 'Select Standard', description: 'Choose the archival standard that fits your content. Each standard provides its own entity model, field definitions, and validation rules.' },
    { title: 'Build Package', description: 'Define metadata elements using schema-driven forms. Create nested hierarchies, attach documents, and fill standard-specific fields.' },
    { title: 'Validate & Submit', description: 'Required fields are validated per standard. Packages are structured and queued for archival storage.' },
  ]}
  capabilities={[
    'All 10 archival standards supported',
    'Schema-driven forms with per-standard field definitions',
    'Document attachment and cloud storage linking',
    'Nested hierarchical element structures',
    'Standard-specific validation and defaults',
    'Multi-tenant with role-based access control',
  ]}
/>
