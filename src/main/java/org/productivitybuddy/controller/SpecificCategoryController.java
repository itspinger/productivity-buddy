package org.productivitybuddy.controller;

import java.util.Comparator;
import java.util.List;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.productivitybuddy.model.AnalyticsSummary;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.model.ProcessInfo;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.util.TimeHelper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SpecificCategoryController implements AnalyticsUpdateListener {
    private final ProcessRegistry registry;
    private final AnalyticsUpdateTimer updateTimer;

    private final ObservableList<Process> tableData = FXCollections.observableArrayList();

    @FXML
    private Label headerLabel;
    @FXML
    private TableView<Process> categoryTable;
    @FXML
    private TableColumn<Process, String> nameColumn;
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

    public SpecificCategoryController(ProcessRegistry registry, AnalyticsUpdateTimer updateTimer) {
        this.registry = registry;
        this.updateTimer = updateTimer;
    }

    @FXML
    private void initialize() {
        this.nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDisplayName()));

        this.resourceColumn.setCellValueFactory(cd -> {
            final Process process = cd.getValue();
            final ProcessInfo info = process.getProcessInfo();
            if (info == null) {
                return new ReadOnlyStringWrapper("N/A");
            }

            return new ReadOnlyStringWrapper(info.getRamUsageKb() + " KB | " + String.format("%.1f%%", info.getCpuUsage()));
        });

        this.timeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(TimeHelper.toString(cd.getValue().getTotalTimeSeconds())));

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
        final List<Process> filtered = this.registry.findAll().values().stream()
            .filter(process -> process.getProcessCategory() == this.category)
            .sorted(Comparator.comparingLong(Process::getTotalTimeSeconds).reversed())
            .toList();

        this.tableData.setAll(filtered);

        final List<Process> top10 = filtered.stream()
            .limit(10)
            .toList();

        final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (final Process process : top10) {
            if (process.getTotalTimeSeconds() > 0) {
                pieData.add(new PieChart.Data(process.getDisplayName(), process.getTotalTimeSeconds()));
            }
        }

        this.top10Chart.setData(pieData);

        final long totalSeconds = filtered.stream()
            .mapToLong(Process::getTotalTimeSeconds)
            .sum();

        this.totalTimeLabel.setText(this.category.getDisplayName() + " total time - " + TimeHelper.toString(totalSeconds));
    }
}
