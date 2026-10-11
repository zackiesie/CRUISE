package com.example.simpleclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.net.URI;
import java.net.http.*;
import java.util.Arrays;
import java.util.Comparator;

public class CustomerNotificationsController {
    @FXML private VBox notificationContainer;
    @FXML private Label notificationStatusLabel;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @FXML private void initialize() {
        if (!AppSession.hasToken()) {
            notificationStatusLabel.setText("Sign in to see booking activity.");
            return;
        }
        new Thread(this::load).start();
    }

    private void load() {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:8080/api/customer/bookings"))
                    .header("Authorization", "Bearer " + AppSession.getToken()).GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 401) { AppSession.clearToken(); return; }
            Booking[] items = mapper.readValue(res.body(), Booking[].class);
            Arrays.sort(items, Comparator.comparing(Booking::getId, Comparator.nullsLast(Comparator.reverseOrder())));
            Platform.runLater(() -> render(items));
        } catch (Exception e) {
            Platform.runLater(() -> notificationStatusLabel.setText("Unable to load activity right now."));
        }
    }

    private void render(Booking[] items) {
        notificationContainer.getChildren().clear();
        if (items.length == 0) { notificationStatusLabel.setText("No activity yet."); return; }
        notificationStatusLabel.setText(items.length + " booking update" + (items.length == 1 ? "" : "s"));
        for (Booking b : items) {
            String title = "PAID".equalsIgnoreCase(b.getPaymentStatus()) ? "Payment confirmed" : "Booking update";
            if ("COMPLETED".equalsIgnoreCase(b.getBookingStatus())) title = "Rental completed";
            if ("CANCELLED".equalsIgnoreCase(b.getBookingStatus())) title = "Booking cancelled";
            Label t = new Label(title + "  •  Booking #" + b.getId()); t.getStyleClass().add("notification-title");
            Label d = new Label((b.getBookingStatus()==null?"—":b.getBookingStatus()) + "  •  " + (b.getPaymentStatus()==null?"—":b.getPaymentStatus()) + "  •  RM " + String.format("%.2f", b.getTotalAmount()));
            d.getStyleClass().add("notification-copy");
            VBox card = new VBox(6, t, d); card.getStyleClass().add("notification-card");
            notificationContainer.getChildren().add(card);
        }
    }
}
