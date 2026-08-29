import { writable } from 'svelte/store';
import { browser } from '$app/environment';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'arcana-theme';

// The inline script in app.html has already stamped <html data-theme="…">
// (stored choice, else OS preference) before the app boots; trust it as the
// initial value so store and DOM never disagree.
function initialTheme(): Theme {
	if (!browser) return 'light';
	const stamped = document.documentElement.dataset.theme;
	if (stamped === 'dark' || stamped === 'light') return stamped;
	try {
		return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
	} catch {
		return 'light';
	}
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
