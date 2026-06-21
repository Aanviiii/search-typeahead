# ─────────────────────────────────────────────
# STAGE 1: BUILD — compile React app to static files
# ─────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /build

# (1) Copy package files first (same caching trick as backend)
COPY package.json package-lock.json* ./

# (2) Install dependencies (cached if package.json unchanged)
RUN npm install

# (3) Copy source code
COPY . .

# (4) Build production-optimized static files
# Output goes to /build/dist (minified JS/CSS, tree-shaken)
RUN npm run build

# ─────────────────────────────────────────────
# STAGE 2: RUNTIME — Nginx serving static files
# ─────────────────────────────────────────────
FROM nginx:alpine

# (5) Copy custom Nginx config (handles SPA routing + API proxy)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# (6) Copy the built static files from Stage 1 into Nginx's web root
COPY --from=build /build/dist /usr/share/nginx/html

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3000 || exit 1

# (7) Nginx's default entrypoint already runs nginx in foreground —
# no custom CMD needed, the base image handles it