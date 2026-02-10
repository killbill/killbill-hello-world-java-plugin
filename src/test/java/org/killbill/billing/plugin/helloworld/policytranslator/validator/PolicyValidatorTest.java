package org.killbill.billing.plugin.helloworld.policytranslator.validator;

import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.model.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "fast")
public class PolicyValidatorTest {

    private PolicyValidator validator;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        validator = new PolicyValidator(new ConceptRegistry());
    }

    @Test(groups = "fast")
    public void testValidPolicy() {
        final PolicyJson policy = buildValidPolicy();
        final ValidationResult result = validator.validate(policy);
        Assert.assertTrue(result.isValid(), "Expected valid but got errors: " + result.getErrors());
    }

    @Test(groups = "fast")
    public void testNullPolicy() {
        final ValidationResult result = validator.validate(null);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("null")));
    }

    @Test(groups = "fast")
    public void testInvalidDiscountPercent_TooHigh() {
        final PolicyJson policy = buildValidPolicy();
        policy.getBenefit().setValuePercent(95);
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("value_percent")));
    }

    @Test(groups = "fast")
    public void testInvalidDiscountPercent_TooLow() {
        final PolicyJson policy = buildValidPolicy();
        policy.getBenefit().setValuePercent(0);
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
    }

    @Test(groups = "fast")
    public void testInvalidConditionN_TooHigh() {
        final PolicyJson policy = buildValidPolicy();
        policy.getCondition().setN(15);
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("condition.n")));
    }

    @Test(groups = "fast")
    public void testInvalidConditionN_TooLow() {
        final PolicyJson policy = buildValidPolicy();
        policy.getCondition().setN(1);
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
    }

    @Test(groups = "fast")
    public void testInvalidCustomerSegment() {
        final PolicyJson policy = buildValidPolicy();
        policy.getEligibility().setCustomerSegment("vip");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("customer_segment")));
    }

    @Test(groups = "fast")
    public void testInvalidAppliesToObject() {
        final PolicyJson policy = buildValidPolicy();
        policy.getAppliesTo().setObject("invoice");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("applies_to")));
    }

    @Test(groups = "fast")
    public void testInvalidTriggerEvent() {
        final PolicyJson policy = buildValidPolicy();
        policy.getTrigger().setEvent("invoice_created");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
    }

    @Test(groups = "fast")
    public void testInvalidReasonCodeFormat() {
        final PolicyJson policy = buildValidPolicy();
        policy.getAudit().setReasonCode("bad-code!");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("reason_code")));
    }

    @Test(groups = "fast")
    public void testInvalidPolicyIdFormat() {
        final PolicyJson policy = buildValidPolicy();
        policy.setPolicyId("INVALID ID!");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
        Assert.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("policy_id")));
    }

    @Test(groups = "fast")
    public void testInvalidDslVersion() {
        final PolicyJson policy = buildValidPolicy();
        policy.setDslVersion("billing-intent/9.9");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
    }

    @Test(groups = "fast")
    public void testInvalidConditionKind() {
        final PolicyJson policy = buildValidPolicy();
        policy.getCondition().setKind("every_nth_month");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
    }

    @Test(groups = "fast")
    public void testInvalidBenefitKind() {
        final PolicyJson policy = buildValidPolicy();
        policy.getBenefit().setKind("flat_discount");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
    }

    @Test(groups = "fast")
    public void testInvalidConditionScope() {
        final PolicyJson policy = buildValidPolicy();
        policy.getCondition().setScope("last_30_days");
        final ValidationResult result = validator.validate(policy);
        Assert.assertFalse(result.isValid());
    }

    @Test(groups = "fast")
    public void testAllValidSegments() {
        for (final String seg : new String[]{"returning", "new", "all"}) {
            final PolicyJson policy = buildValidPolicy();
            policy.getEligibility().setCustomerSegment(seg);
            final ValidationResult result = validator.validate(policy);
            Assert.assertTrue(result.isValid(), "Segment '" + seg + "' should be valid but got: " + result.getErrors());
        }
    }

    @Test(groups = "fast")
    public void testBoundaryDiscountPercent() {
        // Min boundary
        final PolicyJson policy1 = buildValidPolicy();
        policy1.getBenefit().setValuePercent(1);
        Assert.assertTrue(validator.validate(policy1).isValid());

        // Max boundary
        final PolicyJson policy2 = buildValidPolicy();
        policy2.getBenefit().setValuePercent(90);
        Assert.assertTrue(validator.validate(policy2).isValid());
    }

    @Test(groups = "fast")
    public void testBoundaryConditionN() {
        // Min boundary
        final PolicyJson policy1 = buildValidPolicy();
        policy1.getCondition().setN(2);
        Assert.assertTrue(validator.validate(policy1).isValid());

        // Max boundary
        final PolicyJson policy2 = buildValidPolicy();
        policy2.getCondition().setN(12);
        Assert.assertTrue(validator.validate(policy2).isValid());
    }

    private PolicyJson buildValidPolicy() {
        final PolicyJson policy = new PolicyJson();
        policy.setDslVersion("billing-intent/0.1");
        policy.setPolicyId("returning.every_3rd_purchase_25pct");
        policy.setDescription("25% off every 3rd purchase for returning customers.");
        policy.setAppliesTo(new AppliesTo("purchase"));
        policy.setEligibility(new Eligibility("returning"));
        policy.setTrigger(new Trigger("purchase_priced", "before_invoice_finalized"));
        policy.setCondition(new Condition("every_nth_purchase", 3, "successful_purchases", "lifetime"));
        policy.setBenefit(new Benefit("percentage_discount", 25, "all_line_items", false));
        policy.setAudit(new Audit("LOYALTY_EVERY_3RD", "Loyalty reward"));
        policy.setMetadata(new Metadata("test@example.com", "2026-02-10T00:00:00Z", "nl_translation_poc"));
        return policy;
    }
}
