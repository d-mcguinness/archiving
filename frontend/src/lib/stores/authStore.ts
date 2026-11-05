import { writable } from 'svelte/store';
import { goto } from '$app/navigation';
import { API_BASE } from '../api';

export interface AuthState {
  isLoggedIn: boolean;
  user: any | null;
  role: string;
  tenantId: number | null;
}

const defaultState: AuthState = {
  isLoggedIn: false,
  user: null,
  role: '',
  tenantId: null
};

function readFromLocalStorageStatic(): AuthState {
  try {
    const token = localStorage.getItem('auth_token');
    const userJson = localStorage.getItem('auth_user');
    const role = localStorage.getItem('auth_role');
    const tenantId = localStorage.getItem('auth_tenantId');

    if (token && userJson) {
      let user = null;
      try {
        user = JSON.parse(userJson);
      } catch {
        return defaultState;
      }
      return {
        isLoggedIn: true,
        user,
        role: role || '',
        tenantId: tenantId ? parseInt(tenantId, 10) : null
      };
    }
  } catch {
    // SSR or localStorage unavailable
  }
  return defaultState;
}

function createAuthStore() {
  // Eagerly read from localStorage so the store has correct state on first render
  const initialState = typeof window !== 'undefined' ? readFromLocalStorageStatic() : defaultState;
  const { subscribe, set } = writable<AuthState>(initialState);

  function readFromLocalStorage(): AuthState {
    try {
      const token = localStorage.getItem('auth_token');
      const userJson = localStorage.getItem('auth_user');
      const role = localStorage.getItem('auth_role');
      const tenantId = localStorage.getItem('auth_tenantId');

      if (token && userJson) {
        let user = null;
        try {
          user = JSON.parse(userJson);
        } catch (e) {
          console.error('Error parsing auth_user:', e);
          return defaultState;
        }
        return {
          isLoggedIn: true,
          user,
          role: role || '',
          tenantId: tenantId ? parseInt(tenantId, 10) : null
        };
      }
    } catch (e) {
      console.error('Error reading auth from localStorage:', e);
    }
    return defaultState;
  }

  function init() {
    set(readFromLocalStorage());
    window.addEventListener('storage', () => {
      set(readFromLocalStorage());
    });
  }

  function login(token: string, user: any, role: string, tenantId?: number | null, refreshToken?: string | null) {
    localStorage.setItem('auth_token', token || '');
    localStorage.setItem('auth_user', JSON.stringify(user));
    localStorage.setItem('auth_role', role);
    if (tenantId) {
      localStorage.setItem('auth_tenantId', tenantId.toString());
    } else {
      localStorage.removeItem('auth_tenantId');
    }
    // A mimic session has no backend refresh token; clear it so a stray refresh
    // can't spend the real user's token while impersonating (refresh is also
    // short-circuited under isMimicking()).
    if (refreshToken) {
      localStorage.setItem('auth_refresh_token', refreshToken);
    } else {
      localStorage.removeItem('auth_refresh_token');
    }
    set({
      isLoggedIn: true,
      user,
      role,
      tenantId: tenantId ?? null
    });
  }

  /**
   * Start an impersonation ("mimic") session with a token the backend issued via
   * POST /api/auth/impersonate. The caller's own session is parked so exitMimic()
   * can restore it; the impersonation token carries no refresh token and expires
   * on its own (the backend caps it at 30 minutes).
   */
  function startMimic(token: string, user: any, role: string, tenantId?: number | null) {
    if (!isMimicking()) {
      localStorage.setItem('auth_original_token', localStorage.getItem('auth_token') || '');
      localStorage.setItem('auth_original_user', localStorage.getItem('auth_user') || '');
      localStorage.setItem('auth_original_role', localStorage.getItem('auth_role') || '');
      localStorage.setItem('auth_original_tenantId', localStorage.getItem('auth_tenantId') || '');
      localStorage.setItem('auth_original_refresh_token', localStorage.getItem('auth_refresh_token') || '');
    }
    localStorage.setItem('auth_mimic', '1');
    login(token, user, role, tenantId, null);
  }

  function isMimicking(): boolean {
    try {
      return localStorage.getItem('auth_mimic') === '1';
    } catch {
      return false;
    }
  }

  function exitMimic() {
    const originalToken = localStorage.getItem('auth_original_token');
    const originalUser = localStorage.getItem('auth_original_user');
    const originalRole = localStorage.getItem('auth_original_role');
    const originalTenantId = localStorage.getItem('auth_original_tenantId');

    if (originalToken && originalUser) {
      const originalRefreshToken = localStorage.getItem('auth_original_refresh_token');
      localStorage.setItem('auth_token', originalToken);
      localStorage.setItem('auth_user', originalUser);
      localStorage.setItem('auth_role', originalRole || '');
      if (originalTenantId) {
        localStorage.setItem('auth_tenantId', originalTenantId);
      } else {
        localStorage.removeItem('auth_tenantId');
      }
      if (originalRefreshToken) {
        localStorage.setItem('auth_refresh_token', originalRefreshToken);
      } else {
        localStorage.removeItem('auth_refresh_token');
      }

      localStorage.removeItem('auth_original_token');
      localStorage.removeItem('auth_original_user');
      localStorage.removeItem('auth_original_role');
      localStorage.removeItem('auth_original_tenantId');
      localStorage.removeItem('auth_original_refresh_token');
      localStorage.removeItem('auth_mimic');

      set(readFromLocalStorage());
      goto('/admin');
    } else {
      localStorage.removeItem('auth_mimic');
      logout();
    }
  }

  function logout() {
    // Best-effort server-side revoke of the refresh token (device-scoped) so it
    // can't renew the session after logout. Fire-and-forget — never block the UI.
    try {
      const refreshToken = localStorage.getItem('auth_refresh_token');
      if (refreshToken) {
        fetch(`${API_BASE}/api/auth/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        }).catch(() => { /* offline / already gone — local clear below still applies */ });
      }
    } catch { /* storage unavailable */ }

    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_mimic');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('auth_role');
    localStorage.removeItem('auth_tenantId');
    localStorage.removeItem('auth_refresh_token');
    set(defaultState);
    goto('/');
  }

  return {
    subscribe,
    init,
    login,
    logout,
    isMimicking,
    startMimic,
    exitMimic
  };
}

export const auth = createAuthStore();
