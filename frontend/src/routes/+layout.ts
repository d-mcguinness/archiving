// This file exports a load function that provides data to the layout
// This prevents the "unknown prop" warning in SvelteKit

// Disable SSR so the app renders entirely client-side.
// This ensures Svelte DevTools can detect the app and avoids
// SSR issues with localStorage-based auth and Apollo Client.
export const ssr = false;

export const load = async () => {
  return {};
};
