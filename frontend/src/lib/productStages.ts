// Content for the three public product pages. One entry per stage of the OAIS
// workflow, in pipeline order; ProductStagePage renders whichever one the route
// asks for. Intake lives at /ingest and Preservation at /preserve (both shown to
// signed-out visitors); Release is under /product.

export interface ProductStage {
  slug: 'intake' | 'preservation' | 'release';
  /** Where this stage's page lives. */
  path: string;
  /** Accent used for this stage across the product pages (blue / indigo / orange). */
  tone: 'ingest' | 'preserve' | 'release';
  kicker: string;
  stage: string;
  pkg: string;
  headline: string;
  /** One sentence under the headline. */
  lede: string;
  /** Plain-text summary for the page's meta description. */
  description: string;
  /** Heading of the "how it works" section. */
  subhead: string;
  /** The stage in three steps; `icon` names an icon in ProductStagePage. */
  steps: { icon: string; title: string; text: string }[];
  capabilities: { icon: string; label: string }[];
  /** Status the pipeline shows once a record has passed this stage. */
  state: string;
}

export const productStages: ProductStage[] = [
  {
    slug: 'intake',
    path: '/ingest',
    tone: 'ingest',
    kicker: 'Ingest',
    stage: 'Intake',
    pkg: 'Intake package',
    headline: 'Accept submissions, not just files',
    lede: 'Turn what producers send you into structured, standard-compliant records, with the metadata captured at the door.',
    description:
      'Arcana Ingest accepts Intake packages from producers and turns them into structured, standard-compliant records across ten open archival standards.',
    subhead: 'From submission to record in three steps',
    steps: [
      { icon: 'layers', title: 'Pick a standard', text: 'Choose the one that fits your content.' },
      { icon: 'form', title: 'Build the package', text: 'Fill in guided forms and attach documents.' },
      { icon: 'check', title: 'Validate and submit', text: 'Checked against the standard, then queued.' }
    ],
    capabilities: [
      { icon: 'form', label: 'Schema-driven forms' },
      { icon: 'clip', label: 'Document attachments' },
      { icon: 'tree', label: 'Nested hierarchies' },
      { icon: 'shield', label: 'Per-standard validation' }
    ],
    state: 'validated'
  },
  {
    slug: 'preservation',
    path: '/preserve',
    tone: 'preserve',
    kicker: 'Preserve',
    stage: 'Preservation',
    pkg: 'Preservation package',
    headline: 'Storage that proves its own integrity',
    lede: 'Seal records for the long term, with every preservation action captured so the chain of custody is never in doubt.',
    description:
      'Arcana Preserve seals Preservation packages for long-term storage with provenance tracking, fixity verification and format-migration readiness.',
    subhead: 'A chain of custody in three steps',
    steps: [
      { icon: 'lock', title: 'Seal the package', text: 'Records are locked for long-term storage.' },
      { icon: 'shield', title: 'Verify integrity', text: 'Fixity checks show nothing has changed.' },
      { icon: 'history', title: 'Track every action', text: 'Each change is added to the audit trail.' }
    ],
    capabilities: [
      { icon: 'history', label: 'Provenance and audit trail' },
      { icon: 'shield', label: 'Fixity verification' },
      { icon: 'refresh', label: 'Format migration' },
      { icon: 'tag', label: 'Preservation metadata' }
    ],
    state: 'sealed'
  },
  {
    slug: 'release',
    path: '/product/release',
    tone: 'release',
    kicker: 'Release',
    stage: 'Release',
    pkg: 'Release package',
    headline: 'Deliver to every audience',
    lede: 'Share the same archived record many ways, without ever touching the preserved original.',
    description:
      'Arcana Release produces Release packages for end-user access, with metadata and access controls tailored to each audience.',
    subhead: 'From archive to audience in three steps',
    steps: [
      { icon: 'users', title: 'Choose the audience', text: 'Decide who the package is for.' },
      { icon: 'key', title: 'Set the access', text: 'Tailor metadata and permissions.' },
      { icon: 'download', title: 'Export', text: 'Deliver in the format they need.' }
    ],
    capabilities: [
      { icon: 'box', label: 'Tailored access packages' },
      { icon: 'key', label: 'Granular permissions' },
      { icon: 'tag', label: 'Delivery metadata' },
      { icon: 'download', label: 'Multi-format export' }
    ],
    state: 'ready'
  }
];

export const standards = ['NOARK5', 'OAIS', 'PREMIS', 'Dublin Core', 'METS', 'EAD', 'BagIt', 'ISAD(G)', 'MODS', 'E-ARK'];
/** Compliance-grade standards that generate validated, regulator-ready packages. */
export const premiumStandards = ['NOARK5', 'E-ARK'];
