/**
 * Typed access to the archive standard definitions.
 *
 * The JSON files are bundled at build time, so a missing or malformed
 * definition fails `vite build` instead of a user's page, and every page
 * gets the same object without a fetch. Lookups accept any of the three
 * identifiers in circulation — the display key ("E-ARK"), the GraphQL enum
 * value ("EARK") or the file name ("eark.json").
 */
import { standards, type StandardDefinition } from '$lib/standards';
import type {
  EntityDefinition,
  FieldDefinition,
  FieldInput,
  PackageStage,
  SchemeDefinition
} from './types';

export type { EntityDefinition, FieldDefinition, FieldInput, FieldType, PackageStage, SchemeDefinition } from './types';
export { getStageDefaults, applyDefaults, type DefaultsContext } from './defaults';

const modules = import.meta.glob<SchemeDefinition>('./definitions/*.json', {
  eager: true,
  import: 'default'
});

/** file name ("noark5.json") → parsed definition */
const byFile: Record<string, SchemeDefinition> = {};
for (const [path, def] of Object.entries(modules)) {
  byFile[path.slice(path.lastIndexOf('/') + 1)] = def;
}

function resolveStandard(id: string): StandardDefinition | undefined {
  return standards.find((s) => s.key === id || s.graphql === id || s.file === id);
}

/** The definition for a standard, by display key, GraphQL enum value or file name. */
export function getScheme(standard: string | null | undefined): SchemeDefinition | undefined {
  if (!standard) return undefined;
  const std = resolveStandard(standard);
  return byFile[std?.file ?? standard];
}

/** Every bundled definition, in the order of `standards`. */
export function allSchemes(): SchemeDefinition[] {
  return standards.map((s) => byFile[s.file]).filter((d): d is SchemeDefinition => !!d);
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

/** The root entity a package of `stage` is built from, per the standard's mapping in standards.ts. */
export function getStageEntity(standard: string, stage: PackageStage): EntityDefinition | undefined {
  const std = resolveStandard(standard);
  if (!std) return undefined;
  const entityName = std[`${stage}Entity` as const];
  return getEntity(byFile[std.file], entityName);
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
