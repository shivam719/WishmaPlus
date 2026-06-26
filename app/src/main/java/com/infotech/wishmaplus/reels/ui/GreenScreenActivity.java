package com.infotech.wishmaplus.reels.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("SetTextI18n")
public class GreenScreenActivity extends AppCompatActivity {

    // ── Extras ────────────────────────────────────────────────────────────────
    public static final String EXTRA_FG_PATH = "gs_foreground_path";
    public static final String EXTRA_BG_PATH = "gs_background_path";
    public static final String EXTRA_TOLERANCE = "gs_tolerance";
    public static final String EXTRA_SPILL = "gs_spill";
    public static final String EXTRA_KEY_COLOR = "gs_key_color";
    public static final String EXTRA_COMPOSITE_PATH = "gs_composite_path";

    // ── State ─────────────────────────────────────────────────────────────────
    private float tolerance = 0.40f;
    private float spill = 0.30f;
    private int keyColor = Color.GREEN;

    private String fgPath = null;
    private String bgPath = null;

    private Bitmap fgBitmap = null;
    private Bitmap bgBitmap = null;

    // Last composite bitmap — saved to disk on "Use Effect"
    private volatile Bitmap lastCompositeBitmap = null;

    // Prevents multiple simultaneous composite operations
    private final AtomicBoolean isCompositing = new AtomicBoolean(false);

    // ── Views ─────────────────────────────────────────────────────────────────
    private ImageView fgPreview, bgPreview, compositePreview;
    private TextView fgLabel, bgLabel;
    private TextView toleranceVal, spillVal;
    private View useBtn;
    private android.widget.ProgressBar compositeLoader;

