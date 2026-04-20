ALTER TABLE characters
  ADD COLUMN total_jail_months_served INT NOT NULL DEFAULT 0;

CREATE TABLE player_social_group_unlocks (
    player_id        BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    group_id         VARCHAR(100) NOT NULL,
    unlocked_at_turn INT          NOT NULL,
    PRIMARY KEY (player_id, group_id)
);

CREATE TABLE player_social_relationships (
    player_id                  BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    person_id                  VARCHAR(100) NOT NULL,
    score                      INT          NOT NULL DEFAULT 0,
    unlocked_at_turn           INT,
    locked_actions_until_turn  INT          NOT NULL DEFAULT 0,
    monthly_time_spent_count   INT          NOT NULL DEFAULT 0,
    monthly_insult_done        BOOLEAN      NOT NULL DEFAULT FALSE,
    monthly_rob_attempted      BOOLEAN      NOT NULL DEFAULT FALSE,
    had_conflict               BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (player_id, person_id)
);

CREATE TABLE social_action_log (
    id           BIGSERIAL    PRIMARY KEY,
    player_id    BIGINT       NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    person_id    VARCHAR(100) NOT NULL,
    action_type  VARCHAR(50)  NOT NULL,
    score_delta  INT,
    outcome      VARCHAR(50),
    turn_number  INT          NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
