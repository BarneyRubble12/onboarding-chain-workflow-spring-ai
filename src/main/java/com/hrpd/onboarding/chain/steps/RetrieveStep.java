package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Step 2: Retrieve relevant passages for grounding.
 */
@RequiredArgsConstructor
@Slf4j
public class RetrieveStep implements Step {

    private final VectorStore vectorStore;

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        log.info("🔍 RETRIEVE STEP: Starting vector search");
        log.info("🔍 RETRIEVE STEP: User text: '{}'", ctx.userText());
        log.info("🔍 RETRIEVE STEP: Intent: '{}'", ctx.intent());
        
        var query = (ctx.intent() != null ? ctx.intent() + " :: " : "") + ctx.userText();
        log.info("🔍 RETRIEVE STEP: Search query: '{}'", query);

        return Mono
                .fromCallable(() -> {
                    log.info("🔍 RETRIEVE STEP: Building search request...");
                    var request = SearchRequest
                                    .builder()
                                    .query(query)
                                    .topK(6)
                                    .similarityThreshold(0.0) // accept all; tighten if you want filtering by score
                                    // .filterExpression("locale == 'en' && domain == 'onboarding'") // optional
                                    .build();
                    
                    log.info("🔍 RETRIEVE STEP: Executing vector search with topK=6...");
                    var docs = vectorStore.similaritySearch(request);
                    log.info("🔍 RETRIEVE STEP: Found {} documents in vector store", docs.size());
                    
                    var passages = docs
                            .stream()
                            .map(Document::getFormattedContent)
                            .toList();
                    
                    log.info("🔍 RETRIEVE STEP: Extracted {} passages", passages.size());
                    for (int i = 0; i < passages.size(); i++) {
                        String passage = passages.get(i);
                        log.debug("🔍 RETRIEVE STEP: Passage {}: {}...", i + 1, 
                            passage.length() > 100 ? passage.substring(0, 100) + "..." : passage);
                    }
                    
                    return passages;
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(passages -> {
                    log.info("🔍 RETRIEVE STEP: Vector search completed successfully");
                    log.info("🔍 RETRIEVE STEP: Retrieved {} relevant passages", passages.size());
                })
                .doOnError(error -> {
                    log.error("🔍 RETRIEVE STEP: Vector search failed: {}", error.getMessage());
                })
                .map(ctx::withPassages)
                .doOnSuccess(resultCtx -> {
                    log.info("🔍 RETRIEVE STEP: Context updated with {} passages", resultCtx.passages().size());
                })
                .timeout(Duration.ofSeconds(10));
    }
}
