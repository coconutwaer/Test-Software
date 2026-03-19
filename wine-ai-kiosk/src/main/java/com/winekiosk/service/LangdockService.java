package com.winekiosk.service;

import com.winekiosk.config.LangdockConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LangdockService {

    private static final Logger log = LoggerFactory.getLogger(LangdockService.class);

    private final LangdockConfig config;
    private final RestTemplate restTemplate;

    public LangdockService(LangdockConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    /**
     * Sends a chat completion request to the Langdock API.
     *
     * @param systemPrompt the system prompt
     * @param userMessage  the user message
     * @return the AI response text
     */
    public String chat(String systemPrompt, String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    config.getApiUrl(), request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractContent(response.getBody());
            }
            return "I'm sorry, I couldn't get a response from the AI service right now.";
        } catch (Exception e) {
            log.error("Error calling Langdock API: {}", e.getMessage(), e);
            return "I'm experiencing technical difficulties. Please try again shortly.";
        }
    }

    /**
     * First-pass AI call to extract structured JSON search parameters from the user message.
     *
     * @param userMessage the user's raw message
     * @return JSON string with extracted parameters
     */
    public String extractSearchParameters(String userMessage) {
        String systemPrompt = """
                You are a wine search parameter extractor. Analyze the user's wine request and extract search parameters.
                Return ONLY a valid JSON object with these optional fields (omit fields that are not mentioned or implied):
                {
                  "wineType": "Red|White|Rosé",
                  "sweetness": "Dry|Semi-Dry|Sweet",
                  "maxPrice": <number>,
                  "grapeVariety": "<grape name>",
                  "country": "<country name>",
                  "region": "<region name>",
                  "body": "Light|Medium|Full",
                  "flavorKeywords": "<comma-separated flavor words>",
                  "foodPairing": "<food>"
                }
                Do NOT include markdown, code fences, or any text outside the JSON object.
                """;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.0);
            body.put("max_tokens", 256);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    config.getApiUrl(), request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String content = extractContent(response.getBody());
                return stripCodeFences(content);
            }
        } catch (Exception e) {
            log.error("Error extracting search parameters: {}", e.getMessage(), e);
        }
        return "{}";
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> responseBody) {
        try {
            List<?> choices = (List<?>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.error("Error parsing API response: {}", e.getMessage(), e);
        }
        return "I'm sorry, I couldn't process the AI response.";
    }

    private String stripCodeFences(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
            }
        }
        return trimmed;
    }
}
