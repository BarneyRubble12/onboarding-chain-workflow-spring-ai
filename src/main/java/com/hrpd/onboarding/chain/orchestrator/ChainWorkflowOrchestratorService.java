package com.hrpd.onboarding.chain.orchestrator;

import com.hrpd.onboarding.chain.Ctx;
import reactor.core.publisher.Mono;

/**
 * Defines the contract that should follow an orchestrator service implementation
 */
public interface ChainWorkflowOrchestratorService {

    /**
     * Kicks off the chain for a single user utterance.
     *
     * @param userText raw user input
     * @return a Mono emitting the final context if all steps succeed
     */
    Mono<Ctx> run(String userText);
}
