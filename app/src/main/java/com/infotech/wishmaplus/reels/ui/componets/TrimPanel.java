package com.infotech.wishmaplus.reels.ui.componets;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  TrimPanel — FIXED VERSION                                   ║
 * ║                                                              ║
 * ║  BUG FIXES:                                                  ║
 * ║  1. TrimListener callback was being set AFTER dismiss()      ║
 * ║     so values were never delivered → fixed ordering          ║
 * ║  2. VideoView seekTo() was called before video was prepared  ║
 * ║     → now using OnPreparedListener                           ║
 * ║  3. Trim start/end were NOT forwarded to RenderEngine        ║
 * ║     → ReelEditorActivity.proceedToPost() fix included        ║
 * ║  4. Added real-time frame preview on SeekBar drag            ║
 * ║  5. FFmpeg -ss before -i for accurate trim (in RenderEngine) ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
public class TrimPanel extends BottomSheetDialogFragment {

    // ── Factory ───────────────────────────────────────────────────────────────
    private static final String ARG_PATH = "path";
    private static final String ARG_DUR = "dur";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private String videoPath;
    private long durationMs;
    private TrimListener listener;
    // State
    private long startMs = 0;
    private long endMs = 0;
    // UI
    private VideoView trimPreview;
    private ImageView framePreview;     // shows frame on scrub
    private LinearLayout thumbnailStrip;
    private View rangeLeft, rangeRight, rangeBar;
    private TextView tvStart, tvEnd, tvDuration;
    private SeekBar sbStart, sbEnd;

    public static TrimPanel newInstance(String path, long durationMs) {
        TrimPanel f = new TrimPanel();
        Bundle b = new Bundle();
        b.putString(ARG_PATH, path);
        b.putLong(ARG_DUR, durationMs);
        f.setArguments(b);
        return f;
    }

