-- V3: Countries, player travel state, active events player-specific

CREATE TABLE countries (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL UNIQUE,
    travel_cost   NUMERIC(10,2)  NOT NULL,
    travel_months INTEGER        NOT NULL DEFAULT 1,
    emoji         VARCHAR(10),
    description   TEXT
);

CREATE TABLE player_travel (
    player_id            BIGINT      NOT NULL PRIMARY KEY REFERENCES players(id) ON DELETE CASCADE,
    current_country      VARCHAR(100),
    destination_country  VARCHAR(100),
    arrive_at_turn       INTEGER,
    visited_countries    TEXT[]      NOT NULL DEFAULT '{}'
);

-- Make active_events player-specific
ALTER TABLE active_events ADD COLUMN player_id BIGINT REFERENCES players(id) ON DELETE CASCADE;

-- Seed countries (match collectible country_required values from V1)
INSERT INTO countries (name, travel_cost, travel_months, emoji, description) VALUES
('Japan',    2500.00, 2, '🇯🇵', 'Land der aufgehenden Sonne — Anime, Technik und Ramen'),
('Italien',  1200.00, 1, '🇮🇹', 'Kunst, Kulinarik und atemberaubende Architektur'),
('USA',      1800.00, 2, '🇺🇸', 'Route 66, Hollywood und der American Dream'),
('Schweiz',  800.00,  1, '🇨🇭', 'Uhren, Schokolade und die Alpen'),
('UK',       900.00,  1, '🇬🇧', 'Big Ben, Pubs und die Royals'),
('China',    3000.00, 3, '🇨🇳', 'Reich der Mitte — Geschichte trifft Moderne');

-- Create player_travel rows for already-existing players
INSERT INTO player_travel (player_id)
SELECT id FROM players
ON CONFLICT DO NOTHING;
