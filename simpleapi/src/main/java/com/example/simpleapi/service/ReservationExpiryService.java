package com.example.simpleapi.service;

import com.example.simpleapi.Booking;
import com.example.simpleapi.BookingRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationExpiryService {

    private final BookingRepository bookingRepository;

    public ReservationExpiryService(
            BookingRepository bookingRepository) {

        this.bookingRepository =
                bookingRepository;
    }

    // =========================================================
    // RUN EVERY 30 SECONDS
    // =========================================================

    @Scheduled(fixedRate = 30000)
    public void expireOldReservations() {

        // Reservation hidup selama 2 minit.
        LocalDateTime expiryTime =
                LocalDateTime
                        .now()
                        .minusMinutes(2);

        List<Booking> expiredBookings =
                bookingRepository
                        .findByBookingStatusAndReservedAtBefore(
                                "RESERVED",
                                expiryTime
                        );

        if (expiredBookings.isEmpty()) {
            return;
        }

        for (Booking booking : expiredBookings) {

            // =================================================
            // SAFETY
            // =================================================

            if ("PAID".equals(
                    booking.getPaymentStatus())) {

                continue;
            }

            if ("REFUNDED".equals(
                    booking.getPaymentStatus())) {

                continue;
            }

            // =================================================
            // EXPIRE
            // =================================================

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
                    "RESERVATION AUTO EXPIRED"
            );

            System.out.println(
                    "Booking ID: "
                    + booking.getId()
            );

            System.out.println(
                    "Reserved At: "
                    + booking.getReservedAt()
            );

            System.out.println(
                    "Expired At: "
                    + LocalDateTime.now()
            );

            System.out.println(
                    "Payment Status: EXPIRED"
            );

            System.out.println(
                    "Booking Status: EXPIRED"
            );

            System.out.println(
                    "================================="
            );
        }
    }
}