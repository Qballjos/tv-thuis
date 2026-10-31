package io.freemytv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Selection runs on the host; decoding, metadata and drawing additionally run on an explicit emulator. */
public final class PhotoCollageTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        for (int count = 0; count <= 100; count++) {
            for (int first = -1; first <= count; first++) {
                int[] indices = PhotoCollage.indices(count, first);
                check(indices.length == Math.min(3, count), "show only available photos");
                HashSet<Integer> seen = new HashSet<>();
                for (int index : indices) {
                    check(index >= 0 && index < count && seen.add(index), "valid, distinct photos at wraparound");
                }
            }
        }
        if (args.length == 0) {
            System.out.println("Photo collage: empty, single, pair and wrapping collections passed.");
            return;
        }
        File folder = new File(args[0]);
        folder.mkdirs();
        System.setProperty("java.io.tmpdir", folder.getPath());
        if (Looper.getMainLooper() == null) Looper.prepareMainLooper();
        Class.forName("android.app.ActivityThread").getMethod("systemMain").invoke(null);
        android.graphics.Typeface.class.getDeclaredMethod("loadPreinstalledSystemFontMap").invoke(null);
        Method decode = Photos.class.getDeclaredMethod("decode", File.class, int.class, int.class);
        decode.setAccessible(true);
        int[][] shapes = {{1800, 3200}, {3840, 2160}, {2400, 2400}, {3200, 1800}};
        List<File> files = new ArrayList<>();
        for (int i = 0; i < shapes.length; i++) {
            int w = shapes[i][0], h = shapes[i][1];
            File raw = new File(folder, "raw-" + i + ".jpg");
            Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.drawColor(new int[] {0xFF659583, 0xFF719CB7, 0xFFB48A67, 0xFF80907B}[i]);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(0xFFFFDF9E);
            c.drawCircle(w * .7f, h * .25f, Math.min(w, h) * .16f, p);
            p.setColor(0xFF305760);
            c.drawRect(0, h * .7f, w, h, p);
            save(b, raw);
            b.recycle();
            ExifInterface tags = new ExifInterface(raw.getPath());
            tags.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2018:10:26 14:32:00");
            tags.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "52/1,0/1,0/1");
            tags.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            if (i == 3) tags.setAttribute(ExifInterface.TAG_ORIENTATION, "6");
            tags.saveAttributes();
            Bitmap result = (Bitmap) decode.invoke(null, raw, Photos.W, Photos.H);
            check(result != null && result.getWidth() <= Photos.W && result.getHeight() <= Photos.H, "bounded decode");
            double expected = i == 3 ? (double) h / w : (double) w / h;
            check(Math.abs((double) result.getWidth() / result.getHeight() - expected) < .003, "whole aspect and EXIF rotation");
            File imported = new File(folder, "photo-" + i + ".jpg");
            save(result, imported);
            result.recycle();
            Photos.setTakenAt(imported, Photos.takenAt(raw));
            tags = new ExifInterface(imported.getPath());
            check("2018:10:26 14:32:00".equals(Photos.takenAt(imported)), "capture date survives");
            check(tags.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null, "GPS removed from private copy");
            files.add(imported);
        }
        Photos.setTakenAt(files.get(0), "2020-03-15T18:10:42.000Z");
        check("2020:03:15 18:10:42".equals(Photos.takenAt(files.get(0))), "Immich ISO capture date");
        Photos.setTakenAt(files.get(0), "invalid");
        check("2020:03:15 18:10:42".equals(Photos.takenAt(files.get(0))), "invalid date leaves metadata alone");
        for (int count = 1; count <= 3; count++) {
            Bitmap collage = PhotoCollage.render(files.subList(0, count), files.get(0), 1920, 1080, Locale.forLanguageTag("nl"));
            save(collage, new File(folder, "collage-" + count + ".jpg"));
            collage.recycle();
        }
        Bitmap collage = PhotoCollage.render(files, files.get(3), 1280, 720, Locale.US);
        save(collage, new File(folder, "collage-720p.jpg"));
        collage.recycle();
        cacheSelections(folder);
        File pairingLink = new File(folder, "aio-pairing-link");
        if (pairingLink.isFile()) {
            try { aioPairing(new String(java.nio.file.Files.readAllBytes(pairingLink.toPath()), java.nio.charset.StandardCharsets.UTF_8)); }
            finally { pairingLink.delete(); }
        }
        System.out.println("Photo collage: portrait, landscape, square, EXIF rotation, date, GPS removal and 1/2/3-photo drawing passed.");
        System.exit(0);   // Android's preferences worker otherwise keeps app_process alive
    }

    private static void cacheSelections(File folder) throws Exception {
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
        android.content.Context system = (android.content.Context) activityThread.getMethod("getSystemContext").invoke(thread);
        system = system.createPackageContext("io.freemytv", 0);
        final android.content.Context packageContext = system;
        android.content.SharedPreferences registry = system.getSharedPreferences("tv_thuis_test_profiles", 0);
        registry.edit().clear().commit();
        Profiles profiles = new Profiles(registry, name -> packageContext.getSharedPreferences("tv_thuis_test_" + name, 0), "Test");
        java.lang.reflect.Field field = Prefs.class.getDeclaredField("profiles");
        field.setAccessible(true);
        field.set(null, profiles);
        File root = new File(folder, "cache-selection");
        android.content.Context context = new android.content.ContextWrapper(packageContext) {
            @Override public File getFilesDir() { return root; }
            @Override public android.content.Context getApplicationContext() { return this; }
            @Override public File getExternalFilesDir(String type) { return null; }
            @Override public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
                return packageContext.getSharedPreferences("tv_thuis_test_" + name, mode);
            }
        };
        field = Prefs.class.getDeclaredField("app");
        field.setAccessible(true);
        field.set(null, context);
        File photos = new File(root, "photos");
        photos.mkdirs();
        File[] old = photos.listFiles();
        if (old != null) for (File file : old) file.delete();
        Prefs.setPhotoSource("immich", "", "");
        ImmichSource all = ImmichPhotos.source();
        File global = new File(photos, "1800000000000-123" + ImmichSource.importedSuffix(all.inboxName("global")));
        global.createNewFile();
        File legacy = new File(photos, "1800000000001-123-immich.jpg");
        legacy.createNewFile();
        File manual = new File(photos, "1800000000002-123.jpg");
        manual.createNewFile();
        check(Photos.files(context).contains(global) && Photos.files(context).contains(manual), "all-photos cache and manual import available");
        String album = "01234567-89ab-4cde-8fab-0123456789ab";
        Prefs.setPhotoSource("album", album, "Selected album");
        check(Photos.files(context).isEmpty(), "switch immediately hides global, legacy and manual photos while album is loading/offline");
        ImmichSource selected = ImmichPhotos.source();
        File inbox = Photos.privateInbox(context);
        inbox.mkdirs();
        java.nio.file.Files.copy(new File(folder, "photo-0.jpg").toPath(), new File(inbox, selected.inboxName("chosen")).toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        check(Photos.importWaiting(context) == 1, "downloaded photo imports with its album tag");
        check(Photos.files(context).size() == 1, "newly fetched album photos become visible");
        File chosen = Photos.files(context).get(0);
        check(selected.visible(chosen), "imported filename retains the album identity");
        Prefs.setPhotoSource("album", "12345678-9abc-4def-8abc-123456789abc", "Empty album");
        check(Photos.files(context).isEmpty(), "empty album never falls back to another album");
        Prefs.setPhotoSource("album", album, "Selected album");
        check(Photos.files(context).size() == 1 && Photos.files(context).contains(chosen), "switching back reuses only that album despite unchanged directory stamp");
        check(global.exists() && legacy.exists() && manual.exists(), "filtering never deletes originals or other cached selections");
        context.getSharedPreferences("settings", 0).edit().putString("aioUrl", "https://media.example.invalid").commit();
        Prefs.setPhotos(true);
        Prefs.setSaverCollage(false);
        Ui.init(context);
        DreamView view = new DreamView(context);
        Method source = DreamView.class.getDeclaredMethod("photos");
        source.setAccessible(true);
        List<?> picks = (List<?>) source.invoke(view);
        check(picks.size() == 1 && chosen.equals(((Recs.Pick) picks.get(0)).photo), "fullscreen album wins over configured AIO art");
        Prefs.setPhotoSource("album", "12345678-9abc-4def-8abc-123456789abc", "Empty album");
        check(((List<?>) source.invoke(view)).isEmpty(), "empty fullscreen album uses clock fallback, never movie art");
        System.out.println("Immich cache: real directory filtering, pending/empty albums and repeated switches passed.");
    }

    private static void save(Bitmap b, File f) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) { b.compress(Bitmap.CompressFormat.JPEG, 92, out); }
    }

    private static void aioPairing(String link) throws Exception {
        AioPairing.Connection connection = AioPairing.claim(link, "Disposable test TV");
        check(Prefs.setAioPairing(connection), "save device connection atomically");
        check(Prefs.aioPaired() && Prefs.aioUser().isEmpty() && Prefs.aioPassword().isEmpty()
                && Prefs.jellyfinUrl().isEmpty() && Prefs.jellyfinKey().isEmpty(), "paired TV has no administrator/Jellyfin credentials");
        String result = Aio.test();
        check(result.equals(Ui.trf("Signed in. {trending} trending, {recent} recently added.", "trending", "0", "recent", "0")), "connection test uses paired authorization");
        check(Prefs.aioToken().equals(connection.token), "connection test retains paired token instead of trying a password login");
        java.util.List<Recs.Pick> resumed = new java.util.ArrayList<>();
        Aio.resume(resumed);
        check(resumed.size() == 1 && resumed.get(0).resume && resumed.get(0).title.equals("Pairing test movie"), "Continue Watching arrives through AIO without a Jellyfin key");
        try { AioPairing.claim(link, "Replay"); throw new AssertionError("pairing link reused"); } catch (java.io.IOException expected) {}
        check(Aio.unpair() == null && !Prefs.aioPaired(), "TV disconnect revokes its device and removes local credentials");
        System.out.println("AIO device flow: real claim, private preferences, connection test, Continue Watching, replay rejection and revocation passed.");
    }
}
