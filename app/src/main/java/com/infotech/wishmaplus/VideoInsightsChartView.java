package com.infotech.wishmaplus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom line chart for Video Insights — no external library needed.
 * Drop into any layout with a fixed height (e.g. 180dp).
 * <p>
 * Usage:
 * chart.setData(labels, values, "#1877F2");
 */
public class VideoInsightsChartView extends View {

    // ── Geometry ──────────────────────────────────────────────────────────
    private final float PADDING_LEFT = 48f;
    private final float PADDING_RIGHT = 16f;
    private final float PADDING_TOP = 20f;
    private final float PADDING_BOTTOM = 40f;
    // ── Data ──────────────────────────────────────────────────────────────
    private List<String> mLabels = new ArrayList<>();
    private List<Float> mValues = new ArrayList<>();
    private String mColor = "#1877F2";
    private String mMetricLabel = "Minutes Viewed";
    // ── Paints ────────────────────────────────────────────────────────────
    private Paint linePaint, fillPaint, dotPaint, dotBorderPaint;
    private Paint labelPaint, gridPaint, axisPaint, valuePaint;

    public VideoInsightsChartView(Context ctx) {
        super(ctx);
        init();
    }

    public VideoInsightsChartView(Context ctx, AttributeSet a) {
        super(ctx, a);
        init();
    }

    public VideoInsightsChartView(Context ctx, AttributeSet a, int d) {
        super(ctx, a, d);
        init();
    }

    private void init() {
        float density = getContext().getResources().getDisplayMetrics().density;

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);

        dotBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotBorderPaint.setStyle(Paint.Style.FILL);
        dotBorderPaint.setColor(Color.WHITE);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.parseColor("#90949C"));
        labelPaint.setTextSize(10f * density);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setColor(Color.parseColor("#1C1E21"));
        valuePaint.setTextSize(10f * density);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E4E6EA"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(0.8f * density);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{6f, 4f}, 0));

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#E4E6EA"));
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(0.8f * density);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    /**
     * Feed data into the chart. Call this from your Activity/Fragment.
     */
    public void setData(List<String> labels, List<Float> values, String hexColor, String metricLabel) {
        this.mLabels = labels != null ? labels : new ArrayList<>();
        this.mValues = values != null ? values : new ArrayList<>();
        this.mColor = hexColor != null ? hexColor : "#1877F2";
        this.mMetricLabel = metricLabel != null ? metricLabel : "";
        applyColor();
        invalidate();
    }

    public void setData(List<String> labels, List<Float> values) {
        setData(labels, values, "#1877F2", "");
    }

    private void applyColor() {
        int color = Color.parseColor(mColor);
        linePaint.setColor(color);
        dotPaint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mValues.isEmpty()) return;

        float w = getWidth();
        float h = getHeight();
        float chartLeft = PADDING_LEFT;
        float chartRight = w - PADDING_RIGHT;
        float chartTop = PADDING_TOP;
        float chartBottom = h - PADDING_BOTTOM;
        float chartW = chartRight - chartLeft;
        float chartH = chartBottom - chartTop;

        // ── Find min / max ─────────────────────────────────────────────
        float maxVal = 0, minVal = Float.MAX_VALUE;
        for (float v : mValues) {
            if (v > maxVal) maxVal = v;
            if (v < minVal) minVal = v;
        }
        if (maxVal == 0) maxVal = 1;
        float range = maxVal - minVal;
        if (range == 0) range = maxVal;

        // ── Grid lines (4 horizontal) ──────────────────────────────────
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            float y = chartBottom - (chartH * i / gridLines);
            canvas.drawLine(chartLeft, y, chartRight, y, axisPaint);
            // Y-axis labels
            float labelVal = minVal + (range * i / gridLines);
            String yLabel = labelVal >= 1000 ? String.format("%.1fk", labelVal / 1000f) : String.valueOf((int) labelVal);
            Paint yLabelPaint = new Paint(labelPaint);
            yLabelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(yLabel, chartLeft - 6f, y + 4f, yLabelPaint);
        }

        // ── Compute point coordinates ──────────────────────────────────
        int n = mValues.size();
        float[] px = new float[n];
        float[] py = new float[n];
        float step = n > 1 ? chartW / (n - 1) : chartW;
        for (int i = 0; i < n; i++) {
            px[i] = chartLeft + (n > 1 ? step * i : chartW / 2f);
            py[i] = chartBottom - ((mValues.get(i) - minVal) / range) * chartH;
        }

        // ── Build smooth line path ─────────────────────────────────────
        Path linePath = new Path();
        linePath.moveTo(px[0], py[0]);
        for (int i = 1; i < n; i++) {
            float cpX = (px[i - 1] + px[i]) / 2f;
            linePath.cubicTo(cpX, py[i - 1], cpX, py[i], px[i], py[i]);
        }

        // ── Fill under curve ──────────────────────────────────────────
        Path fillPath = new Path(linePath);
        fillPath.lineTo(px[n - 1], chartBottom);
        fillPath.lineTo(px[0], chartBottom);
        fillPath.close();

        int baseColor = Color.parseColor(mColor);
        int alpha0 = Color.argb(50, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        int alpha1 = Color.argb(0, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        fillPaint.setShader(new LinearGradient(0, chartTop, 0, chartBottom, alpha0, alpha1, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);

        // ── Draw line ─────────────────────────────────────────────────
        canvas.drawPath(linePath, linePaint);

        // ── Dots + value labels ───────────────────────────────────────
        float density = getContext().getResources().getDisplayMetrics().density;
        float dotOuter = 5f * density;
        float dotInner = 3f * density;

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(px[i], py[i], dotOuter, dotBorderPaint);
            canvas.drawCircle(px[i], py[i], dotInner, dotPaint);

            // X-axis labels
            if (i < mLabels.size()) {
                canvas.drawText(mLabels.get(i), px[i], chartBottom + 20f * density, labelPaint);
            }
        }
    }
}