package com.infotech.wishmaplus.reels.ui.componets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    // ── Tool types ─────────────────────────────────────────────────────────
    public enum Tool {BRUSH, PEN, HIGHLIGHTER, ERASER, LINE, RECT, CIRCLE, ARROW}

    // ── Stroke model ───────────────────────────────────────────────────────
    private static class Stroke {
        Path path;
        Paint paint;
        Tool tool;
        float startX, startY, endX, endY; // for shapes

        Stroke(Path path, Paint paint, Tool tool) {
            this.path = path;
            this.paint = paint;
            this.tool = tool;
        }
    }

    // ── State ──────────────────────────────────────────────────────────────
    private final List<Stroke> strokes = new ArrayList<>();
    private final List<Stroke> redoStack = new ArrayList<>();

    private Paint currentPaint;
    private Path currentPath;
    private Tool currentTool = Tool.BRUSH;
    private int currentColor = Color.WHITE;
    private float currentSize = 12f;
    private float currentAlpha = 1f;

    // shape drag helpers
    private float downX, downY;
    private Stroke shapeStroke; // in-progress shape

    // temp bitmap for shape preview
    private Bitmap cacheBitmap;
    private Canvas cacheCanvas;

    // ── Constructors ───────────────────────────────────────────────────────
    public DrawingView(Context ctx) {
        super(ctx);
        init();
    }

    public DrawingView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        resetPaint();
    }

    // ── Public API ─────────────────────────────────────────────────────────
    public void setTool(Tool tool) {
        currentTool = tool;
        resetPaint();
    }

    public void setColor(int color) {
        currentColor = color;
        resetPaint();
    }

    public void setBrushSize(float s) {
        currentSize = s;
        resetPaint();
    }

    public void setOpacity(float a) {
        currentAlpha = a;
        resetPaint();
    }

    public Tool getCurrentTool() {
        return currentTool;
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            redoStack.add(strokes.remove(strokes.size() - 1));
            rebuildCache();
            invalidate();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            strokes.add(redoStack.remove(redoStack.size() - 1));
            rebuildCache();
            invalidate();
        }
    }

    public void clear() {
        strokes.clear();
        redoStack.clear();
        rebuildCache();
        invalidate();
    }

    /**
     * Returns Bitmap of this drawing layer (transparent background)
     */
    public Bitmap exportBitmap() {
        Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        drawStrokes(c);
        return bmp;
    }

    // ── Touch ──────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                redoStack.clear();
                if (isShapeTool()) {
                    downX = x;
                    downY = y;
                    shapeStroke = newStroke();
                    shapeStroke.startX = x;
                    shapeStroke.startY = y;
                    shapeStroke.endX = x;
                    shapeStroke.endY = y;
                } else {
                    currentPath = new Path();
                    currentPath.moveTo(x, y);
                    strokes.add(newFreeStroke(currentPath));
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isShapeTool() && shapeStroke != null) {
                    shapeStroke.endX = x;
                    shapeStroke.endY = y;
                    invalidate();
                } else if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isShapeTool() && shapeStroke != null) {
                    shapeStroke.endX = x;
                    shapeStroke.endY = y;
                    strokes.add(shapeStroke);
                    shapeStroke = null;
                    rebuildCache();
                } else {
                    if (currentPath != null) currentPath.lineTo(x, y);
                    currentPath = null;
                    rebuildCache();
                }
                invalidate();
                break;
        }
        return true;
    }

    // ── Draw ───────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // draw cached committed strokes
        if (cacheBitmap != null) canvas.drawBitmap(cacheBitmap, 0, 0, null);
        // draw live stroke on top
        if (currentPath != null && !strokes.isEmpty()) {
            Stroke s = strokes.get(strokes.size() - 1);
            canvas.drawPath(s.path, s.paint);
        }
        // draw in-progress shape preview
        if (shapeStroke != null) drawShape(canvas, shapeStroke);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rebuildCache();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void rebuildCache() {
        if (getWidth() == 0 || getHeight() == 0) return;
        cacheBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        cacheCanvas = new Canvas(cacheBitmap);
        drawStrokes(cacheCanvas);
    }

    private void drawStrokes(Canvas canvas) {
        int count = strokes.size();
        for (int i = 0; i < count; i++) {
            Stroke s = strokes.get(i);
            if (isShapeToolType(s.tool)) drawShape(canvas, s);
            else canvas.drawPath(s.path, s.paint);
        }
    }

    private void drawShape(Canvas canvas, Stroke s) {
        float l = Math.min(s.startX, s.endX);
        float t = Math.min(s.startY, s.endY);
        float r = Math.max(s.startX, s.endX);
        float b = Math.max(s.startY, s.endY);

        switch (s.tool) {
            case LINE:
                canvas.drawLine(s.startX, s.startY, s.endX, s.endY, s.paint);
                break;
            case RECT:
                canvas.drawRect(l, t, r, b, s.paint);
                break;
            case CIRCLE:
                float cx = (s.startX + s.endX) / 2f;
                float cy = (s.startY + s.endY) / 2f;
                float rx2 = Math.abs(s.endX - s.startX) / 2f;
                float ry2 = Math.abs(s.endY - s.startY) / 2f;
                canvas.drawOval(cx - rx2, cy - ry2, cx + rx2, cy + ry2, s.paint);
                break;
            case ARROW:
                drawArrow(canvas, s.startX, s.startY, s.endX, s.endY, s.paint);
                break;
        }
    }

    private void drawArrow(Canvas canvas, float x1, float y1, float x2, float y2, Paint paint) {
        canvas.drawLine(x1, y1, x2, y2, paint);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        float headLen = paint.getStrokeWidth() * 4f;
        float ax1 = (float) (x2 - headLen * Math.cos(angle - Math.PI / 6));
        float ay1 = (float) (y2 - headLen * Math.sin(angle - Math.PI / 6));
        float ax2 = (float) (x2 - headLen * Math.cos(angle + Math.PI / 6));
        float ay2 = (float) (y2 - headLen * Math.sin(angle + Math.PI / 6));
        canvas.drawLine(x2, y2, ax1, ay1, paint);
        canvas.drawLine(x2, y2, ax2, ay2, paint);
    }

    private Stroke newStroke() {
        Paint p = buildPaint();
        p.setStyle(Paint.Style.STROKE);
        return new Stroke(new Path(), p, currentTool);
    }

    private Stroke newFreeStroke(Path path) {
        return new Stroke(path, buildPaint(), currentTool);
    }

    private Paint buildPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStrokeWidth(currentSize);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStyle(Paint.Style.STROKE);

        switch (currentTool) {
            case ERASER:
                p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                p.setStrokeWidth(currentSize * 2.5f);
                break;
            case HIGHLIGHTER:
                p.setColor(currentColor);
                p.setAlpha((int) (80 * currentAlpha));
                p.setStrokeWidth(currentSize * 3f);
                p.setXfermode(null);
                return p;
            case RECT:
            case CIRCLE:
                p.setStyle(Paint.Style.STROKE);
                break;
            default:
                break;
        }
        p.setColor(currentColor);
        p.setAlpha((int) (255 * currentAlpha));
        return p;
    }

    private void resetPaint() {
        currentPaint = buildPaint();
    }

    private boolean isShapeTool() {
        return isShapeToolType(currentTool);
    }

    private boolean isShapeToolType(Tool t) {
        return t == Tool.LINE || t == Tool.RECT || t == Tool.CIRCLE || t == Tool.ARROW;
    }
}
