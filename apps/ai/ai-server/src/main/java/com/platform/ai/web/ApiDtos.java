package com.platform.ai.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {}
    public record ProviderRequest(@NotBlank String name, @NotBlank String baseUrl, String apiKey, boolean enabled, Integer sortOrder) {}
    public record ProviderResponse(Long id, String name, String baseUrl, boolean hasApiKey, boolean enabled, int sortOrder, LocalDateTime updatedAt) {}
    public record MoveRequest(@NotNull Long adjacentId) {}
    public record ModelRequest(@NotNull Long providerId, @NotBlank String code, @NotBlank String displayName, boolean enabled,
                               @PositiveOrZero BigDecimal inputPricePerMillion, @PositiveOrZero BigDecimal outputPricePerMillion,
                               Integer sortOrder, Boolean free) {}
    public record ModelResponse(Long id, Long providerId, String providerName, String code, String displayName, boolean enabled,
                                BigDecimal inputPricePerMillion, BigDecimal outputPricePerMillion, int sortOrder, boolean free, String canonicalSlug,
                                LocalDateTime remoteCreatedAt, String expirationDate, String knowledgeCutoff) {}
    public record ModelSyncResponse(int total, int created, int updated) {}
    public record Message(@NotBlank String role, @NotBlank String content) {}
    public record ChatRequest(@NotNull Long modelId, @NotEmpty List<@Valid Message> messages, BigDecimal temperature, Long conversationId) {}
    public record ChatResponse(String traceId, String content, int inputTokens, int outputTokens, long durationMs, Long conversationId) {}
    public record ConversationRequest(@NotNull Long modelId) {}
    public record ConversationResponse(Long id, Long modelId, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record ConversationSummaryResponse(Long id, Long modelId, String title, LocalDateTime createdAt,
                                              LocalDateTime updatedAt) {}
    public record ConversationMessageResponse(Long id, String role, String content, String traceId, int inputTokens,
                                              int outputTokens, LocalDateTime createdAt) {}
    public record StatsResponse(long totalCalls, long successfulCalls, double successRate, long inputTokens, long outputTokens,
                                double averageDurationMs, BigDecimal estimatedCost) {}
}
