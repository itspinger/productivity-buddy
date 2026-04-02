package org.productivitybuddy.service;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Manages all asynchronous file I/O operations through a single-threaded executor,
 * guaranteeing that concurrent save/snapshot operations never corrupt files.
 */
public interface FileExecutorService {

    /**
     * Submits a file I/O task for asynchronous execution.
     *
     * @param task the task to execute
     * @param <T> the result type
     * @return a Future representing the pending result
     */
    <T> Future<T> submit(Callable<T> task);

    /**
     * Submits a file I/O task for asynchronous execution.
     *
     * @param task the task to execute
     * @return a Future representing the pending completion
     */
    Future<?> submit(Runnable task);

    /**
     * Gracefully shuts down the executor, waiting for pending tasks to complete.
     */
    void shutdown();

    /**
     * Waits for all submitted tasks to finish within the given timeout.
     *
     * @param timeoutSeconds maximum seconds to wait
     * @return true if all tasks completed before timeout
     */
    boolean awaitTermination(long timeoutSeconds);
}
