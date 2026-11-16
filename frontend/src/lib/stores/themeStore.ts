import { writable } from 'svelte/store';
import { browser } from '$app/environment';

export type Theme = 'light' | 'dark';

// Keep in sync with the inline script in app.html. Bumped to v2 when dark
// became the default, so choices saved before then no longer apply.
const STORAGE_KEY = 'arcana-theme-v2';

// The inline script in app.html has already stamped <html data-theme="…">
// (stored choice, else dark) before the app boots; trust it as the initial
// value so store and DOM never disagree.
function initialTheme(): Theme {
	if (!browser) return 'dark';
	return document.documentElement.dataset.theme === 'light' ? 'light' : 'dark';
}

function createThemeStore() {
	const { subscribe, set, update } = writable<Theme>(initialTheme());

	function apply(theme: Theme) {
		if (!browser) return;
		document.documentElement.dataset.theme = theme;
		try {
			localStorage.setItem(STORAGE_KEY, theme);
		} catch {
			// Storage unavailable (private mode etc.) — the choice just won't persist.
		}
	}

	return {
		subscribe,
		set(theme: Theme) {
			apply(theme);
			set(theme);
		},
		toggle() {
			update((current) => {
				const next: Theme = current === 'dark' ? 'light' : 'dark';
				apply(next);
				return next;
			});
		}
	};
}

export const theme = createThemeStore();
