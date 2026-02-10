package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.MockLlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.model.IntentExtraction;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;
import org.killbill.billing.plugin.helloworld.policytranslator.validator.PolicyValidator;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Stage B policy mapping with the mock LLM client.
 */
@Test(groups = "fast")
public class PolicyMapperTest {

    private PolicyMapper mapper;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        final MockLlmClient mockClient = new MockLlmClient();
        final ConceptRegistry registry = new ConceptRegistry();
        final PromptBuilder promptBuilder = new PromptBuilder(registry);
        final PolicyValidator validator = new PolicyValidator(registry);
        mapper = new PolicyMapper(mockClient, promptBuilder, validator);
    }

    @Test(groups = "fast")
    public void testMap_Returning3rd25pct() throws TranslationException {
        final IntentExtraction intent = buildIntent("returning", 25, 3, false);
        final PolicyJson policy = mapper.map(intent);

        Assert.assertNotNull(policy);
        Assert.assertEquals(policy.getEligibility().getCustomerSegment(), "returning");
        Assert.assertEquals(policy.getCondition().getN(), 3);
        Assert.assertEquals(policy.getBenefit().getValuePercent(), 25);
        Assert.assertFalse(policy.getBenefit().isPromoCodeRequired());
        Assert.assertEquals(policy.getAppliesTo().getObject(), "purchase");
        Assert.assertEquals(policy.getTrigger().getEvent(), "purchase_priced");
        Assert.assertEquals(policy.getTrigger().getTiming(), "before_invoice_finalized");
    }

    @Test(groups = "fast")
    public void testMap_All2nd10pct() throws TranslationException {
        final IntentExtraction intent = buildIntent("all", 10, 2, false);
        final PolicyJson policy = mapper.map(intent);

        Assert.assertNotNull(policy);
        Assert.assertEquals(policy.getEligibility().getCustomerSegment(), "all");
        Assert.assertEquals(policy.getCondition().getN(), 2);
        Assert.assertEquals(policy.getBenefit().getValuePercent(), 10);
    }

    @Test(groups = "fast")
    public void testMap_PromoRequired() throws TranslationException {
        final IntentExtraction intent = buildIntent("new", 15, 5, true);
        final PolicyJson policy = mapper.map(intent);

        Assert.assertNotNull(policy);
        Assert.assertTrue(policy.getBenefit().isPromoCodeRequired());
    }

    @Test(groups = "fast")
    public void testMap_OutputHasRequiredFields() throws TranslationException {
        final IntentExtraction intent = buildIntent("returning", 25, 3, false);
        final PolicyJson policy = mapper.map(intent);

        Assert.assertNotNull(policy.getDslVersion());
        Assert.assertNotNull(policy.getPolicyId());
        Assert.assertNotNull(policy.getDescription());
        Assert.assertNotNull(policy.getAppliesTo());
        Assert.assertNotNull(policy.getEligibility());
        Assert.assertNotNull(policy.getTrigger());
        Assert.assertNotNull(policy.getCondition());
        Assert.assertNotNull(policy.getBenefit());
        Assert.assertNotNull(policy.getAudit());
        Assert.assertNotNull(policy.getMetadata());
    }

    private IntentExtraction buildIntent(final String segment, final int percent, final int nth, final boolean promo) {
        final IntentExtraction intent = new IntentExtraction();
        intent.setCustomerSegment(segment);
        intent.setDiscountPercent(percent);
        intent.setCadenceEveryNthPurchase(nth);
        intent.setPromoCodeRequired(promo);
        intent.setTriggerEvent("purchase_priced");
        intent.setScope("lifetime");
        return intent;
    }
}
