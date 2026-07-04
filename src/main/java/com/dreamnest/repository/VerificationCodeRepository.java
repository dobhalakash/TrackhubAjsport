package com.dreamnest.repository;

import com.dreamnest.entity.VerificationCode;
import com.dreamnest.enums.VerificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByUserIdAndChannelAndConsumedFalseOrderByCreatedAtDesc(Long userId, VerificationChannel channel);
}
