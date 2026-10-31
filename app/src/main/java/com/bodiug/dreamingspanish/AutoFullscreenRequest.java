package com.bodiug.dreamingspanish;

final class AutoFullscreenRequest {
    private String targetUrl;
    private int generation;
    private int attempts;

    void start(String url) {
        cancel();
        targetUrl = pageUrl(url);
    }

    void cancel() {
        generation++;
        targetUrl = null;
        attempts = 0;
    }

    void onNavigation(String url) {
        if (isPending() && !targetUrl.equals(pageUrl(url))) cancel();
    }

    boolean isPending() {
        return targetUrl != null;
    }

    boolean nextAttempt() {
        return isPending() && attempts++ < 50;
    }

    int generation() {
        return generation;
    }

    boolean isCurrent(int token) {
        return isPending() && generation == token;
    }

    String targetUrl() {
        return targetUrl;
    }

    private static String pageUrl(String url) {
        return url == null ? null : url.split("#", 2)[0];
    }
}
