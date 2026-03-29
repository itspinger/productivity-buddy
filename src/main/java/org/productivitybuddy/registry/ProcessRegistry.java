package org.productivitybuddy.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.productivitybuddy.model.Process;

/**
 * Manages the in-memory collection of currently running {@link Process} records.
 * Acts as the central access point for live process state during an application session.
 */
public interface ProcessRegistry {

    /**
     * Inserts or updates a tracked process using its PID as the key.
     *
     * @param process the process to insert or update
     */
    void upsert(Process process);

    /**
     * Removes a tracked process by its PID.
     * Has no effect if the PID is not tracked.
     *
     * @param pid the PID of the process to remove
     */
    void remove(long pid);

    /**
     * Returns an unmodifiable snapshot of all currently tracked processes keyed by PID.
     *
     * @return unmodifiable view of all tracked processes
     */
    Map<Long, Process> findAll();

    /**
     * Looks up a tracked process by its PID.
     *
     * @param pid the PID to look up
     * @return the matching {@link Process}, or empty if not tracked
     */
    Optional<Process> findByPid(long pid);

    /**
     * Finds all tracked processes matching the given original name.
     * May return multiple results if several instances of the same process are running.
     *
     * @param originalName the system-level process name (e.g. "chrome.exe")
     * @return all matching processes, empty list if none found
     */
    List<Process> findByName(String originalName);
}