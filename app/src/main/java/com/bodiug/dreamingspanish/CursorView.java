package com.bodiug.dreamingspanish;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

class CursorView extends View {
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
