package com.platform.ai.web;

import com.platform.ai.service.CatalogService;
import com.platform.ai.web.ApiDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class CatalogController {
    private final CatalogService service;
    @GetMapping("/providers") public List<ProviderResponse> providers() { return service.providers(); }
    @PostMapping("/providers") @ResponseStatus(HttpStatus.CREATED) public ProviderResponse createProvider(@Valid @RequestBody ProviderRequest r) { return service.saveProvider(null, r); }
    @PutMapping("/providers/{id}") public ProviderResponse updateProvider(@PathVariable Long id, @Valid @RequestBody ProviderRequest r) { return service.saveProvider(id, r); }
    @PostMapping("/providers/{id}/move") @ResponseStatus(HttpStatus.NO_CONTENT) public void moveProvider(@PathVariable Long id, @Valid @RequestBody MoveRequest r) { service.moveProvider(id, r.adjacentId()); }
    @DeleteMapping("/providers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteProvider(@PathVariable Long id) { service.deleteProvider(id); }
    @GetMapping("/models") public List<ModelResponse> models() { return service.models(); }
    @PostMapping("/models") @ResponseStatus(HttpStatus.CREATED) public ModelResponse createModel(@Valid @RequestBody ModelRequest r) { return service.saveModel(null, r); }
    @PutMapping("/models/{id}") public ModelResponse updateModel(@PathVariable Long id, @Valid @RequestBody ModelRequest r) { return service.saveModel(id, r); }
    @PostMapping("/models/{id}/move") @ResponseStatus(HttpStatus.NO_CONTENT) public void moveModel(@PathVariable Long id, @Valid @RequestBody MoveRequest r) { service.moveModel(id, r.adjacentId()); }
    @DeleteMapping("/models/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteModel(@PathVariable Long id) { service.deleteModel(id); }
}
