package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AppliesTo {

    @JsonProperty("object")
    private String object;

    public AppliesTo() {
    }

    public AppliesTo(final String object) {
        this.object = object;
    }

    public String getObject() {
        return object;
    }

    public void setObject(final String object) {
        this.object = object;
    }
}
