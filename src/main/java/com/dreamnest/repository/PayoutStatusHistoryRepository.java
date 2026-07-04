package com.dreamnest.repository;

import com.dreamnest.entity.PayoutStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayoutStatusHistoryRepository extends JpaRepository<PayoutStatusHistory, Long> {
    List<PayoutStatusHistory> findByPayoutIdOrderByChangedAtDesc(Long payoutId);
}
