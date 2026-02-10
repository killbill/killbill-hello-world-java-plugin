package org.killbill.billing.plugin.helloworld.policytranslator.translator;

import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.MockLlmClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Negative tests: inputs that should fail translation with clear error messages.
 */
@Test(groups = "fast")
public class PolicyTranslatorNegativeTest {

    private PolicyTranslator translator;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        translator = new PolicyTranslator(new MockLlmClient(), new ConceptRegistry());
    }

    @Test(groups = "fast", expectedExceptions = TranslationException.class)
    public void testEmptyInput() throws TranslationException {
        translator.translate("", "user");
    }

    @Test(groups = "fast", expectedExceptions = TranslationException.class)
    public void testNullInput() throws TranslationException {
        translator.translate(null, "user");
    }

    @Test(groups = "fast")
    public void testDiscountTooHigh_110percent() {
        try {
            translator.translate("Give 110% discount on every 3rd purchase", "user");
            Assert.fail("Expected TranslationException for 110% discount");
        } catch (final TranslationException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum") || e.getMessage().contains("out of range"),
                    "Error should mention exceeds maximum, got: " + e.getMessage());
        }
    }

    @Test(groups = "fast")
    public void testApplyOnInvoices() {
        try {
            translator.translate("Apply on invoices 10% off every 3rd", "user");
            Assert.fail("Expected TranslationException for invoices");
        } catch (final TranslationException e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("invoice") ||
                              e.getMessage().toLowerCase().contains("not a supported"),
                    "Error should mention unsupported object, got: " + e.getMessage());
        }
    }

    @Test(groups = "fast")
    public void testEveryThirdMonth() {
        try {
            translator.translate("10% off every third month for returning customers", "user");
            Assert.fail("Expected TranslationException for temporal cadence");
        } catch (final TranslationException e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("month") ||
                              e.getMessage().toLowerCase().contains("not supported"),
                    "Error should mention unsupported temporal, got: " + e.getMessage());
        }
    }

    @Test(groups = "fast")
    public void testUnsupportedScope_last30Days() {
        try {
            translator.translate("25% off every 3rd purchase, use last 30 days, returning customers", "user");
            Assert.fail("Expected TranslationException for unsupported scope");
        } catch (final TranslationException e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("30 days") ||
                              e.getMessage().toLowerCase().contains("not supported") ||
                              e.getMessage().toLowerCase().contains("scope"),
                    "Error should mention unsupported scope, got: " + e.getMessage());
        }
    }

    @Test(groups = "fast", expectedExceptions = TranslationException.class)
    public void testWhitespaceOnlyInput() throws TranslationException {
        translator.translate("   ", "user");
    }

    @Test(groups = "fast")
    public void testInputTooLong() {
        final StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longInput.append("discount ");
        }
        try {
            translator.translate(longInput.toString(), "user");
            Assert.fail("Expected TranslationException for too-long input");
        } catch (final TranslationException e) {
            Assert.assertTrue(e.getMessage().contains("exceeds maximum length"),
                    "Error should mention length, got: " + e.getMessage());
        }
    }
}
