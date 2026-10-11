package com.example.simpleclient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class AdminShellController {

    private static final String ADMIN_ME_URL =
            "http://localhost:8080/api/admin/auth/me";

    private static final String ADMIN_LOGOUT_URL =
            "http://localhost:8080/api/admin/auth/logout";

    @FXML
    private StackPane contentArea;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button carsButton;

    @FXML
    private Button bookingsButton;

    @FXML private Button customersButton;
    @FXML private Button reportsButton;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    @FXML
    private void initialize() {

        /*
         * First check whether a token exists locally.
         */
        if (!AdminSession.hasToken()) {

            Platform.runLater(
                    this::goToAdminLogin
            );

            return;
        }

        /*
         * Do not load admin pages until the backend
         * confirms that the token is valid.
         */
        verifyAdminSession();
    }

    private void verifyAdminSession() {

        Thread thread =
                new Thread(
                        () -> {

                            try {

                                String token =
                                        AdminSession.getToken();

                                HttpRequest request =
                                        HttpRequest
                                                .newBuilder()
                                                .uri(
                                                        URI.create(
                                                                ADMIN_ME_URL
                                                        )
                                                )
                                                .header(
                                                        "Authorization",
                                                        "Bearer " + token
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

                                if (response.statusCode() >= 200
                                        && response.statusCode() < 300) {

                                    Platform.runLater(
                                            this::showDashboard
                                    );

                                } else {

                                    AdminSession.clearToken();

                                    Platform.runLater(
                                            this::goToAdminLogin
                                    );
                                }

                            } catch (Exception e) {

                                e.printStackTrace();

                                /*
                                 * Backend cannot verify the session,
                                 * so do not allow access to admin pages.
                                 */
                                Platform.runLater(
                                        this::goToAdminLogin
                                );
                            }
                        }
                );

        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void showDashboard() {

        if (!AdminSession.hasToken()) {

            goToAdminLogin();
            return;
        }

        showPage(
                "admin-dashboard-view.fxml",
                dashboardButton
        );
    }

    @FXML
    private void showCars() {

        if (!AdminSession.hasToken()) {

            goToAdminLogin();
            return;
        }

        showPage(
                "admin-cars-view.fxml",
                carsButton
        );
    }

    @FXML
    private void showBookings() {

        if (!AdminSession.hasToken()) {

            goToAdminLogin();
            return;
        }

        showPage(
                "admin-bookings-view.fxml",
                bookingsButton
        );
    }

    @FXML private void showCustomers() { if (!AdminSession.hasToken()) { goToAdminLogin(); return; } showPage("admin-customers-view.fxml", customersButton); }

    @FXML private void showReports() { if (!AdminSession.hasToken()) { goToAdminLogin(); return; } showPage("admin-reports-view.fxml", reportsButton); }

    @FXML
    private void backToCustomer() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "customer-shell.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            Stage stage =
                    (Stage) contentArea
                            .getScene()
                            .getWindow();

            WindowManager.show(stage, root, "CRUISE - Car Rental");

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @FXML
    private void logoutAdmin() {

        String token =
                AdminSession.getToken();

        /*
         * Clear the local session immediately.
         */
        AdminSession.clearToken();

        /*
         * Revoke the token on the backend too.
         */
        if (token != null
                && !token.isBlank()) {

            Thread thread =
                    new Thread(
                            () -> {

                                try {

                                    HttpRequest request =
                                            HttpRequest
                                                    .newBuilder()
                                                    .uri(
                                                            URI.create(
                                                                    ADMIN_LOGOUT_URL
                                                            )
                                                    )
                                                    .header(
                                                            "Authorization",
                                                            "Bearer " + token
                                                    )
                                                    .POST(
                                                            HttpRequest
                                                                    .BodyPublishers
                                                                    .noBody()
                                                    )
                                                    .build();

                                    httpClient.send(
                                            request,
                                            HttpResponse
                                                    .BodyHandlers
                                                    .ofString()
                                    );

                                } catch (Exception e) {

                                    /*
                                     * Local token is already cleared.
                                     * Logout should still continue even
                                     * if backend is temporarily unavailable.
                                     */
                                    e.printStackTrace();
                                }
                            }
                    );

            thread.setDaemon(true);
            thread.start();
        }

        goToAdminLogin();
    }

    private void showPage(
            String fxml,
            Button activeButton) {

        /*
         * Prevent loading an admin page
         * when there is no local session.
         */
        if (!AdminSession.hasToken()) {

            goToAdminLogin();
            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    fxml
                            )
                    );

            Parent page =
                    loader.load();

            PageMotion.show(contentArea, page);

            setActiveButton(
                    activeButton
            );

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private void setActiveButton(
            Button activeButton) {

        dashboardButton
                .getStyleClass()
                .remove(
                        "admin-nav-active"
                );

        carsButton
                .getStyleClass()
                .remove(
                        "admin-nav-active"
                );

        bookingsButton.getStyleClass().remove("admin-nav-active");
        customersButton.getStyleClass().remove("admin-nav-active");
        reportsButton.getStyleClass().remove("admin-nav-active");

        if (activeButton != null
                && !activeButton
                        .getStyleClass()
                        .contains(
                                "admin-nav-active"
                        )) {

            activeButton
                    .getStyleClass()
                    .add(
                            "admin-nav-active"
                    );
        }
    }

    private void goToAdminLogin() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "admin-login-view.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            Stage stage =
                    (Stage) contentArea
                            .getScene()
                            .getWindow();

            WindowManager.show(stage, root, "CRUISE - Admin Login");

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}