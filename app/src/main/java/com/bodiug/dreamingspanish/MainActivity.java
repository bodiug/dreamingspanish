package com.bodiug.dreamingspanish;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebStorage;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String START_URL =
            "https://app.dreaming.com/spanish/browse?sort=easy&hide-watched=true";
    private static final int DESKTOP_VIEWPORT_WIDTH = 1280;
    private static final int SCROLL_STEP_DP = 170;
    private static final int MOUSE_STEP_DP = 34;
    private static final long CURSOR_IDLE_TIMEOUT_MS = 3000;
    private static final String PREFS_NAME = "dreaming_spanish_tv";
    private static final String PREF_WEBSITE_AUTOPLAY = "website_autoplay";
    private static final String PREF_AUTO_FULLSCREEN = "auto_fullscreen";
    private static final int SETTINGS_NAVIGATION_INDEX = 0;
    private static final int SETTINGS_AUTOPLAY_INDEX = 1;
    private static final int SETTINGS_AUTO_FULLSCREEN_INDEX = 2;
    private static final int SETTINGS_REFRESH_INDEX = 3;
    private static final int SETTINGS_RESET_INDEX = 4;
    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 DreamingSpanishTV/1.0";

    private WebView webView;
    private FrameLayout root;
    private CursorView cursorView;
    private FrameLayout settingsOverlay;
    private final List<TextView> settingsItems = new ArrayList<>();
    private View fullScreenView;
    private WebChromeClient.CustomViewCallback fullScreenCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean mouseMode;
    private boolean autoplayEnabled = true;
    private boolean autoFullscreenEnabled = true;
    private boolean backLongPressTriggered;
    private boolean pendingAutoFullscreen;
    private boolean tvFullscreenMode;
    private int autoFullscreenAttempts;
    private int settingsIndex;
    private SharedPreferences preferences;
    private final Runnable showSettingsRunnable = () -> {
        backLongPressTriggered = true;
        showSettingsOverlay();
    };
    private final Runnable hideCursorRunnable = () -> {
        if (cursorView != null && fullScreenView == null && !isSettingsOpen()) {
            cursorView.setVisibility(View.INVISIBLE);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        autoplayEnabled = preferences.getBoolean(PREF_WEBSITE_AUTOPLAY, true);
        autoFullscreenEnabled = preferences.getBoolean(PREF_AUTO_FULLSCREEN, true);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        webView.setVisibility(View.INVISIBLE);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus();

        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        cursorView = new CursorView(this);
        cursorView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(cursorView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        root.post(() -> cursorView.centerInParent());
        root.post(this::showCursorTemporarily);

        configureWebView();
        hideSystemUi();

        if (savedInstanceState == null) {
            webView.loadUrl(START_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        webView.onResume();
        webView.requestFocus();
    }

    @Override
    protected void onPause() {
        CookieManager.getInstance().flush();
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            root.removeView(webView);
            webView.destroy();
        }
        handler.removeCallbacks(showSettingsRunnable);
        handler.removeCallbacks(hideCursorRunnable);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (fullScreenView != null) {
            exitFullScreenVideo();
        } else if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fullScreenView != null) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    exitFullScreenVideo();
                }
                return true;
            }
            if (tvFullscreenMode) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    closeOpenPopupOrHideControlsOrExitTvFullscreen();
                }
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    backLongPressTriggered = false;
                    handler.postDelayed(showSettingsRunnable, ViewConfiguration.getLongPressTimeout());
                }
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(showSettingsRunnable);
                if (!backLongPressTriggered) {
                    if (isSettingsOpen()) {
                        hideSettingsOverlay();
                    } else {
                        closeOpenPopup();
                    }
                }
                return true;
            }
        }

        if (fullScreenView != null) {
            return super.dispatchKeyEvent(event);
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event);
        }
        showCursorTemporarily();

        if (isSettingsOpen()) {
            return handleSettingsKey(keyCode);
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            toggleVideoPlayback();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            setVideoPlayback(true);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            setVideoPlayback(false);
            return true;
        }

        if (mouseMode) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                moveMouse(-1, 0);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                moveMouse(1, 0);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                moveMouse(0, -1);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                moveMouse(0, 1);
                return true;
            } else if (isConfirmKey(keyCode)) {
                focusElementUnderCursor();
                clickElementUnderCursor();
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            navigateToElement("left");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            navigateToElement("right");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            navigateToElement("up");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            navigateToElement("down");
            return true;
        } else if (isConfirmKey(keyCode)) {
            clickSelectedElement();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN
                || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN
                || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            scrollPage(1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_PAGE_UP
                || keyCode == KeyEvent.KEYCODE_CHANNEL_UP
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            scrollPage(-1);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private boolean isSettingsOpen() {
        return settingsOverlay != null && settingsOverlay.getParent() != null;
    }

    private boolean isConfirmKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                || keyCode == KeyEvent.KEYCODE_BUTTON_A
                || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT
                || keyCode == KeyEvent.KEYCODE_SPACE;
    }

    private boolean handleSettingsKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            moveSettingsSelection(-1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            moveSettingsSelection(1);
            return true;
        } else if (isConfirmKey(keyCode)
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            activateSettingsItem();
            return true;
        }
        return false;
    }

    private void showSettingsOverlay() {
        if (isSettingsOpen()) {
            return;
        }
        settingsItems.clear();
        settingsIndex = 0;

        settingsOverlay = new FrameLayout(this);
        settingsOverlay.setBackgroundColor(Color.argb(190, 0, 0, 0));
        settingsOverlay.setFocusable(true);
        settingsOverlay.setFocusableInTouchMode(true);
        cursorView.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(hideCursorRunnable);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dpToPx(28), dpToPx(24), dpToPx(28), dpToPx(24));
        panel.setBackgroundColor(Color.rgb(24, 28, 34));

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setPadding(0, 0, 0, dpToPx(18));
        panel.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        addSettingsItem(panel, navigationModeLabel());
        addSettingsItem(panel, autoplayLabel());
        addSettingsItem(panel, autoFullscreenLabel());
        addSettingsItem(panel, "Refresh page");
        addSettingsItem(panel, "Reset session");
        addSettingsItem(panel, "Close");
        updateSettingsSelection();

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dpToPx(420),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        root.addView(settingsOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        settingsOverlay.addView(panel, params);
        settingsOverlay.requestFocus();
        hideSystemUi();
    }

    private void addSettingsItem(LinearLayout panel, String text) {
        TextView item = new TextView(this);
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
        settingsItems.add(item);
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

    private void moveSettingsSelection(int direction) {
        if (settingsItems.isEmpty()) {
            return;
        }
        settingsIndex = Math.max(0, Math.min(settingsItems.size() - 1, settingsIndex + direction));
        updateSettingsSelection();
    }

    private void updateSettingsSelection() {
        for (int i = 0; i < settingsItems.size(); i++) {
            TextView item = settingsItems.get(i);
            boolean selected = i == settingsIndex;
            item.setTextColor(selected ? Color.BLACK : Color.WHITE);
            item.setBackgroundColor(selected ? Color.rgb(0, 208, 132) : Color.rgb(42, 47, 55));
        }
    }

    private void activateSettingsItem() {
        if (settingsIndex == SETTINGS_NAVIGATION_INDEX) {
            mouseMode = !mouseMode;
            settingsItems.get(SETTINGS_NAVIGATION_INDEX).setText(navigationModeLabel());
            updateSettingsSelection();
        } else if (settingsIndex == SETTINGS_AUTOPLAY_INDEX) {
            autoplayEnabled = !autoplayEnabled;
            preferences.edit().putBoolean(PREF_WEBSITE_AUTOPLAY, autoplayEnabled).apply();
            injectAutoplaySupport();
            settingsItems.get(SETTINGS_AUTOPLAY_INDEX).setText(autoplayLabel());
            updateSettingsSelection();
        } else if (settingsIndex == SETTINGS_AUTO_FULLSCREEN_INDEX) {
            autoFullscreenEnabled = !autoFullscreenEnabled;
            preferences.edit().putBoolean(PREF_AUTO_FULLSCREEN, autoFullscreenEnabled).apply();
            settingsItems.get(SETTINGS_AUTO_FULLSCREEN_INDEX).setText(autoFullscreenLabel());
            updateSettingsSelection();
        } else if (settingsIndex == SETTINGS_REFRESH_INDEX) {
            hideSettingsOverlay();
            refreshPage();
        } else if (settingsIndex == SETTINGS_RESET_INDEX) {
            resetSession();
            hideSettingsOverlay();
        } else {
            hideSettingsOverlay();
        }
    }

    private void hideSettingsOverlay() {
        if (settingsOverlay != null) {
            root.removeView(settingsOverlay);
            settingsOverlay = null;
        }
        webView.requestFocus();
        hideSystemUi();
        showCursorTemporarily();
    }

    private void resetSession() {
        webView.evaluateJavascript(
                "try{localStorage.clear();sessionStorage.clear();}catch(e){}",
                null);
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();
        WebStorage.getInstance().deleteAllData();
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(value -> {
            cookieManager.flush();
            webView.loadUrl(START_URL);
        });
    }

    private void refreshPage() {
        pendingAutoFullscreen = false;
        autoFullscreenAttempts = 0;
        tvFullscreenMode = false;
        webView.reload();
    }

    private void moveMouse(int dx, int dy) {
        showCursorTemporarily();
        cursorView.moveBy(dx * dpToPx(MOUSE_STEP_DP), dy * dpToPx(MOUSE_STEP_DP));
        scrollIfNearVerticalEdge(dy);
    }

    private void toggleVideoPlayback() {
        webView.evaluateJavascript(
                "(() => {const v=document.querySelector('video');if(!v)return false;"
                        + "if(v.paused){v.play&&v.play();}else{v.pause&&v.pause();}return true;"
                        + "})()||(() => {"
                        + "if(window.__dsTvPlayerAction)return window.__dsTvPlayerAction('play-pause');"
                        + "const iframe=document.querySelector('.ds-youtube-player iframe');"
                        + "if(!iframe||!iframe.contentWindow)return false;"
                        + "window.__dsTvYoutubePaused=!window.__dsTvYoutubePaused;"
                        + "iframe.contentWindow.postMessage(JSON.stringify({event:'command',func:window.__dsTvYoutubePaused?'pauseVideo':'playVideo',args:[]}),'https://www.youtube.com');"
                        + "return true;"
                        + "})();",
                null);
    }

    private void setVideoPlayback(boolean play) {
        webView.evaluateJavascript(
                "(() => {const v=document.querySelector('video');if(!v)return false;"
                        + (play ? "v.play&&v.play();" : "v.pause&&v.pause();")
                        + "return true;"
                        + "})()||(() => {"
                        + "if(window.__dsTvPlayerAction)return window.__dsTvPlayerAction('" + (play ? "play" : "pause") + "');"
                        + "const iframe=document.querySelector('.ds-youtube-player iframe');"
                        + "if(!iframe||!iframe.contentWindow)return false;"
                        + "window.__dsTvYoutubePaused=" + (!play) + ";"
                        + "iframe.contentWindow.postMessage(JSON.stringify({event:'command',func:'" + (play ? "playVideo" : "pauseVideo") + "',args:[]}),'https://www.youtube.com');"
                        + "return true;"
                        + "})();",
                null);
    }

    private WebResourceResponse loadDesktopHtml(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("User-Agent", DESKTOP_USER_AGENT);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
            connection.setRequestProperty("Accept-Encoding", "identity");
            String cookies = CookieManager.getInstance().getCookie("https://app.dreaming.com");
            if (cookies != null && !cookies.isEmpty()) {
                connection.setRequestProperty("Cookie", cookies);
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                return null;
            }
            String html;
            try (InputStream responseStream = stream) {
                html = new String(readFully(responseStream), StandardCharsets.UTF_8);
            }
            String patchedHtml = patchInitialHtml(html);
            return new WebResourceResponse(
                    "text/html",
                    "UTF-8",
                    status,
                    connection.getResponseMessage(),
                    responseHeaders(connection),
                    new ByteArrayInputStream(patchedHtml.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String patchInitialHtml(String html) {
        String viewport = "<meta name=\"viewport\" content=\"width=" + DESKTOP_VIEWPORT_WIDTH
                + ", initial-scale=1, maximum-scale=1, user-scalable=no\">";
        if (html.contains("name=\"viewport\"")) {
            return html.replaceFirst("<meta\\s+name=\"viewport\"[^>]*>", viewport);
        }
        return html.replaceFirst("<head>", "<head>" + viewport);
    }

    private Map<String, String> responseHeaders(HttpURLConnection connection) {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !entry.getValue().isEmpty()) {
                headers.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        headers.remove("Content-Encoding");
        headers.remove("Content-Length");
        return headers;
    }

    private byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setInitialScale(100);
        settings.setTextZoom(100);
        settings.setUserAgentString(DESKTOP_USER_AGENT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (fullScreenView == null) {
                    view.setVisibility(View.INVISIBLE);
                }
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null
                        && request.isForMainFrame()
                        && "GET".equalsIgnoreCase(request.getMethod())
                        && request.getUrl() != null
                        && "app.dreaming.com".equals(request.getUrl().getHost())) {
                    WebResourceResponse response = loadDesktopHtml(request.getUrl().toString());
                    if (response != null) {
                        return response;
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url == null || !url.contains("/watch")) {
                    tvFullscreenMode = false;
                }
                injectTvLayoutSupport(() -> {
                    injectTvFocusSupport();
                    injectAutoplaySupport();
                    navigateToElement("first");
                    if (fullScreenView == null) {
                        view.setVisibility(View.VISIBLE);
                    }
                    if (pendingAutoFullscreen && url != null && url.contains("/watch")) {
                        scheduleAutoFullscreenAttempt(700);
                    }
                });
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullScreenView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                fullScreenView = view;
                fullScreenCallback = callback;
                pendingAutoFullscreen = false;
                autoFullscreenAttempts = 0;
                webView.setVisibility(View.GONE);
                cursorView.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                hideSystemUi();
            }

            @Override
            public void onHideCustomView() {
                exitFullScreenVideo();
            }
        });
    }

    private void injectTvFocusSupport() {
        String script = "(() => {"
                + "const styleId='ds-tv-focus-style';"
                + "if(!document.getElementById(styleId)){"
                + "const s=document.createElement('style');s.id=styleId;"
                + "s.textContent='a:focus,button:focus,[role=\"button\"]:focus,[tabindex]:focus,video:focus{outline:4px solid #00d084!important;outline-offset:4px!important;border-radius:8px!important;}';"
                + "document.head.appendChild(s);}"
                + "const selector='a,button,[role=\"button\"],video';"
                + "document.querySelectorAll(selector).forEach((el)=>{if(!el.hasAttribute('tabindex'))el.setAttribute('tabindex','0');});"
                + "if(!document.activeElement||document.activeElement===document.body){const first=document.querySelector(selector);if(first)first.focus();}"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private void injectTvLayoutSupport(Runnable afterInjected) {
        String script = "(() => {"
                + "let viewport=document.querySelector('meta[name=\"viewport\"]');"
                + "if(!viewport){viewport=document.createElement('meta');viewport.name='viewport';document.head.appendChild(viewport);}"
                + "viewport.setAttribute('content','width=" + DESKTOP_VIEWPORT_WIDTH + ", initial-scale=1, maximum-scale=1, user-scalable=no');"
                + "const styleId='ds-tv-desktop-viewport';"
                + "let s=document.getElementById(styleId);"
                + "if(!s){s=document.createElement('style');s.id=styleId;document.head.appendChild(s);}"
                + "s.textContent='html,body{overflow-x:hidden!important;} body{touch-action:manipulation!important;}'"
                + "+'.ds-watch-page .ds-watch-page__sections{display:block!important;}'"
                + "+'.ds-watch-page .ds-video-section{width:100%!important;max-width:100%!important;}'"
                + "+'.ds-watch-page .ds-video-section__card-video{width:100%!important;max-width:100%!important;}'"
                + "+'.ds-watch-page .ds-video-section__embed,.ds-watch-page .embed-responsive,.ds-watch-page .ds-shaka-player,.ds-watch-page video{width:100%!important;max-width:100%!important;}'"
                + "+'.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-controls-container,.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-controls-container *,.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-bottom-controls,.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-controls-button-panel{opacity:1!important;visibility:visible!important;}'"
                + "+'.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-bottom-controls{background:linear-gradient(transparent,rgba(0,0,0,.82))!important;}'"
                + "+'.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .ds-video-settings-menu{opacity:1!important;visibility:visible!important;background:rgba(12,16,22,.96)!important;}'"
                + "+'.ds-watch-page .ds-youtube-player{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;background:#000!important;}'"
                + "+'.ds-watch-page .embed-responsive.__ds-tv-has-player .ds-video-section__default{display:none!important;}'"
                + "+'.ds-watch-page .ds-tv-player-controls{position:absolute!important;right:16px!important;top:16px!important;z-index:2147483002!important;display:flex!important;gap:8px!important;opacity:0!important;transition:opacity .12s linear!important;}'"
                + "+'.ds-watch-page .ds-tv-player-controls.__ds-tv-reveal,.ds-watch-page .ds-tv-player-controls.__ds-tv-selection-active{opacity:1!important;}'"
                + "+'.ds-watch-page .ds-tv-player-controls button,.ds-watch-page .ds-tv-player-quality-menu button{appearance:none!important;border:0!important;border-radius:6px!important;background:rgba(12,16,22,.92)!important;color:white!important;font:600 14px system-ui,sans-serif!important;min-width:44px!important;height:36px!important;padding:0 12px!important;}'"
                + "+'.ds-watch-page .ds-tv-player-quality-menu{position:absolute!important;right:16px!important;top:60px!important;z-index:2147483003!important;display:none!important;grid-template-columns:1fr!important;gap:6px!important;background:rgba(12,16,22,.96)!important;padding:8px!important;border-radius:8px!important;opacity:1!important;visibility:visible!important;pointer-events:auto!important;}'"
                + "+'.ds-watch-page .ds-tv-player-quality-menu.show{display:grid!important;opacity:1!important;visibility:visible!important;pointer-events:auto!important;}'"
                + "+'.ds-watch-page .ds-tv-player-quality-menu button.__ds-tv-quality-active{background:#00d084!important;color:#07110c!important;}'"
                + "+'html.__ds-tv-fullscreen,html.__ds-tv-fullscreen body{overflow:hidden!important;background:#000!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-top-navbar,html.__ds-tv-fullscreen .ds-sidebar-area,html.__ds-tv-fullscreen .ds-video-section__information,html.__ds-tv-fullscreen .ds-playlist-section{display:none!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-watch-page,html.__ds-tv-fullscreen .ds-watch-page__sections,html.__ds-tv-fullscreen .ds-video-section,html.__ds-tv-fullscreen .ds-video-section__card-video,html.__ds-tv-fullscreen .ds-video-section__embed,html.__ds-tv-fullscreen .embed-responsive,html.__ds-tv-fullscreen .ds-youtube-player{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;background:#000!important;z-index:2147483000!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-video-section__default,html.__ds-tv-fullscreen .ds-video-overlay{display:none!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-shaka-player,html.__ds-tv-fullscreen iframe,html.__ds-tv-fullscreen video{position:absolute!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;object-fit:contain!important;background:#000!important;}'"
                + "+'html.__ds-tv-fullscreen .shaka-controls-container{z-index:2147483001!important;}'"
                + "+'.ds-watch-page .ds-playlist-section,.ds-watch-page .ds-video-section__card-comments,.ds-watch-page .ds-video-section__comments-mobile,.ds-watch-page .ds-video-section__actions,.ds-watch-page .ds-video-section__share-button,.ds-watch-page .ds-video-section__download,.ds-watch-page [class*=comments]{display:none!important;}'"
                + "+'.ds-watch-page .ds-video-section__information,.ds-watch-page .ds-video-section__description{display:block!important;max-width:100%!important;width:100%!important;}';"
                + "window.__dsTvYoutubeCommand=(func,args)=>{"
                + "const iframe=document.querySelector('.ds-youtube-player iframe');"
                + "if(!iframe||!iframe.contentWindow)return false;"
                + "let origin='*';try{origin=new URL(iframe.src).origin||'*';}catch(e){}"
                + "iframe.contentWindow.postMessage(JSON.stringify({event:'command',func:func,args:args||[]}),origin);"
                + "return true;"
                + "};"
                + "window.__dsTvVisible=(el)=>{if(!el)return false;const r=el.getBoundingClientRect();const s=getComputedStyle(el);return r.width>20&&r.height>20&&r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth&&s.display!=='none'&&s.visibility!=='hidden'&&s.opacity!=='0';};"
                + "window.__dsTvShakaQuality=(value)=>{"
                + "const player=document.querySelector('.ds-shaka-player');if(!player)return false;"
                + "const label={default:'auto',hd1080:'1080',hd720:'720',large:'480',medium:'360',small:'240'}[value||'default']||'auto';"
                + "window.__dsTvRevealPlayerControls&&window.__dsTvRevealPlayerControls(player);"
                + "const clickMatch=()=>{"
                + "const items=[...document.querySelectorAll('.ds-video-settings-menu__option,.shaka-settings-menu button,.shaka-overflow-menu button,button')].filter(window.__dsTvVisible);"
                + "const text=(el)=>((el.getAttribute('aria-label')||el.title||el.innerText||el.textContent||'')+'').toLowerCase();"
                + "const direct=items.find((el)=>label==='auto'?/^\\s*auto\\b/.test(text(el))||/\\bauto\\b/.test(text(el)):(text(el).includes(label)&&/p?\\b/.test(text(el))));"
                + "if(direct){direct.click();return true;}"
                + "const quality=items.find((el)=>/quality|resolution/.test(text(el)));"
                + "if(quality){quality.click();setTimeout(clickMatch,140);return true;}"
                + "return false;"
                + "};"
                + "player.querySelector('.shaka-video-settings-button')?.click();setTimeout(clickMatch,160);return true;"
                + "};"
                + "window.__dsTvPlayerAction=(action,value,source)=>{"
                + "const sourceHost=source&&source.closest&&source.closest('.embed-responsive');"
                + "const sourcePlayer=(sourceHost&&sourceHost.querySelector('.ds-shaka-player,.ds-youtube-player'))||(source&&source.closest&&source.closest('.ds-shaka-player,.ds-youtube-player'));"
                + "const player=sourcePlayer||document.querySelector('.ds-shaka-player,.ds-youtube-player');"
                + "const host=(player&&player.closest&&player.closest('.embed-responsive'))||player;"
                + "if(action==='fullscreen')return true;"
                + "if(action==='quality'){const menu=host?.querySelector(':scope > .ds-tv-player-quality-menu');if(menu){const open=menu.classList.toggle('show');const controls=host.querySelector(':scope > .ds-tv-player-controls');if(open&&window.__dsTvRevealPlayerControls){window.__dsTvRevealPlayerControls(menu);}else if(!open){controls?.classList.remove('__ds-tv-reveal');}return true;}return false;}"
                + "if(action==='set-quality'){document.querySelectorAll('.ds-tv-player-quality-menu button').forEach((b)=>b.classList.toggle('__ds-tv-quality-active',b.dataset.dsTvPlayerValue===(value||'default')));const menu=source?.closest?.('.ds-tv-player-quality-menu')||host?.querySelector(':scope > .ds-tv-player-quality-menu');const targetHost=menu?.closest('.embed-responsive')||host;const targetPlayer=targetHost?.querySelector('.ds-shaka-player,.ds-youtube-player')||player;const controls=targetHost?.querySelector(':scope > .ds-tv-player-controls');menu?.classList.remove('show');setTimeout(()=>controls?.classList.remove('__ds-tv-reveal'),450);if(targetPlayer?.classList.contains('ds-youtube-player'))return !!((window.__dsTvYoutubeCommand&&window.__dsTvYoutubeCommand('setPlaybackQuality',[value||'default']))|(window.__dsTvYoutubeCommand&&window.__dsTvYoutubeCommand('setPlaybackQualityRange',[value||'default'])));return window.__dsTvShakaQuality&&window.__dsTvShakaQuality(value||'default');}"
                + "if(action==='pause'){const v=player?.querySelector('video')||document.querySelector('video');if(v){v.pause&&v.pause();return true;}window.__dsTvYoutubePaused=true;return window.__dsTvYoutubeCommand&&window.__dsTvYoutubeCommand('pauseVideo',[]);}"
                + "if(action==='play'){const v=player?.querySelector('video')||document.querySelector('video');if(v){v.play&&v.play();return true;}window.__dsTvYoutubePaused=false;return window.__dsTvYoutubeCommand&&window.__dsTvYoutubeCommand('playVideo',[]);}"
                + "if(action==='play-pause'){const v=player?.querySelector('video')||document.querySelector('video');if(v){if(v.paused){v.play&&v.play();}else{v.pause&&v.pause();}return true;}window.__dsTvYoutubePaused=!window.__dsTvYoutubePaused;return window.__dsTvYoutubeCommand&&window.__dsTvYoutubeCommand(window.__dsTvYoutubePaused?'pauseVideo':'playVideo',[]);}"
                + "return false;"
                + "};"
                + "const ensurePlayerControls=()=>{document.querySelectorAll('.ds-shaka-player,.ds-youtube-player').forEach((player)=>{"
                + "const host=player.closest('.embed-responsive')||player;"
                + "host.classList.add('__ds-tv-has-player');"
                + "if(host.querySelector(':scope > .ds-tv-player-controls'))return;"
                + "const controls=document.createElement('div');controls.className='ds-tv-player-controls';"
                + "const makeButton=(text,action,value)=>{const b=document.createElement('button');b.type='button';b.textContent=text;b.setAttribute('aria-label',text);b.dataset.dsTvPlayerAction=action;if(value)b.dataset.dsTvPlayerValue=value;return b;};"
                + "controls.appendChild(makeButton('Play/Pause','play-pause'));"
                + "controls.appendChild(makeButton('Quality','quality'));"
                + "controls.appendChild(makeButton('Fullscreen','fullscreen'));"
                + "const menu=document.createElement('div');menu.className='ds-tv-player-quality-menu';"
                + "[['Auto','default'],['1080p','hd1080'],['720p','hd720'],['480p','large'],['360p','medium'],['240p','small']].forEach(([text,value])=>menu.appendChild(makeButton(text,'set-quality',value)));"
                + "host.appendChild(controls);host.appendChild(menu);"
                + "});};"
                + "ensurePlayerControls();"
                + "clearInterval(window.__dsTvEnsurePlayerControlsInterval);"
                + "window.__dsTvEnsurePlayerControlsInterval=setInterval(ensurePlayerControls,1000);"
                + "window.__dsTvPlayerControlsObserver&&window.__dsTvPlayerControlsObserver.disconnect&&window.__dsTvPlayerControlsObserver.disconnect();"
                + "window.__dsTvPlayerControlsObserver=new MutationObserver(()=>ensurePlayerControls());"
                + "window.__dsTvPlayerControlsObserver.observe(document.body,{childList:true,subtree:true});"
                + "window.__dsTvRevealPlayerControls=(el)=>{"
                + "const host=el&&el.closest&&el.closest('.embed-responsive');"
                + "const player=(el&&el.closest&&el.closest('.ds-shaka-player,.ds-youtube-player'))||(host&&host.querySelector('.ds-shaka-player,.ds-youtube-player'))||document.querySelector('.ds-shaka-player,.ds-youtube-player');"
                + "if(!player)return;"
                + "player.classList.add('__ds-tv-reveal-player-controls');"
                + "(player.closest('.embed-responsive')||player).querySelector(':scope > .ds-tv-player-controls')?.classList.add('__ds-tv-reveal');"
                + "clearTimeout(window.__dsTvRevealPlayerTimer);"
                + "window.__dsTvRevealPlayerTimer=setTimeout(()=>{player.classList.remove('__ds-tv-reveal-player-controls');(player.closest('.embed-responsive')||player).querySelector(':scope > .ds-tv-player-controls')?.classList.remove('__ds-tv-reveal');},2200);"
                + "};"
                + "window.__dsTvEnterFullscreen=()=>{"
                + "if(!location.href.includes('/watch'))return false;"
                + "const player=(document.querySelector('.ds-shaka-player,.ds-youtube-player,video')?.closest('.embed-responsive'))||document.querySelector('.embed-responsive')||document.querySelector('.ds-video-section__embed');"
                + "if(!player)return false;"
                + "document.documentElement.classList.add('__ds-tv-fullscreen');"
                + "window.scrollTo(0,0);"
                + "window.__dsTvRevealPlayerControls&&window.__dsTvRevealPlayerControls(player);"
                + "return true;"
                + "};"
                + "})();";
        webView.evaluateJavascript(script, value -> {
            if (afterInjected != null) {
                afterInjected.run();
            }
        });
    }

    private void injectAutoplaySupport() {
        String script = "(() => {"
                + "const enabled=" + autoplayEnabled + ";"
                + "const eventOptions={bubbles:true,cancelable:true};"
                + "const text=(el)=>((el?.innerText||el?.textContent||'')+'').replace(/\\s+/g,' ').trim();"
                + "const checked=(el)=>{"
                + "if(!el)return null;"
                + "if(el.matches&&el.matches('input[type=\"checkbox\"],input[type=\"radio\"]'))return !!el.checked;"
                + "const aria=el.getAttribute&&el.getAttribute('aria-checked');"
                + "if(aria==='true'||aria==='false')return aria==='true';"
                + "return null;"
                + "};"
                + "const setChecked=(el,value)=>{"
                + "if(!el)return false;"
                + "const current=checked(el);"
                + "if(current===value)return true;"
                + "if(el.matches&&el.matches('input[type=\"checkbox\"],input[type=\"radio\"]')){"
                + "const setter=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'checked')?.set;"
                + "if(setter)setter.call(el,value);else el.checked=value;"
                + "el.dispatchEvent(new Event('input',eventOptions));"
                + "el.dispatchEvent(new Event('change',eventOptions));"
                + "}"
                + "if(checked(el)!==value){(el.closest&&el.closest('label'))?.click?.();}"
                + "if(checked(el)!==value){el.click&&el.click();}"
                + "return checked(el)===value;"
                + "};"
                + "const findAutoplayControl=()=>{"
                + "const controls=[...document.querySelectorAll('input[type=\"checkbox\"],input[type=\"radio\"],button,[role=\"switch\"],[role=\"checkbox\"],label')];"
                + "for(const el of controls){"
                + "const area=el.closest('label,.form-check,.custom-control,.ds-toggle,.ds-playlist-section,.ds-video-section')||el.parentElement||el;"
                + "const nearby=text(area);"
                + "if(/\\bAutoplay\\b/i.test(nearby)&&!/Skip watched/i.test(nearby)){"
                + "return el.matches&&el.matches('label')?(el.querySelector('input,[role=\"switch\"],[role=\"checkbox\"],button')||el):el;"
                + "}"
                + "}"
                + "const labels=[...document.querySelectorAll('*')].filter((el)=>text(el)==='Autoplay');"
                + "for(const label of labels){"
                + "let node=label;"
                + "for(let i=0;i<4&&node;i++,node=node.parentElement){"
                + "const input=node.querySelector&&node.querySelector('input[type=\"checkbox\"],input[type=\"radio\"],[role=\"switch\"],[role=\"checkbox\"],button');"
                + "if(input)return input;"
                + "}"
                + "}"
                + "return null;"
                + "};"
                + "const apply=()=>setChecked(findAutoplayControl(),enabled);"
                + "apply();"
                + "clearInterval(window.__dsTvWebsiteAutoplayInterval);"
                + "window.__dsTvWebsiteAutoplayInterval=setInterval(()=>{if(apply())clearInterval(window.__dsTvWebsiteAutoplayInterval);},500);"
                + "setTimeout(()=>clearInterval(window.__dsTvWebsiteAutoplayInterval),8000);"
                + "if(enabled){"
                + "window.__dsTvNextAutoplayEnabled=true;"
                + "return;"
                + "}"
                + "window.__dsTvNextAutoplayEnabled=false;"
                + "if(!window.__dsTvEndedBlockerInstalled){"
                + "window.__dsTvEndedBlockerInstalled=true;"
                + "window.addEventListener('ended',(event)=>{"
                + "if(window.__dsTvNextAutoplayEnabled)return;"
                + "const target=event.target;"
                + "if(target&&target.matches&&target.matches('video,audio')){"
                + "event.preventDefault();"
                + "event.stopImmediatePropagation();"
                + "setTimeout(()=>{try{target.pause();}catch(e){}},0);"
                + "}"
                + "},true);"
                + "}"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private void exitFullScreenVideo() {
        if (fullScreenView == null) {
            return;
        }
        root.removeView(fullScreenView);
        fullScreenView = null;
        tvFullscreenMode = false;
        webView.setVisibility(View.VISIBLE);
        webView.evaluateJavascript(
                "(() => {"
                        + "document.documentElement.classList.remove('__ds-tv-fullscreen');"
                        + "if(document.fullscreenElement&&document.exitFullscreen){document.exitFullscreen().catch(()=>{});}"
                        + "document.querySelector('.ds-tv-player-quality-menu')?.classList.remove('show');"
                        + "document.querySelector('.ds-tv-player-controls')?.classList.remove('__ds-tv-reveal');"
                        + "const iframe=document.querySelector('.ds-youtube-player iframe');"
                        + "if(iframe&&iframe.contentWindow){window.__dsTvYoutubePaused=true;iframe.contentWindow.postMessage(JSON.stringify({event:'command',func:'pauseVideo',args:[]}),'https://www.youtube.com');}"
                        + "return true;"
                        + "})();",
                null);
        showCursorTemporarily();
        webView.requestFocus();
        if (fullScreenCallback != null) {
            fullScreenCallback.onCustomViewHidden();
            fullScreenCallback = null;
        }
        hideSystemUi();
    }

    private void clickElementUnderCursor() {
        webView.evaluateJavascript(pointerScript("click"), null);
    }

    private void clickSelectedElement() {
        webView.evaluateJavascript(selectedElementScript("click"), value -> {
            if (value != null && value.contains("FULLSCREEN")) {
                if (tvFullscreenMode) {
                    exitTvFullscreenMode();
                } else {
                    enterTvFullscreenMode();
                }
            }
            if (value != null && value.contains("VIDEO_CARD")) {
                pendingAutoFullscreen = true;
                autoFullscreenAttempts = 0;
                scheduleAutoFullscreenAttempt(1200);
            }
            handler.postDelayed(this::selectOpenPopupIfPresent, 180);
        });
    }

    private void enterTvFullscreenMode() {
        if (webView == null) {
            return;
        }
        webView.evaluateJavascript(enterTvFullscreenScript(), value -> {
            if ("true".equals(value)) {
                tvFullscreenMode = true;
            }
        });
    }

    private void navigateToElement(String direction) {
        webView.evaluateJavascript(spatialNavigationScript(direction), value -> {
            if ("\"SCROLLED\"".equals(value)) {
                handler.postDelayed(() -> navigateToElement("nearest"), 120);
            } else {
                moveCursorToPagePoint(value);
            }
        });
    }

    private void scheduleAutoFullscreenAttempt(long delayMs) {
        handler.postDelayed(this::tryAutoFullscreen, delayMs);
    }

    private void tryAutoFullscreen() {
        if (!pendingAutoFullscreen || fullScreenView != null || webView == null) {
            return;
        }
        autoFullscreenAttempts++;
        webView.evaluateJavascript(autoFullscreenScript(), value -> {
            if ("true".equals(value)) {
                pendingAutoFullscreen = false;
                tvFullscreenMode = true;
                autoFullscreenAttempts = 0;
            } else if (pendingAutoFullscreen && autoFullscreenAttempts < 10) {
                scheduleAutoFullscreenAttempt(700);
            } else {
                pendingAutoFullscreen = false;
                autoFullscreenAttempts = 0;
            }
        });
    }

    private void selectOpenPopupIfPresent() {
        webView.evaluateJavascript(hasOpenPopupScript(), value -> {
            if ("true".equals(value)) {
                navigateToElement("nearest");
            }
        });
    }

    private void closeOpenPopup() {
        webView.evaluateJavascript(closeOpenPopupScript(), value -> {
            if (!"true".equals(value)) {
                onBackPressed();
            }
        });
    }

    private void closeOpenPopupOrHideControlsOrExitTvFullscreen() {
        webView.evaluateJavascript(closeOpenPopupScript(), value -> {
            if (!"true".equals(value)) {
                webView.evaluateJavascript(hideVisiblePlayerControlsScript(), hidden -> {
                    if (!"true".equals(hidden)) {
                        exitTvFullscreenMode();
                    }
                });
            }
        });
    }

    private String hideVisiblePlayerControlsScript() {
        return "(() => {"
                + "const visible=(el)=>{if(!el)return false;const r=el.getBoundingClientRect();const s=getComputedStyle(el);return r.width>20&&r.height>20&&r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth&&s.display!=='none'&&s.visibility!=='hidden'&&s.opacity!=='0';};"
                + "const controls=[...document.querySelectorAll('.ds-tv-player-controls.__ds-tv-reveal,.ds-tv-player-controls.__ds-tv-selection-active,.ds-shaka-player.__ds-tv-reveal-player-controls')];"
                + "if(!controls.some(visible))return false;"
                + "document.querySelectorAll('.ds-tv-player-controls').forEach((el)=>el.classList.remove('__ds-tv-reveal','__ds-tv-selection-active'));"
                + "document.querySelectorAll('.ds-shaka-player').forEach((el)=>el.classList.remove('__ds-tv-reveal-player-controls'));"
                + "return true;"
                + "})();";
    }

    private void focusElementUnderCursor() {
        webView.evaluateJavascript(selectedElementScript("focus"), this::moveCursorToPagePoint);
    }

    private String pointerScript(String action) {
        float x = cursorView.getCursorX();
        float y = cursorView.getCursorY();
        int width = Math.max(1, webView.getWidth());
        int height = Math.max(1, webView.getHeight());
        return "(() => {"
                + "const x=" + x + " * window.innerWidth / " + width + ";"
                + "const y=" + y + " * window.innerHeight / " + height + ";"
                + "const el=document.elementFromPoint(x,y);"
                + "if(!el)return;"
                + "const target=el.closest('a,button,[role=\"button\"],input,textarea,select,video,[tabindex]')||el;"
                + "target.focus&&target.focus({preventScroll:true});"
                + ("click".equals(action)
                ? "['pointerdown','mousedown','pointerup','mouseup','click'].forEach((type)=>target.dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,clientX:x,clientY:y,view:window})));"
                : "")
                + "})();";
    }

    private String selectedElementScript(String action) {
        return "(() => {"
                + "const selected=window.__dsTvSelected;"
                + "const pointerEl=document.elementFromPoint(window.__dsTvPointerX||window.innerWidth/2,window.__dsTvPointerY||window.innerHeight/2);"
                + "const base=selected||pointerEl;"
                + "if(!base)return '';"
                + "const isPopupItem=base.closest&&base.closest('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role=\"listbox\"],[role=\"menu\"],[role=\"dialog\"],.ds-select-menu,.ds-popover,[class*=\"popover\"],.ds-tv-player-quality-menu.show');"
                + "const commentsCard=base.closest&&base.closest('.ds-video-section__card-comments');"
                + "const target=(commentsCard||isPopupItem)?base:(window.__dsTvClickTarget?window.__dsTvClickTarget(base):base);"
                + "const videoCard=!!(target.closest&&target.closest('.ds-browse-videos__video > a[href],.ds-browse-videos__video'));"
                + "const playerAction=target.dataset&&target.dataset.dsTvPlayerAction;"
                + "const playerValue=target.dataset&&target.dataset.dsTvPlayerValue;"
                + "const label=((target.getAttribute&&target.getAttribute('aria-label'))||target.title||target.innerText||target.textContent||'').trim();"
                + "const fullscreenButton=playerAction==='fullscreen'||!!(target.closest&&target.closest('.shaka-fullscreen-button'))||/\\b(full screen|fullscreen)\\b/i.test(label);"
                + "const playPauseButton=playerAction==='play-pause'||playerAction==='play'||playerAction==='pause'||!!(target.closest&&target.closest('.shaka-play-button'))||/\\b(play|pause)\\b/i.test(label);"
                + "target.focus&&target.focus({preventScroll:true});"
                + ("click".equals(action)
                ? "if(fullscreenButton){}"
                        + "else if(playerAction){window.__dsTvPlayerAction&&window.__dsTvPlayerAction(playerAction,playerValue,target);}"
                        + "else if(playPauseButton){const v=document.querySelector('video');if(v){if(v.paused){v.play&&v.play();}else{v.pause&&v.pause();}}else{['pointerdown','mousedown','pointerup','mouseup','click'].forEach((type)=>target.dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,clientX:window.__dsTvPointerX||0,clientY:window.__dsTvPointerY||0,view:window})));}}"
                        + "else{['pointerdown','mousedown','pointerup','mouseup','click'].forEach((type)=>target.dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,clientX:window.__dsTvPointerX||0,clientY:window.__dsTvPointerY||0,view:window})));}"
                : "")
                + "const r=target.getBoundingClientRect();"
                + "return (fullscreenButton&&'" + action + "'==='click'?'FULLSCREEN|':'')+(videoCard&&'" + action + "'==='click'?'VIDEO_CARD|':'')+Math.round(r.left+r.width/2)+','+Math.round(r.top+r.height/2)+','+innerWidth+','+innerHeight;"
                + "})();";
    }

    private String autoFullscreenScript() {
        return enterTvFullscreenScript();
    }

    private String enterTvFullscreenScript() {
        return "(() => {"
                + "if(!location.href.includes('/watch'))return false;"
                + "const player=(document.querySelector('.ds-shaka-player,.ds-youtube-player,video')?.closest('.embed-responsive'))||document.querySelector('.embed-responsive')||document.querySelector('.ds-video-section__embed');"
                + "if(!player)return false;"
                + "let s=document.getElementById('ds-tv-fullscreen-fallback-style');"
                + "if(!s){s=document.createElement('style');s.id='ds-tv-fullscreen-fallback-style';document.head.appendChild(s);}"
                + "s.textContent='html.__ds-tv-fullscreen,html.__ds-tv-fullscreen body{overflow:hidden!important;background:#000!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-top-navbar,html.__ds-tv-fullscreen .ds-sidebar-area,html.__ds-tv-fullscreen .ds-video-section__information,html.__ds-tv-fullscreen .ds-playlist-section{display:none!important;}'"
                + "+'.ds-watch-page .ds-youtube-player{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;background:#000!important;}'"
                + "+'.ds-watch-page .embed-responsive.__ds-tv-has-player .ds-video-section__default{display:none!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-watch-page,html.__ds-tv-fullscreen .ds-watch-page__sections,html.__ds-tv-fullscreen .ds-video-section,html.__ds-tv-fullscreen .ds-video-section__card-video,html.__ds-tv-fullscreen .ds-video-section__embed,html.__ds-tv-fullscreen .embed-responsive,html.__ds-tv-fullscreen .ds-youtube-player{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;background:#000!important;z-index:2147483000!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-video-section__default,html.__ds-tv-fullscreen .ds-video-overlay{display:none!important;}'"
                + "+'html.__ds-tv-fullscreen .ds-shaka-player,html.__ds-tv-fullscreen iframe,html.__ds-tv-fullscreen video{position:absolute!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;object-fit:contain!important;background:#000!important;}'"
                + "+'html.__ds-tv-fullscreen .shaka-controls-container{z-index:2147483001!important;}';"
                + "document.documentElement.classList.add('__ds-tv-fullscreen');"
                + "window.scrollTo(0,0);"
                + "if(window.__dsTvRevealPlayerControls){window.__dsTvRevealPlayerControls(player);}"
                + "else{player.classList.add('__ds-tv-reveal-player-controls');}"
                + "return true;"
                + "})();";
    }

    private void exitTvFullscreenMode() {
        tvFullscreenMode = false;
        if (webView != null) {
            webView.evaluateJavascript(
                    "(() => {"
                            + "document.documentElement.classList.remove('__ds-tv-fullscreen');"
                            + "if(document.fullscreenElement&&document.exitFullscreen){document.exitFullscreen().catch(()=>{});}"
                            + "document.querySelector('.ds-tv-player-quality-menu')?.classList.remove('show');"
                            + "document.querySelector('.ds-tv-player-controls')?.classList.remove('__ds-tv-reveal');"
                            + "const player=document.querySelector('.ds-shaka-player,.ds-youtube-player');"
                            + "if(player){player.classList.remove('__ds-tv-reveal-player-controls');}"
                            + "const button=document.querySelector('.shaka-video-settings-button,.shaka-fullscreen-button');"
                            + "if(button){button.focus&&button.focus({preventScroll:true});}"
                            + "return true;"
                            + "})();",
                    null);
            webView.requestFocus();
        }
        showCursorTemporarily();
        hideSystemUi();
    }

    private String hasOpenPopupScript() {
        return "(() => {"
                + "const visible=(el)=>{const r=el.getBoundingClientRect();const s=getComputedStyle(el);return r.width>30&&r.height>30&&r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth&&s.display!=='none'&&s.visibility!=='hidden'&&s.opacity!=='0';};"
                + "return [...document.querySelectorAll('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role=\"listbox\"],[role=\"menu\"],[role=\"dialog\"],.ds-select-menu,.ds-popover,[class*=\"popover\"],.ds-video-settings-menu,.ds-tv-player-quality-menu.show')].some((el)=>visible(el)&&!el.closest('.ds-sidebar-area')&&!el.classList.contains('ds-browse-menu'));"
                + "})();";
    }

    private String closeOpenPopupScript() {
        return "(() => {"
                + "const visible=(el)=>{const r=el.getBoundingClientRect();const s=getComputedStyle(el);return r.width>30&&r.height>30&&r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth&&s.display!=='none'&&s.visibility!=='hidden'&&s.opacity!=='0';};"
                + "const popup=[...document.querySelectorAll('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role=\"listbox\"],[role=\"menu\"],[role=\"dialog\"],.ds-select-menu,.ds-popover,[class*=\"popover\"],.ds-video-settings-menu,.ds-tv-player-quality-menu.show')].find((el)=>visible(el)&&!el.closest('.ds-sidebar-area')&&!el.classList.contains('ds-browse-menu'));"
                + "if(!popup)return false;"
                + "if(popup.classList.contains('ds-tv-player-quality-menu')){"
                + "popup.classList.remove('show');"
                + "const host=popup.closest('.embed-responsive');"
                + "const toggle=host?.querySelector('[data-ds-tv-player-action=\"quality\"]');"
                + "document.querySelectorAll('.ds-tv-player-controls.__ds-tv-selection-active').forEach((el)=>el.classList.remove('__ds-tv-selection-active'));"
                + "document.querySelectorAll('.__ds-tv-selected').forEach((el)=>el.classList.remove('__ds-tv-selected'));"
                + "window.__dsTvSelected=toggle||null;"
                + "if(toggle){toggle.classList.add('__ds-tv-selected');toggle.closest('.ds-tv-player-controls')?.classList.add('__ds-tv-selection-active');const r=toggle.getBoundingClientRect();window.__dsTvPointerX=r.left+r.width/2;window.__dsTvPointerY=r.top+r.height/2;}"
                + "return true;"
                + "}"
                + "const toggle=popup.closest('.dropdown,.ds-form-dropdown')?.querySelector('button,.dropdown-toggle,[role=\"button\"]')||popup.closest('.ds-shaka-player,.ds-youtube-player')?.querySelector('[data-ds-tv-player-action=\"quality\"]')||popup.closest('.ds-shaka-player')?.querySelector('.shaka-video-settings-button');"
                + "document.querySelectorAll('.__ds-tv-selected').forEach((el)=>el.classList.remove('__ds-tv-selected'));"
                + "window.__dsTvSelected=toggle||null;"
                + "if(toggle){toggle.click();const r=toggle.getBoundingClientRect();window.__dsTvPointerX=r.left+r.width/2;window.__dsTvPointerY=r.top+r.height/2;}"
                + "else{document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',code:'Escape',bubbles:true}));}"
                + "return true;"
                + "})();";
    }

    private String spatialNavigationScript(String direction) {
        return "(() => {"
                + "const direction='" + direction + "';"
                + "const interactive='a[href],button,[role=\"button\"],[onclick],input:not([type=\"hidden\"]),textarea,select,video,[tabindex]:not([tabindex=\"-1\"])';"
                + "const styleId='ds-tv-selection-style';"
                + "if(!document.getElementById(styleId)){const s=document.createElement('style');s.id=styleId;s.textContent='.__ds-tv-selected{outline:5px solid #00d084!important;outline-offset:5px!important;box-shadow:0 0 0 9px rgba(0,208,132,.28)!important;border-radius:10px!important;}';document.head.appendChild(s);}"
                + "const isVisible=(el)=>{const r=el.getBoundingClientRect();const cs=getComputedStyle(el);return r.width>24&&r.height>24&&r.bottom>8&&r.right>8&&r.top<innerHeight-8&&r.left<innerWidth-8&&cs.visibility!=='hidden'&&cs.display!=='none'&&cs.opacity!=='0';};"
                + "const fullyVisible=(el)=>{const r=el.getBoundingClientRect();return r.top>=155&&r.bottom<=innerHeight-12&&r.left>=0&&r.right<=innerWidth;};"
                + "const center=(el)=>{const r=el.getBoundingClientRect();return {x:r.left+r.width/2,y:r.top+r.height/2,w:r.width,h:r.height};};"
                + "const clickable=(el)=>{if(el.matches&&el.matches(interactive))return el;const inner=el.querySelector&&el.querySelector(interactive);if(inner)return inner;return el.closest&&el.closest(interactive)||el;};"
                + "window.__dsTvClickTarget=clickable;"
                + "const menuRoot=document.querySelector('.ds-sidebar-area')||document.querySelector('nav,aside,[role=\"navigation\"]')||document.body;"
                + "const blockedByMenu=(el)=>{const p=center(el);const top=document.elementFromPoint(p.x,p.y);return top&&top.closest&&top.closest('nav,aside,[role=\"navigation\"],header')&&!el.contains(top);};"
                + "const allVideos=[...document.querySelectorAll('.ds-browse-videos__video > a[href]')];"
                + "const videoItems=allVideos.filter((el)=>isVisible(el)&&!blockedByMenu(el)).sort((a,b)=>{const ar=a.getBoundingClientRect();const br=b.getBoundingClientRect();return ar.top-br.top||ar.left-br.left;});"
                + "const watchRoot=document.querySelector('.ds-watch-page');"
                + "const watchSelector='[data-ds-tv-player-action],.ds-video-settings-menu__option';"
                + "const labelOf=(el)=>(((el&&el.getAttribute&&el.getAttribute('aria-label'))||(el&&el.title)||(el&&el.innerText)||(el&&el.textContent)||'')+'').trim();"
                + "const playerControlVisible=(el)=>{const r=el.getBoundingClientRect();const cs=getComputedStyle(el);return r.width>20&&r.height>20&&r.bottom>8&&r.right>8&&r.top<innerHeight-8&&r.left<innerWidth-8&&cs.visibility!=='hidden'&&cs.display!=='none';};"
                + "const isWantedPlayerControl=(el)=>{const label=labelOf(el);return !!(el.matches&&el.matches('[data-ds-tv-player-action],.shaka-play-button,.shaka-video-settings-button,.shaka-fullscreen-button,.ds-video-settings-menu__option'))||/^(play\\/pause|play|pause|video settings|settings|quality|full screen|fullscreen)$/i.test(label)||/\\b(quality|resolution)\\b/i.test(label)||/^(auto|1080p?|720p?|480p?|360p?|240p?)$/i.test(label);};"
                + "const isBlockedPlayerControl=(el)=>{const label=labelOf(el);return !!(el.matches&&el.matches('.shaka-skip-button,.shaka-mute-button,.shaka-current-time,.shaka-volume-bar,.shaka-seek-bar'))||/\\b(next video|skip|mute|volume|share|download|comments?)\\b/i.test(label);};"
                + "const watchItems=watchRoot?[...new Set([...watchRoot.querySelectorAll(watchSelector)].map(clickable))].filter((el)=>playerControlVisible(el)&&isWantedPlayerControl(el)&&!isBlockedPlayerControl(el)&&!el.closest('.ds-sidebar-area')).sort((a,b)=>{const ar=a.getBoundingClientRect();const br=b.getBoundingClientRect();return ar.top-br.top||ar.left-br.left;}):[];"
                + "const toolbarRoot=document.querySelector('.ds-browse-toolbar');"
                + "const toolbarItems=toolbarRoot?[...new Set([...toolbarRoot.querySelectorAll('button,[role=\"button\"],a[href],input:not([type=\"hidden\"]),[tabindex]:not([tabindex=\"-1\"])')].map(clickable))].filter(isVisible).sort((a,b)=>a.getBoundingClientRect().left-b.getBoundingClientRect().left):[];"
                + "const menuItems=[...new Set([...menuRoot.querySelectorAll('a[href],button,[role=\"button\"],[tabindex]:not([tabindex=\"-1\"])')].map(clickable))].filter(isVisible).sort((a,b)=>a.getBoundingClientRect().top-b.getBoundingClientRect().top);"
                + "const popupRoots=[...document.querySelectorAll('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role=\"listbox\"],[role=\"menu\"],[role=\"dialog\"],.ds-select-menu,.ds-popover,[class*=\"popover\"],.ds-video-settings-menu,.ds-tv-player-quality-menu.show')].filter((el)=>isVisible(el)&&!el.closest('.ds-sidebar-area')&&!el.classList.contains('ds-browse-menu'));"
                + "const popupRoot=popupRoots.sort((a,b)=>b.getBoundingClientRect().width*b.getBoundingClientRect().height-a.getBoundingClientRect().width*a.getBoundingClientRect().height)[0];"
                + "const popupItems=popupRoot?[...new Set([...popupRoot.querySelectorAll('[data-ds-tv-player-action],.dropdown-item,.ds-form-dropdown__item,.ds-video-settings-menu__option,button,a[role=\"button\"],[role=\"button\"],[role=\"option\"],[role=\"menuitem\"],a[href],label,[tabindex]:not([tabindex=\"-1\"])')].map((el)=>el.classList&&el.classList.contains('dropdown-item')?el:clickable(el)))].filter(isVisible).sort((a,b)=>{const ar=a.getBoundingClientRect();const br=b.getBoundingClientRect();return ar.top-br.top||ar.left-br.left;}):[];"
                + "const watchMenuItem=menuItems.find((el)=>el.matches&&el.matches('.ds-sidebar__links-link--watch'))||menuItems.find((el)=>/\\bwatch\\b/i.test((el.textContent||'').trim()))||menuItems[0];"
                + "const menuStartItem=watchMenuItem||menuItems[0];"
                + "let current=window.__dsTvSelected;"
                + "let currentIsPopup=!!(current&&popupItems.includes(current));"
                + "let currentIsMenu=!!(current&&menuItems.includes(current));"
                + "let currentIsToolbar=!!(current&&toolbarItems.includes(current));"
                + "let currentIsVideo=!!(current&&videoItems.includes(current));"
                + "let currentIsWatch=!!(current&&watchItems.includes(current));"
                + "let items=popupItems.length?popupItems:(currentIsMenu&&direction!=='right'?menuItems:(watchItems.length&&!videoItems.length?watchItems:(currentIsToolbar?toolbarItems:videoItems)));"
                + "if(direction==='left'&&currentIsVideo&&!popupItems.length){"
                + "const c=center(current);const leftVideos=videoItems.filter((el)=>center(el).x<c.x-10);"
                + "items=leftVideos.length?videoItems:(menuStartItem?[menuStartItem]:menuItems);"
                + "}"
                + "if(direction==='left'&&currentIsWatch&&menuStartItem&&!popupItems.length){items=[menuStartItem];}"
                + "if(direction==='right'&&currentIsMenu&&!popupItems.length){items=watchItems.length&&!videoItems.length?watchItems:videoItems;}"
                + "if(!items.length){items=[...videoItems,...toolbarItems,...menuItems];}"
                + "if(!items.length&&watchItems.length){items=watchItems;}"
                + "if(!items.length)return '';"
                + "if(!current||!document.contains(current)||!isVisible(current)){current=document.activeElement&&isVisible(document.activeElement)?document.activeElement:null;}"
                + "let origin=current?center(current):{x:window.__dsTvPreferredX||innerWidth/2,y:window.__dsTvPreferredY||innerHeight/2};"
                + "let next=null;"
                + "const videoCenters=videoItems.map((el)=>({el,c:center(el),r:el.getBoundingClientRect()}));"
                + "const watchCenters=watchItems.map((el)=>({el,c:center(el),r:el.getBoundingClientRect()}));"
                + "const toolbarCenters=toolbarItems.map((el)=>({el,c:center(el),r:el.getBoundingClientRect()}));"
                + "const popupCenters=popupItems.map((el)=>({el,c:center(el),r:el.getBoundingClientRect()}));"
                + "const selectNearest=(pool,x,y)=>pool.map((item)=>({item,score:Math.abs(item.c.x-x)*2+Math.abs(item.c.y-y)})).sort((a,b)=>a.score-b.score)[0]?.item?.el;"
                + "if(direction==='first'){next=videoItems[0]||watchItems[0]||items[0];}"
                + "else if(direction==='nearest'&&window.__dsTvPendingElement&&document.contains(window.__dsTvPendingElement)&&isVisible(window.__dsTvPendingElement)){next=window.__dsTvPendingElement;window.__dsTvPendingElement=null;}"
                + "else if(popupItems.length){"
                + "const popupToggle=popupRoot.closest('.embed-responsive')?.querySelector('[data-ds-tv-player-action=\"quality\"]')||popupRoot.closest('.dropdown,.ds-form-dropdown')?.querySelector('button,.dropdown-toggle,[role=\"button\"]');"
                + "if(direction==='left'&&popupRoot.classList.contains('ds-tv-player-quality-menu')&&currentIsPopup){next=popupToggle||current;}"
                + "else if(direction==='nearest'||!currentIsPopup){next=selectNearest(popupCenters,window.__dsTvPointerX||origin.x,window.__dsTvPointerY||origin.y)||popupItems[0];}"
                + "else if(direction==='up'||direction==='down'){const index=Math.max(0,popupItems.indexOf(current));const nextIndex=Math.max(0,Math.min(popupItems.length-1,index+(direction==='down'?1:-1)));next=popupItems[nextIndex];}"
                + "else {const candidates=popupCenters.filter((item)=>item.el!==current).filter((item)=>direction==='left'?item.c.x<origin.x-10:item.c.x>origin.x+10);next=selectNearest(candidates,origin.x,origin.y)||current;}"
                + "}"
                + "else if(currentIsMenu){"
                + "if(direction==='right'){next=watchItems.length&&!videoItems.length?watchItems[0]:videoItems[0];}"
                + "else if(direction==='up'||direction==='down'){const index=Math.max(0,menuItems.indexOf(current));const nextIndex=Math.max(0,Math.min(menuItems.length-1,index+(direction==='down'?1:-1)));next=menuItems[nextIndex];}"
                + "else{next=current;}"
                + "}"
                + "else if(watchItems.length&&!videoItems.length){"
                + "const index=Math.max(0,watchItems.indexOf(current));"
                + "if(direction==='left'&&menuStartItem){next=menuStartItem;}"
                + "else if(direction==='up'||direction==='down'){const nextIndex=Math.max(0,Math.min(watchItems.length-1,index+(direction==='down'?1:-1)));next=watchItems[nextIndex];}"
                + "else{const candidates=watchCenters.filter((item)=>item.el!==current).filter((item)=>{const dx=item.c.x-origin.x;return direction==='left'?dx<-10:dx>10;});next=selectNearest(candidates,origin.x,origin.y)||current;}"
                + "if(next&&!fullyVisible(next)&&!watchItems.includes(next)){window.__dsTvPendingElement=next;const p=center(next);window.__dsTvPreferredX=p.x;window.__dsTvPreferredY=p.y;window.__dsTvSelected=null;document.querySelectorAll('.__ds-tv-selected').forEach((el)=>el.classList.remove('__ds-tv-selected'));next.scrollIntoView({block:'center',inline:'nearest',behavior:'auto'});return 'SCROLLED';}"
                + "}"
                + "else if(direction==='nearest'){next=selectNearest(videoCenters.length?videoCenters:items.map((el)=>({el,c:center(el)})),window.__dsTvPreferredX||origin.x,window.__dsTvPreferredY||origin.y);}"
                + "else if(direction==='down'&&currentIsToolbar){next=selectNearest(videoCenters,window.__dsTvPreferredX||origin.x,innerHeight/2);}"
                + "else if((direction==='up'||direction==='down')&&currentIsVideo){"
                + "const sign=direction==='down'?1:-1;"
                + "const rows=[...new Set(videoCenters.map((item)=>Math.round(item.r.top)))].sort((a,b)=>a-b);"
                + "const currentTop=current.getBoundingClientRect().top;"
                + "const currentRow=rows.reduce((best,row)=>Math.abs(row-currentTop)<Math.abs(best-currentTop)?row:best,rows[0]);"
                + "const targetRow=direction==='down'?rows.find((row)=>row>currentRow+30):[...rows].reverse().find((row)=>row<currentRow-30);"
                + "if(targetRow!==undefined){"
                + "const rowItems=videoCenters.filter((item)=>Math.abs(item.r.top-targetRow)<35);"
                + "next=selectNearest(rowItems,window.__dsTvPreferredX||origin.x,targetRow);"
                + "if(next&&!fullyVisible(next)){const nr=next.getBoundingClientRect();const cr=current.getBoundingClientRect();window.__dsTvPendingElement=next;window.__dsTvPreferredX=center(next).x;window.__dsTvPreferredY=origin.y;window.__dsTvSelected=null;document.querySelectorAll('.__ds-tv-selected').forEach((el)=>el.classList.remove('__ds-tv-selected'));window.scrollBy({top:nr.top-cr.top,left:0,behavior:'auto'});return 'SCROLLED';}"
                + "}"
                + "if(!next&&direction==='up'&&toolbarCenters.length){next=selectNearest(toolbarCenters,window.__dsTvPreferredX||origin.x,0);}"
                + "if(!next){const rowStep=Math.max(240,Math.min(330,Math.abs((rows[1]||origin.y+272)-rows[0])||272));window.__dsTvPreferredX=origin.x;window.__dsTvPreferredY=origin.y;window.__dsTvSelected=null;document.querySelectorAll('.__ds-tv-selected').forEach((el)=>el.classList.remove('__ds-tv-selected'));window.scrollBy({top:sign*rowStep,left:0,behavior:'auto'});return 'SCROLLED';}"
                + "}else{"
                + "const candidates=items.filter((el)=>el!==current).map((el)=>({el,c:center(el)})).filter((item)=>{const dx=item.c.x-origin.x;const dy=item.c.y-origin.y;return direction==='left'?dx<-10:direction==='right'?dx>10:direction==='up'?dy<-10:dy>10;});"
                + "const scored=candidates.map((item)=>{const dx=item.c.x-origin.x;const dy=item.c.y-origin.y;const horizontal=direction==='left'||direction==='right';const primary=horizontal?Math.abs(dx):Math.abs(dy);const secondary=horizontal?Math.abs(dy):Math.abs(dx);const rowPenalty=horizontal&&secondary>80?2000:0;const colPenalty=!horizontal&&secondary>150?700:0;return {el:item.el,c:item.c,score:primary+secondary*4+rowPenalty+colPenalty};}).sort((a,b)=>a.score-b.score);"
                + "next=scored[0]&&scored[0].el;"
                + "}"
                + "if(!next){return current?(Math.round(origin.x)+','+Math.round(origin.y)+','+innerWidth+','+innerHeight):'';}"
                + "document.querySelectorAll('.ds-tv-player-controls.__ds-tv-selection-active').forEach((el)=>el.classList.remove('__ds-tv-selection-active'));"
                + "document.querySelectorAll('.__ds-tv-selected').forEach((el)=>el.classList.remove('__ds-tv-selected'));"
                + "window.__dsTvSelected=next;next.classList.add('__ds-tv-selected');"
                + "next.closest&&next.closest('.ds-tv-player-controls')?.classList.add('__ds-tv-selection-active');"
                + "const p=center(next);window.__dsTvPointerX=p.x;window.__dsTvPointerY=p.y;"
                + "window.__dsTvPreferredX=p.x;window.__dsTvPreferredY=p.y;"
                + "next.focus&&next.focus({preventScroll:true});"
                + "if(window.__dsTvRevealPlayerControls&&(next.closest&&next.closest('.ds-shaka-player,.ds-video-section__embed,.embed-responsive')||watchItems.includes(next))){window.__dsTvRevealPlayerControls(next);}"
                + "return Math.round(p.x)+','+Math.round(p.y)+','+innerWidth+','+innerHeight;"
                + "})();";
    }

    private void moveCursorToPagePoint(String encodedPoint) {
        if (encodedPoint == null || encodedPoint.equals("null") || encodedPoint.length() < 5) {
            return;
        }
        String point = encodedPoint;
        if (point.startsWith("\"") && point.endsWith("\"")) {
            point = point.substring(1, point.length() - 1);
        }
        String[] parts = point.split(",");
        if (parts.length != 4) {
            return;
        }
        try {
            float pageX = Float.parseFloat(parts[0]);
            float pageY = Float.parseFloat(parts[1]);
            float pageWidth = Float.parseFloat(parts[2]);
            float pageHeight = Float.parseFloat(parts[3]);
            float nativeX = pageX * webView.getWidth() / Math.max(1f, pageWidth);
            float nativeY = pageY * webView.getHeight() / Math.max(1f, pageHeight);
            showCursorTemporarily();
            cursorView.moveTo(nativeX, nativeY);
        } catch (NumberFormatException ignored) {
        }
    }

    private void showCursorTemporarily() {
        if (cursorView == null || fullScreenView != null || isSettingsOpen()) {
            return;
        }
        cursorView.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideCursorRunnable);
        handler.postDelayed(hideCursorRunnable, CURSOR_IDLE_TIMEOUT_MS);
    }

    private void scrollIfNearVerticalEdge(int direction) {
        int edge = Math.max(dpToPx(90), webView.getHeight() / 7);
        boolean nearTop = cursorView.getCursorY() <= edge;
        boolean nearBottom = cursorView.getCursorY() >= webView.getHeight() - edge;
        if ((direction < 0 && nearTop) || (direction > 0 && nearBottom)) {
            scrollBy(direction * dpToPx(SCROLL_STEP_DP));
        }
    }

    private void scrollPage(int direction) {
        scrollBy(direction * Math.max(dpToPx(320), webView.getHeight() - dpToPx(120)));
    }

    private void scrollBy(int pixels) {
        webView.evaluateJavascript("window.scrollBy({top:" + pixels + ",left:0,behavior:'smooth'});", null);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private static class CursorView extends View {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float cursorX;
        private float cursorY;
        private float radius;

        CursorView(Activity activity) {
            super(activity);
            float density = activity.getResources().getDisplayMetrics().density;
            radius = 12f * density;
            setBackgroundColor(Color.TRANSPARENT);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            fillPaint.setColor(Color.WHITE);
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setAlpha(235);
            strokePaint.setColor(Color.rgb(0, 208, 132));
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(3f * density);
            setWillNotDraw(false);
            setFocusable(false);
            setClickable(false);
        }

        void centerInParent() {
            cursorX = getWidth() / 2f;
            cursorY = getHeight() / 2f;
            invalidate();
        }

        void moveBy(float dx, float dy) {
            float padding = radius + 2f;
            cursorX = clamp(cursorX + dx, padding, Math.max(padding, getWidth() - padding));
            cursorY = clamp(cursorY + dy, padding, Math.max(padding, getHeight() - padding));
            invalidate();
        }

        void moveTo(float x, float y) {
            float padding = radius + 2f;
            cursorX = clamp(x, padding, Math.max(padding, getWidth() - padding));
            cursorY = clamp(y, padding, Math.max(padding, getHeight() - padding));
            invalidate();
        }

        float getCursorX() {
            return cursorX;
        }

        float getCursorY() {
            return cursorY;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (cursorX == 0f && cursorY == 0f) {
                cursorX = w / 2f;
                cursorY = h / 2f;
            } else {
                moveBy(0, 0);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawCircle(cursorX, cursorY, radius, fillPaint);
            canvas.drawCircle(cursorX, cursorY, radius, strokePaint);
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
