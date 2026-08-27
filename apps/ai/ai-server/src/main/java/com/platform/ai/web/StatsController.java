package com.platform.ai.web;

import com.platform.ai.service.StatsService;
import com.platform.ai.web.ApiDtos.StatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/stats") @RequiredArgsConstructor
public class StatsController {
    private final StatsService service;
    @GetMapping("/overview")
    public StatsResponse overview(@RequestParam(required = false) Long providerId,
                                  @RequestParam(required = false) Long modelId) {
        return service.overview(providerId, modelId);
    }
}
