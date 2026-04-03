package org.productivitybuddy.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Process {
    private String originalName;
    private String aliasName;
    private ProcessCategory processCategory;
    private boolean trackingFrozen;
    private long totalTimeSeconds;

    private transient ProcessInfo processInfo;

    public Process(String originalName) {
        this.originalName = originalName;
        this.processCategory = ProcessCategory.UNCATEGORIZED;
    }

    public String getDisplayName() {
        if (this.aliasName != null && !this.aliasName.isBlank()) {
            return this.aliasName;
        }

        return this.originalName;
    }

    public Process copy() {
        final Process copy = new Process(this.originalName);
        copy.setAliasName(this.aliasName);
        copy.setProcessCategory(this.processCategory);
        copy.setTrackingFrozen(this.trackingFrozen);
        copy.setTotalTimeSeconds(this.totalTimeSeconds);
        return copy;
    }
}
