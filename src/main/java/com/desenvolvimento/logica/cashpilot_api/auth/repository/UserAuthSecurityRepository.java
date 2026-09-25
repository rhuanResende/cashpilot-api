package com.desenvolvimento.logica.cashpilot_api.auth.repository;

import com.desenvolvimento.logica.cashpilot_api.auth.model.UserAuthSecurity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthSecurityRepository extends JpaRepository<UserAuthSecurity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT security
            FROM UserAuthSecurity security
            WHERE security.userId = :userId
            """)
    Optional<UserAuthSecurity> findByUserIdForUpdate(
            @Param("userId") UUID userId
    );
}
