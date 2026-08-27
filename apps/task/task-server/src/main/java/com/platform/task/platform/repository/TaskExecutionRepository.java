package com.platform.task.platform.repository;

import com.platform.task.platform.domain.TaskExecution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface TaskExecutionRepository extends JpaRepository<TaskExecution, String> {
    @EntityGraph(attributePaths = "task")
    List<TaskExecution> findTop100ByOrderByRequestedAtDesc();
    List<TaskExecution> findTop20ByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
            TaskExecution.Status status, LocalDateTime nextRunAt);
}
