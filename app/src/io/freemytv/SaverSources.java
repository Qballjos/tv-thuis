package io.freemytv;

import android.content.Context;
import java.util.List;

/** Fork-owned screensaver source precedence and media-manager launching. */
final class SaverSources {
    enum Source { AIO, IMMICH, PHOTOS, RECOMMENDATIONS }

    static Source source(String selected, boolean photos, boolean album, boolean ownPhotosOn) {
        if ("aio".equals(selected)) return Source.AIO;
        if ("immich".equals(selected)) return Source.IMMICH;
        if (photos && album) return Source.PHOTOS;
        return ownPhotosOn ? Source.PHOTOS : Source.RECOMMENDATIONS;
    }

    /** An explicit album never falls back to unrelated art while empty or loading. */
    static List<Recs.Pick> photos(Context context, Recs recs) {
        String selected = Prefs.saverSource();
        boolean photos = Prefs.photos(), album = "album".equals(Prefs.photoSource());
        // Do not inspect automatic photo availability for explicit sources (which may be loading).
        boolean own = !"aio".equals(selected) && !"immich".equals(selected) && !(photos && album) && Photos.on(context);
        switch (source(selected, photos, album, own)) {
            case AIO: return recs.aioShowcase();
            case IMMICH: return Photos.picks(context, Photos.saverFiles(context));
            case PHOTOS: return Photos.picks(context);
            default: return recs.showcase();
        }
    }

    static boolean watch(Context context, Recs.Pick p) {
        if (p.aio && p.canRequest) {
            final String kind = p.kind;
            final int id = p.id;
            new Thread(() -> Aio.request(kind, id), "aio-request").start();
            return true;
        }
        if (p.aio) {
            String player = Aio.playerPackage(context);
            if (player == null) return false;
            if (Aio.viewInWholphin(context, player, p.itemId)) return true;
            boolean opened = HomeApps.open(context, HomeApps.app(player, player.contains("plex") ? "Plex" : "Wholphin", null), null);
            if (!opened) AppLog.w("screensaver: couldn't open “" + p.title + "” in " + player);
            return opened;
        }
        return false;
    }
}
