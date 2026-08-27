package com.platform.ai.repository;
import com.platform.ai.domain.AiModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;
public interface AiModelRepository extends JpaRepository<AiModel, Long> {
    @EntityGraph(attributePaths = "provider")
    Optional<AiModel> findWithProviderById(Long id);
    List<AiModel> findAllByProvider_IdOrderBySortOrderAscIdAsc(Long providerId);
    @EntityGraph(attributePaths = "provider")
    List<AiModel> findAllByOrderBySortOrderAscIdAsc();
}
