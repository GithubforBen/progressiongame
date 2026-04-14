-- V7: Real estate, loans, SCHUFA score, YAML-loader constraints

-- SCHUFA score on characters
ALTER TABLE characters ADD COLUMN schufa_score INTEGER NOT NULL DEFAULT 500
    CHECK (schufa_score BETWEEN 0 AND 1000);

-- Unique names for upserts in DataLoader
ALTER TABLE jobs         ADD CONSTRAINT jobs_name_unique        UNIQUE (name);
ALTER TABLE collectibles ADD CONSTRAINT collectibles_name_unique UNIQUE (name);

-- Multiple education requirements per job (JSON override; NULL = old columns still used)
ALTER TABLE jobs ADD COLUMN education_requirements_json JSONB;
-- Format: [{"type":"BACHELOR","field":"INFORMATIK"},{"type":"AUSBILDUNG","field":"FACHINFORMATIKER"}]

-- Real estate catalog
CREATE TABLE real_estate_catalog (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL UNIQUE,
    location       VARCHAR(100) NOT NULL,
    category       VARCHAR(30)  NOT NULL DEFAULT 'WOHNUNG'
                       CHECK (category IN ('WOHNUNG','HAUS','GEWERBE')),
    description    TEXT,
    purchase_price NUMERIC(15,2) NOT NULL,
    monthly_rent   NUMERIC(10,2) NOT NULL,
    rent_savings   NUMERIC(10,2) NOT NULL
);

-- Player real estate
CREATE TABLE player_real_estate (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT      NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    catalog_id      BIGINT      NOT NULL REFERENCES real_estate_catalog(id),
    mode            VARCHAR(20) NOT NULL DEFAULT 'RENTED_OUT'
                        CHECK (mode IN ('SELF_OCCUPIED','RENTED_OUT')),
    purchased_at_turn INTEGER   NOT NULL,
    purchase_price  NUMERIC(15,2) NOT NULL
);

-- Player loans
CREATE TABLE player_loans (
    id               BIGSERIAL PRIMARY KEY,
    player_id        BIGINT         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    label            VARCHAR(100)   NOT NULL,
    amount_borrowed  NUMERIC(15,2)  NOT NULL,
    amount_remaining NUMERIC(15,2)  NOT NULL,
    interest_rate    NUMERIC(5,4)   NOT NULL,
    monthly_payment  NUMERIC(10,2)  NOT NULL,
    turns_remaining  INTEGER        NOT NULL,
    taken_at_turn    INTEGER        NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE'
                         CHECK (status IN ('ACTIVE','PAID_OFF','DEFAULTED'))
);
