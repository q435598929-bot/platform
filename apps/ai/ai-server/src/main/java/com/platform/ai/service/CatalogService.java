package com.platform.ai.service;

import com.platform.ai.domain.*;
import com.platform.ai.repository.*;
import com.platform.ai.web.ApiDtos.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Service @RequiredArgsConstructor
public class CatalogService {
    private final AiProviderRepository providers;
    private final AiModelRepository models;
    private final CryptoService crypto;

    public List<ProviderResponse> providers() { return providers.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toResponse).toList(); }
    @Transactional public ProviderResponse saveProvider(Long id, ProviderRequest request) {
        AiProvider item = id == null ? new AiProvider() : providers.findById(id).orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        item.setName(request.name()); item.setBaseUrl(stripTrailingSlash(request.baseUrl())); item.setEnabled(request.enabled());
        if (request.sortOrder() != null) item.setSortOrder(request.sortOrder());
        if (request.apiKey() != null && !request.apiKey().isBlank()) item.setApiKeyCiphertext(crypto.encrypt(request.apiKey()));
        return toResponse(providers.save(item));
    }
    public void deleteProvider(Long id) { providers.deleteById(id); }
    @Transactional public void moveProvider(Long id, Long adjacentId) {
        List<AiProvider> items = providers.findAllByOrderBySortOrderAscIdAsc();
        int current = providerIndex(items, id);
        int adjacent = providerIndex(items, adjacentId);
        Collections.swap(items, current, adjacent);
        for (int i = 0; i < items.size(); i++) items.get(i).setSortOrder(i * 10);
        providers.saveAll(items);
    }
    public List<ModelResponse> models() { return models.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toResponse).toList(); }
    @Transactional public ModelResponse saveModel(Long id, ModelRequest request) {
        AiModel item = id == null ? new AiModel() : models.findById(id).orElseThrow(() -> new IllegalArgumentException("Model not found"));
        item.setProvider(providers.findById(request.providerId()).orElseThrow(() -> new IllegalArgumentException("Provider not found")));
        item.setCode(request.code()); item.setDisplayName(request.displayName()); item.setEnabled(request.enabled());
        if (request.sortOrder() != null) item.setSortOrder(request.sortOrder());
        if (request.free() != null) item.setFree(request.free());
        item.setInputPricePerMillion(zero(request.inputPricePerMillion())); item.setOutputPricePerMillion(zero(request.outputPricePerMillion()));
        return toResponse(models.save(item));
    }
    public void deleteModel(Long id) { models.deleteById(id); }
    @Transactional public void moveModel(Long id, Long adjacentId) {
        List<AiModel> items = models.findAllByOrderBySortOrderAscIdAsc();
        int current = modelIndex(items, id);
        int adjacent = modelIndex(items, adjacentId);
        Collections.swap(items, current, adjacent);
        for (int i = 0; i < items.size(); i++) items.get(i).setSortOrder(i * 10);
        models.saveAll(items);
    }
    private ProviderResponse toResponse(AiProvider p) { return new ProviderResponse(p.getId(), p.getName(), p.getBaseUrl(), p.getApiKeyCiphertext() != null, p.isEnabled(), p.getSortOrder(), p.getUpdatedAt()); }
    private ModelResponse toResponse(AiModel m) { return new ModelResponse(m.getId(), m.getProvider().getId(), m.getProvider().getName(), m.getCode(), m.getDisplayName(), m.isEnabled(), m.getInputPricePerMillion(), m.getOutputPricePerMillion(), m.getSortOrder(), m.isFree(), m.getCanonicalSlug(), m.getRemoteCreatedAt(), m.getExpirationDate(), m.getKnowledgeCutoff()); }
    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private static int providerIndex(List<AiProvider> items, Long id) {
        for (int i = 0; i < items.size(); i++) if (items.get(i).getId().equals(id)) return i;
        throw new IllegalArgumentException("Provider not found");
    }
    private static int modelIndex(List<AiModel> items, Long id) {
        for (int i = 0; i < items.size(); i++) if (items.get(i).getId().equals(id)) return i;
        throw new IllegalArgumentException("Model not found");
    }
    private static String stripTrailingSlash(String url) { return url.replaceAll("/+$", ""); }
}
