package io.freemytv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Owned library pages keep every movie/show, rather than recommendation limits or franchise deduplication. */
public final class AioLibraryTest {
    private static Recs.Pick item(int id, String kind) {
        Recs.Pick p = new Recs.Pick();
        p.aio = p.recent = true;
        p.itemId = "item-" + id;
        p.kind = kind;
        p.title = "Franchise: Part " + id;
        return p;
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        List<Recs.Pick> movies = new ArrayList<>();
        for (int i = 0; i < 45; i++) movies.add(item(i, "movie"));
        List<Recs.Pick> first = Recs.libraryItems(Collections.emptyList(), movies.subList(0, 20), "movie");
        List<Recs.Pick> second = Recs.libraryItems(first, movies.subList(19, 40), "movie");
        List<Recs.Pick> all = Recs.libraryItems(second, movies.subList(40, 45), "movie");
        check(first.size() == 20 && second.size() == 40 && all.equals(movies), "Pages append past 40, deduplicating IDs without hiding sequels");
        Recs.Pick series = item(90, "tv"), resume = item(91, "movie"), trending = item(92, "movie");
        resume.resume = true;
        trending.recent = false;
        check(Recs.libraryItems(all, Arrays.asList(series, resume, trending), "movie").equals(all), "Films exclude series, resume and recommendations");
        check(Recs.libraryItems(Collections.emptyList(), Arrays.asList(series, resume, trending), "tv").equals(Collections.singletonList(series)), "Series keeps only owned shows");
        check(first.size() == 20 && movies.size() == 45, "Appending does not mutate earlier pages or source data");
        System.out.println("AIO library: page append, >40 titles, stable IDs, sequels and movie/show/source filtering passed.");
    }
}
