package org.productivitybuddy.controller;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.productivitybuddy.lifecycle.Lifecycle;
import org.productivitybuddy.model.AnalyticsSummary;
import org.productivitybuddy.service.AnalyticsService;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsUpdateTimer implements Lifecycle {
    private final AnalyticsService analyticsService;
    private final List<AnalyticsUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private final Timeline timeline;

    public AnalyticsUpdateTimer(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        this.timeline = new Timeline(new KeyFrame(Duration.seconds(2), event -> this.tick()));
        this.timeline.setCycleCount(Timeline.INDEFINITE);
    }

    @Override
    public void start() {
        Platform.runLater(this.timeline::play);
    }

    @Override
    public void stop() {
        Platform.runLater(this.timeline::stop);
    }

    public void addListener(AnalyticsUpdateListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(AnalyticsUpdateListener listener) {
        this.listeners.remove(listener);
    }

    private void tick() {
        final AnalyticsSummary summary = this.analyticsService.getLatestSummary();
        if (summary != null) {
            for (final AnalyticsUpdateListener listener : this.listeners) {
                listener.onAnalyticsUpdate(summary);
            }
        }
    }
}
