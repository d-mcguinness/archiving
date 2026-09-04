/**
 * Shared archive standard definitions used across SIP, AIP, DIP pages.
 */

export interface StandardDefinition {
  key: string;
  label: string;
  file: string;
  graphql: string;
  intakeEntity: string;
  intakeLabel: string;
  preservationEntity: string;
  preservationLabel: string;
  releaseEntity: string;
  releaseLabel: string;
}

export const standards: StandardDefinition[] = [
  { key: 'NOARK5', label: 'NOARK5', file: 'noark5.json', graphql: 'NOARK5', intakeEntity: 'Archive', intakeLabel: 'Archive (Arkiv)', preservationEntity: 'Archive', preservationLabel: 'Archive (Arkiv)', releaseEntity: 'Archive', releaseLabel: 'Archive (Arkiv)' },
  { key: 'OAIS', label: 'OAIS', file: 'oais.json', graphql: 'OAIS', intakeEntity: 'Submission Information Package', intakeLabel: 'Submission Information Package', preservationEntity: 'Archival Information Package', preservationLabel: 'Archival Information Package', releaseEntity: 'Archival Information Package', releaseLabel: 'Dissemination Information Package' },
  { key: 'PREMIS', label: 'PREMIS', file: 'premis.json', graphql: 'PREMIS', intakeEntity: 'Object', intakeLabel: 'Preservation Object', preservationEntity: 'Object', preservationLabel: 'Preservation Object', releaseEntity: 'Object', releaseLabel: 'Preservation Object' },
  { key: 'Dublin Core', label: 'Dublin Core', file: 'dublincore.json', graphql: 'DUBLIN_CORE', intakeEntity: 'Resource', intakeLabel: 'Resource', preservationEntity: 'Resource', preservationLabel: 'Resource', releaseEntity: 'Resource', releaseLabel: 'Resource' },
  { key: 'METS', label: 'METS', file: 'mets.json', graphql: 'METS', intakeEntity: 'METS Document', intakeLabel: 'METS Document', preservationEntity: 'METS Document', preservationLabel: 'METS Document', releaseEntity: 'METS Document', releaseLabel: 'METS Document' },
  { key: 'EAD', label: 'EAD', file: 'ead.json', graphql: 'EAD', intakeEntity: 'EAD', intakeLabel: 'Finding Aid (EAD)', preservationEntity: 'EAD', preservationLabel: 'Finding Aid (EAD)', releaseEntity: 'EAD', releaseLabel: 'Finding Aid (EAD)' },
  { key: 'BagIt', label: 'BagIt', file: 'bagit.json', graphql: 'BAGIT', intakeEntity: 'Bag', intakeLabel: 'Bag', preservationEntity: 'Bag', preservationLabel: 'Bag', releaseEntity: 'Bag', releaseLabel: 'Bag' },
  { key: 'ISAD(G)', label: 'ISAD(G)', file: 'isadg.json', graphql: 'ISADG', intakeEntity: 'Archival Description', intakeLabel: 'Archival Description', preservationEntity: 'Archival Description', preservationLabel: 'Archival Description', releaseEntity: 'Archival Description', releaseLabel: 'Archival Description' },
  { key: 'MODS', label: 'MODS', file: 'mods.json', graphql: 'MODS', intakeEntity: 'MODS', intakeLabel: 'MODS Record', preservationEntity: 'MODS', preservationLabel: 'MODS Record', releaseEntity: 'MODS', releaseLabel: 'MODS Record' },
  { key: 'E-ARK', label: 'E-ARK', file: 'eark.json', graphql: 'EARK', intakeEntity: 'Archival Information Package', intakeLabel: 'Archival Information Package', preservationEntity: 'Archival Information Package', preservationLabel: 'Archival Information Package', releaseEntity: 'Dissemination Information Package', releaseLabel: 'Dissemination Information Package' },
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
