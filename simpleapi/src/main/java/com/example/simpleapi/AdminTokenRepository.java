package com.example.simpleapi;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminTokenRepository
        extends JpaRepository<AdminToken, Integer> {

    Optional<AdminToken>
            findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                    String tokenHash,
                    LocalDateTime now
            );
}