package com.infotech.wishmaplus.reels.bottomsheet;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;

/**
 * Templates Bottom Sheet
 * Shows predefined reel templates (aspect ratios, durations, styles)
 *
 */

public class TemplatesBottomSheet extends BottomSheetDialogFragment {

    public interface TemplateSelectedListener {
        void onTemplateSelected(TemplateModel template);
    }

    public static class TemplateModel {
        public final String name;
        public final String emoji;
        public final String description;
        public final int durationSec;
        public final float aspectRatio; // e.g. 9f/16f
        public final String style;     // "cinematic", "vlog", "story", etc.
        public final int accentColor;

        public TemplateModel(String name, String emoji, String description,
                             int durationSec, float aspectRatio, String style, int accentColor) {
            this.name = name;
            this.emoji = emoji;
            this.description = description;
            this.durationSec = durationSec;
            this.aspectRatio = aspectRatio;
            this.style = style;
            this.accentColor = accentColor;
        }
    }

    private TemplateSelectedListener listener;

    public static TemplatesBottomSheet newInstance() {
        return new TemplatesBottomSheet();
    }

    public void setListener(TemplateSelectedListener listener) {
        this.listener = listener;
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
        root.setPadding(dp(16), dp(20), dp(16), dp(40));

        // Header
        TextView title = new TextView(requireContext());
        title.setText("Templates");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18f);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.bottomMargin = dp(4);
        title.setLayoutParams(tlp);
        root.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Choose a style to get started quickly");
        subtitle.setTextColor(0x88FFFFFF);
        subtitle.setTextSize(13f);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = dp(20);
        subtitle.setLayoutParams(slp);
        root.addView(subtitle);

        // Category tabs
        String[] categories = {"All", "Trending", "Cinematic", "Vlog", "Story", "Music"};
        root.addView(buildCategoryTabs(categories));

        // Templates grid
        RecyclerView recycler = new RecyclerView(requireContext());
        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        TemplateAdapter adapter = new TemplateAdapter(buildTemplates(), template -> {
            if (listener != null) listener.onTemplateSelected(template);
            dismiss();
        });
        recycler.setAdapter(adapter);
        recycler.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(recycler);

        return root;
    }

    @SuppressLint("SetTextI18n")
    private View buildCategoryTabs(String[] categories) {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(requireContext());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hsvLp.bottomMargin = dp(16);
        hsv.setLayoutParams(hsvLp);

        LinearLayout strip = new LinearLayout(requireContext());
        strip.setOrientation(LinearLayout.HORIZONTAL);

        TextView[] tabs = new TextView[categories.length];
        for (int i = 0; i < categories.length; i++) {
            final int idx = i;
            TextView tab = new TextView(requireContext());
            tab.setText(categories[i]);
            tab.setTextSize(13f);
            tab.setPadding(dp(16), dp(7), dp(16), dp(7));
            tab.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tabLp.setMargins(0, 0, dp(8), 0);
            tab.setLayoutParams(tabLp);

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

    private void styleTab(TextView tab, boolean selected) {
        tab.setTextColor(selected ? 0xFFFFFFFF : 0x88FFFFFF);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(selected ? 0xFF1877F2 : 0x22FFFFFF);
        tab.setBackground(bg);
    }

    private List<TemplateModel> buildTemplates() {
        List<TemplateModel> list = new ArrayList<>();
        list.add(new TemplateModel("Cinematic", "🎬", "Wide letterbox style\n15 sec", 15, 16f/9f, "cinematic", 0xFF1A1A2E));
        list.add(new TemplateModel("Story", "📱", "Vertical story\n30 sec", 30, 9f/16f, "story", 0xFF16213E));
        list.add(new TemplateModel("Vlog", "🎥", "Vlog cut style\n60 sec", 60, 9f/16f, "vlog", 0xFF0F3460));
        list.add(new TemplateModel("Music Clip", "🎵", "Beat-sync cuts\n30 sec", 30, 9f/16f, "music", 0xFF533483));
        list.add(new TemplateModel("Quick Reel", "⚡", "Fast cuts\n15 sec", 15, 9f/16f, "quick", 0xFFE94560));
        list.add(new TemplateModel("Slideshow", "🖼️", "Image slideshow\n20 sec", 20, 9f/16f, "slideshow", 0xFF1DB954));
        list.add(new TemplateModel("Tutorial", "📚", "Step-by-step\n60 sec", 60, 9f/16f, "tutorial", 0xFFFF6B35));
        list.add(new TemplateModel("Travel", "✈️", "Travel montage\n45 sec", 45, 9f/16f, "travel", 0xFF00B4DB));
        return list;
    }

    private int dp(int val) {
        return (int) (val * requireContext().getResources().getDisplayMetrics().density);
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    private class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.VH> {
        private final List<TemplateModel> templates;
        private final TemplateSelectedListener cb;

        TemplateAdapter(List<TemplateModel> templates, TemplateSelectedListener cb) {
            this.templates = templates;
            this.cb = cb;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout card = new FrameLayout(requireContext());
            int size = (getResources().getDisplayMetrics().widthPixels - dp(16) * 2 - dp(12)) / 2;
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (size * 1.4f));
            cardLp.setMargins(dp(4), dp(4), dp(4), dp(4));
            card.setLayoutParams(cardLp);
            card.setClipToOutline(true);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setCornerRadius(dp(12));
            bg.setColor(0xFF222222);
            card.setBackground(bg);
            card.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            return new VH(card);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            TemplateModel t = templates.get(position);
            FrameLayout card = (FrameLayout) holder.itemView;
            card.removeAllViews();

            // Color bg with accent
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                    new int[]{t.accentColor, adjustBrightness(t.accentColor, 1.4f)}
            );
            bg.setCornerRadius(dp(12));
            card.setBackground(bg);

            // Emoji large
            TextView emoji = new TextView(requireContext());
            emoji.setText(t.emoji);
            emoji.setTextSize(36f);
            emoji.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elp.topMargin = dp(24);
            emoji.setLayoutParams(elp);
            card.addView(emoji);

            // Name
            TextView name = new TextView(requireContext());
            name.setText(t.name);
            name.setTextColor(0xFFFFFFFF);
            name.setTextSize(15f);
            name.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            nlp.bottomMargin = dp(28);
            name.setLayoutParams(nlp);
            card.addView(name);

            // Duration badge
            TextView dur = new TextView(requireContext());
            dur.setText(t.durationSec + "s");
            dur.setTextColor(0xCCFFFFFF);
            dur.setTextSize(10f);
            dur.setBackgroundColor(0x44000000);
            dur.setPadding(dp(6), dp(2), dp(6), dp(2));
            FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.END);
            dlp.setMargins(0, 0, dp(8), dp(8));
            dur.setLayoutParams(dlp);
            card.addView(dur);

            // Desc
            TextView desc = new TextView(requireContext());
            desc.setText(t.description);
            desc.setTextColor(0xAAFFFFFF);
            desc.setTextSize(11f);
            desc.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams deslp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            deslp.bottomMargin = dp(8);
            desc.setLayoutParams(deslp);
            card.addView(desc);

            card.setOnClickListener(v -> cb.onTemplateSelected(t));
        }

        @Override
        public int getItemCount() { return templates.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }
    }

    private int adjustBrightness(int color, float factor) {
        int r = Math.min(255, (int) (android.graphics.Color.red(color) * factor));
        int g = Math.min(255, (int) (android.graphics.Color.green(color) * factor));
        int b = Math.min(255, (int) (android.graphics.Color.blue(color) * factor));
        return android.graphics.Color.argb(android.graphics.Color.alpha(color), r, g, b);
    }
}
