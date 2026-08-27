package com.platform.task.platform.repository;

import com.platform.task.platform.domain.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, String> {
    List<TaskTemplate> findAllByOrderByCategoryAscDisplayNameAsc();
}
