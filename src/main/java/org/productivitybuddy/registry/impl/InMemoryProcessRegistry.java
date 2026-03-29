package org.productivitybuddy.registry.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.registry.ProcessRegistry;
import org.springframework.stereotype.Component;

@Component
public class InMemoryProcessRegistry implements ProcessRegistry {
    private final Map<Long, Process> processes = new ConcurrentHashMap<>();

    @Override
    public void upsert(Process process) {
        this.processes.put(process.getProcessInfo().getPid(), process);
    }

    @Override
    public void remove(long pid) {
        this.processes.remove(pid);
    }

    @Override
    public Map<Long, Process> findAll() {
        return Map.copyOf(this.processes);
    }

    @Override
    public Optional<Process> findByPid(long pid) {
        return Optional.ofNullable(this.processes.get(pid));
    }

    @Override
    public List<Process> findByName(String originalName) {
        return this.processes.values().stream()
            .filter(p -> p.getOriginalName().equals(originalName))
            .toList();
    }
}
