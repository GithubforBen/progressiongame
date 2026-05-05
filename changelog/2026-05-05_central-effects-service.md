# 2026-05-05 — Central Effects Service

## What changed

- **New `PlayerEffectsService`**: single aggregator for all player multipliers, discounts, and stat-ticks. All consumer services now query it instead of maintaining ad-hoc boost logic.
- **`EffectContributor` pattern**: three auto-discovered contributors:
  - `SocialEffectContributor` — all 21 social boost types from `persons.yaml` now wired (previously 8 of 21 were consumed, 13 were silently ignored)
  - `CollectionEffectContributor` — replaces the old `CollectionBonusApplier` strategy pattern
  - `LifestyleEffectContributor` — stress reduction and tax detection reduction from owned lifestyle items
- **Deleted**: `CollectionBonusApplier` interface and 5 implementations — replaced by `CollectionEffectContributor`
- **Wired consumers** (all previously disconnected):
  - `LoanService`: interest rate reduced by `LOAN_INTEREST_REDUCTION`
  - `RealEstateService`: purchase price discounted by `PROPERTY_PRICE_DISCOUNT` (max 50%)
  - `TravelService`: cost reduced by `TRAVEL_COST_REDUCTION` (max 80%), duration reduced by `TRAVEL_DURATION_REDUCTION`
  - `CollectibleService`: price discounted by `COLLECTIBLE_PRICE_DISCOUNT` (max 50%)
  - `StockService`: OU sigma reduced by `STOCK_VOLATILITY_REDUCTION` (max 50%)
  - `TurnService`: job acceptance boost, collectible drop-rate boost, all stat-ticks consolidated
- **REST endpoint** `GET /api/effects`: returns all active effects grouped by type with per-source breakdowns
- **Frontend** `/effekte` page: shows all active effects with source breakdown (Beziehung / Sammlung / Lifestyle), accessible from sidebar
- No DB migration needed.
- Version badge bumped to v16.

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
None (V23 was the last migration).

### 5. Manual steps
None.

## Notes

- Effects stack additively across sources. Caps are enforced per consumer, not in the aggregator.
- `GAMBLING_LUCK_BOOST` and `COLLECTIBLE_DROP_RATE_BOOST` are wired but have small per-unit impact by design.
- To add a new effect source: create `@Component class MyContributor implements EffectContributor` — no other changes needed.
- To add a new effect type: add to `EffectType` enum, map in the contributor, add the consumer call-site.
