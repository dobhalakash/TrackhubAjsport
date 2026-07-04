package com.dreamnest.repository;

import com.dreamnest.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByProductIdAndNotifiedFalse(Long productId);
    boolean existsByProductIdAndEmailAndNotifiedFalse(Long productId, String email);
}
