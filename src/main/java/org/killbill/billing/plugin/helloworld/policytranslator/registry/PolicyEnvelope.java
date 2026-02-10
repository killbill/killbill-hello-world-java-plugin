package org.killbill.billing.plugin.helloworld.policytranslator.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;

/**
 * Wrapper that stores the original NL text, the policy, status, and version together.
 */
public class PolicyEnvelope {

    @JsonProperty("policy")
    private PolicyJson policy;

    @JsonProperty("original_text")
    private String originalText;

    @JsonProperty("status")
    private PolicyStatus status;

    @JsonProperty("version")
    private int version;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("created_at")
    private String createdAt;

    public PolicyEnvelope() {
    }

    public PolicyEnvelope(final PolicyJson policy, final String originalText,
                          final PolicyStatus status, final int version,
                          final String createdBy, final String createdAt) {
        this.policy = policy;
        this.originalText = originalText;
        this.status = status;
        this.version = version;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public PolicyJson getPolicy() {
        return policy;
    }

    public void setPolicy(final PolicyJson policy) {
        this.policy = policy;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(final String originalText) {
        this.originalText = originalText;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public void setStatus(final PolicyStatus status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }
}
