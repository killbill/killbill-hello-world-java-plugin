package org.killbill.billing.plugin.helloworld.policytranslator.simulator;

import org.killbill.billing.plugin.helloworld.policytranslator.model.*;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Test(groups = "fast")
public class PolicySimulatorTest {

    private PolicySimulator simulator;

    @BeforeMethod(groups = "fast")
    public void setUp() {
        simulator = new PolicySimulator();
    }

    @Test(groups = "fast")
    public void testDiscountOnThirdPurchase() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = Arrays.asList(
                new Purchase("p1", "success", 100.0),
                new Purchase("p2", "success", 150.0),
                new Purchase("p3", "success", 200.0)
        );

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertTrue(result.isDiscountApplied());
        Assert.assertEquals(result.getDiscountAmount(), 50.0, 0.01); // 25% of 200
        Assert.assertTrue(result.getExplanation().contains("3rd"));
    }

    @Test(groups = "fast")
    public void testNoDiscountOnFirstPurchase() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = Arrays.asList(
                new Purchase("p1", "success", 100.0)
        );

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertFalse(result.isDiscountApplied());
        Assert.assertEquals(result.getDiscountAmount(), 0.0, 0.01);
    }

    @Test(groups = "fast")
    public void testNoDiscountOnSecondPurchase() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = Arrays.asList(
                new Purchase("p1", "success", 100.0),
                new Purchase("p2", "success", 150.0)
        );

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertFalse(result.isDiscountApplied());
    }

    @Test(groups = "fast")
    public void testDiscountOnSixthPurchase() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            purchases.add(new Purchase("p" + i, "success", 100.0));
        }

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertTrue(result.isDiscountApplied());
        Assert.assertEquals(result.getDiscountAmount(), 25.0, 0.01);
    }

    @Test(groups = "fast")
    public void testNoDiscountOnFourthPurchase() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            purchases.add(new Purchase("p" + i, "success", 100.0));
        }

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertFalse(result.isDiscountApplied());
    }

    @Test(groups = "fast")
    public void testFailedPurchasesSkipped() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = Arrays.asList(
                new Purchase("p1", "success", 100.0),
                new Purchase("p2", "failed", 150.0),     // Failed, doesn't count
                new Purchase("p3", "success", 200.0),
                new Purchase("p4", "success", 250.0)      // This is 3rd successful
        );

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertTrue(result.isDiscountApplied());
        Assert.assertEquals(result.getDiscountAmount(), 62.5, 0.01); // 25% of 250
    }

    @Test(groups = "fast")
    public void testCurrentPurchaseFailed() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = Arrays.asList(
                new Purchase("p1", "success", 100.0),
                new Purchase("p2", "success", 150.0),
                new Purchase("p3", "failed", 200.0)
        );

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertFalse(result.isDiscountApplied());
        Assert.assertTrue(result.getExplanation().contains("not successful"));
    }

    @Test(groups = "fast")
    public void testEvery2ndPurchase() {
        final PolicyJson policy = buildPolicy(2, 10);
        final List<Purchase> purchases = Arrays.asList(
                new Purchase("p1", "success", 100.0),
                new Purchase("p2", "success", 200.0)
        );

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertTrue(result.isDiscountApplied());
        Assert.assertEquals(result.getDiscountAmount(), 20.0, 0.01);
    }

    @Test(groups = "fast")
    public void testNullPolicy() {
        final SimulationResult result = simulator.simulate(null, Arrays.asList(new Purchase("p1", "success", 100.0)));
        Assert.assertFalse(result.isDiscountApplied());
        Assert.assertTrue(result.getExplanation().contains("No policy"));
    }

    @Test(groups = "fast")
    public void testEmptyPurchases() {
        final PolicyJson policy = buildPolicy(3, 25);
        final SimulationResult result = simulator.simulate(policy, new ArrayList<>());
        Assert.assertFalse(result.isDiscountApplied());
    }

    @Test(groups = "fast")
    public void testNinthPurchase_EveryThird() {
        final PolicyJson policy = buildPolicy(3, 25);
        final List<Purchase> purchases = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            purchases.add(new Purchase("p" + i, "success", 100.0));
        }

        final SimulationResult result = simulator.simulate(policy, purchases);
        Assert.assertTrue(result.isDiscountApplied());
    }

    private PolicyJson buildPolicy(final int n, final int percent) {
        final PolicyJson policy = new PolicyJson();
        policy.setCondition(new Condition("every_nth_purchase", n, "successful_purchases", "lifetime"));
        policy.setBenefit(new Benefit("percentage_discount", percent, "all_line_items", false));
        return policy;
    }
}
