package io.freemytv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Host-side checks for profile filtering and the upstream Continue Watching selection rules. */
public final class WatchNextTest {
    private static WatchNext.Item item(String pkg, String title, long time) {
        WatchNext.Item it = new WatchNext.Item(pkg, null, null);
        it.title = title;
        it.intent = "app://" + title;
        it.engaged = time;
        return it;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        List<WatchNext.Item> source = Arrays.asList(
                item("netflix", "Older", 1), item("netflix", "Newest", 8),
                item("netflix", "Newest", 7), item("netflix", "Next", 6),
                item("youtube", "Hidden", 9), item("youtube", "Visible", 5),
                item("prime", "Disabled", 10));
        List<WatchNext.Item> adult = WatchNext.select(source, Collections.singleton("prime"),
                Collections.singleton("youtube|Hidden"), pkg -> true);
        check(adult.size() == 3 && adult.get(0).title.equals("Newest")
                && adult.get(1).title.equals("Next") && adult.get(2).title.equals("Visible"),
                "Newest-first ordering, duplicate removal and per-app limits must remain intact");
        List<WatchNext.Item> kids = WatchNext.select(source, Collections.singleton("prime"),
                Collections.singleton("youtube|Hidden"), pkg -> !pkg.equals("netflix"));
        check(kids.size() == 1 && kids.get(0).pkg.equals("youtube")
                && kids.get(0).title.equals("Visible"), "A profile-hidden app must not return through Continue Watching");
        check(source.size() == 7 && source.get(0).title.equals("Older") && adult.size() == 3,
                "Selecting another profile must not mutate the source or the earlier result");
        List<WatchNext.Item> many = new ArrayList<>();
        for (int i = 0; i < WatchNext.MAX + 3; i++) many.add(item("app" + i, "Title" + i, i));
        List<WatchNext.Item> limited = WatchNext.select(many, Collections.emptySet(), Collections.emptySet(), pkg -> true);
        check(limited.size() == WatchNext.MAX && limited.get(0).engaged == WatchNext.MAX + 2,
                "The global card limit must still keep the newest items");
        System.out.println("Continue Watching: profile visibility, hidden items, app choices, ordering, duplicates and limits passed.");
    }
}
