package com.platform.ai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "ai_conversation_message")
public class AiConversationMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id") private AiConversation conversation;
    @Column(nullable = false, length = 30) private String role;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "trace_id", length = 36) private String traceId;
    @Column(name = "input_tokens", nullable = false) private int inputTokens;
    @Column(name = "output_tokens", nullable = false) private int outputTokens;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}
