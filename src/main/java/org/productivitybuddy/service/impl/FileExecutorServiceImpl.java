package org.productivitybuddy.service.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.productivitybuddy.service.FileExecutorService;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileExecutorServiceImpl implements FileExecutorService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(r, "file-io-thread");
        thread.setDaemon(false);
        return thread;
    });

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.executor.submit(task);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.executor.submit(task);
    }

    @Override
    public void shutdown() {
        this.executor.shutdown();
        log.info("FileExecutorService shutdown initiated");
    }

    @Override
    public boolean awaitTermination(long timeoutSeconds) {
        try {
            final boolean terminated = this.executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
            if (terminated) {
                log.info("FileExecutorService terminated gracefully");
            } else {
                log.warn("FileExecutorService did not terminate within {} seconds", timeoutSeconds);
            }

            return terminated;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("FileExecutorService termination interrupted", e);
            return false;
        }
    }
}
