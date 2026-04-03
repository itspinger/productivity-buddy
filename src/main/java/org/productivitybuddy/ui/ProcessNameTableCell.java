package org.productivitybuddy.ui;

import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import org.productivitybuddy.model.Process;

public class ProcessNameTableCell extends TableCell<Process, Process> {
    @Override
    protected void updateItem(Process process, boolean empty) {
        super.updateItem(process, empty);

        if (empty || process == null) {
            this.setText(null);
            this.setGraphic(null);
            return;
        }

        final Label nameLabel = new Label(process.getDisplayName());
        nameLabel.getStyleClass().add("process-name-label");

        final HBox content = new HBox(8, Icons.processCategory(process.getProcessCategory()), nameLabel);
        content.setAlignment(Pos.CENTER_LEFT);

        this.setText(null);
        this.setGraphic(content);
        this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }
}
