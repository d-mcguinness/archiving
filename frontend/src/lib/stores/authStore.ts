import { writable } from 'svelte/store';
import { goto } from '$app/navigation';

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

function createAuthStore() {
  const { subscribe, set } = writable<AuthState>(defaultState);

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

  function login(token: string, user: any, role: string, tenantId?: number | null) {
    localStorage.setItem('auth_token', token || '');
    localStorage.setItem('auth_user', JSON.stringify(user));
    localStorage.setItem('auth_role', role);
    if (tenantId) {
      localStorage.setItem('auth_tenantId', tenantId.toString());
    }
    set({
      isLoggedIn: true,
      user,
      role,
      tenantId: tenantId ?? null
    });
  }

  function logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    localStorage.removeItem('auth_role');
    localStorage.removeItem('auth_tenantId');
    set(defaultState);
    goto('/login');
  }

  return {
    subscribe,
    init,
    login,
    logout
  };
}

export const auth = createAuthStore();
