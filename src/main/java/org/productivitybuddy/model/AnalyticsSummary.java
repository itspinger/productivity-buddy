package org.productivitybuddy.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnalyticsSummary {
    private final Map<ProcessCategory, Long> timePerCategory;
    private final List<Process> topByCpu;
    private final List<Process> topByRam;
    private final List<Process> topByTime;

    public static AnalyticsSummary empty() {
        final Map<ProcessCategory, Long> emptyMap = new EnumMap<>(ProcessCategory.class);
        for (final ProcessCategory category : ProcessCategory.values()) {
            emptyMap.put(category, 0L);
        }

        return new AnalyticsSummary(emptyMap, List.of(), List.of(), List.of());
    }
}
