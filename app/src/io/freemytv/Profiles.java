package io.freemytv;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

/** Local launcher profiles. The first profile keeps the original settings file, without moving any data. */
final class Profiles {
    static final String FIRST = "default";
    static final int MAX = 8;
    static final List<String> AVATARS = Collections.unmodifiableList(Arrays.asList(
            "person", "fox", "cat", "robot", "astronaut", "owl", "panda", "rocket",
            "dog", "lion", "bear", "rabbit", "frog", "koala", "penguin", "tiger",
            "monkey", "unicorn", "dinosaur", "alien", "ninja", "pirate", "star", "gamepad", "rainbow",
            "quiet-slate", "quiet-stone", "quiet-moon", "quiet-mountain", "quiet-leaf", "quiet-rings"));
    private final SharedPreferences registry;
    private final Function<String, SharedPreferences> files;
    private final String firstName;
    private volatile String active;

    Profiles(SharedPreferences registry, Function<String, SharedPreferences> files, String firstName) {
        this.registry = registry;
        this.files = files;
        this.firstName = firstName;
        String saved = registry.getString("active", FIRST);
        List<String> ids = ids();
        active = ids.contains(saved) ? saved : ids.get(0);
    }

    List<String> ids() {
        List<String> ids = new ArrayList<>();
        if (!registry.getBoolean("firstRemoved", false)) ids.add(FIRST);
        for (String id : registry.getString("ids", "").split(",")) {
            if (id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}") && !ids.contains(id)) ids.add(id);
        }
        if (ids.isEmpty()) ids.add(FIRST);   // a damaged registry must still leave a usable home screen
        return ids;
    }

    String active() { return active; }
    String name(String id) { return registry.getString("name." + id, FIRST.equals(id) ? firstName : ""); }
    String name() { return name(active); }
    String namespace(String base) { return namespace(base, active); }
    private String namespace(String base, String id) { return FIRST.equals(id) ? base : base + "-" + id; }
    SharedPreferences settings() { return files.apply(namespace("settings")); }
    SharedPreferences settings(String id) {
        if (!ids().contains(id)) throw new IllegalArgumentException();
        return files.apply(namespace("settings", id));
    }

    String avatar(String id) {
        String fallback = AVATARS.get(Math.max(0, ids().indexOf(id)) % AVATARS.size());
        String saved = registry.getString("avatar." + id, fallback);
        return AVATARS.contains(saved) ? saved : fallback;
    }

    private String validAvatar(String avatar) {
        if (!AVATARS.contains(avatar)) throw new IllegalArgumentException();
        return avatar;
    }

    synchronized boolean select(String id) {
        if (!ids().contains(id) || !registry.edit().putString("active", id).commit()) return false;
        active = id;
        return true;
    }

    private String validName(String text, String except) {
        String name = text.trim();
        if (name.isEmpty() || name.length() > 32 || name.matches("(?s).*\\p{Cntrl}.*")) throw new IllegalArgumentException();
        for (String id : ids()) if (!id.equals(except) && name(id).equalsIgnoreCase(name)) throw new IllegalArgumentException();
        return name;
    }

    synchronized boolean rename(String id, String name) {
        return update(id, name, avatar(id));
    }

    synchronized boolean update(String id, String name, String avatar) {
        if (!ids().contains(id)) return false;
        return registry.edit().putString("name." + id, validName(name, id))
                .putString("avatar." + id, validAvatar(avatar)).commit();
    }

    synchronized String add(String text) {
        return add(text, AVATARS.get(ids().size() % AVATARS.size()));
    }

    synchronized String add(String text, String avatar) {
        String name = validName(text, "");
        validAvatar(avatar);
        List<String> ids = ids();
        if (ids.size() >= MAX) throw new IllegalArgumentException();
        String id = UUID.randomUUID().toString();
        SharedPreferences.Editor copy = files.apply(namespace("settings", id)).edit().clear();
        for (Map.Entry<String, ?> entry : settings().getAll().entrySet()) {
            String key = entry.getKey();
            if (shared(key)) continue;
            Object v = entry.getValue();
            if (v instanceof String) copy.putString(key, (String) v);
            else if (v instanceof Boolean) copy.putBoolean(key, (Boolean) v);
            else if (v instanceof Integer) copy.putInt(key, (Integer) v);
            else if (v instanceof Long) copy.putLong(key, (Long) v);
            else if (v instanceof Float) copy.putFloat(key, (Float) v);
            else if (v instanceof Set) {
                @SuppressWarnings("unchecked") Set<String> values = (Set<String>) v;
                copy.putStringSet(key, new HashSet<>(values));
            }
        }
        if (!copy.commit()) return null;
        ids.remove(FIRST);
        ids.add(id);
        if (!registry.edit().putString("ids", String.join(",", ids)).putString("name." + id, name)
                .putString("avatar." + id, avatar).commit()) {
            files.apply(namespace("settings", id)).edit().clear().commit();
            return null;
        }
        return id;
    }

    /** Device credentials, permissions and timers keep using the original settings file. */
    static boolean shared(String key) {
        return key.equals("tmdbKey") || key.equals("nowPlaying") || key.startsWith("sleep") || key.startsWith("key")
                || key.equals("aioUrl") || key.equals("aioUser") || key.equals("aioPassword") || key.equals("aioToken") || key.equals("aioDeviceId")
                || key.equals("jellyfinUrl") || key.equals("jellyfinKey");
    }

    synchronized boolean remove(String id) {
        List<String> ids = ids();
        if (ids.size() <= 1 || !ids.remove(id)) return false;
        String fallback = ids.get(0);
        SharedPreferences.Editor edit = registry.edit().putString("ids", String.join(",", ids))
                .remove("name." + id).remove("avatar." + id);
        if (FIRST.equals(id)) edit.putBoolean("firstRemoved", true);
        if (active.equals(id)) edit.putString("active", fallback);
        if (!edit.commit()) return false;
        if (active.equals(id)) active = fallback;
        SharedPreferences removed = files.apply(namespace("settings", id));
        SharedPreferences.Editor clear = removed.edit();
        if (FIRST.equals(id)) {
            // The legacy settings file also holds the shared TMDB key and device state.
            for (String key : removed.getAll().keySet()) if (!shared(key)) clear.remove(key);
        } else clear.clear();
        clear.commit();
        files.apply(namespace("home-ui", id)).edit().clear().commit();
        return true;
    }
}