    public void setTrimListener(TrimListener l) {
        this.listener = l;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL,
                com.google.android.material.R.style.Theme_Material3_Dark_BottomSheetDialog);
        if (getArguments() != null) {
            videoPath = getArguments().getString(ARG_PATH, "");
            durationMs = getArguments().getLong(ARG_DUR, 60_000L);
        }
        endMs = durationMs; // default: full video
    }

    @Override
    public void onStart() {
        super.onStart();
        // Expand bottom sheet fully
        if (getDialog() != null) {
            FrameLayout sheet = getDialog().findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return buildUI();
    }

    // =========================================================================
    // UI BUILD
    // =========================================================================
    @SuppressLint("ClickableViewAccessibility")
    private View buildUI() {
        int dp4 = dp(4), dp8 = dp(8);
        int dp12 = dp(12), dp16 = dp(16), dp20 = dp(20);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0D0D0D);
        root.setPadding(dp16, dp20, dp16, dp(40));

        // ── Title row ────────────────────────────────────────────────────────
        LinearLayout titleRow = row(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        ((LinearLayout.LayoutParams) titleRow.getLayoutParams()).bottomMargin = dp16;

        TextView title = new TextView(requireContext());
        title.setText("Trim Video");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18f);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleLp);
        titleRow.addView(title);
        root.addView(titleRow);

        // ── Video preview ────────────────────────────────────────────────────
        FrameLayout previewContainer = new FrameLayout(requireContext());
        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(200));
        pcLp.bottomMargin = dp12;
        previewContainer.setLayoutParams(pcLp);
        previewContainer.setBackgroundColor(0xFF1A1A1A);

        trimPreview = new VideoView(requireContext());
        FrameLayout.LayoutParams vpLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        trimPreview.setLayoutParams(vpLp);
        previewContainer.addView(trimPreview);

        // Frame preview (shown while scrubbing, hidden during playback)
        framePreview = new ImageView(requireContext());
        framePreview.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        framePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        framePreview.setVisibility(View.GONE);
        previewContainer.addView(framePreview);

        root.addView(previewContainer);

        // ── Thumbnail strip ──────────────────────────────────────────────────
        thumbnailStrip = new LinearLayout(requireContext());
        thumbnailStrip.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        tsLp.bottomMargin = dp12;
        thumbnailStrip.setLayoutParams(tsLp);
        thumbnailStrip.setBackgroundColor(0xFF1A1A1A);
        root.addView(thumbnailStrip);
        loadThumbnailStrip();

        // ── Time labels ──────────────────────────────────────────────────────
        LinearLayout timeRow = row(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        ((LinearLayout.LayoutParams) timeRow.getLayoutParams()).bottomMargin = dp4;

        tvStart = label("00:00", Gravity.START);
        tvEnd = label(formatMs(durationMs), Gravity.END);
        tvDuration = label("Duration: " + formatMs(durationMs), Gravity.CENTER);

        tvStart.setLayoutParams(new LinearLayout.LayoutParams(dp(60),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tvEnd.setLayoutParams(new LinearLayout.LayoutParams(dp(60),
                ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams durLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvDuration.setLayoutParams(durLp);

        timeRow.addView(tvStart);
        timeRow.addView(tvDuration);
        timeRow.addView(tvEnd);
        root.addView(timeRow);

        // ── Start SeekBar ────────────────────────────────────────────────────
        TextView lbStart = new TextView(requireContext());
        lbStart.setText("Start");
        lbStart.setTextColor(0xFF00C7BE);
        lbStart.setTextSize(12f);
        lbStart.setPadding(0, dp8, 0, dp4);
        root.addView(lbStart);

        sbStart = new SeekBar(requireContext());
        sbStart.setMax(1000);
        sbStart.setProgress(0);
        sbStart.getThumb().setColorFilter(
                0xFF00C7BE, android.graphics.PorterDuff.Mode.SRC_IN);
        sbStart.getProgressDrawable().setColorFilter(
                0xFF00C7BE, android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams sbsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbsLp.bottomMargin = dp12;
        sbStart.setLayoutParams(sbsLp);
        root.addView(sbStart);

        // ── End SeekBar ──────────────────────────────────────────────────────
        TextView lbEnd = new TextView(requireContext());
        lbEnd.setText("End");
        lbEnd.setTextColor(0xFFFF9500);
        lbEnd.setTextSize(12f);
        lbEnd.setPadding(0, 0, 0, dp4);
        root.addView(lbEnd);

        sbEnd = new SeekBar(requireContext());
        sbEnd.setMax(1000);
        sbEnd.setProgress(1000);
        sbEnd.getThumb().setColorFilter(
                0xFFFF9500, android.graphics.PorterDuff.Mode.SRC_IN);
        sbEnd.getProgressDrawable().setColorFilter(
                0xFFFF9500, android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams sbeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbeLp.bottomMargin = dp(24);
        sbEnd.setLayoutParams(sbeLp);
        root.addView(sbEnd);

        // ── Buttons ──────────────────────────────────────────────────────────
        LinearLayout btnRow = row(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        btnRow.setGravity(Gravity.CENTER);

        TextView btnCancel = button("Cancel", 0xFF333333, 0xFFFFFFFF);
        TextView btnApply = button("Apply Trim", 0xFF1877F2, 0xFFFFFFFF);

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                0, dp(48), 1f);
        btnLp.setMargins(dp(6), 0, dp(6), 0);
        btnCancel.setLayoutParams(btnLp);
        btnApply.setLayoutParams(new LinearLayout.LayoutParams(btnLp));

        btnRow.addView(btnCancel);
        btnRow.addView(btnApply);
        root.addView(btnRow);

        // ── Setup video preview ───────────────────────────────────────────────
        setupVideoPreview();

        // ── SeekBar listeners ─────────────────────────────────────────────────
        sbStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (!fromUser) return;
                long newStart = (long) (p / 1000f * durationMs);
                // Don't allow start >= end
                if (newStart >= endMs - 1000) {
                    sbStart.setProgress((int) (endMs - 1000) * 1000 / (int) durationMs);
                    return;
                }
                startMs = newStart;
                tvStart.setText(formatMs(startMs));
                tvDuration.setText("Duration: " + formatMs(endMs - startMs));
                showFrameAt(startMs);          // ← FIX: show actual video frame
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                pausePreview();
                framePreview.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                framePreview.setVisibility(View.GONE);
                seekPreviewTo(startMs);        // seek video to start point
            }
        });

        sbEnd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (!fromUser) return;
                long newEnd = (long) (p / 1000f * durationMs);
                if (newEnd <= startMs + 1000) {
                    sbEnd.setProgress((int) ((startMs + 1000) * 1000 / durationMs));
                    return;
                }
                endMs = newEnd;
                tvEnd.setText(formatMs(endMs));
                tvDuration.setText("Duration: " + formatMs(endMs - startMs));
                showFrameAt(endMs);           // ← FIX: show actual video frame
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                pausePreview();
                framePreview.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                framePreview.setVisibility(View.GONE);
                seekPreviewTo(endMs - 500);
            }
        });

        // ── Button actions ────────────────────────────────────────────────────
        btnCancel.setOnClickListener(v -> dismiss());

        // FIX: deliver callback BEFORE dismiss so Activity receives the values
        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                // Ensure sane bounds
                long safeStart = Math.max(0, startMs);
                long safeEnd = Math.min(durationMs, endMs > 0 ? endMs : durationMs);
                listener.onTrimSet(safeStart, safeEnd);  // ← fires first
            }
            dismiss();                                    // ← then closes
        });

        return root;
    }

    // ── Video preview setup ────────────────────────────────────────────────────
    private void setupVideoPreview() {
        if (videoPath == null || videoPath.isEmpty()) return;
        try {
            trimPreview.setVideoURI(Uri.parse(videoPath));
            trimPreview.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                // FIX: seekTo called AFTER prepare, not before
                trimPreview.seekTo((int) startMs);
                trimPreview.start();
            });
            trimPreview.setOnErrorListener((mp, what, extra) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void seekPreviewTo(long ms) {
        try {
            if (trimPreview != null) {
                trimPreview.seekTo((int) ms);
                trimPreview.start();
            }
        } catch (Exception ignored) {
        }
    }

    private void pausePreview() {
        try {
            if (trimPreview != null && trimPreview.isPlaying()) trimPreview.pause();
        } catch (Exception ignored) {
        }
    }

    // ── Show single frame at timestamp ─────────────────────────────────────────
    private void showFrameAt(long posMs) {
        if (videoPath == null) return;
        executor.execute(() -> {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(videoPath);
                // getFrameAtTime works in microseconds
                Bitmap frame = mmr.getFrameAtTime(posMs * 1000L,
                        MediaMetadataRetriever.OPTION_CLOSEST);
                if (frame != null) {
                    uiHandler.post(() -> {
                        framePreview.setImageBitmap(frame);
                        framePreview.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    mmr.release();
                } catch (Exception ignored) {
                }
            }
        });
    }

    // ── Generate thumbnail strip ────────────────────────────────────────────────
    private void loadThumbnailStrip() {
        if (videoPath == null) return;
        int count = 10;
        executor.execute(() -> {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(videoPath);
                for (int i = 0; i < count; i++) {
                    long timeUs = (long) (i * durationMs / (float) count) * 1000L;
                    Bitmap bmp = mmr.getFrameAtTime(timeUs,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (bmp != null) {
                        Bitmap scaled = Bitmap.createScaledBitmap(bmp, dp(50), dp(50), false);
                        uiHandler.post(() -> {
                            ImageView iv = new ImageView(requireContext());
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                            iv.setLayoutParams(lp);
                            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            iv.setImageBitmap(scaled);
                            thumbnailStrip.addView(iv);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    mmr.release();
                } catch (Exception ignored) {
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private LinearLayout row(int w, int h) {
        LinearLayout ll = new LinearLayout(requireContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        return ll;
    }

    private TextView label(String text, int gravity) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xAAFFFFFF);
        tv.setTextSize(11f);
        tv.setGravity(gravity);
        return tv;
    }

    private TextView button(String text, int bgColor, int textColor) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(12));
        tv.setBackground(bg);
        return tv;
    }

    private String formatMs(long ms) {
        long s = ms / 1000;
        return String.format(java.util.Locale.getDefault(), "%d:%02d", s / 60, s % 60);
    }

    private int dp(int v) {
        return (int) (v * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
        if (trimPreview != null) {
            try {
                trimPreview.stopPlayback();
            } catch (Exception ignored) {
            }
        }
    }

    public interface TrimListener {
        void onTrimSet(long startMs, long endMs);
    }
}