package com.example.simpleapi.controller;

import com.example.simpleapi.Car;
import com.example.simpleapi.CarRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    private final CarRepository carRepository;

    public CarController(
            CarRepository carRepository) {

        this.carRepository =
                carRepository;
    }

    @GetMapping
    public List<Car> getCars() {

        return carRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCar(
            @PathVariable Integer id) {

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

        return ResponseEntity.ok(
                car
        );
    }
}