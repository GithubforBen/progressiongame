-- V22: Stock delisting support
-- Adds initial_price (immutable seed price) to stocks table.
-- Creates per-player delisting state table for bankruptcy tracking.

ALTER TABLE stocks ADD COLUMN initial_price NUMERIC NOT NULL DEFAULT 0;
UPDATE stocks SET initial_price = current_price;
ALTER TABLE stocks ALTER COLUMN initial_price DROP DEFAULT;

CREATE TABLE player_delisted_stocks (
    id               BIGSERIAL PRIMARY KEY,
    player_id        BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    stock_id         BIGINT NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    delisted_at_turn INT    NOT NULL,
    UNIQUE (player_id, stock_id)
);

CREATE INDEX idx_player_delisted_stocks_player ON player_delisted_stocks(player_id);
