-- V17: Lifestyle items (Fahrrad, Auto, Privat Jet, Jacht, etc.)

CREATE TABLE lifestyle_item_catalog (
    id                       VARCHAR(50)    NOT NULL PRIMARY KEY,
    name                     VARCHAR(100)   NOT NULL,
    icon                     VARCHAR(10)    NOT NULL DEFAULT '🏷',
    cost                     NUMERIC(15,2)  NOT NULL DEFAULT 0,
    monthly_cost             NUMERIC(10,2)  NOT NULL DEFAULT 0,
    stress_reduction_month   INTEGER        NOT NULL DEFAULT 0,
    tax_evasion_boost        BOOLEAN        NOT NULL DEFAULT FALSE,
    unlocks_billionaire      BOOLEAN        NOT NULL DEFAULT FALSE,
    description              TEXT
);

CREATE TABLE player_lifestyle_items (
    player_id        BIGINT      NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    item_id          VARCHAR(50) NOT NULL REFERENCES lifestyle_item_catalog(id),
    acquired_at_turn INTEGER     NOT NULL DEFAULT 1,
    PRIMARY KEY (player_id, item_id)
);
