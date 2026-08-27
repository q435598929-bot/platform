package com.platform.ai.web;

import com.platform.ai.service.ChatService;
import com.platform.ai.web.ApiDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/chat") @RequiredArgsConstructor
public class ChatController {
    private final ChatService service;
    @PostMapping("/completions") public ChatResponse chat(@Valid @RequestBody ChatRequest request) { return service.chat(request); }
}
