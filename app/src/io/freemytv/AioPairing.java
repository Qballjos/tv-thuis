package io.freemytv;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

final class AioPairing {
    private AioPairing() {}
    static final class Link {
        final String server, code;
        Link(String server, String code) { this.server = server; this.code = code; }
    }
    static Link parse(String text) throws IOException {
        try {
            URI uri = new URI(text.trim());
            String fragment = uri.getRawFragment();
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getPort() == 0 || uri.getPort() > 65535
                    || !(uri.getPath().isEmpty() || uri.getPath().equals("/")) || fragment == null || !fragment.startsWith("tv-pair="))
                throw new IOException("Invalid AIO pairing link");
            String code = URLDecoder.decode(fragment.substring(8), "UTF-8");
            if (!code.matches("[A-Za-z0-9_-]{43}")) throw new IOException("Invalid AIO pairing link");
            return new Link(uri.getScheme() + "://" + uri.getRawAuthority(), code);
        } catch (URISyntaxException | IllegalArgumentException e) { throw new IOException("Invalid AIO pairing link"); }
    }

    static final class Connection {
        final String server, token, id;
        Connection(String server, String token, String id) { this.server = server; this.token = token; this.id = id; }
    }
    static Connection claim(String pasted, String deviceName) throws IOException {
        Link link = parse(pasted);
        HttpURLConnection c = AioHttp.open(link.server + "/api/tv/pairings/claim");
        try {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            JSONObject body = new JSONObject().put("code", link.code).put("deviceName", deviceName);
            try (OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            int status = c.getResponseCode();
            if (status == 401) throw new IOException(Ui.tr("This pairing link has expired or was already used."));
            if (status != 200) throw new IOException("AIO pairing: HTTP " + status);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buffer = new byte[4096];
                for (int n; (n = in.read(buffer)) >= 0;) {
                    if (out.size() + n > 16384) throw new IOException("Invalid AIO pairing response");
                    out.write(buffer, 0, n);
                }
            }
            JSONObject response = new JSONObject(out.toString("UTF-8"));
            String token = response.optString("accessToken"), id = response.optString("deviceId");
            if (response.optInt("version") != 1 || !token.matches("tv_[A-Za-z0-9_-]{43}")
                    || !id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) throw new IOException("Invalid AIO pairing response");
            return new Connection(link.server, token, id);
        } catch (org.json.JSONException e) { throw new IOException("Invalid AIO pairing response"); }
        finally { c.disconnect(); }
    }
}
