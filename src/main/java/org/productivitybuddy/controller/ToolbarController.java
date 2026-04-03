package org.productivitybuddy.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.service.FileExecutorService;
import org.productivitybuddy.store.ProcessStore;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ToolbarController {
    private final ProcessRegistry registry;
    private final ProcessStore processStore;
    private final FileExecutorService fileExecutorService;

    @FXML
    private HBox toolbar;
    @FXML
    private HBox toggleSwitch;
    @FXML
    private Region toggleThumb;

    private boolean darkMode;

    public ToolbarController(ProcessRegistry registry, ProcessStore processStore, FileExecutorService fileExecutorService) {
        this.registry = registry;
        this.processStore = processStore;
        this.fileExecutorService = fileExecutorService;
    }

    @FXML
    private void onSave() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Process Mapping");
        fileChooser.setInitialFileName("process_info.json");

        final ExtensionFilter jsonFiles = new ExtensionFilter("JSON Files", "*.json");
        fileChooser.getExtensionFilters().add(jsonFiles);

        final File file = fileChooser.showSaveDialog(this.toolbar.getScene().getWindow());
        if (file == null) {
            return;
        }

        this.fileExecutorService.submit(() -> {
            final List<Process> processes = new ArrayList<>(this.registry.findAll().values());
            this.processStore.saveAll(processes, file.toPath());
        });
    }

    @FXML
    private void onLoad() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Process Mapping");

        final ExtensionFilter jsonFiles = new ExtensionFilter("JSON Files", "*.json");
        fileChooser.getExtensionFilters().add(jsonFiles);

        final File file = fileChooser.showOpenDialog(this.toolbar.getScene().getWindow());
        if (file == null) {
            return;
        }

        this.fileExecutorService.submit(() -> {
            final List<Process> loaded = this.processStore.loadAll(file.toPath());
            Platform.runLater(() -> {
                for (final Process saved : loaded) {
                    for (final Process active : this.registry.findByName(saved.getOriginalName())) {
                        active.setAliasName(saved.getAliasName());
                        active.setProcessCategory(saved.getProcessCategory());
                        active.setTrackingFrozen(saved.isTrackingFrozen());
                    }
                }
            });
        });
    }

    @FXML
    private void onShutdown() {
        this.fileExecutorService.submit(() -> {
            final List<Process> processes = new ArrayList<>(this.registry.findAll().values());
            this.processStore.saveAll(processes);
        });

        this.fileExecutorService.shutdown();
        this.fileExecutorService.awaitTermination(10);

        Platform.exit();
    }

    @FXML
    private void onToggleDarkMode() {
        this.darkMode = !this.darkMode;

        final TranslateTransition transition = new TranslateTransition(Duration.millis(150), this.toggleThumb);
        transition.setToX(this.darkMode ? 20 : 0);
        transition.play();

        this.toggleSwitch.getStyleClass().setAll(this.darkMode ? "toggle-switch-on" : "toggle-switch");
    }
}
