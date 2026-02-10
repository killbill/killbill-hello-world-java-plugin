package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.MockLlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.model.IntentExtraction;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Stage A intent extraction with the mock LLM client.
 */
@Test(groups = "fast")
public class IntentExtractorTest {

    private IntentExtractor extractor;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        final MockLlmClient mockClient = new MockLlmClient();
        final ConceptRegistry registry = new ConceptRegistry();
        final PromptBuilder promptBuilder = new PromptBuilder(registry);
        extractor = new IntentExtractor(mockClient, promptBuilder, registry);
    }

    @Test(groups = "fast")
    public void testExtract_ReturningCustomers25Pct3rd() throws TranslationException {
        final IntentExtraction intent = extractor.extract(
                "Returning customers get 25% off every third purchase, no promo code.");

        Assert.assertEquals(intent.getCustomerSegment(), "returning");
        Assert.assertEquals(intent.getDiscountPercent(), 25);
        Assert.assertEquals(intent.getCadenceEveryNthPurchase(), 3);
        Assert.assertFalse(intent.isPromoCodeRequired());
        Assert.assertEquals(intent.getTriggerEvent(), "purchase_priced");
        Assert.assertEquals(intent.getScope(), "lifetime");
    }

    @Test(groups = "fast")
    public void testExtract_AllCustomers10Pct2nd() throws TranslationException {
        final IntentExtraction intent = extractor.extract(
                "10% off every 2nd purchase for all customers.");

        Assert.assertEquals(intent.getCustomerSegment(), "all");
        Assert.assertEquals(intent.getDiscountPercent(), 10);
        Assert.assertEquals(intent.getCadenceEveryNthPurchase(), 2);
        Assert.assertFalse(intent.isPromoCodeRequired());
    }

    @Test(groups = "fast")
    public void testExtract_NewCustomers15Pct5th_PromoRequired() throws TranslationException {
        final IntentExtraction intent = extractor.extract(
                "New customers, 15 percent discount on every 5th order, requires promo code.");

        Assert.assertEquals(intent.getCustomerSegment(), "new");
        Assert.assertEquals(intent.getDiscountPercent(), 15);
        Assert.assertEquals(intent.getCadenceEveryNthPurchase(), 5);
        Assert.assertTrue(intent.isPromoCodeRequired());
    }

    @Test(groups = "fast", expectedExceptions = TranslationException.class)
    public void testExtract_EmptyInput() throws TranslationException {
        extractor.extract("");
    }

    @Test(groups = "fast", expectedExceptions = TranslationException.class)
    public void testExtract_NullInput() throws TranslationException {
        extractor.extract(null);
    }

    @Test(groups = "fast")
    public void testExtract_NotesGenerated() throws TranslationException {
        final IntentExtraction intent = extractor.extract(
                "Returning customers get 25% off every third purchase, no promo code.");
        Assert.assertNotNull(intent.getNotes());
        Assert.assertFalse(intent.getNotes().isEmpty());
    }
}
