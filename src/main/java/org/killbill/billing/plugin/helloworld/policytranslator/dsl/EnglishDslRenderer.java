package org.killbill.billing.plugin.helloworld.policytranslator.dsl;

import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;

/**
 * Renders a validated PolicyJson into a human-readable English-like DSL view.
 * This is a template-based rendering, not LLM-generated.
 */
public class EnglishDslRenderer {

    /**
     * Render a policy as English-like DSL.
     *
     * @param policy the validated canonical policy
     * @return formatted English DSL string
     */
    public String render(final PolicyJson policy) {
        if (policy == null) {
            return "POLICY <null>";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("POLICY \"").append(safe(policy.getPolicyId())).append("\"\n");

        // Customer segment
        if (policy.getEligibility() != null) {
            sb.append("  FOR ").append(upper(policy.getEligibility().getCustomerSegment())).append(" CUSTOMERS\n");
        }

        // Condition
        if (policy.getCondition() != null) {
            sb.append("  ON EVERY ").append(toOrdinal(policy.getCondition().getN()).toUpperCase())
              .append(" SUCCESSFUL PURCHASE");
            if (policy.getCondition().getScope() != null) {
                sb.append(" (").append(policy.getCondition().getScope().toUpperCase()).append(")");
            }
            sb.append("\n");
        }

        // Benefit
        if (policy.getBenefit() != null) {
            sb.append("  APPLY ").append(policy.getBenefit().getValuePercent())
              .append("% DISCOUNT TO ").append(upper(policy.getBenefit().getTarget())).append("\n");
            sb.append("  PROMO CODE: ")
              .append(policy.getBenefit().isPromoCodeRequired() ? "REQUIRED" : "NOT REQUIRED")
              .append("\n");
        }

        // Audit
        if (policy.getAudit() != null) {
            sb.append("  REASON: ").append(safe(policy.getAudit().getReasonCode())).append("\n");
        }

        return sb.toString();
    }

    private static String safe(final String s) {
        return s != null ? s : "<missing>";
    }

    private static String upper(final String s) {
        return s != null ? s.toUpperCase().replace('_', ' ') : "<MISSING>";
    }

    private static String toOrdinal(final int n) {
        switch (n) {
            case 2: return "2nd";
            case 3: return "3rd";
            default: return n + "th";
        }
    }
}
