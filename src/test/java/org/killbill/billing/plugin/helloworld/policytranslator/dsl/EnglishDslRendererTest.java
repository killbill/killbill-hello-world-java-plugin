package org.killbill.billing.plugin.helloworld.policytranslator.dsl;

import org.killbill.billing.plugin.helloworld.policytranslator.model.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "fast")
public class EnglishDslRendererTest {

    private EnglishDslRenderer renderer;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        renderer = new EnglishDslRenderer();
    }

    @Test(groups = "fast")
    public void testRender_ReturningCustomer25Pct() {
        final PolicyJson policy = buildPolicy("returning.every_3rd_purchase_25pct", "returning", 3, 25, false, "LOYALTY_EVERY_3RD");
        final String dsl = renderer.render(policy);

        Assert.assertTrue(dsl.contains("POLICY \"returning.every_3rd_purchase_25pct\""));
        Assert.assertTrue(dsl.contains("FOR RETURNING CUSTOMERS"));
        Assert.assertTrue(dsl.contains("ON EVERY 3RD SUCCESSFUL PURCHASE (LIFETIME)"));
        Assert.assertTrue(dsl.contains("APPLY 25% DISCOUNT TO ALL LINE ITEMS"));
        Assert.assertTrue(dsl.contains("PROMO CODE: NOT REQUIRED"));
        Assert.assertTrue(dsl.contains("REASON: LOYALTY_EVERY_3RD"));
    }

    @Test(groups = "fast")
    public void testRender_AllCustomer10Pct_2nd() {
        final PolicyJson policy = buildPolicy("all.every_2nd_purchase_10pct", "all", 2, 10, false, "LOYALTY_EVERY_2ND");
        final String dsl = renderer.render(policy);

        Assert.assertTrue(dsl.contains("FOR ALL CUSTOMERS"));
        Assert.assertTrue(dsl.contains("ON EVERY 2ND SUCCESSFUL PURCHASE"));
        Assert.assertTrue(dsl.contains("APPLY 10% DISCOUNT"));
    }

    @Test(groups = "fast")
    public void testRender_PromoRequired() {
        final PolicyJson policy = buildPolicy("new.every_5th_purchase_15pct", "new", 5, 15, true, "LOYALTY_EVERY_5TH");
        final String dsl = renderer.render(policy);

        Assert.assertTrue(dsl.contains("FOR NEW CUSTOMERS"));
        Assert.assertTrue(dsl.contains("ON EVERY 5TH SUCCESSFUL PURCHASE"));
        Assert.assertTrue(dsl.contains("PROMO CODE: REQUIRED"));
    }

    @Test(groups = "fast")
    public void testRender_NullPolicy() {
        final String dsl = renderer.render(null);
        Assert.assertEquals(dsl, "POLICY <null>");
    }

    @Test(groups = "fast")
    public void testRender_Consistency() {
        // Render same policy twice, should be identical
        final PolicyJson policy = buildPolicy("returning.every_3rd_purchase_25pct", "returning", 3, 25, false, "LOYALTY_EVERY_3RD");
        final String dsl1 = renderer.render(policy);
        final String dsl2 = renderer.render(policy);
        Assert.assertEquals(dsl1, dsl2);
    }

    private PolicyJson buildPolicy(final String policyId, final String segment, final int n,
                                    final int percent, final boolean promoRequired, final String reasonCode) {
        final PolicyJson policy = new PolicyJson();
        policy.setPolicyId(policyId);
        policy.setEligibility(new Eligibility(segment));
        policy.setCondition(new Condition("every_nth_purchase", n, "successful_purchases", "lifetime"));
        policy.setBenefit(new Benefit("percentage_discount", percent, "all_line_items", promoRequired));
        policy.setAudit(new Audit(reasonCode, "Loyalty reward"));
        return policy;
    }
}
