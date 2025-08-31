-- src/main/resources/db/migration/V2__documents.sql
-- Documents table used by Spring AI's pgvector VectorStore implementation
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS documents (
  id BIGSERIAL PRIMARY KEY,
  content  TEXT NOT NULL,
  metadata JSONB,
  embedding VECTOR(1536),     -- must match spring.ai.vectorstore.dimensions
  created_at TIMESTAMPTZ DEFAULT now()
);

-- ANN index for faster similarity search on large corpora
-- cosine ops here since we configured cosine similarity
CREATE INDEX IF NOT EXISTS idx_documents_embedding
  ON documents USING ivfflat (embedding vector_cosine_ops)
  WITH (lists = 100);
