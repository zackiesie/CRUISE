package com.example.simpleclient;

public class Car {

    private Integer id;
    private String brand;
    private String model;
    private double pricePerDay;

    private String category;
    private String transmission;
    private String fuelType;
    private Integer seats;
    private String description;
    private String imageUrl;
    private String status;
    private Integer mileage;
    private java.time.LocalDate nextServiceDate;

    public Car() {
    }

    /*
     * Constructor lama.
     *
     * Legacy constructors may use:
     *
     * new Car(id, brand, model, pricePerDay)
     *
     * Jadi constructor ini dikekalkan supaya
     * code lama masih boleh compile.
     */
    public Car(
            Integer id,
            String brand,
            String model,
            double pricePerDay) {

        this.id = id;
        this.brand = brand;
        this.model = model;
        this.pricePerDay = pricePerDay;
    }

    /*
     * Constructor baru untuk model Car lengkap.
     */
    public Car(
            Integer id,
            String brand,
            String model,
            double pricePerDay,
            String category,
            String transmission,
            String fuelType,
            Integer seats,
            String description,
            String imageUrl,
            String status) {

        this.id = id;
        this.brand = brand;
        this.model = model;
        this.pricePerDay = pricePerDay;
        this.category = category;
        this.transmission = transmission;
        this.fuelType = fuelType;
        this.seats = seats;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(double pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {

        return brand
                + " "
                + model
                + " - RM "
                + String.format(
                        "%.2f",
                        pricePerDay
                )
                + "/day";
    }

    public Integer getMileage() { return mileage; }
    public void setMileage(Integer mileage) { this.mileage = mileage; }
    public java.time.LocalDate getNextServiceDate() { return nextServiceDate; }
    public void setNextServiceDate(java.time.LocalDate nextServiceDate) { this.nextServiceDate = nextServiceDate; }
}