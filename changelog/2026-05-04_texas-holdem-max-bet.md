# 2026-05-04 — Texas Hold'em Max Bet Cap

## What changed

- Texas Hold'em raises are now capped at **15× the initial bet** per action.
- The cap is enforced server-side in `GamblingService`: `raiseBy = min(raiseBy, initialBet × 15)`.
- `TexasHoldemStateDto` gains a new `maxRaise` field (null on terminal states) so the frontend always knows the current cap.
- Frontend updates:
  - Raise input gets `:max="maxRaise"` and a "Max: X€" label.
  - Quick-select multiplier buttons (2×, 5×, 10×) are clamped to `maxRaise`.
  - Raise button is disabled when the entered amount exceeds `maxRaise`.
  - All In sends `min(cash, maxRaise)` as the raise amount.
- Version badge bumped to v12.

## Update steps

### 1. Pull latest code
```bash
git pull origin main
```

### 2. New environment variables
None.

### 3. Rebuild and restart
```bash
docker compose build backend frontend && docker compose up -d
```

### 4. Migrations
None.

### 5. Manual steps
None.

## Notes

- No DB changes. The cap is purely runtime logic.
- Ongoing sessions are unaffected — the cap applies to the next raise action.
