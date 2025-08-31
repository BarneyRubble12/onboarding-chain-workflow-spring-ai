package com.hrpd.onboarding.controller;

import com.hrpd.onboarding.chain.steps.IntentStep;
import com.hrpd.onboarding.chain.steps.RetrieveStep;
import com.hrpd.onboarding.chain.Ctx;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/test")
public class TestController {

    private final IntentStep intentStep;
    private final RetrieveStep retrieveStep;

    public TestController(IntentStep intentStep, RetrieveStep retrieveStep) {
        this.intentStep = intentStep;
        this.retrieveStep = retrieveStep;
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("Application is running!");
    }

    @GetMapping("/intent")
    public Mono<String> testIntent(@RequestParam String text) {
        Ctx ctx = new Ctx(text, null, null, null, null);
        return intentStep.apply(ctx)
                .map(Ctx::intent)
                .onErrorReturn("ERROR: " + text);
    }

    @GetMapping("/retrieve")
    public Mono<String> testRetrieve(@RequestParam String text) {
        Ctx ctx = new Ctx(text, "ONBOARDING_IT", null, null, null);
        return retrieveStep.apply(ctx)
                .map(Ctx::passages)
                .map(passages -> "Found " + passages.size() + " passages")
                .onErrorReturn("ERROR: " + text);
    }
}
