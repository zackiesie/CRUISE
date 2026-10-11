package com.example.simpleclient;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("customer-shell.fxml"));
        Parent root = loader.load();
        WindowManager.show(stage, root, "CRUISE - Car Rental");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
