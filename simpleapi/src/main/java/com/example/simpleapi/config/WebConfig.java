package com.example.simpleapi.config;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig
        implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry) {

        Path uploadDirectory =
                Paths.get(
                        "uploads"
                )
                .toAbsolutePath()
                .normalize();

        String uploadPath =
                uploadDirectory
                        .toUri()
                        .toString();

        registry
                .addResourceHandler(
                        "/uploads/**"
                )
                .addResourceLocations(
                        uploadPath
                );
    }
}