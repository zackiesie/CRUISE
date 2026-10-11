package com.example.simpleclient;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Shared restrained entrance animation for both desktop workspaces. */
public final class PageMotion {
 private PageMotion() {}
 public static void show(StackPane host, Node page) {
  host.getChildren().setAll(page);
  FadeTransition fade=new FadeTransition(Duration.millis(180),page);
  fade.setFromValue(.25);fade.setToValue(1);fade.play();
 }
}
