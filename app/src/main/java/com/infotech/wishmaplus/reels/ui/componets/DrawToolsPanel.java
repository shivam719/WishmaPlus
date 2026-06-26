package com.infotech.wishmaplus.reels.ui.componets;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.infotech.wishmaplus.R;

public class DrawToolsPanel extends BottomSheetDialogFragment {

    public interface DrawToolsListener {
        void onToolSelected(DrawingView.Tool tool);

        void onColorSelected(int color);

        void onSizeChanged(float size);

        void onOpacityChanged(float opacity);

        void onUndo();

        void onRedo();

        void onClear();

        void onDone();
    }

    private static final int[] COLORS = {
            0xFFFFFFFF, 0xFF000000, 0xFFFF3B30, 0xFFFF9500,
            0xFFFFCC00, 0xFF34C759, 0xFF00C7BE, 0xFF007AFF,
            0xFF5856D6, 0xFFFF2D55, 0xFFFF6B6B, 0xFFFFE66D,
            0xFF4ECDC4, 0xFF45B7D1, 0xFFDDA0DD, 0xFFF7DC6F
    };

    private DrawToolsListener listener;
    private DrawingView.Tool selectedTool = DrawingView.Tool.BRUSH;
    private int selectedColor = Color.BLACK;

    public static DrawToolsPanel newInstance() {
        return new DrawToolsPanel();
    }

    public void setListener(DrawToolsListener l) {
        this.listener = l;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.panel_draw_tools, container, false);

        // ── Tool buttons ────────────────────────────────────────────────
        setupToolBtn(v, R.id.toolBrush, DrawingView.Tool.BRUSH);
        setupToolBtn(v, R.id.toolPen, DrawingView.Tool.PEN);
        setupToolBtn(v, R.id.toolHighlighter, DrawingView.Tool.HIGHLIGHTER);
        setupToolBtn(v, R.id.toolEraser, DrawingView.Tool.ERASER);
        setupToolBtn(v, R.id.toolLine, DrawingView.Tool.LINE);
        setupToolBtn(v, R.id.toolRect, DrawingView.Tool.RECT);
        setupToolBtn(v, R.id.toolCircle, DrawingView.Tool.CIRCLE);
        setupToolBtn(v, R.id.toolArrow, DrawingView.Tool.ARROW);

        // ── Color strip ─────────────────────────────────────────────────
        LinearLayout colorStrip = v.findViewById(R.id.drawColorStrip);
        for (int color : COLORS) {
            FrameLayout dot = makeDot(color);
            dot.setOnClickListener(view -> {
                selectedColor = color;
                if (listener != null) listener.onColorSelected(color);
            });
            colorStrip.addView(dot);
        }

        // ── Size seekbar ────────────────────────────────────────────────
        SeekBar sizeBar = v.findViewById(R.id.drawSizeBar);
        sizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                if (listener != null) listener.onSizeChanged(4 + p * 0.5f);
            }

            public void onStartTrackingTouch(SeekBar sb) {
            }

            public void onStopTrackingTouch(SeekBar sb) {
            }
        });

        // ── Opacity seekbar ─────────────────────────────────────────────
        SeekBar opacityBar = v.findViewById(R.id.drawOpacityBar);
        opacityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                if (listener != null) listener.onOpacityChanged(p / 100f);
            }

            public void onStartTrackingTouch(SeekBar sb) {
            }

            public void onStopTrackingTouch(SeekBar sb) {
            }
        });
        opacityBar.setProgress(100);

        // ── Action buttons ──────────────────────────────────────────────
        v.findViewById(R.id.btnDrawUndo).setOnClickListener(b -> {
            if (listener != null) listener.onUndo();
        });
        v.findViewById(R.id.btnDrawRedo).setOnClickListener(b -> {
            if (listener != null) listener.onRedo();
        });
        v.findViewById(R.id.btnDrawClear).setOnClickListener(b -> {
            if (listener != null) listener.onClear();
        });
        v.findViewById(R.id.btnDrawDone).setOnClickListener(b -> {
            if (listener != null) listener.onDone();
            dismiss();
        });

        return v;
    }

    private void setupToolBtn(View root, int viewId, DrawingView.Tool tool) {
        ImageView btn = root.findViewById(viewId);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            selectedTool = tool;
            if (listener != null) listener.onToolSelected(tool);
            highlightTool(root, viewId);
        });
    }

    private void highlightTool(View root, int selectedId) {
        int[] ids = {R.id.toolBrush, R.id.toolPen, R.id.toolHighlighter, R.id.toolEraser,
                R.id.toolLine, R.id.toolRect, R.id.toolCircle, R.id.toolArrow};
        for (int id : ids) {
            View v = root.findViewById(id);
            if (v != null) v.setAlpha(id == selectedId ? 1f : 0.45f);
        }
    }

    private FrameLayout makeDot(int color) {
        Context ctx = requireContext();
        FrameLayout frame = new FrameLayout(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(38), dp(38));
        lp.setMargins(dp(4), 0, dp(4), 0);
        frame.setLayoutParams(lp);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(dp(2), Color.BLACK);
        frame.setBackground(gd);
        return frame;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
