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
        var prompt = """
          You are a classifier. Respond with ONLY ONE label:
          [ONBOARDING_IT, ONBOARDING_HR, BENEFITS, VACATIONS, POLICIES, OTHER]
          Text: "%s"
        """.formatted(ctx.userText());

        return Mono
                .fromCallable(() -> model.call(prompt))
                .publishOn(Schedulers.boundedElastic())
                .map(label -> ctx.withIntent(label.trim().toUpperCase()))
                .timeout(Duration.ofSeconds(10));
    }
}
