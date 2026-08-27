package com.platform.task.platform.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_step_execution")
public class TaskStepExecution {
    @Id @Column(length = 36) private String id;
    @ManyToOne(optional = false) @JoinColumn(name = "execution_id") private TaskExecution execution;
    @Column(nullable = false) private int stepIndex;
    @Column(nullable = false, length = 120) private String stepKey;
    @Column(nullable = false, length = 120) private String templateTaskId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(nullable = false) private int attemptCount;
    @Column(nullable = false, columnDefinition = "TEXT") private String inputsJson = "{}";
    @Column(nullable = false, columnDefinition = "TEXT") private String outputsJson = "{}";
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime nextRunAt;
    @Column(length = 2000) private String errorMessage;

    public enum Status { QUEUED, RUNNING, WAITING, SUCCEEDED, SKIPPED, FAILED }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public TaskExecution getExecution() { return execution; }
    public void setExecution(TaskExecution execution) { this.execution = execution; }
    public int getStepIndex() { return stepIndex; }
    public void setStepIndex(int stepIndex) { this.stepIndex = stepIndex; }
    public String getStepKey() { return stepKey; }
    public void setStepKey(String stepKey) { this.stepKey = stepKey; }
    public String getTemplateTaskId() { return templateTaskId; }
    public void setTemplateTaskId(String templateTaskId) { this.templateTaskId = templateTaskId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
    public String getInputsJson() { return inputsJson; }
    public void setInputsJson(String inputsJson) { this.inputsJson = inputsJson; }
    public String getOutputsJson() { return outputsJson; }
    public void setOutputsJson(String outputsJson) { this.outputsJson = outputsJson; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(LocalDateTime nextRunAt) { this.nextRunAt = nextRunAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
