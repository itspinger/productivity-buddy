package org.productivitybuddy.service.impl;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.config.ApplicationConfig;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessInfo;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.service.ProcessScannerService;
import org.productivitybuddy.store.ProcessStore;
import org.productivitybuddy.thread.ThreadFactory;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;

@Component
@Slf4j
public class ProcessScannerServiceImpl implements ProcessScannerService {
    private final ProcessRegistry registry;
    private final ProcessStore processStore;
    private final ApplicationConfig config;
    private final SystemInfo systemInfo;
    private final ScheduledExecutorService scheduler = ThreadFactory.newSingleThreadScheduler("process-scanner-thread");
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final Map<String, Process> savedProcesses = new ConcurrentHashMap<>();

    public ProcessScannerServiceImpl(ProcessRegistry registry, ProcessStore processStore, ApplicationConfig config, SystemInfo systemInfo) {
        this.registry = registry;
        this.processStore = processStore;
        this.config = config;
        this.systemInfo = systemInfo;
    }

    @Override
    public void start() {
        final List<Process> loaded = this.processStore.loadAll();
        this.replaceSavedProcesses(loaded);
        log.info("Loaded {} saved processes from store", loaded.size());

        this.scheduler.scheduleAtFixedRate(this::scan, 0, this.config.getMonitorInterval(), TimeUnit.MILLISECONDS);
        log.info("Process scanner started with interval {}ms", this.config.getMonitorInterval());
    }

    @Override
    public void stop() {
        this.scheduler.shutdown();
        this.forkJoinPool.shutdown();
        log.info("Process scanner stopped");
    }

    @Override
    public Map<String, Process> getSavedProcesses() {
        return Map.copyOf(this.savedProcesses);
    }

    @Override
    public void updateSavedProcess(String originalName, java.util.function.Consumer<Process> updater) {
        final Process saved = this.savedProcesses.get(originalName);
        if (saved != null) {
            updater.accept(saved);
        }
    }

    @Override
    public void replaceSavedProcesses(List<Process> processes) {
        this.savedProcesses.clear();
        for (final Process process : processes) {
            if (process.getOriginalName() == null || process.getOriginalName().isBlank()) {
                continue;
            }

            final Process copy = process.copy();

            this.savedProcesses.merge(copy.getOriginalName(), copy, (existing, incoming) -> {
                existing.setTotalTimeSeconds(existing.getTotalTimeSeconds() + incoming.getTotalTimeSeconds());
                if (incoming.getAliasName() != null && !incoming.getAliasName().isBlank()) {
                    existing.setAliasName(incoming.getAliasName());
                }

                if (incoming.getProcessCategory() != null) {
                    existing.setProcessCategory(incoming.getProcessCategory());
                }

                existing.setTrackingFrozen(incoming.isTrackingFrozen());
                return existing;
            });
        }
    }

    @Override
    public void scan() {
        final List<OSProcess> processes = this.systemInfo.getOperatingSystem().getProcesses();
        final Set<Long> activePids = ConcurrentHashMap.newKeySet();

        final long scanIntervalSeconds = this.config.getMonitorInterval() / 1000;

        this.forkJoinPool.invoke(new ProcessScanTask(
            processes,
            0,
            processes.size(),
            this.registry,
            activePids,
            scanIntervalSeconds,
            this.savedProcesses
        ));

        this.removeTerminatedProcesses(activePids);

        log.debug("Scan complete, {} active processes", activePids.size());
    }

    /**
     * Flushes session time of terminated processes into savedProcesses
     * and removes them from the registry.
     */
    private void removeTerminatedProcesses(Set<Long> activePids) {
        final var entries = this.registry.findAll().entrySet();
        for (final var entry : entries) {
            if (activePids.contains(entry.getKey())) {
                continue;
            }

            final Process terminated = entry.getValue();
            final String name = terminated.getOriginalName();

            this.savedProcesses.compute(name, (key, existing) -> {
                if (existing != null) {
                    existing.setTotalTimeSeconds(existing.getTotalTimeSeconds() + terminated.getTotalTimeSeconds());
                    return existing;
                }

                return terminated.copy();
            });

            this.registry.remove(entry.getKey());
        }
    }

