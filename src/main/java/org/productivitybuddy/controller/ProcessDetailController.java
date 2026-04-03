package org.productivitybuddy.controller;

import java.util.List;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
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
public class ProcessDetailController implements AnalyticsUpdateListener {
    private final ProcessRegistry registry;
    private final AnalyticsUpdateTimer updateTimer;

    @FXML
    private Label nameLabel;
    @FXML
    private Label totalTimeLabel;
    @FXML
    private Label ramLabel;
    @FXML
    private Label ramRankLabel;
    @FXML
    private Label cpuLabel;
    @FXML
    private Label cpuRankLabel;
    @FXML
    private Button freezeButton;

    private Process process;
    private MainViewController mainController;

    public ProcessDetailController(ProcessRegistry registry, AnalyticsUpdateTimer updateTimer) {
        this.registry = registry;
        this.updateTimer = updateTimer;
    }

    void init(Process process, MainViewController mainController) {
        this.process = process;
        this.mainController = mainController;

        this.freezeButton.setText(this.process.isTrackingFrozen() ? "Unfreeze Tracking" : "Freeze Tracking");
        this.refreshInfo();
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

    @FXML
    private void onKill() {
        if (this.process.getProcessInfo() == null) {
            return;
        }

        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Kill process " + this.process.getDisplayName() + " (PID: " + this.process.getProcessInfo().getPid() + ")?",
            ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirm Kill");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                ProcessHandle.of(this.process.getProcessInfo().getPid()).ifPresent(ProcessHandle::destroy);
                this.disposeView();
                this.mainController.showMainView();
            }
        });
    }

    @FXML
    private void onToggleFreeze() {
        this.process.setTrackingFrozen(!this.process.isTrackingFrozen());
        this.freezeButton.setText(this.process.isTrackingFrozen() ? "Unfreeze Tracking" : "Freeze Tracking");
    }

    @FXML
    private void onChangeName() {
        final TextInputDialog dialog = new TextInputDialog(
            this.process.getAliasName() != null ? this.process.getAliasName() : "");
        dialog.setTitle("Change Name");
        dialog.setHeaderText("Set alias for " + this.process.getOriginalName());
        dialog.setContentText("Alias:");
        dialog.showAndWait().ifPresent(name -> {
            this.process.setAliasName(name.isBlank() ? null : name);
            this.refreshInfo();
            this.mainController.refreshTable();
        });
    }

    @FXML
    private void onChangeCategory() {
        final List<ProcessCategory> categories = List.of(ProcessCategory.values());
        final ChoiceDialog<ProcessCategory> dialog = new ChoiceDialog<>(this.process.getProcessCategory(), categories);
        dialog.setTitle("Change Category");
        dialog.setHeaderText("Select category for " + this.process.getDisplayName());
        dialog.showAndWait().ifPresent(category -> {
            this.process.setProcessCategory(category);
            this.refreshInfo();
            this.mainController.refreshTable();
        });
    }

    @Override
    public void onAnalyticsUpdate(AnalyticsSummary ignored) {
        this.refreshInfo();
    }

    private void refreshInfo() {
        if (this.process.getProcessInfo() == null) {
            return;
        }

        final Optional<Process> live = this.registry.findByPid(this.process.getProcessInfo().getPid());
        if (live.isEmpty()) {
            this.nameLabel.setText(this.process.getDisplayName() + " (terminated)");
            return;
        }

        this.process = live.get();
        final ProcessInfo info = this.process.getProcessInfo();

        this.nameLabel.setText(this.process.getDisplayName());
        this.totalTimeLabel.setText("Total time - " + TimeHelper.toString(this.process.getTotalTimeSeconds()));

        final double cpu = info != null ? info.getCpuUsage() : 0.0;
        final long ram = info != null ? info.getRamUsageKb() : 0L;

        this.cpuLabel.setText(String.format("CPU usage %.2f%%", cpu));
        this.ramLabel.setText(String.format("RAM usage %d KB", ram));

        final var allProcesses = this.registry.findAll().values();

        final long ramRank = allProcesses.stream()
            .filter(p -> p.getProcessInfo() != null && p.getProcessInfo().getRamUsageKb() > ram)
            .count() + 1;
        this.ramRankLabel.setText(ramRank + this.ordinalSuffix(ramRank) + " on RAM usage");

        final long cpuRank = allProcesses.stream()
            .filter(p -> p.getProcessInfo() != null && p.getProcessInfo().getCpuUsage() > cpu)
            .count() + 1;
        this.cpuRankLabel.setText(cpuRank + this.ordinalSuffix(cpuRank) + " on CPU usage");
    }

    private String ordinalSuffix(long rank) {
        if (rank % 100 >= 11 && rank % 100 <= 13) {
            return "th";
        }
        
        return switch ((int) (rank % 10)) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
