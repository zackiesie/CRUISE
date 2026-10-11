package com.example.simpleclient;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class AdminBookingsController {

    private static final String API_URL =
        "http://localhost:8080/api/admin/bookings";

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> paymentFilter;

    @FXML
    private ComboBox<String> bookingFilter;

    @FXML
    private VBox bookingContainer;

    @FXML
    private Label totalBookingsLabel;

    @FXML
    private Label paidBookingsLabel;

    @FXML
    private Label activeBookingsLabel;

    @FXML
    private Label resultCountLabel;

    @FXML
    private Label bookingPageStatusLabel;

    @FXML
    private Button refreshButton;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    private final List<Booking> allBookings =
            new ArrayList<>();

    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy"
            );

    @FXML
    private void initialize() {

        paymentFilter
                .getItems()
                .addAll(
                        "All Payments",
                        "PAID",
                        "PENDING",
                        "PENDING_PAYMENT",
                        "REFUNDED"
                );

        bookingFilter
                .getItems()
                .addAll(
                        "All Bookings",
                        "RESERVED",
                        "ACTIVE",
                        "CANCELLED",
                        "EXPIRED"
                );

        paymentFilter.setValue(
                "All Payments"
        );

        bookingFilter.setValue(
                "All Bookings"
        );

        searchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );

        paymentFilter
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );

        bookingFilter
                .valueProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyFilters()
                );

        loadBookings();
    }

    @FXML
    private void refreshBookings() {

        loadBookings();
    }

    private void loadBookings() {

        refreshButton.setDisable(
                true
        );

        bookingPageStatusLabel.setText(
                "Loading bookings..."
        );

        Thread thread =
                new Thread(
                        this::fetchBookings
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void fetchBookings() {

        try {

            HttpRequest request =
            HttpRequest
                    .newBuilder()
                    .uri(
                            URI.create(
                                    API_URL
                            )
                    )
                    .header(
                            "Authorization",
                            "Bearer "
                            + AdminSession.getToken()
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

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "HTTP "
                        + response.statusCode()
                );
            }

            Booking[] bookings =
                    mapper.readValue(
                            response.body(),
                            Booking[].class
                    );

            List<Booking> loaded =
                    new ArrayList<>(
                            Arrays.asList(
                                    bookings
                            )
                    );

            loaded.sort(
                    Comparator.comparing(
                            Booking::getId,
                            Comparator.nullsLast(
                                    Comparator.reverseOrder()
                            )
                    )
            );

            Platform.runLater(
                    () -> {

                        allBookings.clear();

                        allBookings.addAll(
                                loaded
                        );

                        refreshButton.setDisable(
                                false
                        );

                        updateStatistics();

                        applyFilters();

                        bookingPageStatusLabel.setText(
                                "Booking data is up to date."
                        );
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        refreshButton.setDisable(
                                false
                        );

                        bookingContainer
                                .getChildren()
                                .clear();

                        Label error =
                                new Label(
                                        "Unable to load bookings."
                                );

                        error.getStyleClass()
                                .add(
                                        "admin-booking-empty"
                                );

                        bookingContainer
                                .getChildren()
                                .add(error);

                        resultCountLabel.setText(
                                "0 results"
                        );

                        bookingPageStatusLabel.setText(
                                "Unable to connect to backend."
                        );
                    }
            );
        }
    }

    private void updateStatistics() {

        int total =
                allBookings.size();

        long paid =
                allBookings
                        .stream()
                        .filter(
                                booking ->
                                        equalsIgnoreCase(
                                                booking.getPaymentStatus(),
                                                "PAID"
                                        )
                        )
                        .count();

        long active =
                allBookings
                        .stream()
                        .filter(
                                booking ->
                                        equalsIgnoreCase(
                                                booking.getBookingStatus(),
                                                "ACTIVE"
                                        )
                        )
                        .count();

        totalBookingsLabel.setText(
                String.valueOf(total)
        );

        paidBookingsLabel.setText(
                String.valueOf(paid)
        );

        activeBookingsLabel.setText(
                String.valueOf(active)
        );
    }

    private void applyFilters() {

        if (bookingContainer == null) {
            return;
        }

        String search =
                searchField
                        .getText()
                        .trim()
                        .toLowerCase();

        String selectedPayment =
                paymentFilter.getValue();

        String selectedBooking =
                bookingFilter.getValue();

        List<Booking> filtered =
                allBookings
                        .stream()
                        .filter(
                                booking ->
                                        matchesSearch(
                                                booking,
                                                search
                                        )
                        )
                        .filter(
                                booking ->
                                        matchesPayment(
                                                booking,
                                                selectedPayment
                                        )
                        )
                        .filter(
                                booking ->
                                        matchesBookingStatus(
                                                booking,
                                                selectedBooking
                                        )
                        )
                        .toList();

        renderBookings(
                filtered
        );
    }

    private boolean matchesSearch(
            Booking booking,
            String search) {

        if (search == null
                || search.isBlank()) {

            return true;
        }

        return contains(
                booking.getCustomerName(),
                search
        )
                || contains(
                        booking.getCustomerEmail(),
                        search
                )
                || contains(
                        booking.getCustomerPhone(),
                        search
                )
                || contains(
                        booking.getPaymentStatus(),
                        search
                )
                || contains(
                        booking.getBookingStatus(),
                        search
                )
                || contains(
                        booking.getId(),
                        search
                )
                || contains(
                        booking.getCarId(),
                        search
                )
                || contains(
                        booking.getCustomerId(),
                        search
                );
    }

    private boolean matchesPayment(
            Booking booking,
            String filter) {

        if (filter == null
                || filter.equals(
                        "All Payments"
                )) {

            return true;
        }

        return equalsIgnoreCase(
                booking.getPaymentStatus(),
                filter
        );
    }

    private boolean matchesBookingStatus(
            Booking booking,
            String filter) {

        if (filter == null
                || filter.equals(
                        "All Bookings"
                )) {

            return true;
        }

        return equalsIgnoreCase(
                booking.getBookingStatus(),
                filter
        );
    }

    private void renderBookings(
            List<Booking> bookings) {

        bookingContainer
                .getChildren()
                .clear();

        resultCountLabel.setText(
                bookings.size()
                + (
                        bookings.size() == 1
                                ? " result"
                                : " results"
                )
        );

        if (bookings.isEmpty()) {

            VBox emptyBox =
                    new VBox(8);

            emptyBox.setAlignment(
                    Pos.CENTER
            );

            emptyBox.setPadding(
                    new Insets(
                            55
                    )
            );

            emptyBox.getStyleClass()
                    .add(
                            "admin-booking-empty-box"
                    );

            Label icon =
                    new Label(
                            "▣"
                    );

            icon.getStyleClass()
                    .add(
                            "admin-booking-empty-icon"
                    );

            Label title =
                    new Label(
                            "No bookings found"
                    );

            title.getStyleClass()
                    .add(
                            "admin-booking-empty-title"
                    );

            Label description =
                    new Label(
                            "Try changing your search or filters."
                    );

            description
                    .getStyleClass()
                    .add(
                            "admin-booking-empty"
                    );

            emptyBox
                    .getChildren()
                    .addAll(
                            icon,
                            title,
                            description
                    );

            bookingContainer
                    .getChildren()
                    .add(
                            emptyBox
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
                new VBox(18);

        card.getStyleClass()
                .add(
                        "admin-booking-card"
                );

        /*
         * HEADER
         */
        HBox header =
                new HBox(12);

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        VBox titleBox =
                new VBox(3);

        Label bookingId =
                new Label(
                        "Booking #"
                        + safeNumber(
                                booking.getId()
                        )
                );

        bookingId.getStyleClass()
                .add(
                        "admin-booking-id"
                );

        Label customer =
                new Label(
                        display(
                                booking.getCustomerName(),
                                "Unknown customer"
                        )
                );

        customer.getStyleClass()
                .add(
                        "admin-booking-customer"
                );

        titleBox
                .getChildren()
                .addAll(
                        bookingId,
                        customer
                );

        Region spacer =
                new Region();

        HBox.setHgrow(
                spacer,
                javafx.scene.layout.Priority.ALWAYS
        );

        Label paymentBadge =
                createBadge(
                        display(
                                booking.getPaymentStatus(),
                                "UNKNOWN"
                        ),
                        "payment"
                );

        Label bookingBadge =
                createBadge(
                        display(
                                booking.getBookingStatus(),
                                "UNKNOWN"
                        ),
                        "booking"
                );

        header
                .getChildren()
                .addAll(
                        titleBox,
                        spacer,
                        paymentBadge,
                        bookingBadge
                );

        /*
         * INFORMATION
         */
        HBox information =
                new HBox(30);

        information.setAlignment(
                Pos.TOP_LEFT
        );

        VBox customerInfo =
                createInfoSection(
                        "CUSTOMER",
                        new String[][]{
                            {
                                "Email",
                                display(
                                        booking.getCustomerEmail(),
                                        "—"
                                )
                            },
                            {
                                "Phone",
                                display(
                                        booking.getCustomerPhone(),
                                        "—"
                                )
                            },
                            {
                                "Customer ID",
                                safeNumber(
                                        booking.getCustomerId()
                                )
                            }
                        }
                );

        VBox rentalInfo =
                createInfoSection(
                        "RENTAL",
                        new String[][]{
                            {
                                "Car ID",
                                safeNumber(
                                        booking.getCarId()
                                )
                            },
                            {
                                "Start",
                                formatDate(
                                        booking.getStartDate()
                                )
                            },
                            {
                                "End",
                                formatDate(
                                        booking.getEndDate()
                                )
                            },
                            {
                                "Duration",
                                booking.getDays() == null
                                        ? "—"
                                        : booking.getDays()
                                        + " day(s)"
                            }
                        }
                );

        VBox paymentInfo =
                createInfoSection(
                        "PAYMENT",
                        new String[][]{
                            {
                                "Total",
                                "RM "
                                + String.format(
                                        "%.2f",
                                        booking.getTotalAmount()
                                )
                            },
                            {
                                "Payment",
                                display(
                                        booking.getPaymentStatus(),
                                        "—"
                                )
                            },
                            {
                                "Booking",
                                display(
                                        booking.getBookingStatus(),
                                        "—"
                                )
                            }
                        }
                );

        information
                .getChildren()
                .addAll(
                        customerInfo,
                        rentalInfo,
                        paymentInfo
                );

        card.getChildren()
                .addAll(
                        header,
                        information
                );

        if (equalsIgnoreCase(booking.getPaymentStatus(), "PAID")
                && !equalsIgnoreCase(booking.getBookingStatus(), "COMPLETED")
                && !equalsIgnoreCase(booking.getBookingStatus(), "CANCELLED")) {
            Button complete = new Button("Mark Rental Completed");
            complete.getStyleClass().add("admin-primary-button");
            complete.setOnAction(event -> completeRental(booking));
            card.getChildren().add(complete);
        }

        return card;
    }

    private VBox createInfoSection(
            String title,
            String[][] values) {

        VBox section =
                new VBox(7);

        section.setPrefWidth(
                220
        );

        Label heading =
                new Label(title);

        heading.getStyleClass()
                .add(
                        "admin-booking-section-title"
                );

        section
                .getChildren()
                .add(
                        heading
                );

        for (String[] value : values) {

            HBox row =
                    new HBox(8);

            Label key =
                    new Label(
                            value[0] + ":"
                    );

            key.getStyleClass()
                    .add(
                            "admin-booking-key"
                    );

            Label data =
                    new Label(
                            value[1]
                    );

            data.setWrapText(
                    true
            );

            data.getStyleClass()
                    .add(
                            "admin-booking-value"
                    );

            row.getChildren()
                    .addAll(
                            key,
                            data
                    );

            section
                    .getChildren()
                    .add(
                            row
                    );
        }

        return section;
    }

    private Label createBadge(
            String value,
            String type) {

        Label badge =
                new Label(value);

        badge.getStyleClass()
                .add(
                        "admin-status-badge"
                );

        String status =
                value.toUpperCase();

        if (status.equals("PAID")
                || status.equals("ACTIVE")) {

            badge.getStyleClass()
                    .add(
                            "admin-status-success"
                    );

        } else if (status.equals("REFUNDED")
                || status.equals("CANCELLED")
                || status.equals("EXPIRED")) {

            badge.getStyleClass()
                    .add(
                            "admin-status-muted"
                    );

        } else {

            badge.getStyleClass()
                    .add(
                            "admin-status-warning"
                    );
        }

        return badge;
    }

    private String formatDate(
            java.time.LocalDate date) {

        if (date == null) {
            return "—";
        }

        return date.format(
                dateFormatter
        );
    }

    private boolean contains(
            Object value,
            String search) {

        if (value == null) {
            return false;
        }

        return String.valueOf(
                value
        )
                .toLowerCase()
                .contains(search);
    }

    private boolean equalsIgnoreCase(
            String first,
            String second) {

        return first != null
                && second != null
                && first.equalsIgnoreCase(
                        second
                );
    }

    private String display(
            String value,
            String fallback) {

        if (value == null
                || value.isBlank()) {

            return fallback;
        }

        return value;
    }

    private String safeNumber(
            Integer value) {

        return value == null
                ? "—"
                : String.valueOf(value);
    }
    private void completeRental(Booking booking) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Process Vehicle Return");
        dialog.setHeaderText("Complete booking #" + booking.getId());

        TextField mileageField = new TextField();
        mileageField.setPromptText("e.g. 45210");
        ComboBox<String> fuel = new ComboBox<>();
        fuel.getItems().addAll("FULL", "3/4", "1/2", "1/4", "EMPTY");
        fuel.setValue("FULL");
        TextArea notes = new TextArea();
        notes.setPromptText("Return condition, damage notes or remarks");
        notes.setPrefRowCount(3);

        VBox form = new VBox(8, new Label("RETURN MILEAGE"), mileageField,
                new Label("FUEL LEVEL"), fuel, new Label("RETURN NOTES"), notes);
        form.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(form);
        ButtonType complete = new ButtonType("Complete Return", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(complete, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != complete) return;
            Integer mileage = null;
            try { if (!mileageField.getText().isBlank()) mileage = Integer.valueOf(mileageField.getText().trim()); }
            catch (NumberFormatException ex) { bookingPageStatusLabel.setText("Mileage must be a whole number."); return; }
            final Integer finalMileage = mileage;
            new Thread(() -> {
                try {
                    String url = API_URL + "/" + booking.getId() + "/complete"
                            + "?fuelLevel=" + java.net.URLEncoder.encode(fuel.getValue(), java.nio.charset.StandardCharsets.UTF_8)
                            + "&notes=" + java.net.URLEncoder.encode(notes.getText(), java.nio.charset.StandardCharsets.UTF_8)
                            + (finalMileage == null ? "" : "&mileage=" + finalMileage);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Authorization", "Bearer " + AdminSession.getToken())
                            .PUT(HttpRequest.BodyPublishers.noBody()).build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() >= 200 && response.statusCode() < 300)
                        Platform.runLater(() -> { bookingPageStatusLabel.setText("Return processed successfully."); loadBookings(); });
                    else Platform.runLater(() -> bookingPageStatusLabel.setText("Unable to complete rental: " + response.body()));
                } catch (Exception e) { Platform.runLater(() -> bookingPageStatusLabel.setText("Unable to connect to backend.")); }
            }).start();
        });
    }

}