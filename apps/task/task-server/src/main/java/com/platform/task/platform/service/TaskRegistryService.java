package com.platform.task.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.task.platform.domain.MerchantProfile;
import com.platform.task.platform.domain.TaskDefinition;
import com.platform.task.platform.domain.TaskTemplate;
import com.platform.task.platform.repository.MerchantProfileRepository;
import com.platform.task.platform.repository.TaskDefinitionRepository;
import com.platform.task.platform.repository.TaskTemplateRepository;
import com.platform.task.platform.web.TaskDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaskRegistryService {
    private final MerchantProfileRepository merchants;
    private final TaskTemplateRepository templates;
    private final TaskDefinitionRepository tasks;
    private final TaskInputCatalog inputs;
    private final TaskWorkflowService workflows;
    private final ObjectMapper objectMapper;

    public TaskRegistryService(MerchantProfileRepository merchants, TaskTemplateRepository templates,
                               TaskDefinitionRepository tasks, TaskInputCatalog inputs,
                               TaskWorkflowService workflows, ObjectMapper objectMapper) {
        this.merchants = merchants;
        this.templates = templates;
        this.tasks = tasks;
        this.inputs = inputs;
        this.workflows = workflows;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TaskTemplateResponse> templates() {
        return templates.findAllByOrderByCategoryAscDisplayNameAsc().stream().map(template ->
                new TaskTemplateResponse(template.getId(), template.getDisplayName(), template.getDescription(),
                        template.getCategory(), template.getClassName(), template.isDangerous(),
                        inputs.fields(template.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<WorkflowTemplateResponse> workflows() {
        return workflows.workflows();
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> merchants() {
        return merchants.findAllByOrderByNameAsc().stream().map(this::merchantResponse).toList();
    }

    @Transactional(readOnly = true)
    public MerchantConfigurationResponse configuration(String id) {
        MerchantProfile merchant = requireMerchant(id);
        return new MerchantConfigurationResponse(id, readConfiguration(merchant));
    }

    public List<MerchantConfigurationFieldResponse> configurationFields() {
        return MerchantConfigurationCatalog.fields().stream().map(field ->
                new MerchantConfigurationFieldResponse(field.key(), field.label(), field.description(),
                        field.placeholder(), field.required(), field.secret(), field.multiline())).toList();
    }

    @Transactional
    public MerchantResponse createMerchant(CreateMerchantRequest request) {
        if (merchants.existsByCodeIgnoreCase(request.code())) throw new IllegalArgumentException("Merchant code already exists");
        LocalDateTime now = LocalDateTime.now();
        MerchantProfile merchant = new MerchantProfile();
        merchant.setId(UUID.randomUUID().toString());
        merchant.setCode(request.code().trim().toUpperCase());
        merchant.setName(request.name().trim());
        merchant.setDescription(request.description());
        merchant.setConfigurationJson(writeConfiguration(request.configuration()));
        merchant.setCreatedAt(now);
        merchant.setUpdatedAt(now);
        return merchantResponse(merchants.save(merchant));
    }

    @Transactional
    public MerchantConfigurationResponse updateConfiguration(String id, UpdateMerchantConfigurationRequest request) {
        MerchantProfile merchant = requireMerchant(id);
        merchant.setConfigurationJson(writeConfiguration(request.configuration()));
        merchant.setUpdatedAt(LocalDateTime.now());
        merchants.save(merchant);
        return new MerchantConfigurationResponse(id, readConfiguration(merchant));
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        if (tasks.existsById(request.id())) throw new IllegalArgumentException("Task id already exists");
        MerchantProfile merchant = requireMerchant(request.merchantId());
        boolean hasTaskTemplate = request.templateTaskId() != null && !request.templateTaskId().isBlank();
        boolean hasWorkflowTemplate = request.workflowTemplateId() != null && !request.workflowTemplateId().isBlank();
        if (hasTaskTemplate == hasWorkflowTemplate) {
            throw new IllegalArgumentException("Choose exactly one task template or workflow template");
        }
        com.platform.task.platform.domain.TaskWorkflowTemplate workflow = hasWorkflowTemplate
                ? workflows.require(request.workflowTemplateId()) : null;
        TaskTemplate template = workflow == null
                ? templates.findById(request.templateTaskId())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown task template: " + request.templateTaskId()))
                : workflows.primaryTemplate(workflow);
        LocalDateTime now = LocalDateTime.now();
        TaskDefinition task = new TaskDefinition();
        task.setId(request.id());
        task.setDisplayName(request.displayName().trim());
        String defaultDescription = workflow == null ? template.getDescription() : workflow.getDescription();
        task.setDescription(request.description() == null || request.description().isBlank()
                ? defaultDescription : request.description().trim());
        task.setCategory(merchant.getCode());
        task.setMerchant(merchant);
        task.setTemplateTaskId(template.getId());
        task.setWorkflowTemplate(workflow);
        task.setClassName(template.getClassName());
        task.setEnabled(false);
        task.setDangerous(workflow == null ? template.isDangerous() : workflow.isDangerous());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        return taskResponse(tasks.save(task));
    }

    private MerchantProfile requireMerchant(String id) {
        return merchants.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown merchant: " + id));
    }

    private MerchantResponse merchantResponse(MerchantProfile merchant) {
        return new MerchantResponse(merchant.getId(), merchant.getCode(), merchant.getName(), merchant.getDescription(),
                readConfiguration(merchant).keySet().stream().sorted().toList(), merchant.getUpdatedAt());
    }

    private TaskResponse taskResponse(TaskDefinition task) {
        String templateId = task.getTemplateTaskId() == null ? task.getId() : task.getTemplateTaskId();
        MerchantProfile merchant = task.getMerchant();
        var workflow = task.getWorkflowTemplate();
        return new TaskResponse(task.getId(), task.getDisplayName(), task.getDescription(), task.getCategory(),
                merchant == null ? null : merchant.getId(), merchant == null ? null : merchant.getName(), templateId,
                workflow == null ? null : workflow.getId(), workflow == null ? null : workflow.getDisplayName(),
                task.getClassName(), task.isEnabled(), task.isDangerous(),
                workflow == null ? inputs.fields(templateId) : workflows.inputFields(workflow));
    }

    private Map<String, String> readConfiguration(MerchantProfile merchant) {
        try { return objectMapper.readValue(merchant.getConfigurationJson(), new TypeReference<LinkedHashMap<String, String>>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Stored merchant configuration is invalid", e); }
    }

    private String writeConfiguration(Map<String, String> configuration) {
        Map<String, String> normalized = MerchantConfigurationCatalog.normalize(configuration);
        try { return objectMapper.writeValueAsString(normalized); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Merchant configuration cannot be serialized", e); }
    }
}
