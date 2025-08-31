package com.hrpd.onboarding.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * High-level ingestion service that converts raw texts to
 * Spring AI Documents and adds them to the configured VectorStore.
 *
 * Note: Spring AI will call the EmbeddingModel for each document,
 * then store content + metadata + embedding into pgvector automatically.
 */
@Service
public class IngestionService {

    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Adds a batch of documents with shared metadata.
     *
     * @param texts   raw content items to index
     * @param commonMeta optional metadata attached to each document (nullable)
     * @return Mono that completes when all documents are added
     */
    public Mono<Void> addDocs(List<String> texts, Map<String, Object> commonMeta) {
        var docs = texts.stream()
                .map(t -> new Document(t, commonMeta == null ? Map.of() : commonMeta))
                .toList();

        return Mono.fromCallable(() -> {
            vectorStore.add(docs);
            return null;
        }).publishOn(Schedulers.boundedElastic()).then();
    }

}
