/**
 * The forms a single deposited document needs under each archive standard,
 * as wizard steps for the Add Document flow.
 *
 * Only the selection lives here: which entities of the standard describe one
 * document at its most basic level, grouped into steps. The fields themselves
 * (labels, types, vocabularies, which are required) come from the backend
 * definitions through ./index, so a wizard form and an element form of the
 * same entity can never disagree.
 */
import { getEntity, getScheme } from './index';
import type { EntityDefinition, FieldDefinition } from './types';

interface StepSpec {
  title: string;
  /** Entity names in the standard's definition, in display order. */
  entities: string[];
}

export interface DocumentFormStep {
  title: string;
  entities: EntityDefinition[];
}

/**
 * Keyed by GraphQL enum value. Each list is the smallest set the standard
 * itself asks for when describing one document: Noark 5's registration →
 * document description → document object chain, the OAIS content object plus
 * its preservation description, the ISAD(G) essential elements, and so on.
 */
const DOCUMENT_FORMS: Record<string, StepSpec[]> = {
  NOARK5: [
    { title: 'Registration', entities: ['Registration'] },
    { title: 'Document description', entities: ['Document Description'] },
    { title: 'Document object', entities: ['Document Object'] }
  ],
  OAIS: [
    { title: 'Content', entities: ['Content Data Object'] },
    { title: 'Reference & provenance', entities: ['Reference Information', 'Provenance Information'] },
    { title: 'Fixity', entities: ['Fixity Information'] }
  ],
  PREMIS: [
    { title: 'File', entities: ['File'] },
    { title: 'Event & agent', entities: ['Event', 'Agent'] }
  ],
  DUBLIN_CORE: [
    { title: 'Identification', entities: ['Title', 'Identifier', 'Type'] },
    { title: 'Origin', entities: ['Creator', 'Date', 'Format'] }
  ],
  METS: [
    { title: 'Header', entities: ['Agent'] },
    { title: 'Description', entities: ['Descriptive Metadata Section', 'Metadata Wrapper'] },
    { title: 'File', entities: ['File', 'File Location'] }
  ],
  EAD: [
    { title: 'Control', entities: ['Record ID', 'Title Statement', 'Maintenance Agency'] },
    { title: 'Description', entities: ['Archival Description', 'Unit ID', 'Unit Title', 'Unit Date'] }
  ],
  BAGIT: [
    { title: 'Declaration', entities: ['Bag Declaration'] },
    { title: 'Payload', entities: ['Payload File'] }
  ],
  ISADG: [
    { title: 'Identity', entities: ['Reference Code', 'Title', 'Date', 'Level of Description', 'Extent and Medium'] },
    { title: 'Context', entities: ['Name of Creator'] }
  ],
  MODS: [
    { title: 'Title & type', entities: ['Title', 'Type of Resource'] },
    { title: 'Origin', entities: ['Name Part', 'Date Created'] }
  ],
  EARK: [
    { title: 'Representation', entities: ['Representation', 'Content Information'] },
    { title: 'Metadata', entities: ['Descriptive Metadata', 'Preservation Metadata'] }
  ]
};

/**
 * The wizard steps for a standard (GraphQL enum value or display key).
 * Entities the loaded definition does not have are dropped, and so are steps
 * left with nothing to ask; an unknown standard yields no steps.
 */
export function documentFormSteps(standard: string | null | undefined): DocumentFormStep[] {
  const scheme = getScheme(standard);
  if (!scheme) return [];
  return (DOCUMENT_FORMS[scheme.standard] ?? [])
    .map((spec) => ({
      title: spec.title,
      entities: spec.entities
        .map((name) => getEntity(scheme, name))
        .filter((e): e is EntityDefinition => !!e && e.fields.length > 0)
    }))
    .filter((step) => step.entities.length > 0);
}

