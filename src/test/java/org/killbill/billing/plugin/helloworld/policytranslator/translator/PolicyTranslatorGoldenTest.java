package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.MockLlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;
import org.killbill.billing.plugin.helloworld.policytranslator.model.TranslationResult;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Golden tests: given NL input, translator must produce the exact canonical JSON after normalization.
 */
@Test(groups = "fast")
public class PolicyTranslatorGoldenTest {

    private PolicyTranslator translator;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        translator = new PolicyTranslator(new MockLlmClient(), new ConceptRegistry());
    }

    @Test(groups = "fast")
    public void testGolden_25pct_everyThird_returning() throws TranslationException {
        final TranslationResult result = translator.translate(
                "Returning customers get 25% off every third purchase, no promo code.",
                "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getPolicyJson());
        Assert.assertTrue(result.getValidation().isValid(),
                "Validation errors: " + result.getValidation().getErrors());

        final PolicyJson p = result.getPolicyJson();
        Assert.assertEquals(p.getDslVersion(), "billing-intent/0.1");
        Assert.assertEquals(p.getEligibility().getCustomerSegment(), "returning");
        Assert.assertEquals(p.getCondition().getN(), 3);
        Assert.assertEquals(p.getCondition().getKind(), "every_nth_purchase");
        Assert.assertEquals(p.getCondition().getBasis(), "successful_purchases");
        Assert.assertEquals(p.getCondition().getScope(), "lifetime");
        Assert.assertEquals(p.getBenefit().getKind(), "percentage_discount");
        Assert.assertEquals(p.getBenefit().getValuePercent(), 25);
        Assert.assertEquals(p.getBenefit().getTarget(), "all_line_items");
        Assert.assertFalse(p.getBenefit().isPromoCodeRequired());
        Assert.assertEquals(p.getAppliesTo().getObject(), "purchase");
        Assert.assertEquals(p.getTrigger().getEvent(), "purchase_priced");
        Assert.assertEquals(p.getTrigger().getTiming(), "before_invoice_finalized");

        // English DSL should be generated from canonical JSON
        Assert.assertNotNull(result.getEnglishDsl());
        Assert.assertTrue(result.getEnglishDsl().contains("RETURNING"));
        Assert.assertTrue(result.getEnglishDsl().contains("3RD"));
        Assert.assertTrue(result.getEnglishDsl().contains("25%"));
    }

    @Test(groups = "fast")
    public void testGolden_10pct_every2nd_all() throws TranslationException {
        final TranslationResult result = translator.translate(
                "10% off every 2nd purchase for all customers.",
                "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.getValidation().isValid(),
                "Validation errors: " + result.getValidation().getErrors());

        final PolicyJson p = result.getPolicyJson();
        Assert.assertEquals(p.getEligibility().getCustomerSegment(), "all");
        Assert.assertEquals(p.getCondition().getN(), 2);
        Assert.assertEquals(p.getBenefit().getValuePercent(), 10);
        Assert.assertFalse(p.getBenefit().isPromoCodeRequired());
    }

    @Test(groups = "fast")
    public void testGolden_15pct_every5th_returning() throws TranslationException {
        final TranslationResult result = translator.translate(
                "Returning customers, 15 percent discount on every 5th order",
                "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.getValidation().isValid(),
                "Validation errors: " + result.getValidation().getErrors());

        final PolicyJson p = result.getPolicyJson();
        Assert.assertEquals(p.getEligibility().getCustomerSegment(), "returning");
        Assert.assertEquals(p.getCondition().getN(), 5);
        Assert.assertEquals(p.getBenefit().getValuePercent(), 15);
    }

    @Test(groups = "fast")
    public void testGolden_noPromoCode() throws TranslationException {
        final TranslationResult result = translator.translate(
                "25% off every 3rd purchase, no promo code required",
                "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.getValidation().isValid(),
                "Validation errors: " + result.getValidation().getErrors());
        Assert.assertFalse(result.getPolicyJson().getBenefit().isPromoCodeRequired());
    }

    @Test(groups = "fast")
    public void testGolden_requiresPromoCode() throws TranslationException {
        final TranslationResult result = translator.translate(
                "Returning customers get 20% off every 4th purchase, requires promo code",
                "test@example.com");

        Assert.assertNotNull(result);
        Assert.assertTrue(result.getValidation().isValid(),
                "Validation errors: " + result.getValidation().getErrors());
        Assert.assertTrue(result.getPolicyJson().getBenefit().isPromoCodeRequired());
    }

    @Test(groups = "fast")
    public void testGolden_deterministic() throws TranslationException {
        // Same input should produce same output
        final TranslationResult r1 = translator.translate(
                "Returning customers get 25% off every third purchase, no promo code.",
                "test@example.com");
        final TranslationResult r2 = translator.translate(
                "Returning customers get 25% off every third purchase, no promo code.",
                "test@example.com");

        Assert.assertEquals(r1.getPolicyJson().getPolicyId(), r2.getPolicyJson().getPolicyId());
        Assert.assertEquals(r1.getPolicyJson().getEligibility().getCustomerSegment(),
                            r2.getPolicyJson().getEligibility().getCustomerSegment());
        Assert.assertEquals(r1.getPolicyJson().getCondition().getN(), r2.getPolicyJson().getCondition().getN());
        Assert.assertEquals(r1.getPolicyJson().getBenefit().getValuePercent(),
                            r2.getPolicyJson().getBenefit().getValuePercent());
    }

    @Test(groups = "fast")
    public void testAssumptionsIncluded() throws TranslationException {
        final TranslationResult result = translator.translate(
                "Returning customers get 25% off every third purchase, no promo code.",
                "test@example.com");

        Assert.assertNotNull(result.getAssumptions());
        Assert.assertFalse(result.getAssumptions().isEmpty());
    }
}
