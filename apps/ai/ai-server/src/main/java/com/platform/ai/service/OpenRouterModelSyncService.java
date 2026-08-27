package com.platform.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.platform.ai.domain.AiModel;
import com.platform.ai.domain.AiProvider;
import com.platform.ai.repository.AiModelRepository;
import com.platform.ai.repository.AiProviderRepository;
import com.platform.ai.web.ApiDtos.ModelSyncResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenRouterModelSyncService {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    private final AiProviderRepository providers;
    private final AiModelRepository models;
    private final CryptoService crypto;
    private final RestClient.Builder restClientBuilder;

    @Transactional
    public ModelSyncResponse sync(Long providerId) {
        AiProvider provider = providers.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        verifyOpenRouter(provider.getBaseUrl());

        var request = restClientBuilder.baseUrl(provider.getBaseUrl()).build().get().uri("/models");
        String apiKey = crypto.decrypt(provider.getApiKeyCiphertext());
        if (apiKey != null && !apiKey.isBlank()) request.header("Authorization", "Bearer " + apiKey);
        JsonNode response = request.retrieve().body(JsonNode.class);
        if (response == null || !response.path("data").isArray()) {
            throw new IllegalStateException("OpenRouter returned an invalid models response");
        }

        Map<String, AiModel> existing = new LinkedHashMap<>();
        models.findAllByProvider_IdOrderBySortOrderAscIdAsc(providerId).forEach(model -> existing.put(model.getCode(), model));
        int created = 0;
        int updated = 0;
        for (JsonNode remote : response.path("data")) {
            String code = remote.path("id").asText("").trim();
            if (code.isEmpty()) continue;
            AiModel model = existing.get(code);
            if (model == null) {
                model = new AiModel();
                model.setProvider(provider);
                model.setCode(code);
                model.setEnabled(isFree(code));
                created++;
            } else {
                updated++;
            }
            String name = remote.path("name").asText(code).trim();
            model.setDisplayName(name.isEmpty() ? code : name);
            model.setCanonicalSlug(textOrNull(remote.path("canonical_slug")));
            long createdAt = remote.path("created").asLong(0);
            model.setRemoteCreatedAt(createdAt > 0 ? LocalDateTime.ofInstant(Instant.ofEpochSecond(createdAt), ZoneOffset.UTC) : null);
            model.setExpirationDate(textOrNull(remote.path("expiration_date")));
            model.setKnowledgeCutoff(textOrNull(remote.path("knowledge_cutoff")));
            model.setInputPricePerMillion(perMillion(remote.path("pricing").path("prompt")));
            model.setOutputPricePerMillion(perMillion(remote.path("pricing").path("completion")));
            models.save(model);
        }
        return new ModelSyncResponse(created + updated, created, updated);
    }

    private static BigDecimal perMillion(JsonNode price) {
        if (!price.isValueNode()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(price.asText()).multiply(ONE_MILLION).setScale(6, RoundingMode.HALF_UP);
        } catch (NumberFormatException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static boolean isFree(String code) {
        return "openrouter/free".equals(code) || code.endsWith(":free");
    }

    private static String textOrNull(JsonNode value) {
        if (value.isNull() || value.isMissingNode()) return null;
        String text = value.asText("").trim();
        return text.isEmpty() ? null : text;
    }

    private static void verifyOpenRouter(String baseUrl) {
        URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid OpenRouter Base URL", e);
        }
        String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"openrouter.ai".equalsIgnoreCase(uri.getHost())
                || !"/api/v1".equals(path)) {
            throw new IllegalArgumentException("OpenRouter Base URL must be https://openrouter.ai/api/v1");
        }
    }
}
