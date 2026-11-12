package io.freemytv;
/** Profile selection boundary; the real Recent implementation does all file IO and ordering. */
final class Prefs {
    static String profile = "default";
    static boolean enabled = true;
    static boolean shelfRecent() { return enabled; }
    static Profile profiles() { return new Profile(); }
    static final class Profile {
        String namespace(String base) { return profile.equals("default") ? base : base + "-" + profile; }
    }
}
