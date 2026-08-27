package com.platform.task.platform.repository;

import com.platform.task.platform.domain.TaskStepExecution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskStepExecutionRepository extends JpaRepository<TaskStepExecution, String> {
    @EntityGraph(attributePaths = "execution")
    List<TaskStepExecution> findByExecutionIdOrderByStepIndexAsc(String executionId);
    Optional<TaskStepExecution> findByExecutionIdAndStepIndex(String executionId, int stepIndex);
}
