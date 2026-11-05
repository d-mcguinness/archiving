// Impersonation ("mimic") lives apart from api.ts: the auth store imports api.ts
// for API_BASE, so importing the store from api.ts would be a cycle.
import { apiFetch } from './api';
import { auth } from './stores/authStore';
import { toasts } from './stores/toastStore';
/**
 * Ask the backend for an impersonation token for `userId` (optionally scoped to
 * `tenantId`) and switch the UI session to it. Returns false (after a toast) when
 * the backend refuses.
 */
export async function impersonate(userId: string | number, tenantId?: string | number | null): Promise<boolean> {
  const res = await apiFetch('/api/auth/impersonate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tenantId != null ? { userId: String(userId), tenantId: String(tenantId) } : { userId: String(userId) })
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok || !data?.token) {
    toasts.error(data?.error || `Could not impersonate (HTTP ${res.status})`);
    return false;
  }
  auth.startMimic(data.token, data.user, data.role, data.tenantId != null ? Number(data.tenantId) : null);
  return true;
}
