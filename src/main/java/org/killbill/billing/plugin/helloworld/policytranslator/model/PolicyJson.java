package org.killbill.billing.plugin.helloworld.policytranslator.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PolicyJson {

    @JsonProperty("dsl_version")
    private String dslVersion;

    @JsonProperty("policy_id")
    private String policyId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("applies_to")
    private AppliesTo appliesTo;

    @JsonProperty("eligibility")
    private Eligibility eligibility;

    @JsonProperty("trigger")
    private Trigger trigger;

    @JsonProperty("condition")
    private Condition condition;

    @JsonProperty("benefit")
    private Benefit benefit;

    @JsonProperty("audit")
    private Audit audit;

    @JsonProperty("metadata")
    private Metadata metadata;

    public PolicyJson() {
    }

    public String getDslVersion() {
        return dslVersion;
    }

    public void setDslVersion(final String dslVersion) {
        this.dslVersion = dslVersion;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(final String policyId) {
        this.policyId = policyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public AppliesTo getAppliesTo() {
        return appliesTo;
    }

    public void setAppliesTo(final AppliesTo appliesTo) {
        this.appliesTo = appliesTo;
    }

    public Eligibility getEligibility() {
        return eligibility;
    }

    public void setEligibility(final Eligibility eligibility) {
        this.eligibility = eligibility;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public void setTrigger(final Trigger trigger) {
        this.trigger = trigger;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    public Benefit getBenefit() {
        return benefit;
    }

    public void setBenefit(final Benefit benefit) {
        this.benefit = benefit;
    }

    public Audit getAudit() {
        return audit;
    }

    public void setAudit(final Audit audit) {
        this.audit = audit;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(final Metadata metadata) {
        this.metadata = metadata;
    }
}
