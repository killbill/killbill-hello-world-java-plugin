package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Benefit {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("value_percent")
    private int valuePercent;

    @JsonProperty("target")
    private String target;

    @JsonProperty("promo_code_required")
    private boolean promoCodeRequired;

    public Benefit() {
    }

    public Benefit(final String kind, final int valuePercent, final String target, final boolean promoCodeRequired) {
        this.kind = kind;
        this.valuePercent = valuePercent;
        this.target = target;
        this.promoCodeRequired = promoCodeRequired;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(final String kind) {
        this.kind = kind;
    }

    public int getValuePercent() {
        return valuePercent;
    }

    public void setValuePercent(final int valuePercent) {
        this.valuePercent = valuePercent;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    public boolean isPromoCodeRequired() {
        return promoCodeRequired;
    }

    public void setPromoCodeRequired(final boolean promoCodeRequired) {
        this.promoCodeRequired = promoCodeRequired;
    }
}
