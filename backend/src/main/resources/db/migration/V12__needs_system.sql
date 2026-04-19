-- V12: Needs items catalog + depression/burnout state on character

CREATE TABLE needs_items (
  id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  price NUMERIC(10,2) NOT NULL,
  hunger_effect INT DEFAULT 0,
  energy_effect INT DEFAULT 0,
  happiness_effect INT DEFAULT 0,
  stress_effect INT DEFAULT 0,
  depression_reduction BOOLEAN DEFAULT false
);

ALTER TABLE characters ADD COLUMN depression_months_remaining INT NOT NULL DEFAULT 0;
ALTER TABLE characters ADD COLUMN burnout_active BOOLEAN NOT NULL DEFAULT false;
