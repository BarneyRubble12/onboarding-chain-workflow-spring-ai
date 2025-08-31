package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Step 3: Generate a grounded draft answer using ONLY the retrieved passages.
 * This prevents hallucinations and encourages citations.
 */
public class DraftAnswerStep implements Step {

    private final ChatModel model;

    public DraftAnswerStep(ChatModel model) {
        this.model = model;
    }

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        // Join passages into a simple, bullet-like context.
        var context = ctx
                .passages()
                .stream()
                .map(p -> "- " + p.replace("\n", " ").trim())
                .collect(Collectors.joining("\n"));

        // Grounding prompt: force the model to only use the provided context.
        String prompt = """
        Instructions:
        - Use ONLY the provided CONTEXT to answer.
        - If information is missing, say it explicitly and suggest the right channel (IT/HR/Legal).
        - Provide actionable steps.
        - Add a "References" section with [#] markers based on the order of the context items.

        CONTEXT:
        %s

        INTENT: %s
        QUESTION: %s

        Format:
        - Short, bullet-point answer (max 7 bullets).
        - Final section: "References" with [#] and a short title.
        """.formatted(context, ctx.intent(), ctx.userText());

        return Mono.fromCallable(() -> model.call(prompt))
                .publishOn(Schedulers.boundedElastic())
                .map(ctx::withDraft)
                .timeout(Duration.ofSeconds(12));
    }
}
