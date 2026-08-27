package com.platform.task.platform.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity @Table(name = "task_execution")
public class TaskExecution {
    @Id @Column(length = 36) private String id;
    @ManyToOne(optional = false) @JoinColumn(name = "task_id") private TaskDefinition task;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status;
    @Column(nullable = false, columnDefinition = "TEXT") private String argumentsJson;
    @Column(nullable = false, columnDefinition = "TEXT") private String inputsJson;
    @Column(nullable = false, length = 30) private String executionType = "LEGACY";
    @Column(nullable = false) private int currentStepIndex;
    @Column(nullable = false, columnDefinition = "TEXT") private String contextJson = "{}";
    @Column(nullable = false, columnDefinition = "TEXT") private String resultJson = "{}";
    private LocalDateTime nextRunAt;
    @Column(nullable = false, length = 50) private String triggerSource;
    @Column(nullable = false) private boolean confirmed;
    @Column(nullable = false) private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    @Column(length = 2000) private String errorMessage;

    public enum Status { QUEUED, RUNNING, WAITING, SUCCEEDED, FAILED, CANCELLED }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public TaskDefinition getTask() { return task; }
    public void setTask(TaskDefinition task) { this.task = task; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getArgumentsJson() { return argumentsJson; }
    public void setArgumentsJson(String argumentsJson) { this.argumentsJson = argumentsJson; }
    public String getInputsJson() { return inputsJson; }
    public void setInputsJson(String inputsJson) { this.inputsJson = inputsJson; }
    public String getExecutionType() { return executionType; }
    public void setExecutionType(String executionType) { this.executionType = executionType; }
    public int getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(int currentStepIndex) { this.currentStepIndex = currentStepIndex; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(LocalDateTime nextRunAt) { this.nextRunAt = nextRunAt; }
    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
