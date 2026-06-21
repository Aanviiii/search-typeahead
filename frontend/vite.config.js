import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// (1) Vite configuration
export default defineConfig({
    // (2) Enable JSX/React support
    plugins: [react()],

    // (3) Dev server settings
    server: {
        port: 3000,
        // (4) Proxy /api/* requests to the backend during development
        // This avoids CORS issues in dev and lets frontend code use
        // relative paths instead of hardcoding http://localhost:8080
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                // (5) Strip the /api prefix before forwarding
                // /api/suggest?q=apple  →  http://localhost:8080/suggest?q=apple
                rewrite: (path) => path.replace(/^\/api/, '')
            }
        }
    }
})