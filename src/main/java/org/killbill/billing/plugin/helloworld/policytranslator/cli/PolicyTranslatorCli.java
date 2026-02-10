package org.killbill.billing.plugin.helloworld.policytranslator.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.killbill.billing.plugin.helloworld.policytranslator.concept.ConceptRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.LlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.MockLlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.llm.OpenAiLlmClient;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;
import org.killbill.billing.plugin.helloworld.policytranslator.model.Purchase;
import org.killbill.billing.plugin.helloworld.policytranslator.model.SimulationResult;
import org.killbill.billing.plugin.helloworld.policytranslator.model.TranslationResult;
import org.killbill.billing.plugin.helloworld.policytranslator.registry.FileBasedPolicyRegistry;
import org.killbill.billing.plugin.helloworld.policytranslator.registry.PolicyEnvelope;
import org.killbill.billing.plugin.helloworld.policytranslator.simulator.PolicySimulator;
import org.killbill.billing.plugin.helloworld.policytranslator.translator.PolicyTranslator;
import org.killbill.billing.plugin.helloworld.policytranslator.translator.TranslationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CLI entry point for the Policy Translator POC.
 *
 * Usage:
 *   translate [--mock] [--save] [--created-by email] "natural language text"
 *   simulate  --policy &lt;file&gt; --purchases &lt;json_file&gt;
 *   list
 *   show &lt;policy_id&gt;
 */
public class PolicyTranslatorCli {

    private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(final String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
            return;
        }

        final String command = args[0];
        try {
            switch (command) {
                case "translate":
                    handleTranslate(Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "simulate":
                    handleSimulate(Arrays.copyOfRange(args, 1, args.length));
                    break;
                case "list":
                    handleList();
                    break;
                case "show":
                    handleShow(Arrays.copyOfRange(args, 1, args.length));
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (final TranslationException e) {
            System.err.println("Translation error: " + e.getMessage());
            System.exit(2);
        } catch (final Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleTranslate(final String[] args) throws TranslationException, IOException {
        boolean useMock = false;
        boolean save = false;
        String createdBy = "cli-user";
        String text = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--mock":
                    useMock = true;
                    break;
                case "--save":
                    save = true;
                    break;
                case "--created-by":
                    if (i + 1 < args.length) {
                        createdBy = args[++i];
                    }
                    break;
                default:
                    text = args[i];
                    break;
            }
        }

        if (text == null || text.trim().isEmpty()) {
            System.err.println("Error: No input text provided.");
            System.err.println("Usage: translate [--mock] [--save] [--created-by email] \"text\"");
            System.exit(1);
            return;
        }

        final LlmClient llmClient = createLlmClient(useMock);
        final ConceptRegistry registry = new ConceptRegistry();
        final PolicyTranslator translator = new PolicyTranslator(llmClient, registry);

        System.out.println("=== Translating ===");
        System.out.println("Input: \"" + text + "\"");
        System.out.println();

        final TranslationResult result = translator.translate(text, createdBy);

        // English DSL
        System.out.println("=== Generated Policy (English DSL) ===");
        System.out.println(result.getEnglishDsl());

        // Canonical JSON
        System.out.println("=== Canonical Policy JSON ===");
        System.out.println(PRETTY_MAPPER.writeValueAsString(result.getPolicyJson()));
        System.out.println();

        // Validation
        System.out.println("=== Validation Results ===");
        if (result.getValidation().isValid()) {
            System.out.println("  Status: VALID");
        } else {
            System.out.println("  Status: INVALID");
            for (final String error : result.getValidation().getErrors()) {
                System.out.println("  ERROR: " + error);
            }
        }
        for (final String warning : result.getValidation().getWarnings()) {
            System.out.println("  WARNING: " + warning);
        }
        System.out.println();

        // Assumptions
        if (!result.getAssumptions().isEmpty()) {
            System.out.println("=== Explanation and Assumptions ===");
            for (final String assumption : result.getAssumptions()) {
                System.out.println("  - " + assumption);
            }
            System.out.println();
        }

        // Questions
        if (!result.getQuestions().isEmpty()) {
            System.out.println("=== Clarification Questions ===");
            for (final String question : result.getQuestions()) {
                System.out.println("  ? " + question);
            }
            System.out.println();
        }

        // Save if requested
        if (save && result.getValidation().isValid()) {
            final FileBasedPolicyRegistry policyRegistry = new FileBasedPolicyRegistry();
            final int version = policyRegistry.save(result.getPolicyJson(), text, createdBy);
            System.out.println("=== Saved ===");
            System.out.println("  Policy: " + result.getPolicyJson().getPolicyId() + " v" + version);
            System.out.println("  Location: " + policyRegistry.getBaseDir());
        }
    }

    private static void handleSimulate(final String[] args) throws IOException {
        String policyFile = null;
        String purchasesFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--policy":
                    if (i + 1 < args.length) {
                        policyFile = args[++i];
                    }
                    break;
                case "--purchases":
                    if (i + 1 < args.length) {
                        purchasesFile = args[++i];
                    }
                    break;
                default:
                    break;
            }
        }

        if (policyFile == null || purchasesFile == null) {
            System.err.println("Usage: simulate --policy <file> --purchases <json_file>");
            System.exit(1);
            return;
        }

        final ObjectMapper mapper = new ObjectMapper();
        final PolicyJson policy = mapper.readValue(new File(policyFile), PolicyJson.class);
        final Purchase[] purchases = mapper.readValue(new File(purchasesFile), Purchase[].class);

        final PolicySimulator simulator = new PolicySimulator();

        System.out.println("=== Simulation ===");
        System.out.println("Policy: " + policy.getPolicyId());
        System.out.println("Purchases: " + purchases.length);
        System.out.println();

        // Simulate for each purchase as "current" (cumulative)
        final List<Purchase> history = new ArrayList<>();
        for (final Purchase purchase : purchases) {
            history.add(purchase);
            final SimulationResult result = simulator.simulate(policy, new ArrayList<>(history));
            final String marker = result.isDiscountApplied() ? "[DISCOUNT]" : "[--------]";
            System.out.println(marker + " " + result.getExplanation());
            if (result.isDiscountApplied()) {
                System.out.printf("           Discount amount: %.2f%n", result.getDiscountAmount());
            }
        }
    }

