package io.freemytv;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Runtime AIO addresses: HTTPS normally, plain HTTP only to a pinned private IPv4 address. */
final class AioHttp {
    private AioHttp() {}

    static HttpURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        if (url.getUserInfo() != null || url.getRef() != null) throw new IOException("Invalid AIO address");
        HttpURLConnection c;
        if ("https".equals(url.getProtocol())) c = (HttpURLConnection) url.openConnection();
        else if ("http".equals(url.getProtocol())) c = new LanConnection(url);
        else throw new IOException("AIO requires HTTPS, or HTTP on your own network");
        c.setConnectTimeout(8000);
        c.setReadTimeout(20000);
        c.setInstanceFollowRedirects(false);
        return c;
    }

    static boolean sameOrigin(String base, String address) {
        try {
            URL a = new URL(base), b = new URL(address);
            return a.getProtocol().equals(b.getProtocol()) && a.getHost().equalsIgnoreCase(b.getHost())
                    && (a.getPort() < 0 ? a.getDefaultPort() : a.getPort()) == (b.getPort() < 0 ? b.getDefaultPort() : b.getPort());
        } catch (MalformedURLException e) { return false; }
    }

    // Android cannot express private IP ranges in network_security_config. This connection keeps the restriction
    // at the socket boundary, without enabling arbitrary public cleartext traffic for the app.
    private static final class LanConnection extends HttpURLConnection {
        private final ByteArrayOutputStream request = new ByteArrayOutputStream();
        private byte[] response;
        private Socket socket;

        LanConnection(URL url) { super(url); }
        @Override public OutputStream getOutputStream() { return request; }
        @Override public void connect() throws IOException {
            if (response != null) return;
            InetAddress address = InetAddress.getByName(url.getHost());
            if (!(address instanceof Inet4Address) || !address.isSiteLocalAddress())
                throw new IOException("Plain HTTP AIO must be on your private network; otherwise use HTTPS");
            socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(address, url.getPort() < 0 ? 80 : url.getPort()), getConnectTimeout());
                socket.setSoTimeout(getReadTimeout());
                OutputStream out = socket.getOutputStream();
                String path = url.getFile().isEmpty() ? "/" : url.getFile();
                if (path.indexOf('\r') >= 0 || path.indexOf('\n') >= 0) throw new IOException("Invalid path");
                StringBuilder head = new StringBuilder(method + " " + path + " HTTP/1.0\r\nHost: " + url.getAuthority()
                        + "\r\nConnection: close\r\nAccept-Encoding: identity\r\n");
                for (Map.Entry<String, List<String>> h : getRequestProperties().entrySet()) {
                    for (String value : h.getValue()) {
                        if (!h.getKey().matches("[A-Za-z0-9-]+") || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
                            throw new IOException("Invalid request header");
                        head.append(h.getKey()).append(": ").append(value).append("\r\n");
                    }
                }
                head.append("Content-Length: ").append(request.size()).append("\r\n\r\n");
                out.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
                request.writeTo(out);
                out.flush();
                final long until = System.nanoTime() + 30_000_000_000L;
                InputStream in = new BufferedInputStream(new FilterInputStream(socket.getInputStream()) {
                    private void check() throws IOException {
                        long left = (until - System.nanoTime()) / 1_000_000;
                        if (left <= 0) throw new SocketTimeoutException("AIO response timed out");
                        socket.setSoTimeout((int) Math.min(getReadTimeout(), left));
                    }
                    @Override public int read() throws IOException { check(); return super.read(); }
                    @Override public int read(byte[] b, int off, int len) throws IOException { check(); return in.read(b, off, len); }
                });
                String status = line(in);
                String[] parts = status.split(" ", 3);
                if (parts.length < 2 || !parts[0].startsWith("HTTP/")) throw new IOException("Invalid AIO response");
                try { responseCode = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { throw new IOException("Invalid AIO response"); }
                int total = status.length();
                String header;
                boolean chunked = false;
                long length = -1;
                while (!(header = line(in)).isEmpty()) {
                    total += header.length();
                    if (total > 16384) throw new IOException("AIO headers too large");
                    int colon = header.indexOf(':');
                    if (colon < 1) throw new IOException("Invalid AIO header");
                    String key = header.substring(0, colon).trim(), value = header.substring(colon + 1).trim();
                    if (key.equalsIgnoreCase("Transfer-Encoding")) {
                        if (!value.equalsIgnoreCase("chunked")) throw new IOException("Unsupported transfer encoding");
                        chunked = true;
                    }
                    if (key.equalsIgnoreCase("Content-Length")) {
                        try { length = Long.parseLong(value); } catch (NumberFormatException e) { throw new IOException("Invalid response length"); }
                    }
                    if (key.equalsIgnoreCase("Content-Encoding") && !value.equalsIgnoreCase("identity")) throw new IOException("Unsupported content encoding");
                }
                ByteArrayOutputStream body = new ByteArrayOutputStream();
                if (chunked) {
                    for (;;) {
                        String size = line(in).split(";", 2)[0];
                        long n;
                        try { n = Long.parseLong(size, 16); } catch (NumberFormatException e) { throw new IOException("Invalid chunk"); }
                        if (n < 0) throw new IOException("Invalid chunk");
                        if (n == 0) break;
                        copy(in, body, n);
                        if (!line(in).isEmpty()) throw new IOException("Invalid chunk ending");
                    }
                } else copy(in, body, length);
                response = body.toByteArray();
                connected = true;
            } finally { if (socket != null) socket.close(); }
        }
        private static String line(InputStream in) throws IOException {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            for (int n; (n = in.read()) >= 0;) {
                if (n == '\n') return b.toString("ISO-8859-1").replace("\r", "");
                if (b.size() >= 8192) throw new IOException("Response line too large");
                b.write(n);
            }
            throw new EOFException("Incomplete AIO response");
        }
        private static void copy(InputStream in, ByteArrayOutputStream out, long left) throws IOException {
            if (left < -1) throw new IOException("Invalid response length");
            byte[] buffer = new byte[8192];
            while (left != 0) {
                int n = in.read(buffer, 0, left < 0 ? buffer.length : (int) Math.min(left, buffer.length));
                if (n < 0) { if (left > 0) throw new EOFException("Incomplete AIO response"); else break; }
                if (out.size() + n > 12 * 1024 * 1024) throw new IOException("AIO response too large");
                out.write(buffer, 0, n);
                if (left > 0) left -= n;
            }
        }
        @Override public int getResponseCode() throws IOException { connect(); return responseCode; }
        @Override public InputStream getInputStream() throws IOException { connect(); if (responseCode >= 400) throw new IOException("HTTP " + responseCode); return new ByteArrayInputStream(response); }
        @Override public InputStream getErrorStream() { return response != null && responseCode >= 400 ? new ByteArrayInputStream(response) : null; }
        @Override public void disconnect() { try { if (socket != null) socket.close(); } catch (IOException ignored) {} }
        @Override public boolean usingProxy() { return false; }
    }
}
