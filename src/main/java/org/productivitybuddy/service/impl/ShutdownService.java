package org.productivitybuddy.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.lifecycle.Lifecycle;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.service.FileExecutorService;
import org.productivitybuddy.store.ProcessStore;
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
@Slf4j
public class ShutdownService implements Lifecycle {
    private final ProcessRegistry registry;
    private final ProcessStore processStore;
    private final FileExecutorService fileExecutorService;

    public ShutdownService(ProcessRegistry registry, ProcessStore processStore, FileExecutorService fileExecutorService) {
        this.registry = registry;
        this.processStore = processStore;
        this.fileExecutorService = fileExecutorService;
    }

    @Override
    public void start() {}

    @Override
    public void stop() {
        this.fileExecutorService.submit(() -> {
            final List<Process> previouslySaved = this.processStore.loadAll();
            final Map<String, Process> activeByName = new HashMap<>();

            for (final Process active : this.registry.findAll().values()) {
                activeByName.merge(active.getOriginalName(), active, (existing, incoming) -> {
                    existing.setTotalTimeSeconds(existing.getTotalTimeSeconds() + incoming.getTotalTimeSeconds());
                    return existing;
                });
            }

            for (final Process saved : previouslySaved) {
                final Process active = activeByName.remove(saved.getOriginalName());
                if (active != null) {
                    saved.setTotalTimeSeconds(saved.getTotalTimeSeconds() + active.getTotalTimeSeconds());
                    saved.setAliasName(active.getAliasName());
                    saved.setProcessCategory(active.getProcessCategory());
                    saved.setTrackingFrozen(active.isTrackingFrozen());
                }
            }

            final List<Process> merged = new ArrayList<>(previouslySaved);
            merged.addAll(activeByName.values());
            this.processStore.saveAll(merged);
            log.info("Synced {} processes to mapping file on shutdown", merged.size());
        });

        this.fileExecutorService.shutdown();
        this.fileExecutorService.awaitTermination(10);
    }
}
