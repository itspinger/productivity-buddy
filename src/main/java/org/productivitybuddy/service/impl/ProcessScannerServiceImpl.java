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
        loaded.forEach(p -> this.savedProcesses.put(p.getOriginalName(), p));
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
    public void scan() {
        final List<ProcessHandle> handles = ProcessHandle.allProcesses().toList();
        final Set<Long> activePids = ConcurrentHashMap.newKeySet();

        final long scanIntervalSeconds = this.config.getMonitorInterval() / 1000;
        this.forkJoinPool.invoke(new ProcessScanTask(handles, 0, handles.size(), this.registry, activePids, this.systemInfo, scanIntervalSeconds, this.savedProcesses));

        this.registry.findAll().keySet()
            .stream()
            .filter(pid -> !activePids.contains(pid))
            .forEach(this.registry::remove);

        log.debug("Scan complete, {} active processes", activePids.size());
    }

    private static class ProcessScanTask extends RecursiveAction {
        private static final int THRESHOLD = 10;

        private final List<ProcessHandle> handles;
        private final int start;
        private final int end;
        private final ProcessRegistry registry;
        private final Set<Long> activePids;
        private final SystemInfo systemInfo;
        private final long scanIntervalSeconds;
        private final Map<String, Process> savedProcesses;

        private ProcessScanTask(List<ProcessHandle> handles, int start, int end, ProcessRegistry registry, Set<Long> activePids, SystemInfo systemInfo, long scanIntervalSeconds, Map<String, Process> savedProcesses) {
            this.handles = handles;
            this.start = start;
            this.end = end;
            this.registry = registry;
            this.activePids = activePids;
            this.systemInfo = systemInfo;
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

            final ProcessScanTask left = new ProcessScanTask(this.handles, this.start, mid, this.registry, this.activePids, this.systemInfo, this.scanIntervalSeconds, this.savedProcesses);
            final ProcessScanTask right = new ProcessScanTask(this.handles, mid, this.end, this.registry, this.activePids, this.systemInfo, this.scanIntervalSeconds, this.savedProcesses);

            left.fork();
            right.compute();
            left.join();
        }

        private void computeChunk() {
            for (int i = this.start; i < this.end; i++) {
                final ProcessHandle handle = this.handles.get(i);
                if (!handle.isAlive()) {
                    continue;
                }

                final long pid = handle.pid();
                final ProcessHandle.Info handleInfo = handle.info();

                final String name = handleInfo.command()
                    .map(cmd -> Paths.get(cmd).getFileName().toString())
                    .orElse("unknown");

                final long startTime = handleInfo.startInstant()
                    .map(instant -> instant.toEpochMilli())
                    .orElse(0L);

                final OSProcess osProcess = this.systemInfo.getOperatingSystem().getProcess((int) pid);
                final double cpu = osProcess != null ? osProcess.getProcessCpuLoadCumulative() * 100.0 : 0.0;
                final long ramUsageKb = osProcess != null ? osProcess.getResidentSetSize() / 1024 : 0L;

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
                            newProcess.setTotalTimeSeconds(saved.getTotalTimeSeconds());
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
    }
}
