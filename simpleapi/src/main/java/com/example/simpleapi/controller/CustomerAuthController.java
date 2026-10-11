package com.example.simpleapi.controller;

import com.example.simpleapi.Customer;
import com.example.simpleapi.service.DesktopAuthService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerAuthController {

    private final DesktopAuthService desktopAuthService;

    public CustomerAuthController(
            DesktopAuthService desktopAuthService) {

        this.desktopAuthService =
                desktopAuthService;
    }

    // =========================================================
    // CURRENT CUSTOMER
    // =========================================================

    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        String token =
                extractBearerToken(
                        authorization
                );

        if (token == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "Not logged in"
                            )
                    );
        }

        Optional<Customer> customerOptional =
                desktopAuthService
                        .authenticate(token);

        if (customerOptional.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "Login expired"
                            )
                    );
        }

        Customer customer =
                customerOptional.get();

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "id",
                customer.getId()
        );

        response.put(
                "name",
                customer.getName()
        );

        response.put(
                "email",
                customer.getEmail()
        );

        response.put(
                "phone",
                customer.getPhone()
        );

        response.put(
                "picture",
                customer.getProfilePicture()
        );

        return ResponseEntity.ok(
                response
        );
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        String token =
                extractBearerToken(
                        authorization
                );

        if (token != null) {

            desktopAuthService.revoke(
                    token
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logged out"
                )
        );
    }

    // =========================================================
    // BEARER TOKEN
    // =========================================================

    private String extractBearerToken(
            String authorization) {

        if (authorization == null
                || !authorization.startsWith(
                        "Bearer "
                )) {

            return null;
        }

        String token =
                authorization
                        .substring(7)
                        .trim();

        if (token.isBlank()) {
            return null;
        }

        return token;
    }
}