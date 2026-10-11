package com.example.simpleclient;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.stage.FileChooser;

public class AdminCarsController {

    private static final String API_URL =
        "http://localhost:8080/api/admin/cars";

    @FXML
    private ListView<Car> carListView;

    @FXML
    private TextField brandField;

    @FXML
    private TextField modelField;

    @FXML
    private TextField priceField;

    @FXML
    private ComboBox<String> categoryBox;

    @FXML
    private ComboBox<String> transmissionBox;

    @FXML
    private ComboBox<String> fuelBox;

    @FXML
    private ComboBox<Integer> seatsBox;

    @FXML
    private ComboBox<String> statusBox;

    @FXML private TextField mileageField;
    @FXML private DatePicker nextServiceDatePicker;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ImageView imagePreview;

    @FXML
    private Label imagePlaceholder;

    @FXML
    private Label selectedImageLabel;

    @FXML
    private Label formTitleLabel;

    @FXML
    private Label adminCarStatusLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button deleteButton;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper()
                    .findAndRegisterModules();

    private Car selectedCar;

    private File selectedImageFile;

    @FXML
    private void initialize() {

        categoryBox
                .getItems()
                .addAll(
                        "Sedan",
                        "SUV",
                        "MPV",
                        "Hatchback",
                        "Coupe",
                        "Pickup",
                        "Luxury"
                );

        transmissionBox
                .getItems()
                .addAll(
                        "Automatic",
                        "Manual"
                );

        fuelBox
                .getItems()
                .addAll(
                        "Petrol",
                        "Diesel",
                        "Hybrid",
                        "Electric"
                );

        seatsBox
                .getItems()
                .addAll(
                        2,
                        4,
                        5,
                        6,
                        7,
                        8
                );

        statusBox
                .getItems()
                .addAll(
                        "AVAILABLE",
                        "UNAVAILABLE",
                        "MAINTENANCE"
                );

        carListView.setCellFactory(
                list ->
                        new ListCell<>() {

                            @Override
                            protected void updateItem(
                                    Car car,
                                    boolean empty) {

                                super.updateItem(
                                        car,
                                        empty
                                );

                                if (empty
                                        || car == null) {

                                    setText(null);
                                    return;
                                }

                                setText(
                                        car.getBrand()
                                        + " "
                                        + car.getModel()
                                        + "\nRM "
                                        + String.format(
                                                "%.2f",
                                                car.getPricePerDay()
                                        )
                                        + " / day"
                                );
                            }
                        }
        );

        carListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                showCar(
                                        newValue
                                );
                            }
                        }
                );

        newCar();

        loadCars();
    }

    @FXML
    private void refreshCars() {

        loadCars();
    }

    private void loadCars() {

        adminCarStatusLabel.setText(
                "Loading vehicles..."
        );

        Thread thread =
                new Thread(
                        this::fetchCars
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void fetchCars() {

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
                                    "Bearer " + AdminSession.getToken()
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
                        "Unable to load cars. HTTP "
                        + response.statusCode()
                );
            }

            Car[] cars =
                    mapper.readValue(
                            response.body(),
                            Car[].class
                    );

            List<Car> carList =
                    Arrays.asList(
                            cars
                    );

            Platform.runLater(
                    () -> {

                        carListView
                                .getItems()
                                .setAll(
                                        carList
                                );

                        adminCarStatusLabel.setText(
                                carList.size()
                                + " vehicle(s) loaded."
                        );
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () ->
                            adminCarStatusLabel.setText(
                                    "Unable to connect to backend."
                            )
            );
        }
    }

    @FXML
    private void newCar() {

        selectedCar = null;
        selectedImageFile = null;

        carListView
                .getSelectionModel()
                .clearSelection();

        formTitleLabel.setText(
                "Add New Vehicle"
        );

        brandField.clear();
        modelField.clear();
        priceField.clear();

        categoryBox.setValue(
                "Sedan"
        );

        transmissionBox.setValue(
                "Automatic"
        );

        fuelBox.setValue(
                "Petrol"
        );

        seatsBox.setValue(
                5
        );

        statusBox.setValue("AVAILABLE");
        mileageField.clear();
        nextServiceDatePicker.setValue(null);

        descriptionArea.clear();

        imagePreview.setImage(null);
        imagePreview.setVisible(false);

        imagePlaceholder.setVisible(true);

        selectedImageLabel.setText(
                "No image selected"
        );

        deleteButton.setDisable(true);

        saveButton.setText(
                "Add Vehicle"
        );

        adminCarStatusLabel.setText(
                "Ready to add a new vehicle."
        );
    }

    private void showCar(
            Car car) {

        selectedCar =
                car;

        selectedImageFile =
                null;

        formTitleLabel.setText(
                "Edit Vehicle"
        );

        brandField.setText(
                safe(
                        car.getBrand()
                )
        );

        modelField.setText(
                safe(
                        car.getModel()
                )
        );

        priceField.setText(
                String.format(
                        "%.2f",
                        car.getPricePerDay()
                )
        );

        categoryBox.setValue(
                defaultText(
                        car.getCategory(),
                        "Sedan"
                )
        );

        transmissionBox.setValue(
                defaultText(
                        car.getTransmission(),
                        "Automatic"
                )
        );

        fuelBox.setValue(
                defaultText(
                        car.getFuelType(),
                        "Petrol"
                )
        );

        seatsBox.setValue(
                car.getSeats() == null
                        ? 5
                        : car.getSeats()
        );

        statusBox.setValue(
                defaultText(
                        car.getStatus(),
                        "AVAILABLE"
                )
        );

        mileageField.setText(car.getMileage() == null ? "" : String.valueOf(car.getMileage()));
        nextServiceDatePicker.setValue(car.getNextServiceDate());

        descriptionArea.setText(
                safe(
                        car.getDescription()
                )
        );

        selectedImageLabel.setText(
                car.getImageUrl() == null
                        || car.getImageUrl().isBlank()
                        ? "No image uploaded"
                        : "Current vehicle image"
        );

        showRemoteImage(
                car.getImageUrl()
        );

        deleteButton.setDisable(false);

        saveButton.setText(
                "Save Changes"
        );

        adminCarStatusLabel.setText(
                "Editing vehicle #"
                + car.getId()
        );
    }

    @FXML
    private void chooseImage() {

        FileChooser chooser =
                new FileChooser();

        chooser.setTitle(
                "Choose Vehicle Image"
        );

        chooser
                .getExtensionFilters()
                .add(
                        new FileChooser.ExtensionFilter(
                                "Images",
                                "*.jpg",
                                "*.jpeg",
                                "*.png",
                                "*.webp"
                        )
                );

        File file =
                chooser.showOpenDialog(
                        imagePreview
                                .getScene()
                                .getWindow()
                );

        if (file == null) {
            return;
        }

        selectedImageFile =
                file;

        selectedImageLabel.setText(
                file.getName()
        );

        try {

            Image image =
                    new Image(
                            file
                                    .toURI()
                                    .toString(),
                            true
                    );

            imagePreview.setImage(
                    image
            );

            imagePreview.setVisible(true);

            imagePlaceholder.setVisible(false);

        } catch (Exception e) {

            e.printStackTrace();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Image Error",
                    "Unable to preview the selected image."
            );
        }
    }

    @FXML
    private void saveCar() {

        String brand =
                brandField
                        .getText()
                        .trim();

        String model =
                modelField
                        .getText()
                        .trim();

        if (brand.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation",
                    "Brand is required."
            );

            return;
        }

        if (model.isBlank()) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation",
                    "Model is required."
            );

            return;
        }

        double price;

        try {

            price =
                    Double.parseDouble(
                            priceField
                                    .getText()
                                    .trim()
                    );

        } catch (NumberFormatException e) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation",
                    "Enter a valid price per day."
            );

            return;
        }

        if (price <= 0) {

            showAlert(
                    Alert.AlertType.WARNING,
                    "Validation",
                    "Price must be greater than 0."
            );

            return;
        }

        Car car =
                new Car();

        if (selectedCar != null) {

            car.setId(
                    selectedCar.getId()
            );

            car.setImageUrl(
                    selectedCar.getImageUrl()
            );
        }

        car.setBrand(
                brand
        );

        car.setModel(
                model
        );

        car.setPricePerDay(
                price
        );

        car.setCategory(
                categoryBox.getValue()
        );

        car.setTransmission(
                transmissionBox.getValue()
        );

        car.setFuelType(
                fuelBox.getValue()
        );

        car.setSeats(
                seatsBox.getValue()
        );

        car.setDescription(
                descriptionArea
                        .getText()
                        .trim()
        );

        car.setStatus(statusBox.getValue());
        try {
            car.setMileage(mileageField.getText().isBlank() ? null : Integer.valueOf(mileageField.getText().trim()));
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Mileage must be a whole number.");
            return;
        }
        car.setNextServiceDate(nextServiceDatePicker.getValue());

        saveButton.setDisable(true);

        adminCarStatusLabel.setText(
                "Saving vehicle..."
        );

        Thread thread =
                new Thread(
                        () -> saveCarRequest(
                                car
                        )
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void saveCarRequest(
            Car car) {

        try {

            boolean editing =
                    selectedCar != null;

            String json =
                    mapper.writeValueAsString(
                            car
                    );

            HttpRequest.Builder builder =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            editing
                                                    ? API_URL
                                                    + "/"
                                                    + selectedCar.getId()
                                                    : API_URL
                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + AdminSession.getToken()
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            );

            HttpRequest request;

            if (editing) {

                request =
                        builder
                                .PUT(
                                        HttpRequest
                                                .BodyPublishers
                                                .ofString(json)
                                )
                                .build();

            } else {

                request =
                        builder
                                .POST(
                                        HttpRequest
                                                .BodyPublishers
                                                .ofString(json)
                                )
                                .build();
            }

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
                        "Save failed. HTTP "
                        + response.statusCode()
                        + "\n"
                        + response.body()
                );
            }

            Car savedCar =
                    mapper.readValue(
                            response.body(),
                            Car.class
                    );

            /*
             * If admin selected an image,
             * upload it after the car exists.
             */
            if (selectedImageFile != null) {

                savedCar =
                        uploadImage(
                                savedCar.getId(),
                                selectedImageFile
                        );
            }

            Car finalSavedCar =
                    savedCar;

            Platform.runLater(
                    () -> {

                        saveButton.setDisable(false);

                        selectedCar =
                                finalSavedCar;

                        selectedImageFile =
                                null;

                        showCar(
                                finalSavedCar
                        );

                        loadCars();

                        showAlert(
                                Alert.AlertType.INFORMATION,
                                "Vehicle Saved",
                                "Vehicle saved successfully."
                        );
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        saveButton.setDisable(false);

                        adminCarStatusLabel.setText(
                                "Unable to save vehicle."
                        );

                        showAlert(
                                Alert.AlertType.ERROR,
                                "Save Error",
                                e.getMessage() == null
                                        ? "Unable to save vehicle."
                                        : e.getMessage()
                        );
                    }
            );
        }
    }

    private Car uploadImage(
            Integer carId,
            File imageFile)
            throws Exception {

        String boundary =
                "----CRUISE"
                + UUID.randomUUID();

        String contentType =
                Files.probeContentType(
                        imageFile.toPath()
                );

        if (contentType == null) {

            contentType =
                    "application/octet-stream";
        }

        byte[] fileBytes =
                Files.readAllBytes(
                        imageFile.toPath()
                );

        String start =
                "--"
                + boundary
                + "\r\n"
                + "Content-Disposition: form-data; "
                + "name=\"file\"; filename=\""
                + imageFile.getName()
                + "\"\r\n"
                + "Content-Type: "
                + contentType
                + "\r\n\r\n";

        String end =
                "\r\n--"
                + boundary
                + "--\r\n";

        byte[] startBytes =
                start.getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] endBytes =
                end.getBytes(
                        StandardCharsets.UTF_8
                );

        HttpRequest.BodyPublisher body =
                HttpRequest
                        .BodyPublishers
                        .concat(
                                HttpRequest
                                        .BodyPublishers
                                        .ofByteArray(
                                                startBytes
                                        ),
                                HttpRequest
                                        .BodyPublishers
                                        .ofByteArray(
                                                fileBytes
                                        ),
                                HttpRequest
                                        .BodyPublishers
                                        .ofByteArray(
                                                endBytes
                                        )
                        );

        HttpRequest request =
                HttpRequest
                        .newBuilder()
                        .uri(
                                URI.create(
                                        API_URL
                                        + "/"
                                        + carId
                                        + "/image"
                                )
                        )
                        .header(
                                "Authorization",
                                "Bearer " + AdminSession.getToken()
                        )
                        .header(
                                "Content-Type",
                                "multipart/form-data; boundary="
                                + boundary
                        )
                        .POST(body)
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
                    "Vehicle saved, but image upload failed. HTTP "
                    + response.statusCode()
                    + "\n"
                    + response.body()
            );
        }

        return mapper.readValue(
                response.body(),
                Car.class
        );
    }

    @FXML
    private void deleteCar() {

        if (selectedCar == null) {
            return;
        }

        Alert confirmation =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );

        confirmation.setTitle(
                "Delete Vehicle"
        );

        confirmation.setHeaderText(
                "Delete "
                + selectedCar.getBrand()
                + " "
                + selectedCar.getModel()
                + "?"
        );

        confirmation.setContentText(
                "This action cannot be undone."
        );

        if (confirmation
                .showAndWait()
                .isEmpty()
                || confirmation
                        .getResult()
                        .getButtonData()
                        .isCancelButton()) {

            return;
        }

        Integer carId =
                selectedCar.getId();

        deleteButton.setDisable(true);

        Thread thread =
                new Thread(
                        () -> deleteCarRequest(
                                carId
                        )
                );

        thread.setDaemon(true);
        thread.start();
    }

    private void deleteCarRequest(
            Integer carId) {

        try {

            HttpRequest request =
                    HttpRequest
                            .newBuilder()
                            .uri(
                                    URI.create(
                                            API_URL
                                            + "/"
                                            + carId
                                    )
                            )
                            .header(
                                    "Authorization",
                                    "Bearer " + AdminSession.getToken()
                            )
                            .DELETE()
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
                        "Delete failed. HTTP "
                        + response.statusCode()
                        + "\n"
                        + response.body()
                );
            }

            Platform.runLater(
                    () -> {

                        newCar();

                        loadCars();

                        showAlert(
                                Alert.AlertType.INFORMATION,
                                "Vehicle Deleted",
                                "Vehicle deleted successfully."
                        );
                    }
            );

        } catch (Exception e) {

            e.printStackTrace();

            Platform.runLater(
                    () -> {

                        deleteButton.setDisable(false);

                        showAlert(
                                Alert.AlertType.ERROR,
                                "Delete Error",
                                e.getMessage() == null
                                        ? "Unable to delete vehicle."
                                        : e.getMessage()
                        );
                    }
            );
        }
    }

    private void showRemoteImage(
            String imageUrl) {

        if (imageUrl == null
                || imageUrl.isBlank()) {

            imagePreview.setImage(null);
            imagePreview.setVisible(false);

            imagePlaceholder.setVisible(true);

            return;
        }

        try {

            String url =
                    imageUrl;

            if (url.startsWith("/")) {

                url =
                        "http://localhost:8080"
                        + url;
            }

            Image image =
                    new Image(
                            url,
                            true
                    );

            imagePreview.setImage(
                    image
            );

            imagePreview.setVisible(true);
            imagePlaceholder.setVisible(false);

        } catch (Exception e) {

            imagePreview.setImage(null);
            imagePreview.setVisible(false);

            imagePlaceholder.setVisible(true);
        }
    }

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

    private String defaultText(
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
                new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }
}