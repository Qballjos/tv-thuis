package io.freemytv;

import android.content.Context;
import android.content.pm.PackageManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The household media manager (aio-media-server-manager). Home Settings stores the address and
 * either a paired device token or the legacy admin login, and talks to the Homepage API.
 */
final class Aio {
    private Aio() {}

    static final class Hit {
        final String title;
        final String kind;
        final int id;
        final boolean canRequest;

        Hit(String title, String kind, int id, boolean canRequest) {
            this.title = title;
            this.kind = kind;
            this.id = id;
            this.canRequest = canRequest;
        }
    }

    /** A server address is filled in (on the TV, or in config.properties). */
    static boolean configured() {
        return !Prefs.aioUrl().isEmpty();
    }

    /** What changed the list: the address and the account, so a new login fetches again. */
    static String identity() {
        return Prefs.aioUrl() + "\n" + Prefs.aioUser() + "\n" + Prefs.aioToken()
                + "\n" + Prefs.jellyfinUrl() + "\n" + Integer.toHexString(Prefs.jellyfinKey().hashCode());
    }

    /** Opens this Jellyfin item in Wholphin. False when Wholphin isn't the player, or the card has no item id. */
    static boolean viewInWholphin(Context context, String pkg, String itemId) {
        if (context == null || itemId == null || itemId.isEmpty() || pkg == null || !pkg.contains("wholphin")) return false;
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("wholphin://view?itemId=" + android.net.Uri.encode(itemId)));
            intent.setPackage(pkg);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (RuntimeException e) {
            AppLog.w("wholphin: " + (e.getMessage() == null ? e.toString() : e.getMessage()));
            return false;
        }
    }

    /** Wholphin, then the official Jellyfin app, then Plex. */
    static String playerPackage(Context c) {
        String[] pkgs = {"com.github.damontecres.wholphin", "com.github.damontecres.wholphin.debug",
                "org.jellyfin.androidtv", "org.jellyfin.androidtv.firetv", "com.plexapp.android"};
        PackageManager pm = c.getPackageManager();
        for (String pkg : pkgs) {
            try {
                pm.getPackageInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    /** Signs in and reads Home. A sentence for the settings row; never throws. */
    static String test() {
        try {
            if (!Prefs.aioPaired()) forgetToken();
            ensureToken();
            JSONObject home = get("/api/homepage");
            int trending = array(home, "trending").length();
            int recent = array(home, "recent").length();
            return Ui.trf("Signed in. {trending} trending, {recent} recently added.",
                    "trending", String.valueOf(trending), "recent", String.valueOf(recent));
        } catch (Exception e) {
            return message(e);
        }
    }

    static String unpair() {
        try {
            send("POST", "/api/tv/disconnect", "{}");
            Prefs.clearAioPairing();
            return null;
        } catch (Exception e) { return message(e); }
    }

    /** Enough titles that franchise dedup still leaves a full row (Prefs.cards() is at least 25). */
    private static final int RAIL = 40;

    /** Trending titles and recently added movies and shows. The snapshot is one short page; the rest is paged. */
    static void fill(List<Recs.Pick> trending, List<Recs.Pick> recent) throws IOException {
        ensureToken();
        JSONObject home = get("/api/homepage");
        readCards(array(home, "trending"), trending, false);
        readCards(array(home, "recent"), recent, true);
        for (int page = 2; page <= 8 && (countKind(trending, "movie") < RAIL || countKind(trending, "tv") < RAIL); page++) {
            JSONArray items;
            try {
                items = array(get("/api/homepage/trending?page=" + page), "items");
            } catch (IOException e) {
                break;
            }
            if (items.length() == 0) break;
            readCards(items, trending, false);
        }
        for (int page = 1; page <= 4 && recent.size() < RAIL; page++) {
            JSONArray items;
            try {
                items = array(get("/api/homepage/recent?page=" + page), "items");
            } catch (IOException e) {
                break;
            }
            if (items.length() == 0) break;
            readCards(items, recent, true);
        }
        if (trending.isEmpty() && recent.isEmpty()) throw new IOException(Ui.tr("The media manager returned no titles."));
    }

    /** A page of the owned library; older AIO servers ignore the type filter, so the caller also filters it. */
    static boolean fillLibrary(String kind, int page, List<Recs.Pick> into) throws IOException {
        if (!("movie".equals(kind) || "tv".equals(kind))) throw new IOException("Invalid library type");
        ensureToken();
        JSONObject result = get("/api/homepage/recent?mediaType=" + kind + "&page=" + page);
        JSONArray rows = array(result, "items");
        if (rows.length() == 0 && !result.optString("error").isEmpty()) throw new IOException(result.optString("error"));
        readCards(rows, into, true);
        return rows.length() >= 20 && page < 500;
    }

    /** Seerr search. Empty when the query is too short. */
    static List<Hit> search(String query) throws IOException {
        List<Hit> out = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) return out;
        ensureToken();
        JSONObject res = get("/api/homepage/search?provider=seerr&q=" + java.net.URLEncoder.encode(q, "UTF-8"));
        if (res.optString("error", "").length() > 0 && array(res, "results").length() == 0) {
            throw new IOException(res.optString("error"));
        }
        JSONArray rows = array(res, "results");
        for (int i = 0; i < rows.length() && out.size() < 12; i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            int id = row.optInt("mediaId", row.optInt("id", 0));
            if (id <= 0) continue;
            String kind = kind(row.optString("mediaType", "movie"));
            String title = row.optString("title", row.optString("name", kind + " " + id));
            boolean can = row.has("can_request") ? row.optBoolean("can_request") : row.optBoolean("canRequest", true);
            out.add(new Hit(title, kind, id, can));
        }
        return out;
    }

    /** Submits a Seerr request. Null when it went through, otherwise why not. */
    static String request(String kind, int mediaId) {
        try {
            ensureToken();
            JSONObject body = new JSONObject().put("mediaType", kind(kind)).put("mediaId", mediaId);
            if ("tv".equals(kind(kind))) body.put("seasons", "all");
            JSONObject res = send("POST", "/api/homepage/request", body.toString());
            if (res.optBoolean("ok", true) && !res.has("detail")) return null;
            if (res.optBoolean("ok")) return null;
            String detail = res.optString("detail", "");
            return detail.isEmpty() ? Ui.tr("The request was refused.") : detail;
        } catch (Exception e) {
            return message(e);
        }
    }

    /** Opens a connection to an image, with the session when the image lives on the media manager. */
    static HttpURLConnection openImage(String url) throws IOException {
        HttpURLConnection c = AioHttp.open(url);
        c.setConnectTimeout(8000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(!onServer(url) && !onJellyfin(url));
        if (onServer(url)) c.setRequestProperty("Authorization", "Bearer " + Prefs.aioToken());
        else if (onJellyfin(url)) jellyfinAuth(c);
        return c;
    }

    /** Jellyfin Primary is the portrait poster; Backdrop is the landscape art. Empty without a server or an item id. */
    private static String jellyfinImage(String itemId, String kind, String query) {
        if (itemId == null || itemId.isEmpty() || Prefs.jellyfinUrl().isEmpty()) return "";
        return Prefs.jellyfinUrl() + "/Items/" + itemId + "/Images/" + kind + "?" + query;
    }

    /**
     * The picture for one card shape. The row's own address comes first (TMDB or the manager's art link, both
     * https). Only when that is empty do we ask the manager for Jellyfin art, so every TV can load it: plain
     * Jellyfin is often http, which the release APK does not allow.
     */
    private static String sizedImage(String raw, String itemId, boolean wide) {
        String url = raw == null ? "" : raw.trim();
        if (url.isEmpty()) return artUrl(itemId, wide);
        if (url.startsWith("/")) url = Prefs.aioUrl() + url;
        if (url.contains("/api/homepage/art")) return artQuery(url, wide);
        String size = wide ? "/w1280/" : "/w780/";
        return url.replace("/w92/", size).replace("/w154/", size).replace("/w185/", size)
                .replace("/w342/", size).replace("/w500/", size).replace("/w780/", size).replace("/original/", size);
    }

    /** Manager art over https, else Jellyfin on the LAN when that is all there is. */
    private static String artUrl(String itemId, boolean wide) {
        if (itemId == null || itemId.isEmpty()) return "";
        if (!Prefs.aioUrl().isEmpty()) {
            return Prefs.aioUrl() + "/api/homepage/art?source=jellyfin&item_id=" + itemId
                    + (wide ? "&image=Backdrop&fillWidth=1280&fillHeight=720"
                            : "&image=Primary&fillWidth=780&fillHeight=1170");
        }
        return jellyfinImage(itemId, wide ? "Backdrop" : "Primary", wide ? "maxWidth=1280" : "maxHeight=780");
    }

    private static String artQuery(String url, boolean wide) {
        int q = url.indexOf('?');
        String base = q >= 0 ? url.substring(0, q) : url;
        String query = q >= 0 ? url.substring(q + 1) : "";
        StringBuilder kept = new StringBuilder();
        for (String part : query.split("&")) {
            if (part.isEmpty() || part.startsWith("image=") || part.startsWith("fillWidth=")
                    || part.startsWith("fillHeight=")) continue;
            if (kept.length() > 0) kept.append('&');
            kept.append(part);
        }
        if (kept.length() > 0) kept.append('&');
        kept.append("image=").append(wide ? "Backdrop" : "Primary");
        kept.append(wide ? "&fillWidth=1280&fillHeight=720" : "&fillWidth=780&fillHeight=1170");
        return base + "?" + kept;
    }

    private static void readCards(JSONArray rows, List<Recs.Pick> into, boolean recent) {
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Recs.Pick have : into) seen.add(cardKey(have));
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            String title = row.optString("title", "").trim();
            if (title.isEmpty()) continue;
            Recs.Pick p = new Recs.Pick();
            p.aio = true;
            p.recent = recent;
            p.addedAt = MediaOrder.time(row.optString("when", row.optString("DateCreated", row.optString("addedAt", ""))));
            p.kind = kind(row.optString("mediaType", recent ? "movie" : "movie"));
            int mediaId = row.optInt("mediaId", 0);
            p.tmdbId = mediaId;
            p.id = mediaId > 0 ? mediaId : into.size() + i + 1;
            p.itemId = libraryId(row);
            p.title = title;
            p.canRequest = !recent && (row.has("can_request") ? row.optBoolean("can_request") : row.optBoolean("canRequest"));
            String detail = row.optString("detail", "");
            String status = row.optString("status", "");
            p.meta = status.isEmpty() ? detail : (detail.isEmpty() ? statusLabel(status) : detail + " · " + statusLabel(status));
            String portrait = sizedImage(row.optString("poster", ""), p.itemId, false);
            String landscape = sizedImage(row.optString("backdrop", ""), p.itemId, true);
            p.imageUrl = !landscape.isEmpty() ? landscape : portrait;   // wide card
            p.posterUrl = !portrait.isEmpty() ? portrait : p.imageUrl;  // tall card
            String token = p.itemId != null ? p.itemId : p.kind + "-" + p.id;
            p.backdrop = "aio-v2-" + (recent ? "r-" : "t-") + token;
            p.poster = p.backdrop;
            p.detailed = true;
            p.app = p.canRequest ? "request" : "library";
            p.pkg = "aio";
            if (!seen.add(cardKey(p))) continue;
            into.add(p);
        }
    }

    /** A small id is the row index given when Seerr sent no mediaId, so the title keeps later pages apart. */
    private static String cardKey(Recs.Pick p) {
        String side = p.recent ? "r:" : "t:";
        if (p.itemId != null) return side + p.itemId;
        if (p.id > 1000) return side + p.kind + ":" + p.id;
        return side + p.kind + ":" + p.title;
    }

    private static int countKind(List<Recs.Pick> list, String kind) {
        int n = 0;
        for (Recs.Pick p : list) if (kind.equals(p.kind)) n++;
        return n;
    }

    private static String statusLabel(String status) {
        if ("available".equals(status)) return Ui.tr("In your library");
        if ("requested".equals(status)) return Ui.tr("Requested");
        if ("partial".equals(status)) return Ui.tr("Partly in your library");
        if ("missing".equals(status)) return Ui.tr("Request");
        return status;
    }

    private static String kind(String raw) {
        String v = raw == null ? "" : raw.trim().toLowerCase();
        return "tv".equals(v) || "series".equals(v) || "show".equals(v) ? "tv" : "movie";
    }

    private static JSONArray array(JSONObject o, String key) {
        JSONArray a = o.optJSONArray(key);
        return a == null ? new JSONArray() : a;
    }

    private static void forgetToken() {
        Prefs.setAioToken("");
    }

    private static void ensureToken() throws IOException {
        if (!Prefs.aioToken().isEmpty()) return;
        if (Prefs.aioPaired()) throw new IOException(Ui.tr("The AIO connection was revoked. Pair again in AIO."));
        if (Prefs.aioUrl().isEmpty() || Prefs.aioUser().isEmpty() || Prefs.aioPassword().isEmpty()) {
            throw new IOException(Ui.tr("Fill in the server, username and password."));
        }
        JSONObject body = new JSONObject();
        try {
            body.put("username", Prefs.aioUser()).put("password", Prefs.aioPassword());
        } catch (org.json.JSONException e) {
            throw new IOException(e.getMessage());
        }
        JSONObject res = send("POST", "/api/auth/login", body.toString(), false);
        String token = res.optString("access_token", "");
        if (token.isEmpty()) throw new IOException(Ui.tr("The media manager did not return a session."));
        Prefs.setAioToken(token);
    }

    private static JSONObject get(String path) throws IOException {
        return send("GET", path, null);
    }

    private static JSONObject send(String method, String path, String body) throws IOException {
        return send(method, path, body, true);
    }

    private static JSONObject send(String method, String path, String body, boolean authed) throws IOException {
        String base = Prefs.aioUrl();
        if (base.isEmpty()) throw new IOException(Ui.tr("Fill in the server, username and password."));
        HttpURLConnection c = AioHttp.open(base + path);
        c.setConnectTimeout(8000);
        c.setReadTimeout(20000);
        c.setRequestMethod(method);
        c.setRequestProperty("Accept", "application/json");
        if (authed) c.setRequestProperty("Authorization", "Bearer " + Prefs.aioToken());
        if (body != null) {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = read(in);
        if (code == 401) {
            Prefs.setAioToken("");
            throw new IOException(Prefs.aioPaired() ? Ui.tr("The AIO connection was revoked. Pair again in AIO.")
                    : Ui.tr("The media manager refused the login."));
        }
        if (code < 200 || code >= 300) {
            String detail = text;
            try {
                detail = new JSONObject(text).optString("detail", text);
            } catch (org.json.JSONException ignored) {
            }
            throw new IOException(detail == null || detail.isEmpty() ? "HTTP " + code : detail);
        }
        try {
            return text.isEmpty() ? new JSONObject() : new JSONObject(text);
        } catch (org.json.JSONException e) {
            throw new IOException(Ui.tr("The media manager sent something that is not JSON."));
        }
    }

    private static boolean onServer(String url) {
        String base = Prefs.aioUrl();
        return !base.isEmpty() && url != null && AioHttp.sameOrigin(base, url);
    }

    private static boolean onJellyfin(String url) {
        String base = Prefs.jellyfinUrl();
        return !base.isEmpty() && url != null && AioHttp.sameOrigin(base, url);
    }

    /** Jellyfin id on a library row: the id itself, or the id= in the item's web address. */
    private static String libraryId(JSONObject row) {
        String id = row.optString("id", "");
        if (!uuid(id)) {
            String url = row.optString("url", "");
            int q = url.indexOf("id=");
            if (q >= 0) {
                id = url.substring(q + 3);
                int cut = id.indexOf('&');
                if (cut >= 0) id = id.substring(0, cut);
                cut = id.indexOf('#');
                if (cut >= 0) id = id.substring(0, cut);
            }
        }
        return uuid(id) ? id : null;
    }

    private static boolean uuid(String id) {
        if (id == null) return false;
        String s = id.replace("-", "");
        if (s.length() != 32) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) return false;
        }
        return true;
    }

    /**
     * In-progress and next-up items from every Jellyfin account, fullest first.
     * The first user Jellyfin lists often has nothing in progress. No-op until a server and API key are saved.
     */
    static void resume(List<Recs.Pick> into) throws IOException {
        if (configured()) {
            try {
                ensureToken();
                addResume(into, new java.util.HashSet<>(), array(get("/api/homepage/resume"), "items"));
                if (!into.isEmpty() || Prefs.aioPaired()) return;
            } catch (IOException e) {
                if (Prefs.aioPaired()) throw e;   // paired TVs never need a Jellyfin credential
            }
        }
        if (Prefs.jellyfinUrl().isEmpty() || Prefs.jellyfinKey().isEmpty()) return;
        JSONArray users = jfList("/Users");
        List<List<Recs.Pick>> buckets = new ArrayList<>();
        for (int u = 0; u < users.length(); u++) {
            JSONObject user = users.optJSONObject(u);
            String id = user == null ? "" : user.optString("Id", "");
            if (!uuid(id)) continue;
            List<Recs.Pick> rows = new ArrayList<>();
            java.util.HashSet<String> one = new java.util.HashSet<>();
            addResume(rows, one, jfItems("/Users/" + id + "/Items/Resume?Limit=24&Fields=ProductionYear,SeriesName"));
            addResume(rows, one, jfItems("/Shows/NextUp?userId=" + id + "&Limit=16&Fields=ProductionYear,SeriesName"));
            if (!rows.isEmpty()) buckets.add(rows);
        }
        buckets.sort((a, b) -> Integer.compare(b.size(), a.size()));
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (List<Recs.Pick> rows : buckets) {
            for (Recs.Pick p : rows) {
                if (into.size() >= RAIL) return;
                if (p.itemId != null && seen.add(p.itemId)) into.add(p);
            }
        }
    }

    private static void addResume(List<Recs.Pick> into, java.util.HashSet<String> seen, JSONArray items) {
        String base = Prefs.jellyfinUrl();
        for (int i = 0; i < items.length() && into.size() < RAIL; i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String id = item.optString("Id", "");
            if (!uuid(id) || !seen.add(id)) continue;
            String name = item.optString("Name", "").trim();
            String series = item.optString("SeriesName", "").trim();
            String type = item.optString("Type", "");
            if (name.isEmpty() && series.isEmpty()) continue;
            Recs.Pick p = new Recs.Pick();
            p.aio = true;
            p.resume = true;
            p.itemId = id;
            p.id = i + 1;
            p.canRequest = false;
            p.detailed = true;
            p.app = "library";
            p.pkg = "aio";
            boolean episode = "Episode".equals(type);
            p.kind = episode || "Series".equals(type) ? "tv" : "movie";
            p.title = episode && !series.isEmpty() ? series : name;
            int season = item.optInt("ParentIndexNumber", 0), episodeNo = item.optInt("IndexNumber", 0);
            if (episode && season > 0 && episodeNo > 0) {
                String code = "S" + season + "E" + episodeNo;
                p.meta = name.isEmpty() || name.equals(p.title) ? code : code + " · " + name;
            } else {
                int year = item.optInt("ProductionYear", 0);
                p.meta = year > 0 ? Integer.toString(year) : "";
            }
            String imageId = item.optString("SeriesId", "");
            if (!uuid(imageId)) imageId = id;
            p.posterUrl = sizedImage("", imageId, false);
            p.imageUrl = sizedImage("", imageId, true);
            if (p.imageUrl.isEmpty()) p.imageUrl = p.posterUrl;
            p.backdrop = "aio-v2-c-" + id;
            p.poster = p.backdrop;
            into.add(p);
        }
    }

    private static JSONArray jfList(String path) throws IOException {
        String text = jf(path).trim();
        try {
            return text.startsWith("[") ? new JSONArray(text) : new JSONArray();
        } catch (org.json.JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    private static JSONArray jfItems(String path) throws IOException {
        String text = jf(path).trim();
        try {
            if (text.startsWith("[")) return new JSONArray(text);
            JSONArray items = new JSONObject(text).optJSONArray("Items");
            return items == null ? new JSONArray() : items;
        } catch (org.json.JSONException e) {
            throw new IOException(e.getMessage());
        }
    }

    private static String jf(String path) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(Prefs.jellyfinUrl() + path).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(20000);
        jellyfinAuth(c);
        c.setRequestProperty("Accept", "application/json");
        int code = c.getResponseCode();
        String text = read(code >= 400 ? c.getErrorStream() : c.getInputStream());
        if (code >= 400) throw new IOException("HTTP " + code);
        return text;
    }

    /** Jellyfin 10.11 and newer refuse a bare X-Emby-Token; the Authorization line has to name the app. */
    private static void jellyfinAuth(HttpURLConnection c) {
        String token = Prefs.jellyfinKey().replace("\"", "");
        if (token.isEmpty()) return;
        String auth = "MediaBrowser Client=\"TV Thuis\", Device=\"TV\", DeviceId=\"freemytv\", Version=\"1.0.0\", Token=\""
                + token + "\"";
        c.setRequestProperty("Authorization", auth);
        c.setRequestProperty("X-Emby-Authorization", auth);
        c.setRequestProperty("X-Emby-Token", token);
        c.setRequestProperty("X-MediaBrowser-Token", token);
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toString("UTF-8");
    }

    private static String message(Exception e) {
        String m = e.getMessage() == null ? "" : e.getMessage();
        if (m.isEmpty() || m.startsWith("HTTP ") || m.contains("Exception") || m.contains("timed out")
                || m.contains("Failed to") || m.contains("Unable to") || m.contains("Connection")) {
            if (m.isEmpty()) m = e.getClass().getSimpleName();
            return Ui.trf("Couldn't reach the media manager: {error}", "error", m);
        }
        return m;
    }
}
