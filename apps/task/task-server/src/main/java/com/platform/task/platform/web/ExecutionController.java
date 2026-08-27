package com.platform.task.platform.web;

import com.platform.task.platform.service.TaskManagementService;
import com.platform.task.platform.web.TaskDtos.ExecutionResponse;
import com.platform.task.platform.web.TaskDtos.LogResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionController {
    private final TaskManagementService service;
    public ExecutionController(TaskManagementService service) { this.service = service; }
    @GetMapping public List<ExecutionResponse> executions() { return service.executions(); }
    @GetMapping("/{id}/logs") public List<LogResponse> logs(@PathVariable String id) { return service.logs(id); }
    @PostMapping("/{id}/query-now") public ExecutionResponse queryNow(@PathVariable String id) {
        return service.triggerNow(id);
    }
}
