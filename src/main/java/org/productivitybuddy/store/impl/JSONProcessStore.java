package org.productivitybuddy.store.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.config.ApplicationConfig;
import org.productivitybuddy.model.Process;
import org.productivitybuddy.store.ProcessStore;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JSONProcessStore implements ProcessStore {
    private final Gson gson;
    private final ApplicationConfig config;

    public JSONProcessStore(Gson gson, ApplicationConfig config) {
        this.gson = gson;
        this.config = config;
    }

    @Override
    public List<Process> loadAll() {
        return this.loadAll(Paths.get(this.config.getMappingFile()));
    }

    @Override
    public List<Process> loadAll(Path path) {
        if (Files.notExists(path)) {
            log.info("Mapping file {} does not exist", path);
            return new ArrayList<>();
        }

        try (final Reader reader = Files.newBufferedReader(path)) {
            final JsonObject root = this.gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("processes")) {
                log.warn("Mapping file {} is empty or missing 'processes' array", path);
                return new ArrayList<>();
            }

            final JsonArray processes = root.getAsJsonArray("processes");
            if (processes == null) {
                log.warn("Mapping file {} has null 'processes' array", path);
                return new ArrayList<>();
            }

            return StreamSupport.stream(processes.spliterator(), false)
                .map(element -> this.gson.fromJson(element, Process.class))
                .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load process info from " + path, e);
        }
    }

    @Override
    public void saveAll(List<Process> processes) {
        this.saveAll(processes, Paths.get(this.config.getMappingFile()));
    }

    @Override
    public void saveAll(List<Process> processes, Path path) {
        final JsonObject root = new JsonObject();
        root.add("processes", this.gson.toJsonTree(processes));

        try {
            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (final Writer writer = Files.newBufferedWriter(path)) {
                this.gson.toJson(root, writer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save processes to " + path, e);
        }
    }
}
