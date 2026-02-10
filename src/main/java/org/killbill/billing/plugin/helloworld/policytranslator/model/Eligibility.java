package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Eligibility {

    @JsonProperty("customer_segment")
    private String customerSegment;

    public Eligibility() {
    }

    public Eligibility(final String customerSegment) {
        this.customerSegment = customerSegment;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public void setCustomerSegment(final String customerSegment) {
        this.customerSegment = customerSegment;
    }
}
