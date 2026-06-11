// Single-flight access-token refresh. When a request 401s because the short-lived
// access token has expired, callers await refreshAccessToken(): it spends the
// stored refresh token at /api/auth/refresh for a fresh access + refresh token
// (rotated, single-use) and updates the auth store. Concurrent 401s share ONE
// in-flight call so we never rotate the refresh token more than once per expiry.

import { API_BASE } from './api';
import { auth } from './stores/authStore';

let inFlight: Promise<boolean> | null = null;

/**
 * Try to renew the access token. Resolves true if a fresh token was stored,
 * false otherwise (no refresh token, mimic session, or the refresh was rejected).
 * Never throws.
 */
export function refreshAccessToken(): Promise<boolean> {
  // A mimic/impersonation session has no backend refresh session — never try to
  // refresh it (and never recurse). The caller should exit mimic instead.
  if (auth.isMimicking()) {
    return Promise.resolve(false);
  }
  if (inFlight) {
    return inFlight; // de-dupe concurrent 401s into one /refresh call
  }
  inFlight = doRefresh().finally(() => {
    inFlight = null;
  });
  return inFlight;
}

async function doRefresh(): Promise<boolean> {
  let refreshToken: string | null = null;
  try {
    refreshToken = localStorage.getItem('auth_refresh_token');
  } catch {
    return false; // SSR / storage unavailable
  }
  if (!refreshToken) {
    return false;
  }

  try {
    // BARE fetch: no Authorization header and NOT routed through apiFetch/Apollo,
    // so a 401 from /refresh itself cannot re-trigger a refresh (no recursion).
    const response = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    if (!response.ok) {
      return false;
    }
    const result = await response.json();
    if (!result?.success || !result?.token) {
      return false;
    }
    // Store the rotated access + refresh token (login() persists both + state).
    auth.login(result.token, result.user, result.role, result.tenantId, result.refreshToken);
    return true;
  } catch {
    return false;
  }
}
