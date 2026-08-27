package com.platform.ai.web;

import com.platform.ai.service.ConversationService;
import com.platform.ai.web.ApiDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/conversations") @RequiredArgsConstructor
public class ConversationController {
    private final ConversationService service;
    @PostMapping public ConversationResponse create(@Valid @RequestBody ConversationRequest request) { return service.create(request.modelId()); }
    @GetMapping public List<ConversationSummaryResponse> list(@RequestParam Long modelId) { return service.list(modelId); }
    @GetMapping("/latest") public ResponseEntity<ConversationResponse> latest(@RequestParam Long modelId) {
        return service.latest(modelId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
    @GetMapping("/{id}/messages") public List<ConversationMessageResponse> messages(@PathVariable Long id) { return service.messages(id); }
}
