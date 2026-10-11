package com.example.simpleclient;

public final class SelectedCarSession {

    private static Car selectedCar;

    private SelectedCarSession() {
    }

    public static Car getSelectedCar() {
        return selectedCar;
    }

    public static void setSelectedCar(Car car) {
        selectedCar = car;
    }

    public static void clear() {
        selectedCar = null;
    }
}