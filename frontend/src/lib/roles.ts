// Role hierarchy helpers shared by the app shell and pages:
// ADMIN (whole system) > TENANT (one organization) > USER (own profile + documents).
import type { AuthState } from './stores/authStore';

export type Role = 'ADMIN' | 'TENANT' | 'USER';

const LABELS: Record<string, string> = {
  ADMIN: 'Administrator',
  TENANT: 'Tenant manager',
  USER: 'Member'
};

/** Human-readable name for a role ("TENANT" → "Tenant manager"). */
export function roleLabel(role: string | null | undefined): string {
  return (role && LABELS[role]) || 'Signed in';
}

/** Where a signed-in account lands: the top of what its role is allowed to see. */
export function roleHome(state: AuthState): string {
  const { role, tenantId, user } = state;
  if (role === 'ADMIN') return '/admin';
  if (role === 'TENANT' && tenantId) return `/tenants/${tenantId}/archives`;
  if (role === 'USER' && tenantId && user?.id) return `/tenants/${tenantId}/users/${user.id}`;
  return '/';
}

export interface NavItem {
  label: string;
  href: string;
  /** Only the exact path counts as active (e.g. the dashboard at /admin). */
  exact?: boolean;
  /** Extra path roots that should also light this item up. */
  match?: string[];
  /** Path roots under `href` that belong to a different item. */
  exclude?: string[];
}

/**
 * Primary navigation for a role, as ordered groups. Groups are separated
 * visually: for ADMIN "who" (tenants, users) comes before "what" (the archive
 * pipeline); for TENANT the daily pipeline comes before team management.
 */
export function navFor(state: AuthState): NavItem[][] {
  const { role, tenantId, user } = state;

  if (role === 'ADMIN') {
    return [
      [
        { label: 'Dashboard', href: '/admin', exact: true },
        { label: 'Tenants', href: '/admin/tenants', match: ['/tenants'] },
        { label: 'Users', href: '/admin/users', match: ['/users'] }
      ],
      [
        { label: 'Archives', href: '/admin/archives', match: ['/archives'] },
        { label: 'Intakes', href: '/admin/intake', match: ['/intake'] },
        { label: 'Preservations', href: '/admin/preservation', match: ['/preservation'] },
        { label: 'Releases', href: '/admin/release', match: ['/release'] },
        { label: 'Documents', href: '/admin/documents' }
      ]
    ];
  }

  if (role === 'TENANT' && tenantId) {
    const base = `/tenants/${tenantId}`;
    return [
      [
        { label: 'Archives', href: `${base}/archives`, match: ['/archives'] },
        { label: 'Intakes', href: `${base}/intakes`, match: ['/intake'] },
        { label: 'Preservations', href: `${base}/preservations`, match: ['/preservation'] },
        { label: 'Releases', href: `${base}/releases`, match: ['/release'] },
        { label: 'Documents', href: `${base}/documents` }
      ],
      [{ label: 'Users', href: `${base}/users`, match: ['/users'] }]
    ];
  }

  if (role === 'USER' && tenantId && user?.id) {
    const self = `/tenants/${tenantId}/users/${user.id}`;
    // A member's profile is reached from their name in the header, not the nav.
    return [[{ label: 'My documents', href: `${self}/documents` }]];
  }

  return [];
}

const under = (path: string, root: string) => path === root || path.startsWith(root + '/');

/** Whether `item` is the section the current path belongs to. */
export function isNavActive(item: NavItem, path: string): boolean {
  if (item.exact) return path === item.href;
  if ((item.exclude ?? []).some((root) => under(path, root))) return false;
  return [item.href, ...(item.match ?? [])].some((root) => under(path, root));
}
