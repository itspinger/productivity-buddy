package org.productivitybuddy.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.registry.ProcessRegistry;
import org.productivitybuddy.service.ProcessScannerService;
import org.productivitybuddy.service.ProcessStateService;
import org.springframework.stereotype.Component;

@Component
public class ProcessStateServiceImpl implements ProcessStateService {
    private final ProcessRegistry registry;
    private final ProcessScannerService processScannerService;

    public ProcessStateServiceImpl(ProcessRegistry registry, ProcessScannerService processScannerService) {
        this.registry = registry;
        this.processScannerService = processScannerService;
    }

    @Override
    public void renameByOriginalName(String originalName, String aliasName) {
        this.forEachMatchingProcess(originalName, process -> process.setAliasName(aliasName));
        this.updateSaved(originalName, saved -> saved.setAliasName(aliasName));
    }

    @Override
    public void changeCategoryByOriginalName(String originalName, ProcessCategory category) {
        this.forEachMatchingProcess(originalName, process -> process.setProcessCategory(category));
        this.updateSaved(originalName, saved -> saved.setProcessCategory(category));
    }

    @Override
    public void setTrackingFrozenByOriginalName(String originalName, boolean frozen) {
        this.forEachMatchingProcess(originalName, process -> process.setTrackingFrozen(frozen));
        this.updateSaved(originalName, saved -> saved.setTrackingFrozen(frozen));
    }

    @Override
    public void loadState(List<Process> loadedProcesses) {
        this.processScannerService.replaceSavedProcesses(loadedProcesses);
        this.applySavedMetadata();
    }

    @Override
    public void commitState(List<Process> persistedProcesses) {
        this.processScannerService.replaceSavedProcesses(persistedProcesses);
        this.applySavedMetadata();

        this.resetSessionTime();
    }

    private void applySavedMetadata() {
        final Map<String, Process> savedByName = new LinkedHashMap<>(this.processScannerService.getSavedProcesses());
        for (final Process active : this.registry.findAll().values()) {
            final Process saved = savedByName.get(active.getOriginalName());
            if (saved == null) {
                continue;
            }

            this.applySavedMetadata(active, saved);
        }
    }

    private void applySavedMetadata(Process active, Process saved) {
        active.setAliasName(saved.getAliasName());
        active.setProcessCategory(saved.getProcessCategory());
        active.setTrackingFrozen(saved.isTrackingFrozen());
    }

    private void resetSessionTime() {
        for (final Process process : this.registry.findAll().values()) {
            process.setTotalTimeSeconds(0);
        }
    }

    private void forEachMatchingProcess(String originalName, Consumer<Process> action) {
        for (final Process process : this.registry.findByName(originalName)) {
            action.accept(process);
        }
    }

    private void updateSaved(String originalName, Consumer<Process> action) {
        this.processScannerService.updateSavedProcess(originalName, action);
    }
}
