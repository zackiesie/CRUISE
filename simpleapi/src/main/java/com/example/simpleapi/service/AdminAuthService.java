package com.example.simpleapi.service;

import com.example.simpleapi.Admin;
import com.example.simpleapi.AdminRepository;
import com.example.simpleapi.AdminToken;
import com.example.simpleapi.AdminTokenRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.time.LocalDateTime;

import java.util.Base64;
import java.util.Optional;

@Service
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final AdminTokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public AdminAuthService(
            AdminRepository adminRepository,
            AdminTokenRepository tokenRepository,
            BCryptPasswordEncoder passwordEncoder) {

        this.adminRepository =
                adminRepository;

        this.tokenRepository =
                tokenRepository;

        this.passwordEncoder =
                passwordEncoder;
    }

    public LoginResult login(
            String username,
            String password) {

        if (username == null
                || username.isBlank()
                || password == null
                || password.isBlank()) {

            throw new SecurityException(
                    "Username and password are required"
            );
        }

        Admin admin =
                adminRepository
                        .findByUsername(
                                username.trim()
                        )
                        .orElseThrow(
                                () ->
                                        new SecurityException(
                                                "Invalid username or password"
                                        )
                        );

        if (!passwordEncoder.matches(
                password,
                admin.getPasswordHash()
        )) {

            throw new SecurityException(
                    "Invalid username or password"
            );
        }

        String rawToken =
                generateToken();

        AdminToken token =
                new AdminToken();

        token.setAdminId(
                admin.getId()
        );

        token.setTokenHash(
                hashToken(rawToken)
        );

        token.setCreatedAt(
                LocalDateTime.now()
        );

        token.setExpiresAt(
                LocalDateTime
                        .now()
                        .plusHours(8)
        );

        tokenRepository.save(
                token
        );

        return new LoginResult(
                rawToken,
                token.getExpiresAt(),
                admin
        );
    }

    public Optional<Admin> authenticate(
            String rawToken) {

        if (rawToken == null
                || rawToken.isBlank()) {

            return Optional.empty();
        }

        Optional<AdminToken> token =
                tokenRepository
                        .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                                hashToken(rawToken),
                                LocalDateTime.now()
                        );

        if (token.isEmpty()) {

            return Optional.empty();
        }

        return adminRepository.findById(
                token.get().getAdminId()
        );
    }

    public void revoke(
            String rawToken) {

        if (rawToken == null
                || rawToken.isBlank()) {

            return;
        }

        Optional<AdminToken> token =
                tokenRepository
                        .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                                hashToken(rawToken),
                                LocalDateTime.now()
                        );

        if (token.isEmpty()) {

            return;
        }

        AdminToken adminToken =
                token.get();

        adminToken.setRevokedAt(
                LocalDateTime.now()
        );

        tokenRepository.save(
                adminToken
        );
    }

    private String generateToken() {

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(
                bytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(
            String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return java.util.HexFormat
                    .of()
                    .formatHex(hash);

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to hash admin token",
                    e
            );
        }
    }

    public record LoginResult(
            String token,
            LocalDateTime expiresAt,
            Admin admin
    ) {
    }
}