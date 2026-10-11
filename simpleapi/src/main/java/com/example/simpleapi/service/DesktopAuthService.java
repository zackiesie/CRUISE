package com.example.simpleapi.service;

import com.example.simpleapi.Customer;
import com.example.simpleapi.CustomerRepository;
import com.example.simpleapi.CustomerToken;
import com.example.simpleapi.CustomerTokenRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.time.LocalDateTime;

import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class DesktopAuthService {

    private static final String GOOGLE_USERINFO =
            "https://openidconnect.googleapis.com/v1/userinfo";

    private final CustomerRepository customerRepository;
    private final CustomerTokenRepository customerTokenRepository;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final SecureRandom secureRandom;

    public DesktopAuthService(
            CustomerRepository customerRepository,
            CustomerTokenRepository customerTokenRepository) {

        this.customerRepository = customerRepository;
        this.customerTokenRepository = customerTokenRepository;

        this.httpClient =
                HttpClient.newHttpClient();

        this.mapper =
                new ObjectMapper();

        this.secureRandom =
                new SecureRandom();
    }

    // =========================================================
    // GOOGLE LOGIN
    // =========================================================

    public LoginResult loginWithGoogle(
            String googleAccessToken)
            throws Exception {

        if (googleAccessToken == null
                || googleAccessToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Google access token is required"
            );
        }

        // =====================================================
        // ASK GOOGLE WHO OWNS THIS TOKEN
        // =====================================================

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        GOOGLE_USERINFO
                                )
                        )
                        .header(
                                "Authorization",
                                "Bearer " + googleAccessToken
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse
                                .BodyHandlers
                                .ofString()
                );

        if (response.statusCode() != 200) {

            throw new SecurityException(
                    "Google token is invalid"
            );
        }

        JsonNode json =
                mapper.readTree(
                        response.body()
                );

        String googleId =
                requiredText(
                        json,
                        "sub"
                );

        String email =
                requiredText(
                        json,
                        "email"
                );

        String name =
                optionalText(
                        json,
                        "name"
                );

        String picture =
                optionalText(
                        json,
                        "picture"
                );

        if (name == null || name.isBlank()) {
            name = email;
        }

        // =====================================================
        // FIND / CREATE CUSTOMER
        // =====================================================

        Customer customer =
                customerRepository
                        .findByGoogleId(
                                googleId
                        )
                        .orElse(null);

        if (customer == null) {

            Optional<Customer> sameEmail =
                    customerRepository
                            .findByEmail(
                                    email
                            );

            if (sameEmail.isPresent()) {

                customer =
                        sameEmail.get();

                customer.setGoogleId(
                        googleId
                );

            } else {

                customer =
                        new Customer();

                customer.setGoogleId(
                        googleId
                );

                customer.setEmail(
                        email
                );

                customer.setCreatedAt(
                        LocalDateTime.now()
                );
            }
        }

        customer.setName(name);
        customer.setEmail(email);
        customer.setProfilePicture(picture);
        customer.setLastLoginAt(
                LocalDateTime.now()
        );

        customer =
                customerRepository.save(
                        customer
                );

        // =====================================================
        // CREATE OUR OWN APP TOKEN
        // =====================================================

        String rawToken =
                generateToken();

        CustomerToken customerToken =
                new CustomerToken();

        customerToken.setCustomerId(
                customer.getId()
        );

        customerToken.setTokenHash(
                hashToken(
                        rawToken
                )
        );

        customerToken.setCreatedAt(
                LocalDateTime.now()
        );

        // Practice:
        // login stays valid for 30 days.
        customerToken.setExpiresAt(
                LocalDateTime
                        .now()
                        .plusDays(30)
        );

        customerTokenRepository.save(
                customerToken
        );

        return new LoginResult(
                rawToken,
                customerToken.getExpiresAt(),
                customer
        );
    }

    // =========================================================
    // AUTHENTICATE APP TOKEN
    // =========================================================

    public Optional<Customer> authenticate(
            String rawToken) {

        if (rawToken == null
                || rawToken.isBlank()) {

            return Optional.empty();
        }

        String tokenHash =
                hashToken(
                        rawToken
                );

        Optional<CustomerToken> token =
                customerTokenRepository
                        .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                                tokenHash,
                                LocalDateTime.now()
                        );

        if (token.isEmpty()) {
            return Optional.empty();
        }

        return customerRepository.findById(
                token.get().getCustomerId()
        );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    public void revoke(
            String rawToken) {

        if (rawToken == null
                || rawToken.isBlank()) {

            return;
        }

        String tokenHash =
                hashToken(
                        rawToken
                );

        Optional<CustomerToken> token =
                customerTokenRepository
                        .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                                tokenHash,
                                LocalDateTime.now()
                        );

        if (token.isEmpty()) {
            return;
        }

        CustomerToken customerToken =
                token.get();

        customerToken.setRevokedAt(
                LocalDateTime.now()
        );

        customerTokenRepository.save(
                customerToken
        );
    }

    // =========================================================
    // RANDOM APP TOKEN
    // =========================================================

    private String generateToken() {

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // =========================================================
    // SHA-256 TOKEN HASH
    // =========================================================

    private String hashToken(
            String token) {

        try {

            MessageDigest digest =
                    MessageDigest
                            .getInstance(
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
                    "Unable to hash token",
                    e
            );
        }
    }

    // =========================================================
    // JSON
    // =========================================================

    private String requiredText(
            JsonNode json,
            String field) {

        JsonNode node =
                json.get(field);

        if (node == null
                || node.isNull()
                || node.asText().isBlank()) {

            throw new SecurityException(
                    "Google response missing "
                    + field
            );
        }

        return node.asText();
    }

    private String optionalText(
            JsonNode json,
            String field) {

        JsonNode node =
                json.get(field);

        if (node == null
                || node.isNull()
                || node.asText().isBlank()) {

            return null;
        }

        return node.asText();
    }

    // =========================================================
    // RESULT
    // =========================================================

    public record LoginResult(
            String token,
            LocalDateTime expiresAt,
            Customer customer) {
    }
}