package io.freemytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

/** Fork-owned AIO settings state, dialogs and background actions. The host owns navigation and rendering. */
final class AioSettings {
    interface Host {
        void pair();
        void firstRow();
        void invalidate();
    }

    private final Activity activity;
    private final Handler handler;
    private final Host host;
    private String aioStatus = "";
    private boolean aioBusy;

    AioSettings(Activity activity, Handler handler, Host host) {
        this.activity = activity;
        this.handler = handler;
        this.host = host;
    }

    String[] labels() {
        if (Prefs.aioPaired()) return new String[] {Ui.tr("Connect with AIO"), Ui.tr("Search and request"),
                Ui.tr("Test connection"), Ui.tr("Disconnect AIO")};
        return new String[] {Ui.tr("Connect with AIO"), Ui.tr("AIO server"), Ui.tr("Username"), Ui.tr("Password"),
                Ui.tr("Jellyfin server"), Ui.tr("Jellyfin API key"),
                Ui.tr("Search and request"), Ui.tr("Test connection")};
    }

    String[] values() {
        String host = Prefs.aioUrl();
        String status = aioBusy ? Ui.tr("Checking…") : (aioStatus.isEmpty() ? "›" : aioStatus);
        if (Prefs.aioPaired()) return new String[] {host, "›", status, "›"};
        return new String[] {
                "›",
                host.isEmpty() ? Ui.tr("Not set") : host,
                Prefs.aioUser().isEmpty() ? Ui.tr("Not set") : Prefs.aioUser(),
                Prefs.aioPasswordSet() ? "••••" : Ui.tr("Not set"),
                Prefs.jellyfinUrl().isEmpty() ? Ui.tr("Not set") : Prefs.jellyfinUrl(),
                Prefs.jellyfinKeySet() ? "••••" : Ui.tr("Not set"),
                "›",
                aioBusy ? Ui.tr("Checking…") : (aioStatus.isEmpty() ? "›" : aioStatus)
        };
    }

    void activate(int focus) {
        if (aioBusy) return;
        if (focus == 0) { host.pair(); return; }
        if (Prefs.aioPaired()) {
            if (focus == 1) searchAio();
            else if (focus == 2) testAio();
            else {
                aioBusy = true;
                new Thread(() -> {
                    String why = Aio.unpair();
                    handler.post(() -> {
                        aioBusy = false;
                        aioStatus = why == null ? Ui.tr("Disconnected") : why;
                        host.firstRow();
                        host.invalidate();
                    });
                }, "aio-unpair").start();
            }
            return;
        }
        switch (focus - 1) {
            case 0: editAio(0); break;
            case 1: editAio(1); break;
            case 2: editAio(2); break;
            case 3: editAio(3); break;
            case 4: editAio(4); break;
            case 5: searchAio(); break;
            default: testAio();
        }
    }

    private void editAio(int which) {
        EditText field = new EditText(activity);
        field.setSingleLine(true);
        String title;
        if (which == 0) {
            title = Ui.tr("AIO server");
            field.setText(Prefs.aioUrl());
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        } else if (which == 1) {
            title = Ui.tr("Username");
            field.setText(Prefs.aioUser());
            field.setInputType(InputType.TYPE_CLASS_TEXT);
        } else if (which == 2) {
            title = Ui.tr("Password");
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else if (which == 3) {
            title = Ui.tr("Jellyfin server");
            field.setText(Prefs.jellyfinUrl());
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        } else {
            title = Ui.tr("Jellyfin API key");
            field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        field.setImeOptions(EditorInfo.IME_ACTION_DONE);
        AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setView(field)
                .setPositiveButton(Ui.tr("Save"), (d, w) -> saveAio(which, field.getText().toString()))
                .setNegativeButton(Ui.tr("Cancel"), null)
                .create();
        field.setOnEditorActionListener((v, action, event) -> {
            dialog.dismiss();
            saveAio(which, field.getText().toString());
            return true;
        });
        dialog.show();
    }

    private void saveAio(int which, String value) {
        if (which == 0) Prefs.setAioUrl(value);
        else if (which == 1) Prefs.setAioUser(value);
        else if (which == 2) Prefs.setAioPassword(value);
        else if (which == 3) Prefs.setJellyfinUrl(value);
        else Prefs.setJellyfinKey(value);
        aioStatus = "";
        Recs.get(activity).refreshIfStale();
        host.invalidate();
    }

    private void testAio() {
        if (aioBusy) return;
        aioBusy = true;
        aioStatus = "";
        host.invalidate();
        new Thread(() -> {
            String msg = Aio.test();
            handler.post(() -> {
                aioBusy = false;
                aioStatus = msg;
                Recs.get(activity).refreshIfStale();
                host.invalidate();
            });
        }, "aio-test").start();
    }

    private void searchAio() {
        EditText field = new EditText(activity);
        field.setSingleLine(true);
        field.setHint(Ui.tr("Type a title"));
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        AlertDialog dialog = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(Ui.tr("Search and request"))
                .setView(field)
                .setPositiveButton(Ui.tr("Search"), (d, w) -> runAioSearch(field.getText().toString()))
                .setNegativeButton(Ui.tr("Cancel"), null)
                .create();
        field.setOnEditorActionListener((v, action, event) -> {
            dialog.dismiss();
            runAioSearch(field.getText().toString());
            return true;
        });
        dialog.show();
    }

    private void runAioSearch(String query) {
        aioBusy = true;
        host.invalidate();
        new Thread(() -> {
            try {
                java.util.List<Aio.Hit> hits = Aio.search(query);
                handler.post(() -> {
                    aioBusy = false;
                    host.invalidate();
                    if (hits.isEmpty()) {
                        Toast.makeText(activity, Ui.tr("No titles found."), Toast.LENGTH_LONG).show();
                        return;
                    }
                    String[] labels = new String[hits.size()];
                    for (int i = 0; i < hits.size(); i++) labels[i] = hits.get(i).title;
                    new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                            .setTitle(Ui.tr("Search and request"))
                            .setItems(labels, (d, which) -> requestHit(hits.get(which)))
                            .setNegativeButton(Ui.tr("Cancel"), null)
                            .show();
                });
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                handler.post(() -> {
                    aioBusy = false;
                    aioStatus = msg;
                    host.invalidate();
                });
            }
        }, "aio-search").start();
    }

    private void requestHit(Aio.Hit hit) {
        if (!hit.canRequest) {
            Toast.makeText(activity, Ui.tr("Already in your library."), Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            String err = Aio.request(hit.kind, hit.id);
            handler.post(() -> Toast.makeText(activity,
                    err == null ? Ui.tr("Requested. It will show up in Recently added.") : err,
                    Toast.LENGTH_LONG).show());
        }, "aio-request").start();
    }

}
