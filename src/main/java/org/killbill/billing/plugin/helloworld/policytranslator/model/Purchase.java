package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Purchase {

    @JsonProperty("purchase_id")
    private String purchaseId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("amount")
    private double amount;

    public Purchase() {
    }

    public Purchase(final String purchaseId, final String status, final double amount) {
        this.purchaseId = purchaseId;
        this.status = status;
        this.amount = amount;
    }

    public String getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(final String purchaseId) {
        this.purchaseId = purchaseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(final double amount) {
        this.amount = amount;
    }
}