    private static void handleList() throws IOException {
        final FileBasedPolicyRegistry registry = new FileBasedPolicyRegistry();
        final List<String> policies = registry.listPolicies();

        if (policies.isEmpty()) {
            System.out.println("No policies found.");
            System.out.println("Registry location: " + registry.getBaseDir());
            return;
        }

        System.out.println("=== Saved Policies ===");
        for (final String policyId : policies) {
            final List<Integer> versions = registry.listVersions(policyId);
            System.out.println("  " + policyId + " (" + versions.size() + " version(s))");
        }
    }

    private static void handleShow(final String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: show <policy_id>");
            System.exit(1);
            return;
        }

        final String policyId = args[0];
        final FileBasedPolicyRegistry registry = new FileBasedPolicyRegistry();
        final PolicyEnvelope envelope = registry.getLatestVersion(policyId);

        if (envelope == null) {
            System.err.println("Policy not found: " + policyId);
            System.exit(1);
            return;
        }

        System.out.println("=== Policy: " + policyId + " v" + envelope.getVersion() + " ===");
        System.out.println("Status: " + envelope.getStatus());
        System.out.println("Created by: " + envelope.getCreatedBy());
        System.out.println("Created at: " + envelope.getCreatedAt());
        System.out.println("Original text: \"" + envelope.getOriginalText() + "\"");
        System.out.println();
        System.out.println("=== Canonical JSON ===");
        System.out.println(PRETTY_MAPPER.writeValueAsString(envelope.getPolicy()));
    }

    private static LlmClient createLlmClient(final boolean useMock) {
        if (useMock) {
            return new MockLlmClient();
        }

        final String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("Note: OPENAI_API_KEY not set, using mock LLM client.");
            return new MockLlmClient();
        }

        return new OpenAiLlmClient();
    }

    private static void printUsage() {
        System.out.println("Policy Translator POC - CLI");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  translate [--mock] [--save] [--created-by email] \"text\"");
        System.out.println("    Translate natural language to a billing policy.");
        System.out.println();
        System.out.println("  simulate --policy <file> --purchases <json_file>");
        System.out.println("    Simulate a policy against a list of purchases.");
        System.out.println();
        System.out.println("  list");
        System.out.println("    List all saved policies.");
        System.out.println();
        System.out.println("  show <policy_id>");
        System.out.println("    Show the latest version of a saved policy.");
    }
}
