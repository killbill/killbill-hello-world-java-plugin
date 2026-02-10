package org.killbill.billing.plugin.helloworld.policytranslator.normalizer;

import org.killbill.billing.plugin.helloworld.policytranslator.model.Audit;
import org.killbill.billing.plugin.helloworld.policytranslator.model.Metadata;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Deterministic normalizer that fills in defaults, generates identifiers, and coerces values.
 */
public class PolicyNormalizer {

    private static final String DSL_VERSION = "billing-intent/0.1";
    private static final String SOURCE = "nl_translation_poc";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Normalize a PolicyJson in place (returns the same object with fields filled in).
     *
     * @param policy    the policy to normalize
     * @param createdBy the user who requested the translation
     * @return the normalized policy
     */
    public PolicyJson normalize(final PolicyJson policy, final String createdBy) {
        // dsl_version
        policy.setDslVersion(DSL_VERSION);

        // Trim strings
        if (policy.getDescription() != null) {
            policy.setDescription(policy.getDescription().trim());
        }

        // Generate policy_id from fields if missing or malformed
        if (policy.getPolicyId() == null || policy.getPolicyId().trim().isEmpty()) {
            policy.setPolicyId(generatePolicyId(policy));
        }

        // Generate description if missing
        if (policy.getDescription() == null || policy.getDescription().trim().isEmpty()) {
            policy.setDescription(generateDescription(policy));
        }

        // Generate audit fields if missing
        if (policy.getAudit() == null) {
            policy.setAudit(new Audit());
        }
        if (policy.getAudit().getReasonCode() == null || policy.getAudit().getReasonCode().trim().isEmpty()) {
            policy.getAudit().setReasonCode(generateReasonCode(policy));
        }
        if (policy.getAudit().getLabel() == null || policy.getAudit().getLabel().trim().isEmpty()) {
            policy.getAudit().setLabel("Loyalty reward");
        }

        // Set metadata
        if (policy.getMetadata() == null) {
            policy.setMetadata(new Metadata());
        }
        if (createdBy != null && !createdBy.trim().isEmpty()) {
            policy.getMetadata().setCreatedBy(createdBy);
        } else if (policy.getMetadata().getCreatedBy() == null || policy.getMetadata().getCreatedBy().trim().isEmpty()) {
            policy.getMetadata().setCreatedBy("system");
        }
        policy.getMetadata().setCreatedAt(Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER));
        policy.getMetadata().setSource(SOURCE);

        return policy;
    }

    /**
     * Generate a policy_id from fields: {segment}.every_{ordinal}_purchase_{percent}pct
     */
    public String generatePolicyId(final PolicyJson policy) {
        final String segment = policy.getEligibility() != null ? policy.getEligibility().getCustomerSegment() : "all";
        final int n = policy.getCondition() != null ? policy.getCondition().getN() : 0;
        final int percent = policy.getBenefit() != null ? policy.getBenefit().getValuePercent() : 0;

        final String ordinal = toOrdinal(n);
        return slugify(segment + ".every_" + ordinal + "_purchase_" + percent + "pct");
    }

    /**
     * Generate a human-readable description.
     */
    public String generateDescription(final PolicyJson policy) {
        final String segment = policy.getEligibility() != null ? policy.getEligibility().getCustomerSegment() : "all";
        final int n = policy.getCondition() != null ? policy.getCondition().getN() : 0;
        final int percent = policy.getBenefit() != null ? policy.getBenefit().getValuePercent() : 0;
        final boolean promoRequired = policy.getBenefit() != null && policy.getBenefit().isPromoCodeRequired();

        final String ordinal = toOrdinal(n);
        final String promoText = promoRequired ? "with a promo code" : "without a promo code";
        return percent + "% off every " + ordinal + " purchase for " + segment + " customers " + promoText + ".";
    }

    /**
     * Generate a reason code: LOYALTY_EVERY_{N}{SUFFIX}
     */
    public String generateReasonCode(final PolicyJson policy) {
        final int n = policy.getCondition() != null ? policy.getCondition().getN() : 0;
        return "LOYALTY_EVERY_" + n + toOrdinalSuffixUpper(n);
    }

    private static String toOrdinal(final int n) {
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

    private static String slugify(final String input) {
        return input.toLowerCase()
                    .replaceAll("[^a-z0-9._]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
    }
}
