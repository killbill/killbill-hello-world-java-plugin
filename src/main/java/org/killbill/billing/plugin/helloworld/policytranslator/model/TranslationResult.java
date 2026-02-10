package org.killbill.billing.plugin.helloworld.policytranslator.model;

import java.util.ArrayList;
import java.util.List;

public class TranslationResult {

    private PolicyJson policyJson;
    private String englishDsl;
    private ValidationResult validation;
    private List<String> assumptions;
    private List<String> questions;

    public TranslationResult() {
        this.assumptions = new ArrayList<>();
        this.questions = new ArrayList<>();
        this.validation = new ValidationResult();
    }

    public PolicyJson getPolicyJson() {
        return policyJson;
    }

    public void setPolicyJson(final PolicyJson policyJson) {
        this.policyJson = policyJson;
    }

    public String getEnglishDsl() {
        return englishDsl;
    }

    public void setEnglishDsl(final String englishDsl) {
        this.englishDsl = englishDsl;
    }

    public ValidationResult getValidation() {
        return validation;
    }

    public void setValidation(final ValidationResult validation) {
        this.validation = validation;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(final List<String> assumptions) {
        this.assumptions = assumptions;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(final List<String> questions) {
        this.questions = questions;
    }
}
