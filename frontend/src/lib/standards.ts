/**
 * Shared archive standard definitions used across SIP, AIP, DIP pages.
 */

export interface StandardDefinition {
  key: string;
  label: string;
  file: string;
  graphql: string;
  sipEntity: string;
  sipLabel: string;
  aipEntity: string;
  aipLabel: string;
  dipEntity: string;
  dipLabel: string;
}

export const standards: StandardDefinition[] = [
  { key: 'NOARK5', label: 'NOARK5', file: 'noark5.json', graphql: 'NOARK5', sipEntity: 'Archive', sipLabel: 'Archive (Arkiv)', aipEntity: 'Archive', aipLabel: 'Archive (Arkiv)', dipEntity: 'Archive', dipLabel: 'Archive (Arkiv)' },
  { key: 'OAIS', label: 'OAIS', file: 'oais.json', graphql: 'OAIS', sipEntity: 'Submission Information Package', sipLabel: 'Submission Information Package', aipEntity: 'Archival Information Package', aipLabel: 'Archival Information Package', dipEntity: 'Archival Information Package', dipLabel: 'Dissemination Information Package' },
  { key: 'PREMIS', label: 'PREMIS', file: 'premis.json', graphql: 'PREMIS', sipEntity: 'Object', sipLabel: 'Preservation Object', aipEntity: 'Object', aipLabel: 'Preservation Object', dipEntity: 'Object', dipLabel: 'Preservation Object' },
  { key: 'Dublin Core', label: 'Dublin Core', file: 'dublincore.json', graphql: 'DUBLIN_CORE', sipEntity: 'Resource', sipLabel: 'Resource', aipEntity: 'Resource', aipLabel: 'Resource', dipEntity: 'Resource', dipLabel: 'Resource' },
  { key: 'METS', label: 'METS', file: 'mets.json', graphql: 'METS', sipEntity: 'METS Document', sipLabel: 'METS Document', aipEntity: 'METS Document', aipLabel: 'METS Document', dipEntity: 'METS Document', dipLabel: 'METS Document' },
  { key: 'EAD', label: 'EAD', file: 'ead.json', graphql: 'EAD', sipEntity: 'EAD', sipLabel: 'Finding Aid (EAD)', aipEntity: 'EAD', aipLabel: 'Finding Aid (EAD)', dipEntity: 'EAD', dipLabel: 'Finding Aid (EAD)' },
  { key: 'BagIt', label: 'BagIt', file: 'bagit.json', graphql: 'BAGIT', sipEntity: 'Bag', sipLabel: 'Bag', aipEntity: 'Bag', aipLabel: 'Bag', dipEntity: 'Bag', dipLabel: 'Bag' },
  { key: 'ISAD(G)', label: 'ISAD(G)', file: 'isadg.json', graphql: 'ISADG', sipEntity: 'Archival Description', sipLabel: 'Archival Description', aipEntity: 'Archival Description', aipLabel: 'Archival Description', dipEntity: 'Archival Description', dipLabel: 'Archival Description' },
  { key: 'MODS', label: 'MODS', file: 'mods.json', graphql: 'MODS', sipEntity: 'MODS', sipLabel: 'MODS Record', aipEntity: 'MODS', aipLabel: 'MODS Record', dipEntity: 'MODS', dipLabel: 'MODS Record' },
  { key: 'E-ARK', label: 'E-ARK', file: 'eark.json', graphql: 'EARK', sipEntity: 'Archival Information Package', sipLabel: 'Archival Information Package', aipEntity: 'Archival Information Package', aipLabel: 'Archival Information Package', dipEntity: 'Dissemination Information Package', dipLabel: 'Dissemination Information Package' },
];

/** Maps GraphQL enum value (e.g. 'NOARK5', 'EARK') to its schema JSON filename. */
export const standardFileMap: Record<string, string> = Object.fromEntries(
  standards.map(s => [s.graphql, s.file])
);

/** Maps GraphQL enum value to display key (e.g. 'EARK' → 'E-ARK'). */
export const graphqlToKey: Record<string, string> = Object.fromEntries(
  standards.map(s => [s.graphql, s.key])
);

/** Maps display key to standard definition. */
export const standardsByKey: Record<string, StandardDefinition> = Object.fromEntries(
  standards.map(s => [s.key, s])
);
