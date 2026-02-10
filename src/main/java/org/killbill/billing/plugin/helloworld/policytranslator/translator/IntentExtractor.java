package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.LlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.LlmException;
import org.killbill.billing.plugin.helloworld.policytranslator.model.IntentExtraction;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage A: Extracts structured intent fields from natural language text using an LLM.
 */
public class IntentExtractor {

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ConceptRegistry registry;
    private final ObjectMapper objectMapper;

    public IntentExtractor(final LlmClient llmClient, final PromptBuilder promptBuilder, final ConceptRegistry registry) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.registry = registry;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Extract intent from natural language text.
     *
     * @param naturalLanguageText the user's billing rule description
     * @return extracted intent
     * @throws TranslationException if extraction or validation fails
     */
    public IntentExtraction extract(final String naturalLanguageText) throws TranslationException {
        if (naturalLanguageText == null || naturalLanguageText.trim().isEmpty()) {
            throw new TranslationException("Input text is empty");
        }

        if (naturalLanguageText.length() > 2000) {
            throw new TranslationException("Input text exceeds maximum length of 2000 characters");
        }

        final String systemPrompt = promptBuilder.buildStageASystemPrompt();
        final String userPrompt = promptBuilder.buildStageAUserPrompt(naturalLanguageText);

        final String response;
        try {
            response = llmClient.complete(systemPrompt, userPrompt);
        } catch (final LlmException e) {
            throw new TranslationException("LLM call failed during intent extraction: " + e.getMessage(), e);
        }

        final IntentExtraction intent;
        try {
            intent = objectMapper.readValue(response, IntentExtraction.class);
        } catch (final Exception e) {
            throw new TranslationException("Failed to parse intent extraction response as JSON: " + e.getMessage(), e);
        }

        // Validate extracted fields against concept registry
        final List<String> errors = validateIntent(intent);
        if (!errors.isEmpty()) {
            throw new TranslationException("Intent extraction produced invalid values: " + String.join("; ", errors));
        }

        return intent;
    }

    private List<String> validateIntent(final IntentExtraction intent) {
        final List<String> errors = new ArrayList<>();

        // customer_segment: must be known or "unknown"
        final String segment = intent.getCustomerSegment();
        if (segment != null && !"unknown".equals(segment) && !registry.isValidCustomerSegment(segment)) {
            errors.add("Unknown customer_segment: '" + segment + "'. Allowed: " + registry.getCustomerSegments());
        }
        if ("unknown".equals(segment)) {
            errors.add("Could not determine customer segment from input");
        }

        // trigger_event: must be known or "unknown"
        final String triggerEvent = intent.getTriggerEvent();
        if (triggerEvent != null && !"unknown".equals(triggerEvent) && !registry.isValidEvent(triggerEvent)) {
            errors.add("Unknown trigger_event: '" + triggerEvent + "'. Allowed: " + registry.getEvents());
        }

        // scope: must be known or "unknown"
        final String scope = intent.getScope();
        if (scope != null && !"unknown".equals(scope) && !registry.getConditionScopes().contains(scope)) {
            errors.add("Unknown scope: '" + scope + "'. Allowed: " + registry.getConditionScopes());
        }
        if ("unknown".equals(scope)) {
            errors.add("Could not determine scope from input. Only 'lifetime' is supported.");
        }

        // discount_percent: basic range check
        if (intent.getDiscountPercent() <= 0) {
            errors.add("discount_percent must be positive, got: " + intent.getDiscountPercent());
        }
        if (intent.getDiscountPercent() > registry.getBenefitMaxPercent()) {
            errors.add("discount_percent " + intent.getDiscountPercent() + " exceeds maximum of " + registry.getBenefitMaxPercent());
        }

        // cadence_every_nth_purchase: range check
        if (intent.getCadenceEveryNthPurchase() < registry.getConditionNMin()) {
            errors.add("cadence_every_nth_purchase " + intent.getCadenceEveryNthPurchase() +
                       " is below minimum of " + registry.getConditionNMin());
        }
        if (intent.getCadenceEveryNthPurchase() > registry.getConditionNMax()) {
            errors.add("cadence_every_nth_purchase " + intent.getCadenceEveryNthPurchase() +
                       " exceeds maximum of " + registry.getConditionNMax());
        }

        return errors;
    }
}
