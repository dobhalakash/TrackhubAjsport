package com.dreamnest.repository;

import com.dreamnest.entity.BusinessProfile;
import com.dreamnest.enums.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    Optional<BusinessProfile> findByUserId(Long userId);
    Page<BusinessProfile> findByStatus(BusinessStatus status, Pageable pageable);
    long countByStatus(BusinessStatus status);
}
