package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import com.hrpd.onboarding.persistence.TicketRepository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Step 5: Persist the exchange for auditing/analytics.
 * Uses reactive DatabaseClient under the hood (R2DBC).
 */
@RequiredArgsConstructor
@Slf4j
public class PersistStep implements Step {

    private final TicketRepository repo;

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        log.info("💾 PERSIST STEP: Starting data persistence");
        log.info("💾 PERSIST STEP: User text: '{}'", ctx.userText());
        log.info("💾 PERSIST STEP: Intent: '{}'", ctx.intent());
        log.info("💾 PERSIST STEP: Passages to persist: {}", ctx.passages().size());
        log.info("💾 PERSIST STEP: Answer to persist: {} characters", 
            ctx.draftAnswer() != null ? ctx.draftAnswer().length() : 0);
        
        log.info("💾 PERSIST STEP: Calling repository to save draft...");
        
        return repo
                .saveDraft(ctx.userText(), ctx.intent(), ctx.draftAnswer(), ctx.passages())
                .doOnSuccess(result -> {
                    log.info("💾 PERSIST STEP: Repository save operation completed successfully");
                })
                .doOnError(error -> {
                    log.error("💾 PERSIST STEP: Repository save operation failed: {}", error.getMessage());
                })
                .thenReturn(ctx)
                .doOnSuccess(resultCtx -> {
                    log.info("💾 PERSIST STEP: Data persistence completed successfully");
                    log.info("💾 PERSIST STEP: Context returned unchanged for final result");
                })
                .doOnError(error -> {
                    log.error("💾 PERSIST STEP: Persistence step failed: {}", error.getMessage());
                })
                .timeout(Duration.ofSeconds(5));
    }
}
