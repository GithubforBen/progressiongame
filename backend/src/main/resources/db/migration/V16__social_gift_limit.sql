ALTER TABLE player_social_relationships
    ADD COLUMN IF NOT EXISTS monthly_gift_done BOOLEAN NOT NULL DEFAULT FALSE;
