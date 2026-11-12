package android.content;
import java.io.File;
/** Minimal filesystem boundary for host-side Recent tests. */
public final class Context {
    private final File dir;
    public Context(File dir) { this.dir = dir; }
    public Context getApplicationContext() { return this; }
    public File getFilesDir() { return dir; }
}
