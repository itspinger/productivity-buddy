package org.productivitybuddy.service.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.lifecycle.Lifecycle;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.service.ProcessAggregationService;
import org.productivitybuddy.service.FileExecutorService;
import org.productivitybuddy.store.ProcessStore;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Responsible for merging session-accumulated time into the persistent process_info.json
 * on application shutdown. Runs as the last lifecycle component to stop.
 * <p>
 * For each previously saved process, the session time is added on top of the stored value.
 * New processes discovered during this session are appended. Processes that were saved
 * but not active in this session remain untouched.
 */
@Component
@Order(-1000)
@Slf4j
public class ShutdownService implements Lifecycle {
    private final ProcessStore processStore;
    private final FileExecutorService fileExecutorService;
    private final ProcessAggregationService processAggregationService;

    public ShutdownService(ProcessStore processStore, FileExecutorService fileExecutorService, ProcessAggregationService processAggregationService) {
        this.processStore = processStore;
        this.fileExecutorService = fileExecutorService;
        this.processAggregationService = processAggregationService;
    }

    @Override
    public void start() {}

    @Override
    public void stop() {
        this.fileExecutorService.submit(() -> {
            final List<Process> snapshot = this.processAggregationService.buildProcessSnapshot();
            this.processStore.saveAll(snapshot);
            log.info("Synced {} processes to mapping file on shutdown", snapshot.size());
        });

        this.fileExecutorService.shutdown();
        this.fileExecutorService.awaitTermination(10);
    }
}
