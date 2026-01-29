-- Init schema for subscription app (PostgreSQL)
CREATE SCHEMA IF NOT EXISTS subscription_app;

CREATE TABLE IF NOT EXISTS subscription_app.app_user (
    email       VARCHAR(255) PRIMARY KEY,
    hash        VARCHAR(64)  NOT NULL,
    salt        VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
