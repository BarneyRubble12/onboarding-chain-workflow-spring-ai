package com.hrpd.onboarding.chain.orchestrator;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import com.hrpd.onboarding.chain.steps.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Orchestrates the chain workflow:<br>
 *  1. IntentStep      - classify user intent<br>
 *  2. RetrieveStep    - fetch top-k relevant passages from VectorStore<br>
 *  3. DraftAnswerStep - ask LLM to draft an answer using ONLY the passages<br>
 *  4. ValidateStep    - ensure structure/compliance (e.g., has references)<br>
 *  5. PersistStep     - persist the result (auditing/analytics)<br>
 *<br>
 * Notes:
 *  <li>Each step runs with timeouts and light retry to improve resiliency.</li>
 *  <li>Any failure fails the whole chain (propagates an error).</li>
 */
@RequiredArgsConstructor
@Slf4j
public class OnboardingChainOrchestratorService implements ChainWorkflowOrchestratorService {

    private final List<Step> steps;

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
