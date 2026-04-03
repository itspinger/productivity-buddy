package org.productivitybuddy.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

@Component
public class ApplicationLifecycleManager {
    private final List<Lifecycle> orderedLifecycles;

    public ApplicationLifecycleManager(List<Lifecycle> lifecycles) {
        this.orderedLifecycles = new ArrayList<>(lifecycles);
        AnnotationAwareOrderComparator.sort(this.orderedLifecycles);
    }

    @PostConstruct
    private void init() {
        this.orderedLifecycles.forEach(Lifecycle::start);
    }

    @PreDestroy
    private void destroy() {
        final List<Lifecycle> shutdownOrder = new ArrayList<>(this.orderedLifecycles);
        Collections.reverse(shutdownOrder);
        shutdownOrder.forEach(Lifecycle::stop);
    }
}
