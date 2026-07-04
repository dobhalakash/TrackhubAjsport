package com.dreamnest.repository;

import com.dreamnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.name = :roleName")
    org.springframework.data.domain.Page<User> findByRoleName(@Param("roleName") com.dreamnest.enums.RoleName roleName,
                                                                org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
    long countByRoleName(@Param("roleName") com.dreamnest.enums.RoleName roleName);
}
