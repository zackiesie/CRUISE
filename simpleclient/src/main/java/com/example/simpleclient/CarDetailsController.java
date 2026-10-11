package com.example.simpleclient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;



public class CarDetailsController {

    @FXML
    private Label brandLabel;

    @FXML
    private Label modelLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label transmissionLabel;

    @FXML
    private Label seatsLabel;

    @FXML
    private Label fuelLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Label priceLabel;

    @FXML
    private Label totalLabel;

    @FXML
    private Label rentalDaysLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private ImageView carImageView;

    @FXML
    private Label imagePlaceholder;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField phoneField;

    @FXML
    private Button paymentButton;

    @FXML private Button backButton;
    @FXML private CheckBox childSeatCheckBox;
    @FXML private CheckBox additionalDriverCheckBox;
    @FXML private RadioButton standardProtectionRadio;
    @FXML private RadioButton plusProtectionRadio;
    @FXML private RadioButton premiumProtectionRadio;
    @FXML private ToggleGroup protectionGroup;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper();

    private Car selectedCar;

    @FXML
    private void initialize() {

        selectedCar =
                SelectedCarSession.getSelectedCar();

        if (selectedCar == null) {

            statusLabel.setText(
                    "No vehicle selected."
            );

            paymentButton.setDisable(true);

            return;
        }

        showCar();

        startDatePicker.setValue(
                LocalDate.now().plusDays(1)
        );

        endDatePicker.setValue(
                LocalDate.now().plusDays(2)
        );

        startDatePicker
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                updateTotal()
                );

