/**
 * UI metadata for the archive standards: keys, labels and routing. The
 * definitions themselves (entities, fields, which entity each stage uses)
 * come from the backend through $lib/schemes.
 */

export interface StandardDefinition {
  key: string;
  label: string;
  file: string;
  graphql: string;
  intakeLabel: string;
  preservationLabel: string;
  releaseLabel: string;
}

export const standards: StandardDefinition[] = [
  { key: 'NOARK5', label: 'NOARK5', file: 'noark5.json', graphql: 'NOARK5', intakeLabel: 'Archive (Arkiv)', preservationLabel: 'Archive (Arkiv)', releaseLabel: 'Archive (Arkiv)' },
  { key: 'OAIS', label: 'OAIS', file: 'oais.json', graphql: 'OAIS', intakeLabel: 'Submission Information Package', preservationLabel: 'Archival Information Package', releaseLabel: 'Dissemination Information Package' },
  { key: 'PREMIS', label: 'PREMIS', file: 'premis.json', graphql: 'PREMIS', intakeLabel: 'Preservation Object', preservationLabel: 'Preservation Object', releaseLabel: 'Preservation Object' },
  { key: 'Dublin Core', label: 'Dublin Core', file: 'dublincore.json', graphql: 'DUBLIN_CORE', intakeLabel: 'Resource', preservationLabel: 'Resource', releaseLabel: 'Resource' },
  { key: 'METS', label: 'METS', file: 'mets.json', graphql: 'METS', intakeLabel: 'METS Document', preservationLabel: 'METS Document', releaseLabel: 'METS Document' },
  { key: 'EAD', label: 'EAD', file: 'ead.json', graphql: 'EAD', intakeLabel: 'Finding Aid (EAD)', preservationLabel: 'Finding Aid (EAD)', releaseLabel: 'Finding Aid (EAD)' },
  { key: 'BagIt', label: 'BagIt', file: 'bagit.json', graphql: 'BAGIT', intakeLabel: 'Bag', preservationLabel: 'Bag', releaseLabel: 'Bag' },
  { key: 'ISAD(G)', label: 'ISAD(G)', file: 'isadg.json', graphql: 'ISADG', intakeLabel: 'Archival Description', preservationLabel: 'Archival Description', releaseLabel: 'Archival Description' },
  { key: 'MODS', label: 'MODS', file: 'mods.json', graphql: 'MODS', intakeLabel: 'MODS Record', preservationLabel: 'MODS Record', releaseLabel: 'MODS Record' },
  { key: 'E-ARK', label: 'E-ARK', file: 'eark.json', graphql: 'EARK', intakeLabel: 'Archival Information Package', preservationLabel: 'Archival Information Package', releaseLabel: 'Dissemination Information Package' },
];

/** Maps GraphQL enum value to display key (e.g. 'EARK' → 'E-ARK'). */
export const graphqlToKey: Record<string, string> = Object.fromEntries(
  standards.map(s => [s.graphql, s.key])
);

/** Maps display key to standard definition. */
export const standardsByKey: Record<string, StandardDefinition> = Object.fromEntries(
  standards.map(s => [s.key, s])
);
