package com.example.simpleapi.config;

import com.example.simpleapi.security.GoogleLoginSuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final GoogleLoginSuccessHandler
            googleLoginSuccessHandler;

    public SecurityConfig(
            GoogleLoginSuccessHandler googleLoginSuccessHandler) {

        this.googleLoginSuccessHandler =
                googleLoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http
            .csrf(
                    csrf ->
                            csrf.disable()
            )

            .authorizeHttpRequests(
                    auth ->
                            auth.requestMatchers(
                                    "/",
                                    "/uploads/**",

                                    /*
                                     * Public customer car browsing.
                                     * CarController is now GET-only.
                                     */
                                    "/api/cars/**",

                                    /*
                                     * Admin endpoints perform custom
                                     * Bearer-token validation.
                                     */
                                    "/api/admin/**",

                                    "/api/checkout/**",
                                    "/api/stripe/**",

                                    "/api/auth/desktop/**",
                                    "/api/customer/**",

                                    "/payment-success",
                                    "/payment-cancel",
                                    "/login-success",
                                    "/login-error",

                                    "/oauth2/**",
                                    "/login/**",
                                    "/error"
                            )
                            .permitAll()

                            .anyRequest()
                            .authenticated()
            )

            .oauth2Login(
                    oauth ->
                            oauth.successHandler(
                                    googleLoginSuccessHandler
                            )
            );

        return http.build();
    }
}