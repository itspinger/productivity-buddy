package org.productivitybuddy.service;

import java.util.List;
import java.util.Map;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;

public interface ProcessAggregationService {

    /**
     * Returns the total tracked time that should be shown for a process name.
     * <p>
     * The returned value is the sum of:
     * the saved historical total for the given original process name, plus the session
     * time accumulated by all currently running instances with the same original name.
     *
     * @param originalName the system process name used as the persistence key
     * @return the display total in seconds, or {@code 0} if the process name has never
     *         been seen in either the saved baseline or the current runtime registry
     */
    long getDisplayTotalTimeSeconds(String originalName);

    /**
     * Returns aggregated process views keyed by original process name.
     * <p>
     * Each returned entry represents one logical persisted process name and combines:
     * the saved metadata for that name with the session time accumulated by all active
     * runtime instances of the same name.
     *
     * @return one aggregated process per original process name
     */
    List<Process> getAggregatedProcesses();

    /**
     * Returns aggregated process views for a single category.
     *
     * @param category the category to filter by
     * @return aggregated processes whose resolved category matches the requested category
     */
    List<Process> getAggregatedProcessesByCategory(ProcessCategory category);

    /**
     * Returns aggregated tracked time per category.
     * <p>
     * Each category total includes the saved historical total plus the session time of
     * all active runtime instances that resolve to that category.
     *
     * @return category totals in seconds for all defined categories
     */
    Map<ProcessCategory, Long> getTimePerCategory();

    /**
     * Builds a persistence snapshot that is ready to be written to
     * {@code process_info.json}.
     * <p>
     * The snapshot contains exactly one record per original process name and carries the
     * latest persisted metadata together with the correct total tracked time for that
     * name.
     *
     * @return a persisted snapshot suitable for {@link org.productivitybuddy.store.ProcessStore#saveAll(List)}
     */
    List<Process> buildProcessSnapshot();
}
