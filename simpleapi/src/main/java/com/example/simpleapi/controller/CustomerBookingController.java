package com.example.simpleapi.controller;

import com.example.simpleapi.Booking;
import com.example.simpleapi.BookingRepository;
import com.example.simpleapi.Customer;
import com.example.simpleapi.service.DesktopAuthService;

import com.stripe.StripeClient;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer/bookings")
public class CustomerBookingController {

    private final BookingRepository bookingRepository;
    private final DesktopAuthService desktopAuthService;
    private final StripeClient stripeClient;

    public CustomerBookingController(
            BookingRepository bookingRepository,
            DesktopAuthService desktopAuthService,
            StripeClient stripeClient) {

        this.bookingRepository =
                bookingRepository;

        this.desktopAuthService =
                desktopAuthService;

        this.stripeClient =
                stripeClient;
    }

    // =========================================================
    // MY BOOKINGS
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getMyBookings(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        // =====================================================
        // AUTHENTICATE CUSTOMER
        // =====================================================

        Customer customer =
                authenticateCustomer(
                        authorization
                );

        if (customer == null) {

            return error(
                    HttpStatus.UNAUTHORIZED,
                    "Login expired. Please login again."
            );
        }

        // =====================================================
        // LOAD ONLY THIS CUSTOMER'S BOOKINGS
        // =====================================================

        List<Booking> bookings =
                bookingRepository
                        .findByCustomerIdOrderByIdDesc(
                                customer.getId()
                        );

        return ResponseEntity.ok(
                bookings
        );
    }

    // =========================================================
    // CANCEL MY BOOKING + STRIPE REFUND
    // =========================================================

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelMyBooking(
            @PathVariable Integer id,

            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        // =====================================================
        // AUTHENTICATE CUSTOMER
        // =====================================================

        Customer customer =
                authenticateCustomer(
                        authorization
                );

        if (customer == null) {

            return error(
                    HttpStatus.UNAUTHORIZED,
                    "Login expired. Please login again."
            );
        }

        // =====================================================
        // FIND BOOKING
        // =====================================================

        Booking booking =
                bookingRepository
                        .findById(id)
                        .orElse(null);

        if (booking == null) {

            return error(
                    HttpStatus.NOT_FOUND,
                    "Booking not found"
            );
        }

        // =====================================================
        // OWNERSHIP CHECK
        //
        // Customer hanya boleh cancel booking sendiri.
        // =====================================================

        if (booking.getCustomerId() == null
                || !booking.getCustomerId()
                        .equals(
                                customer.getId()
                        )) {

            return error(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to cancel this booking"
            );
        }

        // =====================================================
        // ALREADY CANCELLED
        // =====================================================

        if ("CANCELLED".equals(
                booking.getBookingStatus()
        )) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Booking is already cancelled"
            );
        }

        // =====================================================
        // ONLY PAID BOOKING CAN BE REFUNDED
        // =====================================================

        if (!"PAID".equals(
                booking.getPaymentStatus()
        )) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Only paid bookings can be refunded"
            );
        }

        // =====================================================
        // PAYMENT INTENT REQUIRED
        // =====================================================

        String paymentIntentId =
                booking
                        .getStripePaymentIntentId();

        if (paymentIntentId == null
                || paymentIntentId.isBlank()) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Stripe PaymentIntent is missing for this booking"
            );
        }

        try {

            // =================================================
            // CREATE STRIPE REFUND
            // =================================================

            RefundCreateParams params =
                    RefundCreateParams
                            .builder()

                            .setPaymentIntent(
                                    paymentIntentId
                            )

                            .setReason(
                                    RefundCreateParams
                                            .Reason
                                            .REQUESTED_BY_CUSTOMER
                            )

                            .build();

            Refund refund =
                    stripeClient
                            .v1()
                            .refunds()
                            .create(
                                    params
                            );

            // =================================================
            // SAVE REFUND
            // =================================================

            booking.setStripeRefundId(
                    refund.getId()
            );

            booking.setPaymentStatus(
                    "REFUNDED"
            );

            booking.setBookingStatus(
                    "CANCELLED"
            );

            bookingRepository.save(
                    booking
            );

            // =================================================
            // LOG
            // =================================================

            System.out.println(
                    "================================="
            );

            System.out.println(
                    "CUSTOMER REFUND SUCCESS"
            );

            System.out.println(
                    "Customer ID: "
                    + customer.getId()
            );

            System.out.println(
                    "Booking ID: "
                    + booking.getId()
            );

            System.out.println(
                    "PaymentIntent: "
                    + paymentIntentId
            );

            System.out.println(
                    "Refund ID: "
                    + refund.getId()
            );

            System.out.println(
                    "================================="
            );

            // =================================================
            // RESPONSE
            // =================================================

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Booking cancelled and payment refunded",
                            "bookingId",
                            String.valueOf(
                                    booking.getId()
                            ),
                            "refundId",
                            refund.getId()
                    )
            );

        } catch (Exception e) {

            /*
             * IMPORTANT:
             *
             * Kalau Stripe refund gagal,
             * booking TIDAK diubah.
             *
             * Jadi status masih:
             *
             * PAID
             * ACTIVE
             */

            e.printStackTrace();

            return error(
                    HttpStatus.BAD_GATEWAY,
                    "Stripe refund failed. Booking was not cancelled."
            );
        }
    }

    // =========================================================
    // AUTHENTICATE CUSTOMER
    // =========================================================

    private Customer authenticateCustomer(
            String authorization) {

        String token =
                extractBearerToken(
                        authorization
                );

        if (token == null) {
            return null;
        }

        Optional<Customer> customerOptional =
                desktopAuthService
                        .authenticate(
                                token
                        );

        return customerOptional
                .orElse(null);
    }

    // =========================================================
    // EXTRACT BEARER TOKEN
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

    // =========================================================
    // ERROR RESPONSE
    // =========================================================

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(
                        Map.of(
                                "message",
                                message
                        )
                );
    }
}