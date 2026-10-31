package io.freemytv;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Host-side regression checks: the Android interface is backed by in-memory preferences. */
public final class ProfilesTest {
    private static final class Memory {
        final Map<String, Object> data = new HashMap<>();
        boolean writable = true;
        SharedPreferences prefs() {
            return (SharedPreferences) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {SharedPreferences.class}, (p, m, a) -> {
                String name = m.getName();
                if (name.equals("getAll")) return new HashMap<>(data);
                if (name.equals("contains")) return data.containsKey(a[0]);
                if (name.startsWith("get")) return data.containsKey(a[0]) ? data.get(a[0]) : a[1];
                if (name.equals("edit")) {
                    Map<String, Object> staged = new HashMap<>(data);
                    return Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {SharedPreferences.Editor.class}, (e, call, values) -> {
                        String op = call.getName();
                        if (op.equals("commit") || op.equals("apply")) {
                            if (writable) { data.clear(); data.putAll(staged); }
                            return op.equals("commit") ? writable : null;
                        }
                        if (op.equals("clear")) staged.clear();
                        else if (op.equals("remove")) staged.remove(values[0]);
                        else if (op.startsWith("put")) staged.put((String) values[0], values[1]);
                        return e;
                    });
                }
                return null;
            });
        }
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Map<String, Memory> files = new HashMap<>();
        Memory registry = new Memory();
        Memory original = files.computeIfAbsent("settings", k -> new Memory());
        original.data.put("theme", "dark");
        original.data.put("shelf", "netflix,youtube");
        original.data.put("cards", 6);
        original.data.put("weather", false);
        original.data.put("hiddenApps", new HashSet<>(Collections.singleton("prime")));
        original.data.put("topContent", "continue");
        original.data.put("watchOff", new HashSet<>(Collections.singleton("netflix")));
        original.data.put("watchHidden", new HashSet<>(Collections.singleton("prime|A film")));
        original.data.put("tmdbKey", "test-device-key");
        original.data.put("sleepAt", 123L);
        original.data.put("nowPlaying", true);
        Map<String, String> credentials = new HashMap<>();
        credentials.put("aioUrl", "https://media.example.invalid");
        credentials.put("aioUser", "test-user");
        credentials.put("aioPassword", "test-password");
        credentials.put("aioToken", "test-token");
        credentials.put("aioDeviceId", "test-device-id");
        credentials.put("jellyfinUrl", "https://jellyfin.example.invalid");
        credentials.put("jellyfinKey", "test-jellyfin-key");
        original.data.putAll(credentials);
        java.util.function.Function<String, SharedPreferences> factory = n -> files.computeIfAbsent(n, k -> new Memory()).prefs();
        Profiles profiles = new Profiles(registry.prefs(), factory, "Personal");
        check(profiles.active().equals(Profiles.FIRST), "An upgrade must keep the first profile");
        check(profiles.settings().getString("shelf", "").equals("netflix,youtube"), "Existing app order was lost");
        check(profiles.namespace("home-ui").equals("home-ui"), "Legacy UI state must stay readable");
        check(!profiles.select("../../settings"), "Unknown profile must not become a filename");
        check(!profiles.remove(Profiles.FIRST), "The last profile must remain available");

        String kids = profiles.add("Kids", "robot");
        check(kids != null && profiles.select(kids), "New profile must be selectable");
        check(profiles.avatar(kids).equals("robot"), "Chosen avatar was not saved");
        check(profiles.settings().getString("theme", "").equals("dark"), "New profile should start from current settings");
        check(profiles.settings().getInt("cards", 0) == 6 && !profiles.settings().getBoolean("weather", true), "Typed preferences must be copied");
        check(profiles.settings().getString("topContent", "").equals("continue"), "Continue Watching selection must survive a profile copy");
        Set<String> copiedWatchOff = profiles.settings().getStringSet("watchOff", Collections.emptySet());
        copiedWatchOff.add("youtube");
        check(!original.data.get("watchOff").equals(copiedWatchOff), "Continue Watching app choices must not share a mutable set");
        profiles.settings().edit().putString("topContent", "none")
                .putStringSet("watchOff", Collections.singleton("youtube"))
                .putStringSet("watchHidden", Collections.singleton("youtube|A show")).commit();
        check(!profiles.settings().contains("tmdbKey") && !profiles.settings().contains("sleepAt") && !profiles.settings().contains("nowPlaying"), "Device state must not be duplicated");
        for (String key : credentials.keySet()) check(!profiles.settings().contains(key), "Server credentials must not be copied into profiles: " + key);
        profiles.settings().edit().putString("theme", "light").putString("shelf", "youtube").putStringSet("hiddenApps", Collections.singleton("netflix")).commit();
        check(profiles.select(Profiles.FIRST), "First profile must remain available");
        check(profiles.settings().getString("theme", "").equals("dark"), "Changing Kids must not alter the first profile");
        check(profiles.settings().getStringSet("hiddenApps", Collections.emptySet()).contains("prime"), "App selections leaked between profiles");
        check(profiles.settings().getString("topContent", "").equals("continue")
                && profiles.settings().getStringSet("watchOff", Collections.emptySet()).equals(Collections.singleton("netflix"))
                && profiles.settings().getStringSet("watchHidden", Collections.emptySet()).equals(Collections.singleton("prime|A film")),
                "Continue Watching choices leaked between profiles");
        check(profiles.rename(Profiles.FIRST, "Ramon"), "Renaming the first profile should work");
        check(profiles.update(kids, "Kids", "fox"), "An inactive profile must be editable");
        try { profiles.update(kids, "Changed", "../photo"); throw new AssertionError("Unknown avatar accepted"); }
        catch (IllegalArgumentException expected) { }
        check(profiles.name(kids).equals("Kids") && profiles.avatar(kids).equals("fox"), "Invalid edits must be atomic");
        for (String bad : new String[] {"", "  ", "Ramon", "Kids", "line\nbreak", String.join("", Collections.nCopies(33, "x"))}) {
            try { profiles.add(bad); throw new AssertionError("Invalid or duplicate name accepted"); }
            catch (IllegalArgumentException expected) { }
        }

        check(profiles.select(kids), "Kids selection should persist");
        Profiles restarted = new Profiles(registry.prefs(), factory, "Personal");
        check(restarted.active().equals(kids) && restarted.name().equals("Kids"), "Active profile was lost at restart");
        check(restarted.avatar(kids).equals("fox"), "Avatar was lost at restart");
        check(Profiles.AVATARS.subList(0, 8).equals(java.util.Arrays.asList(
                "person", "fox", "cat", "robot", "astronaut", "owl", "panda", "rocket")),
                "An expanded catalogue must preserve existing default avatars");
        check(new HashSet<>(Profiles.AVATARS).size() == Profiles.AVATARS.size(), "Avatar IDs must be unique");
        for (String avatar : Profiles.AVATARS) {
            check(restarted.update(kids, "Kids", avatar), "Bundled avatar must be selectable: " + avatar);
            check(new Profiles(registry.prefs(), factory, "Personal").avatar(kids).equals(avatar),
                    "Bundled avatar must persist after restart: " + avatar);
        }
        check(restarted.update(kids, "Kids", "fox"), "Restoring the original avatar must work");
        check(restarted.settings().getString("shelf", "").equals("youtube"), "Profile settings were lost at restart");
        registry.writable = false;
        check(!restarted.select(Profiles.FIRST) && restarted.active().equals(kids), "Failed saves must not silently switch profiles");
        check(restarted.add("Unsaved") == null, "A failed registry write must not register a profile");
        registry.writable = true;
        check(restarted.remove(kids) && restarted.active().equals(Profiles.FIRST), "Removing active profile must return to the original");
        check(original.data.get("tmdbKey").equals("test-device-key"), "Profile removal must preserve the device key");
        check(!restarted.ids().contains(kids), "Removed profile still listed");
        String replacement = restarted.add("New profile", "panda");
        check(replacement != null && restarted.select(Profiles.FIRST), "A replacement profile must be available");
        check(restarted.remove(Profiles.FIRST), "The original profile must be removable once another exists");
        check(restarted.active().equals(replacement), "Deleting the original must select a remaining profile");
        check(original.data.get("tmdbKey").equals("test-device-key") && original.data.get("sleepAt").equals(123L), "Deleting the original must preserve shared device state");
        check(!original.data.containsKey("theme") && !original.data.containsKey("shelf"), "Deleted original profile settings remain");
        for (Map.Entry<String, String> entry : credentials.entrySet())
            check(entry.getValue().equals(original.data.get(entry.getKey())), "Deleting the original profile must preserve server credentials: " + entry.getKey());
        Profiles afterDelete = new Profiles(registry.prefs(), factory, "Personal");
        check(!afterDelete.ids().contains(Profiles.FIRST), "Deleted original reappeared at restart");
        check(afterDelete.avatar(replacement).equals("panda") && !afterDelete.remove(replacement), "The remaining profile and its avatar must be retained");
        registry.data.put("avatar." + replacement, "unknown");
        check(Profiles.AVATARS.contains(afterDelete.avatar(replacement)), "Invalid stored avatar must have a bundled fallback");
        for (int i = afterDelete.ids().size(); i < Profiles.MAX; i++) check(afterDelete.add("Profile " + i) != null, "Profiles up to the limit must be creatable");
        try { afterDelete.add("Too many"); throw new AssertionError("Profile limit ignored"); }
        catch (IllegalArgumentException expected) { }
        registry.data.put("active", "../../invalid");
        registry.data.put("ids", "../../invalid,default");
        Profiles damaged = new Profiles(registry.prefs(), factory, "Personal");
        check(damaged.active().equals(Profiles.FIRST) && damaged.ids().size() == 1, "Corrupt registry must fall back safely");
        System.out.println("Profiles: migration, isolation, avatars, restart, validation, failed writes, limits and safe deletion passed.");
    }
}
