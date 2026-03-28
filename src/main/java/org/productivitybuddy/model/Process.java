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

    public String getDisplayName() {
        if (this.aliasName != null && !this.aliasName.isBlank()) {
            return this.aliasName;
        }

        return this.originalName;
    }
}
