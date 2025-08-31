-- src/main/resources/db/migration/V1__init.sql
-- Create extension and app table to persist chain results (drafts)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS drafts (
  id           BIGSERIAL PRIMARY KEY,
  user_text    TEXT NOT NULL,
  intent       TEXT NOT NULL,
  draft_answer TEXT NOT NULL,
  passages     JSONB NOT NULL,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_drafts_created_at ON drafts (created_at);
