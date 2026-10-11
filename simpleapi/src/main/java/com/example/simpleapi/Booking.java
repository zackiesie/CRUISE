package com.example.simpleapi;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Customer account yang memiliki booking ini
    private Integer customerId;

    private Integer carId;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private Integer days;
    private double totalAmount;

    private String paymentStatus;
    private String bookingStatus;

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime reservedAt;

    private String stripeSessionId;
    private String stripePaymentIntentId;
    private String stripeRefundId;

    // V3 rental options and lifecycle
    private double baseAmount;
    private double addOnAmount;
    private String addOns;
    private String protectionPlan;
    private LocalDateTime completedAt;
    private Integer returnMileage;
    private String returnFuelLevel;

    @Column(length = 1000)
    private String returnNotes;

    public Booking() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public Integer getCarId() {
        return carId;
    }

    public void setCarId(Integer carId) {
        this.carId = carId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripeRefundId() {
        return stripeRefundId;
    }

    public void setStripeRefundId(String stripeRefundId) {
        this.stripeRefundId = stripeRefundId;
    }

    public double getBaseAmount() { return baseAmount; }
    public void setBaseAmount(double baseAmount) { this.baseAmount = baseAmount; }
    public double getAddOnAmount() { return addOnAmount; }
    public void setAddOnAmount(double addOnAmount) { this.addOnAmount = addOnAmount; }
    public String getAddOns() { return addOns; }
    public void setAddOns(String addOns) { this.addOns = addOns; }
    public String getProtectionPlan() { return protectionPlan; }
    public void setProtectionPlan(String protectionPlan) { this.protectionPlan = protectionPlan; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getReturnMileage() { return returnMileage; }
    public void setReturnMileage(Integer returnMileage) { this.returnMileage = returnMileage; }
    public String getReturnFuelLevel() { return returnFuelLevel; }
    public void setReturnFuelLevel(String returnFuelLevel) { this.returnFuelLevel = returnFuelLevel; }
    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = returnNotes; }
}