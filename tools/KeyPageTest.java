package io.freemytv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Exercises the shared phone page without Android's graphics/translation runtime. */
public final class KeyPageTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }

    public static void main(String[] args) throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            boolean watchmode = mode == 1, aio = mode == 2;
            String heading = aio ? "Connect with AIO" : watchmode ? "Watchmode key" : "TMDB key";
            int[] accepted = {0};
            KeyPage.Listener listener = new KeyPage.Listener() {
                public void checking() {}
                public String accept(String key, boolean watch) {
                    check(watch == watchmode, "page routes to its own key service");
                    check(key.equals("one+use/link"), "form input is decoded intact");
                    accepted[0]++;
                    return null;
                }
                public void closed(KeyPage page, boolean expired) {}
            };
            KeyPage page = new KeyPage(InetAddress.getLoopbackAddress(), watchmode, listener, aio);
            try {
                String path = URI.create(page.url).getPath();
                String form = request(page, "GET", path, "");
                check(form.contains("<h1>" + heading + " · Free My TV</h1>"), "correct page heading");
                check(form.contains(aio ? "AIO pairing link" : watchmode ? "api.watchmode.com" : "themoviedb.org"), "correct instructions");
                check(request(page, "POST", "/wrong-token", "key=bad").contains("404 Not Found"), "unknown token rejected");
                check(accepted[0] == 0, "wrong token cannot consume the page");
                check(request(page, "POST", path, "key=one%2Buse%2Flink").contains("Saved on the TV"), "correct input accepted");
                check(request(page, "POST", path, "key=again").contains("409 Conflict"), "page is one use");
                check(accepted[0] == 1, "exactly one save");
            } finally {
                page.close();
            }
        }
        System.out.println("Key pages: TMDB, Watchmode and AIO instructions, routing, token validation and one-use submission passed.");
    }

    private static String request(KeyPage page, String method, String path, String body) throws Exception {
        byte[] raw = (method + " " + path + " HTTP/1.1\r\nContent-Length: " + body.length()
                + "\r\n\r\n" + body).getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        page.handle(new ByteArrayInputStream(raw), out, System.nanoTime() + 1_000_000_000L);
        return out.toString(StandardCharsets.UTF_8.name());
    }
}

// Only Android-dependent presentation and logging are replaced; KeyPage's protocol is the production class.
final class Ui {
    static String language = "en";
    static String tr(String text) { return text; }
}
final class AppLog {
    static void w(String text) { throw new AssertionError(text); }
}
