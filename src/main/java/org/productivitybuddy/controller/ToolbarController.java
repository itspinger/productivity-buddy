package org.productivitybuddy.controller;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.config.ApplicationConfig;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.service.ProcessAggregationService;
import org.productivitybuddy.service.ProcessStateService;
import org.productivitybuddy.service.FileExecutorService;
import org.productivitybuddy.store.ProcessStore;
import org.productivitybuddy.ui.Icons;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Slf4j
public class ToolbarController {
    private final ApplicationConfig config;
    private final ProcessStore processStore;
    private final FileExecutorService fileExecutorService;
    private final ProcessAggregationService processAggregationService;
    private final ProcessStateService processStateService;

    @FXML
    private HBox toolbar;
    @FXML
    private Button saveButton;
    @FXML
    private Button loadButton;
    @FXML
    private Button shutdownButton;
    @FXML
    private HBox toggleSwitch;
    @FXML
    private Region toggleThumb;

    private boolean darkMode;

    public ToolbarController(ApplicationConfig config, ProcessStore processStore, FileExecutorService fileExecutorService, ProcessAggregationService processAggregationService, ProcessStateService processStateService) {
        this.config = config;
        this.processStore = processStore;
        this.fileExecutorService = fileExecutorService;
        this.processAggregationService = processAggregationService;
        this.processStateService = processStateService;
    }

    @FXML
    private void initialize() {
        this.saveButton.getStyleClass().add("toolbar-button");
        this.loadButton.getStyleClass().add("toolbar-button");
        this.shutdownButton.getStyleClass().addAll("toolbar-button", "button-danger");
        this.saveButton.setGraphic(Icons.save());
        this.loadButton.setGraphic(Icons.load());
        this.shutdownButton.setGraphic(Icons.shutdown());
        this.syncToggleVisualState();
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

        final Task<List<Process>> saveTask = new Task<>() {
            @Override
            protected List<Process> call() {
                final List<Process> snapshot = ToolbarController.this.processAggregationService.buildProcessSnapshot();
                ToolbarController.this.processStore.saveAll(snapshot, file.toPath());
                return snapshot;
            }
        };

        saveTask.setOnSucceeded((event) -> {
            if (this.isPrimaryMappingFile(file.toPath())) {
                this.processStateService.commitState(saveTask.getValue());
            }
        });

        saveTask.setOnFailed(event -> log.error("Failed to save process mapping to {}", file.toPath(), saveTask.getException()));

        this.fileExecutorService.submit(saveTask);
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

        final Task<List<Process>> loadTask = new Task<>() {
            @Override
            protected List<Process> call() {
                return ToolbarController.this.processStore.loadAll(file.toPath());
            }
        };

        loadTask.setOnSucceeded(event -> this.processStateService.loadState(loadTask.getValue()));
        loadTask.setOnFailed(event -> log.error("Failed to load process mapping from {}", file.toPath(), loadTask.getException()));

        this.fileExecutorService.submit(loadTask);
    }

    @FXML
    private void onShutdown() {
        Platform.exit();
    }

    @FXML
    private void onToggleDarkMode() {
        this.darkMode = !this.darkMode;

        final TranslateTransition transition = new TranslateTransition(Duration.millis(150), this.toggleThumb);
        transition.setToX(this.darkMode ? 16 : 0);
        transition.play();

        this.syncToggleVisualState();
        this.applyTheme();
    }

    private void applyTheme() {
        if (this.toolbar.getScene() == null) {
            return;
        }

        final Parent root = this.toolbar.getScene().getRoot();
        root.getStyleClass().removeAll("theme-light", "theme-dark");
        root.getStyleClass().add(this.darkMode ? "theme-dark" : "theme-light");
    }

    private void syncToggleVisualState() {
        this.toggleSwitch.getStyleClass().setAll("toggle-switch");
        if (this.darkMode) {
            this.toggleSwitch.getStyleClass().add("toggle-switch-on");
        }
    }

    private boolean isPrimaryMappingFile(Path path) {
        final Path selectedPath = path.toAbsolutePath().normalize();
        final Path primaryPath = Paths.get(this.config.getMappingFile()).toAbsolutePath().normalize();
        return selectedPath.equals(primaryPath);
    }
}
