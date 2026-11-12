package io.freemytv;

import android.content.Context;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class RecentTest {
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    static void drain(ExecutorService writer) throws Exception { writer.submit(() -> {}).get(5, TimeUnit.SECONDS); }
    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("recent-profiles").toFile();
        Context c = new Context(dir);
        Field field = Recent.class.getDeclaredField("WRITER");
        field.setAccessible(true);
        ExecutorService writer = (ExecutorService) field.get(null);
        HomeApps.Item a = new HomeApps.Item("a"), b = new HomeApps.Item("b");
        List<HomeApps.Item> shelf = Arrays.asList(a, b);
        try {
            Recent.note(c, "b"); drain(writer);
            check(Recent.order(c, shelf).get(0) == b, "Default profile remembers its launch");
            Prefs.profile = "other";
            check(Recent.orderKnown(shelf).get(0) == a, "UI cache must not leak another profile's history");
            check(Recent.order(c, shelf).get(0) == a, "A new profile starts without another profile's history");
            Recent.note(c, "a"); drain(writer);
            Recent.clear(c); drain(writer);
            Prefs.profile = "default";
            check(Recent.order(c, shelf).get(0) == b, "Clearing another profile must preserve default history");

            CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
            writer.submit(() -> { entered.countDown(); try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            check(entered.await(5, TimeUnit.SECONDS), "Writer gate started");
            Prefs.profile = "queued";
            Recent.note(c, "b");
            Prefs.profile = "other";
            release.countDown(); drain(writer);
            check(Recent.order(c, shelf).get(0) == a, "Queued launch must not move to newly selected profile");
            Prefs.profile = "queued";
            check(Recent.order(c, shelf).get(0) == b, "Queued launch stays with originating profile");
            CountDownLatch clearing = new CountDownLatch(1), clearRelease = new CountDownLatch(1);
            writer.submit(() -> { clearing.countDown(); try { clearRelease.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
            check(clearing.await(5, TimeUnit.SECONDS), "Clear gate started");
            Recent.clear(c);
            Prefs.profile = "default";
            clearRelease.countDown(); drain(writer);
            check(Recent.order(c, shelf).get(0) == b, "Queued clear must not erase newly selected profile");
            Prefs.profile = "queued";
            check(Recent.order(c, shelf).get(0) == a, "Queued clear deletes only originating profile history");
            Prefs.enabled = false;
            Recent.note(c, "b"); drain(writer);
            check(Recent.order(c, shelf).get(0) == a, "Manual shelf order must not record launches");
            System.out.println("Recent: profile files, UI cache, queued launches/clears and disabled recording passed.");
        } finally {
            writer.shutdownNow();
            for (File f : dir.listFiles()) f.delete();
            dir.delete();
        }
    }
}
