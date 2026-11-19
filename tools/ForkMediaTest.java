package io.freemytv;

import java.util.*;
import java.io.IOException;

/** Deterministic tests for fork policy and async library ownership, without a TV. */
public final class ForkMediaTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static Recs.Pick item(int id, String kind) {
        Recs.Pick p = new Recs.Pick();
        p.id = id; p.itemId = "item-" + id; p.kind = kind;
        p.title = "Saga: Part " + id; p.aio = p.recent = true;
        return p;
    }
    static final class Host implements AioRecommendations.Host {
        String identity = "first";
        long time = 1000;
        int requests, notifications;
        boolean fail, more = true;
        List<Runnable> pending = new ArrayList<>();
        public boolean configured() { return true; }
        public String identity() { return identity; }
        public long now() { return time; }
        public void execute(Runnable task) { pending.add(task); }
        public void changed() { notifications++; }
        public void failed(IOException e) {}
        public boolean fill(String kind, int page, List<Recs.Pick> out) throws IOException {
            requests++;
            if (fail) throw new IOException("fixture");
            out.add(item(page, kind));
            return more;
        }
        void finish() { pending.remove(0).run(); }
    }
    public static void main(String[] args) {
        for (String youtube : Arrays.asList("com.google.android.youtube.tv", "com.google.android.youtube", "com.amazon.firetv.youtube")) {
            check(!HomeMode.mayTurnOff(youtube), "YouTube must stay enabled, including when already recorded: " + youtube);
            check(!HomeApps.isHidden(youtube), "YouTube must be available in All Apps: " + youtube);
        }
        Host h = new Host(); AioRecommendations library = new AioRecommendations(h);
        check(library.libraryNow("movie").isEmpty(), "Initial page is lazy");
        library.libraryNow("movie"); library.moreLibrary("movie");
        check(h.pending.size() == 1, "Only one page per kind may be pending");
        h.finish();
        check(library.libraryNow("movie").size() == 1 && h.pending.isEmpty(), "Reading loaded pages does not fetch all artwork");
        library.moreLibrary("movie"); h.more = false; h.finish();
        check(library.libraryNow("movie").size() == 2, "Append preserves page order");
        library.moreLibrary("movie"); check(h.pending.isEmpty(), "Final page stops requests");
        library.libraryNow("tv"); h.identity = "second"; h.finish();
        check(library.libraryNow("tv").isEmpty(), "Old identity response is discarded");
        h.finish(); check(library.libraryNow("tv").size() == 1, "New identity fetches independently");
        library.clear(); h.fail = true; library.libraryNow("movie"); h.finish();
        library.moreLibrary("movie"); check(h.pending.isEmpty(), "Failure backs off");
        h.time += 30001; library.moreLibrary("movie"); check(h.pending.size() == 1, "Failure retries after 30 seconds");
        h.fail = false; library.clear(); h.finish();
        check(library.libraryNow("movie").isEmpty(), "Clearing pages rejects an in-flight result");
        h.finish();
        Recs.Pick a = item(10, "movie"), b = item(11, "movie"), c = item(12, "tv");
        a.addedAt = 100; b.addedAt = 300; c.addedAt = 200;
        List<Recs.Pick> picks = Arrays.asList(a, b, c);
        check(AioRecommendations.now(picks, "recent", 3, 0).equals(Arrays.asList(b,c,a)), "Recent keeps sequels newest first");
        check(AioRecommendations.now(picks, "recent", 3, 99).equals(Arrays.asList(b,c,a)), "Recent does not rotate");
        a.resume = b.resume = true;
        check(AioRecommendations.now(picks, "resume", 1, 99).equals(Collections.singletonList(a)), "Resume preserves provider order and limit");
        a.resume = b.resume = false; a.recent = b.recent = false;
        check(AioRecommendations.now(picks, "trending", 3, 0).size() == 1, "Trending keeps franchise deduplication");
        Recs.Pick duplicate = item(12, "tv"), foreign = item(13, "movie");
        foreign.aio = false;
        b.resume = true;
        check(AioRecommendations.showcase(Arrays.asList(a, b, c, duplicate, foreign), pick -> pick != a)
                .equals(Collections.singletonList(c)), "Showcase excludes missing images, resume, duplicates and TMDB");
        check(SaverSources.source("aio", true, true, true) == SaverSources.Source.AIO, "Explicit AIO wins");
        check(SaverSources.source("immich", true, true, false) == SaverSources.Source.IMMICH, "Explicit Immich never falls back");
        check(SaverSources.source("auto", true, true, false) == SaverSources.Source.PHOTOS, "Explicit empty album never falls back");
        check(SaverSources.source("auto", false, false, true) == SaverSources.Source.PHOTOS, "Own photos win over recommendations");
        check(SaverSources.source("auto", false, false, false) == SaverSources.Source.RECOMMENDATIONS, "Automatic fallback uses showcase");
        System.out.println("Fork media: YouTube availability, lazy paging, identity/reset races, backoff, rail ordering and saver source policy passed.");
    }
}
