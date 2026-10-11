package com.example.simpleapi.security;

import com.example.simpleapi.Customer;
import com.example.simpleapi.CustomerRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final CustomerRepository customerRepository;

    public GoogleLoginSuccessHandler(
            CustomerRepository customerRepository) {

        this.customerRepository =
                customerRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        OidcUser googleUser =
                (OidcUser) authentication.getPrincipal();

        String googleId =
                googleUser.getSubject();

        String name =
                googleUser.getFullName();

        String email =
                googleUser.getEmail();

        String picture =
                googleUser.getPicture();

        // =====================================================
        // VALIDATION
        // =====================================================

        if (googleId == null
                || googleId.isBlank()
                || email == null
                || email.isBlank()) {

            response.sendRedirect(
                    "/login-error"
            );

            return;
        }

        // =====================================================
        // FIND CUSTOMER BY GOOGLE ID
        // =====================================================

        Optional<Customer> existingCustomer =
                customerRepository
                        .findByGoogleId(googleId);

        Customer customer;

        if (existingCustomer.isPresent()) {

            // =================================================
            // EXISTING CUSTOMER
            // =================================================

            customer =
                    existingCustomer.get();

            customer.setName(
                    name
            );

            customer.setEmail(
                    email
            );

            customer.setProfilePicture(
                    picture
            );

            customer.setLastLoginAt(
                    LocalDateTime.now()
            );

            customerRepository.save(
                    customer
            );

            System.out.println(
                    "================================="
            );

            System.out.println(
                    "GOOGLE CUSTOMER LOGIN"
            );

            System.out.println(
                    "Customer ID: "
                    + customer.getId()
            );

            System.out.println(
                    "Name: "
                    + customer.getName()
            );

            System.out.println(
                    "Email: "
                    + customer.getEmail()
            );

            System.out.println(
                    "Existing customer"
            );

            System.out.println(
                    "================================="
            );

        } else {

            // =================================================
            // CHECK EMAIL
            // =================================================

            Optional<Customer> sameEmail =
                    customerRepository
                            .findByEmail(email);

            if (sameEmail.isPresent()) {

                // Customer mungkin pernah wujud tetapi
                // Google ID belum disimpan.
                customer =
                        sameEmail.get();

                customer.setGoogleId(
                        googleId
                );

                customer.setName(
                        name
                );

                customer.setProfilePicture(
                        picture
                );

                customer.setLastLoginAt(
                        LocalDateTime.now()
                );

                customerRepository.save(
                        customer
                );

            } else {

                // =================================================
                // NEW CUSTOMER
                // =================================================

                customer =
                        new Customer();

                customer.setGoogleId(
                        googleId
                );

                customer.setName(
                        name
                );

                customer.setEmail(
                        email
                );

                customer.setProfilePicture(
                        picture
                );

                customer.setCreatedAt(
                        LocalDateTime.now()
                );

                customer.setLastLoginAt(
                        LocalDateTime.now()
                );

                customerRepository.save(
                        customer
                );
            }

            System.out.println(
                    "================================="
            );

            System.out.println(
                    "GOOGLE CUSTOMER REGISTERED"
            );

            System.out.println(
                    "Customer ID: "
                    + customer.getId()
            );

            System.out.println(
                    "Name: "
                    + customer.getName()
            );

            System.out.println(
                    "Email: "
                    + customer.getEmail()
            );

            System.out.println(
                    "================================="
            );
        }

        // =====================================================
        // SAVE CUSTOMER ID IN SESSION
        // =====================================================

        request
                .getSession()
                .setAttribute(
                        "customerId",
                        customer.getId()
                );

        // =====================================================
        // LOGIN SUCCESS
        // =====================================================

        response.sendRedirect(
                "/login-success"
        );
    }
}