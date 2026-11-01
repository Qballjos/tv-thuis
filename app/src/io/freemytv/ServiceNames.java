package io.freemytv;

import java.util.regex.Pattern;

/** Shared provider-name matching for TMDB and Watchmode. Names arrive in lower case. */
final class ServiceNames {
    private static final Pattern MAX_WORD = Pattern.compile("\\bmax\\b");

    private ServiceNames() {}

    /** Match Max as a whole word without wildcard backtracking over provider names. */
    static boolean max(String name) { return MAX_WORD.matcher(name).find(); }
}
