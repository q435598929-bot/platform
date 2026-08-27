package com.platform.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "ai_provider")
public class AiProvider {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    @Column(name = "base_url", nullable = false, length = 500) private String baseUrl;
    @Column(name = "api_key_ciphertext", columnDefinition = "TEXT") private String apiKeyCiphertext;
    @Column(nullable = false) private boolean enabled = true;
    @Column(name = "sort_order", nullable = false) private int sortOrder = 0;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
