package org.productivitybuddy.model;

public enum ProcessCategory {
    WORK("Work"),
    FUN("Fun"),
    OTHER("Other"),
    UNCATEGORIZED("Uncategorized");

    private final String displayName;

    ProcessCategory(String displayName) {
        this.displayName = displayName;
    }

    public static ProcessCategory fromDisplayName(String displayName) {
        for (final ProcessCategory processCategory : ProcessCategory.values()) {
            if (processCategory.getDisplayName().equalsIgnoreCase(displayName)) {
                return processCategory;
            }
        }

        return ProcessCategory.UNCATEGORIZED;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
