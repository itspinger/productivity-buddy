package org.productivitybuddy.lifecycle;

/**
 * Defines a managed lifecycle for application components that require explicit startup and shutdown.
 * <p>
 * Components implementing this interface are started by {@link ApplicationLifecycleManager} after the
 * Spring context is fully initialized, and stopped when the application is shutting down.
 * Implementing classes should ensure that {@code start()} initializes all required resources
 * and {@code stop()} releases them cleanly to avoid resource leaks on shutdown.
 */
public interface Lifecycle {

    /**
     * Starts the component and begins its intended operation.
     * Called once during application startup after all dependencies are injected.
     */
    void start();

    /**
     * Stops the component and releases any held resources such as threads or file handles.
     * Called during application shutdown to ensure a clean exit.
     */
    void stop();
}