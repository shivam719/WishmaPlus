package com.infotech.wishmaplus.reels.ui.componets;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.infotech.wishmaplus.R;

public class FilterPanel extends BottomSheetDialogFragment {

    public interface FilterListener {
        void onFilterChanged(FilterEngine.FilterState state);
        void onFilterApplied(FilterEngine.FilterState state);
    }

    private FilterListener listener;

    // Working state (modified live by user)
    private FilterEngine.FilterState state = new FilterEngine.FilterState();

    // Snapshot before any edits — restored on Cancel
    private FilterEngine.FilterState originalState;

    private Bitmap        previewBitmap;
    private LinearLayout  presetStrip;

    // Slider references so we can reset them if needed
    private SeekBar sbBrightness, sbContrast, sbSaturation, sbWarmth;

    public static FilterPanel newInstance() { return new FilterPanel(); }

    public void setListener(FilterListener l)     { this.listener = l; }
    public void setPreviewBitmap(Bitmap bmp)      { this.previewBitmap = bmp; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.panel_filter, container, false);

        // Snapshot state before edits (for cancel)
        originalState = copyState(state);

        // ── Preset strip ─────────────────────────────────────────────────
        presetStrip = v.findViewById(R.id.filterPresetStrip);
        for (FilterEngine.Preset preset : FilterEngine.Preset.values()) {
            presetStrip.addView(buildPresetTile(preset));
        }

        // ── Sliders — each triggers notifyChange() immediately ────────────
        sbBrightness = v.findViewById(R.id.sliderBrightness);
        sbContrast   = v.findViewById(R.id.sliderContrast);
        sbSaturation = v.findViewById(R.id.sliderSaturation);
        sbWarmth     = v.findViewById(R.id.sliderWarmth);

        // Brightness: -100 to +100, initial 0
        setupSlider(sbBrightness, -100, 100, (int)(state.brightness * 100),
                val -> { state.brightness = val / 100f; notifyChange(); });

        // Contrast: 0 to 300, initial 100 (= 1.0f neutral)
        setupSlider(sbContrast, 0, 300, (int)(state.contrast * 100),
                val -> { state.contrast = val / 100f; notifyChange(); });

        // Saturation: 0 to 300, initial 100 (= 1.0f neutral)
        setupSlider(sbSaturation, 0, 300, (int)(state.saturation * 100),
                val -> { state.saturation = val / 100f; notifyChange(); });

        // Warmth: -100 to +100, initial 0
        setupSlider(sbWarmth, -100, 100, (int)(state.warmth * 100),
                val -> { state.warmth = val / 100f; notifyChange(); });

        // ── Apply button ─────────────────────────────────────────────────
        v.findViewById(R.id.btnFilterApply).setOnClickListener(b -> {
            if (listener != null) listener.onFilterApplied(state);
            dismiss();
        });

        // ── Cancel button — KEY FIX: notify listener to reset preview ────
        v.findViewById(R.id.btnFilterCancel).setOnClickListener(b -> {
            state = copyState(originalState);
            if (listener != null) listener.onFilterChanged(state); // resets preview
            dismiss();
        });

        return v;
    }

    // ── Notify live preview change ─────────────────────────────────────────────
    private void notifyChange() {
        if (listener != null) listener.onFilterChanged(state);
    }

    // ── Build preset tile ─────────────────────────────────────────────────────
    private View buildPresetTile(FilterEngine.Preset preset) {
        Context ctx = requireContext();

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(72), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        col.setLayoutParams(lp);

        ImageView img = new ImageView(ctx);
        img.setLayoutParams(new LinearLayout.LayoutParams(dp(64), dp(64)));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable clip = new android.graphics.drawable.GradientDrawable();
        clip.setCornerRadius(dp(8));
        img.setBackground(clip);
        img.setClipToOutline(true);
        img.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

        if (previewBitmap != null) {
            // Apply ONLY the preset (not sliders) to the tile thumbnail
            FilterEngine.FilterState tileState = new FilterEngine.FilterState();
            tileState.preset = preset;
            Bitmap filtered = FilterEngine.applyToBitmap(previewBitmap, tileState);
            img.setImageBitmap(filtered);
        } else {
            img.setBackgroundColor(0xFF333333);
        }

        TextView label = new TextView(ctx);
        label.setText(preset.name());
        label.setTextColor(0xCCFFFFFF);
        label.setTextSize(10f);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(4), 0, 0);

        col.addView(img);
        col.addView(label);

        col.setOnClickListener(view -> {
            state.preset = preset;
            notifyChange(); // KEY FIX: preset tap now triggers live preview
            highlightPreset(col);
        });
        return col;
    }

    private void highlightPreset(View selected) {
        if (presetStrip == null) return;
        for (int i = 0; i < presetStrip.getChildCount(); i++) {
            View child = presetStrip.getChildAt(i);
            boolean sel = child == selected;
            child.setAlpha(sel ? 1f : 0.55f);
            child.animate().scaleX(sel ? 1.08f : 1f).scaleY(sel ? 1.08f : 1f)
                    .setDuration(150).start();
        }
    }

    // ── Deep copy FilterState ─────────────────────────────────────────────────
    private FilterEngine.FilterState copyState(FilterEngine.FilterState src) {
        FilterEngine.FilterState copy = new FilterEngine.FilterState();
        copy.preset     = src.preset;
        copy.brightness = src.brightness;
        copy.contrast   = src.contrast;
        copy.saturation = src.saturation;
        copy.warmth     = src.warmth;
        return copy;
    }

    // ── Slider helper ──────────────────────────────────────────────────────────
    interface SliderCb { void onValue(int value); }

    private void setupSlider(SeekBar sb, int min, int max, int initial, SliderCb cb) {
        if (sb == null) return;
        sb.setMax(max - min);
        // Clamp initial to valid range
        int clamped = Math.max(0, Math.min(max - min, initial - min));
        sb.setProgress(clamped);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { cb.onValue(p + min); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s)  {}
        });
    }

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }
}