# 2026-04-30 — Security, Mobile Layout, Lifestyle Items, Victory Condition, Finanzamt Audit, Needs Cooldown, Unlimited Monetary Fields

## What changed
- **Security hardening:** Auth rate limiter hardened, request validation improved.
- **Mobile layout:** Hamburger drawer navigation for small screens.
- **Lifestyle items (V17):** New `lifestyle_item_catalog` and `player_lifestyle_items` tables. Items like Fahrrad, Auto, Privatjet — one-time purchase with monthly costs and stat effects.
- **Victory condition (V18):** `victory_achieved` and `personal_best_net_worth` columns added to `characters`. Victory state tracked per player.
- **Finanzamt audit (V19):** `finanzamt_audit_months_remaining` added to `characters`. During an audit, Steuerhinterziehung is disabled.
- **Needs cooldown (V20):** `cooldown_turns` added to `needs_items`. New `player_needs_usage` table tracks last use per item per player. Limits how often an item can be purchased per month.
- **Unlimited monetary fields (V21):** All `NUMERIC(precision, scale)` columns on monetary fields replaced with unconstrained `NUMERIC`. Removes the upper-bound on cash, investments, real estate, etc.
- **Social gift limit (V16):** `monthly_gift_done` boolean added to `player_social_relationships` — enforces one gift per person per month.
- **Loan repayment improvements:** Immediate payoff option added, affordability checks implemented.
- **Poker AI rework:** Personality-based bots with equity-driven decisions.

## Update steps

### 1. Pull latest code
```bash
git pull origin main
```

### 2. Rebuild both services
```bash
docker compose build
docker compose up -d
```

### 3. Migrations (automatic on restart)
| Migration | What it does |
|---|---|
| V16 | Adds `monthly_gift_done` to `player_social_relationships` |
| V17 | Creates `lifestyle_item_catalog` and `player_lifestyle_items` tables |
| V18 | Adds `victory_achieved` (bool) and `personal_best_net_worth` (numeric) to `characters` |
| V19 | Adds `finanzamt_audit_months_remaining` (int) to `characters` |
| V20 | Adds `cooldown_turns` to `needs_items`; creates `player_needs_usage` table |
| V21 | Removes precision limits from all monetary `NUMERIC` columns across all tables — pure DDL, no data loss |

> **V21 alters many tables.** On a large dataset this may take a few seconds. No data is modified, only column type declarations are changed.

### 4. No new environment variables
No `.env` changes required.

## Notes
- Lifestyle items are seeded from `backend/src/main/resources/data/lifestyle_items.yaml` on backend startup via `GameDataLoaderService`.
- V21 is purely structural — existing monetary values are preserved exactly.
