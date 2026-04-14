-- =============================================================
-- V5 – Beziehungssystem (Step 14)
-- =============================================================

CREATE TABLE npcs (
    id                       SERIAL PRIMARY KEY,
    name                     VARCHAR(100) NOT NULL,
    description              TEXT         NOT NULL,
    personality              VARCHAR(50)  NOT NULL,
    happiness_bonus_per_level INT         NOT NULL DEFAULT 2
);

CREATE TABLE player_relationships (
    id                   SERIAL  PRIMARY KEY,
    player_id            BIGINT  NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    npc_id               BIGINT  NOT NULL REFERENCES npcs(id),
    level                INT     NOT NULL DEFAULT 0,
    months_known         INT     NOT NULL DEFAULT 0,
    last_interacted_turn INT     NOT NULL DEFAULT -1,
    UNIQUE (player_id, npc_id)
);

-- Seed NPCs
INSERT INTO npcs (name, description, personality, happiness_bonus_per_level) VALUES
('Klaus',      'Ein freundlicher Rentner aus der Nachbarschaft. Immer gut für ein Gespräch.',      'FRIENDLY',  2),
('Dr. Müller', 'Ein erfahrener Unternehmer, der dir mit Rat und Tat zur Seite steht.',             'MENTOR',    3),
('Sarah',      'Eine optimistische Studentin mit ansteckender Energie und vielen Ideen.',          'FRIENDLY',  2),
('Marco',      'Ein ehrgeiziger Kollege, der dich ständig herausfordert – aber fair bleibt.',      'RIVAL',     1),
('Lena',       'Eine zuverlässige Kollegin, die dir gelegentlich aus der Patsche hilft.',          'COLLEAGUE', 2);
