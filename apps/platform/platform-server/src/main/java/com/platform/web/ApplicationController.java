package com.platform.web;

import com.platform.domain.ApplicationView;
import com.platform.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;
    public ApplicationController(ApplicationService service) { this.service = service; }

    @GetMapping public List<ApplicationView> list() { return service.list(); }
    @GetMapping("/{id}") public ApplicationView get(@PathVariable String id) { return service.get(id); }
    @PostMapping("/{id}/start") public ApplicationView start(@PathVariable String id) { return service.start(id); }
    @PostMapping("/{id}/stop") public ApplicationView stop(@PathVariable String id) { return service.stop(id); }
    @PostMapping("/reload") public Map<String, Boolean> reload() { service.reload(); return Map.of("reloaded", true); }
}
