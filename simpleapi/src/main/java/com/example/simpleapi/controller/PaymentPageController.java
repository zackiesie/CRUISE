package com.example.simpleapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentPageController {

    @GetMapping("/payment-success")
    public String paymentSuccess() {
        return "Payment successful! You may return to the Car Rental application.";
    }

    @GetMapping("/payment-cancel")
    public String paymentCancel() {
        return "Payment cancelled. You may return to the Car Rental application.";
    }
}