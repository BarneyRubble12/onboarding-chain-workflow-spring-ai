package com.hrpd.onboarding.chain;

import reactor.core.publisher.Mono;

/**
 * A processing step in the chain workflow.
 */
public interface Step {

    /**
     * Applies the step logic, returning the enriched context.
     *
     * @param ctx the current context
     * @return Mono emitting the next context state
     */
    Mono<Ctx> apply(Ctx ctx);
}
