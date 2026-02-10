package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Trigger {

    @JsonProperty("event")
    private String event;

    @JsonProperty("timing")
    private String timing;

    public Trigger() {
    }

    public Trigger(final String event, final String timing) {
        this.event = event;
        this.timing = timing;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(final String event) {
        this.event = event;
    }

    public String getTiming() {
        return timing;
    }

    public void setTiming(final String timing) {
        this.timing = timing;
    }
}
