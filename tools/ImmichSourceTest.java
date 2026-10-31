package io.freemytv;

import java.io.File;

/** Regression: selecting an album must never expose cached photos or refresh stamps from another source. */
public final class ImmichSourceTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static File imported(ImmichSource source) {
        return new File("1800000000000-123" + ImmichSource.importedSuffix(source.inboxName("01234567-89ab-4cde-8fab-0123456789ab")));
    }

    public static void main(String[] args) {
        String id = "01234567-89ab-4cde-8fab-0123456789ab";
        String other = "12345678-9abc-4def-8abc-123456789abc";
        ImmichSource all = new ImmichSource("https://photos.example.invalid", "immich", "");
        ImmichSource album = new ImmichSource(all.server, "album", id);
        ImmichSource second = new ImmichSource(all.server, "album", other);
        ImmichSource anotherServer = new ImmichSource("https://other.example.invalid", "album", id);
        File albumPhoto = imported(album), allPhoto = imported(all), secondPhoto = imported(second);
        check(album.enabled && album.visible(albumPhoto), "selected album reads its own cache");
        check(!album.visible(allPhoto) && !album.visible(secondPhoto), "all-photos and other-album caches stay hidden");
        check(!all.visible(albumPhoto), "switching back cannot mix album caches into all-photos");
        check(!anotherServer.visible(albumPhoto), "a different server cannot reuse the album cache");
        check(!album.visible(new File("1800000000000-123-immich.jpg")), "untagged copies from older builds stay hidden");
        File manual = new File("1800000000000-123.jpg");
        check(all.visible(manual) && !album.visible(manual), "manual photos are retained, but excluded from a specific album");
        check(!album.stamp(new File("cache")).equals(all.stamp(new File("cache"))), "an old sync cannot mark a new selection fresh");
        check(!album.stamp(new File("cache")).equals(second.stamp(new File("cache"))), "albums have separate refresh stamps");
        check(new ImmichSource(all.server + "/", "album", id).key.equals(album.key), "trailing slash keeps the same source");
        check(!new ImmichSource(all.server, "album", "").enabled, "missing album ID cannot become a global search");
        check(!new ImmichSource(all.server, "album", "not-an-id").enabled, "invalid album ID cannot become a global search");
        check(!new ImmichSource(all.server, "default", "").enabled, "default source does not fetch Immich");
        check(album.matches("album", id) && !album.matches("album", other), "running work detects a changed album");
        check(ImmichSource.importedSuffix("holiday.jpg").equals(".jpg"), "manual import format stays compatible");
        check(ImmichSource.importedSuffix("immich-invalid.jpg").equals("-immich.jpg"), "malformed tags cannot impersonate an album");
        System.out.println("Immich source: album/server isolation, legacy cache, manual photos, refresh stamps and invalid selections passed.");
    }
}
