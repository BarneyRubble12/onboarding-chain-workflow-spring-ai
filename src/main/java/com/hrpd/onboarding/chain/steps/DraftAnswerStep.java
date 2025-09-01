package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.stream.Collectors;

/**
 * Step 3: Generate a grounded draft answer using ONLY the retrieved passages.
 * This prevents hallucinations and encourages citations.
 */
@RequiredArgsConstructor
@Slf4j
public class DraftAnswerStep implements Step {

    private final ChatModel model;

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        log.info("✍️  DRAFT ANSWER STEP: Starting answer generation");
        log.info("✍️  DRAFT ANSWER STEP: User text: '{}'", ctx.userText());
        log.info("✍️  DRAFT ANSWER STEP: Intent: '{}'", ctx.intent());
        log.info("✍️  DRAFT ANSWER STEP: Available passages: {}", ctx.passages().size());
        
        // Join passages into a simple, bullet-like context.
        var context = ctx
                .passages()
                .stream()
                .map(p -> "- " + p.replace("\n", " ").trim())
                .collect(Collectors.joining("\n"));

        log.info("✍️  DRAFT ANSWER STEP: Prepared context with {} passages", ctx.passages().size());
        log.debug("✍️  DRAFT ANSWER STEP: Context length: {} characters", context.length());

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

        log.info("✍️  DRAFT ANSWER STEP: Sending generation prompt to LLM");
        log.debug("✍️  DRAFT ANSWER STEP: Prompt length: {} characters", prompt.length());

        return Mono.fromCallable(() -> {
                log.info("✍️  DRAFT ANSWER STEP: Calling LLM for answer generation...");
                return model.call(prompt);
            })
            .publishOn(Schedulers.boundedElastic())
            .doOnSuccess(rawResponse -> {
                log.info("✍️  DRAFT ANSWER STEP: LLM response received");
                log.info("✍️  DRAFT ANSWER STEP: Generated answer length: {} characters", 
                    rawResponse != null ? rawResponse.length() : 0);
                log.debug("✍️  DRAFT ANSWER STEP: Generated answer: {}", rawResponse);
            })
            .doOnError(error -> {
                log.error("✍️  DRAFT ANSWER STEP: Answer generation failed: {}", error.getMessage());
            })
            .map(ctx::withDraft)
            .doOnSuccess(resultCtx -> {
                log.info("✍️  DRAFT ANSWER STEP: Answer generation completed successfully");
                log.info("✍️  DRAFT ANSWER STEP: Final answer length: {} characters", 
                    resultCtx.draftAnswer() != null ? resultCtx.draftAnswer().length() : 0);
            })
            .timeout(Duration.ofSeconds(12));
    }
}
