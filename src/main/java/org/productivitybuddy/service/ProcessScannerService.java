package org.productivitybuddy.service;

import java.util.concurrent.ForkJoinPool;
import org.productivitybuddy.lifecycle.Lifecycle;
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
}