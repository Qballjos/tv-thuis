package io.freemytv;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public final class AioPairingTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        String code = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ";
        AioPairing.Link link = AioPairing.parse("https://media.example.invalid/#tv-pair=" + code);
        check(link.server.equals("https://media.example.invalid") && link.code.equals(code), "pairing link carries origin and one-use code");
        for (String bad : new String[] {"file:///tmp/secret#tv-pair=" + code, "https://user:password@media.example.invalid/#tv-pair=" + code,
                "https://media.example.invalid/api/anything#tv-pair=" + code, "https://media.example.invalid/?code=x#tv-pair=" + code,
                "https://media.example.invalid/#tv-pair=short", "https://media.example.invalid/#tv-pair=" + code + "&extra=1"}) {
            try { AioPairing.parse(bad); throw new AssertionError("accepted invalid link"); } catch (IOException expected) {}
        }
        check(AioHttp.sameOrigin("https://media.example.invalid", "https://media.example.invalid:443/api/homepage/art"), "default HTTPS port");
        check(!AioHttp.sameOrigin("https://media.example.invalid", "https://media.example.invalid.attacker.invalid/art"), "hostname prefixes cannot receive the token");
        check(!AioHttp.sameOrigin("https://media.example.invalid", "http://media.example.invalid/art"), "no downgrade");
        try { AioHttp.open("http://8.8.8.8/api").getResponseCode(); throw new AssertionError("public HTTP allowed"); } catch (IOException expected) {}
        InetAddress lan = null;
        for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
            for (Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses(); addresses.hasMoreElements();) {
                InetAddress a = addresses.nextElement();
                if (a instanceof Inet4Address && a.isSiteLocalAddress()) lan = a;
            }
        }
        if (lan != null) {
            HttpServer server = HttpServer.create(new InetSocketAddress(lan, 0), 0);
            server.createContext("/echo", exchange -> {
                byte[] body = exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(200, 0);   // chunked, including on our HTTP/1.0 request
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.createContext("/redirect", exchange -> {
                exchange.getResponseHeaders().set("Location", "http://8.8.8.8/never");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            });
            server.start();
            try {
                String base = "http://" + lan.getHostAddress() + ":" + server.getAddress().getPort();
                HttpURLConnection c = AioHttp.open(base + "/echo");
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Authorization", "Bearer test-device-token");
                c.getOutputStream().write("a scoped request".getBytes(StandardCharsets.UTF_8));
                check(c.getResponseCode() == 200 && new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8).equals("a scoped request"), "bounded LAN request/response");
                check(AioHttp.open(base + "/redirect").getResponseCode() == 302, "redirects are never followed");
            } finally { server.stop(0); }
        }
        System.out.println("AIO pairing: link validation, token origin, HTTPS downgrade, private HTTP, chunked responses and redirects passed.");
    }
}
