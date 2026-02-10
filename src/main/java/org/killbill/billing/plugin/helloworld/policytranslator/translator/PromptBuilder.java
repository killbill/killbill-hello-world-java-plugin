package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Constructs system and user prompts for Stage A (intent extraction) and Stage B (policy mapping).
 */
public class PromptBuilder {

    private static final String STAGE_A_EXAMPLES = "/policytranslator/examples/few-shot-stage-a.json";
    private static final String STAGE_B_EXAMPLES = "/policytranslator/examples/few-shot-stage-b.json";

    private final ConceptRegistry registry;

    public PromptBuilder(final ConceptRegistry registry) {
        this.registry = registry;
    }

    /**
     * Build the system prompt for Stage A: intent extraction.
     */
    public String buildStageASystemPrompt() {
        final String examples = loadResource(STAGE_A_EXAMPLES);
        return "You are a billing policy intent extraction system. Your task is to extract structured fields " +
               "from a natural-language billing rule description.\n\n" +
               "You must output JSON only, no prose, no markdown.\n\n" +
               registry.toPromptSummary() + "\n" +
               "Output schema:\n" +
               "{\n" +
               "  \"customer_segment\": \"returning|new|all|unknown\",\n" +
               "  \"discount_percent\": <integer>,\n" +
               "  \"cadence_every_nth_purchase\": <integer>,\n" +
               "  \"promo_code_required\": <boolean>,\n" +
               "  \"trigger_event\": \"purchase_priced|unknown\",\n" +
               "  \"scope\": \"lifetime|unknown\",\n" +
               "  \"notes\": [<string>],\n" +
               "  \"missing_info_questions\": [<string>]\n" +
               "}\n\n" +
               "Rules:\n" +
               "- Only use values from the allowed concepts list.\n" +
               "- If a concept is not mentioned, use the most reasonable default (e.g., trigger_event=purchase_priced, scope=lifetime).\n" +
               "- If no customer segment is specified, use \"all\".\n" +
               "- If promo code is not mentioned, default to false (not required).\n" +
               "- Treat \"order\", \"transaction\", \"purchase\" as synonyms for \"purchase\".\n" +
               "- If you cannot map the input to supported concepts, set the field to \"unknown\".\n" +
               "- Add explanatory notes for any assumptions you make.\n\n" +
               "Few-shot examples:\n" + examples;
    }

    /**
     * Build the user prompt for Stage A.
     */
    public String buildStageAUserPrompt(final String naturalLanguageText) {
        return "Extract structured billing policy intent from this description:\n\n\"" + naturalLanguageText + "\"";
    }

    /**
     * Build the system prompt for Stage B: mapping intent to canonical policy JSON.
     */
    public String buildStageBSystemPrompt() {
        final String examples = loadResource(STAGE_B_EXAMPLES);
        return "You are a billing policy schema mapper. Your task is to Map the following intent extraction JSON " +
               "into a canonical billing policy JSON document.\n\n" +
               "You must output JSON only, no prose, no markdown.\n\n" +
               registry.toPromptSummary() + "\n" +
               "The canonical policy JSON must conform to this structure:\n" +
               "{\n" +
               "  \"dsl_version\": \"billing-intent/0.1\",\n" +
               "  \"policy_id\": \"<segment>.every_<n>th_purchase_<percent>pct\" (use 2nd, 3rd for n=2,3),\n" +
               "  \"description\": \"<human readable description>\",\n" +
               "  \"applies_to\": { \"object\": \"purchase\" },\n" +
               "  \"eligibility\": { \"customer_segment\": \"<segment>\" },\n" +
               "  \"trigger\": { \"event\": \"purchase_priced\", \"timing\": \"before_invoice_finalized\" },\n" +
               "  \"condition\": { \"kind\": \"every_nth_purchase\", \"n\": <int>, \"basis\": \"successful_purchases\", \"scope\": \"lifetime\" },\n" +
               "  \"benefit\": { \"kind\": \"percentage_discount\", \"value_percent\": <int>, \"target\": \"all_line_items\", \"promo_code_required\": <bool> },\n" +
               "  \"audit\": { \"reason_code\": \"LOYALTY_EVERY_<N>TH\" (uppercase, use 2ND, 3RD for n=2,3), \"label\": \"Loyalty reward\" },\n" +
               "  \"metadata\": { \"created_by\": \"system\", \"created_at\": \"<ISO-8601>\", \"source\": \"nl_translation_poc\" }\n" +
               "}\n\n" +
               "Rules:\n" +
               "- Only use values from the allowed concepts list.\n" +
               "- All fields are required.\n" +
               "- Do not invent new fields.\n\n" +
               "Few-shot examples:\n" + examples;
    }

    /**
     * Build the user prompt for Stage B.
     */
    public String buildStageBUserPrompt(final String intentExtractionJson) {
        return "Map the following intent extraction JSON into a canonical billing policy JSON:\n\n" + intentExtractionJson;
    }

    /**
     * Build a retry user prompt for Stage B when validation failed.
     */
    public String buildStageBRetryPrompt(final String intentExtractionJson, final String validationErrors) {
        return "Your previous output had validation errors. Please fix them and output valid canonical policy JSON.\n\n" +
               "Validation errors:\n" + validationErrors + "\n\n" +
               "Original intent extraction:\n" + intentExtractionJson;
    }

    private static String loadResource(final String resourcePath) {
        try (final InputStream is = PromptBuilder.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return "[]";
            }
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (final IOException e) {
            return "[]";
        }
    }
}
