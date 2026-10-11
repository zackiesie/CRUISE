package com.example.simpleapi.controller;

import com.example.simpleapi.*;
import com.example.simpleapi.service.AdminAuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminInsightsController {
    private final AdminAuthService auth; private final BookingRepository bookings; private final CustomerRepository customers; private final CarRepository cars;
    public AdminInsightsController(AdminAuthService auth, BookingRepository bookings, CustomerRepository customers, CarRepository cars){this.auth=auth;this.bookings=bookings;this.customers=customers;this.cars=cars;}
    @GetMapping("/summary") public ResponseEntity<?> summary(@RequestHeader(value="Authorization",required=false) String h){if(!ok(h))return unauth(); List<Booking> bs=bookings.findAll(); double revenue=bs.stream().filter(b->"PAID".equalsIgnoreCase(b.getPaymentStatus())).mapToDouble(Booking::getTotalAmount).sum(); long active=bs.stream().filter(b->"ACTIVE".equalsIgnoreCase(b.getBookingStatus())).count(); long paid=bs.stream().filter(b->"PAID".equalsIgnoreCase(b.getPaymentStatus())).count(); return ResponseEntity.ok(Map.of("cars",cars.count(),"customers",customers.count(),"bookings",bs.size(),"activeRentals",active,"paidBookings",paid,"revenue",revenue));}
    @GetMapping("/customers") public ResponseEntity<?> customers(@RequestHeader(value="Authorization",required=false) String h){if(!ok(h))return unauth(); return ResponseEntity.ok(customers.findAll());}
    private boolean ok(String h){return h!=null&&h.startsWith("Bearer ")&&auth.authenticate(h.substring(7).trim()).isPresent();}
    private ResponseEntity<?> unauth(){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","Admin authentication required"));}
}
