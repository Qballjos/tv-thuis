package io.freemytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** One profile overview and editor, reachable with a TV remote or TalkBack. */
public final class ProfileManagerActivity extends Activity {
    private boolean managing;

    static void open(Activity activity) {
        activity.startActivity(new Intent(activity, ProfileManagerActivity.class));
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Watchdog.install(this);
        Ui.init(this);
        managing = state != null && state.getBoolean("managing", false);
        render(Prefs.profiles().active());
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("managing", managing);
        super.onSaveInstanceState(state);
    }

    private int dp(float size) { return Math.round(size * getResources().getDisplayMetrics().density); }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Ui.medium);
        return view;
    }

    private GradientDrawable background(int color, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(14));
        if (stroke != 0) drawable.setStroke(dp(2), stroke);
        return drawable;
    }

    private StateListDrawable tileBackground(boolean active) {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[] {android.R.attr.state_focused}, background(0xFF2A3550, Color.WHITE));
        drawable.addState(new int[0], background(0xFF182137, active ? 0xFF84B6FF : 0));
        return drawable;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setId(View.generateViewId());
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setBackground(tileBackground(false));
        button.setPadding(dp(20), 0, dp(20), 0);
        return button;
    }

    private LinearLayout card(String avatar, String label, boolean active, boolean showActive) {
        LinearLayout card = new LinearLayout(this);
        card.setId(View.generateViewId());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        card.setBackground(tileBackground(active));
        card.setFocusable(true);
        card.setClickable(true);
        card.setContentDescription(label + (active && showActive ? ", " + Ui.tr("Active profile") : ""));
        if (avatar == null) {
            TextView add = text("+", 52, 0xFFA8CFFF);
            card.addView(add, new LinearLayout.LayoutParams(dp(94), dp(94)));
        } else {
            ImageView image = new ImageView(this);
            image.setImageBitmap(ProfileAvatars.image(avatar));
            card.addView(image, new LinearLayout.LayoutParams(dp(94), dp(94)));
        }
        TextView name = text(label, 17, Color.WHITE);
        name.setMaxLines(1);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setAutoSizeTextTypeUniformWithConfiguration(12, 17, 1, TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams nameLayout = new LinearLayout.LayoutParams(-1, dp(28));
        nameLayout.topMargin = dp(8);
        card.addView(name, nameLayout);
        if (showActive) card.addView(text(active ? Ui.tr("Active profile") : "", 12, 0xFFA8CFFF),
                new LinearLayout.LayoutParams(-1, dp(18)));
        return card;
    }

    private void render(String focusId) {
        Profiles profiles = Prefs.profiles();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(20));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF19223A, 0xFF0A0E19}));
        root.addView(text(managing ? Ui.tr("Manage profiles") : Ui.tr("Who's watching?"), 30, Color.WHITE),
                new LinearLayout.LayoutParams(-1, dp(45)));
        root.addView(text(managing ? Ui.tr("Choose a profile to edit its name, avatar and apps.") : Ui.tr("Choose a profile."),
                15, 0xFFB8C2D8), new LinearLayout.LayoutParams(-1, dp(34)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollLayout = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollLayout.topMargin = dp(12);
        scrollLayout.bottomMargin = dp(12);
        root.addView(scroll, scrollLayout);
        LinearLayout centered = new LinearLayout(this);
        centered.setGravity(Gravity.CENTER);
        scroll.addView(centered, new ScrollView.LayoutParams(-1, -1));
        GridLayout grid = new GridLayout(this);
        int columns = Math.min(4, profiles.ids().size() + (profiles.ids().size() < Profiles.MAX ? 1 : 0));
        grid.setColumnCount(columns);
        centered.addView(grid, new LinearLayout.LayoutParams(-2, -2));
        List<View> cards = new ArrayList<>();
        View focus = null;
        for (String id : profiles.ids()) {
            LinearLayout card = card(profiles.avatar(id), profiles.name(id), id.equals(profiles.active()), true);
            card.setOnClickListener(v -> { if (managing) edit(id); else select(id); });
            GridLayout.LayoutParams layout = new GridLayout.LayoutParams();
            layout.width = dp(138); layout.height = dp(174); layout.setMargins(dp(6), dp(6), dp(6), dp(6));
            grid.addView(card, layout);
            cards.add(card);
            if (id.equals(focusId)) focus = card;
        }
        if (cards.size() < Profiles.MAX) {
            LinearLayout add = card(null, Ui.tr("Add profile"), false, true);
            add.setOnClickListener(v -> edit(null));
            GridLayout.LayoutParams layout = new GridLayout.LayoutParams();
            layout.width = dp(138); layout.height = dp(174); layout.setMargins(dp(6), dp(6), dp(6), dp(6));
            grid.addView(add, layout);
            cards.add(add);
        }
        Button manage = button(managing ? Ui.tr("Done") : Ui.tr("Manage profiles"));
        manage.setOnClickListener(v -> { managing = !managing; render(profiles.active()); });
        root.addView(manage, new LinearLayout.LayoutParams(-2, dp(44)));
        for (int i = 0; i < cards.size(); i++) {
            View item = cards.get(i);
            item.setNextFocusLeftId(i % columns > 0 ? cards.get(i - 1).getId() : item.getId());
            item.setNextFocusRightId(i % columns < columns - 1 && i + 1 < cards.size() ? cards.get(i + 1).getId() : item.getId());
            item.setNextFocusUpId(i >= columns ? cards.get(i - columns).getId() : item.getId());
            item.setNextFocusDownId(i + columns < cards.size() ? cards.get(i + columns).getId() : manage.getId());
        }
        if (focus == null) focus = cards.get(0);
        manage.setNextFocusUpId(focus.getId());
        setContentView(root);
        View initialFocus = focus;
        root.post(initialFocus::requestFocusFromTouch);
    }

    private void select(String id) {
        if (!Prefs.profiles().select(id)) { failed(); return; }
        changed();
        startActivity(new Intent(this, HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    private void edit(String id) {
        Profiles profiles = Prefs.profiles();
        boolean adding = id == null;
        String[] avatar = {adding ? Profiles.AVATARS.get(profiles.ids().size() % Profiles.AVATARS.size()) : profiles.avatar(id)};
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setGravity(Gravity.CENTER_HORIZONTAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(8));
        LinearLayout portrait = card(avatar[0], Ui.tr("Choose avatar"), false, false);
        LinearLayout identity = new LinearLayout(this);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        identity.addView(portrait, new LinearLayout.LayoutParams(dp(150), dp(154)));
        form.addView(identity, new LinearLayout.LayoutParams(-1, -2));
        portrait.setOnClickListener(v -> chooseAvatar(avatar[0], selected -> {
            avatar[0] = selected;
            ((ImageView) portrait.getChildAt(0)).setImageBitmap(ProfileAvatars.image(selected));
        }));
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint(Ui.tr("Profile name"));
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(32)});
        if (!adding) name.setText(profiles.name(id));
        LinearLayout.LayoutParams nameLayout = new LinearLayout.LayoutParams(0, dp(54), 1);
        nameLayout.leftMargin = dp(16);
        identity.addView(name, nameLayout);
        Button remove = null;
        if (!adding) {
            Button apps = button(Ui.tr("Apps in this profile"));
            apps.setOnClickListener(v -> apps(id));
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, dp(44));
            layout.topMargin = dp(12);
            form.addView(apps, layout);
            remove = button(Ui.tr("Delete profile"));
            remove.setEnabled(profiles.ids().size() > 1);
            remove.setAlpha(remove.isEnabled() ? 1 : 0.4f);
            LinearLayout.LayoutParams removeLayout = new LinearLayout.LayoutParams(-1, dp(44));
            removeLayout.topMargin = dp(8);
            form.addView(remove, removeLayout);
        }
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(adding ? Ui.tr("Add profile") : Ui.tr("Edit profile"))
                .setMessage(adding ? Ui.tr("Starts with a copy of the current apps and settings.") : null)
                .setView(form).setPositiveButton(Ui.tr("Save"), null).setNegativeButton(Ui.tr("Cancel"), null).create();
        if (remove != null) remove.setOnClickListener(v -> confirmDelete(id, dialog));
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    String saved = adding ? profiles.add(name.getText().toString(), avatar[0]) : id;
                    if (saved == null || (!adding && !profiles.update(id, name.getText().toString(), avatar[0]))) { failed(); return; }
                    changed(); dialog.dismiss(); render(saved);
                } catch (IllegalArgumentException error) {
                    name.setError(Ui.tr("Choose a unique name (1–32 characters)."));
                }
            });
            if (adding) name.post(name::requestFocus); else portrait.post(portrait::requestFocusFromTouch);
        });
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(adding
                ? WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE : WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void chooseAvatar(String current, java.util.function.Consumer<String> onPick) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setPadding(dp(12), dp(12), dp(12), dp(12));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(grid, new ScrollView.LayoutParams(-1, -2));
        LinearLayout content = new LinearLayout(this);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, dp(348)));
        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(Ui.tr("Choose avatar")).setView(content).setNegativeButton(Ui.tr("Cancel"), null).create();
        List<View> cards = new ArrayList<>();
        View focus = null;
        for (String avatar : Profiles.AVATARS) {
            LinearLayout card = card(avatar, ProfileAvatars.label(avatar), avatar.equals(current), false);
            GridLayout.LayoutParams layout = new GridLayout.LayoutParams();
            layout.width = dp(110); layout.height = dp(154); layout.setMargins(dp(4), dp(4), dp(4), dp(4));
            grid.addView(card, layout);
            cards.add(card);
            card.setOnClickListener(v -> { onPick.accept(avatar); dialog.dismiss(); });
            if (avatar.equals(current)) focus = card;
        }
        View initialFocus = focus;
        dialog.setOnShowListener(d -> {
            Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            for (int i = 0; i < cards.size(); i++) {
                View card = cards.get(i);
                card.setNextFocusLeftId(i % 4 > 0 ? cards.get(i - 1).getId() : card.getId());
                card.setNextFocusRightId(i % 4 < 3 && i + 1 < cards.size() ? cards.get(i + 1).getId() : card.getId());
                card.setNextFocusUpId(i >= 4 ? cards.get(i - 4).getId() : card.getId());
                card.setNextFocusDownId(i + 4 < cards.size() ? cards.get(i + 4).getId() : cancel.getId());
                if (i + 4 >= cards.size()) card.setOnKeyListener((v, key, event) -> {
                    // ScrollView keeps arrow focus inside itself; a column's end must reach Cancel.
                    if (key != KeyEvent.KEYCODE_DPAD_DOWN || event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    return cancel.requestFocus();
                });
            }
            int lastRowStart = (cards.size() - 1) / 4 * 4;
            cancel.setNextFocusUpId(cards.get(lastRowStart).getId());
            if (initialFocus != null) grid.post(initialFocus::requestFocusFromTouch);
        });
        dialog.show();
    }

    private void apps(String id) {
        List<String[]> apps = HomeApps.shelfCandidates(this, true);
        String[] labels = new String[apps.size()];
        boolean[] checked = new boolean[apps.size()];
        Set<String> hidden = new HashSet<>(Prefs.profiles().settings(id).getStringSet("hiddenApps", java.util.Collections.emptySet()));
        for (int i = 0; i < apps.size(); i++) {
            labels[i] = apps.get(i)[1]; checked[i] = !hidden.contains(apps.get(i)[0]);
        }
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle(Ui.tr("Apps in this profile"))
                .setMultiChoiceItems(labels, checked, (d, i, on) -> {
                    if (on) hidden.remove(apps.get(i)[0]); else hidden.add(apps.get(i)[0]);
                }).setPositiveButton(Ui.tr("Save"), (d, i) -> {
                    if (Prefs.profiles().settings(id).edit().putStringSet("hiddenApps", hidden).commit()) changed(); else failed();
                }).setNegativeButton(Ui.tr("Cancel"), null).show();
    }

    private void confirmDelete(String id, AlertDialog editor) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle(Ui.tr("Delete profile"))
                .setMessage(Ui.trf("Delete {name} and its Free My TV settings?", "name", Prefs.profiles().name(id)))
                .setPositiveButton(Ui.tr("Delete profile"), (d, i) -> {
                    if (!Prefs.profiles().remove(id)) { failed(); return; }
                    editor.dismiss(); changed(); render(Prefs.profiles().active());
                }).setNegativeButton(Ui.tr("Cancel"), null).show();
    }

    private void changed() {
        Prefs.profileChanged();
        HomeData.get(this).settingsChanged();
    }

    private void failed() { Toast.makeText(this, Ui.tr("Couldn't save the profile."), Toast.LENGTH_LONG).show(); }
}
