/**
 * Typed access to the archive standard definitions.
 *
 * The backend is the single source (classpath:schemes/*.json, served by the
 * `allStandardDefinitions` query), so the forms here and the generators there
 * read the same field names. The definitions are fetched once per app load
 * with {@link loadSchemes} and cached; every lookup below is then synchronous.
 * The authenticated layout awaits the load before rendering any page, and
 * the public standards pages await it in onMount.
 *
 * Lookups accept any of the identifiers in circulation — the display key
 * ("E-ARK"), the GraphQL enum value ("EARK") or the legacy file name
 * ("eark.json").
 */
import { client } from '$lib/apollo';
import { ALL_STANDARD_DEFINITIONS } from '$lib/graphql/queries';
import { standards, type StandardDefinition } from '$lib/standards';
import type {
  EntityDefinition,
  FieldDefinition,
  FieldInput,
  PackageStage,
  SchemeDefinition
} from './types';

export type { EntityDefinition, FieldDefinition, FieldInput, FieldOption, FieldType, PackageStage, SchemeDefinition } from './types';
export { isEnumerated } from './types';
export { getStageDefaults, applyDefaults, type DefaultsContext } from './defaults';

/** GraphQL enum value ("EARK") → definition */
const byStandard = new Map<string, SchemeDefinition>();
let loading: Promise<void> | null = null;

/** Exactly what the `allStandardDefinitions` query returns (vocabularies still JSON-encoded). */
interface WireDefinition {
  standard: string;
  displayName: string;
  fullName: string;
  reference?: string | null;
  description?: string | null;
  version?: string | null;
  xsdReference?: string | null;
  url?: string | null;
  stageEntities: Record<PackageStage, string>;
  entities: EntityDefinition[];
  vocabulariesJson: string;
}

function fromWire(w: WireDefinition): SchemeDefinition {
  let vocabularies: Record<string, unknown> = {};
  try {
    vocabularies = JSON.parse(w.vocabulariesJson || '{}');
  } catch {
    console.warn(`Unparseable vocabularies for ${w.standard}`);
  }
  return {
    ...vocabularies,
    standard: w.standard,
    displayName: w.displayName,
    fullName: w.fullName,
    reference: w.reference ?? '',
    description: w.description ?? '',
    version: w.version ?? '',
    xsdReference: w.xsdReference ?? undefined,
    url: w.url ?? undefined,
    stageEntities: {
      intake: w.stageEntities.intake,
      preservation: w.stageEntities.preservation,
      release: w.stageEntities.release
    },
    entities: w.entities.map((e) => ({
      name: e.name,
      type: e.type,
      description: e.description,
      parent: e.parent,
      children: [...(e.children ?? [])],
      note: e.note ?? undefined,
      fields: e.fields.map((f) => ({
        name: f.name,
        label: f.label,
        type: f.type,
        required: f.required,
        vocabulary: f.vocabulary ?? null,
        options: (f.options ?? []).map((o) => ({ value: o.value, label: o.label ?? null }))
      }))
    }))
  };
}

/**
 * Fetch the definitions once. Safe to call from many places: concurrent and
 * later calls share the first request; a failed load can be retried.
 */
export function loadSchemes(): Promise<void> {
  if (byStandard.size > 0) return Promise.resolve();
  if (!loading) {
    loading = client
      .query<{ allStandardDefinitions: WireDefinition[] }>({ query: ALL_STANDARD_DEFINITIONS })
      .then((result) => {
        for (const w of result.data?.allStandardDefinitions ?? []) {
          byStandard.set(w.standard, fromWire(w));
        }
        if (byStandard.size === 0) throw new Error('Backend returned no standard definitions');
      })
      .finally(() => {
        loading = null;
      });
  }
  return loading;
}

/** True once {@link loadSchemes} has completed. */
export function schemesLoaded(): boolean {
  return byStandard.size > 0;
}

function resolveStandard(id: string): StandardDefinition | undefined {
  return standards.find((s) => s.key === id || s.graphql === id || s.file === id);
}

/** The definition for a standard, by display key, GraphQL enum value or file name. Undefined before load. */
export function getScheme(standard: string | null | undefined): SchemeDefinition | undefined {
  if (!standard) return undefined;
  const std = resolveStandard(standard);
  return byStandard.get(std?.graphql ?? standard);
}

/** Every loaded definition, in the order of `standards`. */
export function allSchemes(): SchemeDefinition[] {
  return standards.map((s) => byStandard.get(s.graphql)).filter((d): d is SchemeDefinition => !!d);
}

export function getEntity(scheme: SchemeDefinition | undefined, entityName: string): EntityDefinition | undefined {
  return scheme?.entities.find((e) => e.name === entityName);
}

/** The entities an element of `parentName` may have as children. */
export function getChildEntities(scheme: SchemeDefinition | undefined, parentName: string): EntityDefinition[] {
  const parent = getEntity(scheme, parentName);
  if (!parent?.children?.length) return [];
  return parent.children.map((n) => getEntity(scheme, n)).filter((e): e is EntityDefinition => !!e);
}

/** The root entity a package of `stage` is built from, as the definition declares it. */
export function getStageEntity(standard: string, stage: PackageStage): EntityDefinition | undefined {
  const scheme = getScheme(standard);
  return scheme ? getEntity(scheme, scheme.stageEntities[stage]) : undefined;
}

/** Human label for the stage entity ("Archive (Arkiv)"), for section headings. */
export function getStageLabel(standard: string, stage: PackageStage): string {
  const std = resolveStandard(standard);
  return std ? std[`${stage}Label` as const] : '';
}

export function emptyValues(entity: EntityDefinition | null | undefined): Record<string, string> {
  const values: Record<string, string> = {};
  for (const f of entity?.fields ?? []) values[f.name] = '';
  return values;
}

export function splitFields(entity: EntityDefinition | null | undefined): {
  required: FieldDefinition[];
  optional: FieldDefinition[];
} {
  const fields = entity?.fields ?? [];
  return {
    required: fields.filter((f) => f.required),
    optional: fields.filter((f) => !f.required)
  };
}

/** First required field without a value, as a user-facing message; null when all are filled. */
export function validateRequired(
  entity: EntityDefinition | null | undefined,
  values: Record<string, string>
): string | null {
  for (const f of entity?.fields ?? []) {
    if (f.required && !values[f.name]?.trim()) {
      return `Required field "${f.label}" is empty`;
    }
  }
  return null;
}

/** The fields array the create mutations take, in definition order. */
export function toFieldInputs(entity: EntityDefinition, values: Record<string, string>): FieldInput[] {
  return entity.fields.map((f) => ({
    name: f.name,
    label: f.label,
    type: f.type,
    value: values[f.name] || ''
  }));
}

/**
 * Field names that act as the element identifier, per standard, in priority
 * order. Whichever is filled first wins; `fallback` is used when none are.
 */
const IDENTIFIER_FIELDS = [
  'systemID',
  'packageID',
  'aipID',
  'dipID',
  'objectIdentifierValue',
  'bagName',
  'metsID',
  'eadID',
  'descriptionID',
  'modsID',
  'resourceIdentifier'
];

export function deriveElementIdentifier(values: Record<string, string>, fallback: string): string {
  for (const name of IDENTIFIER_FIELDS) {
    if (values[name]) return values[name];
  }
  return fallback;
}
