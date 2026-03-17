package com.winekiosk.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.winekiosk.dto.ChatRequest;
import com.winekiosk.dto.ChatResponse;
import com.winekiosk.dto.WineFilterRequest;
import com.winekiosk.model.ChatLog;
import com.winekiosk.model.Wine;
import com.winekiosk.repository.ChatLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final LangdockService langdockService;
    private final WineService wineService;
    private final ChatLogRepository chatLogRepository;
    private final ObjectMapper objectMapper;

    public ChatService(LangdockService langdockService,
                       WineService wineService,
                       ChatLogRepository chatLogRepository) {
        this.langdockService = langdockService;
        this.wineService = wineService;
        this.chatLogRepository = chatLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    public ChatResponse processChat(ChatRequest chatRequest) {
        String userMessage = chatRequest.getMessage();

        // Step 1: Extract search parameters from user message
        String paramsJson = langdockService.extractSearchParameters(userMessage);
        WineFilterRequest filterRequest = parseFilterRequest(paramsJson);

        // Step 2: Query database with extracted parameters
        List<Wine> matchedWines = wineService.filterWines(filterRequest);

        // Fall back to all wines if nothing matched
        if (matchedWines.isEmpty()) {
            matchedWines = wineService.getAllWines();
        }

        // Limit to top 10 wines for context injection
        List<Wine> winesForContext = matchedWines.stream().limit(10).collect(Collectors.toList());

        // Step 3: Build sommelier system prompt with inventory
        String systemPrompt = buildSommelierPrompt(winesForContext);

        // Step 4: Call AI for natural language recommendation
        String aiReply = langdockService.chat(systemPrompt, userMessage);

        // Step 5: Persist chat log
        saveChatLog(userMessage, aiReply);

        return new ChatResponse(aiReply, winesForContext);
    }

    private WineFilterRequest parseFilterRequest(String paramsJson) {
        WineFilterRequest request = new WineFilterRequest();
        try {
            Map<String, Object> params = objectMapper.readValue(paramsJson,
                    new TypeReference<Map<String, Object>>() {});

            request.setWineType(getStringParam(params, "wineType"));
            request.setSweetness(getStringParam(params, "sweetness"));
            request.setGrapeVariety(getStringParam(params, "grapeVariety"));
            request.setCountry(getStringParam(params, "country"));
            request.setRegion(getStringParam(params, "region"));
            request.setBody(getStringParam(params, "body"));

            Object maxPriceObj = params.get("maxPrice");
            if (maxPriceObj instanceof Number) {
                request.setMaxPrice(BigDecimal.valueOf(((Number) maxPriceObj).doubleValue()));
            }

            String foodPairing = getStringParam(params, "foodPairing");
            if (foodPairing != null) {
                request.setSearchText(foodPairing);
            }

            String flavorKeywords = getStringParam(params, "flavorKeywords");
            if (flavorKeywords != null && request.getSearchText() == null) {
                request.setSearchText(flavorKeywords);
            }

        } catch (Exception e) {
            log.warn("Could not parse search parameters from JSON: {}", paramsJson);
        }
        return request;
    }

    private String buildSommelierPrompt(List<Wine> wines) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are Vinothek AI, a warm and knowledgeable professional wine sommelier working in a wine store.
                Your task is to recommend wines to customers from the store's inventory.
                
                RULES:
                - ONLY recommend wines from the provided inventory list below — NEVER invent or mention wines not in this list.
                - Be warm, knowledgeable, and conversational in your responses.
                - Explain WHY each wine matches the customer's request.
                - Mention flavor profiles, food pairings, and value.
                - If no perfect match exists, suggest the closest alternatives from the inventory.
                - Keep responses concise (2-4 paragraphs).
                - Format wine names in **bold**.
                - Number multiple recommendations.
                
                CURRENT INVENTORY:
                """);

        for (Wine wine : wines) {
            sb.append("- ").append(wine.toSummary()).append("\n");
        }

        sb.append("""
                
                Use ONLY these wines in your recommendations. Do not mention any other wines.
                """);
        return sb.toString();
    }

    private void saveChatLog(String userMessage, String aiResponse) {
        try {
            ChatLog log = new ChatLog(userMessage, aiResponse, LocalDateTime.now());
            chatLogRepository.save(log);
        } catch (Exception e) {
            this.log.warn("Could not save chat log: {}", e.getMessage());
        }
    }

    private String getStringParam(Map<String, Object> params, String key) {
        Object val = params.get(key);
        if (val instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }
}
