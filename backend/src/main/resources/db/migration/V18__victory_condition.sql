-- V18: Victory condition fields

ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS victory_achieved      BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS personal_best_net_worth NUMERIC(20,2) NOT NULL DEFAULT 0;
