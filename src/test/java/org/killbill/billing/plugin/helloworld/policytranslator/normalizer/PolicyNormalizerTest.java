package org.killbill.billing.plugin.helloworld.policytranslator.normalizer;

import org.killbill.billing.plugin.helloworld.policytranslator.model.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "fast")
public class PolicyNormalizerTest {

    private PolicyNormalizer normalizer;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        normalizer = new PolicyNormalizer();
    }

    @Test(groups = "fast")
    public void testGeneratePolicyId_3rd() {
        final PolicyJson policy = buildMinimalPolicy("returning", 3, 25);
        final String id = normalizer.generatePolicyId(policy);
        Assert.assertEquals(id, "returning.every_3rd_purchase_25pct");
    }

    @Test(groups = "fast")
    public void testGeneratePolicyId_2nd() {
        final PolicyJson policy = buildMinimalPolicy("all", 2, 10);
        final String id = normalizer.generatePolicyId(policy);
        Assert.assertEquals(id, "all.every_2nd_purchase_10pct");
    }

    @Test(groups = "fast")
    public void testGeneratePolicyId_5th() {
        final PolicyJson policy = buildMinimalPolicy("new", 5, 15);
        final String id = normalizer.generatePolicyId(policy);
        Assert.assertEquals(id, "new.every_5th_purchase_15pct");
    }

    @Test(groups = "fast")
    public void testGenerateDescription() {
        final PolicyJson policy = buildMinimalPolicy("returning", 3, 25);
        policy.getBenefit().setPromoCodeRequired(false);
        final String desc = normalizer.generateDescription(policy);
        Assert.assertTrue(desc.contains("25%"));
        Assert.assertTrue(desc.contains("3rd"));
        Assert.assertTrue(desc.contains("returning"));
        Assert.assertTrue(desc.contains("without a promo code"));
    }

    @Test(groups = "fast")
    public void testGenerateDescription_PromoRequired() {
        final PolicyJson policy = buildMinimalPolicy("all", 2, 10);
        policy.getBenefit().setPromoCodeRequired(true);
        final String desc = normalizer.generateDescription(policy);
        Assert.assertTrue(desc.contains("with a promo code"));
    }

    @Test(groups = "fast")
    public void testGenerateReasonCode_3rd() {
        final PolicyJson policy = buildMinimalPolicy("returning", 3, 25);
        final String code = normalizer.generateReasonCode(policy);
        Assert.assertEquals(code, "LOYALTY_EVERY_3RD");
    }

    @Test(groups = "fast")
    public void testGenerateReasonCode_2nd() {
        final PolicyJson policy = buildMinimalPolicy("all", 2, 10);
        final String code = normalizer.generateReasonCode(policy);
        Assert.assertEquals(code, "LOYALTY_EVERY_2ND");
    }

    @Test(groups = "fast")
    public void testGenerateReasonCode_5th() {
        final PolicyJson policy = buildMinimalPolicy("new", 5, 15);
        final String code = normalizer.generateReasonCode(policy);
        Assert.assertEquals(code, "LOYALTY_EVERY_5TH");
    }

    @Test(groups = "fast")
    public void testNormalize_FillsDefaults() {
        final PolicyJson policy = buildMinimalPolicy("returning", 3, 25);
        // Leave audit and metadata empty
        policy.setAudit(null);
        policy.setMetadata(null);
        policy.setPolicyId(null);
        policy.setDescription(null);

        final PolicyJson normalized = normalizer.normalize(policy, "test@example.com");

        Assert.assertEquals(normalized.getDslVersion(), "billing-intent/0.1");
        Assert.assertNotNull(normalized.getPolicyId());
        Assert.assertEquals(normalized.getPolicyId(), "returning.every_3rd_purchase_25pct");
        Assert.assertNotNull(normalized.getDescription());
        Assert.assertNotNull(normalized.getAudit());
        Assert.assertEquals(normalized.getAudit().getReasonCode(), "LOYALTY_EVERY_3RD");
        Assert.assertEquals(normalized.getAudit().getLabel(), "Loyalty reward");
        Assert.assertNotNull(normalized.getMetadata());
        Assert.assertEquals(normalized.getMetadata().getCreatedBy(), "test@example.com");
        Assert.assertNotNull(normalized.getMetadata().getCreatedAt());
        Assert.assertEquals(normalized.getMetadata().getSource(), "nl_translation_poc");
    }

    @Test(groups = "fast")
    public void testNormalize_TrimsStrings() {
        final PolicyJson policy = buildMinimalPolicy("returning", 3, 25);
        policy.setDescription("  some description with spaces  ");
        final PolicyJson normalized = normalizer.normalize(policy, "user");
        Assert.assertEquals(normalized.getDescription(), "some description with spaces");
    }

    @Test(groups = "fast")
    public void testNormalize_PreservesExistingValidFields() {
        final PolicyJson policy = buildMinimalPolicy("returning", 3, 25);
        policy.setPolicyId("returning.every_3rd_purchase_25pct");
        policy.setDescription("Existing description");
        policy.setAudit(new Audit("CUSTOM_CODE", "Custom label"));

        final PolicyJson normalized = normalizer.normalize(policy, "user");

        // Should preserve existing valid fields
        Assert.assertEquals(normalized.getPolicyId(), "returning.every_3rd_purchase_25pct");
        Assert.assertEquals(normalized.getDescription(), "Existing description");
        Assert.assertEquals(normalized.getAudit().getReasonCode(), "CUSTOM_CODE");
        Assert.assertEquals(normalized.getAudit().getLabel(), "Custom label");
    }

    private PolicyJson buildMinimalPolicy(final String segment, final int n, final int percent) {
        final PolicyJson policy = new PolicyJson();
        policy.setEligibility(new Eligibility(segment));
        policy.setCondition(new Condition("every_nth_purchase", n, "successful_purchases", "lifetime"));
        policy.setBenefit(new Benefit("percentage_discount", percent, "all_line_items", false));
        policy.setAppliesTo(new AppliesTo("purchase"));
        policy.setTrigger(new Trigger("purchase_priced", "before_invoice_finalized"));
        return policy;
    }
}
