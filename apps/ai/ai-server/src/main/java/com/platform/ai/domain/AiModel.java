package com.platform.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "ai_model")
public class AiModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "provider_id") private AiProvider provider;
    @Column(nullable = false, length = 150) private String code;
    @Column(name = "display_name", nullable = false, length = 150) private String displayName;
    @Column(name = "canonical_slug", length = 200) private String canonicalSlug;
    @Column(name = "remote_created_at") private LocalDateTime remoteCreatedAt;
    @Column(name = "expiration_date", length = 64) private String expirationDate;
    @Column(name = "knowledge_cutoff", length = 100) private String knowledgeCutoff;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "sort_order", nullable = false) private int sortOrder = 0;
    @Column(name = "is_free", nullable = false) private boolean free = true;
    @Column(name = "input_price_per_million", nullable = false, precision = 18, scale = 6) private BigDecimal inputPricePerMillion = BigDecimal.ZERO;
    @Column(name = "output_price_per_million", nullable = false, precision = 18, scale = 6) private BigDecimal outputPricePerMillion = BigDecimal.ZERO;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
