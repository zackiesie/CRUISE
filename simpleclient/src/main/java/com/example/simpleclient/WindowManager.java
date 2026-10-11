package com.example.simpleclient;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Centralises desktop window behaviour so every screen feels like one app. */
public final class WindowManager {
    public static final double DEFAULT_WIDTH = 1280;
    public static final double DEFAULT_HEIGHT = 800;
    public static final double MIN_WIDTH = 1000;
    public static final double MIN_HEIGHT = 680;

    private WindowManager() {}

    public static void configure(Stage stage) {
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setResizable(true);
    }

    public static void show(Stage stage, Parent root, String title) {
        configure(stage);
        if (stage.getScene() == null) stage.setScene(new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT));
        else stage.getScene().setRoot(root);
        stage.setTitle(title);
        stage.show();
    }
}
