# 2026-04-20 — Social Rework + Stock Isolation

## What changed
- **Social Rework (V14):** Old NPC system replaced with a full social network — 36 persons, 8 groups, score-based relationships, actions (spendTime, giveGift, insult, rob), monthly score decay and social boosts. Old `npcs`/`player_relationships` tables kept for DB compatibility.
- **Stock Isolation (V15):** Stock price history is now per-player instead of shared. Prevents cross-account price prediction.

## Update steps

### 1. Pull latest code
```bash
git pull origin main
```

### 2. Rebuild backend and restart
```bash
docker compose build backend
docker compose up -d backend
```

The frontend does not need a rebuild for these changes.

### 3. Migrations (automatic on restart)
Flyway applies new migrations automatically.

| Migration | What it does |
|---|---|
| V14 | Adds `player_social_relationships`, `player_social_group_unlocks`, `social_action_log` tables; adds `total_jail_months_served` column to `characters` |
| V15 | Adds `player_id` column to `stock_price_history`; drops the old global unique constraint; adds per-player unique constraint; **truncates** existing price history (shared data is now invalid) |

> **Warning — V15 truncates `stock_price_history`.**
> All existing stock price history will be deleted. This only affects the history charts — active positions (investments table) are unaffected. Players keep their portfolios and balances.

### 4. No new environment variables
No `.env` changes required.

## Notes
- The old relationship pages (`GET /api/npcs`) still work — they show the legacy NPC data. The `beziehungen.vue` page has been replaced to show only the new social system.
- `total_jail_months_served` is tracked but not yet incremented in the turn engine (see open TODO in agent.md §7).
