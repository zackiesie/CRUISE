package com.example.simpleclient;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

/** Fleet editor: side-by-side on wide desktops, stacked when the workspace narrows. */
public final class ResponsiveColumns extends Pane {
    private static final double GAP = 22;
    private boolean stacked(double width) { return width < 900; }
    @Override protected double computeMinWidth(double height) { return 280; }
    @Override protected double computePrefWidth(double height) { return 1080; }
    @Override protected double computePrefHeight(double width) {
        if (getChildren().size() != 2) return 0;
        double w = width < 0 ? Math.max(280, getWidth()) : width;
        Region list = (Region) getChildren().get(0), form = (Region) getChildren().get(1);
        return stacked(w) ? 220 + GAP + form.prefHeight(w)
            : Math.max(list.prefHeight(280), form.prefHeight(Math.max(280,w - 280 - GAP)));
    }
    @Override protected void layoutChildren() {
        if (getChildren().size() != 2) return;
        Region list = (Region) getChildren().get(0), form = (Region) getChildren().get(1);
        double w = getWidth();
        if (stacked(w)) {
            list.resizeRelocate(0,0,w,220);
            form.resizeRelocate(0,220 + GAP,w,form.prefHeight(w));
        } else {
            list.resizeRelocate(0,0,280,getHeight());
            form.resizeRelocate(280 + GAP,0,w - 280 - GAP,getHeight());
        }
    }
}
