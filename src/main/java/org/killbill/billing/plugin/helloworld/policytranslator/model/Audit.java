package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Audit {

    @JsonProperty("reason_code")
    private String reasonCode;

    @JsonProperty("label")
    private String label;

    public Audit() {
    }

    public Audit(final String reasonCode, final String label) {
        this.reasonCode = reasonCode;
        this.label = label;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(final String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }
}
