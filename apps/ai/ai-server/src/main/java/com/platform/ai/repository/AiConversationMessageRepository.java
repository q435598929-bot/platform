package com.platform.ai.repository;

import com.platform.ai.domain.AiConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, Long> {
    List<AiConversationMessage> findAllByConversation_IdOrderByCreatedAtAscIdAsc(Long conversationId);
    Optional<AiConversationMessage> findFirstByConversation_IdAndRoleOrderByCreatedAtAscIdAsc(Long conversationId, String role);
}
