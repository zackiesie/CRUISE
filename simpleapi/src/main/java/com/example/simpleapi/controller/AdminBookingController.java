package com.example.simpleapi.controller;

import com.example.simpleapi.Booking;
import com.example.simpleapi.BookingRepository;
import com.example.simpleapi.service.AdminAuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingRepository bookingRepository;
    private final AdminAuthService authService;

    public AdminBookingController(
            BookingRepository bookingRepository,
            AdminAuthService authService) {

        this.bookingRepository =
                bookingRepository;

        this.authService =
                authService;
    }

    @GetMapping
    public ResponseEntity<?> getBookings(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        if (!authorized(
                authorization
        )) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            Map.of(
                                    "message",
                                    "Admin authentication required"
                            )
                    );
        }

        List<Booking> bookings =
                bookingRepository
                        .findAll();

        return ResponseEntity.ok(
                bookings
        );
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeRental(
            @PathVariable Integer id,
            @RequestHeader(value="Authorization", required=false) String authorization,
            @RequestParam(required=false) Integer mileage,
            @RequestParam(defaultValue="FULL") String fuelLevel,
            @RequestParam(defaultValue="") String notes) {
        if (!authorized(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Admin authentication required"));
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Booking not found"));
        if (!"PAID".equalsIgnoreCase(booking.getPaymentStatus())) return ResponseEntity.badRequest().body(Map.of("message","Only paid rentals can be completed"));
        if ("CANCELLED".equalsIgnoreCase(booking.getBookingStatus())) return ResponseEntity.badRequest().body(Map.of("message","Cancelled booking cannot be completed"));
        booking.setBookingStatus("COMPLETED");
        booking.setCompletedAt(LocalDateTime.now());
        booking.setReturnMileage(mileage);
        booking.setReturnFuelLevel(fuelLevel);
        booking.setReturnNotes(notes == null ? "" : notes.trim());
        bookingRepository.save(booking);
        return ResponseEntity.ok(booking);
    }

    private boolean authorized(
            String authorization) {

        if (authorization == null
                || !authorization.startsWith(
                        "Bearer "
                )) {

            return false;
        }

        String token =
                authorization
                        .substring(7)
                        .trim();

        return authService
                .authenticate(token)
                .isPresent();
    }
}