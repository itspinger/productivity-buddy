package org.productivitybuddy.service.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.config.ApplicationConfig;
import org.productivitybuddy.lifecycle.Lifecycle;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.service.FileExecutorService;
import org.productivitybuddy.thread.ThreadFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(300)
@Slf4j
public class SnapshotService implements Lifecycle {
    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SS").withZone(ZoneId.systemDefault());
    private static final String CSV_HEADER = "timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name";

    private final ProcessRegistry registry;
    private final FileExecutorService fileExecutorService;
    private final ApplicationConfig config;
    private final ScheduledExecutorService scheduler = ThreadFactory.newSingleThreadScheduler("snapshot-scheduler-thread");

    public SnapshotService(ProcessRegistry registry, FileExecutorService fileExecutorService, ApplicationConfig config) {
        this.registry = registry;
        this.fileExecutorService = fileExecutorService;
        this.config = config;
    }

    @Override
    public void start() {
        this.scheduler.scheduleAtFixedRate(this::takeSnapshot,
            this.config.getSnapshotInterval(),
            this.config.getSnapshotInterval(),
            TimeUnit.SECONDS
        );

        log.info("Snapshot service started with interval {}s", this.config.getSnapshotInterval());
    }

    @Override
    public void stop() {
        this.scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Snapshot scheduler did not terminate within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping snapshot scheduler", e);
        }
        log.info("Snapshot service stopped");
    }

    public void takeSnapshot() {
        final Map<Long, Process> processes = this.registry.findAll();
        if (processes.isEmpty()) {
            return;
        }

        this.fileExecutorService.submit(() -> {
            final Instant now = Instant.now();
            final String fileName = "snapshot_" + FILE_NAME_FORMATTER.format(now) + ".csv";
            final Path path = Paths.get(fileName);

            try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(CSV_HEADER);
                writer.newLine();

                final String timestamp = now.toString();
                for (final Map.Entry<Long, Process> entry : processes.entrySet()) {
                    writer.write(formatCsvLine(timestamp, entry.getKey(), entry.getValue()));
                    writer.newLine();
                }

                log.info("Snapshot written to {}", path);
            } catch (IOException e) {
                log.error("Failed to write snapshot to {}", path, e);
            }
        });
    }

    private static String formatCsvLine(String timestamp, long pid, Process process) {
        final double cpu = process.getProcessInfo() != null ? process.getProcessInfo().getCpuUsage() : 0.0;
        final long ram = process.getProcessInfo() != null ? process.getProcessInfo().getRamUsageKb() : 0L;
        final ProcessCategory category = Optional.ofNullable(process.getProcessCategory()).orElse(ProcessCategory.UNCATEGORIZED);
        final String alias = process.getAliasName() != null ? process.getAliasName() : "";

        return "%s,%d,%s,%.1f,%d,%s,%s".formatted(timestamp, pid, process.getOriginalName(), cpu, ram, category.getDisplayName(), alias);
    }
}
