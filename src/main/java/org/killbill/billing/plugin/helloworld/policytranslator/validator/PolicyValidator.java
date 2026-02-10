package org.killbill.billing.plugin.helloworld.policytranslator.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;
import org.killbill.billing.plugin.helloworld.policytranslator.model.ValidationResult;

import java.io.InputStream;
import java.util.Set;

/**
 * Validates a PolicyJson against the JSON Schema and performs additional semantic checks
 * using the ConceptRegistry.
 */
public class PolicyValidator {

    private static final String POLICY_SCHEMA_RESOURCE = "/policytranslator/policy-schema.json";
    private static final String POLICY_ID_PATTERN = "^[a-z][a-z0-9_.]{2,80}$";
    private static final String REASON_CODE_PATTERN = "^[A-Z][A-Z0-9_]{2,50}$";

    private final ConceptRegistry registry;
    private final ObjectMapper objectMapper;
    private final JsonSchema schema;

    public PolicyValidator(final ConceptRegistry registry) {
        this.registry = registry;
        this.objectMapper = new ObjectMapper();
        this.schema = loadSchema();
    }

    private JsonSchema loadSchema() {
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        try (final InputStream is = getClass().getResourceAsStream(POLICY_SCHEMA_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Cannot find policy-schema.json on classpath");
            }
            return factory.getSchema(is);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to load policy JSON Schema", e);
        }
    }

    /**
     * Validate a PolicyJson against the schema and semantic rules.
     *
     * @param policy the policy to validate
     * @return validation result with errors and warnings
     */
    public ValidationResult validate(final PolicyJson policy) {
        final ValidationResult result = new ValidationResult();

        if (policy == null) {
            result.addError("Policy is null");
            return result;
        }

        // JSON Schema validation
        try {
            final String json = objectMapper.writeValueAsString(policy);
            final JsonNode node = objectMapper.readTree(json);
            final Set<ValidationMessage> schemaErrors = schema.validate(node);
            for (final ValidationMessage msg : schemaErrors) {
                result.addError("Schema: " + msg.getMessage());
            }
        } catch (final Exception e) {
            result.addError("Failed to validate against schema: " + e.getMessage());
            return result;
        }

        // Semantic validations (even if schema passed, double-check business rules)
        validateSemantics(policy, result);

        return result;
    }

    private void validateSemantics(final PolicyJson policy, final ValidationResult result) {
        // applies_to.object
        if (policy.getAppliesTo() != null && policy.getAppliesTo().getObject() != null) {
            if (!registry.isValidAppliesToObject(policy.getAppliesTo().getObject())) {
                result.addError("applies_to.object '" + policy.getAppliesTo().getObject() +
                                "' is not supported. Allowed: " + registry.getAppliesToObjects());
            }
        }

        // eligibility.customer_segment
        if (policy.getEligibility() != null && policy.getEligibility().getCustomerSegment() != null) {
            if (!registry.isValidCustomerSegment(policy.getEligibility().getCustomerSegment())) {
                result.addError("eligibility.customer_segment '" + policy.getEligibility().getCustomerSegment() +
                                "' is not supported. Allowed: " + registry.getCustomerSegments());
            }
        }

        // trigger.event
        if (policy.getTrigger() != null && policy.getTrigger().getEvent() != null) {
            if (!registry.isValidEvent(policy.getTrigger().getEvent())) {
                result.addError("trigger.event '" + policy.getTrigger().getEvent() +
                                "' is not supported. Allowed: " + registry.getEvents());
            }
        }

        // trigger.timing
        if (policy.getTrigger() != null && policy.getTrigger().getTiming() != null) {
            if (!registry.isValidTiming(policy.getTrigger().getTiming())) {
                result.addError("trigger.timing '" + policy.getTrigger().getTiming() +
                                "' is not supported. Allowed: " + registry.getTimings());
            }
        }

        // condition.kind
        if (policy.getCondition() != null && policy.getCondition().getKind() != null) {
            if (!registry.isValidConditionKind(policy.getCondition().getKind())) {
                result.addError("condition.kind '" + policy.getCondition().getKind() +
                                "' is not supported. Allowed condition kinds: " + "every_nth_purchase");
            }
        }

        // condition.n bounds
        if (policy.getCondition() != null) {
            final int n = policy.getCondition().getN();
            if (n < registry.getConditionNMin() || n > registry.getConditionNMax()) {
                result.addError("condition.n = " + n + " is out of range [" +
                                registry.getConditionNMin() + ", " + registry.getConditionNMax() + "]");
            }
        }

        // condition.basis
        if (policy.getCondition() != null && policy.getCondition().getBasis() != null) {
            if (!registry.getConditionBasis().contains(policy.getCondition().getBasis())) {
                result.addError("condition.basis '" + policy.getCondition().getBasis() +
                                "' is not supported. Allowed: " + registry.getConditionBasis());
            }
        }

        // condition.scope
        if (policy.getCondition() != null && policy.getCondition().getScope() != null) {
            if (!registry.getConditionScopes().contains(policy.getCondition().getScope())) {
                result.addError("condition.scope '" + policy.getCondition().getScope() +
                                "' is not supported. Allowed: " + registry.getConditionScopes());
            }
        }

        // benefit.kind
        if (policy.getBenefit() != null && policy.getBenefit().getKind() != null) {
            if (!registry.isValidBenefitKind(policy.getBenefit().getKind())) {
                result.addError("benefit.kind '" + policy.getBenefit().getKind() +
                                "' is not supported");
            }
        }

        // benefit.value_percent bounds
        if (policy.getBenefit() != null) {
            final int pct = policy.getBenefit().getValuePercent();
            if (pct < registry.getBenefitMinPercent() || pct > registry.getBenefitMaxPercent()) {
                result.addError("benefit.value_percent = " + pct + " is out of range [" +
                                registry.getBenefitMinPercent() + ", " + registry.getBenefitMaxPercent() + "]");
            }
        }

        // benefit.target
        if (policy.getBenefit() != null && policy.getBenefit().getTarget() != null) {
            if (!registry.isValidTarget(policy.getBenefit().getTarget())) {
                result.addError("benefit.target '" + policy.getBenefit().getTarget() +
                                "' is not supported. Allowed: " + registry.getTargets());
            }
        }

        // audit.reason_code format
        if (policy.getAudit() != null && policy.getAudit().getReasonCode() != null) {
            if (!policy.getAudit().getReasonCode().matches(REASON_CODE_PATTERN)) {
                result.addError("audit.reason_code '" + policy.getAudit().getReasonCode() +
                                "' does not match pattern " + REASON_CODE_PATTERN);
            }
        }

        // policy_id format
        if (policy.getPolicyId() != null) {
            if (!policy.getPolicyId().matches(POLICY_ID_PATTERN)) {
                result.addError("policy_id '" + policy.getPolicyId() +
                                "' does not match pattern " + POLICY_ID_PATTERN);
            }
        }

        // dsl_version
        if (policy.getDslVersion() != null && !"billing-intent/0.1".equals(policy.getDslVersion())) {
            result.addError("dsl_version must be 'billing-intent/0.1', got '" + policy.getDslVersion() + "'");
        }
    }
}
