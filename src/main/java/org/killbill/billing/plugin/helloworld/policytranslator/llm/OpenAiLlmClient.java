package org.killbill.billing.plugin.helloworld.policytranslator.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * LLM client implementation that calls the OpenAI Chat Completions API.
 * Reads OPENAI_API_KEY from environment. Uses temperature 0 and JSON response format.
 */
public class OpenAiLlmClient implements LlmClient {

    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiLlmClient() {
        this(System.getenv("OPENAI_API_KEY"), DEFAULT_API_URL, DEFAULT_MODEL);
    }

    public OpenAiLlmClient(final String apiKey, final String apiUrl, final String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set");
        }
        this.apiKey = apiKey;
        this.apiUrl = apiUrl != null ? apiUrl : DEFAULT_API_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.httpClient = HttpClient.newBuilder()
                                    .connectTimeout(TIMEOUT)
                                    .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String complete(final String systemPrompt, final String userPrompt) throws LlmException {
        try {
            final String requestBody = buildRequestBody(systemPrompt, userPrompt);

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(TIMEOUT)
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new LlmException("OpenAI API returned status " + response.statusCode() + ": " + response.body());
            }

            return extractContent(response.body());
        } catch (final IOException | InterruptedException e) {
            throw new LlmException("Failed to call OpenAI API", e);
        }
    }

    private String buildRequestBody(final String systemPrompt, final String userPrompt) throws IOException {
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0);

        final ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_object");
        root.set("response_format", responseFormat);

        final ArrayNode messages = objectMapper.createArrayNode();

        final ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        final ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        root.set("messages", messages);

        return objectMapper.writeValueAsString(root);
    }

    private String extractContent(final String responseBody) throws LlmException {
        try {
            final JsonNode root = objectMapper.readTree(responseBody);
            final JsonNode choices = root.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new LlmException("No choices in OpenAI response");
            }
            final JsonNode message = choices.get(0).get("message");
            if (message == null) {
                throw new LlmException("No message in OpenAI response choice");
            }
            final JsonNode content = message.get("content");
            if (content == null) {
                throw new LlmException("No content in OpenAI response message");
            }
            return content.asText();
        } catch (final IOException e) {
            throw new LlmException("Failed to parse OpenAI response", e);
        }
    }
}
