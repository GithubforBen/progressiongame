-- V2: Stock price history table + stock_id foreign key on investments

-- Separate price history table (replaces JSONB history in stocks)
CREATE TABLE stock_price_history (
    id       BIGSERIAL PRIMARY KEY,
    stock_id BIGINT        NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    price    NUMERIC(10,2) NOT NULL,
    turn     INTEGER       NOT NULL,
    UNIQUE (stock_id, turn)
);

-- Migrate existing seed history from JSONB column
INSERT INTO stock_price_history (stock_id, turn, price)
SELECT s.id, (elem->>'turn')::int, (elem->>'price')::numeric
FROM stocks s, jsonb_array_elements(s.history) elem;

-- Add stock_id reference to investments for proper FK linking
ALTER TABLE investments ADD COLUMN stock_id BIGINT REFERENCES stocks(id);
