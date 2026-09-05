package com.bodiug.dreamingspanish;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class TvWebScripts {
    private static final String CORE_ASSET = "js/ds-tv-core.js";
    private static final String FOCUS_ASSET = "js/ds-tv-focus.js";
    private static final String AUTOPLAY_ASSET = "js/ds-tv-autoplay.js";

    private final AssetManager assetManager;
    private String coreScript;
    private String focusScript;
    private String autoplayScript;

    TvWebScripts(Context context) {
        this.assetManager = context.getApplicationContext().getAssets();
    }

    String coreScript() {
        if (coreScript == null) {
            coreScript = readAsset(CORE_ASSET);
        }
        return coreScript;
    }

    String focusScript() {
        if (focusScript == null) {
            focusScript = readAsset(FOCUS_ASSET);
        }
        return focusScript;
    }

    String applyAutoplayCall(boolean enabled) {
        return autoplayScript() + "\nwindow.__dsTvApplyAutoplay(" + enabled + ");";
    }

    String pointerActionCall(float cursorX, float cursorY, int viewWidth, int viewHeight, String action) {
        return "window.__dsTvPointerAction(" + cursorX + "," + cursorY + ","
                + Math.max(1, viewWidth) + "," + Math.max(1, viewHeight) + ",'" + action + "');";
    }

    String selectedElementActionCall(String action) {
        return "window.__dsTvSelectedElementAction('" + action + "');";
    }

    String spatialNavigateCall(String direction) {
        return "window.__dsTvSpatialNavigate('" + direction + "');";
    }

    String hasOpenPopupCall() {
        return "window.__dsTvHasOpenPopup();";
    }

    String closeOpenPopupCall() {
        return "window.__dsTvCloseOpenPopup();";
    }

    String hideVisiblePlayerControlsCall() {
        return "window.__dsTvHideVisiblePlayerControls();";
    }

    String enterTvFullscreenCall() {
        return "window.__dsTvEnterTvFullscreen();";
    }

    String exitTvFullscreenCall() {
        return "window.__dsTvExitTvFullscreen();";
    }

    String exitFullScreenVideoCleanupCall() {
        return "window.__dsTvExitFullScreenVideoCleanup();";
    }

    String setPlaybackCall(String action) {
        return "window.__dsTvSetPlayback('" + action + "');";
    }

    String videoEndedCall() {
        return "(() => {"
                + "const v=document.querySelector('video');"
                + "if(v)return v.ended;"
                + "return window.__dsTvYoutubeEnded===true;"
                + "})();";
    }

    String clearStorageCall() {
        return "window.__dsTvClearStorage();";
    }

    private String autoplayScript() {
        if (autoplayScript == null) {
            autoplayScript = readAsset(AUTOPLAY_ASSET);
        }
        return autoplayScript;
    }

    private String readAsset(String path) {
        try (InputStream input = assetManager.open(path)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load asset: " + path, e);
        }
    }
}
