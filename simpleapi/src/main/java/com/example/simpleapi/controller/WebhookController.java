package com.example.simpleapi.controller;

import com.example.simpleapi.Booking;
import com.example.simpleapi.BookingRepository;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
public class WebhookController {

    private final BookingRepository bookingRepository;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public WebhookController(
            BookingRepository bookingRepository) {

        this.bookingRepository =
                bookingRepository;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature")
            String signature) {

        try {

            Event event =
                    Webhook.constructEvent(
                            payload,
                            signature,
                            webhookSecret
                    );

            System.out.println(
                    "Stripe event received: "
                    + event.getType()
            );

            // =================================================
            // CHECKOUT COMPLETED
            // =================================================

            if ("checkout.session.completed"
                    .equals(event.getType())) {

                handleCheckoutCompleted(event);
            }

            // =================================================
            // CHECKOUT EXPIRED
            // =================================================

            else if ("checkout.session.expired"
                    .equals(event.getType())) {

                handleCheckoutExpired(event);
            }

            return ResponseEntity.ok(
                    "Webhook received"
            );

        } catch (Exception e) {

            System.err.println(
                    "Webhook error"
            );

            e.printStackTrace();

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Webhook error"
                    );
        }
    }

    // =========================================================
    // PAYMENT COMPLETED
    // =========================================================

    private void handleCheckoutCompleted(
            Event event) throws Exception {

        Session session =
                (Session)
                        event
                                .getDataObjectDeserializer()
                                .deserializeUnsafe();

        String bookingIdText =
                session
                        .getMetadata()
                        .get(
                                "bookingId"
                        );

        if (bookingIdText == null
                || bookingIdText.isBlank()) {

            System.err.println(
                    "Completed session missing bookingId"
            );

            return;
        }

        Integer bookingId =
                Integer.valueOf(
                        bookingIdText
                );

        Booking booking =
                bookingRepository
                        .findById(
                                bookingId
                        )
                        .orElse(null);

        if (booking == null) {

            System.err.println(
                    "Booking not found: "
                    + bookingId
            );

            return;
        }

        // Jangan process event dua kali.
        if ("PAID".equals(
                booking.getPaymentStatus())) {

            System.out.println(
                    "Booking "
                    + bookingId
                    + " already PAID"
            );

            return;
        }

        // =====================================================
        // SAVE STRIPE DATA
        // =====================================================

        booking.setStripeSessionId(
                session.getId()
        );

        booking.setStripePaymentIntentId(
                session.getPaymentIntent()
        );

        // =====================================================
        // UPDATE STATUS
        // =====================================================

        booking.setPaymentStatus(
                "PAID"
        );

        booking.setBookingStatus(
                "ACTIVE"
        );

        bookingRepository.save(
                booking
        );

        System.out.println(
                "================================="
        );

        System.out.println(
                "PAYMENT SUCCESS"
        );

        System.out.println(
                "Booking ID: "
                + bookingId
        );

        System.out.println(
                "Stripe Session: "
                + session.getId()
        );

        System.out.println(
                "PaymentIntent: "
                + session.getPaymentIntent()
        );

        System.out.println(
                "Payment Status: PAID"
        );

        System.out.println(
                "Booking Status: ACTIVE"
        );

        System.out.println(
                "================================="
        );
    }

    // =========================================================
    // CHECKOUT EXPIRED
    // =========================================================

    private void handleCheckoutExpired(
            Event event) throws Exception {

        Session session =
                (Session)
                        event
                                .getDataObjectDeserializer()
                                .deserializeUnsafe();

        String bookingIdText =
                session
                        .getMetadata()
                        .get(
                                "bookingId"
                        );

        if (bookingIdText == null
                || bookingIdText.isBlank()) {

            System.err.println(
                    "Expired session missing bookingId"
            );

            return;
        }

        Integer bookingId =
                Integer.valueOf(
                        bookingIdText
                );

        Booking booking =
                bookingRepository
                        .findById(
                                bookingId
                        )
                        .orElse(null);

        if (booking == null) {

            System.err.println(
                    "Booking not found: "
                    + bookingId
            );

            return;
        }

        // =====================================================
        // SAFETY
        // Jangan expire booking yang dah dibayar/refund.
        // =====================================================

        if ("PAID".equals(
                booking.getPaymentStatus())) {

            System.out.println(
                    "Ignoring expired event because booking "
                    + bookingId
                    + " is already PAID"
            );

            return;
        }

        if ("REFUNDED".equals(
                booking.getPaymentStatus())) {

            System.out.println(
                    "Ignoring expired event because booking "
                    + bookingId
                    + " is already REFUNDED"
            );

            return;
        }

        // =====================================================
        // MARK EXPIRED
        // =====================================================

        booking.setBookingStatus(
                "EXPIRED"
        );

        booking.setPaymentStatus(
                "EXPIRED"
        );

        bookingRepository.save(
                booking
        );

        System.out.println(
                "================================="
        );

        System.out.println(
                "CHECKOUT EXPIRED"
        );

        System.out.println(
                "Booking ID: "
                + bookingId
        );

        System.out.println(
                "Stripe Session: "
                + session.getId()
        );

        System.out.println(
                "Payment Status: EXPIRED"
        );

        System.out.println(
                "Booking Status: EXPIRED"
        );

        System.out.println(
                "Reservation released"
        );

        System.out.println(
                "================================="
        );
    }
}