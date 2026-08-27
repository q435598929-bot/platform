package com.platform.task.platform.web;

import com.platform.task.platform.domain.TaskExecution;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class TaskDtos {
    private TaskDtos() {}
    public record TaskResponse(String id, String displayName, String description, String category,
                               String merchantId, String merchantName, String templateTaskId,
                               String workflowTemplateId, String workflowName,
                               String className, boolean enabled, boolean dangerous,
                               List<InputFieldResponse> inputFields) {}
    public record TaskTemplateResponse(String id, String displayName, String description, String category,
                                       String className, boolean dangerous, List<InputFieldResponse> inputFields) {}
    public record WorkflowStepResponse(String key, int order, String templateTaskId, String templateName,
                                       boolean optional, boolean polling, int intervalSeconds, int maxAttempts) {}
    public record WorkflowTemplateResponse(String id, String displayName, String description, String category,
                                           int version, boolean dangerous, List<WorkflowStepResponse> steps,
                                           List<InputFieldResponse> inputFields) {}
    public record CreateTaskRequest(
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,119}$") String id,
            @NotBlank @Size(max = 200) String displayName,
            @Size(max = 1000) String description,
            @NotBlank String merchantId,
            String templateTaskId,
            String workflowTemplateId) {}
    public record MerchantResponse(String id, String code, String name, String description,
                                   List<String> configurationKeys, LocalDateTime updatedAt) {}
    public record MerchantConfigurationResponse(String merchantId, Map<String, String> configuration) {}
    public record MerchantConfigurationFieldResponse(String key, String label, String description,
                                                     String placeholder, boolean required,
                                                     boolean secret, boolean multiline) {}
    public record CreateMerchantRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{1,79}$") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 1000) String description,
            Map<@NotBlank @Size(max = 100) String, @Size(max = 10000) String> configuration) {}
    public record UpdateMerchantConfigurationRequest(
            @NotNull Map<@NotBlank @Size(max = 100) String, @Size(max = 10000) String> configuration) {}
    public record InputFieldResponse(String key, String label, String type, boolean required,
                                     String placeholder, String description, String defaultValue,
                                     List<OptionResponse> options, boolean secret) {}
    public record OptionResponse(String label, String value) {}
    public record EnableRequest(boolean enabled) {}
    public record RunRequest(boolean confirmed,
                             @Size(max = 50) List<@Size(max = 2000) String> arguments,
                             Map<String, @Size(max = 10000) String> inputs) {}
    public record ExecutionResponse(String id, String taskId, String taskName, TaskExecution.Status status,
                                    List<String> arguments, Map<String, String> inputs,
                                    String triggerSource, String executionType, boolean confirmed,
                                    LocalDateTime requestedAt, LocalDateTime startedAt, LocalDateTime finishedAt,
                                    LocalDateTime nextRunAt, String errorMessage,
                                    Map<String, Object> outputs,
                                    List<StepExecutionResponse> steps) {}
    public record StepExecutionResponse(String id, int stepIndex, String stepKey, String templateTaskId,
                                        String status, int attemptCount, Map<String, Object> outputs,
                                        LocalDateTime startedAt, LocalDateTime finishedAt,
                                        LocalDateTime nextRunAt, String errorMessage) {}
    public record LogResponse(Long id, String level, String message, LocalDateTime createdAt) {}
}
