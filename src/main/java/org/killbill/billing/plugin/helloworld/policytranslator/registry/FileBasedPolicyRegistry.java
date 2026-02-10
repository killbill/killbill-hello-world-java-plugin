package org.killbill.billing.plugin.helloworld.policytranslator.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.killbill.billing.plugin.helloworld.policytranslator.model.PolicyJson;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File-based policy registry. Stores each policy version as a JSON file:
 * {baseDir}/{policy_id}/v{version}.json
 */
public class FileBasedPolicyRegistry {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final Path baseDir;
    private final ObjectMapper objectMapper;

    public FileBasedPolicyRegistry() {
        this(Path.of(System.getProperty("user.home"), ".policytranslator", "policies"));
    }

    public FileBasedPolicyRegistry(final Path baseDir) {
        this.baseDir = baseDir;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Save a policy as a new draft version. Returns the version number.
     */
    public int save(final PolicyJson policy, final String originalText, final String createdBy) throws IOException {
        final String policyId = policy.getPolicyId();
        if (policyId == null || policyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Policy must have a policy_id");
        }

        final Path policyDir = baseDir.resolve(sanitizeFilename(policyId));
        Files.createDirectories(policyDir);

        final int nextVersion = getNextVersion(policyDir);
        final String now = Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);

        final PolicyEnvelope envelope = new PolicyEnvelope(
                policy, originalText, PolicyStatus.DRAFT, nextVersion, createdBy, now);

        final Path versionFile = policyDir.resolve("v" + nextVersion + ".json");
        objectMapper.writeValue(versionFile.toFile(), envelope);

        return nextVersion;
    }

    /**
     * Load the latest version of a policy.
     */
    public PolicyEnvelope getLatestVersion(final String policyId) throws IOException {
        final Path policyDir = baseDir.resolve(sanitizeFilename(policyId));
        if (!Files.isDirectory(policyDir)) {
            return null;
        }

        final int latest = getLatestVersionNumber(policyDir);
        if (latest <= 0) {
            return null;
        }

        final Path versionFile = policyDir.resolve("v" + latest + ".json");
        return objectMapper.readValue(versionFile.toFile(), PolicyEnvelope.class);
    }

    /**
     * Load a specific version of a policy.
     */
    public PolicyEnvelope getVersion(final String policyId, final int version) throws IOException {
        final Path versionFile = baseDir.resolve(sanitizeFilename(policyId)).resolve("v" + version + ".json");
        if (!Files.exists(versionFile)) {
            return null;
        }
        return objectMapper.readValue(versionFile.toFile(), PolicyEnvelope.class);
    }

    /**
     * List all version numbers for a policy.
     */
    public List<Integer> listVersions(final String policyId) throws IOException {
        final Path policyDir = baseDir.resolve(sanitizeFilename(policyId));
        if (!Files.isDirectory(policyDir)) {
            return Collections.emptyList();
        }

        try (final Stream<Path> files = Files.list(policyDir)) {
            return files
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith("v") && name.endsWith(".json"))
                    .map(name -> {
                        try {
                            return Integer.parseInt(name.substring(1, name.length() - 5));
                        } catch (final NumberFormatException e) {
                            return -1;
                        }
                    })
                    .filter(v -> v > 0)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * List all policy IDs in the registry.
     */
    public List<String> listPolicies() throws IOException {
        if (!Files.isDirectory(baseDir)) {
            return Collections.emptyList();
        }

        try (final Stream<Path> dirs = Files.list(baseDir)) {
            return dirs
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Update the status of a specific policy version.
     */
    public void updateStatus(final String policyId, final int version, final PolicyStatus status) throws IOException {
        final Path versionFile = baseDir.resolve(sanitizeFilename(policyId)).resolve("v" + version + ".json");
        if (!Files.exists(versionFile)) {
            throw new IOException("Policy version not found: " + policyId + " v" + version);
        }

        final PolicyEnvelope envelope = objectMapper.readValue(versionFile.toFile(), PolicyEnvelope.class);
        envelope.setStatus(status);
        objectMapper.writeValue(versionFile.toFile(), envelope);
    }

    public Path getBaseDir() {
        return baseDir;
    }

    private int getNextVersion(final Path policyDir) throws IOException {
        return getLatestVersionNumber(policyDir) + 1;
    }

    private int getLatestVersionNumber(final Path policyDir) throws IOException {
        if (!Files.isDirectory(policyDir)) {
            return 0;
        }

        try (final Stream<Path> files = Files.list(policyDir)) {
            return files
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.startsWith("v") && name.endsWith(".json"))
                    .map(name -> {
                        try {
                            return Integer.parseInt(name.substring(1, name.length() - 5));
                        } catch (final NumberFormatException e) {
                            return 0;
                        }
                    })
                    .max(Integer::compareTo)
                    .orElse(0);
        }
    }

    private static String sanitizeFilename(final String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
