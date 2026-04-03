package org.productivitybuddy.service.impl;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.config.ApplicationConfig;
import org.productivitybuddy.model.AnalyticsSummary;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.model.ProcessInfo;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.service.AnalyticsService;
import org.productivitybuddy.service.ProcessAggregationService;
import org.productivitybuddy.thread.ThreadFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {
    private static final int TOP_PROCESS_COUNT = 10;

    private final ProcessRegistry registry;
    private final SnapshotService snapshotService;
    private final ApplicationConfig config;
    private final ProcessAggregationService processAggregationService;
    private final ScheduledExecutorService scheduler = ThreadFactory.newSingleThreadScheduler("analytics-thread");

    private volatile AnalyticsSummary latestSummary = AnalyticsSummary.empty();

    public AnalyticsServiceImpl(ProcessRegistry registry, SnapshotService snapshotService, ApplicationConfig config, ProcessAggregationService processAggregationService) {
        this.registry = registry;
        this.snapshotService = snapshotService;
        this.config = config;
        this.processAggregationService = processAggregationService;
    }

    @Override
    public AnalyticsSummary getLatestSummary() {
        return this.latestSummary;
    }

    @Override
    public void start() {
        this.scheduler.scheduleAtFixedRate(this::summarize, 1, 1, TimeUnit.SECONDS);
        log.info("Analytics service started");
    }

    @Override
    public void stop() {
        this.scheduler.shutdown();
        log.info("Analytics service stopped");
    }

    private void summarize() {
        final Map<Long, Process> processes = this.registry.findAll();

        final Map<ProcessCategory, Long> timePerCategory = this.processAggregationService.getTimePerCategory();
        final List<Process> topByCpu = this.topProcessesByInfoDouble(processes, ProcessInfo::getCpuUsage);
        final List<Process> topByRam = this.topProcessesByInfoLong(processes, ProcessInfo::getRamUsageKb);
        final List<Process> topByTime = this.processAggregationService.getAggregatedProcesses().stream()
            .sorted(Comparator.comparingLong(Process::getTotalTimeSeconds).reversed())
            .limit(TOP_PROCESS_COUNT)
            .toList();

        this.latestSummary = new AnalyticsSummary(timePerCategory, topByCpu, topByRam, topByTime);

        this.checkFixedTimes();
    }

    private List<Process> topProcessesByInfoLong(Map<Long, Process> processes, ToLongFunction<ProcessInfo> metric) {
        return processes.values().stream()
            .sorted(Comparator.comparingLong((Process process) -> this.metricOrZero(process, metric)).reversed())
            .limit(TOP_PROCESS_COUNT)
            .toList();
    }

    private List<Process> topProcessesByInfoDouble(Map<Long, Process> processes, ToDoubleFunction<ProcessInfo> metric) {
        return processes.values().stream()
            .sorted(Comparator.comparingDouble((Process process) -> this.metricOrZero(process, metric)).reversed())
            .limit(TOP_PROCESS_COUNT)
            .toList();
    }

    private long metricOrZero(Process process, ToLongFunction<ProcessInfo> metric) {
        final ProcessInfo info = process.getProcessInfo();
        return info != null ? metric.applyAsLong(info) : 0L;
    }

    private double metricOrZero(Process process, ToDoubleFunction<ProcessInfo> metric) {
        final ProcessInfo info = process.getProcessInfo();
        return info != null ? metric.applyAsDouble(info) : 0.0;
    }

    private void checkFixedTimes() {
        final LocalTime now = LocalTime.now().withNano(0);
        for (final LocalTime fixedTime : this.config.getSnapshotFixedTimes()) {
            if (now.equals(fixedTime)) {
                log.info("Fixed-time snapshot triggered at {}", fixedTime);
                this.snapshotService.takeSnapshot();
            }
        }
    }
}
