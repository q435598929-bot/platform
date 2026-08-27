package com.platform.task.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.task.controller.util.TaskExecutionContext;
import com.platform.task.platform.domain.TaskDefinition;
import com.platform.task.platform.domain.MerchantProfile;
import com.platform.task.platform.domain.TaskExecution;
import com.platform.task.platform.domain.TaskExecutionLog;
import com.platform.task.platform.repository.TaskDefinitionRepository;
import com.platform.task.platform.repository.TaskExecutionLogRepository;
import com.platform.task.platform.repository.TaskExecutionRepository;
import com.platform.task.platform.repository.TaskStepExecutionRepository;
import com.platform.task.platform.web.TaskDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TaskManagementService {
    private final TaskDefinitionRepository definitions;
    private final TaskExecutionRepository executions;
    private final TaskExecutionLogRepository logs;
    private final TaskCompatibilityCatalog catalog;
    private final TaskInputCatalog inputCatalog;
    private final TaskWorkflowService workflowService;
    private final TaskWorkflowExecutionService workflowExecutions;
    private final TaskStepExecutionRepository stepExecutions;
    private final ObjectMapper objectMapper;
    private final AtomicReference<String> runningExecution = new AtomicReference<>();
    private final Map<String, Map<String, String>> pendingInputs = new ConcurrentHashMap<>();

    public TaskManagementService(TaskDefinitionRepository definitions, TaskExecutionRepository executions,
                                 TaskExecutionLogRepository logs, TaskCompatibilityCatalog catalog,
                                 TaskInputCatalog inputCatalog, TaskWorkflowService workflowService,
                                 TaskWorkflowExecutionService workflowExecutions,
                                 TaskStepExecutionRepository stepExecutions, ObjectMapper objectMapper) {
        this.definitions = definitions;
        this.executions = executions;
        this.logs = logs;
        this.catalog = catalog;
        this.inputCatalog = inputCatalog;
        this.workflowService = workflowService;
        this.workflowExecutions = workflowExecutions;
        this.stepExecutions = stepExecutions;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> tasks() {
        return definitions.findAllByOrderByCategoryAscDisplayNameAsc().stream().map(this::taskResponse).toList();
    }

    @Transactional
    public TaskResponse enable(String id, boolean enabled) {
        TaskDefinition task = requireTask(id);
        task.setEnabled(enabled);
        task.setUpdatedAt(LocalDateTime.now());
        return taskResponse(definitions.save(task));
    }

    @Transactional
    public ExecutionResponse run(String id, RunRequest request) {
        TaskDefinition task = requireTask(id);
        if (!task.isEnabled()) throw new IllegalStateException("Task is disabled; explicitly enable it first");
        if (task.isDangerous() && !request.confirmed()) throw new IllegalStateException("Explicit confirmation is required");
        if (task.getWorkflowTemplate() != null) return executionResponse(workflowExecutions.run(task, request));
        String templateId = task.getTemplateTaskId() == null ? task.getId() : task.getTemplateTaskId();
        TaskInputCatalog.PreparedInputs prepared = inputCatalog.prepare(templateId, request.inputs(), request.arguments());

        String executionId = UUID.randomUUID().toString();
        if (!runningExecution.compareAndSet(null, executionId)) {
            throw new IllegalStateException("Another task execution is already running: " + runningExecution.get());
        }
        TaskExecution execution = new TaskExecution();
        execution.setId(executionId);
        execution.setTask(task);
        execution.setStatus(TaskExecution.Status.QUEUED);
        execution.setArgumentsJson(writeJson(prepared.arguments()));
        execution.setInputsJson(writeJson(prepared.storedInputs()));
        execution.setTriggerSource("WEB_API");
        execution.setConfirmed(request.confirmed());
        execution.setRequestedAt(LocalDateTime.now());
        executions.save(execution);
        Map<String, String> runtimeInputs = new LinkedHashMap<>();
        MerchantProfile merchant = task.getMerchant();
        if (merchant != null) {
            Map<String, String> configuration = readMap(merchant.getConfigurationJson());
            configuration.forEach((key, value) -> {
                runtimeInputs.put(key, value);
                runtimeInputs.put("merchant." + key, value);
            });
            runtimeInputs.put("merchant.code", merchant.getCode());
            runtimeInputs.put("merchant.name", merchant.getName());
        }
        runtimeInputs.putAll(prepared.runtimeInputs());
        pendingInputs.put(executionId, Map.copyOf(runtimeInputs));
        append(execution, "INFO", "Execution queued through the compatibility main() adapter");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Thread.ofVirtual().name("task-execution-" + executionId).start(() -> execute(executionId));
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    pendingInputs.remove(executionId);
                    runningExecution.compareAndSet(executionId, null);
                }
            }
        });
        return executionResponse(execution);
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> executions() {
        return executions.findTop100ByOrderByRequestedAtDesc().stream().map(this::executionResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LogResponse> logs(String executionId) {
        if (!executions.existsById(executionId)) throw new IllegalArgumentException("Unknown execution: " + executionId);
        return logs.findByExecutionIdOrderByIdAsc(executionId).stream()
                .map(log -> new LogResponse(log.getId(), log.getLevel(), log.getMessage(), log.getCreatedAt())).toList();
    }

    @Transactional
    public ExecutionResponse triggerNow(String executionId) {
        return executionResponse(workflowExecutions.triggerNow(executionId));
    }

    void execute(String executionId) {
        TaskExecution execution = null;
        Map<String, String> runtimeInputs = pendingInputs.remove(executionId);
        try (TaskExecutionContext context = TaskExecutionContext.open(runtimeInputs)) {
            execution = executions.findById(executionId).orElseThrow();
            execution.setStatus(TaskExecution.Status.RUNNING);
            execution.setStartedAt(LocalDateTime.now());
            executions.save(execution);
            append(execution, "INFO", "Task started: " + execution.getTask().getClassName());
            Method main = catalog.requireMain(execution.getTask().getClassName());
            String[] arguments = readList(execution.getArgumentsJson()).toArray(String[]::new);
            main.invoke(null, (Object) arguments);
            execution.setResultJson(writeJson(context.output()));
            execution.setStatus(TaskExecution.Status.SUCCEEDED);
            append(execution, "INFO", "Task completed successfully");
        } catch (Throwable throwable) {
            Throwable cause = throwable instanceof InvocationTargetException && throwable.getCause() != null
                    ? throwable.getCause() : throwable;
            if (execution != null) {
                execution.setStatus(TaskExecution.Status.FAILED);
                execution.setErrorMessage(limit(cause.getClass().getSimpleName() + ": " + cause.getMessage(), 2000));
                append(execution, "ERROR", execution.getErrorMessage());
            }
        } finally {
            try {
                if (execution != null) {
                    execution.setFinishedAt(LocalDateTime.now());
                    executions.save(execution);
                }
            } finally {
                runningExecution.compareAndSet(executionId, null);
            }
        }
    }

    private TaskDefinition requireTask(String id) {
        return definitions.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown task: " + id));
    }
    private TaskResponse taskResponse(TaskDefinition task) {
        String templateId = task.getTemplateTaskId() == null ? task.getId() : task.getTemplateTaskId();
        MerchantProfile merchant = task.getMerchant();
        var workflow = task.getWorkflowTemplate();
        return new TaskResponse(task.getId(), task.getDisplayName(), task.getDescription(), task.getCategory(),
                merchant == null ? null : merchant.getId(), merchant == null ? null : merchant.getName(), templateId,
                workflow == null ? null : workflow.getId(), workflow == null ? null : workflow.getDisplayName(),
                task.getClassName(), task.isEnabled(), task.isDangerous(),
                workflow == null ? inputCatalog.fields(templateId) : workflowService.inputFields(workflow));
    }
    private ExecutionResponse executionResponse(TaskExecution execution) {
        List<StepExecutionResponse> stepResponses = stepExecutions.findByExecutionIdOrderByStepIndexAsc(execution.getId())
                .stream().map(step -> new StepExecutionResponse(step.getId(), step.getStepIndex(), step.getStepKey(),
                        step.getTemplateTaskId(), step.getStatus().name(), step.getAttemptCount(),
                        readObjectMap(step.getOutputsJson()), step.getStartedAt(), step.getFinishedAt(),
                        step.getNextRunAt(), step.getErrorMessage())).toList();
        return new ExecutionResponse(execution.getId(), execution.getTask().getId(), execution.getTask().getDisplayName(),
                execution.getStatus(), readList(execution.getArgumentsJson()), readMap(execution.getInputsJson()), execution.getTriggerSource(),
                execution.getExecutionType(), execution.isConfirmed(), execution.getRequestedAt(), execution.getStartedAt(),
                execution.getFinishedAt(), execution.getNextRunAt(), execution.getErrorMessage(),
                readObjectMap(execution.getResultJson()), stepResponses);
    }
    private void append(TaskExecution execution, String level, String message) {
        TaskExecutionLog log = new TaskExecutionLog();
        log.setExecution(execution); log.setLevel(level); log.setMessage(message); log.setCreatedAt(LocalDateTime.now());
        logs.save(log);
    }
    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Task inputs cannot be serialized", e); }
    }
    private List<String> readList(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Stored arguments are invalid", e); }
    }
    private Map<String, String> readMap(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Stored page inputs are invalid", e); }
    }
    private Map<String, Object> readObjectMap(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Stored step outputs are invalid", e); }
    }
    private String limit(String value, int max) { return value == null ? null : value.substring(0, Math.min(value.length(), max)); }
}
