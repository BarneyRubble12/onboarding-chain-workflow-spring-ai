package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Step 4: Validate the draft answer (placeholder implementation).
 * In a real implementation, this would validate structure, compliance, etc.
 */
@Slf4j
public class ValidateStep implements Step {

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        // For now, just pass through the context without validation
        // In a real implementation, you might validate:
        // - Answer has references
        // - Answer follows required format
        // - Answer doesn't contain sensitive information
        return Mono.just(ctx);
    }
}
