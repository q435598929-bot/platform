package com.platform.task.platform.repository;

import com.platform.task.platform.domain.MerchantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, String> {
    List<MerchantProfile> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    Optional<MerchantProfile> findByCodeIgnoreCase(String code);
}
