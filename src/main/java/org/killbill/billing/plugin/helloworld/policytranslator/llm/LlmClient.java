package org.killbill.billing.plugin.helloworld.policytranslator.llm;

/**
 * Abstraction over LLM providers. Implementations must return raw JSON strings.
 */
public interface LlmClient {

    /**
     * Send a system prompt and user prompt to the LLM and return the response content.
     *
     * @param systemPrompt the system-level instruction
     * @param userPrompt   the user-level input
     * @return raw JSON string response from the model
     * @throws LlmException if the call fails or return is unparseable
     */
    String complete(String systemPrompt, String userPrompt) throws LlmException;
}
