package com.hrpd.onboarding.controller;

import com.hrpd.onboarding.rag.IngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Admin API to ingest content into the VectorStore.
 * POST /admin/ingest
 * {
 *   "texts": ["doc1", "doc2"],
 *   "metadata": {"locale":"en","domain":"onboarding"}
 * }
 */
@RestController
@RequestMapping("/admin/ingest")
public class IngestController {

    private final IngestionService ingestionService;

    public IngestController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Ingests texts with optional metadata into the VectorStore.
     *
     * @param body JSON payload containing "texts" and optional "metadata"
     * @return Mono that completes when ingestion is done
     */
    @PostMapping
    @SuppressWarnings("unchecked")
    public Mono<Void> ingest(@RequestBody Map<String, Object> body) {
        List<String> texts = (List<String>) body.get("texts");
        Map<String,Object> meta = (Map<String, Object>) body.getOrDefault("metadata", Map.of());

        if (texts == null || texts.isEmpty()) {
            return Mono.error(new IllegalArgumentException("'texts' must be a non-empty array"));
        }

        return ingestionService.addDocs(texts, meta);
    }

}
