// Impersonation ("mimic") lives apart from api.ts: the auth store imports api.ts
// for API_BASE, so importing the store from api.ts would be a cycle.
import { apiFetch } from './api';
import { auth } from './stores/authStore';
import { toasts } from './stores/toastStore';
/**
 * Ask the backend for an impersonation token and switch the UI session to it.
 * Pass a `userId` to act as that user (optionally scoped to `tenantId`), or
 * only a `tenantId` to act as the account that represents that tenant (its
 * owner, or a tenant-role member when the stored owner is not usable).
 * Returns false (after a toast) when the backend refuses.
 */
export async function impersonate(
  userId: string | number | null | undefined,
  tenantId?: string | number | null
): Promise<boolean> {
  const payload: Record<string, string> = {};
  if (userId != null && String(userId) !== '') payload.userId = String(userId);
  if (tenantId != null && String(tenantId) !== '') payload.tenantId = String(tenantId);
  if (!payload.userId && !payload.tenantId) {
    toasts.error('Nothing to impersonate');
    return false;
  }
  const res = await apiFetch('/api/auth/impersonate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok || !data?.token) {
    toasts.error(data?.error || `Could not impersonate (HTTP ${res.status})`);
    return false;
  }
  auth.startMimic(data.token, data.user, data.role, data.tenantId != null ? Number(data.tenantId) : null);
  return true;
}
