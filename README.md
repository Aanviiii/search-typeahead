# Search Typeahead System

A search autocomplete (typeahead) system: as you type, it returns the top‑ranked
query suggestions for your prefix, records searches to keep rankings fresh, and
exposes a trending list. Built as a full‑stack app with a React frontend and a
Spring Boot backend, packaged to run with Docker Compose.

---

## Features

- **Prefix suggestions** served from an in‑memory **trie**, ranked by a trending score.
- **Search recording** that updates counts asynchronously via a batch‑write queue.
- **Trending queries** computed from a weighted score (historical + recent).
- **Distributed‑cache simulation** with consistent‑hash routing across virtual nodes, with debug/stats endpoints.
- **Dataset bootstrap** — ~224K real queries loaded from CSV on first startup.

---

## Architecture

```
┌──────────────┐      /api/*       ┌──────────────────────────────────────────┐
│   Frontend   │  ───────────────► │                Backend                     │
│ React + Vite │   (nginx / Vite   │              Spring Boot                   │
│  :3000       │    proxy strips   │  :8080                                     │
└──────────────┘     /api)         │                                            │
                                   │  Controllers ─► Services                   │
                                   │     suggest  ─► SuggestService ─┐          │
                                   │     search   ─► SearchService   │          │
                                   │     trending ─► TrendingService │          │
                                   │     cache    ─► DistributedCache│          │
                                   │                                 ▼          │
                                   │   TrieService (in‑memory prefix tree)      │
                                   │   DistributedCacheService (consistent hash)│
                                   │   BatchWriteQueue + Scheduler (async write)│
                                   │   DataLoaderService (CSV bootstrap)        │
                                   │                                 │          │
                                   │                                 ▼          │
                                   │            SQLite (JPA / Hibernate)        │
                                   └────────────────────────────────────────────┘
```

**Suggest flow:** `cache → trie → cache store`. A prefix lookup first checks the
distributed cache; on a miss it queries the trie and caches the result.

**Search flow:** a search is pushed onto an in‑memory queue and flushed to the DB
in batches by a scheduler, which keeps request latency low.

### Tech stack

| Layer    | Technology                                                            |
| -------- | --------------------------------------------------------------------- |
| Frontend | React 18, Vite, Axios (debounced input)                               |
| Backend  | Java 17, Spring Boot 3.2, Spring Data JPA, OpenCSV                     |
| Storage  | SQLite (Hibernate community dialect)                                  |
| Infra    | Docker, Docker Compose, nginx (frontend reverse proxy)                |

---

## Prerequisites

- **Docker Desktop** with Docker Compose v2.
  - On **Windows**, Docker Desktop requires the **WSL2** backend. If `docker` fails
    to start, install/update it with `wsl --install` (run as Administrator) and reboot.
- For local (non‑Docker) development: **JDK 17**, **Maven 3.9+**, and **Node.js 20+**.

---

## Quick start (Docker — recommended)

```bash
git clone https://github.com/Aanviiii/search-typeahead.git
cd search-typeahead/docker
docker compose up --build
```

Then open **http://localhost:3000**.

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080

> **First start loads the dataset.** The backend imports ~224K queries from
> `dataset/queries.csv` into SQLite and the trie. This takes several minutes; the
> app serves from the portion already loaded while it finishes. The database is
> stored in the `typeahead-db-data` volume, so subsequent starts reuse it and the
> trie is rebuilt from the DB (no full re‑import).

Stop and clean up:

```bash
docker compose down            # stop containers
docker compose down -v         # also remove the DB volume (forces a fresh load)
docker compose logs -f backend # watch data‑load / runtime logs
```

---

## Local development (without Docker)

The committed `application.properties` uses container paths (`/app/...`), so when
running locally you must point the backend at local paths.

**Backend** (from `backend/`):

```bash
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:sqlite:../data/typeahead.db?busy_timeout=60000&journal_mode=WAL&synchronous=NORMAL --app.dataset.path=../dataset/queries.csv"
```

