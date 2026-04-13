-- V4: Gambling sessions for Slots, Blackjack and Poker

CREATE TABLE gambling_sessions (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    game_type       VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'IN_PROGRESS',
    bet_amount      NUMERIC(10,2)  NOT NULL,
    payout_amount   NUMERIC(10,2)  NOT NULL DEFAULT 0,
    game_state      TEXT,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gambling_sessions_player ON gambling_sessions(player_id);
