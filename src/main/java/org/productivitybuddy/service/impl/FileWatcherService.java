package org.productivitybuddy.service.impl;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.config.ApplicationConfig;
import org.productivitybuddy.lifecycle.Lifecycle;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.service.ProcessStateService;
import org.productivitybuddy.store.ProcessStore;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileWatcherService implements Lifecycle {
    private final ProcessStore processStore;
    private final ApplicationConfig config;
    private final ProcessStateService processStateService;

    private volatile boolean running;

    private Thread watcherThread;
    private WatchService watchService;

    public FileWatcherService(ProcessStore processStore, ApplicationConfig config,
                              ProcessStateService processStateService) {
        this.processStore = processStore;
        this.config = config;
        this.processStateService = processStateService;
    }

    @Override
    public void start() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            final Path filePath = Paths.get(this.config.getMappingFile()).toAbsolutePath();
            final Path directory = filePath.getParent();

            directory.register(this.watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            this.running = true;

            this.watcherThread = new Thread(this::watch, "file-watcher-thread");
            this.watcherThread.setDaemon(true);
            this.watcherThread.start();

            log.info("FileWatcherService started, watching {}", filePath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start file watcher", e);
        }
    }

    @Override
    public void stop() {
        this.running = false;
        try {
            if (this.watchService != null) {
                this.watchService.close();
            }
        } catch (IOException e) {
            log.error("Error closing watch service", e);
        }

        if (this.watcherThread != null) {
            this.watcherThread.interrupt();
        }

        log.info("FileWatcherService stopped");
    }

    private void watch() {
        final String fileName = Paths.get(this.config.getMappingFile()).getFileName().toString();

        while (this.running) {
            try {
                final WatchKey key = this.watchService.take();

                for (final WatchEvent<?> event : key.pollEvents()) {
                    final Path changed = (Path) event.context();
                    if (changed != null && changed.toString().equals(fileName)) {
                        log.info("Detected change in {}", fileName);
                        this.applyExternalChanges();
                    }
                }

                key.reset();
            } catch (ClosedWatchServiceException e) {
                log.debug("Watch service closed, exiting watcher loop");
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void applyExternalChanges() {
        try {
            final List<Process> loaded = this.processStore.loadAll();
            this.processStateService.loadState(loaded);

            log.info("Applied external changes from {}", this.config.getMappingFile());
        } catch (Exception e) {
            log.error("Failed to apply external changes", e);
        }
    }
}
