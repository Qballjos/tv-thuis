package io.freemytv;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.KeyEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Set;

/**
 * Settings changed on the TV itself (Free My TV > Settings). config.properties only gives the first
 * values; anything the user changes here wins. Read from the :home process (home screen, screensaver, settings).
 */
final class Prefs {
    private Prefs() {}

    /** Remote keys we handle, with the app each opens until the user picks another. Codes read from the TCL's log. */
    static final int KEY_NETFLIX = 4062, KEY_YOUTUBE = 190, KEY_PRIME = 4099, KEY_TCL_CHANNEL = 4020;
    /** TCL's Settings button: NOTIFICATION, which only Google TV's home app (off in our mode) answers. */
    static final int KEY_TCL_SETTINGS = KeyEvent.KEYCODE_NOTIFICATION;
    /** A TCL Settings button's value that opens our quick settings panel over what's playing. */
    static final String QUICK_PANEL = HomeMode.PKG + "#panel";
    /** The Netflix button on Vestel TVs and Formovie projectors: STEM_1, which the TV hands to its Netflix key receiver (dumpsys window policy). */
    static final int KEY_STEM_NETFLIX = KeyEvent.KEYCODE_STEM_1;
    /** KTC's (NextStar) Tabii button: BUTTON_8, which the TV hands to its hot-key receiver (its log: "keycode = 195"). */
    static final int KEY_KTC_TABII = KeyEvent.KEYCODE_BUTTON_8;
    /** Xiaomi's Mi button: BUTTON_4, which the TV hands to its GlobalKeyReceiver for PatchWall (its log: "keyCode = 191"). */
    static final int KEY_MI = KeyEvent.KEYCODE_BUTTON_4;
    /** Xiaomi's PatchWall button: BUTTON_5, which the TV hands to the same receiver to open PatchWall (its log: "192"). */
    static final int KEY_XIAOMI_PATCHWALL = KeyEvent.KEYCODE_BUTTON_5;
    /** Sony's app buttons, as SONY_KEY + scan code (read from a BRAVIA's log; the key codes are shared). */
    static final int SONY_KEY = 100_000;
    static final int KEY_SONY_CORE = SONY_KEY + 496, KEY_SONY_NETFLIX = SONY_KEY + 583, KEY_SONY_DISNEY = SONY_KEY + 491,
            KEY_SONY_PRIME = SONY_KEY + 490, KEY_SONY_YOUTUBE = SONY_KEY + 485, KEY_SONY_CRUNCHYROLL = SONY_KEY + 507,
            KEY_SONY_PLAY = SONY_KEY + 587;   // the Google Play button of older remotes: BUTTON_8 (BRAVIA 4K UR3, Android 10)
    /** The SonyLIV button of the BRAVIA VU31's remote (India, Google TV 14), a key code of its own (#395). */
    static final int KEY_SONY_LIV = 3004;

