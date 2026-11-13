--liquibase formatted sql

--changeset deep-reader:1
CREATE UNLOGGED TABLE ai_cache_entries (
    hash_key      VARCHAR(64)              NOT NULL,
    cache_type    VARCHAR(16)              NOT NULL,
    response_data TEXT                     NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_ai_cache PRIMARY KEY (hash_key)
);

CREATE INDEX idx_ai_cache_expires_at ON ai_cache_entries (expires_at);
