package com.example.simpleclient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

public class AdminLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label loginStatusLabel;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper();

    @FXML
    private void initialize() {

        loginStatusLabel.setText(
                ""
        );
    }

    @FXML
    private void login() {

        String username =
                usernameField
                        .getText()
                        .trim();

        String password =
                passwordField
                        .getText();

        if (username.isBlank()
                || password.isBlank()) {

            loginStatusLabel.setText(
                    "Enter username and password."
            );

            return;
        }

        loginStatusLabel.setText(
                "Signing in..."
        );

        Thread thread =
                new Thread(
                        () ->
                                loginRequest(
                                        username,
                                        password
                                )
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void loginRequest(
            String username,
            String password) {

        try {

            String body =
                    mapper
                            .createObjectNode()
                            .put(
                                    "username",
                                    username
                            )
                            .put(
                                    "password",
                                    password
                            )
                            .toString();

            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            "http://localhost:8080/api/admin/auth/login"
                                    )
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest
                                            .BodyPublishers
                                            .ofString(body)
                            )
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );

            if (response.statusCode() != 200) {

                Platform.runLater(
                        () ->
                                loginStatusLabel.setText(
                                        "Invalid username or password."
                                )
                );

                return;
            }

            JsonNode json =
                    mapper.readTree(
                            response.body()
                    );

            String token =
                    json.path(
                            "token"
                    )
                    .asText();

            if (token.isBlank()) {

                throw new IllegalStateException(
                        "Backend did not return admin token"
                );
            }

            AdminSession.saveToken(
                    token
            );

            Platform.runLater(
                    this::openAdmin
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () ->
                            loginStatusLabel.setText(
                                    "Unable to connect to server."
                            )
            );
        }
    }

    private void openAdmin() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "admin-shell.fxml"
                            )
                    );

            Parent root =
                    loader.load();

            Stage stage =
                    (Stage) usernameField
                            .getScene()
                            .getWindow();

            WindowManager.show(stage, root, "CRUISE - Admin");

        } catch (Exception e) {

            e.printStackTrace();

            loginStatusLabel.setText(
                    "Unable to open admin dashboard."
            );
        }
    }
    @FXML
    private void backToCustomer() {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("customer-shell.fxml")
            );

            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            WindowManager.show(stage, root, "CRUISE - Car Rental");

        } catch (Exception e) {
            e.printStackTrace();
            loginStatusLabel.setText("Unable to open customer app.");
        }
    }

}