    /**
     * A Sony app button by scan code. The BRAVIA VU31's remote (Google TV 14, #395) sends other codes for the same
     * buttons: BUTTON_4 with 758 for Netflix, BUTTON_2 with 652 for Prime Video, BUTTON_15 with 653 for YouTube.
     */
    static int sonyKey(int scanCode) {
        switch (scanCode) {
            case 758: return KEY_SONY_NETFLIX;
            case 652: return KEY_SONY_PRIME;
            case 653: return KEY_SONY_YOUTUBE;
            default: return SONY_KEY + scanCode;
        }
    }
    /**
     * Xiaomi's app buttons (remotes sold in Russia), handed to its hot-key receiver (its log: "dealWithHotkey
     * keycode:199"). None has a default here: "Xiaomi default" (empty) leaves the key to the TV.
     */
    static final int KEY_XIAOMI_OKKO = KeyEvent.KEYCODE_BUTTON_13, KEY_XIAOMI_VK = KeyEvent.KEYCODE_BUTTON_1,
            KEY_XIAOMI_RUTUBE = KeyEvent.KEYCODE_BUTTON_16, KEY_XIAOMI_KION = KeyEvent.KEYCODE_BUTTON_11,
            KEY_XIAOMI_WINK = KeyEvent.KEYCODE_BUTTON_10, KEY_XIAOMI_IVI = KeyEvent.KEYCODE_BUTTON_12;
    /**
     * Hisense's buttons (Q6N, MediaTek, Android TV 12 tested; read from the key filter): global keys for Google TV's
     * home app (launcherx), or Hisense's 607 for Disney+. The gear sends NOTIFICATION; ALL_APPS (284) and PROFILE_SWITCH
     * (288) are API 28/29 names, so numbers here.
     */
    static final int KEY_HISENSE_PRIME = 295, KEY_HISENSE_YOUTUBE = KeyEvent.KEYCODE_BUTTON_3, KEY_HISENSE_DISNEY = 607,
            KEY_HISENSE_APPS = 284, KEY_HISENSE_SETTINGS = KeyEvent.KEYCODE_NOTIFICATION, KEY_HISENSE_PROFILE = 288;
    /** Google TV Streamer's Vendor_18d1_Product_9451.kl; MACRO_1 (313) was added in API 34. */
    static final int KEY_GOOGLE_YOUTUBE = KeyEvent.KEYCODE_BUTTON_3, KEY_GOOGLE_NETFLIX = KeyEvent.KEYCODE_BUTTON_4,
            KEY_GOOGLE_CUSTOM = 313;
    /** A button's value that opens this home screen's All Apps. */
    static final String ALL_APPS = HomeMode.PKG + "#all";
    /**
     * The JVC G24's (Skyworth) Inputs button, one key here whichever way it comes (BUTTON_16 with scan code 383 or 79,
     * or BUTTON_14 with 240: SkyworthRemote.shortcut), and its gear, NOTIFICATION; TV_SHORTCUTS and QUICK_SETTINGS
     * are the values that open the TV's input list and our Quick Settings panel.
     */
    static final int KEY_JVC_SHORTCUT = 200_383, KEY_JVC_SETTINGS = KeyEvent.KEYCODE_NOTIFICATION;
    static final String TV_SHORTCUTS = HomeMode.PKG + "#tvshortcuts";
    static final String QUICK_SETTINGS = HomeMode.PKG + "#quicksettings";
    static final int[] KEYS = RemoteKeys.GOOGLE_STREAMER
            ? new int[] {KEY_GOOGLE_YOUTUBE, KEY_GOOGLE_NETFLIX, KEY_GOOGLE_CUSTOM}
            : RemoteKeys.JVC_SKYWORTH ? new int[] {KEY_JVC_SHORTCUT, KEY_JVC_SETTINGS}
            : RemoteKeys.HISENSE ? new int[] {KEY_HISENSE_PRIME, KEY_HISENSE_YOUTUBE, KEY_HISENSE_DISNEY,
                    KEY_HISENSE_APPS, KEY_HISENSE_SETTINGS, KEY_HISENSE_PROFILE}
            : RemoteKeys.SONY
            ? new int[] {KEY_SONY_CORE, KEY_SONY_NETFLIX, KEY_SONY_DISNEY, KEY_SONY_PRIME, KEY_SONY_YOUTUBE, KEY_SONY_CRUNCHYROLL,
                    KEY_SONY_PLAY, KEY_SONY_LIV}
            : RemoteKeys.STEM_NETFLIX ? new int[] {KEY_STEM_NETFLIX}
            : RemoteKeys.KTC ? new int[] {KEY_KTC_TABII}
            : RemoteKeys.XIAOMI_PATCHWALL_ONLY ? new int[] {KEY_XIAOMI_PATCHWALL}
            : RemoteKeys.XIAOMI ? new int[] {KEY_MI, KEY_YOUTUBE, KEY_XIAOMI_PATCHWALL, KEY_XIAOMI_OKKO, KEY_XIAOMI_VK,
                    KEY_XIAOMI_RUTUBE, KEY_XIAOMI_KION, KEY_XIAOMI_WINK, KEY_XIAOMI_IVI}
            : new int[] {KEY_NETFLIX, KEY_YOUTUBE, KEY_PRIME, KEY_TCL_CHANNEL, KEY_TCL_SETTINGS};

    /** Bumped by every change: screens compare it to know when to redraw. */
    static volatile int version;

    private static Context app;
    private static Profiles profiles;

    static void init(Context c) {
        app = c.getApplicationContext();
        profiles = loadProfiles();
        migrateKeys();   // before anything reads the button choices
        keepBuiltIn();   // the next release has no config.properties, so these stay on the TV
        Ui.applyLanguage();   // the language picked on the TV, now that it can be read
        profiles = loadProfiles();
    }

    /** The TV's own 12/24-hour setting (Settings → System → Date & time). */
    static boolean clock24() {
        return app != null && android.text.format.DateFormat.is24HourFormat(app);
    }

    /** "auto" (the TV's own 12/24-hour setting), "24" or "12". */
    static String clockFormat() {
        String v = sp().getString("clock", "auto");
        return v.equals("24") || v.equals("12") ? v : "auto";
    }

    static void setClockFormat(String v) { put("clock", v); }

    /** The app's context in this process (null before App.onCreate). */
    static Context context() {
        return app;
    }

    private static SharedPreferences sp() {
        return profiles.settings();
    }

    private static SharedPreferences device() { return app.getSharedPreferences("settings", Context.MODE_PRIVATE); }

    @SuppressWarnings("deprecation")
    private static Profiles loadProfiles() {
        return new Profiles(app.getSharedPreferences("profiles", Context.MODE_MULTI_PROCESS),
                name -> app.getSharedPreferences(name, Context.MODE_PRIVATE), Ui.tr("My profile"));
    }

