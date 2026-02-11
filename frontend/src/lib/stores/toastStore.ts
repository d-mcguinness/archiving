import { writable } from 'svelte/store';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

function createToastStore() {
  const { subscribe, update } = writable<Toast[]>([]);

  function add(message: string, type: ToastType = 'info', duration: number = 5000) {
    const id = Math.random().toString(36).substring(7);
    const toast: Toast = { id, message, type, duration };

    update(toasts => [...toasts, toast]);

    if (duration > 0) {
      setTimeout(() => {
        update(toasts => toasts.filter(t => t.id !== id));
      }, duration);
    }

    return id;
  }

  return {
    subscribe,
    add,
    remove: (id: string) => {
      update(toasts => toasts.filter(t => t.id !== id));
    },
    success: (message: string, duration: number = 5000) => {
      return add(message, 'success', duration);
    },
    error: (message: string, duration: number = 5000) => {
      return add(message, 'error', duration);
    },
    info: (message: string, duration: number = 5000) => {
      return add(message, 'info', duration);
    },
    warning: (message: string, duration: number = 5000) => {
      return add(message, 'warning', duration);
    }
  };
}

export const toasts = createToastStore();
