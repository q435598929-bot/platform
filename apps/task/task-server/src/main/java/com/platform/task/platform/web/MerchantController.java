package com.platform.task.platform.web;

import com.platform.task.platform.service.TaskRegistryService;
import com.platform.task.platform.web.TaskDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {
    private final TaskRegistryService registry;

    public MerchantController(TaskRegistryService registry) { this.registry = registry; }

    @GetMapping public List<MerchantResponse> merchants() { return registry.merchants(); }
    @GetMapping("/configuration-fields")
    public List<MerchantConfigurationFieldResponse> configurationFields() { return registry.configurationFields(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public MerchantResponse create(@Valid @RequestBody CreateMerchantRequest request) { return registry.createMerchant(request); }
    @GetMapping("/{id}/configuration")
    public MerchantConfigurationResponse configuration(@PathVariable String id) { return registry.configuration(id); }
    @PutMapping("/{id}/configuration")
    public MerchantConfigurationResponse configuration(@PathVariable String id,
            @Valid @RequestBody UpdateMerchantConfigurationRequest request) {
        return registry.updateConfiguration(id, request);
    }
}
