package com.example.simpleapi;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerTokenRepository
        extends JpaRepository<CustomerToken, Integer> {

    Optional<CustomerToken>
            findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                    String tokenHash,
                    LocalDateTime now
            );
}