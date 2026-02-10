package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.LlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.LlmException;
import org.killbill.billing.plugin.helloworld.policytranslator.model.IntentExtraction;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;
import org.killbill.billing.plugin.helloworld.policytranslator.model.ValidationResult;
import org.killbill.billing.plugin.helloworld.policytranslator.validator.PolicyValidator;

/**
 * Stage B: Maps a validated IntentExtraction into the canonical PolicyJson
 * using an LLM, then validates the output. Supports a single retry with
 * validation error feedback.
 */
public class PolicyMapper {

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final PolicyValidator validator;
    private final ObjectMapper objectMapper;

    public PolicyMapper(final LlmClient llmClient, final PromptBuilder promptBuilder, final PolicyValidator validator) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.validator = validator;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Map intent extraction to canonical policy JSON.
     *
     * @param intent the validated intent extraction from Stage A
     * @return validated PolicyJson
     * @throws TranslationException if mapping or validation fails after retry
     */
    public PolicyJson map(final IntentExtraction intent) throws TranslationException {
        final String intentJson;
        try {
            intentJson = objectMapper.writeValueAsString(intent);
        } catch (final Exception e) {
            throw new TranslationException("Failed to serialize intent extraction: " + e.getMessage(), e);
        }

        final String systemPrompt = promptBuilder.buildStageBSystemPrompt();
        final String userPrompt = promptBuilder.buildStageBUserPrompt(intentJson);

        // First attempt
        PolicyJson policy = callAndParse(systemPrompt, userPrompt);
        ValidationResult validation = validator.validate(policy);

        if (validation.isValid()) {
            return policy;
        }

        // Single retry with validation error feedback
        final String errorSummary = String.join("\n", validation.getErrors());
        final String retryPrompt = promptBuilder.buildStageBRetryPrompt(intentJson, errorSummary);

        policy = callAndParse(systemPrompt, retryPrompt);
        validation = validator.validate(policy);

        if (!validation.isValid()) {
            throw new TranslationException(
                    "Policy mapping failed validation after retry. Errors: " +
                    String.join("; ", validation.getErrors()));
        }

        return policy;
    }

    private PolicyJson callAndParse(final String systemPrompt, final String userPrompt) throws TranslationException {
        final String response;
        try {
            response = llmClient.complete(systemPrompt, userPrompt);
        } catch (final LlmException e) {
            throw new TranslationException("LLM call failed during policy mapping: " + e.getMessage(), e);
        }

        try {
            return objectMapper.readValue(response, PolicyJson.class);
        } catch (final Exception e) {
            throw new TranslationException("Failed to parse policy mapping response as JSON: " + e.getMessage(), e);
        }
    }
}
