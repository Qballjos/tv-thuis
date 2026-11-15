package io.freemytv;

import io.freemytv.Recs.Pick;
import java.io.IOException;
import java.util.*;

/** Fork-owned recommendation rails and incremental library state. Recs supplies its worker and cache hooks. */
final class AioRecommendations {
    interface Host {
        boolean configured();
        String identity();
        long now();
        void execute(Runnable task);
        boolean fill(String kind, int page, List<Pick> out) throws IOException;
        void changed();
        void failed(IOException failure);
    }
    private final Host host;
    private final Map<String, Library> libraries = new HashMap<>();
    private String libraryIdentity = "";
    private static final class Library {
        List<Pick> items = new ArrayList<>();
        int page;
        boolean loading, end;
        long failedAt;
    }
    AioRecommendations(Host host) { this.host = host; }
    void clear() { synchronized (libraries) { libraries.clear(); } }

    /** Called only under the libraries lock. Credentials changing cannot expose the previous server's pages. */
    private Library library(String kind) {
        String identity = host.identity();
        if (!identity.equals(libraryIdentity)) {
            libraries.clear();
            libraryIdentity = identity;
        }
        Library state = libraries.get(kind);
        if (state == null) {
            state = new Library();
            libraries.put(kind, state);
        }
        return state;
    }

    List<Pick> libraryNow(String kind) {
        List<Pick> items;
        boolean first;
        synchronized (libraries) {
            Library state = library(kind);
            items = state.items;
            first = state.page == 0;
        }
        if (first) moreLibrary(kind);
        return items;
    }

    /** Fetch another page near the focused card, never the entire library's artwork at once. */
    void moreLibrary(String kind) {
        if (!host.configured() || !("movie".equals(kind) || "tv".equals(kind))) return;
        final Library state;
        final String identity;
        final int page;
        synchronized (libraries) {
            state = library(kind);
            long sinceFail = host.now() - state.failedAt;
            if (state.loading || state.end || (state.failedAt != 0 && sinceFail >= 0 && sinceFail < 30_000)) return;
            state.loading = true;
            page = state.page + 1;
            identity = libraryIdentity;
        }
        host.execute(() -> {
            try {
                List<Pick> next = new ArrayList<>();
                boolean more = host.fill(kind, page, next);
                synchronized (libraries) {
                    if (libraries.get(kind) != state || !identity.equals(host.identity())) return;
                    state.items = libraryItems(page == 1 ? new ArrayList<>() : state.items, next, kind);
                    state.page = page;
                    state.end = !more;
                    state.failedAt = 0;
                }
            } catch (IOException e) {
                synchronized (libraries) { state.failedAt = host.now(); }
                host.failed(e);
            } finally {
                synchronized (libraries) { state.loading = false; }
                host.changed();
            }
        });
    }

    /** Owned movies/shows keep their order and individual IDs, including sequels in the same franchise. */
    static List<Pick> libraryItems(List<Pick> previous, List<Pick> next, String kind) {
        Map<String, Pick> unique = new LinkedHashMap<>();
        for (List<Pick> page : Arrays.asList(previous, next)) {
            for (Pick p : page) if (p.aio && p.recent && !p.resume && kind.equals(p.kind)) unique.putIfAbsent(Recs.key(p), p);
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * Media manager tabs: "resume" keeps Jellyfin's order (what you were watching), "recent" is newly added,
     * "movie" and "tv" are this week's trending. null is recently added.
     */
    static List<Pick> now(List<Pick> picks, String kind, int cards, long hour) {
        boolean resume = "resume".equals(kind);
        List<Pick> list = new ArrayList<>();
        for (Pick p : picks) {
            if (!p.aio) continue;
            if (resume) { if (p.resume) list.add(p); }
            else if ("recent".equals(kind) || kind == null) { if (p.recent && !p.resume) list.add(p); }
            else if (!p.recent && !p.resume && ("trending".equals(kind) || kind.equals(p.kind))) list.add(p);
        }
        if (!resume && (kind == null || "recent".equals(kind))) {
            list = MediaOrder.newest(list, p -> p.addedAt);
            return new ArrayList<>(list.subList(0, Math.min(cards, list.size())));
        }
        if (resume) return list.size() > cards ? new ArrayList<>(list.subList(0, cards)) : list;
        List<Pick> out = new ArrayList<>();
        if (list.isEmpty()) return out;
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < list.size() && out.size() < cards; i++) {
            Pick p = list.get((int) ((hour * cards + i) % list.size()));
            if (seen.add(franchise(p.title))) out.add(p);
        }
        return out;
    }

    /** "Law & Order: Special Victims Unit" → "law & order" (what comes before ":" or " - "). */
    private static String franchise(String title) {
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        int cut = t.indexOf(':');
        if (cut < 0) cut = t.indexOf(" - ");
        return (cut > 0 ? t.substring(0, cut) : t).trim();
    }

    static List<Pick> showcase(List<Pick> picks, java.util.function.Predicate<Pick> hasImage) {
        List<Pick> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Pick p : picks) {
            if (p.aio && !p.resume && hasImage.test(p) && seen.add(Recs.key(p))) out.add(p);
        }
        return out;
    }

}
