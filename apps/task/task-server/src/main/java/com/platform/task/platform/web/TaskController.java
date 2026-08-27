package com.platform.task.platform.web;

import com.platform.task.platform.service.TaskManagementService;
import com.platform.task.platform.web.TaskDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskManagementService service;
    private final com.platform.task.platform.service.TaskRegistryService registry;
    public TaskController(TaskManagementService service, com.platform.task.platform.service.TaskRegistryService registry) {
        this.service = service;
        this.registry = registry;
    }

    @GetMapping public List<TaskResponse> tasks() { return service.tasks(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) { return registry.createTask(request); }
    @GetMapping("/templates") public List<TaskTemplateResponse> templates() { return registry.templates(); }
    @GetMapping("/workflows") public List<WorkflowTemplateResponse> workflows() {
        return registry.workflows();
    }
    @PutMapping("/{id}/enabled") public TaskResponse enable(@PathVariable String id, @RequestBody EnableRequest request) {
        return service.enable(id, request.enabled());
    }
    @PostMapping("/{id}/executions") @ResponseStatus(HttpStatus.ACCEPTED)
    public ExecutionResponse run(@PathVariable String id, @Valid @RequestBody RunRequest request) {
        return service.run(id, request);
    }
}
