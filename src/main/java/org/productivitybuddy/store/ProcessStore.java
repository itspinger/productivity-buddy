package org.productivitybuddy.store;

import java.nio.file.Path;
import java.util.List;
import org.productivitybuddy.model.Process;

/**
 * Persists and retrieves {@link Process} records across application sessions.
 * Implementations define the underlying storage mechanism (e.g. JSON, database).
 */
public interface ProcessStore {

    /**
     * Loads all tracked processes from the default mapping file.
     *
     * @return all persisted processes, empty list if none exist yet
     */
    List<Process> loadAll();

    /**
     * Loads all tracked processes from the specified file.
     *
     * @param path the file to load from
     * @return all persisted processes, empty list if none exist yet
     */
    List<Process> loadAll(Path path);

    /**
     * Saves all processes to the default mapping file.
     *
     * @param processes current list of tracked processes, must not be {@code null}
     */
    void saveAll(List<Process> processes);

    /**
     * Saves all processes to the specified file.
     *
     * @param processes current list of tracked processes, must not be {@code null}
     * @param path the file to save to
     */
    void saveAll(List<Process> processes, Path path);
}