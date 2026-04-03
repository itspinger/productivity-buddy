package org.productivitybuddy.service.impl;

import org.productivitybuddy.model.Process;
import org.productivitybuddy.model.ProcessCategory;
import org.productivitybuddy.model.ProcessInfo;
import org.productivitybuddy.registry.ProcessRegistry;

/**
 * Helpers for creating test processes and adding them to a registry.
 */
class TestHelper {

    static Process createProcess(String name, long pid, long sessionSeconds) {
        return createProcess(name, pid, sessionSeconds, ProcessCategory.UNCATEGORIZED);
    }

    static Process createProcess(String name, long pid, long sessionSeconds, ProcessCategory category) {
        final Process process = new Process(name);
        process.setProcessInfo(new ProcessInfo(pid, System.currentTimeMillis(), 0.0, 0));
        process.setTotalTimeSeconds(sessionSeconds);
        process.setProcessCategory(category);
        return process;
    }

    static Process createSaved(String name, long totalSeconds) {
        return createSaved(name, totalSeconds, ProcessCategory.UNCATEGORIZED);
    }

    static Process createSaved(String name, long totalSeconds, ProcessCategory category) {
        final Process process = new Process(name);
        process.setTotalTimeSeconds(totalSeconds);
        process.setProcessCategory(category);
        return process;
    }

    static void addToRegistry(ProcessRegistry registry, Process process) {
        registry.upsert(process);
    }
}
