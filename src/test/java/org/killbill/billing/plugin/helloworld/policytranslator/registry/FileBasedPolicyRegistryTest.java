package org.killbill.billing.plugin.helloworld.policytranslator.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.killbill.billing.plugin.helloworld.policytranslator.model.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Test(groups = "fast")
public class FileBasedPolicyRegistryTest {

    private Path tempDir;
    private FileBasedPolicyRegistry registry;

    @BeforeMethod(groups = "fast")
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("policy-registry-test");
        registry = new FileBasedPolicyRegistry(tempDir);
    }

    @AfterMethod(groups = "fast")
    public void tearDown() throws IOException {
        // Clean up temp directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    @Test(groups = "fast")
    public void testSaveAndLoad() throws IOException {
        final PolicyJson policy = buildPolicy("returning.every_3rd_purchase_25pct");
        final int version = registry.save(policy, "Test input text", "test@example.com");
        Assert.assertEquals(version, 1);

        final PolicyEnvelope loaded = registry.getLatestVersion("returning.every_3rd_purchase_25pct");
        Assert.assertNotNull(loaded);
        Assert.assertEquals(loaded.getVersion(), 1);
        Assert.assertEquals(loaded.getStatus(), PolicyStatus.DRAFT);
        Assert.assertEquals(loaded.getOriginalText(), "Test input text");
        Assert.assertEquals(loaded.getCreatedBy(), "test@example.com");
        Assert.assertNotNull(loaded.getPolicy());
        Assert.assertEquals(loaded.getPolicy().getPolicyId(), "returning.every_3rd_purchase_25pct");
    }

    @Test(groups = "fast")
    public void testMultipleVersions() throws IOException {
        final PolicyJson policy = buildPolicy("returning.every_3rd_purchase_25pct");
        Assert.assertEquals(registry.save(policy, "v1 text", "user1"), 1);
        Assert.assertEquals(registry.save(policy, "v2 text", "user2"), 2);
        Assert.assertEquals(registry.save(policy, "v3 text", "user3"), 3);

        final PolicyEnvelope latest = registry.getLatestVersion("returning.every_3rd_purchase_25pct");
        Assert.assertNotNull(latest);
        Assert.assertEquals(latest.getVersion(), 3);
        Assert.assertEquals(latest.getOriginalText(), "v3 text");
    }

    @Test(groups = "fast")
    public void testListVersions() throws IOException {
        final PolicyJson policy = buildPolicy("returning.every_3rd_purchase_25pct");
        registry.save(policy, "v1", "user");
        registry.save(policy, "v2", "user");

        final List<Integer> versions = registry.listVersions("returning.every_3rd_purchase_25pct");
        Assert.assertEquals(versions.size(), 2);
        Assert.assertEquals((int) versions.get(0), 1);
        Assert.assertEquals((int) versions.get(1), 2);
    }

    @Test(groups = "fast")
    public void testListPolicies() throws IOException {
        registry.save(buildPolicy("policy.a"), "text", "user");
        registry.save(buildPolicy("policy.b"), "text", "user");

        final List<String> policies = registry.listPolicies();
        Assert.assertEquals(policies.size(), 2);
        Assert.assertTrue(policies.contains("policy.a"));
        Assert.assertTrue(policies.contains("policy.b"));
    }

    @Test(groups = "fast")
    public void testGetVersion() throws IOException {
        final PolicyJson policy = buildPolicy("test.policy");
        registry.save(policy, "v1 text", "user");
        registry.save(policy, "v2 text", "user");

        final PolicyEnvelope v1 = registry.getVersion("test.policy", 1);
        Assert.assertNotNull(v1);
        Assert.assertEquals(v1.getOriginalText(), "v1 text");

        final PolicyEnvelope v2 = registry.getVersion("test.policy", 2);
        Assert.assertNotNull(v2);
        Assert.assertEquals(v2.getOriginalText(), "v2 text");
    }

    @Test(groups = "fast")
    public void testUpdateStatus() throws IOException {
        final PolicyJson policy = buildPolicy("test.policy");
        registry.save(policy, "text", "user");

        registry.updateStatus("test.policy", 1, PolicyStatus.APPROVED);

        final PolicyEnvelope loaded = registry.getVersion("test.policy", 1);
        Assert.assertEquals(loaded.getStatus(), PolicyStatus.APPROVED);
    }

    @Test(groups = "fast")
    public void testGetLatestVersion_NotFound() throws IOException {
        final PolicyEnvelope result = registry.getLatestVersion("nonexistent.policy");
        Assert.assertNull(result);
    }

    @Test(groups = "fast")
    public void testListVersions_NotFound() throws IOException {
        final List<Integer> versions = registry.listVersions("nonexistent.policy");
        Assert.assertTrue(versions.isEmpty());
    }

    @Test(groups = "fast")
    public void testListPolicies_EmptyRegistry() throws IOException {
        final List<String> policies = registry.listPolicies();
        Assert.assertTrue(policies.isEmpty());
    }

    private PolicyJson buildPolicy(final String policyId) {
        final PolicyJson policy = new PolicyJson();
        policy.setDslVersion("billing-intent/0.1");
        policy.setPolicyId(policyId);
        policy.setDescription("Test policy");
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
