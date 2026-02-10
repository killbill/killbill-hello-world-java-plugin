package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class IntentExtraction {

    @JsonProperty("customer_segment")
    private String customerSegment;

    @JsonProperty("discount_percent")
    private int discountPercent;

    @JsonProperty("cadence_every_nth_purchase")
    private int cadenceEveryNthPurchase;

    @JsonProperty("promo_code_required")
    private boolean promoCodeRequired;

    @JsonProperty("trigger_event")
    private String triggerEvent;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("notes")
    private List<String> notes = new ArrayList<>();

    @JsonProperty("missing_info_questions")
    private List<String> missingInfoQuestions = new ArrayList<>();

    public IntentExtraction() {
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(final String customerSegment) {
        this.customerSegment = customerSegment;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(final int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getCadenceEveryNthPurchase() {
        return cadenceEveryNthPurchase;
    }

    public void setCadenceEveryNthPurchase(final int cadenceEveryNthPurchase) {
        this.cadenceEveryNthPurchase = cadenceEveryNthPurchase;
    }

    public boolean isPromoCodeRequired() {
        return promoCodeRequired;
    }

    public void setPromoCodeRequired(final boolean promoCodeRequired) {
        this.promoCodeRequired = promoCodeRequired;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(final String triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(final List<String> notes) {
        this.notes = notes;
    }

    public List<String> getMissingInfoQuestions() {
        return missingInfoQuestions;
    }

    public void setMissingInfoQuestions(final List<String> missingInfoQuestions) {
        this.missingInfoQuestions = missingInfoQuestions;
    }
}
