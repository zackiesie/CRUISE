package com.example.simpleapi.controller;

import com.example.simpleapi.Customer;
import com.example.simpleapi.service.DesktopAuthService;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/desktop")
public class DesktopAuthController {

    private final DesktopAuthService desktopAuthService;

    public DesktopAuthController(
            DesktopAuthService desktopAuthService) {

        this.desktopAuthService =
                desktopAuthService;
    }

    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> body) {

        String googleAccessToken =
                body.get(
                        "accessToken"
                );

        if (googleAccessToken == null
                || googleAccessToken.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Map.of(
                                    "message",
                                    "Google access token is required"
                            )
                    );
        }

        try {

            DesktopAuthService.LoginResult result =
                    desktopAuthService
                            .loginWithGoogle(
                                    googleAccessToken
                            );

            Customer customer =
                    result.customer();

            Map<String, Object> customerJson =
                    new LinkedHashMap<>();

            customerJson.put(
                    "id",
                    customer.getId()
            );

            customerJson.put(
                    "name",
                    customer.getName()
            );

            customerJson.put(
                    "email",
                    customer.getEmail()
            );

            customerJson.put(
                    "phone",
                    customer.getPhone()
            );

            customerJson.put(
                    "picture",
                    customer.getProfilePicture()
            );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "token",
                    result.token()
            );

            response.put(
                    "expiresAt",
                    result.expiresAt()
            );

            response.put(
                    "customer",
                    customerJson
            );

            return ResponseEntity.ok(
                    response
            );

        } catch (SecurityException e) {

            return ResponseEntity
                    .status(
                            HttpStatus.UNAUTHORIZED
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Google authentication failed"
                            )
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Desktop login failed"
                            )
                    );
        }
    }
}