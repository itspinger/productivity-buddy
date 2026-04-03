package org.productivitybuddy.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.productivitybuddy.model.AnalyticsSummary;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.ui.Icons;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.productivitybuddy.util.TimeHelper;

@Component
@Scope("prototype")
public class DefaultRightPanelController {
    @FXML
    private PieChart categoryPieChart;
    @FXML
    private VBox categoryList;

    private MainViewController mainController;

    private final ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
    private boolean pieInitialized;

    void init(MainViewController mainController) {
        this.mainController = mainController;
        this.categoryPieChart.setAnimated(false);
        this.categoryPieChart.setData(this.pieData);
    }

    void update(AnalyticsSummary summary) {
        final Map<String, Long> newValues = new LinkedHashMap<>();
        for (final Map.Entry<ProcessCategory, Long> entry : summary.getTimePerCategory().entrySet()) {
            if (entry.getValue() > 0) {
                newValues.put(entry.getKey().getDisplayName(), entry.getValue());
            }
        }

        if (!this.pieInitialized || this.pieData.size() != newValues.size()) {
            this.pieData.clear();
            for (final Map.Entry<String, Long> entry : newValues.entrySet()) {
                this.pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }

            this.pieInitialized = true;
        } else {
            int i = 0;
            for (final Map.Entry<String, Long> entry : newValues.entrySet()) {
                this.pieData.get(i).setName(entry.getKey());
                this.pieData.get(i).setPieValue(entry.getValue());
                i++;
            }
        }

        this.categoryList.getChildren().clear();
        for (final ProcessCategory category : ProcessCategory.values()) {
            final long seconds = summary.getTimePerCategory().getOrDefault(category, 0L);

            final Label label = new Label(category.getDisplayName() + " - " + TimeHelper.toString(seconds));
            label.getStyleClass().add("analytics-row-label");
            label.setGraphic(Icons.processCategory(category));
            final Region spacer = new Region();

            HBox.setHgrow(spacer, Priority.ALWAYS);

            final Button detailsButton = new Button("Details");
            detailsButton.getStyleClass().add("inline-action-button");
            detailsButton.setGraphic(Icons.details());
            detailsButton.setOnAction((event) -> this.mainController.openCategoryView(category));

            final HBox row = new HBox(10, label, spacer, detailsButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("analytics-row");

            this.categoryList.getChildren().add(row);
        }
    }
}
