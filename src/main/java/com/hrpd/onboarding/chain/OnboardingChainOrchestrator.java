package com.hrpd.onboarding.chain;

import com.hrpd.onboarding.chain.steps.*;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Orchestrates the chain workflow:
 *  1) IntentStep      - classify user intent
 *  2) RetrieveStep    - fetch top-k relevant passages from VectorStore
 *  3) DraftAnswerStep - ask LLM to draft an answer using ONLY the passages
 *  4) ValidateStep    - ensure structure/compliance (e.g., has references)
 *  5) PersistStep     - persist the result (auditing/analytics)
 *
 * Notes:
 *  - Each step runs with timeouts and light retry to improve resiliency.
 *  - Any failure fails the whole chain (propagates an error).
 */
@Service
public class OnboardingChainOrchestrator {

    private final List<Step> steps;

    public OnboardingChainOrchestrator(
            IntentStep s1, RetrieveStep s2, DraftAnswerStep s3, ValidateStep s4, PersistStep s5
    ) {
        this.steps = List.of(s1, s2, s3, s4, s5);
    }

    /**
     * Kicks off the chain for a single user utterance.
     *
     * @param userText raw user input
     * @return a Mono emitting the final context if all steps succeed
     */
    public Mono<Ctx> run(String userText) {
        Ctx seed = new Ctx(userText, null, List.of(), null, new java.util.HashMap<>());
        Mono<Ctx> flow = Mono.just(seed);

        // Compose steps sequentially
        for (Step s : steps) {
            flow = flow.flatMap(ctx -> s.apply(ctx)
                    .retryWhen(
                            // retry once on transient errors (e.g., timeouts)
                            Retry.fixedDelay(1, Duration.ofMillis(200))
                    )
                    .onErrorResume(ex -> Mono.error(
                            new RuntimeException("Failed in " + s.getClass().getSimpleName() + ": " + ex.getMessage(), ex)
                    )));
        }
        return flow;
    }
}