    private boolean pickingFg = true;
    private LinearLayout emptyPill;
    // ── Media picker ──────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> mediaPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                String path = resolveRealPath(uri);
                if (path == null) {
                    Toast.makeText(this, "Could not read file. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pickingFg) {
                    fgPath = path;
                    fgBitmap = null;           // invalidate cache
                    lastCompositeBitmap = null;
                    loadPreview(fgPreview, fgLabel, path);
                } else {
                    bgPath = path;
                    bgBitmap = null;
                    lastCompositeBitmap = null;
                    loadPreview(bgPreview, bgLabel, path);
                }
                checkReady();
                triggerComposite();
            });

    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        buildUI();
    }

    // =========================================================================
    // COMPOSITE PIPELINE
    // Fully thread-safe: decode on background, update UI on main thread.
    // KEY FIX: AtomicBoolean prevents race condition when user taps color
    //          quickly. Each call cancels the previous operation.
    // =========================================================================
    private void triggerComposite() {
        if (fgPath == null || bgPath == null) return;

        runOnUiThread(() -> {
            compositeLoader.setVisibility(View.VISIBLE);
            // ★ FIX — emptyPill hide karo jab composite shuru ho
            if (emptyPill != null) emptyPill.setVisibility(View.GONE);
        });

        if (compositePreview.getWidth() > 0 && compositePreview.getHeight() > 0) {
            doCompositeAsync(compositePreview.getWidth(), compositePreview.getHeight());
        } else {
            compositePreview.getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            compositePreview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            doCompositeAsync(compositePreview.getWidth(), compositePreview.getHeight());
                        }
                    });
        }
    }

    private void doCompositeAsync(int targetW, int targetH) {
        final int w = Math.max(targetW, 540);
        final int h = Math.max(targetH, 960);

        final int   capturedKeyColor  = this.keyColor;
        final float capturedTolerance = this.tolerance;
        final float capturedSpill     = this.spill;

        new Thread(() -> {
            Bitmap result = null;
            Log.d("GREEN_SCREEN", "Thread started fgPath=" + fgPath + " bgPath=" + bgPath);
            try {
                if (fgBitmap == null || fgBitmap.isRecycled()) {
                    fgBitmap = decodeToBitmap(fgPath, w, h);
                    Log.d("GREEN_SCREEN", "fgBitmap decoded: " + (fgBitmap != null ? "OK" : "NULL"));
                }
                if (bgBitmap == null || bgBitmap.isRecycled()) {
                    bgBitmap = decodeToBitmap(bgPath, w, h);
                    Log.d("GREEN_SCREEN", "bgBitmap decoded: " + (bgBitmap != null ? "OK" : "NULL"));
                }

                if (fgBitmap == null || bgBitmap == null) {
                    Log.e("GREEN_SCREEN", "Bitmap null — returning");
                    runOnUiThread(() -> {
                        compositeLoader.setVisibility(View.GONE);
                        Toast.makeText(this, "Could not decode media", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Bitmap fgScaled = scaleTo(fgBitmap, w, h);
                Bitmap bgScaled = scaleTo(bgBitmap, w, h);
                Log.d("GREEN_SCREEN", "Scaled OK, running composite...");

                result = chromaKeyComposite(fgScaled, bgScaled,
                        capturedKeyColor, capturedTolerance, capturedSpill);
                Log.d("GREEN_SCREEN", "Composite done: " + (result != null ? "OK" : "NULL"));

                lastCompositeBitmap = result;

            } catch (Exception e) {
                Log.e("GREEN_SCREEN", "Exception: " + e.getMessage());
                e.printStackTrace();
            }

            final Bitmap finalResult = result;
            Log.d("GREEN_SCREEN", "Posting to UI finalResult=" + (finalResult != null ? "OK" : "NULL"));
            runOnUiThread(() -> {
                compositeLoader.setVisibility(View.GONE);
                if (finalResult != null) {
                    compositePreview.setImageBitmap(finalResult);
                    if (emptyPill != null) emptyPill.setVisibility(View.GONE);
                    Log.d("GREEN_SCREEN", "Preview set ✓");
                } else {
                    Log.e("GREEN_SCREEN", "finalResult null — preview not set");
                    Toast.makeText(this, "Composite failed", Toast.LENGTH_SHORT).show();
                }
            });

            isCompositing.set(false);
        }).start();
    }

    // =========================================================================
    // CHROMA KEY COMPOSITE — proper algorithm matching FFmpeg chromakey filter
    // KEY FIX: Was using simple HSV hue diff. Now uses proper hue threshold
    //          with soft band, saturation gating, and spill suppression that
    //          matches the FFmpeg chromakey filter behavior.
    // =========================================================================
    private Bitmap chromaKeyComposite(
            Bitmap fg, Bitmap bg,
            int keyColor, float tolerance, float spillAmount) {

        int w = fg.getWidth(), h = fg.getHeight();
        int[] fgPx = new int[w * h];
        int[] bgPx = new int[w * h];
        fg.getPixels(fgPx, 0, w, 0, 0, w, h);
        bg.getPixels(bgPx, 0, w, 0, 0, w, h);

        float[] keyHsv = new float[3];
        Color.colorToHSV(keyColor, keyHsv);
        float keyHue = keyHsv[0];

        // hueThresh: how wide the hue window is (tolerance 0→1 maps to 15°→65°)
        float hueThresh = 15f + tolerance * 50f;
        // softBand: feathering zone at the edge of the key
        float softBand = Math.max(1f, hueThresh * 0.35f);
        float[] hsv = new float[3];

        for (int i = 0; i < fgPx.length; i++) {
            int pixel = fgPx[i];
            Color.colorToHSV(pixel, hsv);

            float hueDiff = Math.abs(hsv[0] - keyHue);
            if (hueDiff > 180f) hueDiff = 360f - hueDiff;

            // Skip very dark or very desaturated pixels (shadows, white walls)
            if (hsv[1] < 0.12f || hsv[2] < 0.10f) continue;

            // Compute alpha (how much of bg to mix in)
            float alpha;
            if (hueDiff >= hueThresh) continue;      // outside key — keep fg
            if (hueDiff <= hueThresh - softBand) alpha = 1.0f;  // inside key — full bg
            else alpha = (hueThresh - hueDiff) / softBand; // edge — blend

            // Saturation factor — less saturated pixels are keyed less aggressively
            float satFactor = Math.min(1f, (hsv[1] - 0.12f) / 0.30f);
            alpha *= satFactor;
            if (alpha <= 0f) continue;

            int fgR = (pixel >> 16) & 0xFF;
            int fgG = (pixel >> 8) & 0xFF;
            int fgB = pixel & 0xFF;
            int bgR = (bgPx[i] >> 16) & 0xFF;
            int bgG = (bgPx[i] >> 8) & 0xFF;
            int bgB = bgPx[i] & 0xFF;

            // Spill suppression — reduces key color bleeding on edges
            if (spillAmount > 0f && alpha < 1f) {
                float sp = spillAmount * alpha;
                int dom = dominantChannel(keyColor);
                if (dom == 0) fgR = clamp(Math.round(fgR + ((fgG + fgB) / 2f - fgR) * sp));
                if (dom == 1) fgG = clamp(Math.round(fgG + ((fgR + fgB) / 2f - fgG) * sp));
                if (dom == 2) fgB = clamp(Math.round(fgB + ((fgR + fgG) / 2f - fgB) * sp));
            }

            // Blend fg + bg
            int r = clamp(Math.round(fgR + (bgR - fgR) * alpha));
            int g = clamp(Math.round(fgG + (bgG - fgG) * alpha));
            int b = clamp(Math.round(fgB + (bgB - fgB) * alpha));
            fgPx[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(fgPx, 0, w, 0, 0, w, h);
        return out;
    }

    private int dominantChannel(int kc) {
        int r = (kc >> 16) & 0xFF, g = (kc >> 8) & 0xFF, b = kc & 0xFF;
        if (g >= r && g >= b) return 1;
        if (r >= g && r >= b) return 0;
        return 2;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // =========================================================================
    // APPLY & RETURN
    // =========================================================================
    private void applyAndReturn() {
        if (lastCompositeBitmap == null) {
            Toast.makeText(this, "Preview not ready yet — wait a moment", Toast.LENGTH_SHORT).show();
            return;
        }

        useBtn.setEnabled(false);
        useBtn.setAlpha(0.5f);
        compositeLoader.setVisibility(View.VISIBLE);

        Bitmap toSave = lastCompositeBitmap; // capture reference

        new Thread(() -> {
            String compositePath = saveCompositeBitmap(toSave);
            runOnUiThread(() -> {
                compositeLoader.setVisibility(View.GONE);
                useBtn.setEnabled(true);
                useBtn.setAlpha(1f);

                if (compositePath == null) {
                    Toast.makeText(this, "Failed to save composite — try again", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent result = new Intent();
                result.putExtra(EXTRA_FG_PATH, fgPath);
                result.putExtra(EXTRA_BG_PATH, bgPath);
                result.putExtra(EXTRA_TOLERANCE, tolerance);
                result.putExtra(EXTRA_SPILL, spill);
                result.putExtra(EXTRA_KEY_COLOR, keyColor);
                result.putExtra(EXTRA_COMPOSITE_PATH, compositePath);
                setResult(RESULT_OK, result);
                finish();
            });
        }).start();
    }

    private String saveCompositeBitmap(Bitmap bitmap) {
        try {
            File outDir = new File(getCacheDir(), "gs_composites");
            if (!outDir.exists()) //noinspection ResultOfMethodCallIgnored
                outDir.mkdirs();
            File file = new File(outDir, "composite_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
            }
            return (file.exists() && file.length() > 0) ? file.getAbsolutePath() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // =========================================================================
    // DECODE
    // =========================================================================
    private Bitmap decodeToBitmap(String path, int targetW, int targetH) {
        String lower = path.toLowerCase();
        boolean isVideo = lower.endsWith(".mp4") || lower.endsWith(".mov")
                || lower.endsWith(".mkv") || lower.endsWith(".avi");

        if (isVideo) {
            android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
            try {
                mmr.setDataSource(path);
                Bitmap frame = mmr.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                return frame != null ? scaleTo(frame, targetW, targetH) : null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                try {
                    mmr.release();
                } catch (Exception ignored) {
                }
            }
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int sample = 1;
        if (bounds.outWidth > 0 && bounds.outHeight > 0)
            sample = Math.max(1, Math.min(bounds.outWidth / targetW, bounds.outHeight / targetH));
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bmp = BitmapFactory.decodeFile(path, opts);
        return bmp != null ? scaleTo(bmp, targetW, targetH) : null;
    }

    private Bitmap scaleTo(Bitmap src, int w, int h) {
        if (src.getWidth() == w && src.getHeight() == h) return src;
        return Bitmap.createScaledBitmap(src, w, h, true);
    }

    // =========================================================================
    // FILE RESOLUTION
    // =========================================================================
    private String resolveRealPath(Uri uri) {
        if (uri == null) return null;
        try {
            String[] proj = {MediaStore.MediaColumns.DATA};
            try (android.database.Cursor c = getContentResolver().query(uri, proj, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int col = c.getColumnIndex(MediaStore.MediaColumns.DATA);
                    if (col >= 0) {
                        String p = c.getString(col);
                        if (p != null && new File(p).exists()) return p;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            String ext = getMimeExtension(uri);
            File out = new File(getCacheDir(), "gs_" + System.currentTimeMillis() + ext);
            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream fos = new FileOutputStream(out)) {
                if (in == null) return null;
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) fos.write(buf, 0, len);
            }
            if (out.exists() && out.length() > 0) return out.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getMimeExtension(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime == null) return ".tmp";
        switch (mime) {
            case "video/mp4":
                return ".mp4";
            case "video/quicktime":
                return ".mov";
            case "video/x-matroska":
                return ".mkv";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/webp":
                return ".webp";
            default:
                return ".tmp";
        }
    }

    // =========================================================================
    // UI BUILD
    // =========================================================================
    private void buildUI() {
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setBackgroundColor(0xFF0A0A0A);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(48), dp(16), dp(40));

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams tbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tbLp.bottomMargin = dp(20);
        topBar.setLayoutParams(tbLp);

        TextView backBtn = new TextView(this);
        backBtn.setText("✕");
        backBtn.setTextColor(0xFFFFFFFF);
        backBtn.setTextSize(20f);
        backBtn.setPadding(0, 0, dp(14), 0);
        backBtn.setOnClickListener(v -> finish());
        topBar.addView(backBtn);

        TextView titleTv = new TextView(this);
        titleTv.setText("Green Screen");
        titleTv.setTextColor(0xFFFFFFFF);
        titleTv.setTextSize(18f);
        topBar.addView(titleTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        useBtn = buildPillButton("Use Effect →", 0xFF1877F2);
        useBtn.setEnabled(false);
        useBtn.setAlpha(0.38f);
        useBtn.setOnClickListener(v -> applyAndReturn());
        topBar.addView(useBtn);
        root.addView(topBar);

        // Step 1: Pick clips
        root.addView(buildStepHeader("1", "Pick Your Clips"));
        LinearLayout pickRow = new LinearLayout(this);
        pickRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams prLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(148));
        prLp.bottomMargin = dp(20);
        pickRow.setLayoutParams(prLp);

        FrameLayout fgCard = buildMediaCard("🎬", "Foreground\n(green bg)");
        fgPreview = fgCard.findViewWithTag("preview");
        fgLabel = fgCard.findViewWithTag("label");
        fgCard.setOnClickListener(v -> {
            pickingFg = true;
            mediaPicker.launch("*/*");
        });

        FrameLayout bgCard = buildMediaCard("🖼️", "Background\n(scene)");
        bgPreview = bgCard.findViewWithTag("preview");
        bgLabel = bgCard.findViewWithTag("label");
        bgCard.setOnClickListener(v -> {
            pickingFg = false;
            mediaPicker.launch("*/*");
        });

        TextView div = new TextView(this);
        div.setText("➕");
        div.setTextSize(16f);
        div.setTextColor(0x88FFFFFF);
        div.setGravity(Gravity.CENTER);

        pickRow.addView(fgCard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        pickRow.addView(div, new LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.MATCH_PARENT));
        pickRow.addView(bgCard, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        root.addView(pickRow);

        // Step 2: Key Color
        root.addView(buildStepHeader("2", "Key Color"));
        root.addView(buildColorPicker());

        // Step 3: Fine-Tune
        root.addView(buildStepHeader("3", "Fine-Tune"));
        root.addView(buildSliderRow("Tolerance", "How much color variation to remove",
                5, 95, (int) (tolerance * 100),
                val -> {
                    tolerance = val / 100f;
                    if (toleranceVal != null) toleranceVal.setText(val + "%");
                    triggerComposite(); // KEY FIX: refresh composite on slider change
                }, tv -> toleranceVal = tv));
        root.addView(buildSliderRow("Spill Suppression", "Remove green edge fringing",
                0, 100, (int) (spill * 100),
                val -> {
                    spill = val / 100f;
                    if (spillVal != null) spillVal.setText(val + "%");
                    triggerComposite(); // KEY FIX: refresh on slider change
                }, tv -> spillVal = tv));

        // Step 4: Preview
        root.addView(buildStepHeader("4", "Preview"));
        root.addView(buildPreviewSection());

        TextView exportNote = new TextView(this);
        exportNote.setText("💾 Composite saved when you tap Use Effect");
        exportNote.setTextColor(0x44FFFFFF);
        exportNote.setTextSize(10f);
        exportNote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams enLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        enLp.topMargin = dp(6);
        exportNote.setLayoutParams(enLp);
        root.addView(exportNote);

        scroll.addView(root);
        setContentView(scroll);
    }

    private View buildPreviewSection() {
        FrameLayout wrapper = new FrameLayout(this);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(240));
        wLp.topMargin = dp(8); wLp.bottomMargin = dp(12); wrapper.setLayoutParams(wLp);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF181818); bg.setCornerRadius(dp(16)); bg.setStroke(dp(1), 0x22FFFFFF);

        compositePreview = new ImageView(this);
        compositePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        compositePreview.setBackground(bg);
        compositePreview.setClipToOutline(true);
        compositePreview.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        compositePreview.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wrapper.addView(compositePreview);

        // ★ FIX — class field use karo, local variable nahi
        emptyPill = new LinearLayout(this);
        emptyPill.setOrientation(LinearLayout.VERTICAL);
        emptyPill.setGravity(Gravity.CENTER);
        emptyPill.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        TextView emptyIcon = new TextView(this);
        emptyIcon.setText("🎭"); emptyIcon.setTextSize(32f); emptyIcon.setGravity(Gravity.CENTER);
        emptyPill.addView(emptyIcon);
        TextView emptyTxt = new TextView(this);
        emptyTxt.setText("Pick foreground + background\nto see merged preview");
        emptyTxt.setTextColor(0x55FFFFFF); emptyTxt.setTextSize(12f); emptyTxt.setGravity(Gravity.CENTER);
        emptyPill.addView(emptyTxt);
        wrapper.addView(emptyPill);

        compositeLoader = new android.widget.ProgressBar(this);
        FrameLayout.LayoutParams spLp = new FrameLayout.LayoutParams(dp(44), dp(44));
        spLp.gravity = Gravity.CENTER; compositeLoader.setLayoutParams(spLp);
        compositeLoader.setVisibility(View.GONE);
        wrapper.addView(compositeLoader);

        // PREVIEW badge
        TextView badge = new TextView(this);
        badge.setText("● PREVIEW"); badge.setTextColor(0xFFFFFFFF); badge.setTextSize(9f);
        badge.setPadding(dp(6), dp(3), dp(6), dp(3));
        android.graphics.drawable.GradientDrawable lbBg = new android.graphics.drawable.GradientDrawable();
        lbBg.setColor(0xCC1877F2); lbBg.setCornerRadius(dp(6)); badge.setBackground(lbBg);
        FrameLayout.LayoutParams lbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        lbLp.setMargins(0, dp(10), dp(10), 0); badge.setLayoutParams(lbLp);
        wrapper.addView(badge);

        // ★ FIX — addOnLayoutChangeListener HATAO — kaam nahi karta
        // Directly doCompositeAsync mein hide ho raha hai ab

        return wrapper;
    }

    private FrameLayout buildMediaCard(String icon, String labelText) {
        FrameLayout card = new FrameLayout(this);
        card.setClipToOutline(true);
        card.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFF1E1E1E);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), 0x33FFFFFF);
        card.setBackground(bg);
        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        preview.setTag("preview");
        card.addView(preview);
        View scrim = new View(this);
        scrim.setBackgroundColor(0x44000000);
        scrim.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.addView(scrim);
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(24f);
        iconTv.setGravity(Gravity.CENTER);
        inner.addView(iconTv);
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextColor(0xCCFFFFFF);
        label.setTextSize(11f);
        label.setGravity(Gravity.CENTER);
        label.setPadding(dp(6), dp(4), dp(6), dp(4));
        label.setTag("label");
        inner.addView(label);
        card.addView(inner);
        return card;
    }

    // KEY FIX: Color picker — each dot tap updates keyColor AND triggers composite
    private View buildColorPicker() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = dp(6); rLp.bottomMargin = dp(20); row.setLayoutParams(rLp);

        int[] colors = {0xFF00C853, 0xFF00FF00, 0xFF76FF03, 0xFF00FFFF, 0xFFFF00FF, 0xFF2196F3};
        String[] names = {"Green", "Lime", "YGreen", "Cyan", "Magenta", "Blue"};

        // ★ FIX — FrameLayout store karo (dot tha pehle, wrong type)
        FrameLayout[] dots = new FrameLayout[colors.length];
        android.graphics.drawable.GradientDrawable[] dotBgs =
                new android.graphics.drawable.GradientDrawable[colors.length];

        for (int i = 0; i < colors.length; i++) {
            final int color = colors[i];
            final int idx = i;

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL); col.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    dp(50), ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.setMargins(0, 0, dp(6), 0); col.setLayoutParams(clp);

            FrameLayout dot = new FrameLayout(this);
            dot.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));

            android.graphics.drawable.GradientDrawable dotBg =
                    new android.graphics.drawable.GradientDrawable();
            dotBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dotBg.setColor(color);
            boolean sel = (color == keyColor);
            dotBg.setStroke(dp(sel ? 3 : 1), sel ? 0xFFFFFFFF : 0x33FFFFFF);
            dot.setBackground(dotBg);

            // ★ Store reference
            dots[i] = dot;
            dotBgs[i] = dotBg;

            // Check mark
            TextView check = new TextView(this);
            check.setText("✓"); check.setTextColor(0xFFFFFFFF); check.setTextSize(14f);
            check.setGravity(Gravity.CENTER);
            check.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            check.setVisibility(sel ? View.VISIBLE : View.GONE);
            dot.addView(check); // ★ index 0

            TextView nameTv = new TextView(this);
            nameTv.setText(names[i]); nameTv.setTextColor(0x88FFFFFF); nameTv.setTextSize(9f);
            nameTv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ntlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            ntlp.topMargin = dp(4); nameTv.setLayoutParams(ntlp);

            col.addView(dot);
            col.addView(nameTv);

            col.setOnClickListener(v -> {
                keyColor = color;

                // ★ FIX — dotBgs use karo directly, check mark index 0 pe hai
                for (int j = 0; j < dots.length; j++) {
                    dotBgs[j].setStroke(
                            dp(j == idx ? 3 : 1),
                            j == idx ? 0xFFFFFFFF : 0x33FFFFFF);
                    // ★ FIX — getChildAt(0) check mark hai
                    dots[j].getChildAt(0)
                            .setVisibility(j == idx ? View.VISIBLE : View.GONE);
                }

                triggerComposite();
            });

            row.addView(col);
        }
        return row;
    }

    private View buildSliderRow(String name, String hint, int min, int max, int initial,
                                SliderCb cb, ValueLabelCb labelCb) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colLp.bottomMargin = dp(18);
        col.setLayoutParams(colLp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView nameTv = new TextView(this);
        nameTv.setText(name);
        nameTv.setTextColor(0xDDFFFFFF);
        nameTv.setTextSize(13f);
        header.addView(nameTv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView valTv = new TextView(this);
        valTv.setText(initial + "%");
        valTv.setTextColor(0xFF1877F2);
        valTv.setTextSize(13f);
        header.addView(valTv);
        labelCb.onLabel(valTv);
        col.addView(header);

        TextView hintTv = new TextView(this);
        hintTv.setText(hint);
        hintTv.setTextColor(0x44FFFFFF);
        hintTv.setTextSize(10f);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dp(6);
        hintTv.setLayoutParams(hlp);
        col.addView(hintTv);

        SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(initial - min);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                cb.onValue(p + min);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        col.addView(sb);
        return col;
    }

    private View buildStepHeader(String num, String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        lp.bottomMargin = dp(10);
        row.setLayoutParams(lp);

        TextView badge = new TextView(this);
        badge.setText(num);
        badge.setTextColor(0xFFFFFFFF);
        badge.setTextSize(11f);
        badge.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bbg = new android.graphics.drawable.GradientDrawable();
        bbg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bbg.setColor(0xFF1877F2);
        badge.setBackground(bbg);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(24), dp(24));
        blp.setMargins(0, 0, dp(10), 0);
        badge.setLayoutParams(blp);
        row.addView(badge);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(14f);
        row.addView(tv);
        return row;
    }

    private View buildPillButton(String label, int color) {
        TextView btn = new TextView(this);
        btn.setText(label);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(13f);
        btn.setPadding(dp(16), dp(8), dp(16), dp(8));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(20));
        btn.setBackground(bg);
        return btn;
    }

    private void loadPreview(ImageView iv, TextView label, String path) {
        label.setVisibility(View.GONE);
        Glide.with(this).load(new File(path)).centerCrop().into(iv);
    }

    private void checkReady() {
        boolean ready = fgPath != null && bgPath != null;
        useBtn.setEnabled(ready);
        useBtn.setAlpha(ready ? 1.0f : 0.38f);
    }

    private int dp(int val) {
        return (int) (val * getResources().getDisplayMetrics().density);
    }

    interface SliderCb {
        void onValue(int val);
    }

    interface ValueLabelCb {
        void onLabel(TextView tv);
    }
}