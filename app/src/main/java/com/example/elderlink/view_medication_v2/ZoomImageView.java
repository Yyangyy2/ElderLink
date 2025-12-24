package com.example.elderlink.view_medication_v2;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomImageView extends AppCompatImageView {
    private Matrix matrix = new Matrix();
    private PointF start = new PointF();
    private Matrix savedMatrix = new Matrix();
    private ScaleGestureDetector scaleDetector;

    // Modes
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // For zoom
    private float[] lastEvent = null;
    private PointF mid = new PointF();
    private float oldDist = 1f;

    // Add these variables to track scale
    private float scaleFactor = 1.0f;
    private float minScale = 1.0f;
    private float maxScale = 5.0f;

    public ZoomImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        // Reset matrix to identity (no zoom)
        matrix.reset();
        setImageMatrix(matrix);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Reset matrix when layout changes
        resetZoom();
    }

    @Override
    public void setImageBitmap(android.graphics.Bitmap bm) {
        super.setImageBitmap(bm);
        // Reset zoom when new image is set
        resetZoom();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        PointF curr = new PointF(event.getX(), event.getY());

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(curr);
                mode = DRAG;
                lastEvent = null;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    matrix.postTranslate(curr.x - start.x, curr.y - start.y);
                } else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / oldDist;
                        scaleFactor *= scale;

                        // Limit zoom scale
                        if (scaleFactor < minScale) scaleFactor = minScale;
                        if (scaleFactor > maxScale) scaleFactor = maxScale;

                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        setImageMatrix(matrix);
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Limit zoom scale
            scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));

            matrix.set(savedMatrix);
            matrix.postScale(scaleFactor, scaleFactor,
                    detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            savedMatrix.set(matrix);
            return true;
        }
    }

    // Add this method to reset zoom
    public void resetZoom() {
        scaleFactor = 1.0f;
        matrix.reset();

        // Calculate initial scale to fit image to screen
        if (getDrawable() != null) {
            int drawableWidth = getDrawable().getIntrinsicWidth();
            int drawableHeight = getDrawable().getIntrinsicHeight();
            int viewWidth = getWidth();
            int viewHeight = getHeight();

            if (drawableWidth > 0 && drawableHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                float scaleX = (float) viewWidth / drawableWidth;
                float scaleY = (float) viewHeight / drawableHeight;
                float scale = Math.min(scaleX, scaleY);

                // Center the image
                float dx = (viewWidth - drawableWidth * scale) / 2;
                float dy = (viewHeight - drawableHeight * scale) / 2;

                matrix.postScale(scale, scale);
                matrix.postTranslate(dx, dy);
            }
        }

        setImageMatrix(matrix);
        invalidate();
    }
}