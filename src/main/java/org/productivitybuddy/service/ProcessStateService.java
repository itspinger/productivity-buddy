package org.productivitybuddy.service;

import java.util.List;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;

/**
 * This service owns name-based process operations such as renaming, category changes,
 * frozen-tracking changes, applying loaded metadata, and committing a newly saved
 * persistence snapshot. Controllers and infrastructure components should delegate these
 * mutations here instead of manually iterating over registry contents.
 */
public interface ProcessStateService {

    /**
     * Updates the alias for every active runtime instance matching the given original
     * process name.
     *
     * @param originalName the persistence key identifying the logical process group
     * @param aliasName the new alias to apply; may be {@code null} to clear the alias
     */
    void renameByOriginalName(String originalName, String aliasName);

    /**
     * Updates the category for every active runtime instance matching the given original
     * process name.
     *
     * @param originalName the persistence key identifying the logical process group
     * @param category the category to apply
     */
    void changeCategoryByOriginalName(String originalName, ProcessCategory category);

    /**
     * Updates the frozen-tracking state for every active runtime instance matching the
     * given original process name.
     *
     * @param originalName the persistence key identifying the logical process group
     * @param frozen {@code true} to freeze tracking, {@code false} to resume tracking
     */
    void setTrackingFrozenByOriginalName(String originalName, boolean frozen);

    /**
     * Called after Load or WatcherService detects a file change.
     * Updates the saved processes map and applies metadata (alias, category, freeze)
     * to active instances. Session time is not affected.
     */
    void loadState(List<Process> loadedProcesses);

    /**
     * Called after Save. Updates the saved processes map and resets session time to 0,
     * because the saved snapshot already includes it.
     */
    void commitState(List<Process> persistedProcesses);
}
