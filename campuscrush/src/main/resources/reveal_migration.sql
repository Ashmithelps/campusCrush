-- Run manually in Supabase SQL Editor before deploying

CREATE TABLE reveal_kits (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT        NOT NULL REFERENCES users(id) UNIQUE,
    hint1            VARCHAR(200),
    hint2            VARCHAR(200),
    hint3            VARCHAR(200),
    guessing_enabled BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE reveal_states (
    id                BIGSERIAL PRIMARY KEY,
    confession_id     BIGINT      NOT NULL REFERENCES confessions(id) UNIQUE,
    status            VARCHAR(25) NOT NULL DEFAULT 'HIDDEN',
    guesses_remaining INT         NOT NULL DEFAULT 3,
    hints_unlocked    INT         NOT NULL DEFAULT 1,
    locked_until      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reveal_guesses (
    id            BIGSERIAL    PRIMARY KEY,
    confession_id BIGINT       NOT NULL REFERENCES confessions(id),
    guessed_roll  VARCHAR(50)  NOT NULL,
    correct       BOOLEAN      NOT NULL,
    guessed_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reveal_guesses_confession ON reveal_guesses (confession_id, guessed_at DESC);
