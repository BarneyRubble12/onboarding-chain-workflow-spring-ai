package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Step 2: Retrieve relevant passages for grounding.
 */
@Component
public class RetrieveStep implements Step {

    private final VectorStore vectorStore;

    public RetrieveStep(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        var query = (ctx.intent() != null ? ctx.intent() + " :: " : "") + ctx.userText();

        return Mono
                .fromCallable(() -> {
                    var request = SearchRequest
                                    .builder()
                                    .query(query)
                                    .topK(6)
                                    .similarityThreshold(0.0) // accept all; tighten if you want filtering by score
                                    // .filterExpression("locale == 'en' && domain == 'onboarding'") // optional
                                    .build();
                    var docs = vectorStore.similaritySearch(request);
                    return docs
                            .stream()
                            .map(Document::getFormattedContent)
                            .toList();
                })
                .publishOn(Schedulers.boundedElastic())
                .map(ctx::withPassages)
                .timeout(Duration.ofSeconds(10));
    }
}
