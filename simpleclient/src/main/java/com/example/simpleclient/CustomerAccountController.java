package com.example.simpleclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class CustomerAccountController {

    @FXML
    private Label accountStateLabel;

    @FXML
    private Label accountDescriptionLabel;

    @FXML
    private Label customerNameLabel;

    @FXML
    private Label customerEmailLabel;

    @FXML
    private Label customerIdLabel;

    @FXML
    private Label accountStatusLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button logoutButton;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    @FXML
    private void initialize() {

        refreshAccount();
    }

    private void refreshAccount() {

        String token =
                AppSession.getToken();

        if (token == null
                || token.isBlank()) {

            showGuestState();

            return;
        }

        showLoadingState();

        Thread thread =
                new Thread(
                        () -> loadCustomer(
                                token
                        )
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void loadCustomer(
            String token) {

        try {

            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            "http://localhost:8080/api/customer/me"
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

            if (response.statusCode() == 401) {

                AppSession.clearToken();

                Platform.runLater(
                        this::showGuestState
                );

                return;
            }

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Server returned HTTP "
                        + response.statusCode()
                        + ": "
                        + response.body()
                );
            }

            JsonNode json =
                    mapper.readTree(
                            response.body()
                    );

            Platform.runLater(
                    () -> showSignedInState(
                            json
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        accountStateLabel.setText(
                                "Unable to load account"
                        );

                        accountDescriptionLabel.setText(
                                "CRUISE could not connect to the account service."
                        );

                        accountStatusLabel.setText(
                                "Connection error"
                        );

                        loginButton.setDisable(false);
                        logoutButton.setDisable(false);

                        showAlert(
                                Alert.AlertType.ERROR,
                                "Account Error",
                                e.getMessage() == null
                                        ? "Unable to load your account."
                                        : e.getMessage()
                        );
                    }
            );
        }
    }

    private void showGuestState() {

        accountStateLabel.setText(
                "Not signed in"
        );

        accountDescriptionLabel.setText(
                "Sign in with Google to book cars, "
                + "manage rentals and access your booking history."
        );

        customerNameLabel.setText(
                "Guest"
        );

        customerEmailLabel.setText(
                "—"
        );

        customerIdLabel.setText(
                "—"
        );

        accountStatusLabel.setText(
                "No active customer session"
        );

        loginButton.setDisable(false);

        loginButton.setVisible(true);
        loginButton.setManaged(true);

        logoutButton.setDisable(false);

        logoutButton.setVisible(false);
        logoutButton.setManaged(false);
    }

    private void showLoadingState() {

        accountStateLabel.setText(
                "Loading account..."
        );

        accountDescriptionLabel.setText(
                "Checking your CRUISE customer session."
        );

        customerNameLabel.setText(
                "Loading..."
        );

        customerEmailLabel.setText(
                "Loading..."
        );

        customerIdLabel.setText(
                "..."
        );

        accountStatusLabel.setText(
                "Verifying session..."
        );

        loginButton.setDisable(true);
        logoutButton.setDisable(true);
    }

    private void showSignedInState(
            JsonNode json) {

        String name =
                readText(
                        json,
                        "name",
                        "Google Customer"
                );

        String email =
                readText(
                        json,
                        "email",
                        "Email unavailable"
                );

        String customerId =
                readCustomerId(
                        json
                );

        accountStateLabel.setText(
                "Signed in"
        );

        accountDescriptionLabel.setText(
                "Your Google account is connected to CRUISE."
        );

        customerNameLabel.setText(
                name
        );

        customerEmailLabel.setText(
                email
        );

        customerIdLabel.setText(
                customerId
        );

        accountStatusLabel.setText(
                "Customer session active"
        );

        loginButton.setDisable(false);
        logoutButton.setDisable(false);

        loginButton.setVisible(false);
        loginButton.setManaged(false);

        logoutButton.setVisible(true);
        logoutButton.setManaged(true);
    }

    @FXML
    private void onGoogleLoginClicked() {

        loginButton.setDisable(true);

        accountStatusLabel.setText(
                "Opening Google sign in..."
        );

        Thread thread =
                new Thread(
                        this::performGoogleLogin
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void performGoogleLogin() {

        try {

            /*
             * STEP 1
             * Authenticate with Google.
             */
            GoogleDesktopAuthService googleAuth =
                    new GoogleDesktopAuthService();

            GoogleUser googleUser =
                    googleAuth.login();

            if (googleUser == null) {

                throw new RuntimeException(
                        "Google sign in did not return a user."
                );
            }

            /*
             * STEP 2
             * Send Google's access token to our
             * Spring Boot backend.
             *
             * Backend verifies Google identity and
             * creates the CRUISE application session.
             */
            JsonNode backendLogin =
                    googleAuth
                            .loginToCarRentalBackend(
                                    googleUser
                            );

            if (backendLogin == null) {

                throw new RuntimeException(
                        "CRUISE backend did not return a login response."
                );
            }

            /*
             * STEP 3
             * Get the CRUISE application token.
             */
            String appToken =
                    readBackendToken(
                            backendLogin
                    );

            if (appToken == null
                    || appToken.isBlank()) {

                throw new RuntimeException(
                        "CRUISE backend did not return an application token."
                );
            }

            /*
             * STEP 4
             * Persist the CRUISE token.
             *
             * IMPORTANT:
             * We store the backend application token,
             * NOT Google's access token.
             */
            AppSession.saveToken(
                    appToken
            );

            /*
             * STEP 5
             * Refresh account using /api/customer/me.
             */
            Platform.runLater(
                    () -> {

                        accountStatusLabel.setText(
                                "Google sign in successful."
                        );

                        refreshAccount();
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        loginButton.setDisable(false);

                        accountStatusLabel.setText(
                                "Google sign in was not completed."
                        );

                        showAlert(
                                Alert.AlertType.ERROR,
                                "Google Sign In",
                                e.getMessage() == null
                                        ? "Unable to sign in with Google."
                                        : e.getMessage()
                        );
                    }
            );
        }
    }

    private String readBackendToken(
            JsonNode json) {

        /*
         * Main expected field.
         */
        JsonNode tokenNode =
                json.get(
                        "token"
                );

        /*
         * Small compatibility fallback in case
         * backend response calls it appToken.
         */
        if (tokenNode == null
                || tokenNode.isNull()
                || tokenNode.asText().isBlank()) {

            tokenNode =
                    json.get(
                            "appToken"
                    );
        }

        if (tokenNode == null
                || tokenNode.isNull()
                || tokenNode.asText().isBlank()) {

            return null;
        }

        return tokenNode.asText();
    }

    @FXML
    private void onLogoutClicked() {

        String token =
                AppSession.getToken();

        if (token == null
                || token.isBlank()) {

            AppSession.clearToken();

            showGuestState();

            return;
        }

        logoutButton.setDisable(true);

        accountStatusLabel.setText(
                "Signing out..."
        );

        Thread thread =
                new Thread(
                        () -> performLogout(
                                token
                        )
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void performLogout(
            String token) {

        try {

            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            "http://localhost:8080/api/customer/logout"
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

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );

            if ((response.statusCode() >= 200
                    && response.statusCode() < 300)
                    || response.statusCode() == 401) {

                AppSession.clearToken();

                Platform.runLater(
                        () -> {

                            showGuestState();

                            showAlert(
                                    Alert.AlertType.INFORMATION,
                                    "Signed Out",
                                    "You have been signed out of CRUISE."
                            );
                        }
                );

                return;
            }

            throw new RuntimeException(
                    "Server returned HTTP "
                    + response.statusCode()
                    + ": "
                    + response.body()
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        logoutButton.setDisable(false);

                        accountStatusLabel.setText(
                                "Unable to sign out."
                        );

                        showAlert(
                                Alert.AlertType.ERROR,
                                "Logout Error",
                                e.getMessage() == null
                                        ? "Unable to sign out from the server."
                                        : e.getMessage()
                        );
                    }
            );
        }
    }

    private String readText(
            JsonNode json,
            String field,
            String fallback) {

        JsonNode value =
                json.get(
                        field
                );

        if (value == null
                || value.isNull()
                || value.asText().isBlank()) {

            return fallback;
        }

        return value.asText();
    }

    private String readCustomerId(
            JsonNode json) {

        JsonNode id =
                json.get(
                        "id"
                );

        if (id == null
                || id.isNull()) {

            id =
                    json.get(
                            "customerId"
                    );
        }

        if (id == null
                || id.isNull()
                || id.asText().isBlank()) {

            return "—";
        }

        return "#"
                + id.asText();
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