    static Profiles profiles() { return profiles; }

    static void profileChanged() {
        version++;
        Ui.applyLanguage();
    }

    private static volatile Set<String> hiddenApps = Collections.emptySet();
    private static volatile int hiddenAt = -1;

    static Set<String> hiddenApps() {
        if (hiddenAt != version) {
            hiddenApps = Collections.unmodifiableSet(new HashSet<>(sp().getStringSet("hiddenApps", Collections.emptySet())));
            hiddenAt = version;
        }
        return hiddenApps;
    }

    static boolean appVisible(String pkg) { return pkg == null || !hiddenApps().contains(pkg); }

    static void setHiddenApps(Set<String> hidden) {
        sp().edit().putStringSet("hiddenApps", new HashSet<>(hidden)).apply();
        version++;
    }

    /**
     * Reads the file again if another process changed it. On Android 11 the screensaver runs in the main process
     * (ScreensaverAlone), which otherwise keeps the values it first read, whatever the settings screen in :home saves.
     */
    @SuppressWarnings("deprecation")
    static void reload() {
        String before = language();
        profiles = loadProfiles();
        app.getSharedPreferences("settings", Context.MODE_MULTI_PROCESS);
        app.getSharedPreferences(profiles.namespace("settings"), Context.MODE_MULTI_PROCESS);
        version++;
        if (!language().equals(before)) Ui.applyLanguage();
    }

    private static void put(String key, String value) {
        sp().edit().putString(key, value).apply();
        version++;
    }

    private static void put(String key, int value) {
        sp().edit().putInt(key, value).apply();
        version++;
    }

    private static void put(String key, boolean value) {
        sp().edit().putBoolean(key, value).apply();
        version++;
    }

    /**
     * Without bumping `version`: a new version makes the home screen start over (and fetch the weather and prices
     * again). Continue Watching's lists don't need that: the home screen reads them on every return (reload).
     */
    private static void putQuietly(String key, Set<String> value) {
        sp().edit().putStringSet(key, new HashSet<>(value)).apply();
    }

    // MARK: - Language and units

    /** "auto" or a language code as picked ("pt", "fr", "zh-CN"…); Ui.applyLanguage finds its catalogue. */
    static String language() {
        String v = app == null ? Config.LANGUAGE : sp().getString("language", Config.LANGUAGE);
        v = v.trim();
        return v.isEmpty() || v.equalsIgnoreCase("auto") ? "auto" : v;
    }

    static void setLanguage(String v) { put("language", v); Ui.applyLanguage(); }

    /** "auto", "C" or "F". */
    static String temperature() {
        String v = sp().getString("temperature", Config.TEMPERATURE).trim().toUpperCase(Locale.ROOT);
        return v.startsWith("F") ? "F" : v.startsWith("C") ? "C" : "auto";
    }

    static void setTemperature(String v) { put("temperature", v); }

    /** Picks per tab: at least Recs.MIN_CARDS, up to Recs.MAX_CARDS. A saved 3 from before is raised. */
    static int cards() {
        return Math.max(Recs.MIN_CARDS, Math.min(Recs.MAX_CARDS, sp().getInt("cards", Recs.MIN_CARDS)));
    }

    static void setCards(int n) { put("cards", n); }

    /** Cartoons and anime out of the picks (Home Settings > Picks): hide_animation in config.properties until changed here. */
    static boolean hideAnimation() {
        return sp().getBoolean("hideAnimation", Config.HIDE_ANIMATION);
    }

    static void setHideAnimation(boolean hide) { put("hideAnimation", hide); }

    // MARK: - Picks from

    private static String offList;
    private static int offListAt = -1;

    /** The services (Recs' keys: "netflix", "prime"...) whose titles are left out of the picks, as ",a,b,". */
    private static String offServices() {
        if (offList == null || offListAt != version) {
            String saved = sp().getString("servicesOff", null);
            if (saved == null) {   // not chosen on the TV yet: what hide_services says
                StringBuilder first = new StringBuilder();
                for (String key : Config.HIDE_SERVICES.split(",")) {
                    key = key.trim().toLowerCase(Locale.ROOT);
                    if (!key.isEmpty()) first.append(first.length() > 0 ? "," : "").append(key);
                }
                saved = first.toString();
            }
            offList = saved.isEmpty() ? "," : "," + saved + ",";
            offListAt = version;
        }
        return offList;
    }

    /** A service found by its app's name is keyed by its package, and is off until switched on. */
    private static boolean byName(String key) {
        return key.indexOf('.') >= 0;
    }

    private static String onServices() {
        String saved = sp().getString("servicesOn", "");
        return saved.isEmpty() ? "," : "," + saved + ",";
    }

    static boolean serviceOff(String key) {
        if (key == null) return false;
        return byName(key) ? !onServices().contains("," + key + ",") : offServices().contains("," + key + ",");
    }

