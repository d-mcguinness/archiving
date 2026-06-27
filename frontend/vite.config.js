import { sveltekit } from '@sveltejs/kit/vite';
import { svelteInspector } from '@sveltejs/vite-plugin-svelte-inspector';
import { defineConfig } from 'vite';

// Dev-server proxy target; matches the VITE_API_BASE default.
const apiTarget = process.env.VITE_API_BASE || 'http://localhost:2020';

export default defineConfig(({ mode }) => ({
    plugins: [sveltekit(), mode === 'development' && svelteInspector()].filter(Boolean),
    ssr: {
        // Ensure Apollo's CommonJS subpaths work during SSR
        noExternal: ['@apollo/client', '@apollo/client/*']
    },
    server: {
        port: 3001,
        proxy: {
            '/graphql': {
                target: apiTarget,
                changeOrigin: true
            },
            '/api': {
                target: apiTarget,
                changeOrigin: true
            }
        }
    }
}));
