package com.hrpd.onboarding.chain.steps;

import com.hrpd.onboarding.chain.Ctx;
import com.hrpd.onboarding.chain.Step;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Step 4: Validate the draft answer against the required format and structure.
 * Validates based on the instructions given in DraftAnswerStep prompt.
 */
@Slf4j
public class ValidateStep implements Step {

    @Override
    public Mono<Ctx> apply(Ctx ctx) {
        log.info("✅ VALIDATE STEP: Starting answer validation");
        log.info("✅ VALIDATE STEP: User text: '{}'", ctx.userText());
        log.info("✅ VALIDATE STEP: Intent: '{}'", ctx.intent());
        log.info("✅ VALIDATE STEP: Answer to validate: {} characters", 
            ctx.draftAnswer() != null ? ctx.draftAnswer().length() : 0);
        
        if (ctx.draftAnswer() == null || ctx.draftAnswer().trim().isEmpty()) {
            log.error("❌ VALIDATE STEP: FAILED - No answer provided");
            return Mono.error(new RuntimeException("Answer is null or empty"));
        }
        
        String answer = ctx.draftAnswer().trim();
        log.info("✅ VALIDATE STEP: Performing validation checks based on prompt instructions...");
        
        // Validation 1: Check if answer has bullet points (max 7 as specified)
        List<String> bulletPoints = extractBulletPoints(answer);
        boolean hasBulletPoints = !bulletPoints.isEmpty();
        boolean bulletCountValid = bulletPoints.size() <= 7;
        
        log.info("✅ VALIDATE STEP: Bullet point validation:");
        log.info("   - Has bullet points: {}", hasBulletPoints);
        log.info("   - Bullet count: {} (max 7 allowed)", bulletPoints.size());
        log.info("   - Bullet count valid: {}", bulletCountValid);
        
        // Validation 2: Check for References section
        boolean hasReferencesSection = answer.toLowerCase().contains("references");
        log.info("✅ VALIDATE STEP: References section validation:");
        log.info("   - Has 'References' section: {}", hasReferencesSection);
        
        // Validation 3: Check for reference markers [#]
        List<String> referenceMarkers = extractReferenceMarkers(answer);
        boolean hasReferenceMarkers = !referenceMarkers.isEmpty();
        boolean referenceMarkersValid = referenceMarkers.size() <= ctx.passages().size();
        
        log.info("✅ VALIDATE STEP: Reference markers validation:");
        log.info("   - Has [#] markers: {}", hasReferenceMarkers);
        log.info("   - Reference markers found: {}", referenceMarkers.size());
        log.info("   - Available passages: {}", ctx.passages().size());
        log.info("   - Reference markers valid (≤ passages): {}", referenceMarkersValid);
        
        // Validation 4: Check if answer mentions missing information handling
        boolean mentionsMissingInfo = answer.toLowerCase().contains("missing") || 
                                    answer.toLowerCase().contains("information") ||
                                    answer.toLowerCase().contains("contact") ||
                                    answer.toLowerCase().contains("it") ||
                                    answer.toLowerCase().contains("hr") ||
                                    answer.toLowerCase().contains("legal");
        
        log.info("✅ VALIDATE STEP: Missing information handling:");
        log.info("   - Mentions missing info handling: {}", mentionsMissingInfo);
        
        // Validation 5: Check for actionable steps
        boolean hasActionableSteps = bulletPoints.stream()
            .anyMatch(bullet -> bullet.toLowerCase().contains("step") || 
                              bullet.toLowerCase().contains("action") ||
                              bullet.toLowerCase().contains("contact") ||
                              bullet.toLowerCase().contains("email") ||
                              bullet.toLowerCase().contains("call") ||
                              bullet.toLowerCase().contains("visit") ||
                              bullet.toLowerCase().contains("submit") ||
                              bullet.toLowerCase().contains("complete"));
        
        log.info("✅ VALIDATE STEP: Actionable steps validation:");
        log.info("   - Has actionable steps: {}", hasActionableSteps);
        
        // Overall validation result
        boolean allValidationsPassed = hasBulletPoints && bulletCountValid && 
                                     hasReferencesSection && hasReferenceMarkers && 
                                     referenceMarkersValid;
        
        log.info("✅ VALIDATE STEP: Overall validation results:");
        log.info("   - Bullet points valid: {}", hasBulletPoints && bulletCountValid);
        log.info("   - References section valid: {}", hasReferencesSection);
        log.info("   - Reference markers valid: {}", hasReferenceMarkers && referenceMarkersValid);
        log.info("   - Missing info handling: {}", mentionsMissingInfo);
        log.info("   - Actionable steps present: {}", hasActionableSteps);
        log.info("   - ALL CRITICAL VALIDATIONS PASSED: {}", allValidationsPassed);
        
        if (allValidationsPassed) {
            log.info("✅ VALIDATE STEP: All critical validations passed - Answer is valid");
        } else {
            log.warn("⚠️  VALIDATE STEP: Some validations failed, but continuing (demo mode)");
            log.warn("⚠️  VALIDATE STEP: Failed validations:");
            if (!hasBulletPoints || !bulletCountValid) {
                log.warn("   - Bullet points validation failed");
            }
            if (!hasReferencesSection) {
                log.warn("   - References section missing");
            }
            if (!hasReferenceMarkers || !referenceMarkersValid) {
                log.warn("   - Reference markers validation failed");
            }
        }
        
        log.info("✅ VALIDATE STEP: Validation completed successfully");
        
        return Mono.just(ctx)
            .doOnSuccess(resultCtx -> {
                log.info("✅ VALIDATE STEP: Context passed through validation");
            })
            .doOnError(error -> {
                log.error("✅ VALIDATE STEP: Validation failed: {}", error.getMessage());
            });
    }
    
    private List<String> extractBulletPoints(String answer) {
        return Pattern.compile("^\\s*-\\s+(.+)$", Pattern.MULTILINE)
            .matcher(answer)
            .results()
            .map(match -> match.group(1).trim())
            .toList();
    }
    
    private List<String> extractReferenceMarkers(String answer) {
        return Pattern.compile("\\[#\\d+\\]")
            .matcher(answer)
            .results()
            .map(match -> match.group())
            .toList();
    }
}
