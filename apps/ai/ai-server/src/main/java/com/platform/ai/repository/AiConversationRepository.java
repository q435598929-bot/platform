package com.platform.ai.repository;

import com.platform.ai.domain.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findAllByModel_IdOrderByUpdatedAtDesc(Long modelId);
    Optional<AiConversation> findFirstByModel_IdOrderByUpdatedAtDesc(Long modelId);
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