        endDatePicker
                .valueProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                updateTotal()
                );

        updateTotal();
    }

    private void showCar() {

        brandLabel.setText(
                valueOr(
                        selectedCar.getBrand(),
                        "Vehicle"
                ).toUpperCase()
        );

        modelLabel.setText(
                valueOr(
                        selectedCar.getModel(),
                        "Unknown model"
                )
        );

        categoryLabel.setText(
                valueOr(
                        selectedCar.getCategory(),
                        "Car"
                )
        );

        transmissionLabel.setText(
                valueOr(
                        selectedCar.getTransmission(),
                        "Automatic"
                )
        );

        seatsLabel.setText(
                selectedCar.getSeats() == null
                        ? "5 seats"
                        : selectedCar.getSeats()
                        + " seats"
        );

        fuelLabel.setText(
                valueOr(
                        selectedCar.getFuelType(),
                        "Petrol"
                )
        );

        descriptionLabel.setText(
                valueOr(
                        selectedCar.getDescription(),
                        "A comfortable and practical vehicle "
                        + "for your next journey."
                )
        );

        priceLabel.setText(
                String.format(
                        "RM %.2f",
                        selectedCar.getPricePerDay()
                )
        );

        loadImage();
    }

    private void loadImage() {

        String imageUrl =
                selectedCar.getImageUrl();

        if (imageUrl == null
                || imageUrl.isBlank()) {

            showImagePlaceholder();

            return;
        }

        try {

            String finalUrl =
                    normalizeImageUrl(
                            imageUrl
                    );

            Image image =
                    new Image(
                            finalUrl,
                            true
                    );

            carImageView.setImage(
                    image
            );

            carImageView.setVisible(
                    true
            );

            imagePlaceholder.setVisible(
                    false
            );

            image.errorProperty()
                    .addListener(
                            (
                                observable,
                                oldValue,
                                hasError
                            ) -> {

                                if (hasError) {

                                    Platform.runLater(
                                            this::showImagePlaceholder
                                    );
                                }
                            }
                    );

        } catch (Exception e) {

            showImagePlaceholder();
        }
    }

    private void showImagePlaceholder() {

        carImageView.setImage(
                null
        );

        carImageView.setVisible(
                false
        );

        imagePlaceholder.setVisible(
                true
        );

        imagePlaceholder.setText(
                "🚘\n"
                + valueOr(
                        selectedCar.getBrand(),
                        ""
                )
                + " "
                + valueOr(
                        selectedCar.getModel(),
                        ""
                )
        );
    }

    private void updateTotal() {

        if (selectedCar == null) {
            return;
        }

        LocalDate start =
                startDatePicker.getValue();

        LocalDate end =
                endDatePicker.getValue();

        if (start == null
                || end == null) {

            rentalDaysLabel.setText(
                    "Select your rental dates"
            );

            totalLabel.setText(
                    "RM 0.00"
            );

            paymentButton.setDisable(true);

            return;
        }

        long days =
                ChronoUnit.DAYS.between(
                        start,
                        end
                );

        if (days <= 0) {

            rentalDaysLabel.setText(
                    "End date must be after start date"
            );

            totalLabel.setText(
                    "RM 0.00"
            );

            paymentButton.setDisable(true);

            return;
        }

        double total = selectedCar.getPricePerDay() * days;
        if (childSeatCheckBox != null && childSeatCheckBox.isSelected()) total += 12.0 * days;
        if (additionalDriverCheckBox != null && additionalDriverCheckBox.isSelected()) total += 18.0 * days;
        if (plusProtectionRadio != null && plusProtectionRadio.isSelected()) total += 15.0 * days;
        if (premiumProtectionRadio != null && premiumProtectionRadio.isSelected()) total += 28.0 * days;

        rentalDaysLabel.setText(
                days
                + (
                    days == 1
                    ? " rental day"
                    : " rental days"
                )
        );

        totalLabel.setText(
                String.format(
                        "RM %.2f",
                        total
                )
        );

        paymentButton.setDisable(
                false
        );
    }

    @FXML
    private void onBackClicked() {
        try {
            SelectedCarSession.clear();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("customer-shell.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) totalLabel
                    .getScene()
                    .getWindow();

            WindowManager.show(stage, root, "CRUISE - Car Rental");

        } catch (IOException e) {
            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Navigation Error",
                    "Unable to return to Discover."
            );
        }
    }

    @FXML
    private void onOptionsChanged() {
        updateTotal();
    }

    private String selectedProtectionPlan() {
        if (premiumProtectionRadio != null && premiumProtectionRadio.isSelected()) return "PREMIUM";
        if (plusProtectionRadio != null && plusProtectionRadio.isSelected()) return "PLUS";
        return "STANDARD";
    }

    @FXML
    private void onPaymentClicked() {

        if (selectedCar == null) {
            return;
        }

        String appToken =
                AppSession.getToken();

        if (appToken == null
                || appToken.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Login Required",
                    "Please sign in with Google before making a booking."
            );

            return;
        }

        String phone =
                phoneField
                        .getText()
                        .trim();

        if (phone.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Phone Required",
                    "Please enter your phone number."
            );

            return;
        }

        LocalDate startDate =
                startDatePicker.getValue();

        LocalDate endDate =
                endDatePicker.getValue();

        if (startDate == null
                || endDate == null) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Dates Required",
                    "Please select your rental dates."
            );

            return;
        }

        if (!endDate.isAfter(
                startDate
        )) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Dates",
                    "End date must be after the start date."
            );

            return;
        }

        if (startDate.isBefore(
                LocalDate.now()
        )) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Invalid Start Date",
                    "Start date cannot be in the past."
            );

            return;
        }

        paymentButton.setDisable(
                true
        );

        statusLabel.setText(
                "Preparing secure checkout..."
        );

        Thread thread =
                new Thread(
                        () ->
                                createCheckout(
                                        appToken,
                                        phone,
                                        startDate,
                                        endDate
                                )
                );

        thread.setDaemon(
                true
        );

        thread.start();
    }

    private void createCheckout(
            String appToken,
            String phone,
            LocalDate startDate,
            LocalDate endDate) {

        try {

            String url =
                    "http://localhost:8080/api/checkout/create"
                    + "?carId="
                    + selectedCar.getId()
                    + "&customerPhone="
                    + encode(phone)
                    + "&startDate="
                    + startDate
                    + "&endDate=" + endDate
                    + "&childSeat=" + (childSeatCheckBox != null && childSeatCheckBox.isSelected())
                    + "&additionalDriver=" + (additionalDriverCheckBox != null && additionalDriverCheckBox.isSelected())
                    + "&protectionPlan=" + encode(selectedProtectionPlan());

            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            url
                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer "
                                    + appToken
                            )
                            .POST(
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

            if (response.statusCode() == 401) {

                AppSession.clearToken();

                Platform.runLater(
                        () -> {

                            statusLabel.setText(
                                    "Your login has expired."
                            );

                            paymentButton.setDisable(
                                    false
                            );

                            showAlert(
                                    Alert.AlertType.WARNING,
                                    "Session Expired",
                                    "Please sign in with Google again."
                            );
                        }
                );

                return;
            }

            if (response.statusCode() == 409) {

                Platform.runLater(
                        () -> {

                            statusLabel.setText(
                                    "This car is unavailable for those dates."
                            );

                            paymentButton.setDisable(
                                    false
                            );

                            showAlert(
                                    Alert.AlertType.WARNING,
                                    "Car Unavailable",
                                    "This vehicle is already reserved "
                                    + "for the selected dates. "
                                    + "Please choose different dates."
                            );
                        }
                );

                return;
            }

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Checkout failed. HTTP "
                        + response.statusCode()
                        + "\n"
                        + response.body()
                );
            }

            JsonNode json =
                    mapper.readTree(
                            response.body()
                    );

            String checkoutUrl =
                    json.path(
                            "checkoutUrl"
                    )
                    .asText();

            int bookingId =
                    json.path("bookingId").asInt(-1);

            if (checkoutUrl.isBlank()) {

                throw new RuntimeException(
                        "Checkout URL was not returned by server."
                );
            }

            Platform.runLater(
                    () -> {

                        statusLabel.setText(
                                "Checkout ready. Opening Stripe..."
                        );

                        openBrowser(
                                checkoutUrl
                        );

                        statusLabel.setText(
                                "Waiting for payment confirmation..."
                        );

                        if (bookingId > 0) {
                            watchPaymentStatus(appToken, bookingId);
                        } else {
                            paymentButton.setDisable(false);
                        }
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        statusLabel.setText(
                                "Unable to create checkout."
                        );

                        paymentButton.setDisable(
                                false
                        );

                        showAlert(
                                Alert.AlertType.ERROR,
                                "Checkout Error",
                                e.getMessage() == null
                                        ? "Unable to connect to the server."
                                        : e.getMessage()
                        );
                    }
            );
        }
    }

    private void openBrowser(
            String url) {

        try {

            new ProcessBuilder(
                    "cmd",
                    "/c",
                    "start",
                    "",
                    url
            ).start();

        } catch (Exception e) {

            showAlert(
                    Alert.AlertType.ERROR,
                    "Browser Error",
                    "Checkout was created, but the browser could not be opened."
            );
        }
    }

    private void watchPaymentStatus(String appToken, int bookingId) {
        Thread watcher = new Thread(() -> {
            for (int attempt = 0; attempt < 120; attempt++) {
                try {
                    Thread.sleep(2500);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/customer/bookings"))
                            .header("Authorization", "Bearer " + appToken)
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

                    if (response.statusCode() == 401) {
                        AppSession.clearToken();
                        Platform.runLater(() -> {
                            paymentButton.setDisable(false);
                            statusLabel.setText("Session expired. Please sign in again.");
                        });
                        return;
                    }

                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        continue;
                    }

                    Booking[] items = new ObjectMapper()
                            .findAndRegisterModules()
                            .readValue(response.body(), Booking[].class);

                    Booking matched = Arrays.stream(items)
                            .filter(b -> b.getId() != null && b.getId() == bookingId)
                            .findFirst()
                            .orElse(null);

                    if (matched == null) {
                        continue;
                    }

                    String payment = matched.getPaymentStatus();
                    String booking = matched.getBookingStatus();

                    if ("PAID".equalsIgnoreCase(payment)) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Payment confirmed — your car is booked!");
                            paymentButton.setDisable(false);
                            showPaymentSuccess(matched);
                        });
                        return;
                    }

                    if ("EXPIRED".equalsIgnoreCase(booking)
                            || "CANCELLED".equalsIgnoreCase(booking)
                            || "REFUNDED".equalsIgnoreCase(payment)) {
                        Platform.runLater(() -> {
                            paymentButton.setDisable(false);
                            statusLabel.setText("Payment was not completed.");
                        });
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ignored) {
                    // Keep polling briefly; browser checkout may still be in progress.
                }
            }

            Platform.runLater(() -> {
                paymentButton.setDisable(false);
                statusLabel.setText("Checkout opened. Refresh My Bookings to confirm payment.");
            });
        });

        watcher.setDaemon(true);
        watcher.start();
    }

    private void showPaymentSuccess(Booking booking) {
        Stage owner = (Stage) totalLabel.getScene().getWindow();
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initOwner(owner);
        popup.initModality(Modality.WINDOW_MODAL);

        Label check = new Label("✓");
        check.getStyleClass().add("payment-success-check");

        Label eyebrow = new Label("PAYMENT CONFIRMED");
        eyebrow.getStyleClass().add("payment-success-eyebrow");

        Label title = new Label("You're ready to drive!");
        title.getStyleClass().add("payment-success-title");

        Label message = new Label(
                "Booking #" + booking.getId()
                + " is confirmed. Your payment of "
                + String.format("RM %.2f", booking.getTotalAmount())
                + " was received successfully."
        );
        message.setWrapText(true);
        message.setMaxWidth(360);
        message.getStyleClass().add("payment-success-message");

        Label dates = new Label(
                booking.getStartDate() + "  →  " + booking.getEndDate()
        );
        dates.getStyleClass().add("payment-success-dates");

        Button viewBooking = new Button("View Booking  →");
        viewBooking.getStyleClass().add("payment-success-button");
        viewBooking.setOnAction(event -> {
            popup.close();
            SelectedBookingSession.setSelectedBooking(booking);
            navigateAfterPayment("booking-details-view.fxml");
        });

        Button dashboard = new Button("Back to Dashboard");
        dashboard.getStyleClass().add("payment-success-secondary-button");
        dashboard.setOnAction(event -> {
            popup.close();
            navigateAfterPayment("customer-dashboard-view.fxml");
        });

        javafx.scene.layout.HBox actions = new javafx.scene.layout.HBox(10, dashboard, viewBooking);
        actions.setAlignment(javafx.geometry.Pos.CENTER);
        VBox card = new VBox(12, check, eyebrow, title, message, dates, actions);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.getStyleClass().add("payment-success-card");

        StackPane root = new StackPane(card);
        root.setPadding(new javafx.geometry.Insets(30));
        root.getStyleClass().add("payment-success-overlay");

        Scene scene = new Scene(root, 500, 470);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add(
                getClass().getResource("styles.css").toExternalForm()
        );
        popup.setScene(scene);

        card.setScaleX(0.78);
        card.setScaleY(0.78);
        card.setOpacity(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(330), card);
        scale.setToX(1);
        scale.setToY(1);

        FadeTransition fade = new FadeTransition(Duration.millis(260), card);
        fade.setToValue(1);

        popup.show();
        new ParallelTransition(scale, fade).play();
    }

    private void navigateAfterPayment(String fxml) {
        try {
            FXMLLoader shellLoader = new FXMLLoader(getClass().getResource("customer-shell.fxml"));
            Parent root = shellLoader.load();
            Stage stage = (Stage) totalLabel.getScene().getWindow();
            WindowManager.show(stage, root, "CRUISE - Car Rental");
            StackPane host = (StackPane) root.lookup("#contentArea");
            if (host != null) {
                Parent page = FXMLLoader.load(getClass().getResource(fxml));
                host.getChildren().setAll(page);
            }
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String normalizeImageUrl(
            String imageUrl) {

        String value =
                imageUrl.trim();

        if (value.startsWith(
                "http://"
        )
                || value.startsWith(
                        "https://"
                )) {

            return value;
        }

        if (!value.startsWith(
                "/"
        )) {

            value =
                    "/"
                    + value;
        }

        return "http://localhost:8080"
                + value;
    }

    private String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
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
}
