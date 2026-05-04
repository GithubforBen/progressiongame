# 2026-05-03 — Loan Repayment UI + Burnout Fix

## What changed
- **Loan repayment UI:** Immediate full payoff option added to the Kredite page. Affordability check prevents payoff when cash is insufficient. Loan status display improved.
- **Burnout stress-reset fix:** `game.burnout.stress-reset` changed from `50` to `70` in `application.yml`. After a burnout event, stress now resets to 70 instead of 50.

## Update steps

### 1. Pull latest code
```bash
git pull origin main
```

### 2. Rebuild and restart
```bash
docker compose build backend frontend
docker compose up -d
```

### 3. Migrations
No new database migrations in this update.

### 4. No new environment variables
No `.env` changes required.

## Notes
- The burnout stress-reset value (`game.burnout.stress-reset`) is in `application.yml`. If you have overridden it in a local config, update it there too.
- This was a config-only balance change — no code path changed, only the value read from config.
