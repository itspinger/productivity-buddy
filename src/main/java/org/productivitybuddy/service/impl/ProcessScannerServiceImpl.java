package org.productivitybuddy.service.impl;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
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
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;

@Component
@Slf4j
public class ProcessScannerServiceImpl implements ProcessScannerService {
    private final ProcessRegistry registry;
    private final ApplicationConfig config;
    private final SystemInfo systemInfo;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();

    public ProcessScannerServiceImpl(ProcessRegistry registry, ApplicationConfig config, SystemInfo systemInfo) {
        this.registry = registry;
        this.config = config;
        this.systemInfo = systemInfo;
    }

    @Override
    public void start() {
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

        this.forkJoinPool.invoke(new ProcessScanTask(handles, 0, handles.size(), this.registry, activePids, this.systemInfo));

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

        private ProcessScanTask(List<ProcessHandle> handles, int start, int end, ProcessRegistry registry, Set<Long> activePids, SystemInfo systemInfo) {
            this.handles = handles;
            this.start = start;
            this.end = end;
            this.registry = registry;
            this.activePids = activePids;
            this.systemInfo = systemInfo;
        }

        @Override
        protected void compute() {
            if (this.chunkSize() <= THRESHOLD) {
                this.computeChunk();
                return;
            }

            final int mid = this.start + (this.end - this.start) / 2;

            final ProcessScanTask left = new ProcessScanTask(this.handles, this.start, mid, this.registry, this.activePids, systemInfo);
            final ProcessScanTask right = new ProcessScanTask(this.handles, mid, this.end, this.registry, this.activePids, systemInfo);

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
                    .orElseGet(() -> new Process(name));

                process.setProcessInfo(new ProcessInfo(pid, startTime, cpu, ramUsageKb));
                this.registry.upsert(process);
            }
        }

        private int chunkSize() {
            return this.end - this.start;
        }
    }
}
