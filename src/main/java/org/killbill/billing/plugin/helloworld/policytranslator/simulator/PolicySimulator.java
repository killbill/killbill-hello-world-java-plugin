package org.killbill.billing.plugin.helloworld.policytranslator.simulator;

import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;
import org.killbill.billing.plugin.helloworld.policytranslator.model.Purchase;
import org.killbill.billing.plugin.helloworld.policytranslator.model.SimulationResult;

import java.util.List;

/**
 * Minimal simulator that applies a policy to a list of purchases and determines
 * whether the discount triggers on the last purchase in the list.
 */
public class PolicySimulator {

    /**
     * Simulate the policy against the given purchase history.
     * The last purchase in the list is treated as the "current" purchase.
     * All prior purchases are the purchase history.
     *
     * @param policy    the validated policy
     * @param purchases list of purchases (historical + current). The last one is current.
     * @return simulation result
     */
    public SimulationResult simulate(final PolicyJson policy, final List<Purchase> purchases) {
        if (policy == null) {
            return new SimulationResult(false, 0.0, "No policy provided.");
        }
        if (purchases == null || purchases.isEmpty()) {
            return new SimulationResult(false, 0.0, "No purchases provided.");
        }
        if (policy.getCondition() == null || !"every_nth_purchase".equals(policy.getCondition().getKind())) {
            return new SimulationResult(false, 0.0,
                    "Unsupported condition kind: " + (policy.getCondition() != null ? policy.getCondition().getKind() : "null"));
        }

        final int n = policy.getCondition().getN();
        final int percent = policy.getBenefit() != null ? policy.getBenefit().getValuePercent() : 0;

        // Count prior successful purchases (all except the last one)
        int priorSuccessful = 0;
        for (int i = 0; i < purchases.size() - 1; i++) {
            if ("success".equalsIgnoreCase(purchases.get(i).getStatus())) {
                priorSuccessful++;
            }
        }

        // Current purchase is the last one
        final Purchase current = purchases.get(purchases.size() - 1);
        final boolean currentIsSuccess = "success".equalsIgnoreCase(current.getStatus());

        // If the current purchase is successful, its ordinal is priorSuccessful + 1
        final int ordinal = currentIsSuccess ? priorSuccessful + 1 : -1;
        final boolean applies = currentIsSuccess && ordinal > 0 && (ordinal % n == 0);

        final double discountAmount;
        final String explanation;

        if (applies) {
            discountAmount = current.getAmount() * percent / 100.0;
            explanation = String.format(
                    "Purchase #%d (id=%s, amount=%.2f) is the %s successful purchase. " +
                    "Discount of %d%% applied: -%.2f",
                    purchases.size(), current.getPurchaseId(), current.getAmount(),
                    toOrdinal(ordinal), percent, discountAmount);
        } else if (!currentIsSuccess) {
            discountAmount = 0.0;
            explanation = String.format(
                    "Purchase #%d (id=%s) has status '%s' (not successful). No discount.",
                    purchases.size(), current.getPurchaseId(), current.getStatus());
        } else {
            discountAmount = 0.0;
            explanation = String.format(
                    "Purchase #%d (id=%s) is the %s successful purchase. " +
                    "Discount triggers on every %s purchase. No discount applied.",
                    purchases.size(), current.getPurchaseId(),
                    toOrdinal(ordinal), toOrdinal(n));
        }

        return new SimulationResult(applies, discountAmount, explanation);
    }

    private static String toOrdinal(final int n) {
        switch (n) {
            case 1: return "1st";
            case 2: return "2nd";
            case 3: return "3rd";
            default: return n + "th";
        }
    }
}
