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
        System.out.println("Photo collage: portrait, landscape, square, EXIF rotation, date, GPS removal and 1/2/3-photo drawing passed.");
    }

    private static void save(Bitmap b, File f) throws Exception {
        try (FileOutputStream out = new FileOutputStream(f)) { b.compress(Bitmap.CompressFormat.JPEG, 92, out); }
    }
}
