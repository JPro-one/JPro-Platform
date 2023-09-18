package one.jpro.platform.mdfx.impl;

import javafx.geometry.Orientation;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

public class AdaptiveImage extends Pane {

    private final Image img;

    public AdaptiveImage(Image image) {
        img = image;
        init();
    }

    public AdaptiveImage(String imagePath) {
        this(new Image(imagePath, true));
    }

    private void init() {
        setBackground(new Background(
                new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER, getBackgroundImagePosition())));
        img.widthProperty().addListener((p,o,n) -> requestLayout());
        img.heightProperty().addListener((p,o,n) -> requestLayout());
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    private double computeHeight(double width) {
        return width * img.getHeight() / img.getWidth();
    }

    @Override
    public double computeMinHeight(double width) {
        return width <= 0.0 ? Double.MIN_VALUE : computeHeight(width);
    }

    @Override
    public double computePrefHeight(double width) {
        return width <= 0.0 ? 1 : computeHeight(width);
    }

    @Override
    public double computeMaxHeight(double width) {
        return width <= 0.0 ? Double.MAX_VALUE : computeHeight(width);
    }

    @Override
    public double computeMinWidth(double height) {
        return 1.0;
    }

    @Override
    public double computePrefWidth(double height) {
        return img.getWidth();
    }

    @Override
    public double computeMaxWidth(double height) {
        return img.getWidth();
    }


    @Override
    public double getBaselineOffset() {
        return 30;
    }

    private BackgroundSize getBackgroundImagePosition() {
        return new BackgroundSize(1.0,1.0, true, true, true, false);
    }
}