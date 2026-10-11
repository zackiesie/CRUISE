package com.example.simpleclient;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

public class CustomerShellController {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button discoverButton;

    @FXML
    private Button bookingsButton;

    @FXML
    private Button accountButton;

    @FXML
    private Button notificationsButton;

    @FXML
    private Label loginStatusLabel;

    @FXML
    private void initialize() {

        contentArea.getChildren().addListener((javafx.collections.ListChangeListener<Node>) change -> {
            if (contentArea.getChildren().isEmpty()) return;
            Node page = contentArea.getChildren().get(0);
            Button active = switch (String.valueOf(page.getId())) {
                case "page-fleet" -> discoverButton;
                case "page-bookings" -> bookingsButton;
                case "page-activity" -> notificationsButton;
                case "page-account" -> accountButton;
                default -> dashboardButton;
            };
            setActiveButton(active); updateLoginStatus();
        });
        updateLoginStatus();

        showDashboard();
    }

    @FXML
    private void onDashboardClicked() {
        showDashboard();
    }

    @FXML
    private void onDiscoverClicked() {

        showDiscover();
    }

    @FXML
    private void onBookingsClicked() {

        showPage(
                "customer-bookings-view.fxml"
        );

        setActiveButton(
                bookingsButton
        );
    }

    @FXML
    private void onNotificationsClicked() {
        showPage("customer-notifications-view.fxml");
        setActiveButton(notificationsButton);
    }

    @FXML
    private void onAccountClicked() {

        showPage(
                "customer-account-view.fxml"
        );

        setActiveButton(
                accountButton
        );
    }

    private void showDashboard() {
        showPage("customer-dashboard-view.fxml");
        setActiveButton(dashboardButton);
    }

    private void showDiscover() {

        showPage(
                "browse-cars-view.fxml"
        );

        setActiveButton(
                discoverButton
        );
    }

    private void showPage(
            String fxmlFile) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass()
                                    .getResource(
                                            fxmlFile
                                    )
                    );

            Node page =
                    loader.load();

            PageMotion.show(contentArea, page);

        } catch (IOException e) {

            e.printStackTrace();

            contentArea
                    .getChildren()
                    .clear();

            Label errorLabel =
                    new Label(
                            "Unable to load page."
                    );

            errorLabel.setStyle(
                    "-fx-text-fill: #ef4444;"
                    + "-fx-font-size: 14px;"
                    + "-fx-font-weight: bold;"
            );

            contentArea
                    .getChildren()
                    .add(
                            errorLabel
                    );
        }
    }

    private void setActiveButton(Button activeButton) {
        Button[] buttons = { dashboardButton, discoverButton, bookingsButton, notificationsButton, accountButton };
        for (Button button : buttons) button.getStyleClass().remove("nav-active");
        if (activeButton != null) activeButton.getStyleClass().add("nav-active");
    }

    private void updateLoginStatus() {

        String token =
                AppSession.getToken();

        if (token == null
                || token.isBlank()) {

            loginStatusLabel.setText(
                    "Guest"
            );

            return;
        }

        loginStatusLabel.setText(
                "Signed in"
        );
    }
    @FXML
    private void openAdminConsole() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("admin-login-view.fxml")
            );

            Parent root = loader.load();
            Stage stage = (Stage) contentArea.getScene().getWindow();
            WindowManager.show(stage, root, "CRUISE - Admin Login");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("How CRUISE works");
        alert.setHeaderText("Rent in four simple steps");
        alert.setContentText("1. Sign in with Google\n2. Discover a car and choose dates\n3. Select extras and pay securely\n4. Track the rental from My Bookings");
        alert.showAndWait();
    }

    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About CRUISE");
        alert.setHeaderText("CRUISE Car Rental Management System");
        alert.setContentText("A JavaFX desktop rental experience with customer booking, Stripe payment and fleet administration.");
        alert.showAndWait();
    }

}