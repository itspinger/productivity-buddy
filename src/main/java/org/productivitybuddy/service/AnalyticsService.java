package org.productivitybuddy.service;

import org.productivitybuddy.lifecycle.Lifecycle;
import org.productivitybuddy.model.AnalyticsSummary;

/**
 * Provides access to aggregated analytics data about tracked processes.
 */
public interface AnalyticsService extends Lifecycle {

    /**
     * Returns the most recently computed analytics summary.
     *
     * @return the latest {@link AnalyticsSummary}, or an empty summary if not yet computed
     */
    AnalyticsSummary getLatestSummary();
}