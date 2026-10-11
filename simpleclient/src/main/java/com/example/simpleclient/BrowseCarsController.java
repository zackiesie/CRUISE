package com.example.simpleclient;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BrowseCarsController {

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane carContainer;

    @FXML
    private Label resultCountLabel;

    @FXML
    private Label browseStatusLabel;

    @FXML
    private Button allButton;

    @FXML
    private Button sedanButton;

    @FXML
    private Button suvButton;

    @FXML
    private Button mpvButton;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper();

    private List<Car> allCars =
            List.of();

    private String selectedCategory =
            "ALL";

    @FXML
    private void initialize() {

        searchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                applyFilters()
                );

        setActiveFilter(
                allButton
        );

        carContainer.widthProperty().addListener((o, before, width) -> resizeCards());
        loadCars();
    }

    @FXML
    private void onRefreshClicked() {

        loadCars();
    }

    @FXML
    private void onAllClicked() {

        selectedCategory = "ALL";

        setActiveFilter(
                allButton
        );

        applyFilters();
    }

    @FXML
    private void onSedanClicked() {

        selectedCategory = "SEDAN";

        setActiveFilter(
                sedanButton
        );

        applyFilters();
    }

    @FXML
    private void onSuvClicked() {

        selectedCategory = "SUV";

        setActiveFilter(
                suvButton
        );

        applyFilters();
    }

    @FXML
    private void onMpvClicked() {

        selectedCategory = "MPV";

        setActiveFilter(
                mpvButton
        );

        applyFilters();
    }

    private void loadCars() {

        browseStatusLabel.setText(
                "Loading vehicles..."
        );

        Task<List<Car>> task =
                new Task<>() {

                    @Override
                    protected List<Car> call()
                            throws Exception {

                        HttpRequest request =
                                HttpRequest
                                        .newBuilder()
                                        .uri(
                                                URI.create(
                                                        "http://localhost:8080/api/cars"
                                                )
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
                                    "Unable to load cars"
                            );
                        }

                        Car[] cars =
                                mapper.readValue(
                                        response.body(),
                                        Car[].class
                                );

                        return Arrays.asList(
                                cars
                        );
                    }
                };

        task.setOnSucceeded(
                event -> {

                    allCars =
                            task.getValue();

                    applyFilters();

                    browseStatusLabel.setText(
                            "Vehicles ready to book"
                    );
                }
        );

        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();

                    browseStatusLabel.setText(
                            "Unable to connect to car rental server"
                    );

                    resultCountLabel.setText(
                            "0 cars available"
                    );

                    carContainer
                            .getChildren()
                            .clear();

                    error.printStackTrace();
                }
        );

        Thread thread =
                new Thread(task);

        thread.setDaemon(true);

        thread.start();
    }

    private void applyFilters() {

        String search =
                searchField
                        .getText()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        List<Car> filteredCars =
                allCars
                        .stream()
                        .filter(
                                this::isAvailable
                        )
                        .filter(
                                car ->
                                        matchesCategory(
                                                car
                                        )
                        )
                        .filter(
                                car ->
                                        matchesSearch(
                                                car,
                                                search
                                        )
                        )
                        .collect(
                                Collectors.toList()
                        );

        renderCars(
                filteredCars
        );
    }

    private boolean isAvailable(
            Car car) {

        String status =
                car.getStatus();

        /*
         * Existing database cars may still have
         * NULL status because they existed before
         * the new field was added.
         *
         * For now NULL is treated as available.
         */
        if (status == null
                || status.isBlank()) {

            return true;
        }

        return "AVAILABLE"
                .equalsIgnoreCase(
                        status
                );
    }

    private boolean matchesCategory(
            Car car) {

        if ("ALL".equals(
                selectedCategory
        )) {

            return true;
        }

        String category =
                car.getCategory();

        if (category == null) {
            return false;
        }

        return selectedCategory
                .equalsIgnoreCase(
                        category.trim()
                );
    }

    private boolean matchesSearch(
            Car car,
            String search) {

        if (search.isBlank()) {
            return true;
        }

        String brand =
                safeText(
                        car.getBrand()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        String model =
                safeText(
                        car.getModel()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        String category =
                safeText(
                        car.getCategory()
                )
                        .toLowerCase(
                                Locale.ROOT
                        );

        return brand.contains(search)
                || model.contains(search)
                || category.contains(search);
    }

    private void renderCars(
            List<Car> cars) {

        carContainer
                .getChildren()
                .clear();

        resultCountLabel.setText(
                cars.size()
                + (
                    cars.size() == 1
                    ? " car available"
                    : " cars available"
                )
        );

        if (cars.isEmpty()) {

            VBox emptyState =
                    createEmptyState();

            carContainer
                    .getChildren()
                    .add(
                            emptyState
                    );

            return;
        }

        for (Car car : cars) {

            VBox card =
                    createCarCard(
                            car
                    );

            carContainer
                    .getChildren()
                    .add(
                            card
                    );
        }
    }

    private double cardWidth() {
        double available = Math.max(300, carContainer.getWidth());
        int columns = Math.max(1, Math.min(4, (int) ((available + 20) / 340)));
        return Math.max(280, (available - (columns - 1) * 20 - 2) / columns);
    }
    private void resizeCards() {
        double width = cardWidth();
        for (javafx.scene.Node node : carContainer.getChildren()) {
            if (node instanceof VBox card && card.getStyleClass().contains("car-card")) {
                card.setMinWidth(width); card.setPrefWidth(width); card.setMaxWidth(width);
            }
        }
    }

    private VBox createCarCard(
            Car car) {

        VBox card =
                new VBox();

        card.setSpacing(
                0
        );

        card.setPrefWidth(
                360
        );

        card.setMaxWidth(
                420
        );

        card.getStyleClass()
                .add(
                        "car-card"
                );

        VBox imageArea =
                createImageArea(
                        car
                );

        VBox information =
                new VBox();

        information.setSpacing(
                12
        );

        information.getStyleClass()
                .add(
                        "car-card-content"
                );

        HBox heading =
                new HBox();

        heading.setAlignment(
                Pos.TOP_LEFT
        );

        VBox titleBox =
                new VBox();

        titleBox.setSpacing(
                3
        );

        Label brandLabel =
                new Label(
                        displayValue(
                                car.getBrand(),
                                "Vehicle"
                        )
                                .toUpperCase(
                                        Locale.ROOT
                                )
                );

        brandLabel
                .getStyleClass()
                .add(
                        "car-brand"
                );

        Label modelLabel =
                new Label(
                        displayValue(
                                car.getModel(),
                                "Unknown model"
                        )
                );

        modelLabel
                .getStyleClass()
                .add(
                        "car-model"
                );

        titleBox
                .getChildren()
                .addAll(
                        brandLabel,
                        modelLabel
                );

        Region headingSpacer =
                new Region();

        HBox.setHgrow(
                headingSpacer,
                javafx.scene.layout.Priority.ALWAYS
        );

        Label categoryLabel =
                new Label(
                        displayValue(
                                car.getCategory(),
                                "Car"
                        )
                );

        categoryLabel
                .getStyleClass()
                .add(
                        "category-pill"
                );

        heading
                .getChildren()
                .addAll(
                        titleBox,
                        headingSpacer,
                        categoryLabel
                );

        HBox specifications =
                new HBox();

        specifications.setSpacing(
                8
        );

        specifications.setAlignment(
                Pos.CENTER_LEFT
        );

        Label transmission =
                createSpecChip(
                        "⚙  "
                        + displayValue(
                                car.getTransmission(),
                                "Automatic"
                        )
                );

        Label seats =
                createSpecChip(
                        "♙  "
                        + (
                            car.getSeats() == null
                            ? "5"
                            : car.getSeats()
                        )
                        + " seats"
                );

        specifications
                .getChildren()
                .addAll(
                        transmission,
                        seats
                );

        Label fuel =
                createSpecChip(
                        "◆  "
                        + displayValue(
                                car.getFuelType(),
                                "Petrol"
                        )
                );

        HBox priceRow =
                new HBox();

        priceRow.setAlignment(
                Pos.CENTER_LEFT
        );

        VBox priceBox =
                new VBox();

        priceBox.setSpacing(
                1
        );

        Label priceLabel =
                new Label(
                        String.format(
                                "RM %.0f",
                                car.getPricePerDay()
                        )
                );

        priceLabel
                .getStyleClass()
                .add(
                        "car-price"
                );

        Label perDayLabel =
                new Label(
                        "per day"
                );

        perDayLabel
                .getStyleClass()
                .add(
                        "car-price-unit"
                );

        priceBox
                .getChildren()
                .addAll(
                        priceLabel,
                        perDayLabel
                );

        Region priceSpacer =
                new Region();

        HBox.setHgrow(
                priceSpacer,
                javafx.scene.layout.Priority.ALWAYS
        );

        Button viewButton =
                new Button(
                        "View details  →"
                );

        viewButton
                .getStyleClass()
                .add(
                        "view-car-button"
                );

        viewButton.setOnAction(
                event ->
                        openCarDetails(
                                car
                        )
        );

        priceRow
                .getChildren()
                .addAll(
                        priceBox,
                        priceSpacer,
                        viewButton
                );

        information
                .getChildren()
                .addAll(
                        heading,
                        specifications,
                        fuel,
                        priceRow
                );

        card.getChildren()
                .addAll(
                        imageArea,
                        information
                );

        double width = cardWidth();
        card.setMinWidth(width); card.setPrefWidth(width); card.setMaxWidth(width);
        return card;
    }

    private VBox createImageArea(
            Car car) {

        VBox imageArea =
                new VBox();

        imageArea.setAlignment(
                Pos.CENTER
        );

        imageArea.setPrefHeight(
                185
        );

        imageArea.setMinHeight(
                185
        );

        imageArea.getStyleClass()
                .add(
                        "car-image-area"
                );

        String imageUrl =
                car.getImageUrl();

        if (imageUrl != null
                && !imageUrl.isBlank()) {

            try {

                Image image =
                        new Image(
                                normalizeImageUrl(
                                        imageUrl
                                ),
                                270,
                                165,
                                true,
                                true,
                                true
                        );

                ImageView imageView =
                        new ImageView(
                                image
                        );

                imageView.setFitWidth(
                        270
                );

                imageView.setFitHeight(
                        165
                );

                imageView.setPreserveRatio(
                        true
                );

                imageView
                        .getStyleClass()
                        .add(
                                "car-image"
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
                                                () ->
                                                        showImagePlaceholder(
                                                                imageArea,
                                                                car
                                                        )
                                        );
                                    }
                                }
                        );

                imageArea
                        .getChildren()
                        .add(
                                imageView
                        );

                return imageArea;

            } catch (Exception e) {

                // Fall through to placeholder.
            }
        }

        showImagePlaceholder(
                imageArea,
                car
        );

        return imageArea;
    }

    private void showImagePlaceholder(
            VBox imageArea,
            Car car) {

        imageArea
                .getChildren()
                .clear();

        VehicleArtwork icon = new VehicleArtwork();
        icon.setPrefSize(270, 140);
        icon.setMaxHeight(140);

        icon.getStyleClass()
                .add(
                        "car-placeholder-icon"
                );

        Label name =
                new Label(
                        displayValue(
                                car.getBrand(),
                                "CRUISE"
                        )
                        + " "
                        + displayValue(
                                car.getModel(),
                                ""
                        )
                );

        name.getStyleClass()
                .add(
                        "car-placeholder-name"
                );

        imageArea
                .getChildren()
                .addAll(
                        icon,
                        name
                );
    }

    private Label createSpecChip(
            String text) {

        Label label =
                new Label(
                        text
                );

        label.getStyleClass()
                .add(
                        "spec-chip"
                );

        return label;
    }

    private VBox createEmptyState() {

        VBox box =
                new VBox();

        box.setAlignment(
                Pos.CENTER
        );

        box.setSpacing(
                8
        );

        box.setPrefWidth(
                700
        );

        box.setPrefHeight(
                220
        );

        box.getStyleClass()
                .add(
                        "empty-state"
                );

        Label icon =
                new Label(
                        "⌕"
                );

        icon.getStyleClass()
                .add(
                        "empty-state-icon"
                );

        Label title =
                new Label(
                        "No cars found"
                );

        title.getStyleClass()
                .add(
                        "empty-state-title"
                );

        Label description =
                new Label(
                        "Try another search or vehicle category."
                );

        description
                .getStyleClass()
                .add(
                        "empty-state-description"
                );

        box.getChildren()
                .addAll(
                        icon,
                        title,
                        description
                );

        return box;
    }

    private void setActiveFilter(
            Button activeButton) {

        Button[] buttons = {
            allButton,
            sedanButton,
            suvButton,
            mpvButton
        };

        for (Button button : buttons) {

            button.getStyleClass()
                    .remove(
                            "filter-button-active"
                    );

            if (!button
                    .getStyleClass()
                    .contains(
                            "filter-button"
                    )) {

                button.getStyleClass()
                        .add(
                                "filter-button"
                        );
            }
        }

        activeButton
                .getStyleClass()
                .add(
                        "filter-button-active"
                );
    }

    private void openCarDetails(
            Car car) {

        try {

            SelectedCarSession.setSelectedCar(
                    car
            );

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass()
                                    .getResource(
                                            "car-details-view.fxml"
                                    )
                    );

            Parent root =
                    loader.load();

            Stage stage =
                    (Stage) carContainer
                            .getScene()
                            .getWindow();

            WindowManager.show(stage, root, "CRUISE - "
                    + displayValue(car.getBrand(), "") + " "
                    + displayValue(car.getModel(), ""));

        } catch (IOException e) {

            e.printStackTrace();

            browseStatusLabel.setText(
                    "Unable to open vehicle details."
            );
        }
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

    private String displayValue(
            String value,
            String fallback) {

        if (value == null
                || value.isBlank()) {

            return fallback;
        }

        return value;
    }

    private String safeText(
            String value) {

        return value == null
                ? ""
                : value;
    }
}