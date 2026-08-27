package com.platform.domain;

import java.util.List;

public record ApplicationView(
        String id,
        int sortOrder,
        String name,
        String description,
        String category,
        String sourceRoot,
        ApplicationDefinition.RuntimeType runtime,
        boolean lifecycleEnabled,
        Status status,
        String statusDetail,
        List<ApplicationDefinition.Link> links,
        List<ApplicationDefinition.Component> components) {
    public enum Status { RUNNING, STOPPED, UNAVAILABLE, DISABLED, UNKNOWN }
}
