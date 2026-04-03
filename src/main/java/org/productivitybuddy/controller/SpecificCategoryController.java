package org.productivitybuddy.controller;

import java.util.Comparator;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.productivitybuddy.model.AnalyticsSummary;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.model.ProcessInfo;
import org.productivitybuddy.service.ProcessAggregationService;
import org.productivitybuddy.ui.ProcessNameTableCell;
import org.productivitybuddy.ui.Icons;
import org.productivitybuddy.util.TimeHelper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SpecificCategoryController implements AnalyticsUpdateListener {
    private final AnalyticsUpdateTimer updateTimer;
    private final ProcessAggregationService processAggregationService;

    private final ObservableList<Process> tableData = FXCollections.observableArrayList();

    @FXML
    private Label headerLabel;
    @FXML
    private Button backButton;
    @FXML
    private TableView<Process> categoryTable;
    @FXML
    private TableColumn<Process, Process> nameColumn;
    @FXML
    private TableColumn<Process, String> resourceColumn;
    @FXML
    private TableColumn<Process, String> timeColumn;
    @FXML
    private PieChart top10Chart;
    @FXML
    private Label totalTimeLabel;

    private ProcessCategory category;
    private MainViewController mainController;

    public SpecificCategoryController(AnalyticsUpdateTimer updateTimer, ProcessAggregationService processAggregationService) {
        this.updateTimer = updateTimer;
        this.processAggregationService = processAggregationService;
    }

    @FXML
    private void initialize() {
        this.backButton.setGraphic(Icons.back());
        this.categoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        this.nameColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        this.nameColumn.setCellFactory(column -> new ProcessNameTableCell());

        this.resourceColumn.setCellValueFactory(cd -> {
            final Process process = cd.getValue();
            final ProcessInfo info = process.getProcessInfo();
            if (info == null) {
                return new ReadOnlyStringWrapper("N/A");
            }

            return new ReadOnlyStringWrapper(info.getRamUsageKb() + " KB | " + String.format("%.1f%%", info.getCpuUsage()));
        });

        this.resourceColumn.setCellFactory(column -> this.centeredCell());

        this.timeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
            TimeHelper.toString(this.processAggregationService.getDisplayTotalTimeSeconds(cd.getValue().getOriginalName()))));
        this.timeColumn.setCellFactory(column -> this.centeredCell());

        this.categoryTable.setItems(this.tableData);
    }

    void init(ProcessCategory category, MainViewController mainController) {
        this.category = category;
        this.mainController = mainController;
        this.headerLabel.setText(this.category.getDisplayName() + " Category");
        this.refreshData();
        this.updateTimer.addListener(this);
    }

    void disposeView() {
        this.updateTimer.removeListener(this);
    }

    @FXML
    private void onBack() {
        this.disposeView();
        this.mainController.showMainView();
    }

    @Override
    public void onAnalyticsUpdate(AnalyticsSummary ignored) {
        this.refreshData();
    }

    private void refreshData() {
        final List<Process> filtered = this.processAggregationService.getAggregatedProcessesByCategory(this.category).stream()
            .sorted(Comparator.comparingLong(
                (Process process) -> this.processAggregationService.getDisplayTotalTimeSeconds(process.getOriginalName()))
                .reversed())
            .toList();

        this.tableData.setAll(filtered);

        final List<Process> top10 = this.processAggregationService.getAggregatedProcessesByCategory(this.category).stream()
            .sorted(Comparator.comparingLong(Process::getTotalTimeSeconds).reversed())
            .limit(10)
            .toList();

        final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (final Process process : top10) {
            if (process.getTotalTimeSeconds() > 0) {
                pieData.add(new PieChart.Data(process.getDisplayName(), process.getTotalTimeSeconds()));
            }
        }

        this.top10Chart.setData(pieData);

        final long totalSeconds = this.processAggregationService.getTimePerCategory().getOrDefault(this.category, 0L);

        this.totalTimeLabel.setText(this.category.getDisplayName() + " total time - " + TimeHelper.toString(totalSeconds));
    }

    private TableCell<Process, String> centeredCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(empty ? null : item);
                this.setGraphic(null);
                this.setAlignment(Pos.CENTER);
            }
        };
    }
}
