package org.killbill.billing.plugin.helloworld.policytranslator.llm;

/**
 * Exception thrown when an LLM call fails.
 */
public class LlmException extends Exception {

    public LlmException(final String message) {
        super(message);
    }

    public LlmException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
