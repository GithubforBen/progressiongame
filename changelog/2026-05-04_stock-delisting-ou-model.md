# 2026-05-04 — Stock Delisting & Mean-Reversion Price Model

## What changed

- **Ornstein-Uhlenbeck mean-reversion model** replaces the pure random walk for stock price simulation. Each turn, prices are pulled gently toward their initial (seed) price via a log-space drift term. Reversion strength is configurable per stock type (`stock-reversion-speed` in `application.yml`) — strong for ETFs/bonds, near-zero for meme/crypto.
- **Per-player stock bankruptcy/delisting**: if a stock's computed price drops below `max(0.01, initialPrice × 10%)`, the stock is delisted for that player. All active investments in that stock are wiped (amount lost is logged in the event log). Penny stocks at their initial price can also be delisted when the OU model computes a sub-cent price, removing the floor-bounce exploit.
- **Automatic relisting**: after a minimum of 6 months (turns), a delisted stock has a 20% chance per turn of returning to the market at its original seed price. A new price-history entry is added so the chart shows the recovery jump.
- **Buy guard**: purchasing a delisted stock returns HTTP 410 Gone.
- **Frontend**: delisted stocks appear at the bottom of the stock list with a red INSOLVENT badge, strikethrough price, and a blocked buy form. The crash history chart remains visible.
- **Version badge**: bumped to v11.

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

### 4. Migrations (automatic on restart)

| File | What it does |
|---|---|
| `V22__stock_delisting.sql` | Adds `initial_price NUMERIC NOT NULL` to `stocks` (seeded from `current_price`). Creates `player_delisted_stocks (player_id, stock_id, delisted_at_turn)` table with unique constraint on `(player_id, stock_id)`. |

No destructive changes. Existing price history and investments are unaffected.

### 5. Manual steps
None.

## Notes

- Existing stock price history is preserved. The new model takes effect starting from the next turn end.
- All delisting/relisting thresholds and timing are configurable in `application.yml` under `game.stock-delisting`.
- Reversion speed per stock type is under `game.stock-reversion-speed`. Unknown types fall back to `0.08`.
- MEME and CRYPTO stocks have near-zero reversion (θ=0.02/0.03) — they behave almost like a random walk and can delist from their initial price if the OU model produces a sub-cent output.
