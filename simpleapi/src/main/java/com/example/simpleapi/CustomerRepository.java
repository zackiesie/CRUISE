package com.example.simpleapi;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository
        extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByGoogleId(String googleId);

    Optional<Customer> findByEmail(String email);
}