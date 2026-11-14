// Local-only preview config (untracked): proxies API calls same-origin and drops
// the Origin header so a backend whose CORS allows only :3001 accepts them.
import base from './vite.config.js';
import { defineConfig } from 'vite';

export default defineConfig((env) => {
    const cfg = base(env);
    for (const key of Object.keys(cfg.server.proxy)) {
        cfg.server.proxy[key].configure = (proxy) => {
            proxy.on('proxyReq', (req) => req.removeHeader('origin'));
        };
    }
    return cfg;
});
