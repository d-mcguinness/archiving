/**
 * Suggested field values per standard and stage, used by the "Fill All
 * Defaults" action on the create pages. One table instead of a copy per page.
 *
 * Values are plain strings or functions of the context, so the table stays
 * declarative while ids, dates and names come from the page.
 */
import type { EntityDefinition, PackageStage } from './types';

export interface DefaultsContext {
  /** Generated package/element id, e.g. "Intake-12-k3j9x1". */
  id: string;
  /** ISO date (yyyy-mm-dd). */
  today: string;
  /** Display name of the selected owner, or "System". */
  userName: string;
  /** The owner's email, when known. */
  userEmail?: string;
  title: string;
  description: string;
}

type Value = string | ((ctx: DefaultsContext) => string);
type Defaults = Record<string, Value>;

const slug = (s: string) => s.toLowerCase().replace(/[^a-z0-9]+/g, '-');

const STAGE_NOUN: Record<PackageStage, string> = {
  intake: 'Intake',
  preservation: 'Preservation',
  release: 'Release'
};
const OAIS_TYPE: Record<PackageStage, string> = { intake: 'SIP', preservation: 'AIP', release: 'DIP' };

/** Defaults that are the same for every stage. */
function common(stage: PackageStage): Record<string, Defaults> {
  return {
    NOARK5: {
      systemID: (c) => c.id,
      title: (c) => c.title || 'New Archive',
      description: (c) => c.description,
      archiveStatus: 'Created',
      documentMedium: 'Electronic archive',
      storageLocation: 'Default storage',
      createdDate: (c) => c.today,
      createdBy: (c) => c.userName,
      closedDate: (c) => c.today,
      closedBy: (c) => c.userName
    },
    'Dublin Core': {
      resourceIdentifier: (c) => c.id,
      resourceType: 'Dataset'
    },
    METS: {
      metsID: (c) => c.id,
      objID: (c) => `OBJ-${c.id}`,
      label: (c) => c.title || 'New METS Document',
      type: 'digital object',
      profile: 'http://www.loc.gov/standards/mets/profiles'
    },
    EAD: {
      eadID: (c) => c.id,
      audience: 'external',
      relatedEncoding: 'Dublin Core',
      lang: 'eng',
      script: 'Latn',
      base: ''
    },
    BagIt: {
      bagName: (c) => slug(c.title || 'new-bag'),
      payloadOxum: '0.0',
      bagSize: '0 KB',
      isComplete: 'true',
      isValid: 'true'
    },
    'ISAD(G)': {
      descriptionID: (c) => c.id,
      levelOfDescription: 'Fonds'
    },
    MODS: {
      modsID: (c) => c.id,
      version: '3.8'
    },
    'E-ARK': {
      packageID: (c) => c.id,
      title: (c) => c.title || `New ${STAGE_NOUN[stage]} package`,
      description: (c) => c.description,
      profile: 'https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml',
      contentInformationType: 'MIXED',
      oaisPackageType: OAIS_TYPE[stage],
      creationDate: (c) => c.today,
      creator: (c) => c.userName,
      preservationLevel: stage === 'release' ? 'reference' : 'full',
      representationCount: '1',
      ...(stage === 'release' ? { accessRights: 'Public' } : {})
    }
  };
}

/** Defaults whose field names or values differ by stage. */
const STAGE_SPECIFIC: Record<PackageStage, Record<string, Defaults>> = {
  intake: {
    OAIS: {
      packageID: (c) => c.id,
      title: (c) => c.title || 'New Intake package',
      description: (c) => c.description,
      submissionDate: (c) => c.today,
      producer: (c) => c.userName,
      producerContact: (c) => c.userEmail ?? '',
      submissionAgreementRef: (c) => `SA-${c.id}`,
      packageType: 'SIP',
      contentInformationType: 'Digital',
      completeness: 'Complete',
      numberOfObjects: '1',
      totalSize: '0'
    },
    PREMIS: {
      objectIdentifierType: 'local',
      objectIdentifierValue: (c) => c.id,
      objectCategory: 'Representation',
      preservationLevelType: 'full',
      preservationLevelValue: 'full preservation',
      preservationLevelRole: 'requirement',
      preservationLevelRationale: 'Default preservation policy',
      preservationLevelDateAssigned: (c) => c.today,
      significantPropertiesType: 'content',
      significantPropertiesValue: 'All content preserved',
      originalName: (c) => c.title || 'Untitled Object'
    }
  },
  preservation: {
    OAIS: {
      aipID: (c) => c.id,
      title: (c) => c.title || 'New Preservation package',
      description: (c) => c.description,
      creationDate: (c) => c.today,
      version: '1.0',
      accessRights: 'Restricted',
      packageType: 'AIP'
    },
    PREMIS: {
      objectIdentifierType: 'local',
      objectIdentifierValue: (c) => c.id,
      objectCategory: 'Representation',
      preservationLevelType: 'full',
      preservationLevelValue: 'full preservation',
      preservationLevelRole: 'requirement',
      preservationLevelRationale: 'Default preservation policy',
      preservationLevelDateAssigned: (c) => c.today,
      significantPropertiesType: 'content',
      significantPropertiesValue: 'All content preserved',
      originalName: (c) => c.title || 'Untitled Object'
    }
  },
  release: {
    OAIS: {
      dipID: (c) => c.id,
      aipID: (c) => c.id,
      title: (c) => c.title || 'New Release package',
      description: (c) => c.description,
      creationDate: (c) => c.today,
      requestDate: (c) => c.today,
      consumer: (c) => c.userName,
      version: '1.0',
      accessRights: 'Public',
      packageType: 'DIP'
    },
    PREMIS: {
      objectIdentifierType: 'local',
      objectIdentifierValue: (c) => c.id,
      objectCategory: 'Representation',
      preservationLevelType: 'reference',
      preservationLevelValue: 'reference copy',
      preservationLevelRole: 'dissemination',
      preservationLevelRationale: 'Default preservation policy',
      preservationLevelDateAssigned: (c) => c.today,
      significantPropertiesType: 'content',
      significantPropertiesValue: 'All content preserved',
      originalName: (c) => c.title || 'Untitled Object'
    }
  }
};

/** Resolved default values for `standardKey` (display key, e.g. "ISAD(G)") at `stage`. */
export function getStageDefaults(
  standardKey: string,
  stage: PackageStage,
  ctx: DefaultsContext
): Record<string, string> {
  const table = STAGE_SPECIFIC[stage][standardKey] ?? common(stage)[standardKey];
  if (!table) return {};
  const out: Record<string, string> = {};
  for (const [name, v] of Object.entries(table)) {
    out[name] = typeof v === 'function' ? v(ctx) : v;
  }
  return out;
}

/**
 * Copy `defaults` into `values` for the fields `entity` actually declares.
 * Returns the same object (reassign it in the page to trigger reactivity).
 */
export function applyDefaults(
  entity: EntityDefinition | null | undefined,
  values: Record<string, string>,
  defaults: Record<string, string>
): Record<string, string> {
  for (const f of entity?.fields ?? []) {
    const v = defaults[f.name];
    if (v !== undefined) values[f.name] = v;
  }
  return values;
}
