package com.hrpd.onboarding.controller;

import com.hrpd.onboarding.chain.OnboardingChainOrchestrator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/onboarding")
public class OnboardingController {


    /** Request DTO for /ask endpoint. */
    private record AskReq(String text) {}

    /** Response DTO for /ask endpoint. */
    private record AskRes(String intent, String answer) {}

    private final OnboardingChainOrchestrator chainOrchestrator;

    public OnboardingController(OnboardingChainOrchestrator chainOrchestrator) {
        this.chainOrchestrator = chainOrchestrator;
    }

    /**
     * Runs the full chain: classify → retrieve → draft → validate → persist.
     *
     * @param askReq user request with "text"
     * @return response with intent and grounded answer
     */
    @PostMapping(
            value = "/ask",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AskRes> ask(@RequestBody AskReq askReq) {
        return chainOrchestrator
                .run(askReq.text())
                .map(ctx -> new AskRes(ctx.intent(), ctx.draftAnswer()));
    }
}
