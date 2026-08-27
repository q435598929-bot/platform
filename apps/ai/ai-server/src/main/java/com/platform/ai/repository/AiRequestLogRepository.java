package com.platform.ai.repository;

import com.platform.ai.domain.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {
    interface StatsAggregate {
        Long getTotalCalls();
        Long getSuccessfulCalls();
        Long getInputTokens();
        Long getOutputTokens();
        Double getAverageDurationMs();
        BigDecimal getEstimatedCost();
    }

    long countBySuccessTrue();
    @Query("select coalesce(sum(r.inputTokens),0) from AiRequestLog r") long sumInputTokens();
    @Query("select coalesce(sum(r.outputTokens),0) from AiRequestLog r") long sumOutputTokens();
    @Query("select coalesce(sum(r.estimatedCost),0) from AiRequestLog r") BigDecimal sumEstimatedCost();
    @Query("select coalesce(avg(r.durationMs),0) from AiRequestLog r") double averageDurationMs();

    @Query("""
            select count(r) as totalCalls,
                   coalesce(sum(case when r.success = true then 1 else 0 end), 0) as successfulCalls,
                   coalesce(sum(r.inputTokens), 0) as inputTokens,
                   coalesce(sum(r.outputTokens), 0) as outputTokens,
                   coalesce(avg(r.durationMs), 0) as averageDurationMs,
                   coalesce(sum(r.estimatedCost), 0) as estimatedCost
              from AiRequestLog r
             where (:providerId is null or r.model.provider.id = :providerId)
               and (:modelId is null or r.model.id = :modelId)
            """)
    StatsAggregate aggregate(@Param("providerId") Long providerId, @Param("modelId") Long modelId);
}
