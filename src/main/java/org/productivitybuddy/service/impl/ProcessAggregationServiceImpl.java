package org.productivitybuddy.service.impl;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.service.ProcessAggregationService;
import org.productivitybuddy.service.ProcessScannerService;
import org.springframework.stereotype.Component;

/**
 * Aggregates runtime process instances by original name and combines them with
 * saved (historical) data to produce display-ready totals.
 * <p>
 * Runtime instances are keyed by PID, but persistence uses original name.
 * This service bridges the two: it sums session time across all PIDs that share
 * a name, then adds the saved historical time once per name.
 */
@Component
public class ProcessAggregationServiceImpl implements ProcessAggregationService {
    private final ProcessRegistry registry;
    private final ProcessScannerService processScannerService;

    public ProcessAggregationServiceImpl(ProcessRegistry registry, ProcessScannerService processScannerService) {
        this.registry = registry;
        this.processScannerService = processScannerService;
    }

    @Override
    public long getDisplayTotalTimeSeconds(String originalName) {
        final Map<String, Process> saved = this.processScannerService.getSavedProcesses();
        final long savedTime = saved.containsKey(originalName) ? saved.get(originalName).getTotalTimeSeconds() : 0L;

        return savedTime + this.sumSessionTime(originalName);
    }

    @Override
    public List<Process> getAggregatedProcesses() {
        return List.copyOf(this.buildAggregatedMap().values());
    }

    @Override
    public List<Process> getAggregatedProcessesByCategory(ProcessCategory category) {
        return this.buildAggregatedMap().values().stream()
            .filter(p -> this.categoryOf(p) == category)
            .toList();
    }

    @Override
    public Map<ProcessCategory, Long> getTimePerCategory() {
        final Map<ProcessCategory, Long> totals = new EnumMap<>(ProcessCategory.class);
        for (final ProcessCategory category : ProcessCategory.values()) {
            totals.put(category, 0L);
        }

        for (final Process process : this.buildAggregatedMap().values()) {
            totals.merge(this.categoryOf(process), process.getTotalTimeSeconds(), Long::sum);
        }

        return totals;
    }

    @Override
    public List<Process> buildProcessSnapshot() {
        return this.buildAggregatedMap().values().stream()
            .map(Process::copy)
            .toList();
    }

    /**
     * Builds one aggregated process per original name by merging saved data
     * with active runtime instances.
     */
    private Map<String, Process> buildAggregatedMap() {
        final Map<String, Process> saved = this.processScannerService.getSavedProcesses();
        final Map<String, Process> result = new HashMap<>();

        // Sum session time per name and pick one active representative for metadata
        final Map<String, Long> sessionTime = new HashMap<>();
        final Map<String, Process> activeRepresentative = new HashMap<>();

        for (final Process active : this.registry.findAll().values()) {
            final String name = active.getOriginalName();
            sessionTime.merge(name, active.getTotalTimeSeconds(), Long::sum);
            activeRepresentative.putIfAbsent(name, active);
        }

        // Merge all unique names from both saved and active
        final Set<String> allNames = new HashSet<>(saved.keySet());
        allNames.addAll(activeRepresentative.keySet());

        for (final String name : allNames) {
            final Process s = saved.get(name);
            final Process active = activeRepresentative.get(name);
            result.put(name, this.buildAggregated(name, s, active, sessionTime));
        }

        return result;
    }

    private Process buildAggregated(String name, Process saved, Process active, Map<String, Long> sessionTime) {
        final Process aggregated = new Process(name);

        // Active metadata takes priority over saved; one of them is always non-null
        final Process source = active != null ? active : saved;
        aggregated.setAliasName(source.getAliasName());
        aggregated.setProcessCategory(source.getProcessCategory());
        aggregated.setTrackingFrozen(source.isTrackingFrozen());
        aggregated.setProcessInfo(active != null ? active.getProcessInfo() : null);

        final long savedTime = saved != null ? saved.getTotalTimeSeconds() : 0L;
        aggregated.setTotalTimeSeconds(savedTime + sessionTime.getOrDefault(name, 0L));

        return aggregated;
    }

    private long sumSessionTime(String originalName) {
        long total = 0;
        for (final Process process : this.registry.findAll().values()) {
            if (originalName.equals(process.getOriginalName())) {
                total += process.getTotalTimeSeconds();
            }
        }

        return total;
    }

    private ProcessCategory categoryOf(Process process) {
        return process.getProcessCategory() != null ? process.getProcessCategory() : ProcessCategory.UNCATEGORIZED;
    }
}
