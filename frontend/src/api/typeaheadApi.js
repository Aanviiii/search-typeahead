import axios from 'axios'

// (1) Single axios instance — all requests go through /api,
// which the Vite proxy (dev) or Nginx (Docker) forwards to the backend
const api = axios.create({
    baseURL: '/api',
    timeout: 5000, // (2) 5-second timeout — prevents requests hanging forever
})

// ─────────────────────────────────────────────
// GET /suggest?q=prefix
// ─────────────────────────────────────────────
export async function getSuggestions(prefix) {
    const response = await api.get('/suggest', {
        params: { q: prefix },
    })
    return response.data
}

// ─────────────────────────────────────────────
// POST /search
// ─────────────────────────────────────────────
export async function postSearch(query) {
    const response = await api.post('/search', { query })
    return response.data
}

// ─────────────────────────────────────────────
// GET /trending
// ─────────────────────────────────────────────
export async function getTrending() {
    const response = await api.get('/trending')
    return response.data
}

// ─────────────────────────────────────────────
// GET /cache/debug?prefix=prefix
// ─────────────────────────────────────────────
export async function getCacheDebug(prefix) {
    const response = await api.get('/cache/debug', {
        params: { prefix },
    })
    return response.data
}