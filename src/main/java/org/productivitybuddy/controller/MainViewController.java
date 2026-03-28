package org.productivitybuddy.controller;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class MainViewController {
    @FXML
    private Region toggleThumb;

    @FXML
    private HBox toggleSwitch;

    private boolean darkMode = false;

    @FXML
    private void onToggleDarkMode() {
        this.darkMode = !this.darkMode;

        final TranslateTransition transition = new TranslateTransition(Duration.millis(150), this.toggleThumb);
        transition.setToX(this.darkMode ? 20 : 0);
        transition.play();

        this.toggleSwitch.getStyleClass().clear();
        this.toggleSwitch.getStyleClass().add(this.darkMode ? "toggle-switch-on" : "toggle-switch");
    }
}
