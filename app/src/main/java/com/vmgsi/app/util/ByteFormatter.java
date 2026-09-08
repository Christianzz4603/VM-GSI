package com.vmgsi.app.util;

/**
 * Plain Java utility, called directly from Kotlin code (e.g. GsiCatalogScreen)
 * with no wrapper needed — Kotlin and Java interop freely in the same module.
 * Kept in Java here since it's a simple, stable, dependency-free helper —
 * exactly the kind of piece where the language choice doesn't matter much
 * and Java is perfectly fine.
 */
public final class ByteFormatter {

    private ByteFormatter() {
        // utility class, no instances
    }

    public static String humanReadable(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = -1;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format("%.1f %s", value, units[unitIndex]);
    }

    /** Estimate remaining download time from bytes/sec, for progress UI. */
    public static String estimateTimeRemaining(long bytesLeft, long bytesPerSecond) {
        if (bytesPerSecond <= 0) {
            return "calculating…";
        }
        long secondsLeft = bytesLeft / bytesPerSecond;
        if (secondsLeft < 60) {
            return secondsLeft + "s";
        }
        long minutes = secondsLeft / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
}
