package com.example.simpleclient;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class CustomerBookingsController {

    @FXML
    private VBox bookingContainer;

    @FXML
    private Label bookingCountLabel;

    @FXML
    private Label bookingStatusLabel;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
        new ObjectMapper()
                .findAndRegisterModules();

    @FXML
    private void initialize() {

        loadBookings();
    }

    @FXML
    private void onRefreshClicked() {

        loadBookings();
    }

    private void loadBookings() {

        String token =
                AppSession.getToken();

        bookingContainer
                .getChildren()
                .clear();

        if (token == null
                || token.isBlank()) {

            bookingCountLabel.setText(
                    "0 bookings"
            );

            bookingStatusLabel.setText(
                    "Sign in to view your bookings"
            );

            showEmptyState(
                    "Sign in required",
                    "Your bookings will appear here after you sign in."
            );

            return;
        }

        bookingStatusLabel.setText(
                "Loading bookings..."
        );

        Thread thread =
                new Thread(
                        () -> fetchBookings(
                                token
                        )
                );

        thread.setDaemon(true);

        thread.start();
    }

    private void fetchBookings(
            String token) {

        try {

            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            "http://localhost:8080/api/customer/bookings"
                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer "
                                    + token
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );

            if (response.statusCode() == 401) {

                AppSession.clearToken();

                Platform.runLater(
                        () -> {

                            bookingCountLabel.setText(
                                    "0 bookings"
                            );

                            bookingStatusLabel.setText(
                                    "Session expired"
                            );

                            showEmptyState(
                                    "Please sign in again",
                                    "Your previous login session has expired."
                            );
                        }
                );

                return;
            }

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Server returned HTTP "
                        + response.statusCode()
                );
            }

            Booking[] bookingArray =
                    mapper.readValue(
                            response.body(),
                            Booking[].class
                    );

            List<Booking> bookings =
                    Arrays.asList(
                            bookingArray
                    );

            Platform.runLater(
                    () -> renderBookings(
                            bookings
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        bookingCountLabel.setText(
                                "0 bookings"
                        );

                        bookingStatusLabel.setText(
                                "Unable to load bookings"
                        );

                        showEmptyState(
                                "Something went wrong",
                                "Unable to connect to the booking service."
                        );
                    }
            );
        }
    }

    private void renderBookings(
            List<Booking> bookings) {

        bookingContainer
                .getChildren()
                .clear();

        bookingCountLabel.setText(
                bookings.size()
                + (
                    bookings.size() == 1
                    ? " booking"
                    : " bookings"
                )
        );

        bookingStatusLabel.setText(
                "Your rental history"
        );

        if (bookings.isEmpty()) {

            showEmptyState(
                    "No bookings yet",
                    "When you rent a car, your booking will appear here."
            );

            return;
        }

        for (Booking booking : bookings) {

            bookingContainer
                    .getChildren()
                    .add(
                            createBookingCard(
                                    booking
                            )
                    );
        }
    }

    private VBox createBookingCard(
            Booking booking) {

        VBox card =
                new VBox();

        card.setSpacing(
                12
        );

        card.getStyleClass()
                .add(
                        "customer-booking-card"
                );

        Label bookingNumber =
                new Label(
                        "BOOKING #"
                        + booking.getId()
                );

        bookingNumber
                .getStyleClass()
                .add(
                        "customer-booking-number"
                );

        Label dates =
                new Label(
                        formatDates(
                                booking.getStartDate(),
                                booking.getEndDate()
                        )
                );

        dates.getStyleClass()
                .add(
                        "customer-booking-dates"
                );

        Label total =
                new Label(
                        String.format(
                                "RM %.2f",
                                booking.getTotalAmount()
                        )
                );

        total.getStyleClass()
                .add(
                        "customer-booking-total"
                );

        Label payment =
                createStatusLabel(
                        "Payment: "
                        + valueOr(
                                booking.getPaymentStatus(),
                                "UNKNOWN"
                        ),
                        booking.getPaymentStatus()
                );

        Label bookingStatus =
                createStatusLabel(
                        "Booking: "
                        + valueOr(
                                booking.getBookingStatus(),
                                "UNKNOWN"
                        ),
                        booking.getBookingStatus()
                );

        card.getChildren()
                .addAll(
                        bookingNumber,
                        dates,
                        total,
                        payment,
                        bookingStatus
                );

        Button detailsButton = new Button("View Details  →");
        detailsButton.getStyleClass().add("dash-secondary-button");
        detailsButton.setOnAction(event -> openBookingDetails(booking));
        card.getChildren().add(detailsButton);

        if ("PAID".equalsIgnoreCase(
                booking.getPaymentStatus()
        )) {

            Button cancelButton =
                    new Button(
                            "Cancel & Refund"
                    );

            cancelButton
                    .getStyleClass()
                    .add(
                            "customer-cancel-button"
                    );

            cancelButton.setOnAction(
                    event ->
                            cancelBooking(
                                    booking
                            )
            );

            card.getChildren()
                    .add(
                            cancelButton
                    );
        }

        return card;
    }

    private Label createStatusLabel(
            String text,
            String status) {

        Label label =
                new Label(
                        text
                );

        label.getStyleClass()
                .add(
                        "customer-booking-status"
                );

        if (status != null) {

            String normalized =
                    status.toUpperCase();

            if (normalized.equals(
                    "PAID"
            )
                    || normalized.equals(
                            "ACTIVE"
                    )) {

                label.getStyleClass()
                        .add(
                                "booking-status-success"
                        );

            } else if (
                    normalized.equals(
                            "CANCELLED"
                    )
                    || normalized.equals(
                            "REFUNDED"
                    )
                    || normalized.equals(
                            "EXPIRED"
                    )) {

                label.getStyleClass()
                        .add(
                                "booking-status-muted"
                        );

            } else {

                label.getStyleClass()
                        .add(
                                "booking-status-warning"
                        );
            }
        }

        return label;
    }

    private void cancelBooking(
            Booking booking) {

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Cancel Booking"
        );

        confirmation.setHeaderText(
                "Cancel booking #"
                + booking.getId()
                + "?"
        );

        confirmation.setContentText(
                "The backend will request a Stripe refund "
                + "for this paid booking."
        );

        confirmation
                .showAndWait()
                .ifPresent(
                        result -> {

                            if (result
                                    == javafx.scene.control.ButtonType.OK) {

                                sendCancellation(
                                        booking
                                );
                            }
                        }
                );
    }

    private void sendCancellation(
            Booking booking) {

        String token =
                AppSession.getToken();

        if (token == null
                || token.isBlank()) {

            loadBookings();

            return;
        }

        bookingStatusLabel.setText(
                "Cancelling booking..."
        );

        Thread thread =
                new Thread(
                        () -> {

                            try {

                                HttpRequest request =
                                        HttpRequest
                                                .newBuilder()
                                                .uri(
                                                        URI.create(
                                                                "http://localhost:8080"
                                                                + "/api/customer/bookings/"
                                                                + booking.getId()
                                                                + "/cancel"
                                                        )
                                                )
                                                .header(
                                                        "Authorization",
                                                        "Bearer "
                                                        + token
                                                )
                                                .PUT(
                                                        HttpRequest
                                                                .BodyPublishers
                                                                .noBody()
                                                )
                                                .build();

                                HttpResponse<String> response =
                                        httpClient.send(
                                                request,
                                                HttpResponse
                                                        .BodyHandlers
                                                        .ofString()
                                        );

                                if (response.statusCode()
                                        == 401) {

                                    AppSession.clearToken();

                                    Platform.runLater(
                                            this::loadBookings
                                    );

                                    return;
                                }

                                if (response.statusCode() < 200
                                        || response.statusCode() >= 300) {

                                    throw new RuntimeException(
                                            response.body()
                                    );
                                }

                                Platform.runLater(
                                        () -> {

                                            showAlert(
                                                    Alert.AlertType.INFORMATION,
                                                    "Booking Cancelled",
                                                    "The booking was cancelled "
                                                    + "and the refund request was processed."
                                            );

                                            loadBookings();
                                        }
                                );

                            } catch (Exception e) {

                                e.printStackTrace();

                                Platform.runLater(
                                        () -> {

                                            bookingStatusLabel.setText(
                                                    "Cancellation failed"
                                            );

                                            showAlert(
                                                    Alert.AlertType.ERROR,
                                                    "Unable to Cancel",
                                                    e.getMessage() == null
                                                            ? "Unable to cancel this booking."
                                                            : e.getMessage()
                                            );
                                        }
                                );
                            }
                        }
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }

    private void showEmptyState(
            String title,
            String description) {

        bookingContainer
                .getChildren()
                .clear();

        VBox empty =
                new VBox();

        empty.setSpacing(
                7
        );

        empty.getStyleClass()
                .add(
                        "customer-booking-empty"
                );

        Label icon =
                new Label(
                        "▣"
                );

        icon.getStyleClass()
                .add(
                        "customer-booking-empty-icon"
                );

        Label titleLabel =
                new Label(
                        title
                );

        titleLabel
                .getStyleClass()
                .add(
                        "customer-booking-empty-title"
                );

        Label descriptionLabel =
                new Label(
                        description
                );

        descriptionLabel.setWrapText(
                true
        );

        descriptionLabel
                .getStyleClass()
                .add(
                        "customer-booking-empty-description"
                );

        empty.getChildren()
                .addAll(
                        icon,
                        titleLabel,
                        descriptionLabel
                );

        bookingContainer
                .getChildren()
                .add(
                        empty
                );
    }

    private String formatDates(
            LocalDate start,
            LocalDate end) {

        if (start == null
                || end == null) {

            return "Rental dates unavailable";
        }

        return start
                + "  →  "
                + end;
    }

    private String valueOr(
            String value,
            String fallback) {

        if (value == null
                || value.isBlank()) {

            return fallback;
        }

        return value;
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert =
                new Alert(
                        type
                );

        alert.setTitle(
                title
        );

        alert.setHeaderText(
                null
        );

        alert.setContentText(
                message
        );

        alert.showAndWait();
    }
    private void openBookingDetails(Booking booking) {
        try {
            SelectedBookingSession.setSelectedBooking(booking);
            javafx.scene.Node page = javafx.fxml.FXMLLoader.load(getClass().getResource("booking-details-view.fxml"));
            javafx.scene.layout.StackPane host = (javafx.scene.layout.StackPane) bookingCountLabel.getScene().lookup("#contentArea");
            if (host != null) host.getChildren().setAll(page);
        } catch (Exception e) { e.printStackTrace(); }
    }

}