package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.dsl.EnglishDslRenderer;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.LlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.model.IntentExtraction;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;
import org.killbill.billing.plugin.helloworld.policytranslator.model.TranslationResult;
import org.killbill.billing.plugin.helloworld.policytranslator.model.ValidationResult;
import org.killbill.billing.plugin.helloworld.policytranslator.normalizer.PolicyNormalizer;
import org.killbill.billing.plugin.helloworld.policytranslator.validator.PolicyValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the two-stage translation pipeline:
 * 1. Stage A: Intent extraction from natural language
 * 2. Stage B: Mapping intent to canonical policy JSON
 * Plus normalization, validation, and English DSL rendering.
 */
public class PolicyTranslator {

    private final IntentExtractor intentExtractor;
    private final PolicyMapper policyMapper;
    private final PolicyNormalizer normalizer;
    private final PolicyValidator validator;
    private final EnglishDslRenderer dslRenderer;

    public PolicyTranslator(final LlmClient llmClient, final ConceptRegistry registry) {
        final PromptBuilder promptBuilder = new PromptBuilder(registry);
        this.validator = new PolicyValidator(registry);
        this.normalizer = new PolicyNormalizer();
        this.dslRenderer = new EnglishDslRenderer();
        this.intentExtractor = new IntentExtractor(llmClient, promptBuilder, registry);
        this.policyMapper = new PolicyMapper(llmClient, promptBuilder, validator);
    }

    /**
     * Translate natural language text into a canonical billing policy.
     *
     * @param naturalLanguageText the user's billing rule description
     * @param createdBy           the user who requested the translation
     * @return translation result with policy JSON, DSL, validation, and assumptions
     * @throws TranslationException if translation fails unrecoverably
     */
    public TranslationResult translate(final String naturalLanguageText, final String createdBy) throws TranslationException {
        final TranslationResult result = new TranslationResult();
        final List<String> assumptions = new ArrayList<>();

        // Stage A: Extract intent
        final IntentExtraction intent = intentExtractor.extract(naturalLanguageText);
        assumptions.addAll(intent.getNotes());

        // Stage B: Map to canonical policy JSON
        PolicyJson policy = policyMapper.map(intent);

        // Normalize
        policy = normalizer.normalize(policy, createdBy);

        // Final validation
        final ValidationResult validation = validator.validate(policy);

        // Render English DSL from canonical JSON (not from LLM)
        final String englishDsl = dslRenderer.render(policy);

        result.setPolicyJson(policy);
        result.setEnglishDsl(englishDsl);
        result.setValidation(validation);
        result.setAssumptions(assumptions);
        result.setQuestions(intent.getMissingInfoQuestions());

        return result;
    }
}
