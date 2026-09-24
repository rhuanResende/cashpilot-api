package com.desenvolvimento.logica.cashpilot_api.verification.repository;

import com.desenvolvimento.logica.cashpilot_api.verification.model.EmailVerificationToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from EmailVerificationToken token
            where token.tokenHash = :tokenHash
            """)
    Optional<EmailVerificationToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Query("""
        SELECT MAX(token.createdAt)
        FROM EmailVerificationToken token
        WHERE token.user.id = :userId
        """)
    Optional<Instant> findLastIssuedAtByUserId(
            @Param("userId") UUID userId
    );
}
