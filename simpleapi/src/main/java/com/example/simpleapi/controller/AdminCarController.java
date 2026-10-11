package com.example.simpleapi.controller;

import com.example.simpleapi.Car;
import com.example.simpleapi.CarRepository;
import com.example.simpleapi.service.AdminAuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/cars")
public class AdminCarController {

    private final CarRepository carRepository;
    private final AdminAuthService authService;

    private static final Path CAR_UPLOAD_DIRECTORY =
            Paths.get(
                    "uploads",
                    "cars"
            )
            .toAbsolutePath()
            .normalize();

    public AdminCarController(
            CarRepository carRepository,
            AdminAuthService authService) {

        this.carRepository =
                carRepository;

        this.authService =
                authService;
    }

    @GetMapping
    public ResponseEntity<?> getCars(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization) {

        if (!authorized(authorization)) {
            return unauthorized();
        }

        List<Car> cars =
                carRepository.findAll();

        return ResponseEntity.ok(
                cars
        );
    }

    @PostMapping
    public ResponseEntity<?> addCar(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,
            @RequestBody Car car) {

        if (!authorized(authorization)) {
            return unauthorized();
        }

        ResponseEntity<Map<String, String>>
                validation =
                validateCar(car);

        if (validation != null) {
            return validation;
        }

        if (car.getStatus() == null
                || car.getStatus().isBlank()) {

            car.setStatus(
                    "AVAILABLE"
            );
        }

        car.setId(null);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        carRepository.save(car)
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCar(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,
            @PathVariable Integer id,
            @RequestBody Car updatedCar) {

        if (!authorized(authorization)) {
            return unauthorized();
        }

        Car car =
                carRepository
                        .findById(id)
                        .orElse(null);

        if (car == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            Map.of(
                                    "message",
                                    "Car not found"
                            )
                    );
        }

        ResponseEntity<Map<String, String>>
                validation =
                validateCar(
                        updatedCar
                );

        if (validation != null) {
            return validation;
        }

        car.setBrand(
                updatedCar.getBrand()
        );

        car.setModel(
                updatedCar.getModel()
        );

        car.setPricePerDay(
                updatedCar.getPricePerDay()
        );

        car.setCategory(
                updatedCar.getCategory()
        );

        car.setTransmission(
                updatedCar.getTransmission()
        );

        car.setFuelType(
                updatedCar.getFuelType()
        );

        car.setSeats(
                updatedCar.getSeats()
        );

        car.setDescription(updatedCar.getDescription());
        car.setMileage(updatedCar.getMileage());
        car.setNextServiceDate(updatedCar.getNextServiceDate());

        /*
         * Preserve existing image unless the JSON
         * intentionally contains another value.
         */
        if (updatedCar.getImageUrl() != null
                && !updatedCar.getImageUrl().isBlank()) {

            car.setImageUrl(
                    updatedCar.getImageUrl()
            );
        }

        if (updatedCar.getStatus() == null
                || updatedCar.getStatus().isBlank()) {

            car.setStatus(
                    "AVAILABLE"
            );

        } else {

            car.setStatus(
                    updatedCar.getStatus()
            );
        }

        return ResponseEntity.ok(
                carRepository.save(car)
        );
    }

