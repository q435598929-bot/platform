package com.platform.task.platform.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity @Table(name = "task_execution_log")
public class TaskExecutionLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "execution_id") private TaskExecution execution;
    @Column(nullable = false, length = 20) private String level;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(nullable = false) private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TaskExecution getExecution() { return execution; }
    public void setExecution(TaskExecution execution) { this.execution = execution; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
