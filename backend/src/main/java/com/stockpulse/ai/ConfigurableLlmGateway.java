package com.stockpulse.ai;

import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Small OpenAI-compatible gateway. Provider-specific response parsing remains
 * in this infrastructure adapter; domain recommendation parsing stays in the AI advisor.
 */
@Component
public class ConfigurableLlmGateway implements LlmGateway {

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String product;
    private final RestClient client;

    public ConfigurableLlmGateway(
            @Value("${llm.enabled:false}") boolean enabled,
            @Value("${llm.base-url:http://localhost:11434}") String baseUrl,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.model:llama3.1}") String model,
            @Value("${llm.product:PC1}") String product,
            RestClient.Builder restClientBuilder) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.product = product;
        this.client = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(new RestTemplateBuilder()
                        .setConnectTimeout(Duration.ofSeconds(3))
                        .setReadTimeout(Duration.ofSeconds(15))
                        .buildRequestFactory())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String call(String prompt) {
        if (!enabled) {
            throw new UnavailableLlmException("LLM integration is disabled");
        }
        try {
            Map<String, Object> request = Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "messages", new Object[]{Map.of("role", "user", "content", prompt)});
            Map<String, Object> response = client.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.set("product", product);
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.setBearerAuth(apiKey);
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new UnavailableLlmException("LLM returned an empty response");
            }
            Object choices = response.get("choices");
            if (!(choices instanceof java.util.List<?> list) || list.isEmpty()
                    || !(list.get(0) instanceof Map<?, ?> choice)
                    || !(choice.get("message") instanceof Map<?, ?> message)
                    || !(message.get("content") instanceof String content)
                    || content.isBlank()) {
                throw new UnavailableLlmException("LLM response did not contain message content");
            }
            return content;
        } catch (RestClientException exception) {
            throw new UnavailableLlmException("LLM request failed", exception);
        }
    }
}