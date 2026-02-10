package org.killbill.billing.plugin.helloworld.policytranslator.translator;

/**
 * Exception thrown when policy translation fails.
 */
public class TranslationException extends Exception {

    public TranslationException(final String message) {
        super(message);
    }

    public TranslationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
