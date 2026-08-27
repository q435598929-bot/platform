package com.platform.ai.repository;
import com.platform.ai.domain.AiProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AiProviderRepository extends JpaRepository<AiProvider, Long> {
    List<AiProvider> findAllByOrderBySortOrderAscIdAsc();
}
