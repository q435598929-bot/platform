package com.platform.domain;

import java.util.ArrayList;
import java.util.List;

public class ApplicationDefinition {
    private String id;
    private int sortOrder;
    private String name;
    private String description;
    private String category;
    private String sourceRoot;
    private RuntimeType runtime;
    private boolean lifecycleEnabled;
    private List<String> startCommand = new ArrayList<>();
    private List<String> stopCommand = new ArrayList<>();
    private List<String> statusCommand = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
    private List<Component> components = new ArrayList<>();

    public enum RuntimeType { DOCKER_COMPOSE, LOCAL_PROCESS, LOCAL_TASK }
    public enum ComponentKind { SERVER, WEB, TASK, DATABASE }
    public record Link(String name, String url) {}
    public record Component(String id, String name, ComponentKind kind, String sourcePath, String technology) {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSourceRoot() { return sourceRoot; }
    public void setSourceRoot(String sourceRoot) { this.sourceRoot = sourceRoot; }
    public RuntimeType getRuntime() { return runtime; }
    public void setRuntime(RuntimeType runtime) { this.runtime = runtime; }
    public boolean isLifecycleEnabled() { return lifecycleEnabled; }
    public void setLifecycleEnabled(boolean lifecycleEnabled) { this.lifecycleEnabled = lifecycleEnabled; }
    public List<String> getStartCommand() { return startCommand; }
    public void setStartCommand(List<String> startCommand) { this.startCommand = startCommand; }
    public List<String> getStopCommand() { return stopCommand; }
    public void setStopCommand(List<String> stopCommand) { this.stopCommand = stopCommand; }
    public List<String> getStatusCommand() { return statusCommand; }
    public void setStatusCommand(List<String> statusCommand) { this.statusCommand = statusCommand; }
    public List<Link> getLinks() { return links; }
    public void setLinks(List<Link> links) { this.links = links; }
    public List<Component> getComponents() { return components; }
    public void setComponents(List<Component> components) { this.components = components; }
}
