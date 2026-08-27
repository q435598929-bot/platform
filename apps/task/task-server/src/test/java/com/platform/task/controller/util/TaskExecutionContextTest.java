package com.platform.task.controller.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskExecutionContextTest {
    @Test
    void capturesStructuredStepOutputWithoutChangingExistingInputLookup() {
        try (TaskExecutionContext context = TaskExecutionContext.open(Map.of("apply_no", "input"))) {
            TaskExecutionContext.recordOutput(Map.of("apply_no", "output", "apply_status", "P"));
            assertThat(TaskExecutionContext.value("apply_no")).isEqualTo("input");
            assertThat(context.output()).containsEntry("apply_no", "output").containsEntry("apply_status", "P");
        }
    }
}
