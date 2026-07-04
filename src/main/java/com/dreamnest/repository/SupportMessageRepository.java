package com.dreamnest.repository;

import com.dreamnest.entity.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    List<SupportMessage> findByBusinessUserIdOrderByCreatedAtAsc(Long businessUserId);

    /** One row per business thread, with its most recent message - powers the admin's inbox list. */
    @Query("SELECT m FROM SupportMessage m WHERE m.id IN " +
            "(SELECT MAX(m2.id) FROM SupportMessage m2 GROUP BY m2.businessUser.id) " +
            "ORDER BY m.createdAt DESC")
    List<SupportMessage> findLatestPerBusinessThread();

    @Query("SELECT COUNT(m) FROM SupportMessage m WHERE m.businessUser.id = :businessUserId " +
            "AND m.senderRole = 'BUSINESS' AND m.readByAdmin = false")
    long countUnreadForAdmin(@Param("businessUserId") Long businessUserId);

    @Query("SELECT COUNT(m) FROM SupportMessage m WHERE m.businessUser.id = :businessUserId " +
            "AND m.senderRole = 'ADMIN' AND m.readByBusiness = false")
    long countUnreadForBusiness(@Param("businessUserId") Long businessUserId);
}