/** What is already known about the upload when the wizard opens. */
export interface DocumentContext {
  /** Generated identifier shared by the document's forms. */
  id: string;
  /** ISO date (yyyy-mm-dd). */
  today: string;
  /** ISO timestamp. */
  now: string;
  title: string;
  userName: string;
  fileName: string;
  fileSize: number;
  /** MIME type, or the file extension when the browser reports none. */
  format: string;
  /** Hex SHA-256 of the file; empty when it could not be computed. */
  sha256: string;
}

const PERSON = new Set(['creator', 'creatorName', 'originalProducer', 'agentName', 'namePart', 'producer']);
const TITLE = new Set(['title', 'titleProper', 'unitTitle']);
const FORMAT = new Set(['format', 'formatName', 'internetMediaType']);
const FILE_NAME = new Set(['fileName', 'fileReference', 'originalName']);
const DIGEST = new Set(['checksum', 'checksumValue', 'messageDigest']);
const DIGEST_ALGORITHM = new Set(['checksumAlgorithm', 'messageDigestAlgorithm', 'algorithm']);

function suggestion(field: FieldDefinition, ctx: DocumentContext): string {
  const n = field.name;
  if (TITLE.has(n)) return ctx.title;
  if (PERSON.has(n) || /By$/.test(n)) return ctx.userName;
  if (FORMAT.has(n)) return ctx.format;
  if (FILE_NAME.has(n)) return ctx.fileName;
  if (n === 'filePath') return `data/${ctx.fileName}`;
  if (n === 'fileSize') return String(ctx.fileSize);
  if (DIGEST.has(n)) return ctx.sha256;
  if (DIGEST_ALGORITHM.has(n)) return ctx.sha256 ? 'SHA-256' : '';
  if (n === 'versionNumber' || n === 'documentNumber') return '1';
  if (/^agent.*IdentifierValue$/.test(n)) return ctx.userName;
  if (/IdentifierType$/.test(n)) return 'local';
  if (n === 'identifier' || /(ID|IDValue|IdentifierValue)$/.test(n)) return ctx.id;
  if (/DateTime$/.test(n)) return field.type === 'date' ? ctx.today : ctx.now;
  if (/date/i.test(n)) return ctx.today;
  return '';
}

const norm = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '');

/**
 * Values the wizard can fill in for the user: identifiers, dates, the
 * uploader, and what the file itself says (name, size, format, checksum).
 * Only required fields are suggested, and an enumerated field only when the
 * suggestion is one of its allowed values — everything else is left for the
 * person to answer.
 */
export function suggestValues(entity: EntityDefinition, ctx: DocumentContext): Record<string, string> {
  const values: Record<string, string> = {};
  for (const field of entity.fields) {
    values[field.name] = '';
    if (!field.required) continue;
    const s = suggestion(field, ctx);
    if (!s) continue;
    if (field.options?.length) {
      const match = field.options.find((o) => norm(o.value) === norm(s));
      if (match) values[field.name] = match.value;
    } else {
      values[field.name] = s;
    }
  }
  return values;
}

/** Hex SHA-256 of a file, or '' where Web Crypto is unavailable (non-secure origins). */
export async function sha256Hex(file: Blob): Promise<string> {
  try {
    const digest = await crypto.subtle.digest('SHA-256', await file.arrayBuffer());
    return Array.from(new Uint8Array(digest), (b) => b.toString(16).padStart(2, '0')).join('');
  } catch {
    return '';
  }
}

/**
 * The filled-in forms as the text stored with the document: one block per
 * form, one "Label: value" line per answered field.
 */
export function describeForms(
  standardLabel: string,
  steps: DocumentFormStep[],
  values: Record<string, Record<string, string>>
): string {
  const blocks: string[] = [];
  for (const entity of steps.flatMap((s) => s.entities)) {
    const lines = entity.fields
      .filter((f) => values[entity.name]?.[f.name]?.trim())
      .map((f) => `${f.label}: ${values[entity.name][f.name].trim()}`);
    if (lines.length) blocks.push(`[${standardLabel} · ${entity.name}]\n${lines.join('\n')}`);
  }
  return blocks.join('\n\n');
}