    static void setServiceOff(String key, boolean off) {
        if (byName(key)) {
            String on = onServices().replace("," + key + ",", ",");
            if (!off) on += key + ",";
            put("servicesOn", on.length() > 1 ? on.substring(1, on.length() - 1) : "");
            return;
        }
        String list = offServices();
        list = off ? (serviceOff(key) ? list : list + key + ",") : list.replace("," + key + ",", ",");
        put("servicesOff", list.length() > 1 ? list.substring(1, list.length() - 1) : "");
    }

    /** "auto" (dark from sunset to sunrise), "light" or "dark". */
    static String theme() {
        String v = sp().getString("theme", Config.THEME.trim());
        return v.equals("light") || v.equals("dark") ? v : "auto";
    }

    static void setTheme(String v) { put("theme", v); }

    /** Wi-Fi/Ethernet, Bluetooth and USB icons at the top-left of the home screen. */
    static boolean statusIcons() { return sp().getBoolean("statusicons", false); }
    static void setStatusIcons(boolean on) { put("statusicons", on); }

    // MARK: - City

    static String city() { return sp().getString("city", Place.city()).trim(); }
    static String latitude() { return sp().getString("latitude", Place.latitude()); }
    static String longitude() { return sp().getString("longitude", Place.longitude()); }

    static void setCity(String name, String latitude, String longitude) {
        sp().edit().putString("city", name).putString("latitude", latitude).putString("longitude", longitude).apply();
        version++;
    }

    /** The home screen's soft click when moving and opening. */
    static boolean sounds() { return sp().getBoolean("sounds", true); }
    static void setSounds(boolean on) { put("sounds", on); }

    /** OK on the screensaver opens the title it shows, in its app; any other key only closes it. Off: any key closes it. */
    static boolean saverWatch() { return sp().getBoolean("saverWatch", false); }
    static void setSaverWatch(boolean on) { put("saverWatch", on); }

    static String saverSource() {
        String source = sp().getString("saverSource", "auto");
        return "aio".equals(source) || "immich".equals(source) ? source : "auto";
    }
    static void setSaverSource(String source) { put("saverSource", source); }

    static boolean saverCollage() { return sp().getBoolean("saverCollage", false); }
    static void setSaverCollage(boolean on) { put("saverCollage", on); }

    /** The button that requests or opens the title on the screensaver. OK until another is picked. */
    static int saverRequestKey() {
        int k = sp().getInt("saverRequestKey", KeyEvent.KEYCODE_DPAD_CENTER);
        return k == KeyEvent.KEYCODE_BACK || k == 0 ? KeyEvent.KEYCODE_DPAD_CENTER : k;
    }

