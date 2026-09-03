package com.bodiug.dreamingspanish;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

class SettingsOverlay {
    interface Listener {
        void onNavigationModeChanged(boolean mouseMode);

        void onAutoplayChanged(boolean enabled);

        void onAutoFullscreenChanged(boolean enabled);

        void onRefreshRequested();

        void onResetRequested();
    }

    private static final int NAVIGATION_INDEX = 0;
    private static final int AUTOPLAY_INDEX = 1;
    private static final int AUTO_FULLSCREEN_INDEX = 2;
    private static final int REFRESH_INDEX = 3;
    private static final int RESET_INDEX = 4;

    private final Context context;
    private final FrameLayout root;
    private final Listener listener;
    private final List<TextView> items = new ArrayList<>();

    private FrameLayout overlay;
    private int selectedIndex;
    private boolean mouseMode;
    private boolean autoplayEnabled;
    private boolean autoFullscreenEnabled;

    SettingsOverlay(Context context, FrameLayout root, Listener listener) {
        this.context = context;
        this.root = root;
        this.listener = listener;
    }

    boolean isOpen() {
        return overlay != null && overlay.getParent() != null;
    }

    void show(boolean mouseMode, boolean autoplayEnabled, boolean autoFullscreenEnabled) {
        if (isOpen()) {
            return;
        }
        this.mouseMode = mouseMode;
        this.autoplayEnabled = autoplayEnabled;
        this.autoFullscreenEnabled = autoFullscreenEnabled;
        items.clear();
        selectedIndex = 0;

        overlay = new FrameLayout(context);
        overlay.setBackgroundColor(Color.argb(190, 0, 0, 0));
        overlay.setFocusable(true);
        overlay.setFocusableInTouchMode(true);

        LinearLayout panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dpToPx(28), dpToPx(24), dpToPx(28), dpToPx(24));
        panel.setBackgroundColor(Color.rgb(24, 28, 34));

        TextView title = new TextView(context);
        title.setText("Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setPadding(0, 0, 0, dpToPx(18));
        panel.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addItem(panel, navigationModeLabel());
        addItem(panel, autoplayLabel());
        addItem(panel, autoFullscreenLabel());
        addItem(panel, "Refresh page");
        addItem(panel, "Reset session");
        addItem(panel, "Close");
        updateSelection();

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dpToPx(420),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.addView(panel, params);
        overlay.requestFocus();
    }

    void hide() {
        if (overlay != null) {
            root.removeView(overlay);
            overlay = null;
        }
    }

    boolean handleKey(int keyCode, boolean isConfirmKey) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            moveSelection(-1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            moveSelection(1);
            return true;
        } else if (isConfirmKey
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            activate();
            return true;
        }
        return false;
    }

    private void addItem(LinearLayout panel, String text) {
        TextView item = new TextView(context);
        item.setText(text);
        item.setTextSize(20);
        item.setTextColor(Color.WHITE);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dpToPx(18), dpToPx(14), dpToPx(18), dpToPx(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(58));
        params.setMargins(0, 0, 0, dpToPx(8));
        panel.addView(item, params);
        items.add(item);
    }

    private String navigationModeLabel() {
        return "Navigation: " + (mouseMode ? "mouse" : "selection");
    }

    private String autoplayLabel() {
        return "Website autoplay: " + (autoplayEnabled ? "on" : "off");
    }

    private String autoFullscreenLabel() {
        return "Auto fullscreen: " + (autoFullscreenEnabled ? "on" : "off");
    }

    private void moveSelection(int direction) {
        if (items.isEmpty()) {
            return;
        }
        selectedIndex = Math.max(0, Math.min(items.size() - 1, selectedIndex + direction));
        updateSelection();
    }

    private void updateSelection() {
        for (int i = 0; i < items.size(); i++) {
            TextView item = items.get(i);
            boolean selected = i == selectedIndex;
            item.setTextColor(selected ? Color.BLACK : Color.WHITE);
            item.setBackgroundColor(selected ? Color.rgb(0, 208, 132) : Color.rgb(42, 47, 55));
        }
    }

    private void activate() {
        if (selectedIndex == NAVIGATION_INDEX) {
            mouseMode = !mouseMode;
            items.get(NAVIGATION_INDEX).setText(navigationModeLabel());
            updateSelection();
            listener.onNavigationModeChanged(mouseMode);
        } else if (selectedIndex == AUTOPLAY_INDEX) {
            autoplayEnabled = !autoplayEnabled;
            items.get(AUTOPLAY_INDEX).setText(autoplayLabel());
            updateSelection();
            listener.onAutoplayChanged(autoplayEnabled);
        } else if (selectedIndex == AUTO_FULLSCREEN_INDEX) {
            autoFullscreenEnabled = !autoFullscreenEnabled;
            items.get(AUTO_FULLSCREEN_INDEX).setText(autoFullscreenLabel());
            updateSelection();
            listener.onAutoFullscreenChanged(autoFullscreenEnabled);
        } else if (selectedIndex == REFRESH_INDEX) {
            hide();
            listener.onRefreshRequested();
        } else if (selectedIndex == RESET_INDEX) {
            listener.onResetRequested();
            hide();
        } else {
            hide();
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
