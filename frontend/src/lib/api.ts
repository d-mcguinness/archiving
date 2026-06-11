// Helpers for authenticated REST calls to the backend.
// GraphQL goes through Apollo's authLink; REST (file/document endpoints) must
// attach the same bearer token, which the backend's RestAuthInterceptor requires.

export const API_BASE = 'http://localhost:2020';

/** Bearer token stored at login, or null on the server / when logged out. */
export function authToken(): string | null {
  return typeof window !== 'undefined' ? localStorage.getItem('auth_token') : null;
}

/** Authorization header object, empty when no token is present. */
export function authHeaders(): Record<string, string> {
  const token = authToken();
  return token ? { Authorization: token } : {};
}

/**
 * fetch() against the backend with the bearer token attached. Merges any
 * caller-supplied headers; do NOT set Content-Type for FormData bodies.
 *
 * On a 401 (typically an expired access token) it transparently refreshes the
 * token once and retries the same request, so callers don't see a spurious 401
 * mid-session. The retry is skipped while mimicking and attempted at most once.
 */
export async function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const url = path.startsWith('http') ? path : `${API_BASE}${path}`;
  const send = () => fetch(url, {
    ...init,
    headers: { ...authHeaders(), ...(init.headers ?? {}) }
  });

  const response = await send();
  if (response.status !== 401) {
    return response;
  }
  // Lazy import avoids a static api.ts <-> refresh.ts import cycle.
  const { refreshAccessToken } = await import('./refresh');
  const refreshed = await refreshAccessToken();
  return refreshed ? send() : response;
}
