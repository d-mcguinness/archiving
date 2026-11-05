/**
 * Types for the archive standard definitions in ./definitions/*.json.
 *
 * Each file describes one standard: a header, an ordered list of entities
 * (the Noark 5 arkiv/arkivdel/mappe hierarchy, the OAIS package types, …)
 * each with its allowed children and typed fields, plus any number of
 * standard-specific controlled vocabularies.
 */

export type FieldType = 'string' | 'text' | 'date' | 'number';

/** One allowed value of an enumerated field. */
export interface FieldOption {
  value: string;
  /** The vocabulary's description, when it says more than the value itself. */
  label?: string | null;
}

export interface FieldDefinition {
  name: string;
  label: string;
  type: FieldType;
  required: boolean;
  /** Dot path of the controlled vocabulary the field draws its values from, if any. */
  vocabulary?: string | null;
  /** Allowed values resolved from that vocabulary; empty for free-text fields. */
  options?: FieldOption[];
}

/** True when the field should render as a select rather than free text. */
export function isEnumerated(field: FieldDefinition): boolean {
  return !!field.options && field.options.length > 0;
}

export interface EntityDefinition {
  name: string;
  /** Structural role: "root", "structural", "metadata", … (free-form in the files). */
  type: string;
  description: string;
  parent: string | null;
  /** Names of entities that may be created beneath this one. */
  children: string[];
  note?: string;
  fields: FieldDefinition[];
}

export interface SchemeDefinition {
  /** The GraphQL ArchiveStandard enum value (e.g. "DUBLIN_CORE"). */
  standard: string;
  /** Short display name ("Dublin Core", "ISAD(G)"). */
  displayName: string;
  fullName: string;
  reference: string;
  description: string;
  version: string;
  xsdReference?: string;
  url?: string;
  /** Root entity name per lifecycle stage; the backend validates each names an entity. */
  stageEntities: Record<PackageStage, string>;
  entities: EntityDefinition[];
  /** Standard-specific controlled vocabularies (statusValues, eventTypes, …). */
  [vocabulary: string]: unknown;
}

/** The three lifecycle stages a package can be created for. */
export type PackageStage = 'intake' | 'preservation' | 'release';

/** Shape the GraphQL create mutations accept for root-element fields. */
export interface FieldInput {
  name: string;
  label: string;
  type: string;
  value: string;
}
