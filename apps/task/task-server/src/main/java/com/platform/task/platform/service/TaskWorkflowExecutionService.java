package com.platform.task.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.task.controller.util.TaskExecutionContext;
import com.platform.task.platform.domain.MerchantProfile;
import com.platform.task.platform.domain.TaskDefinition;
import com.platform.task.platform.domain.TaskExecution;
import com.platform.task.platform.domain.TaskExecutionLog;
import com.platform.task.platform.domain.TaskStepExecution;
import com.platform.task.platform.domain.TaskTemplate;
import com.platform.task.platform.repository.TaskExecutionLogRepository;
import com.platform.task.platform.repository.TaskExecutionRepository;
import com.platform.task.platform.repository.TaskStepExecutionRepository;
import com.platform.task.platform.repository.TaskTemplateRepository;
import com.platform.task.platform.web.TaskDtos.RunRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TaskWorkflowExecutionService {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");
    private final TaskExecutionRepository executions;
    private final TaskStepExecutionRepository steps;
    private final TaskExecutionLogRepository logs;
    private final TaskTemplateRepository templates;
    private final TaskWorkflowService workflows;
    private final TaskInputCatalog inputCatalog;
    private final TaskCompatibilityCatalog catalog;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> activeExecution = new AtomicReference<>();
    private final Map<String, Map<String, String>> pendingInputs = new ConcurrentHashMap<>();

    public TaskWorkflowExecutionService(TaskExecutionRepository executions, TaskStepExecutionRepository steps,
                                        TaskExecutionLogRepository logs, TaskTemplateRepository templates,
                                        TaskWorkflowService workflows, TaskInputCatalog inputCatalog,
                                        TaskCompatibilityCatalog catalog, ObjectMapper objectMapper) {
        this.executions = executions;
        this.steps = steps;
        this.logs = logs;
        this.templates = templates;
        this.workflows = workflows;
        this.inputCatalog = inputCatalog;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TaskExecution run(TaskDefinition task, RunRequest request) {
        var workflow = task.getWorkflowTemplate();
        if (workflow == null) throw new IllegalArgumentException("Task does not use a workflow template");
        if (request.arguments() != null && request.arguments().stream().anyMatch(value -> value != null && !value.isBlank())) {
            throw new IllegalArgumentException("Workflow tasks do not accept legacy main() arguments");
        }
        Map<String, String> merchantInputs = merchantInputs(task.getMerchant());
        TaskWorkflowService.WorkflowPreparedInputs prepared = workflows.prepare(workflow, request.inputs(), merchantInputs);
        String executionId = UUID.randomUUID().toString();
        if (!activeExecution.compareAndSet(null, executionId)) {
            throw new IllegalStateException("Another workflow execution is active: " + activeExecution.get());
        }

        TaskExecution execution = new TaskExecution();
        execution.setId(executionId);
        execution.setTask(task);
        execution.setStatus(TaskExecution.Status.QUEUED);
        execution.setArgumentsJson("[]");
        execution.setInputsJson(writeJson(prepared.storedInputs()));
        execution.setExecutionType("WORKFLOW");
        execution.setCurrentStepIndex(0);
        execution.setContextJson("{}");
        execution.setTriggerSource("WEB_API");
        execution.setConfirmed(request.confirmed());
        execution.setRequestedAt(LocalDateTime.now());
        executions.save(execution);

        List<TaskWorkflowService.StepSpec> specs = workflows.steps(workflow);
        for (int index = 0; index < specs.size(); index++) {
            TaskWorkflowService.StepSpec spec = specs.get(index);
            TaskStepExecution step = new TaskStepExecution();
            step.setId(UUID.randomUUID().toString());
            step.setExecution(execution);
            step.setStepIndex(index);
            step.setStepKey(spec.key());
            step.setTemplateTaskId(spec.templateTaskId());
            step.setStatus(TaskStepExecution.Status.QUEUED);
            step.setAttemptCount(0);
            step.setInputsJson("{}");
            step.setOutputsJson("{}");
            steps.save(step);
        }

        Map<String, String> runtime = new LinkedHashMap<>(merchantInputs);
        runtime.putAll(prepared.runtimeInputs());
        pendingInputs.put(executionId, Map.copyOf(runtime));
        append(execution, "INFO", "Workflow queued: " + workflow.getDisplayName());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { startClaimed(executionId); }
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) release(executionId);
            }
        });
        return execution;
    }

    @Transactional
    public TaskExecution triggerNow(String executionId) {
        TaskExecution execution = executions.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown execution: " + executionId));
        if (!"WORKFLOW".equals(execution.getExecutionType()) || execution.getStatus() != TaskExecution.Status.WAITING) {
            throw new IllegalStateException("Only a waiting workflow execution can be queried manually");
        }
        LocalDateTime now = LocalDateTime.now();
        execution.setNextRunAt(now);
        steps.findByExecutionIdAndStepIndex(executionId, execution.getCurrentStepIndex()).ifPresent(step -> {
            step.setNextRunAt(now);
            steps.save(step);
        });
        append(execution, "INFO", "Manual application query requested");
        executions.save(execution);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { tryStart(executionId); }
        });
        return execution;
    }

    @Scheduled(fixedDelay = 5_000)
    public void resumeDueWorkflows() {
        if (activeExecution.get() != null) return;
        List<TaskExecution> due = executions.findTop20ByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
                TaskExecution.Status.WAITING, LocalDateTime.now());
        if (!due.isEmpty()) tryStart(due.getFirst().getId());
    }

    private void tryStart(String executionId) {
        if (activeExecution.compareAndSet(null, executionId)) startClaimed(executionId);
    }

    private void startClaimed(String executionId) {
        Thread.ofVirtual().name("task-workflow-" + executionId).start(() -> execute(executionId));
    }

    void execute(String executionId) {
        TaskExecution execution = null;
        boolean terminal = false;
        try {
            execution = executions.findById(executionId).orElseThrow();
            var workflow = execution.getTask().getWorkflowTemplate();
            List<TaskWorkflowService.StepSpec> specs = workflows.steps(workflow);
            Map<String, Object> workflowContext = readObjectMap(execution.getContextJson());
            Map<String, String> persistedInputs = readStringMap(execution.getInputsJson());
            persistedInputs.values().removeIf("******"::equals);
            Map<String, String> initialRuntime = pendingInputs.remove(executionId);

            execution.setStatus(TaskExecution.Status.RUNNING);
            execution.setNextRunAt(null);
            if (execution.getStartedAt() == null) execution.setStartedAt(LocalDateTime.now());
            executions.save(execution);
            append(execution, "INFO", "Workflow resumed at step " + (execution.getCurrentStepIndex() + 1));

            while (execution.getCurrentStepIndex() < specs.size()) {
                int index = execution.getCurrentStepIndex();
                TaskWorkflowService.StepSpec spec = specs.get(index);
                TaskStepExecution step = steps.findByExecutionIdAndStepIndex(executionId, index).orElseThrow();
                Map<String, String> supplied = new LinkedHashMap<>(persistedInputs);
                if (initialRuntime != null) supplied.putAll(initialRuntime);
                supplied.putAll(flatten(workflowContext));

                if (spec.optional() && !inputCatalog.hasAnyInput(spec.templateTaskId(), supplied)) {
                    step.setStatus(TaskStepExecution.Status.SKIPPED);
                    step.setFinishedAt(LocalDateTime.now());
                    steps.save(step);
                    append(execution, "INFO", "Optional step skipped: " + spec.key());
                    execution.setCurrentStepIndex(index + 1);
                    executions.save(execution);
                    continue;
                }

                Map<String, String> resolved = resolve(inputCatalog.supportedInputs(spec.templateTaskId(), supplied), supplied);
                TaskInputCatalog.PreparedInputs prepared = inputCatalog.prepare(spec.templateTaskId(), resolved, List.of());
                Map<String, String> runtime = merchantInputs(execution.getTask().getMerchant());
                runtime.putAll(supplied);
                runtime.putAll(prepared.runtimeInputs());

                step.setStatus(TaskStepExecution.Status.RUNNING);
                step.setAttemptCount(step.getAttemptCount() + 1);
                step.setStartedAt(step.getStartedAt() == null ? LocalDateTime.now() : step.getStartedAt());
                step.setNextRunAt(null);
                step.setInputsJson(writeJson(prepared.storedInputs()));
                steps.save(step);
                append(execution, "INFO", "Step started: " + spec.key() + " (attempt " + step.getAttemptCount() + ")");

                TaskTemplate template = templates.findById(spec.templateTaskId()).orElseThrow();
                Map<String, Object> output;
                try (TaskExecutionContext context = TaskExecutionContext.open(runtime)) {
                    Method main = catalog.requireMain(template.getClassName());
                    main.invoke(null, (Object) prepared.arguments().toArray(String[]::new));
                    output = context.output();
                }
                step.setOutputsJson(writeJson(output));
                mergeOutput(workflowContext, spec.key(), output);
                execution.setContextJson(writeJson(workflowContext));

                if (spec.polling() && "P".equalsIgnoreCase(findString(output, "apply_status"))) {
                    if (step.getAttemptCount() >= spec.effectiveMaxAttempts()) {
                        throw new IllegalStateException("Application query exceeded max attempts: " + spec.effectiveMaxAttempts());
                    }
                    LocalDateTime next = LocalDateTime.now().plusSeconds(spec.effectiveIntervalSeconds());
                    step.setStatus(TaskStepExecution.Status.WAITING);
                    step.setNextRunAt(next);
                    steps.save(step);
                    execution.setStatus(TaskExecution.Status.WAITING);
                    execution.setNextRunAt(next);
                    executions.save(execution);
                    append(execution, "INFO", "Application is processing; next query at " + next);
                    return;
                }

                step.setStatus(TaskStepExecution.Status.SUCCEEDED);
                step.setFinishedAt(LocalDateTime.now());
                step.setNextRunAt(null);
                steps.save(step);
                append(execution, "INFO", "Step completed: " + spec.key());
                execution.setCurrentStepIndex(index + 1);
                executions.save(execution);
            }

            execution.setStatus(TaskExecution.Status.SUCCEEDED);
            append(execution, "INFO", "Workflow completed successfully");
            terminal = true;
        } catch (Throwable throwable) {
            Throwable cause = throwable instanceof InvocationTargetException && throwable.getCause() != null
                    ? throwable.getCause() : throwable;
            if (execution != null) {
                execution.setStatus(TaskExecution.Status.FAILED);
                execution.setErrorMessage(limit(cause.getClass().getSimpleName() + ": " + cause.getMessage(), 2000));
                String errorMessage = execution.getErrorMessage();
                steps.findByExecutionIdAndStepIndex(executionId, execution.getCurrentStepIndex()).ifPresent(step -> {
                    step.setStatus(TaskStepExecution.Status.FAILED);
                    step.setErrorMessage(errorMessage);
                    step.setFinishedAt(LocalDateTime.now());
                    steps.save(step);
                });
                append(execution, "ERROR", execution.getErrorMessage());
                terminal = true;
            }
        } finally {
            try {
                if (execution != null && terminal) {
                    execution.setFinishedAt(LocalDateTime.now());
                    execution.setNextRunAt(null);
                    executions.save(execution);
                }
            } finally {
                release(executionId);
            }
        }
    }

    private Map<String, String> merchantInputs(MerchantProfile merchant) {
        Map<String, String> result = new LinkedHashMap<>();
        if (merchant == null) return result;
        Map<String, String> configuration = readStringMap(merchant.getConfigurationJson());
        configuration.forEach((key, value) -> {
            result.put(key, value);
            result.put("merchant." + key, value);
        });
        result.put("merchant.code", merchant.getCode());
        result.put("merchant.name", merchant.getName());
        return result;
    }

    private void mergeOutput(Map<String, Object> workflowContext, String stepKey, Map<String, Object> output) {
        @SuppressWarnings("unchecked")
        Map<String, Object> stepOutputs = (Map<String, Object>) workflowContext.computeIfAbsent("steps", key -> new LinkedHashMap<>());
        stepOutputs.put(stepKey, output == null ? Map.of() : output);
        for (String key : List.of("apply_no", "huifu_id", "apply_status", "file_id")) {
            String value = findString(output, key);
            if (!value.isBlank()) workflowContext.put(key, value);
        }
    }

    private Map<String, String> resolve(Map<String, String> values, Map<String, String> context) {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            Matcher matcher = PLACEHOLDER.matcher(value);
            StringBuilder resolved = new StringBuilder();
            while (matcher.find()) {
                String replacement = context.get(matcher.group(1));
                if (replacement == null) throw new IllegalArgumentException("Unknown workflow output: " + matcher.group(1));
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(resolved);
            result.put(key, resolved.toString());
        });
        return result;
    }

    private Map<String, String> flatten(Map<String, Object> context) {
        Map<String, String> result = new LinkedHashMap<>();
        flattenValue("", context, result);
        return result;
    }

    private void flattenValue(String prefix, Object value, Map<String, String> target) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, child) -> flattenValue(prefix.isEmpty() ? String.valueOf(key) : prefix + "." + key, child, target));
        } else if (value != null && !prefix.isEmpty()) {
            target.put(prefix, value instanceof String text ? text : writeJson(value));
        }
    }

    private String findString(Map<String, Object> response, String key) {
        if (response == null) return "";
        Object direct = response.get(key);
        if (direct != null) return String.valueOf(direct);
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map) {
            Map<String, Object> nested = new LinkedHashMap<>();
            map.forEach((nestedKey, value) -> nested.put(String.valueOf(nestedKey), value));
            return findString(nested, key);
        }
        if (data instanceof String text && !text.isBlank()) {
            try { return findString(objectMapper.readValue(text, new TypeReference<>() {}), key); }
            catch (JsonProcessingException ignored) { }
        }
        return "";
    }

    private void append(TaskExecution execution, String level, String message) {
        TaskExecutionLog log = new TaskExecutionLog();
        log.setExecution(execution);
        log.setLevel(level);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        logs.save(log);
    }

    private void release(String executionId) {
        pendingInputs.remove(executionId);
        activeExecution.compareAndSet(executionId, null);
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Workflow data cannot be serialized", error); }
    }
    private Map<String, String> readStringMap(String json) {
        try { return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {}); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Stored workflow inputs are invalid", error); }
    }
    private Map<String, Object> readObjectMap(String json) {
        try { return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {}); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Stored workflow context is invalid", error); }
    }
    private String limit(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
}
