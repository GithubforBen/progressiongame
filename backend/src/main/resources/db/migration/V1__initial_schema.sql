-- =============================================================
-- V1: Initial schema for FinanzLeben
-- =============================================================

-- Players (authentication)
CREATE TABLE players (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Characters (game state per player)
CREATE TABLE characters (
    id           BIGSERIAL PRIMARY KEY,
    player_id    BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    cash         NUMERIC(15, 2) NOT NULL DEFAULT 1000.00,
    net_worth    NUMERIC(15, 2) NOT NULL DEFAULT 1000.00,
    stress       INTEGER      NOT NULL DEFAULT 0  CHECK (stress BETWEEN 0 AND 100),
    hunger       INTEGER      NOT NULL DEFAULT 100 CHECK (hunger BETWEEN 0 AND 100),
    energy       INTEGER      NOT NULL DEFAULT 100 CHECK (energy BETWEEN 0 AND 100),
    happiness    INTEGER      NOT NULL DEFAULT 70  CHECK (happiness BETWEEN 0 AND 100),
    current_turn INTEGER      NOT NULL DEFAULT 1,
    UNIQUE (player_id)
);

-- Education progress
CREATE TABLE education_progress (
    id                          BIGSERIAL PRIMARY KEY,
    player_id                   BIGINT      NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    main_stage                  VARCHAR(50) NOT NULL DEFAULT 'GRUNDSCHULE',
    main_stage_months_remaining INTEGER     NOT NULL DEFAULT 0,
    main_stage_field            VARCHAR(50),
    side_cert                   VARCHAR(50),
    side_cert_months_remaining  INTEGER     NOT NULL DEFAULT 0,
    completed_stages            TEXT[]      NOT NULL DEFAULT ARRAY['GRUNDSCHULE'],
    UNIQUE (player_id)
);

-- Jobs (global catalogue — refreshed/seeded by the backend)
CREATE TABLE jobs (
    id                          BIGSERIAL PRIMARY KEY,
    name                        VARCHAR(100)   NOT NULL,
    description                 TEXT,
    required_education_type     VARCHAR(50),
    required_education_field    VARCHAR(50),
    required_months_experience  INTEGER        NOT NULL DEFAULT 0,
    salary                      NUMERIC(10, 2) NOT NULL,
    stress_per_month            INTEGER        NOT NULL DEFAULT 5,
    available                   BOOLEAN        NOT NULL DEFAULT TRUE
);

-- Player jobs (active employment)
CREATE TABLE player_jobs (
    player_id      BIGINT  NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    job_id         BIGINT  NOT NULL REFERENCES jobs(id),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    months_worked  INTEGER NOT NULL DEFAULT 0,
    started_at_turn INTEGER NOT NULL,
    PRIMARY KEY (player_id, job_id)
);

-- Job applications (resolved next turn)
CREATE TABLE job_applications (
    id               BIGSERIAL PRIMARY KEY,
    player_id        BIGINT      NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    job_id           BIGINT      NOT NULL REFERENCES jobs(id),
    applied_at_turn  INTEGER     NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    resolved_at_turn INTEGER
);

-- Investments
CREATE TABLE investments (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    type            VARCHAR(30)    NOT NULL
                        CHECK (type IN ('STOCK', 'REAL_ESTATE', 'ART', 'NFT', 'COMPANY', 'COLLECTIBLE')),
    name            VARCHAR(100)   NOT NULL,
    amount_invested NUMERIC(15, 2) NOT NULL,
    current_value   NUMERIC(15, 2) NOT NULL,
    quantity        NUMERIC(15, 6) NOT NULL DEFAULT 1,
    acquired_at_turn INTEGER       NOT NULL
);

-- Stocks (global market simulation)
CREATE TABLE stocks (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    ticker        VARCHAR(10)    NOT NULL UNIQUE,
    type          VARCHAR(10)    NOT NULL DEFAULT 'NORMAL'
                      CHECK (type IN ('NORMAL', 'MEME')),
    current_price NUMERIC(10, 2) NOT NULL,
    history       JSONB          NOT NULL DEFAULT '[]'
);

-- Collectibles catalogue
CREATE TABLE collectibles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    collection_type VARCHAR(50)    NOT NULL,
    country_required VARCHAR(50)   NOT NULL,
    rarity          VARCHAR(20)    NOT NULL
                        CHECK (rarity IN ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
    base_value      NUMERIC(10, 2) NOT NULL,
    description     TEXT
);

-- Player-owned collectibles
CREATE TABLE player_collectibles (
    player_id       BIGINT         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    collectible_id  BIGINT         NOT NULL REFERENCES collectibles(id),
    acquired_at_turn INTEGER       NOT NULL,
    purchase_price  NUMERIC(10, 2) NOT NULL,
    PRIMARY KEY (player_id, collectible_id)
);

-- Time-limited events (tages-events)
CREATE TABLE active_events (
    id              BIGSERIAL PRIMARY KEY,
    type            VARCHAR(30)  NOT NULL,
    country         VARCHAR(50),
    expires_at_turn INTEGER      NOT NULL,
    collectible_id  BIGINT REFERENCES collectibles(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Monthly fixed expenses per player
CREATE TABLE monthly_expenses (
    id        BIGSERIAL PRIMARY KEY,
    player_id BIGINT         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    category  VARCHAR(50)    NOT NULL,
    label     VARCHAR(100)   NOT NULL,
    amount    NUMERIC(10, 2) NOT NULL,
    active    BOOLEAN        NOT NULL DEFAULT TRUE,
    mandatory BOOLEAN        NOT NULL DEFAULT FALSE
);

-- Event log (per player)
CREATE TABLE events_log (
    id             BIGSERIAL PRIMARY KEY,
    player_id      BIGINT         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    description    TEXT           NOT NULL,
    amount_effect  NUMERIC(10, 2),
    event_type     VARCHAR(50),
    created_at_turn INTEGER       NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Monthly snapshots for charts
CREATE TABLE monthly_snapshots (
    id             BIGSERIAL PRIMARY KEY,
    player_id      BIGINT         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    turn           INTEGER        NOT NULL,
    cash           NUMERIC(15, 2) NOT NULL,
    net_worth      NUMERIC(15, 2) NOT NULL,
    total_income   NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_expenses NUMERIC(15, 2) NOT NULL DEFAULT 0,
    snapshot_date  TIMESTAMP      NOT NULL DEFAULT NOW(),
    UNIQUE (player_id, turn)
);

-- Leaderboard view
CREATE VIEW leaderboard AS
SELECT
    p.id         AS player_id,
    p.username,
    c.net_worth,
    c.current_turn,
    p.created_at
FROM players p
JOIN characters c ON c.player_id = p.id
ORDER BY c.net_worth DESC;

-- =============================================================
-- Seed data
-- =============================================================

INSERT INTO jobs (name, description, required_education_type, required_education_field,
                  required_months_experience, salary, stress_per_month)
VALUES
('Zeitungsaustraeger',    'Morgens Zeitungen austragen',                NULL,                  NULL,               0,  450.00,  10),
('Supermarkt-Kassierer',  'An der Kasse im Supermarkt arbeiten',        NULL,                  NULL,               0,  1200.00, 15),
('Lagerarbeiter',         'Waren ein- und auslagern',                   NULL,                  NULL,               0,  1400.00, 20),
('Buerokaufmann/-frau',   'Administrative Taetigkeiten im Buero',       'REALSCHULABSCHLUSS',  NULL,               0,  2200.00, 15),
('Social Media Manager',  'Social-Media-Kanaele betreuen',              'WEITERBILDUNG',       'SOCIAL_MEDIA',     0,  2800.00, 20),
('IT-Support',            'Technischen Support leisten',                'AUSBILDUNG',          'FACHINFORMATIKER', 0,  3200.00, 20),
('Software Engineer',     'Software entwickeln',                        'BACHELOR',            'INFORMATIK',       0,  5500.00, 25),
('Arzt/Aerztin',          'Patienten behandeln',                        'STUDIUM',             'MEDIZIN',          0,  7000.00, 35),
('Manager',               'Abteilung leiten',                           'BACHELOR',            'BWL',              24, 6000.00, 40);

INSERT INTO collectibles (name, collection_type, country_required, rarity, base_value, description)
VALUES
('Nissan Skyline R34',          'AUTOS',  'Japan',    'LEGENDARY', 85000.00,  'Legendaerer JDM-Sportwagen'),
('Ferrari 250 GTO',             'AUTOS',  'Italien',  'LEGENDARY', 450000.00, 'Das wertvollste Auto der Welt'),
('Vintage Harley-Davidson',     'AUTOS',  'USA',      'EPIC',      35000.00,  'Amerikanische Motorrad-Ikone'),
('Rolex Daytona',               'UHREN',  'Schweiz',  'LEGENDARY', 25000.00,  'Die begehrte Sportuhr'),
('Patek Philippe Nautilus',     'UHREN',  'Schweiz',  'LEGENDARY', 80000.00,  'Kultstatuswerk der Uhrmacherei'),
('Banksy Original',             'KUNST',  'UK',       'EPIC',      120000.00, 'Originales Strassenkunstwerk'),
('Porzellane Vase Ming-Dyn.',   'KUNST',  'China',    'RARE',      15000.00,  'Antike chinesische Vase');

INSERT INTO stocks (name, ticker, type, current_price, history)
VALUES
('TechCorp AG',         'TECH', 'NORMAL', 120.00, '[{"price":100,"turn":0},{"price":110,"turn":1},{"price":120,"turn":2}]'),
('GlobalBank',          'GBNK', 'NORMAL', 45.00,  '[{"price":50,"turn":0},{"price":48,"turn":1},{"price":45,"turn":2}]'),
('EnergyPlus',          'ENRG', 'NORMAL', 78.00,  '[{"price":70,"turn":0},{"price":75,"turn":1},{"price":78,"turn":2}]'),
('MoonToken',           'MOON', 'MEME',   0.50,   '[{"price":1.0,"turn":0},{"price":2.5,"turn":1},{"price":0.5,"turn":2}]'),
('DiamondHandsCoin',    'DIAM', 'MEME',   420.00, '[{"price":100,"turn":0},{"price":500,"turn":1},{"price":420,"turn":2}]');
