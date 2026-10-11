package com.example.simpleapi.controller;

import com.example.simpleapi.Admin;
import com.example.simpleapi.service.AdminAuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService authService;

    public AdminAuthController(
            AdminAuthService authService) {

        this.authService =
                authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            AdminAuthService.LoginResult result =
                    authService.login(
                            request.username(),
                            request.password()
                    );

            return ResponseEntity.ok(
                    Map.of(
                            "token",
                            result.token(),
                            "expiresAt",
                            result.expiresAt().toString(),
                            "adminId",
                            result.admin().getId(),
                            "username",
                            result.admin().getUsername(),
                            "name",
                            result.admin().getName()
                    )
            );

        } catch (SecurityException e) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    e.getMessage()
                            )
                    );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        String token =
                bearerToken(
                        authorization
                );

        Admin admin =
                authService
                        .authenticate(token)
                        .orElse(null);

        if (admin == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "Invalid or expired admin session"
                            )
                    );
        }

        return ResponseEntity.ok(
                Map.of(
                        "id",
                        admin.getId(),
                        "username",
                        admin.getUsername(),
                        "name",
                        admin.getName()
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        String token =
                bearerToken(
                        authorization
                );

        authService.revoke(
                token
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Logged out"
                )
        );
    }

    private String bearerToken(
            String authorization) {

        if (authorization == null
                || !authorization.startsWith(
                        "Bearer "
                )) {

            return null;
        }

        return authorization
                .substring(7)
                .trim();
    }

    public record LoginRequest(
            String username,
            String password
    ) {
    }
}