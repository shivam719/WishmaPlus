package com.infotech.wishmaplus.reels.bottomsheet;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;

/**
 * Effects Bottom Sheet
 *
 * Categories:
 *  - Glitch / VHS / Noise
 *  - Blur / Focus
 *  - Light / Bokeh
 *  - Retro / Film
 *  - Motion Blur
 *  - Overlay (rain, snow, sparkle, etc.)
 *
 * Each effect has: name, emoji, intensity slider, preview
 */

public class EffectsBottomSheet extends BottomSheetDialogFragment {

    public interface EffectAppliedListener {
        void onEffectApplied(EffectModel effect);
        void onEffectCleared();
    }

    public static class EffectModel {
        public final String id;
        public final String name;
        public final String emoji;
        public final String category;
        public float intensity; // 0.0 – 1.0
        public final String description;

        public EffectModel(String id, String name, String emoji, String category,
                           float defaultIntensity, String description) {
            this.id = id;
            this.name = name;
            this.emoji = emoji;
            this.category = category;
            this.intensity = defaultIntensity;
            this.description = description;
        }
    }

    private EffectAppliedListener listener;
    private EffectModel selectedEffect = null;
    private Bitmap previewBitmap = null;
    private EffectAdapter adapter;

    private ImageView effectPreview;
    private SeekBar intensityBar;
    private TextView intensityLabel;
    private TextView selectedName;
    private View clearBtn;

    public static EffectsBottomSheet newInstance() {
        return new EffectsBottomSheet();
    }

    public void setListener(EffectAppliedListener listener) {
        this.listener = listener;
    }

    public void setPreviewBitmap(Bitmap bmp) {
        this.previewBitmap = bmp;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_Material3_Dark_BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF1A1A1A);
        root.setPadding(dp(16), dp(16), dp(16), dp(40));

        // ── Header ────────────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dp(16);
        header.setLayoutParams(hlp);

