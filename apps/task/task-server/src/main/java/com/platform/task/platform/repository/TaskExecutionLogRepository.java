package com.platform.task.platform.repository;

import com.platform.task.platform.domain.TaskExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskExecutionLogRepository extends JpaRepository<TaskExecutionLog, Long> {
    List<TaskExecutionLog> findByExecutionIdOrderByIdAsc(String executionId);
}
