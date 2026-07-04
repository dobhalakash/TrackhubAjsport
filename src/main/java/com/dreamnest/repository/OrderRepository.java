package com.dreamnest.repository;

import com.dreamnest.entity.Order;
import com.dreamnest.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.items i WHERE i.product.businessUser.id = :businessUserId")
    Page<Order> findByBusinessUserId(@Param("businessUserId") Long businessUserId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(o.user.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "ORDER BY o.createdAt DESC")
    java.util.List<Order> searchForSupport(@Param("q") String query, org.springframework.data.domain.Pageable pageable);

    long countByStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.grandTotal), 0) FROM Order o WHERE o.status <> 'CANCELLED'")
    java.math.BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(oi.totalPrice), 0) FROM OrderItem oi WHERE oi.product.businessUser.id = :businessUserId " +
            "AND oi.order.status <> 'CANCELLED'")
    java.math.BigDecimal sumRevenueByBusinessUser(@Param("businessUserId") Long businessUserId);
}
