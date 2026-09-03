package com.bodiug.dreamingspanish;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class MainActivity extends Activity {
    private static final String START_URL =
            "https://app.dreaming.com/spanish/browse?sort=easy&hide-watched=true";
    private static final int SCROLL_STEP_DP = 170;
    private static final int MOUSE_STEP_DP = 34;
    private static final long CURSOR_IDLE_TIMEOUT_MS = 3000;
    private static final String PREFS_NAME = "dreaming_spanish_tv";
    private static final String PREF_WEBSITE_AUTOPLAY = "website_autoplay";
    private static final String PREF_AUTO_FULLSCREEN = "auto_fullscreen";

    private WebView webView;
    private FrameLayout root;
    private CursorView cursorView;
    private SettingsOverlay settingsOverlay;
    private TvWebScripts tvWebScripts;
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
    private SharedPreferences preferences;
    private final Runnable showSettingsRunnable = () -> {
        backLongPressTriggered = true;
        showSettingsOverlay();
    };
    private final Runnable hideCursorRunnable = () -> {
        if (cursorView != null && fullScreenView == null && !settingsOverlay.isOpen()) {
            cursorView.setVisibility(View.INVISIBLE);
        }
    };
    private final SettingsOverlay.Listener settingsListener = new SettingsOverlay.Listener() {
        @Override
        public void onNavigationModeChanged(boolean enabled) {
            mouseMode = enabled;
        }

        @Override
        public void onAutoplayChanged(boolean enabled) {
            autoplayEnabled = enabled;
            preferences.edit().putBoolean(PREF_WEBSITE_AUTOPLAY, enabled).apply();
            injectAutoplaySupport();
        }

        @Override
        public void onAutoFullscreenChanged(boolean enabled) {
            autoFullscreenEnabled = enabled;
            preferences.edit().putBoolean(PREF_AUTO_FULLSCREEN, enabled).apply();
        }

        @Override
        public void onRefreshRequested() {
            refreshPage();
        }

        @Override
        public void onResetRequested() {
            resetSession();
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        autoplayEnabled = preferences.getBoolean(PREF_WEBSITE_AUTOPLAY, true);
        autoFullscreenEnabled = preferences.getBoolean(PREF_AUTO_FULLSCREEN, true);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        tvWebScripts = new TvWebScripts(this);
        settingsOverlay = new SettingsOverlay(this, root, settingsListener);

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
                    if (settingsOverlay.isOpen()) {
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

        if (settingsOverlay.isOpen()) {
            return settingsOverlay.handleKey(keyCode, isConfirmKey(keyCode));
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

    private boolean isConfirmKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                || keyCode == KeyEvent.KEYCODE_BUTTON_A
                || keyCode == KeyEvent.KEYCODE_BUTTON_SELECT
                || keyCode == KeyEvent.KEYCODE_SPACE;
    }

    private void showSettingsOverlay() {
        if (settingsOverlay.isOpen()) {
            return;
        }
        cursorView.setVisibility(View.INVISIBLE);
        handler.removeCallbacks(hideCursorRunnable);
        settingsOverlay.show(mouseMode, autoplayEnabled, autoFullscreenEnabled);
        hideSystemUi();
    }

    private void hideSettingsOverlay() {
        settingsOverlay.hide();
        webView.requestFocus();
        hideSystemUi();
        showCursorTemporarily();
    }

    private void resetSession() {
        webView.evaluateJavascript(tvWebScripts.clearStorageCall(), null);
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
        webView.evaluateJavascript(tvWebScripts.setPlaybackCall("toggle"), null);
    }

    private void setVideoPlayback(boolean play) {
        webView.evaluateJavascript(tvWebScripts.setPlaybackCall(play ? "play" : "pause"), null);
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
        settings.setUserAgentString(DesktopHtmlInterceptor.DESKTOP_USER_AGENT);

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
                    WebResourceResponse response = DesktopHtmlInterceptor.loadDesktopHtml(request.getUrl().toString());
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
        webView.evaluateJavascript(tvWebScripts.focusScript(), null);
    }

    private void injectTvLayoutSupport(Runnable afterInjected) {
        webView.evaluateJavascript(tvWebScripts.coreScript(), value -> {
            if (afterInjected != null) {
                afterInjected.run();
            }
        });
    }

    private void injectAutoplaySupport() {
        webView.evaluateJavascript(tvWebScripts.applyAutoplayCall(autoplayEnabled), null);
    }

    private void exitFullScreenVideo() {
        if (fullScreenView == null) {
            return;
        }
        root.removeView(fullScreenView);
        fullScreenView = null;
        tvFullscreenMode = false;
        webView.setVisibility(View.VISIBLE);
        webView.evaluateJavascript(tvWebScripts.exitFullScreenVideoCleanupCall(), null);
        showCursorTemporarily();
        webView.requestFocus();
        if (fullScreenCallback != null) {
            fullScreenCallback.onCustomViewHidden();
            fullScreenCallback = null;
        }
        hideSystemUi();
    }

    private void clickElementUnderCursor() {
        webView.evaluateJavascript(
                tvWebScripts.pointerActionCall(
                        cursorView.getCursorX(), cursorView.getCursorY(),
                        webView.getWidth(), webView.getHeight(), "click"),
                null);
    }

    private void clickSelectedElement() {
        webView.evaluateJavascript(tvWebScripts.selectedElementActionCall("click"), value -> {
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
        webView.evaluateJavascript(tvWebScripts.enterTvFullscreenCall(), value -> {
            if ("true".equals(value)) {
                tvFullscreenMode = true;
            }
        });
    }

    private void navigateToElement(String direction) {
        webView.evaluateJavascript(tvWebScripts.spatialNavigateCall(direction), value -> {
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
        webView.evaluateJavascript(tvWebScripts.enterTvFullscreenCall(), value -> {
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
        webView.evaluateJavascript(tvWebScripts.hasOpenPopupCall(), value -> {
            if ("true".equals(value)) {
                navigateToElement("nearest");
            }
        });
    }

    private void closeOpenPopup() {
        webView.evaluateJavascript(tvWebScripts.closeOpenPopupCall(), value -> {
            if (!"true".equals(value)) {
                onBackPressed();
            }
        });
    }

    private void closeOpenPopupOrHideControlsOrExitTvFullscreen() {
        webView.evaluateJavascript(tvWebScripts.closeOpenPopupCall(), value -> {
            if (!"true".equals(value)) {
                webView.evaluateJavascript(tvWebScripts.hideVisiblePlayerControlsCall(), hidden -> {
                    if (!"true".equals(hidden)) {
                        exitTvFullscreenMode();
                    }
                });
            }
        });
    }

    private void focusElementUnderCursor() {
        webView.evaluateJavascript(tvWebScripts.selectedElementActionCall("focus"), this::moveCursorToPagePoint);
    }

    private void exitTvFullscreenMode() {
        tvFullscreenMode = false;
        if (webView != null) {
            webView.evaluateJavascript(tvWebScripts.exitTvFullscreenCall(), null);
            webView.requestFocus();
        }
        showCursorTemporarily();
        hideSystemUi();
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
        if (cursorView == null || fullScreenView != null || settingsOverlay.isOpen()) {
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
}
