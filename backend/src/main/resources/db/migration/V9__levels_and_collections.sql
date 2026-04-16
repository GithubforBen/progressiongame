-- ============================================================
-- V9: Investment Levels, Property Level Gating, Collections
-- ============================================================

-- Player investment level tracking (one row per player, created on demand)
CREATE TABLE player_investment_levels (
    player_id           BIGINT PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    immobilien_level    INT NOT NULL DEFAULT 0,
    stock_trader_level  INT NOT NULL DEFAULT 0
);

-- Level gating for real estate catalog entries
ALTER TABLE real_estate_catalog ADD COLUMN level_required INT NOT NULL DEFAULT 1;

-- Collections reference table (upserted from collections.yaml at startup)
CREATE TABLE collections (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) UNIQUE NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    bonus_type   VARCHAR(50)  NOT NULL,
    bonus_value  NUMERIC(10,4) NOT NULL,
    item_count   INT NOT NULL DEFAULT 0
);

-- Extend collectibles: link to collection and add shop price
ALTER TABLE collectibles ADD COLUMN collection_name VARCHAR(100) REFERENCES collections(name);
ALTER TABLE collectibles ADD COLUMN price NUMERIC(15,2) NOT NULL DEFAULT 0;
