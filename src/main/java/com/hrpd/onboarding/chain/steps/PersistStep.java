package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import com.hrpd.onboarding.persistence.TicketRepository.TicketRepository;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Step 5: Persist the exchange for auditing/analytics.
 * Uses reactive DatabaseClient under the hood (R2DBC).
 */
public class PersistStep implements Step {

    private final TicketRepository repo;

    public PersistStep(TicketRepository repo) {
        this.repo = repo;
    }

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        return repo
                .saveDraft(ctx.userText(), ctx.intent(), ctx.draftAnswer(), ctx.passages())
                .thenReturn(ctx)
                .timeout(Duration.ofSeconds(5));
    }
}
