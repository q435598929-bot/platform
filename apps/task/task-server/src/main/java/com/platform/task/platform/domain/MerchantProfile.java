package com.platform.task.platform.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity @Table(name = "task_merchant_profile")
public class MerchantProfile {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, unique = true, length = 80) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 1000) private String description;
    @Column(nullable = false, columnDefinition = "TEXT") private String configurationJson = "{}";
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getConfigurationJson() { return configurationJson; }
    public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
