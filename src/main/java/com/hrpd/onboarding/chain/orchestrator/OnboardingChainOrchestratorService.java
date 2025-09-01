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
        log.info("🚀 STARTING CHAIN WORKFLOW");
        log.info("📝 User Input: '{}'", userText);
        log.info("🔗 Total Steps in Chain: {}", steps.size());
        
        Ctx seed = new Ctx(userText, null, List.of(), null, new java.util.HashMap<>());
        Mono<Ctx> flow = Mono.just(seed);

        // Compose steps sequentially
        for (int i = 0; i < steps.size(); i++) {
            Step s = steps.get(i);
            final int stepNumber = i + 1;
            final String stepName = s.getClass().getSimpleName();
            
            log.info("⏭️  STEP {}: {} - Starting execution", stepNumber, stepName);
            
            flow = flow.flatMap(ctx -> {
                log.info("📊 STEP {}: {} - Input Context: intent='{}', passages={}, hasDraft={}", 
                    stepNumber, stepName, 
                    ctx.intent() != null ? ctx.intent() : "null",
                    ctx.passages().size(),
                    ctx.draftAnswer() != null ? "yes" : "no");
                
                return s.apply(ctx)
                    .doOnSuccess(resultCtx -> {
                        log.info("✅ STEP {}: {} - COMPLETED SUCCESSFULLY", stepNumber, stepName);
                        log.info("📊 STEP {}: {} - Output Context: intent='{}', passages={}, hasDraft={}", 
                            stepNumber, stepName,
                            resultCtx.intent() != null ? resultCtx.intent() : "null",
                            resultCtx.passages().size(),
                            resultCtx.draftAnswer() != null ? "yes" : "no");
                    })
                    .doOnError(error -> {
                        log.error("❌ STEP {}: {} - FAILED with error: {}", stepNumber, stepName, error.getMessage());
                    })
                    .retryWhen(
                            // retry once on transient errors (e.g., timeouts)
                            Retry.fixedDelay(1, Duration.ofMillis(200))
                    )
                    .onErrorResume(ex -> {
                        log.error("💥 STEP {}: {} - FINAL FAILURE after retry: {}", stepNumber, stepName, ex.getMessage());
                        return Mono.error(
                                new RuntimeException("Failed in " + stepName + ": " + ex.getMessage(), ex)
                        );
                    });
            });
        }
        
        return flow.doOnSuccess(finalCtx -> {
            log.info("🎉 CHAIN WORKFLOW COMPLETED SUCCESSFULLY!");
            log.info("📋 Final Result Summary:");
            log.info("   - Intent: {}", finalCtx.intent());
            log.info("   - Passages Retrieved: {}", finalCtx.passages().size());
            log.info("   - Draft Answer Length: {} characters", 
                finalCtx.draftAnswer() != null ? finalCtx.draftAnswer().length() : 0);
        }).doOnError(error -> {
            log.error("💥 CHAIN WORKFLOW FAILED: {}", error.getMessage());
        });
    }
}
