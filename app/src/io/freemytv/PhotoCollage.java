package io.freemytv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** A whole collage is prepared off the UI thread; frames only crossfade two finished images. */
final class PhotoCollage {
    private PhotoCollage() {}

    static int[] indices(int count, int first) {
        int[] out = new int[Math.min(3, Math.max(0, count))];
        for (int i = 0; i < out.length; i++) out[i] = Math.floorMod(first + i, count);
        return out;
    }

    static Bitmap render(List<File> files, File first, int width, int height, Locale locale) throws IOException {
        int at = files.indexOf(first);
        if (at < 0) throw new IOException("photo no longer available");
        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            Canvas c = new Canvas(out);
            c.scale(width / 1920f, height / 1080f);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
            p.setShader(new LinearGradient(0, 0, 1920, 1080, 0xFFE2E7E5, 0xFFBBC8CA, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, 1920, 1080, p);
            Bitmap grain = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
            try {
                int[] pixels = new int[128 * 128];
                Random random = new Random(42);
                for (int i = 0; i < pixels.length; i++) pixels[i] = random.nextInt(9) << 24;
                grain.setPixels(pixels, 0, 128, 0, 0, 128, 128);
                p.setShader(new BitmapShader(grain, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
                c.drawRect(0, 0, 1920, 1080, p);
                p.setShader(null);
            } finally {
                grain.recycle();
            }
            int[] chosen = indices(files.size(), at);
            SimpleDateFormat input = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
            input.setLenient(false);
            SimpleDateFormat output = new SimpleDateFormat("d MMM yyyy", locale);
            for (int i = 0; i < chosen.length; i++) {
                File file = files.get(chosen[i]);
                Bitmap b = BitmapFactory.decodeFile(file.getPath());
                if (b == null) throw new IOException("unreadable photo");
                try {
                    float maxW = chosen.length == 1 ? 1320 : chosen.length == 2 ? 780 : 530;
                    float scale = Math.min(maxW / b.getWidth(), 740f / b.getHeight());
                    float w = b.getWidth() * scale, h = b.getHeight() * scale;
                    float x = chosen.length == 1 ? 960 : chosen.length == 2 ? 510 + i * 900 : 355 + i * 605;
                    float y = 505 + (i == 1 ? 24 : -12);
                    c.save();
                    c.translate(x, y);
                    c.rotate(chosen.length == 1 ? -2 : i == 0 ? -5 : i == 1 ? 2 : 5);
                    p.setColor(0xFFF7F8F5);
                    p.setShadowLayer(16, 0, 9, 0x40000000);
                    c.drawRect(-w / 2 - 18, -h / 2 - 18, w / 2 + 18, h / 2 + 18, p);
                    p.clearShadowLayer();
                    c.drawBitmap(b, null, new RectF(-w / 2, -h / 2, w / 2, h / 2), p);
                    String taken = Photos.takenAt(file);
                    if (taken != null) {
                        Date date = null;
                        try { date = input.parse(taken); } catch (java.text.ParseException ignored) {}
                        if (date != null) {
                            p.setShader(new LinearGradient(0, h / 2 - 80, 0, h / 2, 0, 0x99000000, Shader.TileMode.CLAMP));
                            c.drawRect(-w / 2, h / 2 - 80, w / 2, h / 2, p);
                            p.setShader(null);
                            p.setTypeface(Ui.medium);
                            p.setTextSize(22);
                            p.setColor(0xFFFFFFFF);
                            c.drawText(output.format(date), -w / 2 + 16, h / 2 - 18, p);
                        }
                    }
                    c.restore();
                } finally {
                    b.recycle();
                }
            }
            return out;
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            out.recycle();
            throw e;
        }
    }
}
