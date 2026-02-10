package org.killbill.billing.plugin.helloworld.policytranslator.concept;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads and provides access to the allowed vocabulary defined in concepts.json.
 * All translation must map into this vocabulary; if it cannot, translation must fail.
 */
public class ConceptRegistry {

    private static final String CONCEPTS_RESOURCE = "/policytranslator/concepts.json";

    private List<String> events;
    private List<String> timings;
    private List<String> customerSegments;
    private List<String> appliesToObjects;
    private List<String> targets;

    // benefits -> percentage_discount -> {min, max}
    private Map<String, Map<String, Object>> benefits;

    // conditions -> every_nth_purchase -> {n_min, n_max, basis: [...], scope: [...]}
    private Map<String, Map<String, Object>> conditions;

    public ConceptRegistry() {
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        final ObjectMapper mapper = new ObjectMapper();
        try (final InputStream is = getClass().getResourceAsStream(CONCEPTS_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Cannot find concepts.json on classpath: " + CONCEPTS_RESOURCE);
            }
            final Map<String, Object> root = mapper.readValue(is, Map.class);
            this.events = toStringList(root.get("events"));
            this.timings = toStringList(root.get("timings"));
            this.customerSegments = toStringList(root.get("customer_segments"));
            this.appliesToObjects = toStringList(root.get("applies_to_objects"));
            this.targets = toStringList(root.get("targets"));
            this.benefits = (Map<String, Map<String, Object>>) root.get("benefits");
            this.conditions = (Map<String, Map<String, Object>>) root.get("conditions");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load concepts.yml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(final Object obj) {
        if (obj instanceof List) {
            return Collections.unmodifiableList((List<String>) obj);
        }
        return Collections.emptyList();
    }

    public boolean isValidEvent(final String event) {
        return events.contains(event);
    }

    public boolean isValidTiming(final String timing) {
        return timings.contains(timing);
    }

    public boolean isValidCustomerSegment(final String segment) {
        return customerSegments.contains(segment);
    }

    public boolean isValidAppliesToObject(final String object) {
        return appliesToObjects.contains(object);
    }

    public boolean isValidTarget(final String target) {
        return targets.contains(target);
    }

    public boolean isValidBenefitKind(final String kind) {
        return benefits.containsKey(kind);
    }

    public boolean isValidConditionKind(final String kind) {
        return conditions.containsKey(kind);
    }

    public int getBenefitMinPercent() {
        final Map<String, Object> pctDiscount = benefits.get("percentage_discount");
        return pctDiscount != null ? ((Number) pctDiscount.get("min")).intValue() : 1;
    }

    public int getBenefitMaxPercent() {
        final Map<String, Object> pctDiscount = benefits.get("percentage_discount");
        return pctDiscount != null ? ((Number) pctDiscount.get("max")).intValue() : 90;
    }

    public int getConditionNMin() {
        final Map<String, Object> everyNth = conditions.get("every_nth_purchase");
        return everyNth != null ? ((Number) everyNth.get("n_min")).intValue() : 2;
    }

    public int getConditionNMax() {
        final Map<String, Object> everyNth = conditions.get("every_nth_purchase");
        return everyNth != null ? ((Number) everyNth.get("n_max")).intValue() : 12;
    }

    @SuppressWarnings("unchecked")
    public List<String> getConditionBasis() {
        final Map<String, Object> everyNth = conditions.get("every_nth_purchase");
        return everyNth != null ? (List<String>) everyNth.get("basis") : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<String> getConditionScopes() {
        final Map<String, Object> everyNth = conditions.get("every_nth_purchase");
        return everyNth != null ? (List<String>) everyNth.get("scope") : Collections.emptyList();
    }

    public List<String> getEvents() {
        return events;
    }

    public List<String> getTimings() {
        return timings;
    }

    public List<String> getCustomerSegments() {
        return customerSegments;
    }

    public List<String> getAppliesToObjects() {
        return appliesToObjects;
    }

    public List<String> getTargets() {
        return targets;
    }

    /**
     * Returns a summary of all allowed concepts, for embedding in LLM prompts.
     */
    public String toPromptSummary() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Allowed concepts:\n");
        sb.append("  events: ").append(events).append("\n");
        sb.append("  timings: ").append(timings).append("\n");
        sb.append("  customer_segments: ").append(customerSegments).append("\n");
        sb.append("  applies_to_objects: ").append(appliesToObjects).append("\n");
        sb.append("  targets: ").append(targets).append("\n");
        sb.append("  benefit_kinds: ").append(benefits.keySet()).append("\n");
        sb.append("    percentage_discount: min=").append(getBenefitMinPercent())
          .append(", max=").append(getBenefitMaxPercent()).append("\n");
        sb.append("  condition_kinds: ").append(conditions.keySet()).append("\n");
        sb.append("    every_nth_purchase: n_min=").append(getConditionNMin())
          .append(", n_max=").append(getConditionNMax())
          .append(", basis=").append(getConditionBasis())
          .append(", scope=").append(getConditionScopes()).append("\n");
        return sb.toString();
    }
}
