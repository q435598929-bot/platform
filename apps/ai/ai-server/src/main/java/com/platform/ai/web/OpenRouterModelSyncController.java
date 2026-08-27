package com.platform.ai.web;

import com.platform.ai.service.OpenRouterModelSyncService;
import com.platform.ai.web.ApiDtos.ModelSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class OpenRouterModelSyncController {
    private final OpenRouterModelSyncService service;

    @PostMapping("/{providerId}/models/sync")
    public ModelSyncResponse sync(@PathVariable Long providerId) {
        return service.sync(providerId);
    }
}
