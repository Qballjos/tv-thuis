package io.freemytv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** AIO screensaver selection: both sources, stable alternation and the library copy of duplicate franchises. */
public final class AioShowcaseTest {
    private static Recs.Pick pick(String title, boolean recent) {
        Recs.Pick p = new Recs.Pick();
        p.aio = true;
        p.title = title;
        p.recent = recent;
        return p;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Recs.Pick alpha = pick("Alpha", false), beta = pick("Beta", false);
        Recs.Pick library = pick("Shared - newly added", true), delta = pick("Delta", true);
        Recs.Pick resume = pick("Continue watching only", false);
        resume.resume = true;
        Recs.Pick tmdb = pick("Other source", false);
        tmdb.aio = false;
        List<Recs.Pick> source = Arrays.asList(alpha, pick("Shared: trending", false), beta,
                pick("Alpha: sequel", false), library, delta, pick("Delta", true), resume, tmdb);
        List<Recs.Pick> before = new ArrayList<>(source);
        check(Recs.aioShowcase(source).equals(Arrays.asList(alpha, library, beta, delta)),
                "Alternate trending/recent, prefer the recent duplicate and exclude resume/TMDB");
        check(source.equals(before), "Selection must not reorder or change the home-screen sources");
        check(Recs.aioShowcase(Arrays.asList(alpha, beta, delta)).equals(Arrays.asList(alpha, delta, beta)),
                "Finish the longer source when the other runs out");
        check(Recs.aioShowcase(Arrays.asList(library, delta)).equals(Arrays.asList(library, delta)),
                "A recent-only library must work without any trending titles");
        check(Recs.aioShowcase(Arrays.asList(alpha, beta)).equals(Arrays.asList(alpha, beta)),
                "Trending-only selection keeps its order");
        check(Recs.aioShowcase(Arrays.asList(resume, tmdb)).isEmpty()
                && Recs.aioShowcase(Collections.emptyList()).isEmpty(), "No unrelated fallback for an empty source");
        System.out.println("AIO slides: recent/trending alternation, library-first duplicates, uneven/empty sources and source isolation passed.");
    }
}
