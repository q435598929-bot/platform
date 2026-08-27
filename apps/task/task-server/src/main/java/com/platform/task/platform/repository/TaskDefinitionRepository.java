package com.platform.task.platform.repository;

import com.platform.task.platform.domain.TaskDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskDefinitionRepository extends JpaRepository<TaskDefinition, String> {
    List<TaskDefinition> findAllByOrderByCategoryAscDisplayNameAsc();
    List<TaskDefinition> findByMerchantIdOrderByDisplayNameAsc(String merchantId);
}
