# 2026-04-13 — Initial Release

## What changed
Complete initial build from scratch — all 14 feature steps implemented in one session.

## Update steps

### 1. Clone repository
```bash
git clone <repo-url>
cd game
```

### 2. Set environment variables
Copy the example file and fill in all required values:
```bash
cp .env.example .env
```

Required values that have **no default** and will prevent startup if missing:
| Variable | Description |
|---|---|
| `DB_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | Random string ≥ 32 chars (`openssl rand -base64 48`) |

Optional values with defaults:
| Variable | Default | Description |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Allowed frontend origin(s) |
| `NUXT_PUBLIC_API_BASE` | `http://localhost:8080` | Backend URL the browser calls |
| `SECURITY_TRUST_FORWARDED_FOR` | `false` | Set `true` only behind Cloudflare Tunnel |

### 3. Build and start
```bash
docker compose build
docker compose up -d
```

### 4. Migrations (automatic)
Flyway applies all migrations automatically on first backend startup.

Migrations applied: **V1–V13**

| Migration | Contents |
|---|---|
| V1 | Initial schema — all core tables + seed data |
| V2 | Investments + stock price history |
| V3 | Travel, collectibles, active events |
| V4 | Gambling (`gambling_sessions`) |
| V5 | Relationship system (`npcs`, `player_relationships`) |
| V6 | Fix NPC ID types |
| V7 | Real estate, loans, SCHUFA |
| V8 | Job catalog (`category`, `max_parallel`, `required_side_cert`) |
| V9 | Level system + collections |
| V10 | Cert-based unlock system |
| V11 | Extended stock types (DIVIDEND_STOCK, BOND, REIT, LEVERAGE, WARRANT, SHORT, FUTURES) |
| V12 | Needs system (`needs_items`, burnout + depression fields on characters) |
| V13 | Tax evasion fields on characters |

### 5. Verify
- Frontend reachable at `http://localhost:3000`
- Backend health: `curl http://localhost:8080/api/health`
- Register an account and confirm the dashboard loads

## Notes
- This is a clean install — no data migration from a previous version.
