package org.killbill.billing.plugin.helloworld.policytranslator.model;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public ValidationResult(final List<String> errors, final List<String> warnings) {
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
    }

    public void addError(final String error) {
        errors.add(error);
    }

    public void addWarning(final String warning) {
        warnings.add(warning);
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
