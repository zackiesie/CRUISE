package com.example.simpleapi.controller;

import com.example.simpleapi.Customer;
import com.example.simpleapi.CustomerRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final CustomerRepository customerRepository;

    public AuthController(
            CustomerRepository customerRepository) {

        this.customerRepository =
                customerRepository;
    }

    @GetMapping("/")
    public String home() {

        return """
               Car Rental API is running.

               Google Login:
               /oauth2/authorization/google
               """;
    }

    @GetMapping("/login-success")
    public String loginSuccess() {

        return """
               Google Login Successful!

               Customer has been saved to MySQL.

               You may return to the Car Rental application.
               """;
    }

    @GetMapping("/login-error")
    public String loginError() {

        return """
               Google Login Failed.

               Please return to the Car Rental application
               and try again.
               """;
    }

    // =========================================================
    // CURRENT GOOGLE USER
    // =========================================================

    @GetMapping("/api/auth/me")
    public ResponseEntity<?> currentUser(
            @AuthenticationPrincipal OidcUser googleUser,
            HttpServletRequest request) {

        if (googleUser == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "Not logged in"
                            )
                    );
        }

        HttpSession session =
                request.getSession(false);

        Integer customerId =
                null;

        if (session != null) {

            Object value =
                    session.getAttribute(
                            "customerId"
                    );

            if (value instanceof Integer) {

                customerId =
                        (Integer) value;
            }
        }

        Customer customer =
                null;

        if (customerId != null) {

            customer =
                    customerRepository
                            .findById(customerId)
                            .orElse(null);
        }

        Map<String, Object> response =
                new LinkedHashMap<>();

        if (customer != null) {

            response.put(
                    "customerId",
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

            response.put(
                    "createdAt",
                    customer.getCreatedAt()
            );

            response.put(
                    "lastLoginAt",
                    customer.getLastLoginAt()
            );

        } else {

            response.put(
                    "googleId",
                    googleUser.getSubject()
            );

            response.put(
                    "name",
                    googleUser.getFullName()
            );

            response.put(
                    "email",
                    googleUser.getEmail()
            );

            response.put(
                    "picture",
                    googleUser.getPicture()
            );
        }

        return ResponseEntity.ok(
                response
        );
    }
}