import { sveltekit } from '@sveltejs/kit/vite';
import { svelteInspector } from '@sveltejs/vite-plugin-svelte-inspector';
import { defineConfig } from 'vite';

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
                target: 'http://localhost:2020',
                changeOrigin: true
            },
            '/api': {
                target: 'http://localhost:2020',
                changeOrigin: true
            }
        }
    }
}));
