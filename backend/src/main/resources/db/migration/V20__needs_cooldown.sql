-- Add cooldown_turns to needs catalog (0 = unlimited, 1 = once/month, etc.)
ALTER TABLE needs_items ADD COLUMN cooldown_turns INT NOT NULL DEFAULT 1;

-- Track when each player last used each needs item
CREATE TABLE player_needs_usage (
    player_id      BIGINT      NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    item_id        VARCHAR(50) NOT NULL REFERENCES needs_items(id),
    last_used_turn INT         NOT NULL,
    PRIMARY KEY (player_id, item_id)
);
