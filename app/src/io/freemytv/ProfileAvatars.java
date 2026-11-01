package io.freemytv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import java.util.HashMap;
import java.util.Map;

/** Original, bundled vector portraits, rasterized once for both the manager and the home button. */
final class ProfileAvatars {
    private ProfileAvatars() {}
    private static final Map<String, Bitmap> IMAGES = new HashMap<>();
    private static final int[] COLORS = {0xFF5669D8, 0xFFE47435, 0xFFA457B8, 0xFF28A898,
            0xFF3E79CB, 0xFF9B6AB6, 0xFF45A381, 0xFF5265B4,
            0xFF4099B0, 0xFFC86E32, 0xFF9C7153, 0xFFB56592,
            0xFF369A85, 0xFF687FB0, 0xFF3999B7, 0xFFAD5A3B,
            0xFFB98436, 0xFF9C68C8, 0xFF438F71, 0xFF7055B8,
            0xFF52617F, 0xFFB15659, 0xFF5B77B6, 0xFF5876A8, 0xFF4E9DC2};

    static String label(String id) {
        switch (id) {
            case "quiet-slate": return Ui.tr("Slate portrait");
            case "quiet-stone": return Ui.tr("Stone portrait");
            case "quiet-moon": return Ui.tr("Moon");
            case "quiet-mountain": return Ui.tr("Mountains");
            case "quiet-leaf": return Ui.tr("Leaf");
            case "quiet-rings": return Ui.tr("Rings");
            case "fox": return Ui.tr("Fox");
            case "cat": return Ui.tr("Cat");
            case "robot": return Ui.tr("Robot");
            case "astronaut": return Ui.tr("Astronaut");
            case "owl": return Ui.tr("Owl");
            case "panda": return Ui.tr("Panda");
            case "rocket": return Ui.tr("Rocket");
            case "dog": return Ui.tr("Dog");
            case "lion": return Ui.tr("Lion");
            case "bear": return Ui.tr("Bear");
            case "rabbit": return Ui.tr("Rabbit");
            case "frog": return Ui.tr("Frog");
            case "koala": return Ui.tr("Koala");
            case "penguin": return Ui.tr("Penguin");
            case "tiger": return Ui.tr("Tiger");
            case "monkey": return Ui.tr("Monkey");
            case "unicorn": return Ui.tr("Unicorn");
            case "dinosaur": return Ui.tr("Dinosaur");
            case "alien": return Ui.tr("Alien");
            case "ninja": return Ui.tr("Ninja");
            case "pirate": return Ui.tr("Pirate");
            case "star": return Ui.tr("Star");
            case "gamepad": return Ui.tr("Game controller");
            case "rainbow": return Ui.tr("Rainbow");
            default: return Ui.tr("Person");
        }
    }

    static synchronized Bitmap image(String avatar) {
        String id = Profiles.AVATARS.contains(avatar) ? avatar : Profiles.AVATARS.get(0);
        Bitmap found = IMAGES.get(id);
        if (found != null) return found;
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Path clip = new Path();
        clip.addCircle(128, 128, 126, Path.Direction.CW);
        c.clipPath(clip);
        if (id.startsWith("quiet-")) {
            quiet(c, p, id);
            IMAGES.put(id, bitmap);
            return bitmap;
        }
        int color = COLORS[Profiles.AVATARS.indexOf(id)];
        p.setShader(new LinearGradient(24, 12, 220, 256, lighter(color), color, Shader.TileMode.CLAMP));
        c.drawCircle(128, 128, 126, p);
        p.setShader(null);
        circle(c, p, 48, 47, 28, 0x18FFFFFF);
        circle(c, p, 218, 214, 58, 0x10000000);
        switch (id) {
            case "fox": fox(c, p); break;
            case "cat": cat(c, p); break;
            case "robot": robot(c, p); break;
            case "astronaut": astronaut(c, p); break;
            case "owl": owl(c, p); break;
            case "panda": panda(c, p); break;
            case "rocket": rocket(c, p); break;
            case "dog": dog(c, p); break;
            case "lion": lion(c, p); break;
            case "bear": bear(c, p); break;
            case "rabbit": rabbit(c, p); break;
            case "frog": frog(c, p); break;
            case "koala": koala(c, p); break;
            case "penguin": penguin(c, p); break;
            case "tiger": tiger(c, p); break;
            case "monkey": monkey(c, p); break;
            case "unicorn": unicorn(c, p); break;
            case "dinosaur": dinosaur(c, p); break;
            case "alien": alien(c, p); break;
            case "ninja": ninja(c, p); break;
            case "pirate": pirate(c, p); break;
            case "star": star(c, p); break;
            case "gamepad": gamepad(c, p); break;
            case "rainbow": rainbow(c, p); break;
            default: person(c, p);
        }
        IMAGES.put(id, bitmap);
        return bitmap;
    }

