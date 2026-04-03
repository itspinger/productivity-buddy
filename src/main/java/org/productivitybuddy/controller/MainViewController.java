package org.productivitybuddy.controller;

import java.util.ArrayList;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.productivitybuddy.model.AnalyticsSummary;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.ui.ProcessNameTableCell;
import org.productivitybuddy.view.FXMLView;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MainViewController implements AnalyticsUpdateListener {
    private final ProcessRegistry registry;
    private final AnalyticsUpdateTimer updateTimer;
    private final ApplicationContext context;

    private final ObservableList<Process> tableData = FXCollections.observableArrayList();

    @FXML
    private BorderPane root;
    @FXML
    private TextField searchField;
    @FXML
    private TableView<Process> processTable;
    @FXML
    private TableColumn<Process, Process> processNameColumn;
    @FXML
    private TableColumn<Process, String> processCategoryColumn;
    @FXML
    private HBox splitView;
    @FXML
    private VBox processPanel;
    @FXML
    private VBox rightPanel;
    @FXML
    private VBox fullWidthHost;

    private FilteredList<Process> filteredData;
    private Node defaultRightContent;
    private DefaultRightPanelController defaultRightPanelController;
    private Runnable rightPanelCleanup = () -> {};
    private Runnable fullWidthCleanup = () -> {};

    public MainViewController(ProcessRegistry registry, AnalyticsUpdateTimer updateTimer, ApplicationContext context) {
        this.registry = registry;
        this.updateTimer = updateTimer;
        this.context = context;
    }

    void openCategoryView(ProcessCategory category) {
        final FXMLView<SpecificCategoryController> loadedView = this.loadView("specific-category-view.fxml");
        loadedView.getController().init(category, this);
        this.cleanupFullWidthView();
        this.cleanupRightPanelView();
        this.showFullWidthView();
        this.fullWidthCleanup = loadedView.getController()::disposeView;
        this.setHostContent(this.fullWidthHost, loadedView.getView());
    }

    private void showFullWidthView() {
        this.splitView.setManaged(false);
        this.splitView.setVisible(false);
        this.fullWidthHost.setManaged(true);
        this.fullWidthHost.setVisible(true);
    }

    @FXML
    private void initialize() {
        this.initializeTheme();
        this.initializeTable();
        this.initializeSearch();
        this.initializeSelection();

        this.loadDefaultRightPanel();
        this.showMainView();
        this.refreshTable();
        if (this.defaultRightPanelController != null) {
            this.defaultRightPanelController.update(this.updateTimer.getCurrentSummary());
        }
        this.updateTimer.addListener(this);
    }

    private void initializeTheme() {
        if (!this.root.getStyleClass().contains("app-root")) {
            this.root.getStyleClass().add("app-root");
        }

        if (!this.root.getStyleClass().contains("theme-light")) {
            this.root.getStyleClass().add("theme-light");
        }
    }

    private void initializeTable() {
        this.processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        this.processNameColumn.setCellValueFactory(cd ->
            new ReadOnlyObjectWrapper<>(cd.getValue()));
        this.processNameColumn.setCellFactory(column -> new ProcessNameTableCell());
        this.processCategoryColumn.setCellValueFactory(cd ->
            new ReadOnlyStringWrapper(cd.getValue().getProcessCategory() != null
                                      ? cd.getValue().getProcessCategory().getDisplayName() : ""));
        this.filteredData = new FilteredList<>(this.tableData, p -> true);
        this.processTable.setItems(this.filteredData);
    }

    private void initializeSearch() {
        this.searchField.textProperty().addListener((obs, oldVal, newVal) ->
            this.filteredData.setPredicate(process -> {
                if (newVal == null || newVal.isBlank()) {
                    return true;
                }

                final String lower = newVal.toLowerCase();
                return process.getDisplayName().toLowerCase().contains(lower)
                       || process.getOriginalName().toLowerCase().contains(lower);
            }));
    }

    private void initializeSelection() {
        this.processTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                final Process selected = this.processTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    this.openProcessDetail(selected);
                }
            }
        });
    }

    public void showMainView() {
        this.cleanupFullWidthView();
        this.cleanupRightPanelView();
        this.showSplitView();
        this.rightPanel.getChildren().setAll(this.defaultRightContent);
    }

    private void cleanupFullWidthView() {
        this.fullWidthCleanup.run();
        this.fullWidthCleanup = () -> {};
    }

    void openProcessDetail(Process process) {
        final FXMLView<ProcessDetailController> loadedView = this.loadView("process-detail-view.fxml");
        loadedView.getController().init(process, this);
        this.setRightContent(loadedView.getView());
        this.rightPanelCleanup = loadedView.getController()::disposeView;
    }

    public void setRightContent(Node content) {
        this.cleanupRightPanelView();
        this.showSplitView();
        this.setHostContent(this.rightPanel, content);
    }

    private void showSplitView() {
        this.processPanel.setManaged(true);
        this.processPanel.setVisible(true);
        this.splitView.setManaged(true);
        this.splitView.setVisible(true);
        this.fullWidthHost.setManaged(false);
        this.fullWidthHost.setVisible(false);
        this.fullWidthHost.getChildren().clear();
    }

    private void cleanupRightPanelView() {
        this.rightPanelCleanup.run();
        this.rightPanelCleanup = () -> {};
    }

    private void loadDefaultRightPanel() {
        final FXMLView<DefaultRightPanelController> loadedView = this.loadView("default-right-panel.fxml");
        this.defaultRightContent = loadedView.getView();
        this.defaultRightPanelController = loadedView.getController();
        this.defaultRightPanelController.init(this);
    }

    @Override
    public void onAnalyticsUpdate(AnalyticsSummary summary) {
        this.refreshTable();

        if (this.defaultRightPanelController != null) {
            this.defaultRightPanelController.update(summary);
        }
    }

    public void refreshTable() {
        final Process selected = this.processTable.getSelectionModel().getSelectedItem();
        final long selectedPid = selected != null && selected.getProcessInfo() != null
                                 ? selected.getProcessInfo().getPid() : -1;

        this.tableData.setAll(new ArrayList<>(this.registry.findAll().values()));

        if (selectedPid != -1) {
            for (final Process process : this.tableData) {
                if (process.getProcessInfo() != null && process.getProcessInfo().getPid() == selectedPid) {
                    this.processTable.getSelectionModel().select(process);
                    break;
                }
            }
        }
    }

    private <T> FXMLView<T> loadView(String resourcePath) {
        return FXMLView.loadView(resourcePath, this.context);
    }

    private void setHostContent(VBox host, Node content) {
        host.getChildren().setAll(content);
        VBox.setVgrow(content, Priority.ALWAYS);
    }
}
