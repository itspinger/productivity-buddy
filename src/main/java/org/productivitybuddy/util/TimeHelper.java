package org.productivitybuddy.util;

public final class TimeHelper {
    private static final long SECONDS_PER_MINUTE = 60;
    private static final long SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE;
    private static final long SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;
    private static final long SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY;

    private TimeHelper() {}

    public static String toString(long totalSeconds) {
        final long safeSeconds = Math.max(0, totalSeconds);

        final long weeks = safeSeconds / SECONDS_PER_WEEK;
        final long days = (safeSeconds % SECONDS_PER_WEEK) / SECONDS_PER_DAY;
        final long hours = (safeSeconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        final long minutes = (safeSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        final long seconds = safeSeconds % SECONDS_PER_MINUTE;

        if (weeks > 0) {
            return String.format("%dw %dd %02dh %02dm %02ds", weeks, days, hours, minutes, seconds);
        }

        if (days > 0) {
            return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        }

        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        }

        return String.format("%dm %02ds", minutes, seconds);
    }
}
