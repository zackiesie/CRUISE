package com.example.simpleapi.config;

import com.example.simpleapi.Admin;
import com.example.simpleapi.AdminRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminInitializer
        implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminInitializer(
            AdminRepository adminRepository,
            BCryptPasswordEncoder passwordEncoder) {

        this.adminRepository =
                adminRepository;

        this.passwordEncoder =
                passwordEncoder;
    }

    @Override
    public void run(
            String... args) {

        String username =
                System.getenv(
                        "CRUISE_ADMIN_USERNAME"
                );

        String password =
                System.getenv(
                        "CRUISE_ADMIN_PASSWORD"
                );

        if (username == null
                || username.isBlank()
                || password == null
                || password.isBlank()) {

            System.out.println(
                    "Admin account not initialized: "
                    + "CRUISE_ADMIN_USERNAME / "
                    + "CRUISE_ADMIN_PASSWORD not configured."
            );

            return;
        }

        if (adminRepository
                .findByUsername(username)
                .isPresent()) {

            return;
        }

        Admin admin =
                new Admin();

        admin.setUsername(
                username
        );

        admin.setName(
                "CRUISE Administrator"
        );

        admin.setPasswordHash(
                passwordEncoder.encode(
                        password
                )
        );

        admin.setCreatedAt(
                LocalDateTime.now()
        );

        adminRepository.save(
                admin
        );

        System.out.println(
                "Initial CRUISE admin account created."
        );
    }
}