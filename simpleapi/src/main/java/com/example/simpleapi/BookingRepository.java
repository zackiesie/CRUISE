package com.example.simpleapi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository
        extends JpaRepository<Booking, Integer> {

    List<Booking>
    findByCarIdAndBookingStatusInAndStartDateLessThanAndEndDateGreaterThan(
            Integer carId,
            List<String> bookingStatuses,
            LocalDate endDate,
            LocalDate startDate
    );

    List<Booking>
    findByBookingStatusAndReservedAtBefore(
            String bookingStatus,
            LocalDateTime time
    );

    // Semua booking milik seorang customer
    List<Booking> findByCustomerIdOrderByIdDesc(
            Integer customerId
    );
}