package org.killbill.billing.plugin.helloworld.policytranslator.model;

public class SimulationResult {

    private final boolean discountApplied;
    private final double discountAmount;
    private final String explanation;

    public SimulationResult(final boolean discountApplied, final double discountAmount, final String explanation) {
        this.discountApplied = discountApplied;
        this.discountAmount = discountAmount;
        this.explanation = explanation;
    }

    public boolean isDiscountApplied() {
        return discountApplied;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public String getExplanation() {
        return explanation;
    }
}