    private static class ProcessScanTask extends RecursiveAction {
        private static final int THRESHOLD = 10;

        private final List<OSProcess> processes;
        private final int start;
        private final int end;
        private final ProcessRegistry registry;
        private final Set<Long> activePids;
        private final long scanIntervalSeconds;
        private final Map<String, Process> savedProcesses;

        private ProcessScanTask(List<OSProcess> processes, int start, int end, ProcessRegistry registry,
                                Set<Long> activePids,
                                long scanIntervalSeconds, Map<String, Process> savedProcesses) {
            this.processes = processes;
            this.start = start;
            this.end = end;
            this.registry = registry;
            this.activePids = activePids;
            this.scanIntervalSeconds = scanIntervalSeconds;
            this.savedProcesses = savedProcesses;
        }

        @Override
        protected void compute() {
            if (this.chunkSize() <= THRESHOLD) {
                this.computeChunk();
                return;
            }

            final int mid = this.start + (this.end - this.start) / 2;

            final ProcessScanTask left = new ProcessScanTask(this.processes, this.start, mid, this.registry, this.activePids,
                this.scanIntervalSeconds, this.savedProcesses);

            final ProcessScanTask right = new ProcessScanTask(this.processes, mid, this.end, this.registry, this.activePids,
                this.scanIntervalSeconds, this.savedProcesses);

            left.fork();
            right.compute();
            left.join();
        }

        private void computeChunk() {
            for (int i = this.start; i < this.end; i++) {
                final OSProcess osProcess = this.processes.get(i);
                if (osProcess == null) {
                    continue;
                }

                final long pid = osProcess.getProcessID();
                final String name = resolveProcessName(osProcess);
                final long startTime = osProcess.getStartTime();
                final double cpu = osProcess.getProcessCpuLoadCumulative() * 100.0;
                final long ramUsageKb = osProcess.getResidentSetSize() / 1024;

                this.activePids.add(pid);

                final Optional<Process> existing = this.registry.findByPid(pid);
                if (existing.isPresent()) {
                    final ProcessInfo existingInfo = existing.get().getProcessInfo();
                    if (existingInfo != null && existingInfo.getStartTime() != 0
                        && startTime != 0 && existingInfo.getStartTime() != startTime) {
                        this.registry.remove(pid);
                    }
                }

                final Process process = this.registry.findByPid(pid)
                    .orElseGet(() -> {
                        final Process newProcess = new Process(name);
                        final Process saved = this.savedProcesses.get(name);
                        if (saved != null) {
                            newProcess.setAliasName(saved.getAliasName());
                            newProcess.setProcessCategory(saved.getProcessCategory());
                            newProcess.setTrackingFrozen(saved.isTrackingFrozen());
                        }
                        return newProcess;
                    });

                // TODO: Time accumulation uses fixed scan interval, so freeze/unfreeze within
                //  a single interval is not precise. For better accuracy, we can track the last scan
                //  timestamp per process and compute the actual elapsed delta.
                if (!process.isTrackingFrozen() && process.getProcessInfo() != null) {
                    process.setTotalTimeSeconds(process.getTotalTimeSeconds() + this.scanIntervalSeconds);
                }

                process.setProcessInfo(new ProcessInfo(pid, startTime, cpu, ramUsageKb));
                this.registry.upsert(process);
            }
        }

        private int chunkSize() {
            return this.end - this.start;
        }

        private static String resolveProcessName(OSProcess process) {
            final String rawName = process.getName();
            if (rawName != null && !rawName.isBlank()) {
                return rawName;
            }

            final String path = process.getPath();
            if (path != null && !path.isBlank()) {
                try {
                    return Paths.get(path).getFileName().toString();
                } catch (Exception ignored) {
                    return path;
                }
            }

            return "unknown";
        }
    }
}
