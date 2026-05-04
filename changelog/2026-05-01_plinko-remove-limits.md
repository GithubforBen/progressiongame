# 2026-05-01 — Plinko Game, Remove Betting/Stock Limits

## What changed
- **Plinko replaces Poker:** The poker gambling tab has been replaced with a 3D-animated Plinko game. Win calculation is server-side.
- **Roulette bet limit removed:** No more cap on roulette bets.
- **Stock purchase quantity limit removed:** Players can buy any amount of shares.

## Update steps

### 1. Pull latest code
```bash
git pull origin main
```

### 2. Rebuild frontend (Plinko is a frontend-heavy change)
```bash
docker compose build
docker compose up -d
```

### 3. Migrations
No new database migrations in this update.

### 4. No new environment variables
No `.env` changes required.

## Notes
- The old poker game code has been removed from the frontend. The backend `GamblingService` retains the poker hand-evaluation logic but the poker endpoint is no longer called from the UI.
- Plinko win calculation happens in the backend — do not move it to the frontend.
