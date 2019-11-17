package com.cakranegara;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A class that handles pointer to help user
 * see where the anchor will be placed.
 */
public class PointerDrawable extends Drawable {

    private final Paint paint = new Paint();
    private boolean enabled;

    @Override
    public void draw(@NonNull Canvas canvas) {
        float cx = canvas.getWidth()/2;
        float cy = canvas.getHeight()/2;
        if (enabled) {
            paint.setColor(Color.GREEN);
            canvas.drawCircle(cx, cy, 25, paint);
        } else {
            paint.setColor(Color.TRANSPARENT);
            canvas.drawCircle(cx, cy, 25, paint);
        }
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
