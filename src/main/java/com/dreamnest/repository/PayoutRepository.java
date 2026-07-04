package com.dreamnest.repository;

import com.dreamnest.entity.Payout;
import com.dreamnest.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayoutRepository extends JpaRepository<Payout, Long> {

    List<Payout> findByOrderId(Long orderId);

    Optional<Payout> findByOrderIdAndBusinessUserId(Long orderId, Long businessUserId);

    Page<Payout> findByBusinessUserId(Long businessUserId, Pageable pageable);

    Page<Payout> findByBusinessUserIdAndStatus(Long businessUserId, PayoutStatus status, Pageable pageable);

    Page<Payout> findByStatus(PayoutStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.businessShare), 0) FROM Payout p WHERE p.businessUser.id = :businessUserId AND p.status = 'PENDING'")
    java.math.BigDecimal sumPendingForBusiness(@Param("businessUserId") Long businessUserId);

    @Query("SELECT COALESCE(SUM(p.businessShare), 0) FROM Payout p WHERE p.businessUser.id = :businessUserId AND p.status = 'PAID'")
    java.math.BigDecimal sumPaidForBusiness(@Param("businessUserId") Long businessUserId);

    List<Payout> findByBusinessUserIdOrderByCreatedAtDesc(Long businessUserId);
}