**Frontend** (from `frontend/`):

```bash
npm install
npm run dev
```

The Vite dev server runs on http://localhost:3000 and proxies `/api/*` to the
backend on `:8080` (the `/api` prefix is stripped before forwarding).

---

## API Documentation

Interactive API docs (Swagger UI) are served by the frontend at:

**http://localhost:3000/api-docs**

The UI is rendered from the hand-written OpenAPI 3.0 spec at
[`docs/openapi.yaml`](docs/openapi.yaml) and supports "Try it out" — requests go
through the `/api` server entry, which the frontend proxies to the backend. The
spec is also bundled into the frontend (`frontend/public/openapi.yaml`) so it
serves identically in local dev and the Docker build.

---

## API reference

Base URL: `http://localhost:8080` (or `http://localhost:3000/api` via the frontend proxy).

### `GET /suggest?q={prefix}`
Top suggestions for a prefix (from cache or trie).

```json
{
  "suggestions": ["nikon", "nikon tablet", "nikon chair"],
  "prefix": "nikon",
  "cacheHit": false,
  "latencyMs": 2,
  "count": 3
}
```

### `POST /search`
Record a search. Body: `{ "query": "nikon" }`

```json
{ "message": "Searched" }
```

### `GET /trending`
Top trending queries by weighted score.

```json
{
  "trending": [
    { "query": "the", "score": 73903.9, "historicalCount": 105577, "recentCount": 0 }
  ]
}
```

### `GET /cache/debug?prefix={prefix}`
Which cache node a prefix routes to, plus per‑node hit/miss info.

### `GET /cache/stats`
Aggregate cache statistics across all nodes.

---

## Configuration

Key settings in [`backend/src/main/resources/application.properties`](backend/src/main/resources/application.properties):

| Property                          | Default | Meaning                                   |
| --------------------------------- | ------- | ----------------------------------------- |
| `app.trending.historical-weight`  | `0.7`   | Weight of all‑time count in score         |
| `app.trending.recent-weight`      | `0.3`   | Weight of recent count in score           |
| `app.trending.top-n`              | `10`    | Number of trending results                |
| `app.cache.nodes`                 | `3`     | Simulated cache nodes                      |
| `app.cache.virtual-nodes`         | `150`   | Virtual nodes per physical node (ring)    |
| `app.cache.ttl-seconds`           | `60`    | Cache entry TTL                            |
| `app.batch.flush-interval-ms`     | `10000` | Batch‑write flush interval                |
| `app.batch.max-queue-size`        | `10000` | Max queued searches before back‑pressure  |

**Trending score** = `historicalWeight × historicalCount + recentWeight × recentCount`.

---

## Project structure

```
search-typeahead/
├── backend/                 # Spring Boot service
│   ├── src/main/java/com/typeahead/
│   │   ├── controller/      # REST endpoints
│   │   ├── service/         # Suggest / Search / Trending / DataLoader
│   │   ├── trie/            # TrieService + TrieNode
│   │   ├── cache/           # DistributedCacheService + consistent hashing
│   │   ├── batch/           # async write queue + scheduler
│   │   ├── model/           # Query JPA entity
│   │   └── repository/      # Spring Data JPA repository
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                # React + Vite app
│   ├── src/ (components, hooks, api)
│   ├── nginx.conf           # production reverse proxy (/api → backend)
│   └── Dockerfile
├── dataset/                 # queries.csv (~224K) + sample
├── docker/docker-compose.yml
└── README.md
```

---

## Notes & limitations

- **Single‑node demo.** The distributed cache and consistent‑hash ring run
  in‑process — they model the design, they don't span real machines. SQLite is a
  single‑file embedded DB, suitable for the assignment but not horizontal scale.
- **No authentication**, and `GET /cache/debug` / `GET /cache/stats` are open.
  Fine for local/demo use; do not expose this on the public internet as‑is.
- **First‑start load time** (~several minutes) is the CSV import cost; it is
  one‑time per database volume.
