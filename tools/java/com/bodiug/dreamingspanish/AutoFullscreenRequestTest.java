package com.bodiug.dreamingspanish;

public final class AutoFullscreenRequestTest {
    public static void main(String[] args) {
        AutoFullscreenRequest request = new AutoFullscreenRequest();
        require(!request.nextAttempt(), "No attempts without a pending request");
        request.start("https://app.dreaming.com/spanish/watch?id=first");
        int first = request.generation();
        request.onNavigation("https://app.dreaming.com/spanish/watch?id=first#player");
        require(request.isCurrent(first), "Navigation to the selected video preserves its request");
        for (int i = 0; i < 50; i++) require(request.nextAttempt(), "Allow readiness checks for 7.5 seconds");
        require(!request.nextAttempt(), "Stop after the readiness retry budget");
        request.cancel();
        require(!request.isCurrent(first), "Cancelled callbacks cannot reactivate fullscreen");
        require(!request.isPending(), "Cancellation clears the target");
        request.start("https://app.dreaming.com/spanish/watch?id=second");
        int second = request.generation();
        require(!request.isCurrent(first), "New requests invalidate old callbacks");
        require(request.nextAttempt(), "New requests reset the retry budget");
        request.onNavigation("https://app.dreaming.com/spanish/browse");
        require(!request.isCurrent(second), "Leaving the video cancels its request");
        require(!request.nextAttempt(), "No retry after navigating away");
        request.start("https://app.dreaming.com/spanish/watch?id=third");
        request.onNavigation("https://app.dreaming.com/spanish/watch?id=fourth");
        require(!request.isPending(), "A different video must not inherit the request");
        System.out.println("Automatic fullscreen request checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
