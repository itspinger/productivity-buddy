package org.productivitybuddy.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class ThreadFactory {

    private ThreadFactory() {}

    /**
     * Creates a new single-threaded {@link ScheduledExecutorService} using a daemon thread.
     * <p>
     * Tasks submitted to this executor are executed sequentially on a single thread.
     * The thread is marked as daemon, meaning it will not block JVM shutdown.
     *
     * @param name the name assigned to the created thread
     * @return a configured single-threaded scheduled executor service
     */
    public static ScheduledExecutorService newSingleThreadScheduler(String name) {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        });
    }
}
