package com.platform.task.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.task.platform.domain.TaskWorkflowTemplate;
import com.platform.task.platform.repository.TaskTemplateRepository;
import com.platform.task.platform.repository.TaskWorkflowTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaskWorkflowServiceTest {
    private final TaskTemplateRepository templates = mock(TaskTemplateRepository.class);
    private final TaskWorkflowService service;

    TaskWorkflowServiceTest() {
        when(templates.existsById(anyString())).thenReturn(true);
        service = new TaskWorkflowService(mock(TaskWorkflowTemplateRepository.class), templates,
                new TaskInputCatalog(), new ObjectMapper());
    }

    @Test
    void combinesActionInputsAndHidesPollingCorrelationFields() {
        var fields = service.inputFields(workflow());
        assertThat(fields).extracting("key")
                .contains("file_path", "file_type", "huifu_id", "upper_huifu_id")
                .doesNotContain("apply_no");
        assertThat(fields.stream().filter(field -> field.key().equals("file_type")).findFirst().orElseThrow().required())
                .isFalse();
    }

    @Test
    void validatesRequiredActionValuesFromMerchantDefaultsWithoutPersistingThem() {
        var prepared = service.prepare(workflow(), Map.of("huifu_id", "6661"),
                Map.of("upper_huifu_id", "6660"));
        assertThat(prepared.runtimeInputs()).containsEntry("upper_huifu_id", "6660");
        assertThat(prepared.storedInputs()).containsOnlyKeys("huifu_id");
    }

    private TaskWorkflowTemplate workflow() {
        TaskWorkflowTemplate workflow = new TaskWorkflowTemplate();
        workflow.setId("basic-modify-query");
        workflow.setStepsJson("""
                [
                  {"key":"picture","templateTaskId":"merchant-picture-upload","optional":true,"polling":false,"intervalSeconds":0,"maxAttempts":1},
                  {"key":"modify","templateTaskId":"merchant-basicdata-modify","optional":false,"polling":false,"intervalSeconds":0,"maxAttempts":1},
                  {"key":"query","templateTaskId":"merchant-application-status-query","optional":false,"polling":true,"intervalSeconds":30,"maxAttempts":120}
                ]
                """);
        return workflow;
    }
}
