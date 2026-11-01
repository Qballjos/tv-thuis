package io.freemytv;

import java.util.Arrays;
import java.util.List;

public final class MediaOrderTest {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    public static void main(String[] args) {
        long instant = 1_700_000_000_000L;
        check(MediaOrder.time("1700000000") == instant, "Plex Unix seconds");
        check(MediaOrder.time("1700000000000") == instant, "Unix milliseconds");
        check(MediaOrder.time("2023-11-14T22:13:20Z") == instant, "Jellyfin ISO date");
        check(MediaOrder.time("2023-11-14T23:13:20+01:00") == instant, "Timezone offsets");
        check(MediaOrder.time("2023-11-14T22:13:20") == instant, "Offset-free dates use UTC, not TV timezone");
        check(MediaOrder.time(null) == 0 && MediaOrder.time("not a date") == 0, "Missing or invalid dates");
        List<String> input = Arrays.asList("older", "newest", "unknown", "equal");
        List<String> output = MediaOrder.newest(input, s -> s.equals("newest") ? 30 : s.equals("unknown") ? 0 : 10);
        check(output.equals(Arrays.asList("newest", "older", "equal", "unknown")), "Newest first; ties stable; missing dates last");
        check(input.equals(Arrays.asList("older", "newest", "unknown", "equal")), "Never mutate the shared snapshot");
        System.out.println("Addition dates: Unix/ISO/offsets, stable newest-first order and missing dates passed.");
    }
}