    @PostMapping(
            value = "/{id}/image",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<?> uploadImage(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,
            @PathVariable Integer id,
            @RequestParam("file")
            MultipartFile file) {

        if (!authorized(authorization)) {
            return unauthorized();
        }

        Car car =
                carRepository
                        .findById(id)
                        .orElse(null);

        if (car == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            Map.of(
                                    "message",
                                    "Car not found"
                            )
                    );
        }

        if (file == null
                || file.isEmpty()) {

            return badRequest(
                    "Image file is required"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType == null
                || !contentType.startsWith(
                        "image/"
                )) {

            return badRequest(
                    "Only image files are allowed"
            );
        }

        String extension =
                getSafeExtension(
                        file.getOriginalFilename()
                );

        if (!extension.equals(".jpg")
                && !extension.equals(".jpeg")
                && !extension.equals(".png")
                && !extension.equals(".webp")) {

            return badRequest(
                    "Only JPG, JPEG, PNG or WEBP images are allowed"
            );
        }

        try {

            Files.createDirectories(
                    CAR_UPLOAD_DIRECTORY
            );

            String filename =
                    "car-"
                    + id
                    + "-"
                    + UUID.randomUUID()
                    + extension;

            Path destination =
                    CAR_UPLOAD_DIRECTORY
                            .resolve(filename)
                            .normalize();

            if (!destination.startsWith(
                    CAR_UPLOAD_DIRECTORY
            )) {

                return badRequest(
                        "Invalid image path"
                );
            }

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            String oldImage =
                    car.getImageUrl();

            String imageUrl =
                    "/uploads/cars/"
                    + filename;

            car.setImageUrl(
                    imageUrl
            );

            Car saved =
                    carRepository.save(
                            car
                    );

            deleteOldLocalImage(
                    oldImage,
                    imageUrl
            );

            return ResponseEntity.ok(
                    saved
            );

        } catch (IOException e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(
                            Map.of(
                                    "message",
                                    "Unable to save image"
                            )
                    );
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCar(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authorization,
            @PathVariable Integer id) {

        if (!authorized(authorization)) {
            return unauthorized();
        }

        Car car =
                carRepository
                        .findById(id)
                        .orElse(null);

        if (car == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            Map.of(
                                    "message",
                                    "Car not found"
                            )
                    );
        }

        String image =
                car.getImageUrl();

        carRepository.delete(
                car
        );

        deleteOldLocalImage(
                image,
                null
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Car deleted successfully"
                )
        );
    }

    private boolean authorized(
            String authorization) {

        String token =
                bearerToken(
                        authorization
                );

        return authService
                .authenticate(token)
                .isPresent();
    }

    private String bearerToken(
            String authorization) {

        if (authorization == null
                || !authorization.startsWith(
                        "Bearer "
                )) {

            return null;
        }

        return authorization
                .substring(7)
                .trim();
    }

    private ResponseEntity<Map<String, String>>
            validateCar(Car car) {

        if (car.getBrand() == null
                || car.getBrand().isBlank()) {

            return badRequest(
                    "Brand is required"
            );
        }

        if (car.getModel() == null
                || car.getModel().isBlank()) {

            return badRequest(
                    "Model is required"
            );
        }

        if (car.getPricePerDay() <= 0) {

            return badRequest(
                    "Price must be greater than 0"
            );
        }

        if (car.getSeats() != null
                && car.getSeats() <= 0) {

            return badRequest(
                    "Seats must be greater than 0"
            );
        }

        return null;
    }

    private String getSafeExtension(
            String filename) {

        if (filename == null) {
            return "";
        }

        int dot =
                filename.lastIndexOf('.');

        if (dot < 0) {
            return "";
        }

        return filename
                .substring(dot)
                .toLowerCase();
    }

    private void deleteOldLocalImage(
            String oldImage,
            String newImage) {

        if (oldImage == null
                || oldImage.isBlank()
                || !oldImage.startsWith(
                        "/uploads/cars/"
                )
                || oldImage.equals(
                        newImage
                )) {

            return;
        }

        try {

            String filename =
                    oldImage.substring(
                            "/uploads/cars/"
                                    .length()
                    );

            Path path =
                    CAR_UPLOAD_DIRECTORY
                            .resolve(filename)
                            .normalize();

            if (path.startsWith(
                    CAR_UPLOAD_DIRECTORY
            )) {

                Files.deleteIfExists(
                        path
                );
            }

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private ResponseEntity<Map<String, String>>
            badRequest(
                    String message) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                message
                        )
                );
    }

    private ResponseEntity<Map<String, String>>
            unauthorized() {

        return ResponseEntity
                .status(
                        HttpStatus.UNAUTHORIZED
                )
                .body(
                        Map.of(
                                "message",
                                "Admin authentication required"
                        )
                );
    }
}