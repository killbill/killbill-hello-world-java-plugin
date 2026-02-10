package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Condition {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("n")
    private int n;

    @JsonProperty("basis")
    private String basis;

    @JsonProperty("scope")
    private String scope;

    public Condition() {
    }

    public Condition(final String kind, final int n, final String basis, final String scope) {
        this.kind = kind;
        this.n = n;
        this.basis = basis;
        this.scope = scope;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(final String kind) {
        this.kind = kind;
    }

    public int getN() {
        return n;
    }

    public void setN(final int n) {
        this.n = n;
    }

    public String getBasis() {
        return basis;
    }

    public void setBasis(final String basis) {
        this.basis = basis;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }
}
