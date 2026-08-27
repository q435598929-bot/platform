package com.platform.task.platform.repository;

import com.platform.task.platform.domain.TaskWorkflowTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskWorkflowTemplateRepository extends JpaRepository<TaskWorkflowTemplate, String> {
    List<TaskWorkflowTemplate> findAllByOrderByCategoryAscDisplayNameAsc();
}
