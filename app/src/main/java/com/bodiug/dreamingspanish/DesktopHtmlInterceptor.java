package com.bodiug.dreamingspanish;

import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DesktopHtmlInterceptor {
    static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 DreamingSpanishTV/1.0";
    private static final int DESKTOP_VIEWPORT_WIDTH = 1280;

    private DesktopHtmlInterceptor() {
    }

    static WebResourceResponse loadDesktopHtml(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Cache-Control", "no-cache");
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

    private static String patchInitialHtml(String html) {
        String viewport = "<meta name=\"viewport\" content=\"width=" + DESKTOP_VIEWPORT_WIDTH
                + ", initial-scale=1, maximum-scale=1, user-scalable=no\">";
        if (html.contains("name=\"viewport\"")) {
            return html.replaceFirst("<meta\\s+name=\"viewport\"[^>]*>", viewport);
        }
        return html.replaceFirst("<head>", "<head>" + viewport);
    }

    private static Map<String, String> responseHeaders(HttpURLConnection connection) {
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

    private static byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
