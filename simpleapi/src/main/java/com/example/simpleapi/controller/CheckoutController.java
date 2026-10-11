package com.example.simpleapi.controller;

import com.example.simpleapi.Booking;
import com.example.simpleapi.BookingRepository;
import com.example.simpleapi.Car;
import com.example.simpleapi.CarRepository;
import com.example.simpleapi.Customer;
import com.example.simpleapi.service.DesktopAuthService;

import com.stripe.StripeClient;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CarRepository carRepository;
    private final BookingRepository bookingRepository;
    private final StripeClient stripeClient;
    private final DesktopAuthService desktopAuthService;

    public CheckoutController(
            CarRepository carRepository,
            BookingRepository bookingRepository,
            StripeClient stripeClient,
            DesktopAuthService desktopAuthService) {

        this.carRepository =
                carRepository;

        this.bookingRepository =
                bookingRepository;

        this.stripeClient =
                stripeClient;

        this.desktopAuthService =
                desktopAuthService;
    }

    // =========================================================
    // CREATE CHECKOUT
    // =========================================================

    @PostMapping("/create")
    public ResponseEntity<?> createCheckout(

            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,

            @RequestParam Integer carId,

            @RequestParam String customerPhone,

            @RequestParam LocalDate startDate,

            @RequestParam LocalDate endDate,

            @RequestParam(defaultValue = "false") boolean childSeat,
            @RequestParam(defaultValue = "false") boolean additionalDriver,
            @RequestParam(defaultValue = "STANDARD") String protectionPlan) {

        // =====================================================
        // AUTHENTICATION
        // =====================================================

        String appToken =
                extractBearerToken(
                        authorization
                );

        if (appToken == null) {

            return error(
                    HttpStatus.UNAUTHORIZED,
                    "Please login before making a booking"
            );
        }

        Optional<Customer> customerOptional =
                desktopAuthService
                        .authenticate(
                                appToken
                        );

        if (customerOptional.isEmpty()) {

            return error(
                    HttpStatus.UNAUTHORIZED,
                    "Login expired. Please login again."
            );
        }

        Customer customer =
                customerOptional.get();

        // =====================================================
        // CUSTOMER DATA
        //
        // Name, email and customer ID come from authenticated
        // customer account, NOT from JavaFX request.
        // =====================================================

        Integer customerId =
                customer.getId();

        String customerName =
                customer.getName();

        String customerEmail =
                customer.getEmail();

        // =====================================================
        // CUSTOMER ACCOUNT VALIDATION
        // =====================================================

        if (customerName == null
                || customerName.trim().isEmpty()) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Customer account name is missing"
            );
        }

        if (customerEmail == null
                || customerEmail.trim().isEmpty()) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Customer account email is missing"
            );
        }

        // =====================================================
        // PHONE
        // =====================================================

        if (customerPhone == null
                || customerPhone.trim().isEmpty()
                || !customerPhone.matches(
                        "^[0-9+\\- ]{7,20}$"
                )) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Invalid customer phone"
            );
        }

        // =====================================================
        // DATE VALIDATION
        // =====================================================

        if (startDate == null
                || endDate == null) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Start date and end date are required"
            );
        }

        LocalDate today =
                LocalDate.now();

        if (startDate.isBefore(today)) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Start date cannot be in the past"
            );
        }

        if (!endDate.isAfter(startDate)) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "End date must be after start date"
            );
        }

        // =====================================================
        // FIND CAR
        // =====================================================

        Car car =
                carRepository
                        .findById(carId)
                        .orElse(null);

        if (car == null) {

            return error(
                    HttpStatus.NOT_FOUND,
                    "Car not found"
            );
        }

        if (car.getStatus() != null && !car.getStatus().isBlank()
                && !"AVAILABLE".equalsIgnoreCase(car.getStatus())) {
            return error(HttpStatus.CONFLICT,
                    "This vehicle is currently unavailable (" + car.getStatus() + ")");
        }

        // =====================================================
        // AVAILABILITY
        //
        // RESERVED = customer sedang checkout
        // ACTIVE   = customer dah bayar
        //
        // Dua-dua block kereta.
        // =====================================================

        List<String> blockingStatuses =
                List.of(
                        "RESERVED",
                        "ACTIVE"
                );

        List<Booking> overlappingBookings =
                bookingRepository
                        .findByCarIdAndBookingStatusInAndStartDateLessThanAndEndDateGreaterThan(
                                carId,
                                blockingStatuses,
                                endDate,
                                startDate
                        );

        if (!overlappingBookings.isEmpty()) {

            return error(
                    HttpStatus.CONFLICT,
                    "Car is not available for the selected dates"
            );
        }

        // =====================================================
        // CALCULATE DAYS
        // =====================================================

        long rentalDays =
                ChronoUnit.DAYS.between(
                        startDate,
                        endDate
                );

        if (rentalDays <= 0
                || rentalDays > Integer.MAX_VALUE) {

            return error(
                    HttpStatus.BAD_REQUEST,
                    "Invalid rental period"
            );
        }

        int days =
                (int) rentalDays;

        // =====================================================
        // TOTAL
        // =====================================================

        double baseAmount = car.getPricePerDay() * days;
        double addOnAmount = 0.0;
        java.util.List<String> selectedAddOns = new java.util.ArrayList<>();

        if (childSeat) {
            addOnAmount += 12.0 * days;
            selectedAddOns.add("Child seat");
        }
        if (additionalDriver) {
            addOnAmount += 18.0 * days;
            selectedAddOns.add("Additional driver");
        }

        String normalizedProtection = protectionPlan == null
                ? "STANDARD" : protectionPlan.trim().toUpperCase();
        if (!java.util.Set.of("STANDARD", "PLUS", "PREMIUM").contains(normalizedProtection)) {
            normalizedProtection = "STANDARD";
        }
        if ("PLUS".equals(normalizedProtection)) addOnAmount += 15.0 * days;
        if ("PREMIUM".equals(normalizedProtection)) addOnAmount += 28.0 * days;

        double totalAmount = baseAmount + addOnAmount;

        // =====================================================
        // CREATE BOOKING
        // =====================================================

        Booking booking =
                new Booking();

        // Authenticated customer
        booking.setCustomerId(
                customerId
        );

        booking.setCarId(
                car.getId()
        );

        booking.setCustomerName(
                customerName.trim()
        );

        booking.setCustomerEmail(
                customerEmail.trim()
        );

        booking.setCustomerPhone(
                customerPhone.trim()
        );

        booking.setStartDate(
                startDate
        );

        booking.setEndDate(
                endDate
        );

        booking.setDays(
                days
        );

        booking.setBaseAmount(baseAmount);
        booking.setAddOnAmount(addOnAmount);
        booking.setAddOns(selectedAddOns.isEmpty() ? "None" : String.join(", ", selectedAddOns));
        booking.setProtectionPlan(normalizedProtection);
        booking.setTotalAmount(
                totalAmount
        );

        booking.setPaymentStatus(
                "PENDING_PAYMENT"
        );

        booking.setBookingStatus(
                "RESERVED"
        );

        // =====================================================
        // RESERVATION START TIME
        // =====================================================

        booking.setReservedAt(
                LocalDateTime.now()
        );

        // =====================================================
        // SAVE BOOKING
        // =====================================================

        booking =
                bookingRepository.save(
                        booking
                );

        Integer bookingId =
                booking.getId();

        try {

            // =================================================
            // STRIPE AMOUNT
            // =================================================

            long stripeAmount =
                    Math.round(
                            totalAmount * 100
                    );

            String description =
                    car.getBrand()
                    + " "
                    + car.getModel()
                    + " - "
                    + days
                    + " day(s)";

            // =================================================
            // CREATE STRIPE CHECKOUT
            // =================================================

            SessionCreateParams params =
                    SessionCreateParams
                            .builder()

                            .setMode(
                                    SessionCreateParams
                                            .Mode
                                            .PAYMENT
                            )

                            .setSuccessUrl(
                                    "http://localhost:8080/payment-success"
                            )

                            .setCancelUrl(
                                    "http://localhost:8080/payment-cancel"
                            )

                            .setCustomerEmail(
                                    customerEmail.trim()
                            )

                            .putMetadata(
                                    "bookingId",
                                    String.valueOf(
                                            bookingId
                                    )
                            )

                            .putMetadata(
                                    "customerId",
                                    String.valueOf(
                                            customerId
                                    )
                            )

                            .addLineItem(
                                    SessionCreateParams
                                            .LineItem
                                            .builder()

                                            .setQuantity(
                                                    1L
                                            )

                                            .setPriceData(
                                                    SessionCreateParams
                                                            .LineItem
                                                            .PriceData
                                                            .builder()

                                                            .setCurrency(
                                                                    "myr"
                                                            )

                                                            .setUnitAmount(
                                                                    stripeAmount
                                                            )

                                                            .setProductData(
                                                                    SessionCreateParams
                                                                            .LineItem
                                                                            .PriceData
                                                                            .ProductData
                                                                            .builder()

                                                                            .setName(
                                                                                    description
                                                                            )

                                                                            .build()
                                                            )

                                                            .build()
                                            )

                                            .build()
                            )

                            .build();

            Session session =
                    stripeClient
                            .v1()
                            .checkout()
                            .sessions()
                            .create(
                                    params
                            );

            // =================================================
            // SAVE STRIPE SESSION ID
            // =================================================

            booking.setStripeSessionId(
                    session.getId()
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
                    "BOOKING RESERVED"
            );

            System.out.println(
                    "Booking ID: "
                    + bookingId
            );

            System.out.println(
                    "Customer ID: "
                    + customerId
            );

            System.out.println(
                    "Customer: "
                    + customerName
            );

            System.out.println(
                    "Car ID: "
                    + carId
            );

            System.out.println(
                    "Reserved At: "
                    + booking.getReservedAt()
            );

            System.out.println(
                    "Reservation expires in 2 minutes"
            );

            System.out.println(
                    "Payment Status: PENDING_PAYMENT"
            );

            System.out.println(
                    "Booking Status: RESERVED"
            );

            System.out.println(
                    "Stripe Session: "
                    + session.getId()
            );

            System.out.println(
                    "================================="
            );

            // =================================================
            // RESPONSE
            // =================================================

            return ResponseEntity.ok(
                    Map.of(
                            "bookingId",
                            String.valueOf(
                                    bookingId
                            ),
                            "customerId",
                            String.valueOf(
                                    customerId
                            ),
                            "checkoutUrl",
                            session.getUrl()
                    )
            );

        } catch (Exception e) {

            System.err.println(
                    "Stripe Checkout failed"
            );

            e.printStackTrace();

            // Kalau Stripe gagal create checkout,
            // buang reservation supaya kereta
            // tak terkunci.
            bookingRepository.deleteById(
                    bookingId
            );

            return error(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to create Stripe Checkout"
            );
        }
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