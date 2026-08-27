package com.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.ai.domain.*;
import com.platform.ai.repository.*;
import com.platform.ai.web.ApiDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.math.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class ChatService {
    private final AiModelRepository models;
    private final AiRequestLogRepository logs;
    private final CryptoService crypto;
    private final ConversationService conversations;
    private final RestClient.Builder restClientBuilder;

    public ChatResponse chat(ChatRequest request) {
        AiModel model = models.findWithProviderById(request.modelId()).orElseThrow(() -> new IllegalArgumentException("Model not found"));
        if (!model.isEnabled() || !model.getProvider().isEnabled()) throw new IllegalArgumentException("Model or provider is disabled");
        AiConversation conversation = conversations.resolve(request.conversationId(), model);
        Message latestMessage = request.messages().getLast();
        conversations.append(conversation, latestMessage.role(), latestMessage.content(), null, 0, 0);
        String traceId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        AiRequestLog log = new AiRequestLog(); log.setTraceId(traceId); log.setModel(model);
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model.getCode()); body.put("messages", request.messages());
            if (request.temperature() != null) body.put("temperature", request.temperature());
            RestClient client = restClientBuilder.baseUrl(model.getProvider().getBaseUrl()).build();
            var spec = client.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON);
            String apiKey = crypto.decrypt(model.getProvider().getApiKeyCiphertext());
            if (apiKey != null) spec.header("Authorization", "Bearer " + apiKey);
            JsonNode response = spec.body(body).retrieve().body(JsonNode.class);
            if (response == null) throw new IllegalStateException("AI provider returned an empty response");
            String content = response.path("choices").path(0).path("message").path("content").asText();
            int input = response.path("usage").path("prompt_tokens").asInt(0);
            int output = response.path("usage").path("completion_tokens").asInt(0);
            long duration = elapsed(started);
            log.setSuccess(true); log.setDurationMs(duration); log.setInputTokens(input); log.setOutputTokens(output);
            log.setEstimatedCost(cost(model, input, output)); logs.save(log);
            conversations.append(conversation, "assistant", content, traceId, input, output);
            return new ChatResponse(traceId, content, input, output, duration, conversation.getId());
        } catch (Exception e) {
            log.setSuccess(false); log.setDurationMs(elapsed(started)); log.setErrorMessage(limit(e.getMessage())); logs.save(log);
            throw new IllegalStateException("AI call failed, traceId=" + traceId + ": " + e.getMessage(), e);
        }
    }

    private static long elapsed(long started) { return (System.nanoTime() - started) / 1_000_000; }
    private static String limit(String value) { if (value == null) return null; return value.length() > 1000 ? value.substring(0, 1000) : value; }
    private static BigDecimal cost(AiModel m, int input, int output) {
        return m.getInputPricePerMillion().multiply(BigDecimal.valueOf(input))
                .add(m.getOutputPricePerMillion().multiply(BigDecimal.valueOf(output)))
                .divide(BigDecimal.valueOf(1_000_000), 8, RoundingMode.HALF_UP);
    }
}
