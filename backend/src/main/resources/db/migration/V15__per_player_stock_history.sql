-- V15: Isolate stock price history per player (removes global shared simulation)
TRUNCATE TABLE stock_price_history;

ALTER TABLE stock_price_history
    ADD COLUMN player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE;

ALTER TABLE stock_price_history DROP CONSTRAINT stock_price_history_stock_id_turn_key;

ALTER TABLE stock_price_history
    ADD CONSTRAINT stock_price_history_stock_player_turn_key UNIQUE (stock_id, player_id, turn);
