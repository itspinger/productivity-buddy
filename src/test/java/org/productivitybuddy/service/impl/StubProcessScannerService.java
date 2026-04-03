package org.productivitybuddy.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.service.ProcessScannerService;

/**
 * Minimal stub that exposes the savedProcesses map for testing.
 * No scanning, no threads — just the saved state.
 */
class StubProcessScannerService implements ProcessScannerService {
    private final Map<String, Process> savedProcesses = new ConcurrentHashMap<>();

    @Override
    public void start() {}

    @Override
    public void stop() {}

    @Override
    public void scan() {}

    @Override
    public Map<String, Process> getSavedProcesses() {
        return Map.copyOf(this.savedProcesses);
    }

    @Override
    public void replaceSavedProcesses(List<Process> processes) {
        this.savedProcesses.clear();
        for (final Process p : processes) {
            this.savedProcesses.merge(p.getOriginalName(), p.copy(), (existing, incoming) -> {
                existing.setTotalTimeSeconds(existing.getTotalTimeSeconds() + incoming.getTotalTimeSeconds());
                return existing;
            });
        }
    }

    @Override
    public void updateSavedProcess(String originalName, Consumer<Process> updater) {
        final Process saved = this.savedProcesses.get(originalName);
        if (saved != null) {
            updater.accept(saved);
        }
    }

    void putSaved(Process process) {
        this.savedProcesses.put(process.getOriginalName(), process.copy());
    }
}
