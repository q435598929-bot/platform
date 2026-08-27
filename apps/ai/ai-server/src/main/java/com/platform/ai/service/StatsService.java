package com.platform.ai.service;

import com.platform.ai.repository.AiRequestLogRepository;
import com.platform.ai.web.ApiDtos.StatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class StatsService {
    private final AiRequestLogRepository logs;
    public StatsResponse overview() {
        return overview(null, null);
    }

    public StatsResponse overview(Long providerId, Long modelId) {
        if (providerId != null && modelId != null) {
            throw new IllegalArgumentException("providerId and modelId cannot be used together");
        }
        var stats = logs.aggregate(providerId, modelId);
        long total = stats.getTotalCalls(), success = stats.getSuccessfulCalls();
        double rate = total == 0 ? 0 : success * 100.0 / total;
        return new StatsResponse(total, success, rate, stats.getInputTokens(), stats.getOutputTokens(),
                stats.getAverageDurationMs(), stats.getEstimatedCost());
    }
}
