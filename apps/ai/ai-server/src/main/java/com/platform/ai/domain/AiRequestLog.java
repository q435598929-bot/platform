package com.platform.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "ai_request_log")
public class AiRequestLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "trace_id", nullable = false, unique = true, length = 36) private String traceId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "model_id") private AiModel model;
    @Column(nullable = false) private boolean success;
    @Column(name = "duration_ms", nullable = false) private long durationMs;
    @Column(name = "input_tokens", nullable = false) private int inputTokens;
    @Column(name = "output_tokens", nullable = false) private int outputTokens;
    @Column(name = "estimated_cost", nullable = false, precision = 18, scale = 8) private BigDecimal estimatedCost = BigDecimal.ZERO;
    @Column(name = "error_message", length = 1000) private String errorMessage;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}
