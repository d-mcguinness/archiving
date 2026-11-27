// What the search form offers each role. A role searches only what it is
// allowed to see (see $lib/roles): ADMIN the whole system, TENANT its own
// organization, USER their own documents. Each scope lists the inputs that
// make sense for it, so the form is rendered straight from this table.

export type SearchScope = 'archives' | 'documents' | 'tenants' | 'users';

export type SearchField =
  | 'keyword'
  | 'tenant'
  | 'archive'
  | 'archiveStatus'
  | 'standard'
  | 'tenantStatus'
  | 'userRole'
  | 'dateRange';

export interface ScopeDefinition {
  scope: SearchScope;
  label: string;
  /** What the keyword is matched against, shown as the input's placeholder. */
  keywordHint: string;
  fields: SearchField[];
}

const ADMIN_SCOPES: ScopeDefinition[] = [
  {
    scope: 'archives',
    label: 'Archives',
    keywordHint: 'Title, description or content',
    fields: ['keyword', 'tenant', 'archiveStatus', 'standard', 'dateRange']
  },
  {
    scope: 'documents',
    label: 'Documents',
    keywordHint: 'Title, description or file name',
    fields: ['keyword', 'tenant', 'dateRange']
  },
  {
    scope: 'tenants',
    label: 'Tenants',
    keywordHint: 'Name, domain or description',
    fields: ['keyword', 'tenantStatus', 'dateRange']
  },
  {
    scope: 'users',
    label: 'Users',
    keywordHint: 'Name or email',
    fields: ['keyword', 'userRole']
  }
];

const TENANT_SCOPES: ScopeDefinition[] = [
  {
    scope: 'archives',
    label: 'Archives',
    keywordHint: 'Title, description or content',
    fields: ['keyword', 'archiveStatus', 'standard', 'dateRange']
  },
  {
    scope: 'documents',
    label: 'Documents',
    keywordHint: 'Title, description or file name',
    fields: ['keyword', 'archive', 'dateRange']
  }
];

const USER_SCOPES: ScopeDefinition[] = [
  {
    scope: 'documents',
    label: 'My documents',
    keywordHint: 'Title, description or file name',
    fields: ['keyword', 'archive', 'dateRange']
  }
];

/** The things a role may search, in the order they are offered. */
export function searchScopesFor(role: string | null | undefined): ScopeDefinition[] {
  if (role === 'ADMIN') return ADMIN_SCOPES;
  if (role === 'TENANT') return TENANT_SCOPES;
  if (role === 'USER') return USER_SCOPES;
  return [];
}

export const ARCHIVE_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED', 'DELETED'];
export const TENANT_STATUSES = ['ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_ACTIVATION', 'TRIAL', 'EXPIRED'];
export const USER_ROLES = ['ADMIN', 'TENANT', 'USER'];

export interface SearchValues {
  keyword: string;
  tenantId: string;
  archiveId: string;
  archiveStatus: string;
  standard: string;
  tenantStatus: string;
  userRole: string;
  from: string;
  to: string;
}

export const emptySearchValues = (): SearchValues => ({
  keyword: '',
  tenantId: '',
  archiveId: '',
  archiveStatus: '',
  standard: '',
  tenantStatus: '',
  userRole: '',
  from: '',
  to: ''
});

const same = (a: unknown, b: string) => a != null && String(a) === b;

function matchesKeyword(keyword: string, ...haystack: unknown[]): boolean {
  const words = keyword.toLowerCase().split(/\s+/).filter(Boolean);
  if (words.length === 0) return true;
  const text = haystack.filter(Boolean).join(' ').toLowerCase();
  return words.every((word) => text.includes(word));
}

/** `from`/`to` are yyyy-mm-dd from a date input; both ends are inclusive. */
function inDateRange(value: unknown, from: string, to: string): boolean {
  if (!from && !to) return true;
  if (!value) return false;
  const day = String(value).slice(0, 10);
  return (!from || day >= from) && (!to || day <= to);
}

/**
 * Narrow `items` (already limited to what the role may see) by the inputs the
 * scope offers. Inputs the scope does not offer are ignored, so a value left
 * over from another scope never filters silently.
 */
export function applySearch(definition: ScopeDefinition, items: any[], values: SearchValues): any[] {
  const has = (field: SearchField) => definition.fields.includes(field);
  const keyword = has('keyword') ? values.keyword : '';
  const from = has('dateRange') ? values.from : '';
  const to = has('dateRange') ? values.to : '';

  return items.filter((item) => {
    switch (definition.scope) {
      case 'archives':
        return (
          matchesKeyword(keyword, item.title, item.description, item.content) &&
          (!has('tenant') || !values.tenantId || same(item.tenantId, values.tenantId)) &&
          (!has('archiveStatus') || !values.archiveStatus || item.status === values.archiveStatus) &&
          (!has('standard') || !values.standard || item.standard === values.standard) &&
          inDateRange(item.createdAt, from, to)
        );
      case 'documents':
        return (
          matchesKeyword(keyword, item.title, item.description, item.fileName) &&
          (!has('tenant') || !values.tenantId || same(item.tenantId, values.tenantId)) &&
          (!has('archive') || !values.archiveId || same(item.archiveId, values.archiveId)) &&
          inDateRange(item.uploadedAt || item.createdAt, from, to)
        );
      case 'tenants':
        return (
          matchesKeyword(keyword, item.name, item.displayName, item.domain, item.description) &&
          (!has('tenantStatus') || !values.tenantStatus || item.status === values.tenantStatus) &&
          inDateRange(item.createdAt, from, to)
        );
      case 'users':
        return (
          matchesKeyword(keyword, item.name, item.email) &&
          (!has('userRole') || !values.userRole || item.role === values.userRole)
        );
    }
  });
}
