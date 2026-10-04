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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        html = patchWebsiteBootstrap(html);
        String viewport = "<meta name=\"viewport\" content=\"width=" + DESKTOP_VIEWPORT_WIDTH
                + ", initial-scale=1, maximum-scale=1, user-scalable=no\">";
        if (html.contains("name=\"viewport\"")) {
            return html.replaceFirst("<meta\\s+name=\"viewport\"[^>]*>", viewport);
        }
        return html.replaceFirst("<head>", "<head>" + viewport);
    }

    private static String patchWebsiteBootstrap(String html) {
        // Clear only persisted video queries before React hydrates its offline cache.
        Pattern module = Pattern.compile("<script\\b(?=[^>]*type=\"module\")[^>]*src=\"(/assets/[A-Za-z0-9_.-]+\\.js)\"[^>]*>\\s*</script>");
        Matcher match = module.matcher(html);
        if (!match.find()) {
            return html;
        }
        String bootstrap = "<script type=\"module\">"
                + "await new Promise(resolve=>{"
                + "try{const request=indexedDB.open('react_query_offline_db');"
                + "request.onupgradeneeded=()=>{request.transaction.abort();resolve();};"
                + "request.onerror=()=>resolve();"
                + "request.onblocked=()=>resolve();"
                + "request.onsuccess=()=>{const db=request.result;"
                + "if(!db.objectStoreNames.contains('queries')){db.close();resolve();return;}"
                + "try{const tx=db.transaction('queries','readwrite');"
                + "tx.oncomplete=tx.onerror=tx.onabort=()=>{db.close();resolve();};"
                + "const cursor=tx.objectStore('queries').openCursor();"
                + "cursor.onsuccess=()=>{const row=cursor.result;if(!row)return;"
                + "const value=row.value;if(Array.isArray(value.state?.queries)){"
                + "value.state.queries=value.state.queries.filter(query=>{"
                + "const key=Array.isArray(query.queryKey)?query.queryKey[0]:query.queryKey;"
                + "return key!=='videosState'&&key!=='videoState';});row.update(value);}"
                + "row.continue();};}catch(e){db.close();resolve();}};"
                + "}catch(e){resolve();}});"
                + "await import('" + match.group(1) + "');</script>";
        return match.replaceFirst(Matcher.quoteReplacement(bootstrap));
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
