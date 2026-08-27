package com.platform.ai.service;

import com.platform.ai.domain.*;
import com.platform.ai.repository.*;
import com.platform.ai.web.ApiDtos.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class ConversationService {
    private final AiConversationRepository conversations;
    private final AiConversationMessageRepository messages;
    private final AiModelRepository models;

    @Transactional public ConversationResponse create(Long modelId) {
        AiConversation item = new AiConversation();
        item.setModel(models.findById(modelId).orElseThrow(() -> new IllegalArgumentException("Model not found")));
        return response(conversations.save(item));
    }

    public Optional<ConversationResponse> latest(Long modelId) {
        return conversations.findFirstByModel_IdOrderByUpdatedAtDesc(modelId).map(this::response);
    }

    public List<ConversationSummaryResponse> list(Long modelId) {
        return conversations.findAllByModel_IdOrderByUpdatedAtDesc(modelId).stream().map(item -> {
            String title = messages
                    .findFirstByConversation_IdAndRoleOrderByCreatedAtAscIdAsc(item.getId(), "user")
                    .map(message -> message.getContent().replaceAll("\\s+", " ").trim())
                    .filter(value -> !value.isEmpty())
                    .orElse("新对话");
            if (title.length() > 60) title = title.substring(0, 60) + "…";
            return new ConversationSummaryResponse(item.getId(), item.getModel().getId(), title,
                    item.getCreatedAt(), item.getUpdatedAt());
        }).toList();
    }

    public List<ConversationMessageResponse> messages(Long conversationId) {
        if (!conversations.existsById(conversationId)) throw new IllegalArgumentException("Conversation not found");
        return messages.findAllByConversation_IdOrderByCreatedAtAscIdAsc(conversationId).stream()
                .map(m -> new ConversationMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getTraceId(),
                        m.getInputTokens(), m.getOutputTokens(), m.getCreatedAt())).toList();
    }

    @Transactional public AiConversation resolve(Long conversationId, AiModel model) {
        if (conversationId == null) {
            AiConversation item = new AiConversation(); item.setModel(model); return conversations.save(item);
        }
        AiConversation item = conversations.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        if (!item.getModel().getId().equals(model.getId())) throw new IllegalArgumentException("Conversation model mismatch");
        return item;
    }

    @Transactional public void append(AiConversation conversation, String role, String content, String traceId, int input, int output) {
        AiConversationMessage item = new AiConversationMessage(); item.setConversation(conversation); item.setRole(role);
        item.setContent(content); item.setTraceId(traceId); item.setInputTokens(input); item.setOutputTokens(output); messages.save(item);
        conversation.setUpdatedAt(LocalDateTime.now()); conversations.save(conversation);
    }

    @Scheduled(cron = "0 15 * * * *")
    @Transactional public void deleteExpired() {
        conversations.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(3));
    }

    private ConversationResponse response(AiConversation item) {
        return new ConversationResponse(item.getId(), item.getModel().getId(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
