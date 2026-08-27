package com.platform.task.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_workflow_template")
public class TaskWorkflowTemplate {
    @Id @Column(length = 120) private String id;
    @Column(nullable = false, length = 200) private String displayName;
    @Column(length = 1000) private String description;
    @Column(nullable = false, length = 80) private String category;
    @Column(nullable = false) private int version;
    @Column(nullable = false, columnDefinition = "TEXT") private String stepsJson;
    @Column(nullable = false) private boolean dangerous = true;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getStepsJson() { return stepsJson; }
    public void setStepsJson(String stepsJson) { this.stepsJson = stepsJson; }
    public boolean isDangerous() { return dangerous; }
    public void setDangerous(boolean dangerous) { this.dangerous = dangerous; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
