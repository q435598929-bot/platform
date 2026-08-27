package com.platform.task.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.task.platform.domain.TaskTemplate;
import com.platform.task.platform.domain.TaskWorkflowTemplate;
import com.platform.task.platform.repository.TaskTemplateRepository;
import com.platform.task.platform.repository.TaskWorkflowTemplateRepository;
import com.platform.task.platform.web.TaskDtos.InputFieldResponse;
import com.platform.task.platform.web.TaskDtos.WorkflowStepResponse;
import com.platform.task.platform.web.TaskDtos.WorkflowTemplateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaskWorkflowService {
    private final TaskWorkflowTemplateRepository workflows;
    private final TaskTemplateRepository templates;
    private final TaskInputCatalog inputs;
    private final ObjectMapper objectMapper;

    public TaskWorkflowService(TaskWorkflowTemplateRepository workflows, TaskTemplateRepository templates,
                               TaskInputCatalog inputs, ObjectMapper objectMapper) {
        this.workflows = workflows;
        this.templates = templates;
        this.inputs = inputs;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<WorkflowTemplateResponse> workflows() {
        return workflows.findAllByOrderByCategoryAscDisplayNameAsc().stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public TaskWorkflowTemplate require(String id) {
        return workflows.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown workflow template: " + id));
    }

    public List<StepSpec> steps(TaskWorkflowTemplate workflow) {
        try {
            List<StepSpec> result = objectMapper.readValue(workflow.getStepsJson(), new TypeReference<>() {});
            if (result.isEmpty()) throw new IllegalStateException("Workflow has no steps: " + workflow.getId());
            Set<String> keys = new LinkedHashSet<>();
            for (StepSpec step : result) {
                if (step.key() == null || step.key().isBlank() || !keys.add(step.key())) {
                    throw new IllegalStateException("Workflow contains a blank or duplicate step key: " + workflow.getId());
                }
                if (!templates.existsById(step.templateTaskId())) {
                    throw new IllegalStateException("Workflow references an unknown task template: " + step.templateTaskId());
                }
            }
            return List.copyOf(result);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Stored workflow steps are invalid: " + workflow.getId(), error);
        }
    }

    public List<InputFieldResponse> inputFields(TaskWorkflowTemplate workflow) {
        Map<String, InputFieldResponse> merged = new LinkedHashMap<>();
        for (StepSpec step : steps(workflow)) {
            if (step.polling()) continue;
            for (InputFieldResponse field : inputs.fields(step.templateTaskId())) {
                InputFieldResponse visible = step.optional() ? optional(field) : field;
                merged.merge(field.key(), visible, (left, right) -> left.required() ? left : right);
            }
        }
        return List.copyOf(merged.values());
    }

    public WorkflowPreparedInputs prepare(TaskWorkflowTemplate workflow, Map<String, String> supplied,
                                          Map<String, String> defaults) {
        Map<String, String> source = supplied == null ? Map.of() : supplied;
        Set<String> allowed = inputFields(workflow).stream().map(InputFieldResponse::key)
                .collect(java.util.stream.Collectors.toSet());
        source.keySet().stream().filter(key -> !allowed.contains(key)).findFirst()
                .ifPresent(key -> { throw new IllegalArgumentException("Unsupported workflow input: " + key); });

        Map<String, String> runtime = new LinkedHashMap<>();
        Map<String, String> stored = new LinkedHashMap<>();
        Map<String, String> effective = new LinkedHashMap<>();
        if (defaults != null) effective.putAll(defaults);
        effective.putAll(source);
        for (StepSpec step : steps(workflow)) {
            if (step.polling()) continue;
            Map<String, String> stepInputs = inputs.supportedInputs(step.templateTaskId(), effective);
            if (step.optional() && stepInputs.isEmpty()) continue;
            TaskInputCatalog.PreparedInputs prepared = inputs.prepare(step.templateTaskId(), stepInputs, List.of());
            runtime.putAll(prepared.runtimeInputs());
            prepared.storedInputs().forEach((key, value) -> {
                if (source.containsKey(key)) stored.put(key, value);
            });
        }
        return new WorkflowPreparedInputs(Map.copyOf(runtime), Map.copyOf(stored));
    }

    public TaskTemplate primaryTemplate(TaskWorkflowTemplate workflow) {
        StepSpec primary = steps(workflow).stream().filter(step -> !step.optional() && !step.polling()).findFirst()
                .orElseThrow(() -> new IllegalStateException("Workflow has no primary action: " + workflow.getId()));
        return templates.findById(primary.templateTaskId()).orElseThrow();
    }

    private WorkflowTemplateResponse response(TaskWorkflowTemplate workflow) {
        List<StepSpec> specs = steps(workflow);
        Map<String, TaskTemplate> byId = new LinkedHashMap<>();
        templates.findAllById(specs.stream().map(StepSpec::templateTaskId).toList())
                .forEach(template -> byId.put(template.getId(), template));
        List<WorkflowStepResponse> stepResponses = new ArrayList<>();
        for (int index = 0; index < specs.size(); index++) {
            StepSpec step = specs.get(index);
            TaskTemplate template = byId.get(step.templateTaskId());
            stepResponses.add(new WorkflowStepResponse(step.key(), index, step.templateTaskId(),
                    template == null ? step.templateTaskId() : template.getDisplayName(), step.optional(),
                    step.polling(), step.intervalSeconds(), step.maxAttempts()));
        }
        return new WorkflowTemplateResponse(workflow.getId(), workflow.getDisplayName(), workflow.getDescription(),
                workflow.getCategory(), workflow.getVersion(), workflow.isDangerous(), stepResponses,
                inputFields(workflow));
    }

    private static InputFieldResponse optional(InputFieldResponse field) {
        return new InputFieldResponse(field.key(), field.label(), field.type(), false, field.placeholder(),
                field.description(), field.defaultValue(), field.options(), field.secret());
    }

    public record StepSpec(String key, String templateTaskId, boolean optional, boolean polling,
                           int intervalSeconds, int maxAttempts) {
        public int effectiveIntervalSeconds() { return intervalSeconds <= 0 ? 30 : intervalSeconds; }
        public int effectiveMaxAttempts() { return maxAttempts <= 0 ? 120 : maxAttempts; }
    }
    public record WorkflowPreparedInputs(Map<String, String> runtimeInputs, Map<String, String> storedInputs) {}
}
