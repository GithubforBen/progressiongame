#!/usr/bin/env bash
# Runs Flyway migrations by rebuilding and restarting the backend container.
# Spring Boot auto-applies all pending migrations on startup.
#
# Usage:
#   ./migrate.sh            – rebuild backend and run migrations
#   ./migrate.sh --dry-run  – print what would run without doing it

set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "ERROR: .env file not found. Copy .env.example and fill in DB_PASSWORD / JWT_SECRET." >&2
  exit 1
fi

if [[ "${1:-}" == "--dry-run" ]]; then
  echo "[dry-run] Would run: docker compose build backend && docker compose up -d backend"
  echo "[dry-run] Flyway migrations in backend/src/main/resources/db/migration/ would be applied."
  exit 0
fi

echo "==> Building backend image..."
docker compose build backend

echo "==> Restarting backend (Flyway runs on startup)..."
docker compose up -d --no-deps backend

echo "==> Waiting for backend to become healthy..."
timeout=60
elapsed=0
until docker compose exec backend curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1 || \
      docker compose ps backend | grep -q "healthy"; do
  sleep 2
  elapsed=$((elapsed + 2))
  if [ $elapsed -ge $timeout ]; then
    echo "WARNING: Backend did not report healthy within ${timeout}s. Check logs:"
    docker compose logs --tail=50 backend
    exit 1
  fi
done

echo ""
echo "==> Migrations applied. Latest migration files:"
ls -1 backend/src/main/resources/db/migration/*.sql | sort -V | tail -5

echo ""
echo "Done. Backend is running on port ${BACKEND_PORT:-8080}."
