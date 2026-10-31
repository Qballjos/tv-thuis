package io.freemytv;

import android.service.dreams.DreamService;
import android.view.KeyEvent;

/**
 * The Free My TV screensaver (enabled in Free My TV mode via `settings put secure screensaver_components`). The system
 * still decides when it starts and when the TV turns off (e.g. after 10 min idle, then 2 h later), as before: the app
 * doesn't change those timings. ← and → show the previous and next photo. The button chosen in Screensaver settings
 * requests or opens the title (Watch from the screensaver); any other key closes it.
 */
public class Screensaver extends DreamService {
    private DreamView view;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Watchdog.install(this);
        Ui.init(this);
        setInteractive(true);   // ← → stay here, instead of waking the TV
        setFullscreen(true);
        setScreenBright(true);
        view = new DreamView(this);
        setContentView(view);
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN
                || code == KeyEvent.KEYCODE_VOLUME_MUTE) return super.dispatchKeyEvent(event);
        boolean request = Prefs.saverRequest(code);
        boolean left = code == KeyEvent.KEYCODE_DPAD_LEFT && !request;
        boolean right = code == KeyEvent.KEYCODE_DPAD_RIGHT && !request;
        if (left || right) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0 && view != null)
                view.step(left ? -1 : 1);
            return true;
        }
        // The request button acts when it is released, so the app it opens doesn't get the release.
        if (request) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (Prefs.saverWatch() && view != null) view.watch();
                finish();
            }
            return true;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) finish();
        return true;
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        if (view != null) view.start();
    }

    @Override public void onDreamingStopped() {
        if (view != null) view.stop();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        if (view != null) view.stop();
        super.onDetachedFromWindow();
    }
}
