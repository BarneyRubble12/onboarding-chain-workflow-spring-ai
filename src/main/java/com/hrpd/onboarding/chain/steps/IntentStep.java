package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Step 1: Classify user input into a predefined label.
 */
@RequiredArgsConstructor
@Slf4j
public class IntentStep implements Step {

    private final ChatModel model;

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        log.info("🎯 INTENT STEP: Starting intent classification");
        log.info("🎯 INTENT STEP: User text: '{}'", ctx.userText());
        
        var prompt = """
          You are a classifier. Respond with ONLY ONE label:
          [ONBOARDING_IT, ONBOARDING_HR, BENEFITS, VACATIONS, POLICIES, OTHER]
          Text: "%s"
        """.formatted(ctx.userText());

        log.info("🎯 INTENT STEP: Sending classification prompt to LLM");
        log.debug("🎯 INTENT STEP: Prompt: {}", prompt);

        return Mono
                .fromCallable(() -> {
                    log.info("🎯 INTENT STEP: Calling LLM for classification...");
                    return model.call(prompt);
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(rawResponse -> {
                    log.info("🎯 INTENT STEP: LLM response received: '{}'", rawResponse);
                })
                .map(label -> {
                    String cleanLabel = label.trim().toUpperCase();
                    log.info("🎯 INTENT STEP: Classified intent: '{}'", cleanLabel);
                    return ctx.withIntent(cleanLabel);
                })
                .doOnSuccess(resultCtx -> {
                    log.info("🎯 INTENT STEP: Intent classification completed successfully");
                })
                .doOnError(error -> {
                    log.error("🎯 INTENT STEP: Intent classification failed: {}", error.getMessage());
                })
                .timeout(Duration.ofSeconds(10));
    }
}
