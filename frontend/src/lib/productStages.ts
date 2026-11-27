// Content for the three public product pages (/product/intake, /product/preservation,
// /product/release). One entry per stage of the OAIS workflow, in pipeline order;
// ProductStagePage renders whichever one the route asks for.

export interface ProductStage {
  /** URL segment under /product. */
  slug: 'intake' | 'preservation' | 'release';
  /** Accent used for this stage across the product pages (blue / indigo / orange). */
  tone: 'ingest' | 'preserve' | 'release';
  kicker: string;
  stage: string;
  pkg: string;
  abbr: string;
  headline: string;
  lede: string;
  /** Plain-text summary for the page's meta description. */
  description: string;
  subhead: string;
  capabilities: string[];
  note: string;
  /** Status the pipeline shows once a record has passed this stage. */
  state: string;
  produces: string;
  key: string;
  /** The standards deep-dive page for this stage. */
  href: string;
  cta: string;
}

export const productStages: ProductStage[] = [
  {
    slug: 'intake',
    tone: 'ingest',
    kicker: 'Ingest',
    stage: 'Intake',
    pkg: 'Intake package',
    abbr: 'SIP',
    headline: 'Accept submissions, not just files',
    lede:
      'The ingest workflow accepts Intake packages from producers and turns them into structured, standard-compliant records ready for preservation. Capture metadata at the door — never re-key it later.',
    description:
      'Arcana Ingest accepts Intake packages from producers and turns them into structured, standard-compliant records across ten open archival standards.',
    subhead: 'Structured capture from the first submission',
    capabilities: [
      'Schema-driven forms with per-standard field definitions',
      'Document attachment and cloud storage linking',
      'Nested hierarchical element structures',
      'Standard-specific validation and defaults'
    ],
    note: 'Build to NOARK 5.5 and E-ARK conformance from the first submission — validation runs per standard, not as an afterthought.',
    state: 'validated',
    produces: 'Validated submissions',
    key: 'Schema-driven capture across 10 standards',
    href: '/ingest',
    cta: 'Explore Ingest'
  },
  {
    slug: 'preservation',
    tone: 'preserve',
    kicker: 'Preserve',
    stage: 'Preservation',
    pkg: 'Preservation package',
    abbr: 'AIP',
    headline: 'Storage that proves its own integrity',
    lede:
      'The preservation workflow seals Preservation packages for long-term storage with provenance tracking, integrity verification, and format-migration readiness. Every preservation action is captured, so the chain of custody is never in doubt.',
    description:
      'Arcana Preserve seals Preservation packages for long-term storage with provenance tracking, fixity verification and format-migration readiness.',
    subhead: 'A chain of custody you can show',
    capabilities: [
      'Provenance tracking and audit trails',
      'Integrity verification with fixity metadata',
      'Format migration and representation support',
      'Standard-specific preservation metadata'
    ],
    note: 'Fixity, provenance, and audit trails are first-class — the foundations of a trustworthy repository under the OAIS reference model.',
    state: 'sealed',
    produces: 'Sealed long-term records',
    key: 'Provenance, fixity & format migration',
    href: '/preserve',
    cta: 'Explore Preserve'
  },
  {
    slug: 'release',
    tone: 'release',
    kicker: 'Release',
    stage: 'Release',
    pkg: 'Release package',
    abbr: 'DIP',
    headline: 'Deliver to every audience',
    lede:
      'The release workflow produces Release packages for end-user access, with metadata and access controls tailored to each audience. Disseminate the same archived record many ways without ever touching the preserved original.',
    description:
      'Arcana Release produces Release packages for end-user access, with metadata and access controls tailored to each audience.',
    subhead: 'One preserved record, many access copies',
    capabilities: [
      'User-tailored access packages',
      'Granular access controls and permissions',
      'Standard-compliant delivery metadata',
      'Multi-format export support'
    ],
    note: 'Release packages carry standard-compliant metadata, ready for download, integration, or distribution to external systems.',
    state: 'ready',
    produces: 'Tailored access copies',
    key: 'Access control & multi-format export',
    href: '/deliver',
    cta: 'Explore Release'
  }
];

export const standards = ['NOARK5', 'OAIS', 'PREMIS', 'Dublin Core', 'METS', 'EAD', 'BagIt', 'ISAD(G)', 'MODS', 'E-ARK'];
/** Compliance-grade standards that generate validated, regulator-ready packages. */
export const premiumStandards = ['NOARK5', 'E-ARK'];
