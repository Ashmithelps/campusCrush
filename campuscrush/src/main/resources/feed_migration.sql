-- Run this manually against PostgreSQL before deploying the feed feature
-- DDL_AUTO=validate requires these tables to exist before app startup

CREATE TYPE public_confession_status AS ENUM ('VISIBLE', 'HIDDEN', 'REMOVED');

CREATE TABLE public_confessions (
    id            BIGSERIAL PRIMARY KEY,
    author_id     BIGINT        NOT NULL REFERENCES users(id),
    content       VARCHAR(300)  NOT NULL,
    campus_tag    VARCHAR(32)   NOT NULL DEFAULT 'cuchd',
    status        VARCHAR(16)   NOT NULL DEFAULT 'VISIBLE',
    view_count    INT           NOT NULL DEFAULT 0,
    report_count  INT           NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE public_confession_views (
    confession_id BIGINT  NOT NULL REFERENCES public_confessions(id),
    viewer_id     BIGINT  NOT NULL REFERENCES users(id),
    PRIMARY KEY (confession_id, viewer_id)
);

CREATE TABLE public_confession_reports (
    id            BIGSERIAL PRIMARY KEY,
    confession_id BIGINT       NOT NULL REFERENCES public_confessions(id),
    reporter_id   BIGINT       NOT NULL REFERENCES users(id),
    reason        VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (confession_id, reporter_id)
);

CREATE TABLE user_blocks (
    blocker_id  BIGINT NOT NULL REFERENCES users(id),
    blocked_id  BIGINT NOT NULL REFERENCES users(id),
    PRIMARY KEY (blocker_id, blocked_id)
);

CREATE INDEX idx_public_confessions_campus_status
    ON public_confessions (campus_tag, status, id DESC);

CREATE INDEX idx_public_confessions_author
    ON public_confessions (author_id, created_at DESC);
