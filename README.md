# Search Autocomplete System

A high-performance search autocomplete system that returns top-5 suggestions ranked by historical query frequency, with sub-millisecond lookup times.

## Architecture

```
POST /api/v1/queries → in-memory buffer → (5s) → PostgreSQL + Redis sorted set
                                                          ↓ (60s)
GET /api/v1/autocomplete?prefix=X → Caffeine cache → Trie.getTopK(prefix, 5)
```

### Services

| Service | Port | Purpose |
|---------|------|---------|
| Data Gathering Service | 8081 | Accepts search queries, buffers frequency counts, flushes to PostgreSQL + Redis |
| Query Service | 8080 | Serves autocomplete suggestions from in-memory Trie |
| PostgreSQL | 5432 | Durable storage for query frequencies |
| Redis | 6379 | Transfer layer between services (sorted set) |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Metrics dashboards |

### Data Flow

1. **Ingestion**: Search queries arrive via `POST /api/v1/queries` to the Data Gathering Service
2. **Buffering**: Frequencies are buffered in a `ConcurrentHashMap<String, LongAdder>` for 5 seconds
3. **Flushing**: Every 5s, buffered counts are flushed to PostgreSQL (upsert) and Redis (ZADD to sorted set `autocomplete:frequencies`)
4. **Trie Building**: Every 60s, the Query Service reads all frequencies from Redis and rebuilds an in-memory Trie via atomic swap (copy-on-write, lock-free reads)
5. **Serving**: `GET /api/v1/autocomplete?prefix=X` looks up the Trie for top-5 results by frequency, with a Caffeine cache (TTL 5s, max 10k entries) for hot prefixes

### Resilience

- **Query Service** survives Redis outage — serves from last successfully built Trie
- **Data Gathering Service** survives PostgreSQL outage — buffers in memory until next flush
- **Trie swap** is lock-free via volatile reference — no reader contention

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)

### Run with Docker Compose

```bash
docker compose up --build
```

All services will start. Wait ~15 seconds for initial data propagation.

### Run Load Test

```bash
docker compose --profile test up load-client --build
```

This seeds 1000 words, waits for propagation, then runs concurrent autocomplete requests for 30 seconds, printing a latency summary.

## API Reference

### Data Gathering Service (port 8081)

#### Submit a Query

```bash
curl -X POST http://localhost:8081/api/v1/queries \
  -H 'Content-Type: application/json' \
  -d '{"query": "hello world"}'
```

Response: `202 Accepted`

#### Submit Batch Queries

```bash
curl -X POST http://localhost:8081/api/v1/queries/batch \
  -H 'Content-Type: application/json' \
  -d '{"queries": [{"query": "hello"}, {"query": "help"}, {"query": "hero"}]}'
```

Response: `202 Accepted`

### Query Service (port 8080)

#### Get Autocomplete Suggestions

```bash
curl 'http://localhost:8080/api/v1/autocomplete?prefix=hel'
```

Response:
```json
{
  "prefix": "hel",
  "suggestions": [
    {"query": "hello world", "frequency": 150},
    {"query": "hello", "frequency": 120},
    {"query": "help", "frequency": 85},
    {"query": "helmet", "frequency": 42},
    {"query": "helium", "frequency": 10}
  ]
}
```

## Monitoring

### Prometheus

Available at http://localhost:9090. Both services expose metrics at `/actuator/prometheus`.

### Grafana

Available at http://localhost:3000 (credentials: admin/admin).

Pre-configured dashboard **"Search Autocomplete System"** includes:
- Request rate for both services
- Latency percentiles (p50, p95, p99)
- Trie size
- JVM memory and CPU usage

## Project Structure

```
search-autocomplete-system/
├── common/                          # Shared DTOs, QueryValidator
├── data-gathering-service/          # Ingestion + frequency aggregation
├── query-service/                   # Trie-based autocomplete serving
├── load-test-client/                # Virtual-thread load tester
├── docker-compose.yml               # Full stack orchestration
├── prometheus/prometheus.yml         # Prometheus scrape config
├── grafana/                         # Grafana provisioning + dashboards
├── diagrams/components.drawio       # Architecture diagram
└── README.md
```

## Development

### Build

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Run Locally (requires PostgreSQL + Redis)

```bash
./gradlew :data-gathering-service:bootRun
./gradlew :query-service:bootRun
```

## Technology Stack

- **Java 21** with virtual threads
- **Spring Boot 3.2** (Web, JPA, Redis, Cache, Actuator)
- **PostgreSQL 16** for durable storage
- **Redis 7** for inter-service data transfer
- **Caffeine** for local caching
- **Flyway** for database migrations
- **Micrometer + Prometheus** for metrics
- **Grafana** for dashboards
- **Gradle 8.5** multi-module build
