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
 */
export function apiFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const url = path.startsWith('http') ? path : `${API_BASE}${path}`;
  return fetch(url, {
    ...init,
    headers: { ...authHeaders(), ...(init.headers ?? {}) }
  });
}
