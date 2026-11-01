package io.freemytv;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/** Addition dates from Jellyfin (ISO) and Plex (Unix seconds), independent of the TV's time zone. */
final class MediaOrder {
    private MediaOrder() {}

    static long time(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return 0;
        try {
            if (text.matches("[0-9]{9,16}")) {
                long n = Long.parseLong(text);
                return n < 100_000_000_000L ? n * 1000 : n;
            }
            return Instant.parse(text).toEpochMilli();
        } catch (RuntimeException ignored) {}
        try { return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC).toEpochMilli(); }
        catch (RuntimeException ignored) {}
        try { return LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(); }
        catch (RuntimeException ignored) { return 0; }
    }

    /** Stable for equal/missing dates, and never mutates the server snapshot. */
    static <T> List<T> newest(List<T> items, ToLongFunction<T> date) {
        List<T> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> Long.compare(date.applyAsLong(b), date.applyAsLong(a)));
        return sorted;
    }
}