    private static void quiet(Canvas c, Paint p, String id) {
        int background = id.equals("quiet-stone") ? 0xFF504C47 : id.equals("quiet-leaf") ? 0xFF39463F : 0xFF303A46;
        circle(c, p, 128, 128, 126, background);
        int ink = 0xFFD6D9DA;
        if (id.equals("quiet-slate") || id.equals("quiet-stone")) {
            circle(c, p, 128, 98, 32, ink);
            oval(c, p, 64, 145, 192, 251, ink);
        } else if (id.equals("quiet-moon")) {
            circle(c, p, 125, 128, 64, ink);
            circle(c, p, 151, 106, 57, background);
            circle(c, p, 183, 68, 3, ink);
        } else if (id.equals("quiet-mountain")) {
            triangle(c, p, 0xFF8E9AA5, 37, 187, 104, 83, 171, 187);
            triangle(c, p, ink, 98, 187, 161, 109, 220, 187);
        } else if (id.equals("quiet-leaf")) {
            c.save(); c.rotate(35, 128, 128);
            oval(c, p, 91, 60, 165, 193, ink);
            p.setColor(background); p.setStrokeWidth(5); c.drawLine(128, 94, 128, 205, p);
            c.restore();
        } else {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5); p.setColor(ink);
            c.drawCircle(108, 128, 49, p); c.drawCircle(150, 128, 49, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private static int lighter(int color) {
        return Color.rgb((Color.red(color) + 100) / 2, (Color.green(color) + 190) / 2, (Color.blue(color) + 255) / 2);
    }

    private static void circle(Canvas c, Paint p, float x, float y, float r, int color) {
        p.setColor(color); c.drawCircle(x, y, r, p);
    }

    private static void oval(Canvas c, Paint p, float l, float t, float r, float b, int color) {
        p.setColor(color); c.drawOval(l, t, r, b, p);
    }

    private static void box(Canvas c, Paint p, float l, float t, float r, float b, float radius, int color) {
        p.setColor(color); c.drawRoundRect(l, t, r, b, radius, radius, p);
    }

    private static void triangle(Canvas c, Paint p, int color, float... xy) {
        Path path = new Path(); path.moveTo(xy[0], xy[1]);
        path.lineTo(xy[2], xy[3]); path.lineTo(xy[4], xy[5]); path.close();
        p.setColor(color); c.drawPath(path, p);
    }

    private static void eyes(Canvas c, Paint p, float y) {
        circle(c, p, 99, y, 7, 0xFF17243C); circle(c, p, 157, y, 7, 0xFF17243C);
        circle(c, p, 101, y - 2, 2, Color.WHITE); circle(c, p, 159, y - 2, 2, Color.WHITE);
    }

    private static void person(Canvas c, Paint p) {
        oval(c, p, 49, 165, 207, 299, 0xFFE9F0FF);
        box(c, p, 111, 145, 145, 192, 12, 0xFFF2BB96);
        oval(c, p, 75, 54, 181, 165, 0xFF24314E);
        oval(c, p, 83, 73, 173, 175, 0xFFFFD1AE);
        oval(c, p, 78, 50, 161, 104, 0xFF24314E);
        eyes(c, p, 125);
        box(c, p, 115, 146, 141, 152, 3, 0xFFC37669);
    }

    private static void fox(Canvas c, Paint p) {
        triangle(c, p, 0xFF9F402E, 60, 123, 65, 45, 117, 86);
        triangle(c, p, 0xFF9F402E, 139, 86, 191, 45, 196, 123);
        triangle(c, p, 0xFFFFCAB2, 73, 99, 77, 65, 101, 88);
        triangle(c, p, 0xFFFFCAB2, 155, 88, 179, 65, 183, 99);
        oval(c, p, 55, 79, 201, 214, 0xFFFFAA5B);
        oval(c, p, 61, 134, 134, 211, 0xFFFFECDC);
        oval(c, p, 122, 134, 195, 211, 0xFFFFECDC);
        eyes(c, p, 129);
        triangle(c, p, 0xFF293147, 114, 163, 142, 163, 128, 179);
    }

    private static void cat(Canvas c, Paint p) {
        triangle(c, p, 0xFFEAD9FF, 58, 126, 61, 53, 117, 92);
        triangle(c, p, 0xFFEAD9FF, 139, 92, 195, 53, 198, 126);
        oval(c, p, 55, 83, 201, 215, 0xFFEAD9FF);
        triangle(c, p, 0xFFE59BB6, 73, 95, 73, 72, 98, 93);
        triangle(c, p, 0xFFE59BB6, 158, 93, 183, 72, 183, 95);
        eyes(c, p, 137);
        triangle(c, p, 0xFFAE6E91, 119, 159, 137, 159, 128, 170);
        p.setColor(0xFF806A9B); p.setStrokeWidth(4); p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(64, 158, 96, 164, p); c.drawLine(62, 175, 95, 175, p);
        c.drawLine(160, 164, 192, 158, p); c.drawLine(161, 175, 194, 175, p);
    }

    private static void robot(Canvas c, Paint p) {
        box(c, p, 123, 48, 133, 87, 5, 0xFFCEE9E9); circle(c, p, 128, 45, 13, 0xFFFFDA7B);
        box(c, p, 42, 115, 65, 160, 9, 0xFF5A697D); box(c, p, 191, 115, 214, 160, 9, 0xFF5A697D);
        box(c, p, 59, 82, 197, 202, 28, 0xFFE2F2F3);
        box(c, p, 75, 108, 181, 161, 17, 0xFF243850);
        circle(c, p, 101, 134, 12, 0xFF71E5D1); circle(c, p, 155, 134, 12, 0xFF71E5D1);
        box(c, p, 103, 178, 153, 185, 3, 0xFF577283);
    }

    private static void astronaut(Canvas c, Paint p) {
        oval(c, p, 54, 166, 202, 289, 0xFFEAF2FB);
        circle(c, p, 128, 116, 75, 0xFFCFDEEE); circle(c, p, 128, 112, 66, Color.WHITE);
        box(c, p, 72, 73, 184, 148, 30, 0xFF233D65);
        oval(c, p, 83, 83, 151, 104, 0xFF567DB3);
        box(c, p, 97, 183, 159, 224, 8, 0xFF6E9ECD); circle(c, p, 117, 202, 6, 0xFFFFAE70);
        box(c, p, 129, 197, 147, 204, 3, Color.WHITE);
    }

    private static void owl(Canvas c, Paint p) {
        triangle(c, p, 0xFF6D456D, 57, 122, 58, 56, 114, 87);
        triangle(c, p, 0xFF6D456D, 142, 87, 198, 56, 199, 122);
        oval(c, p, 51, 79, 205, 224, 0xFFBE8A91);
        circle(c, p, 96, 133, 40, 0xFFFFE9CD); circle(c, p, 160, 133, 40, 0xFFFFE9CD);
        eyes(c, p, 131);
        triangle(c, p, 0xFFFFC064, 116, 162, 140, 162, 128, 183);
        oval(c, p, 94, 191, 162, 220, 0xFFE8B8A7);
    }

    private static void panda(Canvas c, Paint p) {
        circle(c, p, 76, 81, 28, 0xFF253D42); circle(c, p, 180, 81, 28, 0xFF253D42);
        oval(c, p, 54, 72, 202, 218, 0xFFF5F5E9);
        oval(c, p, 73, 116, 116, 169, 0xFF31474B); oval(c, p, 140, 116, 183, 169, 0xFF31474B);
        circle(c, p, 100, 138, 8, Color.WHITE); circle(c, p, 156, 138, 8, Color.WHITE);
        circle(c, p, 101, 140, 4, 0xFF17243C); circle(c, p, 155, 140, 4, 0xFF17243C);
        oval(c, p, 116, 171, 140, 186, 0xFF31474B);
    }

    private static void rocket(Canvas c, Paint p) {
        circle(c, p, 58, 67, 5, 0xFFECEDFF); circle(c, p, 201, 123, 4, 0xFFECEDFF);
        c.save(); c.rotate(24, 128, 128);
        triangle(c, p, 0xFFFFB950, 107, 187, 149, 187, 128, 235);
        triangle(c, p, 0xFFFFE3A3, 117, 187, 139, 187, 128, 218);
        triangle(c, p, 0xFF91BCEC, 100, 116, 66, 189, 112, 176);
        triangle(c, p, 0xFF91BCEC, 156, 116, 190, 189, 144, 176);
        oval(c, p, 93, 37, 163, 200, 0xFFF4F1F0);
        triangle(c, p, 0xFFE57877, 99, 78, 128, 35, 157, 78);
        circle(c, p, 128, 115, 22, 0xFF4D6EAB); circle(c, p, 124, 110, 12, 0xFF9FCDEA);
        box(c, p, 98, 180, 158, 194, 6, 0xFFE57877);
        c.restore();
    }

    private static void dog(Canvas c, Paint p) {
        oval(c, p, 67, 74, 189, 216, 0xFFFFD5A2);
        oval(c, p, 44, 68, 92, 173, 0xFF825543); oval(c, p, 164, 68, 212, 173, 0xFF825543);
        oval(c, p, 85, 100, 123, 153, 0xFFB77C52);
        eyes(c, p, 132);
        oval(c, p, 91, 151, 165, 199, 0xFFFFEDCC);
        oval(c, p, 115, 156, 141, 174, 0xFF293147);
        box(c, p, 118, 182, 138, 207, 9, 0xFFE58D94);
    }

    private static void lion(Canvas c, Paint p) {
        circle(c, p, 128, 139, 90, 0xFF9C4B30);
        circle(c, p, 69, 85, 26, 0xFFECA44E); circle(c, p, 187, 85, 26, 0xFFECA44E);
        circle(c, p, 69, 85, 14, 0xFFC77642); circle(c, p, 187, 85, 14, 0xFFC77642);
        oval(c, p, 61, 80, 195, 211, 0xFFFFCB73);
        eyes(c, p, 131);
        oval(c, p, 89, 151, 133, 191, 0xFFFFE8B4); oval(c, p, 123, 151, 167, 191, 0xFFFFE8B4);
        triangle(c, p, 0xFF7E493A, 115, 156, 141, 156, 128, 172);
        box(c, p, 117, 186, 139, 192, 3, 0xFF7E493A);
    }

    private static void bear(Canvas c, Paint p) {
        circle(c, p, 72, 79, 30, 0xFF9B6146); circle(c, p, 184, 79, 30, 0xFF9B6146);
        circle(c, p, 72, 79, 17, 0xFFDDA57B); circle(c, p, 184, 79, 17, 0xFFDDA57B);
        oval(c, p, 53, 74, 203, 217, 0xFFB77D57);
        eyes(c, p, 130);
        oval(c, p, 92, 151, 164, 204, 0xFFFFD9AB);
        oval(c, p, 113, 155, 143, 175, 0xFF483633);
        box(c, p, 125, 172, 131, 188, 3, 0xFF483633);
        box(c, p, 114, 185, 142, 191, 3, 0xFF483633);
    }

    private static void rabbit(Canvas c, Paint p) {
        oval(c, p, 70, 25, 114, 133, 0xFFF9E8F3); oval(c, p, 142, 25, 186, 133, 0xFFF9E8F3);
        oval(c, p, 82, 39, 102, 116, 0xFFE5A0BD); oval(c, p, 154, 39, 174, 116, 0xFFE5A0BD);
        oval(c, p, 57, 97, 199, 222, 0xFFF9E8F3);
        eyes(c, p, 143);
        circle(c, p, 80, 167, 14, 0xFFF4CBDC); circle(c, p, 176, 167, 14, 0xFFF4CBDC);
        triangle(c, p, 0xFFD785A4, 116, 166, 140, 166, 128, 180);
        box(c, p, 117, 187, 139, 204, 4, Color.WHITE);
    }

    private static void frog(Canvas c, Paint p) {
        circle(c, p, 87, 90, 34, 0xFF85CC6B); circle(c, p, 169, 90, 34, 0xFF85CC6B);
        oval(c, p, 47, 100, 209, 211, 0xFF85CC6B);
        circle(c, p, 87, 89, 22, 0xFFFFF7D4); circle(c, p, 169, 89, 22, 0xFFFFF7D4);
        circle(c, p, 89, 91, 9, 0xFF17243C); circle(c, p, 167, 91, 9, 0xFF17243C);
        circle(c, p, 72, 148, 13, 0xFFCEE498); circle(c, p, 184, 148, 13, 0xFFCEE498);
        oval(c, p, 91, 151, 165, 189, 0xFF3C7452);
        oval(c, p, 84, 142, 172, 173, 0xFF85CC6B);
    }

    private static void koala(Canvas c, Paint p) {
        circle(c, p, 61, 99, 40, 0xFF9BAABF); circle(c, p, 195, 99, 40, 0xFF9BAABF);
        circle(c, p, 61, 99, 26, 0xFFDCE4EE); circle(c, p, 195, 99, 26, 0xFFDCE4EE);
        oval(c, p, 61, 75, 195, 217, 0xFFBAC7D5);
        eyes(c, p, 131);
        oval(c, p, 109, 132, 147, 180, 0xFF364554);
        box(c, p, 118, 190, 138, 195, 3, 0xFF5E7184);
    }

    private static void penguin(Canvas c, Paint p) {
        oval(c, p, 54, 49, 202, 239, 0xFF24374D);
        oval(c, p, 69, 82, 187, 241, 0xFFEFF5F6);
        eyes(c, p, 123);
        triangle(c, p, 0xFFFFBB55, 110, 147, 146, 147, 128, 171);
        oval(c, p, 63, 213, 115, 239, 0xFFFFBB55); oval(c, p, 141, 213, 193, 239, 0xFFFFBB55);
        oval(c, p, 38, 143, 72, 211, 0xFF24374D); oval(c, p, 184, 143, 218, 211, 0xFF24374D);
    }

    private static void tiger(Canvas c, Paint p) {
        circle(c, p, 71, 78, 27, 0xFFCC753D); circle(c, p, 185, 78, 27, 0xFFCC753D);
        circle(c, p, 71, 78, 14, 0xFFFFDFB0); circle(c, p, 185, 78, 14, 0xFFFFDFB0);
        oval(c, p, 52, 76, 204, 219, 0xFFFFB563);
        triangle(c, p, 0xFF503F39, 116, 77, 140, 77, 128, 113);
        triangle(c, p, 0xFF503F39, 54, 122, 81, 140, 53, 144);
        triangle(c, p, 0xFF503F39, 202, 122, 175, 140, 203, 144);
        triangle(c, p, 0xFF503F39, 58, 156, 82, 166, 65, 178);
        triangle(c, p, 0xFF503F39, 198, 156, 174, 166, 191, 178);
        eyes(c, p, 134);
        oval(c, p, 93, 156, 163, 204, 0xFFFFEDD3);
        triangle(c, p, 0xFF9E6053, 116, 162, 140, 162, 128, 177);
    }

    private static void monkey(Canvas c, Paint p) {
        circle(c, p, 58, 135, 30, 0xFF956043); circle(c, p, 198, 135, 30, 0xFF956043);
        circle(c, p, 58, 135, 18, 0xFFE6B78B); circle(c, p, 198, 135, 18, 0xFFE6B78B);
        oval(c, p, 57, 56, 199, 217, 0xFF956043);
        oval(c, p, 73, 97, 134, 187, 0xFFFFD6A4); oval(c, p, 122, 97, 183, 187, 0xFFFFD6A4);
        oval(c, p, 86, 150, 170, 209, 0xFFFFD6A4);
        eyes(c, p, 131);
        circle(c, p, 118, 164, 3, 0xFF956043); circle(c, p, 138, 164, 3, 0xFF956043);
        box(c, p, 108, 184, 148, 191, 4, 0xFF956043);
    }

    private static void unicorn(Canvas c, Paint p) {
        oval(c, p, 57, 81, 210, 230, 0xFFD696D8);
        triangle(c, p, 0xFFFFEDD2, 115, 86, 128, 19, 144, 86);
        triangle(c, p, 0xFFF8EEFF, 73, 125, 66, 58, 112, 91);
        triangle(c, p, 0xFFF8EEFF, 144, 91, 190, 58, 183, 125);
        triangle(c, p, 0xFFEAA7CD, 80, 100, 78, 79, 99, 99);
        triangle(c, p, 0xFFEAA7CD, 157, 99, 178, 79, 176, 100);
        oval(c, p, 63, 87, 193, 219, 0xFFF8EEFF);
        oval(c, p, 91, 77, 171, 112, 0xFF94D5E4);
        eyes(c, p, 138);
        oval(c, p, 85, 162, 171, 208, 0xFFF1C6E6);
        circle(c, p, 111, 181, 4, 0xFFB176AA); circle(c, p, 145, 181, 4, 0xFFB176AA);
    }

    private static void dinosaur(Canvas c, Paint p) {
        triangle(c, p, 0xFFFFD379, 133, 64, 163, 34, 172, 88);
        triangle(c, p, 0xFFFFD379, 169, 90, 204, 75, 202, 124);
        triangle(c, p, 0xFFFFD379, 185, 127, 222, 125, 202, 166);
        oval(c, p, 68, 63, 194, 207, 0xFF8CD3A4);
        oval(c, p, 41, 119, 183, 213, 0xFFADE4B9);
        circle(c, p, 99, 109, 8, 0xFF17243C); circle(c, p, 146, 109, 8, 0xFF17243C);
        circle(c, p, 65, 150, 4, 0xFF4E9974); circle(c, p, 86, 146, 4, 0xFF4E9974);
        box(c, p, 71, 177, 153, 184, 3, 0xFF4E9974);
        triangle(c, p, Color.WHITE, 117, 177, 129, 177, 123, 191);
    }

    private static void alien(Canvas c, Paint p) {
        oval(c, p, 61, 194, 195, 285, 0xFFB6A1E9);
        oval(c, p, 53, 46, 203, 199, 0xFFAEE4B1);
        triangle(c, p, 0xFFAEE4B1, 67, 137, 189, 137, 128, 224);
        oval(c, p, 74, 104, 113, 152, 0xFF253D42); oval(c, p, 143, 104, 182, 152, 0xFF253D42);
        oval(c, p, 82, 112, 95, 125, 0xFFBDECE1); oval(c, p, 151, 112, 164, 125, 0xFFBDECE1);
        box(c, p, 116, 180, 140, 185, 3, 0xFF5E9B7B);
    }

    private static void ninja(Canvas c, Paint p) {
        oval(c, p, 52, 180, 204, 285, 0xFF263449);
        circle(c, p, 128, 127, 78, 0xFF263449);
        box(c, p, 59, 97, 197, 142, 18, 0xFFFFCFAC);
        eyes(c, p, 120);
        box(c, p, 51, 76, 205, 98, 6, 0xFFBA656A);
        triangle(c, p, 0xFFBA656A, 197, 79, 230, 89, 205, 104);
        triangle(c, p, 0xFFBA656A, 199, 93, 225, 125, 195, 113);
        box(c, p, 119, 196, 137, 228, 3, 0xFFBA656A);
    }

    private static void pirate(Canvas c, Paint p) {
        oval(c, p, 57, 183, 199, 285, 0xFFC96468);
        oval(c, p, 77, 79, 179, 212, 0xFFFFCFAC);
        box(c, p, 67, 70, 189, 106, 10, 0xFFE0A352);
        triangle(c, p, 0xFF28354A, 35, 91, 128, 36, 221, 91);
        box(c, p, 41, 82, 215, 104, 10, 0xFF28354A);
        circle(c, p, 128, 72, 10, 0xFFF6E4C8);
        box(c, p, 121, 78, 135, 87, 2, 0xFFF6E4C8);
        p.setColor(0xFF28354A); p.setStrokeWidth(5); c.drawLine(82, 119, 174, 150, p);
        circle(c, p, 100, 137, 16, 0xFF28354A); circle(c, p, 157, 137, 7, 0xFF17243C);
        box(c, p, 116, 181, 146, 188, 4, 0xFFA46354);
    }

    private static void star(Canvas c, Paint p) {
        Path star = new Path();
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            float radius = i % 2 == 0 ? 94 : 47;
            float x = 128 + (float) Math.cos(angle) * radius, y = 134 + (float) Math.sin(angle) * radius;
            if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
        }
        star.close(); p.setColor(0xFFFFD877); c.drawPath(star, p);
        eyes(c, p, 132);
        box(c, p, 116, 155, 140, 162, 4, 0xFFB48147);
    }

    private static void rainbow(Canvas c, Paint p) {
        int[] colors = {0xFFEF7079, 0xFFFFB56B, 0xFFFFE17D, 0xFF83CD93,
                0xFF79B9E8, 0xFF8E91DD, 0xFFB493E3};
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(12);
        for (int i = 0; i < colors.length; i++) {
            int inset = i * 12;
            p.setColor(colors[i]);
            c.drawArc(38 + inset, 54 + inset, 218 - inset, 234 - inset, 180, 180, false, p);
        }
        p.setStyle(Paint.Style.FILL);
        circle(c, p, 46, 151, 20, Color.WHITE); circle(c, p, 73, 142, 23, Color.WHITE);
        circle(c, p, 96, 153, 18, Color.WHITE); box(c, p, 29, 151, 114, 184, 16, Color.WHITE);
        circle(c, p, 160, 153, 18, Color.WHITE); circle(c, p, 183, 142, 23, Color.WHITE);
        circle(c, p, 210, 151, 20, Color.WHITE); box(c, p, 142, 151, 227, 184, 16, Color.WHITE);
    }

    private static void gamepad(Canvas c, Paint p) {
        box(c, p, 40, 94, 216, 172, 36, 0xFFE4EAF8);
        oval(c, p, 35, 108, 94, 211, 0xFFE4EAF8); oval(c, p, 162, 108, 221, 211, 0xFFE4EAF8);
        box(c, p, 66, 114, 82, 159, 3, 0xFF3E516B);
        box(c, p, 51, 129, 97, 145, 3, 0xFF3E516B);
        circle(c, p, 180, 117, 9, 0xFF65AF96); circle(c, p, 198, 135, 9, 0xFFE6A065);
        circle(c, p, 162, 135, 9, 0xFF719FD2); circle(c, p, 180, 153, 9, 0xFFC77EA4);
        box(c, p, 114, 135, 142, 141, 3, 0xFF8293AC);
        circle(c, p, 103, 171, 12, 0xFF6B7F9B); circle(c, p, 151, 171, 12, 0xFF6B7F9B);
    }
}
