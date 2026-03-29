package org.productivitybuddy.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLifecycleManager {
    private final List<Lifecycle> lifecycles;

    public ApplicationLifecycleManager(List<Lifecycle> lifecycles) {
        this.lifecycles = lifecycles;
    }

    @PostConstruct
    private void init() {
        this.lifecycles.forEach(Lifecycle::start);
    }

    @PreDestroy
    private void destroy() {
        this.lifecycles.forEach(Lifecycle::stop);
    }
}
