package com.amp.trimmy.filters;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.daasuu.mp4compose.filter.GlOverlayFilter;

public class GlCustomWatermarkFilter extends GlOverlayFilter {

    private Bitmap bitmap;
    private Position position = Position.LEFT_TOP;
    private int percentage;
    private int padding;

    public GlCustomWatermarkFilter(Bitmap bitmap) {
        this.bitmap = bitmap;
    }


    public GlCustomWatermarkFilter(Bitmap bitmap, Position position, int percentage, int padding) {
        this.bitmap = bitmap;
        this.position = position;
        this.percentage = percentage;
        this.padding = padding;
    }

    @Override
    protected void drawCanvas(Canvas canvas) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap = getResizedBitmap(bitmap, canvas.getWidth(), canvas.getHeight());
            switch (position) {
                case LEFT_TOP:
                    canvas.drawBitmap(bitmap, padding, padding, null);
                    break;
                case LEFT_BOTTOM:
                    canvas.drawBitmap(bitmap, padding, canvas.getHeight() - bitmap.getHeight() - padding, null);
                    break;
                case RIGHT_TOP:
                    canvas.drawBitmap(bitmap, canvas.getWidth() - bitmap.getWidth() - padding, padding, null);
                    break;
                case RIGHT_BOTTOM:
                    canvas.drawBitmap(bitmap, canvas.getWidth() - bitmap.getWidth() - padding, canvas.getHeight() - bitmap.getHeight() - padding, null);
                    break;
            }
        }
    }

    private Bitmap getResizedBitmap(Bitmap bitmap, int width, int height) {
        float aspectRatio = bitmap.getWidth() / (float) bitmap.getHeight();
        int newWidth = (width * percentage) / 100;
        int newHeight = Math.round(newWidth / aspectRatio);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public enum Position {
        LEFT_TOP,
        LEFT_BOTTOM,
        RIGHT_TOP,
        RIGHT_BOTTOM
    }

}
