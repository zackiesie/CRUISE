package com.example.simpleclient;

public final class SelectedBookingSession {
    private static Booking selectedBooking;
    private SelectedBookingSession() {}
    public static Booking getSelectedBooking() { return selectedBooking; }
    public static void setSelectedBooking(Booking booking) { selectedBooking = booking; }
    public static void clear() { selectedBooking = null; }
}
