package org.productivitybuddy.ui;

import javafx.scene.Node;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.javafx.FontIcon;
import org.productivitybuddy.model.ProcessCategory;

public final class Icons {
    private Icons() {}

    public static Node save() {
        return icon(BootstrapIcons.SAVE, 13, "toolbar-icon");
    }

    public static Node load() {
        return icon(BootstrapIcons.FOLDER2_OPEN, 13, "toolbar-icon");
    }

    public static Node shutdown() {
        return icon(BootstrapIcons.POWER, 13, "toolbar-icon", "danger-icon");
    }

    public static Node back() {
        return icon(BootstrapIcons.ARROW_LEFT, 18, "nav-icon");
    }

    public static Node details() {
        return icon(BootstrapIcons.CHEVRON_RIGHT, 12, "inline-action-icon");
    }

    public static Node changeName() {
        return icon(BootstrapIcons.PENCIL_SQUARE, 14, "button-icon");
    }

    public static Node changeCategory() {
        return icon(BootstrapIcons.TAG, 14, "button-icon");
    }

    public static Node freeze(boolean frozen) {
        return frozen
            ? icon(BootstrapIcons.PLAY_CIRCLE, 14, "button-icon")
            : icon(BootstrapIcons.PAUSE_CIRCLE, 14, "button-icon");
    }

    public static Node kill() {
        return icon(BootstrapIcons.X_OCTAGON, 14, "button-icon", "danger-icon");
    }

    public static Node processCategory(ProcessCategory category) {
        return switch (category != null ? category : ProcessCategory.UNCATEGORIZED) {
            case WORK -> icon(BootstrapIcons.BRIEFCASE, 13, "process-icon", "category-work");
            case FUN -> icon(BootstrapIcons.JOYSTICK, 13, "process-icon", "category-fun");
            case OTHER -> icon(BootstrapIcons.STARS, 13, "process-icon", "category-other");
            case UNCATEGORIZED -> icon(BootstrapIcons.APP_INDICATOR, 13, "process-icon", "category-uncategorized");
        };
    }

    private static FontIcon icon(BootstrapIcons iconCode, int size, String... styleClasses) {
        final FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(size);
        icon.getStyleClass().addAll(styleClasses);
        return icon;
    }
}
