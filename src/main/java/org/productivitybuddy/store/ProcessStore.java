package org.productivitybuddy.store;

import java.util.List;
import org.productivitybuddy.model.Process;

/**
 * Persists and retrieves {@link Process} records across application sessions.
 * Implementations define the underlying storage mechanism (e.g. JSON, database).
 */
public interface ProcessStore {

    /**
     * Loads all tracked processes from the last session.
     *
     * @return all persisted processes, empty list if none exist yet
     */
    List<Process> loadAll();

    /**
     * Overwrites all persisted processes with the current state.
     * Should be called periodically and on shutdown to avoid data loss.
     *
     * @param processes current list of tracked processes, must not be {@code null}
     */
    void saveAll(List<Process> processes);
}