    static void setSaverRequestKey(int code) {
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE || code == 0) return;
        put("saverRequestKey", code);
    }

    /** OK is the center key and Enter, whichever the remote sends. */
    static boolean saverRequest(int code) {
        int want = saverRequestKey();
        if (okKey(want) && okKey(code)) return true;
        return code == want;
    }

    private static boolean okKey(int code) {
        return code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    /** The name of that button, for the menu and the line on the screensaver. */
    static String saverButtonName() {
        int code = saverRequestKey();
        if (okKey(code)) return Ui.tr("OK");
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return Ui.tr("Left");
            case KeyEvent.KEYCODE_DPAD_RIGHT: return Ui.tr("Right");
            case KeyEvent.KEYCODE_DPAD_UP: return Ui.tr("Up");
            case KeyEvent.KEYCODE_DPAD_DOWN: return Ui.tr("Down");
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: return Ui.tr("Play");
            case KeyEvent.KEYCODE_PROG_RED: return Ui.tr("Red");
            case KeyEvent.KEYCODE_PROG_GREEN: return Ui.tr("Green");
            case KeyEvent.KEYCODE_PROG_YELLOW: return Ui.tr("Yellow");
            case KeyEvent.KEYCODE_PROG_BLUE: return Ui.tr("Blue");
            default: return Ui.trf("Key {n}", "n", code);
        }
    }

    // MARK: - Top of the home screen

    static final String TOP_PICKS = "picks", TOP_CONTINUE = "continue", TOP_NONE = "none", TOP_AIO = "aio";
    /** Tips, Continue Watching, the clock, then the media manager. */
    static final String[] TOP_CONTENT = {TOP_PICKS, TOP_CONTINUE, TOP_NONE, TOP_AIO};

    /** What the cards under the title show: Tips (TMDB), Continue Watching, the clock, or the media manager. */
    static String topContent() {
        String v = sp().getString("topContent", TOP_PICKS);
        return v.equals(TOP_CONTINUE) || v.equals(TOP_NONE) || v.equals(TOP_AIO) ? v : TOP_PICKS;
    }

    static void setTopContent(String v) { put("topContent", v); }

    /** Apps left out of Continue Watching (Settings), by package. */
    static Set<String> watchOff() { return new HashSet<>(sp().getStringSet("watchOff", new HashSet<>())); }
    static void setWatchOff(Set<String> packages) { putQuietly("watchOff", packages); }

    /** Single items hidden from Continue Watching (holding OK on a card), as WatchNext.hideKey. */
    static Set<String> watchHidden() { return new HashSet<>(sp().getStringSet("watchHidden", new HashSet<>())); }
    static void setWatchHidden(Set<String> keys) { putQuietly("watchHidden", keys); }

    /** Recent Apps: "off" (the default: Android manages memory on its own), "drawer" (a shortcut in the bottom drawer and the
     *  remote's app-switch key) or "home" (those, and pressing Home twice). The top button is its own choice (Top Buttons). */
    static String recents() {
        String v = sp().getString("recents", "off");
        return v.equals("drawer") || v.equals("home") ? v : "off";
    }

    static void setRecents(String v) { put("recents", v); }

    /**
     * After the TV starts from off, never from standby (Home Settings > At power-on, #396 and #346): "tv" (the TV's
     * choice), "home" (ours, where a TV app takes the screen at boot) or "live" (Live TV, where the TV starts on ours).
     */
    static final String[] POWER_ON = {"tv", "home", "live"};

    static String powerOn() {
        String v = sp().getString("powerOn", "tv");
        return v.equals("home") || v.equals("live") ? v : "tv";
    }

    static void setPowerOn(String v) { put("powerOn", v); }

    // MARK: - Widgets and coins

    static boolean weather() { return sp().getBoolean("weather", true); }
    static void setWeather(boolean on) { put("weather", on); }
    /** The weather card's forecast: "hours" (the next 6 hours) or "days" (the next 5 days). */
    static String forecast() { return "days".equals(sp().getString("forecast", "hours")) ? "days" : "hours"; }
    static void setForecast(String v) { put("forecast", v); }

    static boolean nowPlaying() { return device().getBoolean("nowPlaying", false); }
    static void setNowPlaying(boolean on) {   // committed: the main process reads it right away to allow the listener
        device().edit().putBoolean("nowPlaying", on).commit();
        version++;
    }

    /** Home Assistant's tiles in the drawer (HomeAssistant): off until turned on here, even with a server configured. */
    static boolean homeAssistant() { return sp().getBoolean("homeAssistant", false); }
    static void setHomeAssistant(boolean on) { put("homeAssistant", on); }

    static boolean quotes() { return sp().getBoolean("quotes", Config.SHOW_CRYPTO); }
    static void setQuotes(boolean on) { put("quotes", on); }

    /** Your photos (Photos, sent from a computer) on the screensaver and behind the home screen, once imported. */
    static boolean photos() { return sp().getBoolean("photos", true); }
    static void setPhotos(boolean on) { put("photos", on); }

    /**
     * With an Immich server, where My Photos come from: "default" (none of yours: the picks' photos, as without
     * Immich), "immich" (all its photos) or "album" (photoAlbum, its id; photoAlbumName to show).
     */
    static String photoSource() { return sp().getString("photoSource", "immich"); }
    static String photoAlbum() { return sp().getString("photoAlbum", ""); }
    static String photoAlbumName() { return sp().getString("photoAlbumName", ""); }

    static void setPhotoSource(String source, String album, String name) {
        sp().edit().putString("photoSource", source).putString("photoAlbum", album).putString("photoAlbumName", name).apply();
        version++;
    }

    static boolean coin(int i) { return sp().getBoolean("coin" + i, true); }
    static void setCoin(int i, boolean on) { put("coin" + i, on); }

    /**
     * Also list the TV's own system apps (Settings, Files, the manufacturer's apps) in All Apps and the shelf editor.
     * Off keeps the list as it always was: the apps people installed, plus the system ones a TV needs there.
     */
    static boolean androidApps() { return sp().getBoolean("androidapps", false); }
    static void setAndroidApps(boolean on) { put("androidapps", on); }

    // MARK: - TMDB key

    /**
     * The TMDB key given on the TV (Home Settings > TMDB key: from a phone, or typed), or "": then config.properties'
     * one (Recs.key). Never logged.
     */
    static String tmdbKey() { return app == null ? "" : device().getString("tmdbKey", "").trim(); }

    static void setTmdbKey(String key) {   // committed: ScreensaverAlone reads it from the main process
        device().edit().putString("tmdbKey", key).commit();
        version++;
    }

    // MARK: - AIO media manager (Home Settings). The TV's values win over config.properties.

    static String aioUrl() {
        String saved = app != null && device().contains("aioUrl") ? device().getString("aioUrl", "") : Config.AIO_URL;
        if (saved == null) return "";
        saved = saved.trim();
        while (saved.endsWith("/")) saved = saved.substring(0, saved.length() - 1);
        return saved;
    }

    static String aioUser() {
        String saved = app != null && device().contains("aioUser") ? device().getString("aioUser", "") : Config.AIO_USER;
        return saved == null ? "" : saved.trim();
    }

    static String aioPassword() {
        String saved = app != null && device().contains("aioPassword") ? device().getString("aioPassword", "") : Config.AIO_PASSWORD;
        return saved == null ? "" : saved;
    }

    static boolean aioPasswordSet() { return !aioPassword().isEmpty(); }

    static String aioToken() {
        return app == null ? "" : device().getString("aioToken", "");
    }

    static boolean aioPaired() { return app != null && !device().getString("aioDeviceId", "").isEmpty(); }

    static boolean setAioPairing(AioPairing.Connection connection) {
        String[] keys = {"aioUrl", "aioToken", "aioDeviceId", "aioUser", "aioPassword", "jellyfinUrl", "jellyfinKey"};
        java.util.Map<String, String> before = new java.util.HashMap<>();
        for (String key : keys) if (device().contains(key)) before.put(key, device().getString(key, ""));
        boolean saved = device().edit().putString("aioUrl", connection.server).putString("aioToken", connection.token)
                .putString("aioDeviceId", connection.id).putString("aioUser", "").putString("aioPassword", "")
                .putString("jellyfinUrl", "").putString("jellyfinKey", "").commit();
        if (saved) version++;
        else {
            // SharedPreferences changes memory even when its disk commit fails: restore the previous connection.
            SharedPreferences.Editor undo = device().edit();
            for (String key : keys) {
                if (before.containsKey(key)) undo.putString(key, before.get(key));
                else undo.remove(key);
            }
            undo.commit();
        }
        return saved;
    }

    static void clearAioPairing() {
        device().edit().putString("aioUrl", "").putString("aioToken", "").putString("aioDeviceId", "")
                .putString("aioUser", "").putString("aioPassword", "").commit();
        version++;
    }

    static void setAioUrl(String url) {
        keepAio().putString("aioUrl", url == null ? "" : url.trim()).putString("aioToken", "").putString("aioDeviceId", "").commit();
        version++;
    }

    static void setAioUser(String user) {
        keepAio().putString("aioUser", user == null ? "" : user.trim()).putString("aioToken", "").commit();
        version++;
    }

    static void setAioPassword(String password) {
        keepAio().putString("aioPassword", password == null ? "" : password).putString("aioToken", "").commit();
        version++;
    }

    static void setAioToken(String token) {
        if (app == null) return;
        device().edit().putString("aioToken", token == null ? "" : token).commit();
        version++;
    }

    /** Jellyfin, for continue watching. The API key is the one from the server's dashboard. */
    static String jellyfinUrl() {
        String saved = app != null && device().contains("jellyfinUrl") ? device().getString("jellyfinUrl", "") : Config.JELLYFIN_URL;
        if (saved == null) return "";
        saved = saved.trim();
        while (saved.endsWith("/")) saved = saved.substring(0, saved.length() - 1);
        return saved;
    }

    static String jellyfinKey() {
        String saved = app != null && device().contains("jellyfinKey") ? device().getString("jellyfinKey", "") : Config.JELLYFIN_KEY;
        return saved == null ? "" : saved.trim();
    }

    static boolean jellyfinKeySet() { return !jellyfinKey().isEmpty(); }

    static void setJellyfinUrl(String url) {
        if (app == null) return;
        String value = url == null ? "" : url.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        device().edit().putString("jellyfinUrl", value).commit();
        version++;
    }

    static void setJellyfinKey(String key) {
        if (app == null) return;
        device().edit().putString("jellyfinKey", key == null ? "" : key.trim()).commit();
        version++;
    }

    /**
     * Writes the build's household settings once, into the TV's private storage. A later update from GitHub is built
     * without config.properties, and these values would otherwise go with it.
     */
    private static void keepBuiltIn() {
        SharedPreferences s = device();
        SharedPreferences.Editor e = s.edit();
        boolean changed = false;
        if (!s.contains("tmdbKey") && !Config.TMDB_KEY.trim().isEmpty()) {
            e.putString("tmdbKey", Config.TMDB_KEY.trim());
            changed = true;
        }
        if (!s.contains("aioUrl") && !Config.AIO_URL.trim().isEmpty()) {
            e.putString("aioUrl", Config.AIO_URL.trim());
            e.putString("aioUser", Config.AIO_USER == null ? "" : Config.AIO_USER.trim());
            e.putString("aioPassword", Config.AIO_PASSWORD == null ? "" : Config.AIO_PASSWORD);
            changed = true;
        }
        if (!s.contains("jellyfinUrl") && !Config.JELLYFIN_URL.trim().isEmpty()) {
            e.putString("jellyfinUrl", Config.JELLYFIN_URL.trim());
            e.putString("jellyfinKey", Config.JELLYFIN_KEY == null ? "" : Config.JELLYFIN_KEY.trim());
            changed = true;
        }
        if (changed) e.commit();
        SharedPreferences profile = sp();
        if (!profile.contains("city") && !Config.CITY.trim().isEmpty()) {
            profile.edit().putString("city", Config.CITY.trim())
                    .putString("latitude", Config.LATITUDE.trim())
                    .putString("longitude", Config.LONGITUDE.trim()).commit();
        }
    }

    /** Copies the build's values the first time the TV saves one field, so the others are not lost. */
    private static android.content.SharedPreferences.Editor keepAio() {
        android.content.SharedPreferences.Editor e = device().edit();
        if (!device().contains("aioUrl")) e.putString("aioUrl", Config.AIO_URL);
        if (!device().contains("aioUser")) e.putString("aioUser", Config.AIO_USER);
        if (!device().contains("aioPassword")) e.putString("aioPassword", Config.AIO_PASSWORD);
        return e;
    }

    /** The Watchmode key given on the TV, or "": then config.properties' one (Recs.watchmodeKey). Never logged. */
    static String watchmodeKey() { return app == null ? "" : sp().getString("watchmodeKey", "").trim(); }

    static void setWatchmodeKey(String key) {
        sp().edit().putString("watchmodeKey", key).commit();
        version++;
    }

    // MARK: - Shelf

    /** Packages on the home shelf, in order; null until the user edits it (then the built-in list applies). */
    static List<String> shelf() {
        String v = sp().getString("shelf", null);
        if (v == null) return null;
        List<String> out = new ArrayList<>();
        for (String p : v.split(",")) if (!p.isEmpty()) out.add(p);
        return out;
    }

    static void setShelf(List<String> packages) {
        StringBuilder sb = new StringBuilder();
        for (String p : packages) sb.append(sb.length() == 0 ? "" : ",").append(p);
        put("shelf", sb.toString());
    }

    // MARK: - Sleep timer

    /** When the sleep timer ends (System.currentTimeMillis); SleepTimer's alarm says whether it's still on. */
    static long sleepAt() { return device().getLong("sleepAt", 0); }

    /** When it was set, in uptimeMillis (dumpsys power's clock): a TV turned off since ended it (SleepTimer.check). */
    static long sleepFrom() { return device().getLong("sleepFrom", 0); }

    // No version++ for the sleep settings: HomeActivity would rebuild the home screen and fetch the weather again for a
    // timer; HomeView reads what it needs on resume and every 15 s (updateSleep).
    static void setSleepAt(long at, long from) {
        device().edit().putLong("sleepAt", at).putLong("sleepFrom", from).apply();
    }

    /** The sleep timer's minutes left on screen: over any app (SleepOverlay), or where this TV can't, on the home screen. */
    static boolean sleepShown() { return device().getBoolean("sleepShown", true); }
    static void setSleepShown(boolean on) { device().edit().putBoolean("sleepShown", on).apply(); }

    /** Where it shows over other apps: SleepOverlay's places, the top center first. */
    static int sleepPlace() { return device().getInt("sleepPlace", 0); }

    static void setSleepPlace(int place) { device().edit().putInt("sleepPlace", place).apply(); }

    // MARK: - Top buttons

    /**
     * The round buttons at the top right, in order (HomeApps.TOP_ALL). null until the user edits
     * them (then HomeApps.TOP_DEFAULT applies); empty when they took them all off.
     */
    static List<String> topButtons() {
        String v = sp().getString("topButtons", null);
        if (v == null) return null;
        List<String> out = new ArrayList<>();
        for (String p : v.split(",")) if (!p.isEmpty()) out.add(p);
        return out;
    }

    static void setTopButtons(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String p : keys) sb.append(sb.length() == 0 ? "" : ",").append(p);
        put("topButtons", sb.toString());
    }

    // MARK: - Remote buttons

    private static String defaultApp(int keyCode) {
        if (RemoteKeys.GOOGLE_STREAMER) {
            return keyCode == KEY_GOOGLE_YOUTUBE ? "com.google.android.youtube.tv" : "";
        }
        if (RemoteKeys.JVC_SKYWORTH) {
            if (keyCode == KEY_JVC_SHORTCUT) return TV_SHORTCUTS;
            return keyCode == KEY_JVC_SETTINGS ? QUICK_SETTINGS : "";
        }
        if (RemoteKeys.SONY) {
            if (!sonyGoogleTv()) return "";   // "Sony default": Sony's own receiver opens it
            switch (keyCode) {
                case KEY_SONY_NETFLIX: return "com.netflix.ninja";
                case KEY_SONY_DISNEY: return "com.disney.disneyplus";
                case KEY_SONY_PRIME: return "com.amazon.amazonvideo.livingroom";
                case KEY_SONY_YOUTUBE: return "com.google.android.youtube.tv";
                case KEY_SONY_CRUNCHYROLL: return "com.crunchyroll.crunchyroid";
                case KEY_SONY_LIV: return "com.sonyliv";
                default: return "";
            }
        }
        if (RemoteKeys.HISENSE) {
            switch (keyCode) {
                case KEY_HISENSE_PRIME: return "com.amazon.amazonvideo.livingroom";
                case KEY_HISENSE_YOUTUBE: return "org.smarttube.stable";
                case KEY_HISENSE_DISNEY: return "com.disney.disneyplus";
                case KEY_HISENSE_APPS: return ALL_APPS;
                case KEY_HISENSE_SETTINGS: return "com.android.tv.settings";
                default: return "";
            }
        }
        switch (keyCode) {
            case KEY_NETFLIX: case KEY_STEM_NETFLIX: return "com.netflix.ninja";
            case KEY_YOUTUBE: return "org.smarttube.stable";
            case KEY_PRIME: return "com.amazon.amazonvideo.livingroom";
            case KEY_TCL_CHANNEL: return "";
            case KEY_TCL_SETTINGS: return "com.android.tv.settings";   // TV Settings, as from our home screen (#289)
            case KEY_MI: return HomeMode.PKG;   // our home screen: the remote has no other Home button
            default: return "";
        }
    }

    // The key service runs in its own small process (:keys), so that a busy home screen can never delay the TV's keys.
    // SharedPreferences are not shared between processes, so these few values live in a tiny file both processes read.
    // The service asks on every key: the values stay in memory and the file is read again only when it changes.
    private static File keysFile() {
        return new File(app.getFilesDir(), "remote-keys.txt");
    }

    private static Boolean sonyGoogleTv;

    /**
     * A Sony with Google TV's home app (launcherx), which takes the app buttons from Sony's receiver and which Free My
     * TV mode turns off: there "Sony default" does nothing (BRAVIA VU31, #395), so the buttons open their apps unless
     * the person picks otherwise. Sony's Android TV sets (VH22) keep Sony's default.
     */
    private static boolean sonyGoogleTv() {
        if (sonyGoogleTv == null) {
            boolean has;
            try {
                app.getPackageManager().getApplicationInfo("com.google.android.apps.tv.launcherx",
                        PackageManager.MATCH_DISABLED_COMPONENTS);
                has = true;
            } catch (PackageManager.NameNotFoundException e) {
                has = false;
            }
            sonyGoogleTv = has;
        }
        return sonyGoogleTv;
    }

    private static final String[] keyApps = new String[KEYS.length];   // null = the default
    private static long keysStamp = Long.MIN_VALUE;
    /** Every button saved as Nothing (the key service off): a button added since stays Nothing too, not its default. */
    private static boolean keysAllOff;

    /** A temporary file per process (two can start together after an update), synced before it replaces the old one. */
    private static boolean writeKeys(String text) {
        File tmp = new File(app.getFilesDir(), "remote-keys." + android.os.Process.myPid() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            return false;
        }
        return tmp.renameTo(keysFile());
    }

    /** Choices saved before they moved to the file (in SharedPreferences) are copied there once. */
    private static void migrateKeys() {
        if (keysFile().exists()) return;
        SharedPreferences old = device();
        boolean any = false;
        for (int key : KEYS) any |= old.contains("key" + key);
        if (!any) return;
        StringBuilder sb = new StringBuilder();
        for (int key : KEYS) sb.append(key).append('=').append(old.getString("key" + key, defaultApp(key))).append('\n');
        writeKeys(sb.toString());
    }

    /** Package the key opens ("" = nothing). */
    static String keyApp(int keyCode) {
        synchronized (keyApps) {
            File f = keysFile();
            long stamp = f.lastModified() * 31 + f.length();   // 0 without the file: every key opens its default
            if (stamp != keysStamp) {
                keysStamp = stamp;
                java.util.Arrays.fill(keyApps, null);
                keysAllOff = false;
                if (stamp != 0) {
                    try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            for (int i = 0; i < KEYS.length; i++) {
                                String prefix = KEYS[i] + "=";
                                if (line.startsWith(prefix)) keyApps[i] = line.substring(prefix.length());
                            }
                        }
                    } catch (IOException ignored) {
                    }
                    boolean saved = false, on = false;
                    for (String v : keyApps) if (v != null) { saved = true; on |= !v.isEmpty(); }
                    keysAllOff = saved && !on;
                }
            }
            for (int i = 0; i < KEYS.length; i++) if (KEYS[i] == keyCode && keyApps[i] != null) return keyApps[i];
            return keysAllOff ? "" : defaultApp(keyCode);
        }
    }

    static void setKeyApp(int keyCode, String pkg) {
        StringBuilder sb = new StringBuilder();
        for (int key : KEYS) sb.append(key).append('=').append(key == keyCode ? pkg : keyApp(key)).append('\n');
        if (writeKeys(sb.toString())) version++;
    }
}