        TextView title = new TextView(requireContext());
        title.setText("Effects");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18f);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        clearBtn = buildChip("✕ Clear", 0x22FFFFFF);
        clearBtn.setVisibility(View.GONE);
        clearBtn.setOnClickListener(v -> {
            selectedEffect = null;
            clearBtn.setVisibility(View.GONE);
            selectedName.setText("None");
            intensityBar.setVisibility(View.GONE);
            updatePreview();
            adapter.setSelected(-1);
            if (listener != null) listener.onEffectCleared();
        });
        header.addView(clearBtn);

        root.addView(header);

        // ── Preview ───────────────────────────────────────────────────────
        effectPreview = new ImageView(requireContext());
        effectPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        android.graphics.drawable.GradientDrawable prevBg = new android.graphics.drawable.GradientDrawable();
        prevBg.setColor(0xFF111111);
        prevBg.setCornerRadius(dp(10));
        effectPreview.setBackground(prevBg);
        effectPreview.setClipToOutline(true);
        effectPreview.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(140));
        prevLp.bottomMargin = dp(12);
        effectPreview.setLayoutParams(prevLp);
        if (previewBitmap != null) effectPreview.setImageBitmap(previewBitmap);
        root.addView(effectPreview);

        // ── Selected effect / intensity ───────────────────────────────────
        LinearLayout selRow = new LinearLayout(requireContext());
        selRow.setOrientation(LinearLayout.HORIZONTAL);
        selRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.bottomMargin = dp(8);
        selRow.setLayoutParams(srLp);

        selectedName = new TextView(requireContext());
        selectedName.setText("None");
        selectedName.setTextColor(0xAAFFFFFF);
        selectedName.setTextSize(13f);
        selRow.addView(selectedName, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        intensityLabel = new TextView(requireContext());
        intensityLabel.setTextColor(0x66FFFFFF);
        intensityLabel.setTextSize(12f);
        selRow.addView(intensityLabel);

        root.addView(selRow);

        intensityBar = new SeekBar(requireContext());
        intensityBar.setMax(100);
        intensityBar.setProgress(60);
        intensityBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams ibLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ibLp.bottomMargin = dp(16);
        intensityBar.setLayoutParams(ibLp);
        intensityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (selectedEffect != null) {
                    selectedEffect.intensity = p / 100f;
                    intensityLabel.setText(p + "%");
                    updatePreview();
                    if (listener != null) listener.onEffectApplied(selectedEffect);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        root.addView(intensityBar);

        // ── Category tabs ─────────────────────────────────────────────────
        String[] cats = {"All", "Glitch", "Blur", "Light", "Retro", "Overlay"};
        root.addView(buildCategoryTabs(cats));

        // ── Effects grid ──────────────────────────────────────────────────
        RecyclerView recycler = new RecyclerView(requireContext());
        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        List<EffectModel> effects = buildEffects();
        adapter = new EffectAdapter(effects, effect -> {
            selectedEffect = effect;
            selectedName.setText(effect.emoji + " " + effect.name);
            intensityBar.setVisibility(View.VISIBLE);
            intensityBar.setProgress((int) (effect.intensity * 100));
            intensityLabel.setText((int)(effect.intensity * 100) + "%");
            clearBtn.setVisibility(View.VISIBLE);
            updatePreview();
            if (listener != null) listener.onEffectApplied(effect);
        });
        recycler.setAdapter(adapter);
        root.addView(recycler);

        return root;
    }

    private void updatePreview() {
        // Apply effect preview to bitmap — simplified: just show the original
        // Real implementation would use FilterEngine or RenderScript
        if (previewBitmap != null && effectPreview != null) {
            if (selectedEffect != null) {
                // Apply color matrix-based preview (basic simulation)
                android.graphics.ColorMatrix cm = buildEffectMatrix(selectedEffect);
                if (cm != null) {
                    android.graphics.ColorMatrixColorFilter filter = new android.graphics.ColorMatrixColorFilter(cm);
                    effectPreview.setColorFilter(filter);
                } else {
                    effectPreview.clearColorFilter();
                }
            } else {
                effectPreview.clearColorFilter();
            }
        }
    }

    private android.graphics.ColorMatrix buildEffectMatrix(EffectModel effect) {
        float i = effect.intensity;
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        switch (effect.id) {
            case "glitch":
                // Shift RGB channels
                cm.set(new float[]{
                        1+i, 0, 0, 0, 0,
                        0, 1, 0, 0, 0,
                        0, 0, 1-i*0.5f, 0, 0,
                        0, 0, 0, 1, 0
                });
                return cm;
            case "warm":
                cm.set(new float[]{
                        1+i*0.3f, 0, 0, 0, 0,
                        0, 1, 0, 0, 0,
                        0, 0, 1-i*0.3f, 0, 0,
                        0, 0, 0, 1, 0
                });
                return cm;
            case "cold":
                cm.set(new float[]{
                        1-i*0.3f, 0, 0, 0, 0,
                        0, 1, 0, 0, 0,
                        0, 0, 1+i*0.3f, 0, 0,
                        0, 0, 0, 1, 0
                });
                return cm;
            case "noir":
                cm.setSaturation(1f - i);
                return cm;
            case "vhs":
                // Low saturation + slight green tint
                android.graphics.ColorMatrix sat = new android.graphics.ColorMatrix();
                sat.setSaturation(0.6f);
                cm.set(new float[]{
                        1, 0, 0, 0, 0,
                        0, 1+i*0.1f, 0, 0, 0,
                        0, 0, 0.8f, 0, 0,
                        0, 0, 0, 1, 0
                });
                cm.preConcat(sat);
                return cm;
            default:
                return null;
        }
    }

    private View buildCategoryTabs(String[] cats) {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hsvLp.bottomMargin = dp(12);
        hsv.setLayoutParams(hsvLp);

        LinearLayout strip = new LinearLayout(requireContext());
        strip.setOrientation(LinearLayout.HORIZONTAL);
        TextView[] tabs = new TextView[cats.length];

        for (int i = 0; i < cats.length; i++) {
            final int idx = i;
            TextView tab = new TextView(requireContext());
            tab.setText(cats[i]);
            tab.setTextSize(12f);
            tab.setPadding(dp(14), dp(6), dp(14), dp(6));
            tab.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.setMargins(0, 0, dp(6), 0);
            tab.setLayoutParams(tlp);
            styleTab(tab, i == 0);
            tabs[i] = tab;
            tab.setOnClickListener(v -> {
                for (int j = 0; j < tabs.length; j++) {
                    if (tabs[j] != null) styleTab(tabs[j], j == idx);
                }
            });
            strip.addView(tab);
        }
        hsv.addView(strip);
        return hsv;
    }

    private void styleTab(TextView tab, boolean sel) {
        tab.setTextColor(sel ? 0xFFFFFFFF : 0x88FFFFFF);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(sel ? 0xFF1877F2 : 0x22FFFFFF);
        tab.setBackground(bg);
    }

    private View buildChip(String label, int color) {
        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextColor(0xCCFFFFFF);
        tv.setTextSize(12f);
        tv.setPadding(dp(12), dp(6), dp(12), dp(6));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(16));
        tv.setBackground(bg);
        return tv;
    }

    private List<EffectModel> buildEffects() {
        List<EffectModel> list = new ArrayList<>();
        // Glitch
        list.add(new EffectModel("glitch", "Glitch", "⚡", "Glitch", 0.6f, "RGB channel shift"));
        list.add(new EffectModel("vhs", "VHS", "📼", "Glitch", 0.7f, "Retro tape look"));
        list.add(new EffectModel("scan", "Scanlines", "📺", "Glitch", 0.5f, "CRT scanlines"));
        list.add(new EffectModel("noise", "Noise", "🌫️", "Glitch", 0.4f, "Film grain noise"));
        // Blur
        list.add(new EffectModel("blur_soft", "Soft", "💨", "Blur", 0.4f, "Gentle blur"));
        list.add(new EffectModel("blur_dreamy", "Dreamy", "✨", "Blur", 0.6f, "Glow blur"));
        list.add(new EffectModel("blur_tilt", "Tilt-Shift", "🔭", "Blur", 0.5f, "Miniature effect"));
        // Light
        list.add(new EffectModel("warm", "Warm", "🌅", "Light", 0.5f, "Warm tones"));
        list.add(new EffectModel("cold", "Cold", "❄️", "Light", 0.5f, "Cool blues"));
        list.add(new EffectModel("bokeh", "Bokeh", "🔮", "Light", 0.6f, "Soft light circles"));
        list.add(new EffectModel("lens_flare", "Lens Flare", "☀️", "Light", 0.7f, "Sunburst flare"));
        // Retro
        list.add(new EffectModel("noir", "Noir", "🎬", "Retro", 0.8f, "Black & white"));
        list.add(new EffectModel("sepia", "Sepia", "📷", "Retro", 0.7f, "Vintage brown"));
        list.add(new EffectModel("film", "Film", "🎞️", "Retro", 0.6f, "Film grain + fade"));
        list.add(new EffectModel("polaroid", "Polaroid", "🖼️", "Retro", 0.5f, "Faded corners"));
        // Overlay
        list.add(new EffectModel("rain", "Rain", "🌧️", "Overlay", 0.7f, "Falling rain drops"));
        list.add(new EffectModel("snow", "Snow", "❄️", "Overlay", 0.6f, "Snowfall overlay"));
        list.add(new EffectModel("sparkle", "Sparkle", "✨", "Overlay", 0.8f, "Sparkle particles"));
        list.add(new EffectModel("hearts", "Hearts", "💕", "Overlay", 0.6f, "Floating hearts"));
        return list;
    }

    private int dp(int val) {
        return (int) (val * requireContext().getResources().getDisplayMetrics().density);
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class EffectAdapter extends RecyclerView.Adapter<EffectAdapter.VH> {
        private final List<EffectModel> effects;
        private final EffectSelectedCb cb;
        private int selectedPos = -1;

        EffectAdapter(List<EffectModel> effects, EffectSelectedCb cb) {
            this.effects = effects;
            this.cb = cb;
        }

        void setSelected(int pos) {
            int prev = selectedPos;
            selectedPos = pos;
            if (prev >= 0) notifyItemChanged(prev);
            if (pos >= 0) notifyItemChanged(pos);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout card = new FrameLayout(requireContext());
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(80));
            cardLp.setMargins(dp(4), dp(4), dp(4), dp(4));
            card.setLayoutParams(cardLp);
            card.setClipToOutline(true);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(10));
            bg.setColor(0xFF222222);
            card.setBackground(bg);
            card.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            return new VH(card);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull VH holder, @SuppressLint("RecyclerView") int position) {
            EffectModel e = effects.get(position);
            FrameLayout card = (FrameLayout) holder.itemView;
            card.removeAllViews();

            boolean sel = position == selectedPos;
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(10));
            bg.setColor(sel ? 0x221877F2 : 0xFF222222);
            if (sel) bg.setStroke(dp(2), 0xFF1877F2);
            card.setBackground(bg);

            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            col.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView emojiTv = new TextView(requireContext());
            emojiTv.setText(e.emoji);
            emojiTv.setTextSize(24f);
            emojiTv.setGravity(Gravity.CENTER);
            col.addView(emojiTv);

            TextView nameTv = new TextView(requireContext());
            nameTv.setText(e.name);
            nameTv.setTextColor(sel ? 0xFF1877F2 : 0xAAFFFFFF);
            nameTv.setTextSize(10f);
            nameTv.setGravity(Gravity.CENTER);
            col.addView(nameTv);

            card.addView(col);
            card.setOnClickListener(v -> {
                int prev = selectedPos;
                selectedPos = position;
                if (prev >= 0) notifyItemChanged(prev);
                notifyItemChanged(position);
                cb.onEffect(e);
            });
        }

        @Override
        public int getItemCount() { return effects.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }
    }

    interface EffectSelectedCb { void onEffect(EffectModel e); }
}
