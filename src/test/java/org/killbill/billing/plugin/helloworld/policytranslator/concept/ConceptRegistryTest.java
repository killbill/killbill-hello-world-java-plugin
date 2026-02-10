package org.killbill.billing.plugin.helloworld.policytranslator.concept;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

@Test(groups = "fast")
public class ConceptRegistryTest {

    private ConceptRegistry registry;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        registry = new ConceptRegistry();
    }

    @Test(groups = "fast")
    public void testLoadEvents() {
        final List<String> events = registry.getEvents();
        Assert.assertNotNull(events);
        Assert.assertTrue(events.contains("purchase_priced"));
    }

    @Test(groups = "fast")
    public void testLoadTimings() {
        final List<String> timings = registry.getTimings();
        Assert.assertNotNull(timings);
        Assert.assertTrue(timings.contains("before_invoice_finalized"));
    }

    @Test(groups = "fast")
    public void testLoadCustomerSegments() {
        final List<String> segments = registry.getCustomerSegments();
        Assert.assertNotNull(segments);
        Assert.assertTrue(segments.contains("all"));
        Assert.assertTrue(segments.contains("new"));
        Assert.assertTrue(segments.contains("returning"));
    }

    @Test(groups = "fast")
    public void testLoadAppliesToObjects() {
        Assert.assertTrue(registry.isValidAppliesToObject("purchase"));
        Assert.assertFalse(registry.isValidAppliesToObject("invoice"));
    }

    @Test(groups = "fast")
    public void testLoadTargets() {
        Assert.assertTrue(registry.isValidTarget("all_line_items"));
        Assert.assertFalse(registry.isValidTarget("single_item"));
    }

    @Test(groups = "fast")
    public void testValidEvent() {
        Assert.assertTrue(registry.isValidEvent("purchase_priced"));
        Assert.assertFalse(registry.isValidEvent("invoice_created"));
    }

    @Test(groups = "fast")
    public void testValidCustomerSegment() {
        Assert.assertTrue(registry.isValidCustomerSegment("returning"));
        Assert.assertTrue(registry.isValidCustomerSegment("new"));
        Assert.assertTrue(registry.isValidCustomerSegment("all"));
        Assert.assertFalse(registry.isValidCustomerSegment("vip"));
    }

    @Test(groups = "fast")
    public void testBenefitBounds() {
        Assert.assertEquals(registry.getBenefitMinPercent(), 1);
        Assert.assertEquals(registry.getBenefitMaxPercent(), 90);
    }

    @Test(groups = "fast")
    public void testConditionBounds() {
        Assert.assertEquals(registry.getConditionNMin(), 2);
        Assert.assertEquals(registry.getConditionNMax(), 12);
    }

    @Test(groups = "fast")
    public void testConditionBasis() {
        Assert.assertTrue(registry.getConditionBasis().contains("successful_purchases"));
    }

    @Test(groups = "fast")
    public void testConditionScopes() {
        Assert.assertTrue(registry.getConditionScopes().contains("lifetime"));
    }

    @Test(groups = "fast")
    public void testBenefitKind() {
        Assert.assertTrue(registry.isValidBenefitKind("percentage_discount"));
        Assert.assertFalse(registry.isValidBenefitKind("flat_discount"));
    }

    @Test(groups = "fast")
    public void testConditionKind() {
        Assert.assertTrue(registry.isValidConditionKind("every_nth_purchase"));
        Assert.assertFalse(registry.isValidConditionKind("every_nth_month"));
    }

    @Test(groups = "fast")
    public void testPromptSummary() {
        final String summary = registry.toPromptSummary();
        Assert.assertNotNull(summary);
        Assert.assertTrue(summary.contains("purchase_priced"));
        Assert.assertTrue(summary.contains("returning"));
        Assert.assertTrue(summary.contains("percentage_discount"));
        Assert.assertTrue(summary.contains("every_nth_purchase"));
    }
}
