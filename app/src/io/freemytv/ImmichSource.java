package io.freemytv;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** One immutable selection owns its request, cached photos and refresh stamp, even during an album switch. */
final class ImmichSource {
    static final String INBOX_PREFIX = "immich-", PHOTO_END = "-immich.jpg";
    final String server, mode, album, key;
    final boolean enabled;
    private final String suffix;

    ImmichSource(String server, String mode, String album) {
        this.server = server.trim().replaceAll("/+$", "");
        this.mode = mode;
        this.album = "album".equals(mode) ? album : "";
        enabled = "immich".equals(mode) || ("album".equals(mode)
                && album.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"));
        key = digest(this.server + "\n" + mode + "\n" + this.album);
        suffix = "-" + key + PHOTO_END;
    }

    boolean matches(String mode, String album) {
        return this.mode.equals(mode) && (!"album".equals(mode) || this.album.equals(album));
    }

    boolean visible(File file) {
        String name = file.getName();
        if (name.endsWith(PHOTO_END)) return enabled && name.endsWith(suffix);
        return !"album".equals(mode);   // keep manual photos on disk, but an album shows only its own photos
    }

    File stamp(File files) { return new File(files, "immich-sync-" + key); }
    String inboxName(String asset) { return INBOX_PREFIX + key + "-" + asset + ".jpg"; }

    /** Untagged copies from older builds stay on disk until space is needed, but never enter a selected album. */
    static String importedSuffix(String original) {
        if (!original.startsWith(INBOX_PREFIX)) return ".jpg";
        int start = INBOX_PREFIX.length(), end = start + 64;
        String tag = original.length() > end && original.charAt(end) == '-'
                ? original.substring(start, end) : "";
        return (tag.matches("[0-9a-f]{64}") ? "-" + tag : "") + PHOTO_END;
    }

    private static String digest(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : bytes) hex.append(Character.forDigit((b >>> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);   // SHA-256 is required by the Java runtime
        }
    }
}
