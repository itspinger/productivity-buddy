package org.productivitybuddy.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import org.productivitybuddy.lifecycle.Lifecycle;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.registry.ProcessRegistry;

/**
 * Responsible for periodically scanning all active system processes and maintaining
 * their state in the {@link ProcessRegistry}.
 * <p>
 * On each scan cycle, the full list of active {@link ProcessHandle}s is retrieved from the OS
 * and processed in parallel using a {@link ForkJoinPool}. Processes that are no longer active are
 * automatically removed from the registry.
 */
public interface ProcessScannerService extends Lifecycle {

    /**
     * Performs a single scan of all active system processes.
     * Detected processes are upserted into the {@link ProcessRegistry},
     * and processes that are no longer running are removed.
     * <p>
     * Called automatically on each scheduled interval, but can also be invoked directly for testing.
     */
    void scan();

    /**
     * Returns the saved baseline currently loaded from persisted storage and keyed by
     * original process name.
     * <p>
     * The returned map represents the historical state used to seed runtime metadata and
     * to calculate display totals. Callers must treat the returned view as read-only.
     *
     * @return the saved baseline keyed by original process name
     */
    Map<String, Process> getSavedProcesses();

    /**
     * Replaces the in-memory saved baseline with the provided persisted process list.
     * <p>
     * Implementations should normalize the input into one entry per original process
     * name and detach the stored values from caller-owned instances.
     *
     * @param processes the persisted processes that should become the new saved baseline
     */
    void replaceSavedProcesses(List<Process> processes);
}
