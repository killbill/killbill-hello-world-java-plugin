package org.killbill.billing.plugin.helloworld.policytranslator.llm;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic mock LLM client that pattern-matches on input text and returns
 * hard-coded Stage A / Stage B JSON responses. Supports all golden test fixtures.
 */
public class MockLlmClient implements LlmClient {

    // Patterns for extracting numbers from text
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+)\\s*(%|percent|pct)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NTH_PATTERN = Pattern.compile("every\\s+(\\d+)(?:st|nd|rd|th)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NTH_WORD_PATTERN = Pattern.compile("every\\s+(second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth|eleventh|twelfth)", Pattern.CASE_INSENSITIVE);

    @Override
    public String complete(final String systemPrompt, final String userPrompt) throws LlmException {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new LlmException("Empty input text");
        }

        // Detect whether this is a Stage A (intent extraction) or Stage B (policy mapping) call
        // Check Stage B first because its prompt also contains "intent extraction" in context
        if (systemPrompt.contains("canonical policy") || systemPrompt.contains("Stage B") || systemPrompt.contains("schema mapper")) {
            return handleStageB(userPrompt);
        } else if (systemPrompt.contains("intent extraction") || systemPrompt.contains("Stage A") || systemPrompt.contains("extract structured fields")) {
            return handleStageA(userPrompt);
        }

        // Default: try intent extraction
        return handleStageA(userPrompt);
    }

    private String handleStageA(final String text) throws LlmException {
        final String lower = text.toLowerCase(Locale.ROOT);

        // Check for unsupported concepts - fail early
        if (lower.contains("invoice") && !lower.contains("purchase")) {
            throw new LlmException("Cannot map input to supported concepts: 'invoice' is not a supported applies_to object");
        }
        if (lower.contains("every third month") || lower.contains("every month") || lower.contains("monthly")) {
            throw new LlmException("Cannot map input to supported concepts: temporal cadence 'month' is not supported, only 'every_nth_purchase'");
        }
        if (lower.contains("last 30 days") || lower.contains("past 30 days") || lower.contains("last month")) {
            throw new LlmException("Cannot map input to supported concepts: scope 'last_30_days' is not supported, only 'lifetime'");
        }

        // Extract segment
        String segment = "all";
        if (lower.contains("returning")) {
            segment = "returning";
        } else if (lower.contains("new customer") || lower.contains("new ")) {
            segment = "new";
        }

        // Extract discount percent
        int percent = 0;
        final Matcher pctMatcher = PERCENT_PATTERN.matcher(text);
        if (pctMatcher.find()) {
            percent = Integer.parseInt(pctMatcher.group(1));
        }

        if (percent == 0) {
            throw new LlmException("Could not extract discount percentage from input");
        }

        // Extract nth purchase
        int nth = 0;
        final Matcher nthMatcher = NTH_PATTERN.matcher(text);
        if (nthMatcher.find()) {
            nth = Integer.parseInt(nthMatcher.group(1));
        }
        if (nth == 0) {
            final Matcher nthWordMatcher = NTH_WORD_PATTERN.matcher(text);
            if (nthWordMatcher.find()) {
                nth = wordToNumber(nthWordMatcher.group(1));
            }
        }

        if (nth == 0) {
            throw new LlmException("Could not extract purchase cadence (every Nth) from input");
        }

        // Extract promo code requirement
        boolean promoRequired = false;
        if (lower.contains("requires promo") || lower.contains("require promo") ||
            lower.contains("promo code required") || lower.contains("with promo")) {
            promoRequired = true;
        }
        // "no promo code" explicitly means not required
        if (lower.contains("no promo") || lower.contains("without promo")) {
            promoRequired = false;
        }

        return buildStageAJson(segment, percent, nth, promoRequired);
    }

    private String handleStageB(final String intentJson) throws LlmException {
        // Parse minimal fields from the intent JSON using simple string matching
        // (Avoids circular dependency on ObjectMapper in the mock)
        final String segment = extractJsonString(intentJson, "customer_segment");
        final int percent = extractJsonInt(intentJson, "discount_percent");
        final int nth = extractJsonInt(intentJson, "cadence_every_nth_purchase");
        final boolean promoRequired = extractJsonBoolean(intentJson, "promo_code_required");

        final String ordinal = toOrdinalSuffix(nth);
        final String policyId = segment + ".every_" + ordinal + "_purchase_" + percent + "pct";
        final String promoText = promoRequired ? "with" : "without";
        final String description = percent + "% off every " + ordinal + " purchase for " + segment + " customers " + promoText + " a promo code.";
        final String reasonCode = "LOYALTY_EVERY_" + nth + toOrdinalSuffixUpper(nth);

        return buildStageBJson(policyId, description, segment, nth, percent, promoRequired, reasonCode);
    }

    private static String buildStageAJson(final String segment, final int percent, final int nth, final boolean promoRequired) {
        return "{\n" +
               "  \"customer_segment\": \"" + segment + "\",\n" +
               "  \"discount_percent\": " + percent + ",\n" +
               "  \"cadence_every_nth_purchase\": " + nth + ",\n" +
               "  \"promo_code_required\": " + promoRequired + ",\n" +
               "  \"trigger_event\": \"purchase_priced\",\n" +
               "  \"scope\": \"lifetime\",\n" +
               "  \"notes\": [\"Interpreted as every " + toOrdinalSuffix(nth) + " successful purchase over lifetime.\"],\n" +
               "  \"missing_info_questions\": []\n" +
               "}";
    }

    private static String buildStageBJson(final String policyId, final String description,
                                          final String segment, final int nth, final int percent,
                                          final boolean promoRequired, final String reasonCode) {
        return "{\n" +
               "  \"dsl_version\": \"billing-intent/0.1\",\n" +
               "  \"policy_id\": \"" + policyId + "\",\n" +
               "  \"description\": \"" + description + "\",\n" +
               "  \"applies_to\": { \"object\": \"purchase\" },\n" +
               "  \"eligibility\": { \"customer_segment\": \"" + segment + "\" },\n" +
               "  \"trigger\": { \"event\": \"purchase_priced\", \"timing\": \"before_invoice_finalized\" },\n" +
               "  \"condition\": { \"kind\": \"every_nth_purchase\", \"n\": " + nth + ", \"basis\": \"successful_purchases\", \"scope\": \"lifetime\" },\n" +
               "  \"benefit\": { \"kind\": \"percentage_discount\", \"value_percent\": " + percent + ", \"target\": \"all_line_items\", \"promo_code_required\": " + promoRequired + " },\n" +
               "  \"audit\": { \"reason_code\": \"" + reasonCode + "\", \"label\": \"Loyalty reward\" },\n" +
               "  \"metadata\": { \"created_by\": \"system\", \"created_at\": \"2026-02-10T00:00:00Z\", \"source\": \"nl_translation_poc\" }\n" +
               "}";
    }

    private static String toOrdinalSuffix(final int n) {
        switch (n) {
            case 2: return "2nd";
            case 3: return "3rd";
            default: return n + "th";
        }
    }

    private static String toOrdinalSuffixUpper(final int n) {
        switch (n) {
            case 2: return "ND";
            case 3: return "RD";
            default: return "TH";
        }
    }

    private static int wordToNumber(final String word) {
        switch (word.toLowerCase(Locale.ROOT)) {
            case "second": return 2;
            case "third": return 3;
            case "fourth": return 4;
            case "fifth": return 5;
            case "sixth": return 6;
            case "seventh": return 7;
            case "eighth": return 8;
            case "ninth": return 9;
            case "tenth": return 10;
            case "eleventh": return 11;
            case "twelfth": return 12;
            default: return 0;
        }
    }

    private static String extractJsonString(final String json, final String field) {
        final Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
        final Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static int extractJsonInt(final String json, final String field) {
        final Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*(\\d+)");
        final Matcher m = p.matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static boolean extractJsonBoolean(final String json, final String field) {
        final Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*(true|false)");
        final Matcher m = p.matcher(json);
        return m.find() && Boolean.parseBoolean(m.group(1));
    }
}
