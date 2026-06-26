package com.infotech.wishmaplus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.infotech.wishmaplus.Api.Response.MediaModel;
import com.infotech.wishmaplus.reels.ReelRenderEngine.ReelRenderEngine;
import com.infotech.wishmaplus.reels.adapter.EmojiAdapter;
import com.infotech.wishmaplus.reels.bottomsheet.AudioMixerBottomSheet;
import com.infotech.wishmaplus.reels.bottomsheet.DraftsBottomSheet;
import com.infotech.wishmaplus.reels.bottomsheet.EffectsBottomSheet;
import com.infotech.wishmaplus.reels.bottomsheet.SpeedControlBottomSheet;
import com.infotech.wishmaplus.reels.data.EmojiData;
import com.infotech.wishmaplus.reels.ui.componets.DrawToolsPanel;
import com.infotech.wishmaplus.reels.ui.componets.DrawingView;
import com.infotech.wishmaplus.reels.ui.componets.FilterEngine;
import com.infotech.wishmaplus.reels.ui.componets.FilterPanel;
import com.infotech.wishmaplus.reels.ui.componets.RenderProgressDialog;
import com.infotech.wishmaplus.reels.ui.componets.ServerMusicPickerBottomSheet;
import com.infotech.wishmaplus.reels.ui.componets.TrimPanel;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReelEditorActivity extends AppCompatActivity {

    private static final String[] FONT_FAMILIES = {
            "sans-serif-medium", "serif", "sans-serif-condensed", "monospace", "sans-serif-light", "casual"
    };
    private static final String[] FONT_LABELS = {
            "Modern", "Serif", "Condensed", "Mono", "Light", "Casual"
    };
    private static final int[] TEXT_COLORS = {
            0xFFFFFFFF, 0xFF000000, 0xFFFF3B30, 0xFFFF9500, 0xFFFFCC00,
            0xFF34C759, 0xFF00C7BE, 0xFF007AFF, 0xFF5856D6, 0xFFFF2D55,
            0xFFFF6B6B, 0xFFFFE66D, 0xFF4ECDC4, 0xFF45B7D1, 0xFF96CEB4,
            0xFFDDA0DD, 0xFFF7DC6F, 0xFFBB8FCE, 0xFFF0B27A, 0xFF82E0AA
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // ── Views ─────────────────────────────────────────────────────────────────
    private VideoView videoView;
    private ImageView imagePreview;
    private FrameLayout compositeCanvas, overlayCanvas;
    private FrameLayout textEditorPanel, emojiPanel, stickerPanel, trashZone;
    private LinearLayout clipStrip;
    private EditText textInput;
    private LinearLayout colorStripLayout, fontStripLayout;
    private SeekBar textSizeSeekBar;
    private ImageView textAlignBtn;
    private RecyclerView emojiRecycler, stickerRecycler;
    private LinearLayout emojiCategoryStrip, stickerCategoryStrip;
    private EmojiAdapter emojiAdapter;
    private StickerAdapter stickerAdapter;
    private View playPauseIndicator;
    private ImageView playPauseIcon;

    // ── Template banner ───────────────────────────────────────────────────────
    private TextView templateBanner;

    // ── State ─────────────────────────────────────────────────────────────────
    private List<MediaModel> mediaList = new ArrayList<>();
    private int currentMediaIndex = 0;
    private int currentTextColor = Color.WHITE;
    private int currentTextAlign = Gravity.CENTER;
    private int currentTextBgMode = 0;
    private int currentFontIndex = 0;
    private float currentTextSize = 26f;
    private boolean isVideoPlaying = false;
    private boolean isTrashVisible = false;
    private TextView editingTextView = null;

    private String selectedMusicPath = null;
    private long musicStartMs = 0, musicEndMs = 0;

    private ReelRenderEngine renderEngine;
    private RenderProgressDialog renderDialog;
    DrawingView drawingView;
    boolean isDrawMode = false;

    FilterEngine.FilterState filterState = new FilterEngine.FilterState();
    float currentSpeed = 1.0f;
    long trimStartMs = 0, trimEndMs = 0;
    float originalAudioVolume = 1.0f, musicAudioVolume = 0.8f;
    boolean originalMuted = false, musicMuted = false;
    String currentDraftId = null;

    // Green screen
    String gsBgPath = null;
    float gsTolerance = 0.4f;
    float gsSpill = 0.3f;
    String gsFgPath = null;
    int gsKeyColor = android.graphics.Color.GREEN;
    String gsCompositePath = null;

    // Effect
    EffectsBottomSheet.EffectModel currentEffect = null;

    // Template
    String templateName = null;
    String finalPageId = null;
    String templateStyle = null;
    String templateEmoji = null;
    int templateDuration = 0;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat ctrl =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        ctrl.hide(WindowInsetsCompat.Type.systemBars());
        ctrl.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_reel_editor);

        readIntentExtras();
        bindViews();
        setupTopBar();
        setupRightTools();
        setupTextEditor();
        buildColorStrip();
        buildFontStrip();
        buildClipStrip();
        setupEmojiPanel();
        setupStickerPanel();
        setupBottomActions();
        setupVideoTapToPause();
        loadMediaAtIndex(0);

        // Show template banner if launched from template
        applyTemplateIfPresent();
    }

    // ── Read intent ───────────────────────────────────────────────────────────
    private void readIntentExtras() {
        Intent i = getIntent();
        finalPageId = i.getStringExtra("pageId");
        if (i.hasExtra("draft_id")) currentDraftId = i.getStringExtra("draft_id");
        if (i.hasExtra("media_list")) {
            Object obj = i.getSerializableExtra("media_list");
            if (obj instanceof ArrayList) //noinspection unchecked
                mediaList = (ArrayList<MediaModel>) obj;
        }

        // ★ MUSIC — intent se receive karo
        if (i.hasExtra("music_path")) {
            selectedMusicPath = i.getStringExtra("music_path");
            musicStartMs = i.getLongExtra("music_start", 0);
            musicEndMs = i.getLongExtra("music_end", 0);
            // music_title sirf display ke liye
        }

        // Green screen
        if (i.hasExtra("gs_bg_path")) gsBgPath = i.getStringExtra("gs_bg_path");
        if (i.hasExtra("gs_fg_path")) gsFgPath = i.getStringExtra("gs_fg_path");
        if (i.hasExtra("gs_tolerance")) gsTolerance = i.getFloatExtra("gs_tolerance", 0.4f);
        if (i.hasExtra("gs_spill")) gsSpill = i.getFloatExtra("gs_spill", 0.3f);
        if (i.hasExtra("gs_key_color"))
            gsKeyColor = i.getIntExtra("gs_key_color", android.graphics.Color.GREEN);
        if (i.hasExtra("gs_composite_path"))
            gsCompositePath = i.getStringExtra("gs_composite_path");

        // Template
        if (i.hasExtra("template_name")) templateName = i.getStringExtra("template_name");
        if (i.hasExtra("template_style")) templateStyle = i.getStringExtra("template_style");
        if (i.hasExtra("template_emoji")) templateEmoji = i.getStringExtra("template_emoji");
        if (i.hasExtra("template_duration"))
            templateDuration = i.getIntExtra("template_duration", 0);

        // Effect
        if (i.hasExtra("effect_name")) {
            String eName = i.getStringExtra("effect_name");
            String eEmoji = i.getStringExtra("effect_emoji");
            if (eName != null) {
                currentEffect = new EffectsBottomSheet.EffectModel(
                        eName, eName,
                        eEmoji != null ? eEmoji : "✨",
                        "", 1.0f, ""
                );
            }
        }
    }
    /*private void readIntentExtras() {
        Intent i = getIntent();
        finalPageId = i.getStringExtra("pageId");
        if (i.hasExtra("draft_id")) currentDraftId = i.getStringExtra("draft_id");
        if (i.hasExtra("media_list")) {
            Object obj = i.getSerializableExtra("media_list");
            if (obj instanceof ArrayList) //noinspection unchecked
                mediaList = (ArrayList<MediaModel>) obj;
        }
        // Green screen
        if (i.hasExtra("gs_bg_path")) gsBgPath = i.getStringExtra("gs_bg_path");
        if (i.hasExtra("gs_fg_path")) gsFgPath = i.getStringExtra("gs_fg_path");
        if (i.hasExtra("gs_tolerance")) gsTolerance = i.getFloatExtra("gs_tolerance", 0.4f);
        if (i.hasExtra("gs_spill")) gsSpill = i.getFloatExtra("gs_spill", 0.3f);
        if (i.hasExtra("gs_key_color"))
            gsKeyColor = i.getIntExtra("gs_key_color", android.graphics.Color.GREEN);
        if (i.hasExtra("gs_composite_path"))
            gsCompositePath = i.getStringExtra("gs_composite_path");

        // Template
        if (i.hasExtra("template_name")) templateName = i.getStringExtra("template_name");
        if (i.hasExtra("template_style")) templateStyle = i.getStringExtra("template_style");
        if (i.hasExtra("template_emoji")) templateEmoji = i.getStringExtra("template_emoji");
        if (i.hasExtra("template_duration"))
            templateDuration = i.getIntExtra("template_duration", 0);

        // Effect — passed from CreateReelActivity
        if (i.hasExtra("effect_name")) {
            String eName = i.getStringExtra("effect_name");
            String eEmoji = i.getStringExtra("effect_emoji");
            if (eName != null) {
                currentEffect = new EffectsBottomSheet.EffectModel(
                        eName,                           // id
                        eName,                           // name
                        eEmoji != null ? eEmoji : "✨",  // emoji
                        "",                              // category
                        1.0f,                            // defaultIntensity
                        ""                               // description
                );
            }
        }
    }*/

    // ── Template banner ───────────────────────────────────────────────────────
    private void applyTemplateIfPresent() {
        if (templateName == null) return;

        // Show a floating banner at top of editor
        if (templateBanner != null) {
            String label = (templateEmoji != null ? templateEmoji + " " : "") + templateName;
            templateBanner.setText(label);
            templateBanner.setVisibility(View.VISIBLE);
            // Auto-hide after 3 seconds
            mainHandler.postDelayed(() -> {
                templateBanner.animate().alpha(0f).setDuration(400)
                        .withEndAction(() -> templateBanner.setVisibility(View.GONE)).start();
            }, 3000);
        }

        // Apply template duration to render engine
        if (templateDuration > 0) {
            renderEngine.setTemplate(templateName, templateDuration);
        }
    }

    // ── Bind views ────────────────────────────────────────────────────────────
    private void bindViews() {
        renderEngine = new ReelRenderEngine(this);
        renderDialog = new RenderProgressDialog(this);
        videoView = findViewById(R.id.videoView);
        imagePreview = findViewById(R.id.imagePreview);
        compositeCanvas = findViewById(R.id.compositeCanvas);
        overlayCanvas = findViewById(R.id.overlayCanvas);
        textEditorPanel = findViewById(R.id.textEditorPanel);
        emojiPanel = findViewById(R.id.emojiPanel);
        stickerPanel = findViewById(R.id.stickerPanel);
        trashZone = findViewById(R.id.trashZone);
        clipStrip = findViewById(R.id.clipStrip);
        textInput = findViewById(R.id.textInput);
        colorStripLayout = findViewById(R.id.colorStrip);
        fontStripLayout = findViewById(R.id.fontStyleStrip);
        textSizeSeekBar = findViewById(R.id.textSizeSeek);
        textAlignBtn = findViewById(R.id.textAlignBtn);
        emojiRecycler = findViewById(R.id.emojiRecycler);
        stickerRecycler = findViewById(R.id.stickerRecycler);
        emojiCategoryStrip = findViewById(R.id.emojiCategoryStrip);
        stickerCategoryStrip = findViewById(R.id.stickerCategoryStrip);
        playPauseIndicator = findViewById(R.id.playPauseIndicator);
        playPauseIcon = findViewById(R.id.playPauseIcon);

    }

    // =========================================================================
    // TOP BAR
    // =========================================================================
   /* private void setupTopBar() {
        findViewById(R.id.btnClose).setOnClickListener(v -> showExitDialog());
        findViewById(R.id.btnClose).setOnLongClickListener(v -> {
            saveDraft();
            return true;
        });
        findViewById(R.id.btnMusic).setOnClickListener(v -> {
            ServerMusicPickerBottomSheet sheet = ServerMusicPickerBottomSheet.newInstance(
                    (path, start, end, title) -> {
                        selectedMusicPath = path;
                        musicStartMs = start;
                        musicEndMs = end;
                        ((ImageView) findViewById(R.id.musicIcon)).setColorFilter(0xFF1877F2);
                        showToast("Music: " + title);
                    });
            sheet.show(getSupportFragmentManager(), "music_picker");
        });
        findViewById(R.id.btnNext).setOnClickListener(v -> proceedToPost());
    }*/
    private void setupTopBar() {
        findViewById(R.id.btnClose).setOnClickListener(v -> showExitDialog());
        findViewById(R.id.btnClose).setOnLongClickListener(v -> {
            saveDraft();
            return true;
        });

        // ★ Agar music pehle se intent se aayi hai to icon highlight karo
        if (selectedMusicPath != null) {
            ((ImageView) findViewById(R.id.musicIcon)).setColorFilter(0xFF1877F2);
        }

        findViewById(R.id.btnMusic).setOnClickListener(v -> {
            ServerMusicPickerBottomSheet sheet = ServerMusicPickerBottomSheet.newInstance(
                    (path, start, end, title) -> {
                        selectedMusicPath = path;
                        musicStartMs = start;
                        musicEndMs = end;
                        ((ImageView) findViewById(R.id.musicIcon)).setColorFilter(0xFF1877F2);
                        showToast("Music: " + title);
                    });
            sheet.show(getSupportFragmentManager(), "music_picker");
        });

        findViewById(R.id.btnNext).setOnClickListener(v -> proceedToPost());
    }

    private void showExitDialog() {
        if (mediaList == null || mediaList.isEmpty()) {
            finish();
            return;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Save draft?")
                .setMessage("Save your progress as a draft to continue later.")
                .setPositiveButton("Save Draft", (d, w) -> {
                    saveDraft();
                    finish();
                })
                .setNegativeButton("Discard", (d, w) -> finish())
                .setNeutralButton("Cancel", null).show();
    }

    private void saveDraft() {
        if (mediaList == null || mediaList.isEmpty()) return;
        DraftsBottomSheet.DraftModel draft = new DraftsBottomSheet.DraftModel();
        draft.id = currentDraftId != null ? currentDraftId : "draft_" + System.currentTimeMillis();
        draft.createdAt = System.currentTimeMillis();
        draft.mediaCount = mediaList.size();
        draft.title = mediaList.size() == 1 ? "1 clip" : mediaList.size() + " clips";
        for (MediaModel m : mediaList) draft.mediaPaths.add(m.getPath());
        if (!mediaList.isEmpty()) {
            MediaModel first = mediaList.get(0);
            if (first.isVideo()) {
                Bitmap frame = extractVideoFrame(first.getPath());
                if (frame != null) draft.thumbnailPath = saveBitmap(frame, "draft_thumb_");
            } else draft.thumbnailPath = first.getPath();
        }
        currentDraftId = draft.id;
        DraftsBottomSheet.saveDraft(this, draft);
        showToast("Draft saved ✓");
    }

    // =========================================================================
    // RIGHT TOOLS
    // =========================================================================
    private void setupRightTools() {
        overlayCanvas.setClickable(true);
        overlayCanvas.setFocusable(true);
        findViewById(R.id.toolText).setOnClickListener(v -> {
            closeAllPanels();
            editingTextView = null;
            openTextEditorPanel();
        });
        findViewById(R.id.toolEmoji).setOnClickListener(v -> {
            closeAllPanels();
            slideUpPanel(emojiPanel);
        });
        findViewById(R.id.toolSticker).setOnClickListener(v -> {
            closeAllPanels();
            slideUpPanel(stickerPanel);
        });
        findViewById(R.id.toolDraw).setOnClickListener(v -> openDrawMode());
        // FIX: Filter now shows real-time preview on video
        findViewById(R.id.toolFilter).setOnClickListener(v -> openFilterPanel());
        // FIX: Trim now actually works (passed to render engine)
        findViewById(R.id.toolTrim).setOnClickListener(v -> openTrimPanel());

        overlayCanvas.setOnClickListener(v -> closeAllPanels());
    }

    private void openGreenScreenSettings() {
        if (gsBgPath != null) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Green Screen Active")
                    .setMessage("Background: " + new File(gsBgPath).getName()
                            + "\nTolerance: " + gsTolerance + "\nSpill: " + gsSpill)
                    .setPositiveButton("Keep", null)
                    .setNegativeButton("Remove", (d, w) -> {
                        gsBgPath = null;
                        showToast("Green screen removed");
                    })
                    .show();
        } else {
            showToast("Launch from Green Screen activity to set background.");
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    private void openDrawMode() {
        closeAllPanels();
        if (drawingView == null) {
            drawingView = new DrawingView(this);
            drawingView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            overlayCanvas.addView(drawingView);
        }
        isDrawMode = true;
        drawingView.setVisibility(View.VISIBLE);
        overlayCanvas.setOnClickListener(null);
        DrawToolsPanel panel = DrawToolsPanel.newInstance();
        panel.setListener(new DrawToolsPanel.DrawToolsListener() {
            @Override
            public void onToolSelected(DrawingView.Tool tool) {
                drawingView.setTool(tool);
            }

            @Override
            public void onColorSelected(int color) {
                drawingView.setColor(color);
            }

            @Override
            public void onSizeChanged(float size) {
                drawingView.setBrushSize(size);
            }

            @Override
            public void onOpacityChanged(float opacity) {
                drawingView.setOpacity(opacity);
            }

            @Override
            public void onUndo() {
                drawingView.undo();
            }

            @Override
            public void onRedo() {
                drawingView.redo();
            }

            @Override
            public void onClear() {
                drawingView.clear();
            }

            @Override
            public void onDone() {
                isDrawMode = false;
                overlayCanvas.setOnClickListener(v2 -> closeAllPanels());
            }
        });
        panel.show(getSupportFragmentManager(), "draw_tools");
    }

    // ── Filter ────────────────────────────────────────────────────────────────
    // FIX 1: Live preview now works for VIDEO by showing filtered frame as overlay.
    // FIX 2: Filter state passed to render engine on export.
    private void openFilterPanel() {
        closeAllPanels();
        FilterPanel panel = FilterPanel.newInstance();
        MediaModel m = mediaList.get(currentMediaIndex);

        // Prepare thumbnail for preset tiles
        if (!m.isVideo()) {
            android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap thumb = android.graphics.BitmapFactory.decodeFile(m.getPath(), opts);
            panel.setPreviewBitmap(thumb);
        } else {
            Bitmap frame = extractVideoFrame(m.getPath());
            if (frame != null) panel.setPreviewBitmap(frame);
        }

        panel.setListener(new FilterPanel.FilterListener() {
            @Override
            public void onFilterChanged(FilterEngine.FilterState state) {
                filterState = state;
                if (!m.isVideo()) {
                    // Image: apply ColorMatrix to ImageView directly
                    FilterEngine.apply(imagePreview, state);
                } else {
                    // FIX: Video — extract frame, apply bitmap filter, show as overlay
                    // Run on background thread to avoid jank
                    executor.execute(() -> {
                        Bitmap frame = extractVideoFrame(m.getPath());
                        if (frame == null) return;
                        Bitmap filtered = FilterEngine.applyToBitmap(frame, state);
                        mainHandler.post(() -> {
                            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                    videoView.getWidth(), videoView.getHeight());
                            lp.gravity = android.view.Gravity.CENTER;
                            imagePreview.setLayoutParams(lp);
                            imagePreview.setScaleType(ImageView.ScaleType.FIT_XY); // ★ zoom nahi hoga
                            imagePreview.setImageBitmap(filtered);
                            imagePreview.setColorFilter(null);
                            imagePreview.setAlpha(0.92f);
                            imagePreview.setVisibility(View.VISIBLE);
                            // Video continues playing beneath — user sees color on top
                        });
                    });
                }
            }

            @Override
            public void onFilterApplied(FilterEngine.FilterState state) {
                filterState = state;
                if (m.isVideo()) {
                    imagePreview.setVisibility(View.GONE);
                    imagePreview.setAlpha(1f);
                    imagePreview.setImageBitmap(null);
                    imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
                showToast("Filter applied ✓");
            }
        });

        // FIX: On panel dismiss (cancel) restore video preview
        panel.getLifecycle().addObserver(
                (androidx.lifecycle.LifecycleEventObserver) (source, event) -> {
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_DESTROY && m.isVideo()) {
                        imagePreview.setVisibility(View.GONE);
                        imagePreview.setAlpha(1f);
                        imagePreview.setImageBitmap(null);
                    }
                });

        panel.show(getSupportFragmentManager(), "filter");
    }

    // ── Trim ──────────────────────────────────────────────────────────────────
    // FIX: trimStartMs/trimEndMs now forwarded to renderEngine in proceedToPost()
    private void openTrimPanel() {
        MediaModel m = mediaList.get(currentMediaIndex);
        if (!m.isVideo()) {
            showToast("Trim works only on videos");
            return;
        }
        closeAllPanels();
        long dur = m.getDuration() > 0 ? m.getDuration() : getVideoDuration(m.getPath());
        TrimPanel panel = TrimPanel.newInstance(m.getPath(), dur);
        panel.setTrimListener((startMs, endMs) -> {
            trimStartMs = startMs;
            trimEndMs = endMs;
            showToast("Trim set: " + formatMs(startMs) + " – " + formatMs(endMs));
            videoView.seekTo((int) startMs);
        });
        panel.show(getSupportFragmentManager(), "trim");
    }

    private long getVideoDuration(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(path);
            String d = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return d != null ? Long.parseLong(d) : 60_000L;
        } catch (Exception e) {
            return 60_000L;
        } finally {
            try {
                mmr.release();
            } catch (Exception ignored) {
            }
        }
    }

    private String formatMs(long ms) {
        long s = ms / 1000;
        return String.format(java.util.Locale.getDefault(), "%d:%02d", s / 60, s % 60);
    }

    // =========================================================================
    // PANELS
    // =========================================================================
    private void slideUpPanel(View panel) {
        panel.setVisibility(View.VISIBLE);
        panel.setTranslationY(panel.getHeight() > 0 ? panel.getHeight() : 900f);
        panel.animate().translationY(0).setDuration(300)
                .setInterpolator(new OvershootInterpolator(0.7f)).start();
    }

    private void slideDownPanel(View panel) {
        if (panel.getVisibility() != View.VISIBLE) return;
        panel.animate().translationY(1200f).setDuration(240)
                .withEndAction(() -> panel.setVisibility(View.GONE)).start();
    }

    private void closeAllPanels() {
        emojiPanel.setVisibility(View.GONE);
        textEditorPanel.setVisibility(View.GONE);
        slideDownPanel(textEditorPanel);
        slideDownPanel(emojiPanel);
        slideDownPanel(stickerPanel);
        dismissKeyboard();
    }

    // =========================================================================
    // VIDEO PLAYBACK
    // =========================================================================
    private void playVideo(String path) {
        videoView.setVisibility(View.VISIBLE);
        imagePreview.setVisibility(View.GONE);
        videoView.setVideoURI(Uri.parse(path));
        videoView.setOnPreparedListener(mp -> {
            int vW = mp.getVideoWidth(), vH = mp.getVideoHeight();
            if (vW > 0 && vH > 0) {
                int sW = videoView.getWidth(), sH = videoView.getHeight();
                float vR = (float) vW / vH, sR = (float) sW / sH;
                int finalW, finalH;
                if (vR > sR) {
                    finalW = sW;
                    finalH = (int) (sW / vR);
                } else {
                    finalH = sH;
                    finalW = (int) (sH * vR);
                }
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(finalW, finalH);
                lp.gravity = Gravity.CENTER;
                videoView.setLayoutParams(lp);
            }
            mp.setLooping(true);
            videoView.start();
            isVideoPlaying = true;
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            showToast("Video error: " + what);
            return true;
        });
    }

    private void showImage(String path) {
        imagePreview.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        if (videoView.isPlaying()) videoView.pause();
        Glide.with(this).load(path).centerCrop().into(imagePreview);
    }

    private void setupVideoTapToPause() {
        View mediaArea = findViewById(R.id.mediaContainer);
        mediaArea.setOnClickListener(v -> {
            if (videoView.getVisibility() != View.VISIBLE) return;
            if (isVideoPlaying) {
                videoView.pause();
                isVideoPlaying = false;
                showPlayPauseHint(false);
            } else {
                videoView.start();
                isVideoPlaying = true;
                showPlayPauseHint(true);
            }
        });
    }

    private void showPlayPauseHint(boolean playing) {
        playPauseIcon.setImageResource(playing
                ? R.drawable.ic_play_circle_outline : R.drawable.outline_pause_circle_24);
        playPauseIndicator.setVisibility(View.VISIBLE);
        playPauseIndicator.setAlpha(1f);
        playPauseIndicator.animate().alpha(0f).setStartDelay(600).setDuration(300)
                .withEndAction(() -> playPauseIndicator.setVisibility(View.GONE)).start();
    }

    // =========================================================================
    // TEXT EDITOR
    // =========================================================================
    private void setupTextEditor() {
        findViewById(R.id.textDoneBtn).setOnClickListener(v -> commitTextOverlay());
        textAlignBtn.setOnClickListener(v -> cycleTextAlign());
        findViewById(R.id.textBgBtn).setOnClickListener(v -> cycleTextBg());
        textSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                currentTextSize = 12 + p;
                textInput.setTextSize(currentTextSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });
    }

    private void openTextEditorPanel() {
        textEditorPanel.setVisibility(View.VISIBLE);
        textEditorPanel.setTranslationY(0);
        if (editingTextView != null) {
            textInput.setText(editingTextView.getText().toString());
            textInput.setSelection(textInput.getText().length());
        } else textInput.setText("");
        ViewGroup.LayoutParams lp = textEditorPanel.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        textEditorPanel.setLayoutParams(lp);
        textInput.requestFocus();
        textInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(textInput, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }

    private void commitTextOverlay() {
        if (editingTextView != null) {
            if (editingTextView.getParent() != null) overlayCanvas.removeView(editingTextView);
            editingTextView = null;
        }
        String text = textInput.getText().toString().trim();
        if (!text.isEmpty()) addDraggableTextView(text);
        closeAllPanels();
    }

    // =========================================================================
    // DRAGGABLE VIEWS
    // =========================================================================
    @SuppressLint("ClickableViewAccessibility")
    private void addDraggableTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(currentTextColor);
        tv.setTextSize(currentTextSize);
        tv.setGravity(currentTextAlign);
        tv.setTypeface(Typeface.create(FONT_FAMILIES[currentFontIndex], Typeface.NORMAL));
        tv.setPadding(dp(12), dp(6), dp(12), dp(6));
        applyTextBackground(tv);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        makeDraggableWithTrash(tv);
        tv.setOnClickListener(v -> {
            if (editingTextView != null && editingTextView.getParent() != null)
                overlayCanvas.removeView(editingTextView);
            editingTextView = tv;
            textInput.setText(tv.getText());
            openTextEditorPanel();
        });
        tv.setScaleX(0f);
        tv.setScaleY(0f);
        overlayCanvas.addView(tv);
        tv.animate().scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addDraggableEmoji(String emoji) {
        TextView tv = new TextView(this);
        tv.setText(emoji);
        tv.setTextSize(42f);
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        makeDraggableWithTrash(tv);
        tv.setScaleX(0f);
        tv.setScaleY(0f);
        overlayCanvas.addView(tv);
        tv.animate().scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(new OvershootInterpolator(1.5f)).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addDraggableSticker(String sticker) {
        TextView tv = new TextView(this);
        tv.setText(sticker);
        tv.setTextSize(36f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(8), dp(8), dp(8));
        tv.setClickable(true);
        tv.setFocusable(true);
        tv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        makeDraggableWithTrash(tv);
        tv.setScaleX(0f);
        tv.setScaleY(0f);
        overlayCanvas.addView(tv);
        tv.animate().scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(new OvershootInterpolator(1.5f)).start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void makeDraggableWithTrash(View view) {
        final float[] dX = {0f}, dY = {0f}, startRawX = {0f}, startRawY = {0f}, origScale = {1f};
        final boolean[] dragging = {false};
        ScaleGestureDetector scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        float s = Math.max(0.3f, Math.min(view.getScaleX() * d.getScaleFactor(), 5f));
                        view.setScaleX(s);
                        view.setScaleY(s);
                        origScale[0] = s;
                        return true;
                    }
                });
        view.setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX[0] = v.getX() - event.getRawX();
                    dY[0] = v.getY() - event.getRawY();
                    startRawX[0] = event.getRawX();
                    startRawY[0] = event.getRawY();
                    origScale[0] = v.getScaleX();
                    dragging[0] = false;
                    v.animate().scaleX(origScale[0] * 1.08f).scaleY(origScale[0] * 1.08f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() == 1) {
                        float mx = event.getRawX(), my = event.getRawY();
                        float dist = (float) Math.hypot(mx - startRawX[0], my - startRawY[0]);
                        if (dist > 8 || dragging[0]) {
                            dragging[0] = true;
                            if (!isTrashVisible) showTrashZone();
                            v.setX(mx + dX[0]);
                            v.setY(my + dY[0]);
                            Rect tr = new Rect();
                            trashZone.getGlobalVisibleRect(tr);
                            boolean over = tr.contains((int) mx, (int) my);
                            trashZone.setAlpha(over ? 1f : 0.8f);
                            trashZone.setScaleX(over ? 1.2f : 1f);
                            trashZone.setScaleY(over ? 1.2f : 1f);
                            v.setAlpha(over ? 0.4f : 1f);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    hideTrashZone();
                    if (dragging[0]) {
                        Rect tr = new Rect();
                        trashZone.getGlobalVisibleRect(tr);
                        if (tr.contains((int) event.getRawX(), (int) event.getRawY())) {
                            v.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200)
                                    .withEndAction(() -> overlayCanvas.removeView(v)).start();
                        } else {
                            v.animate().scaleX(origScale[0]).scaleY(origScale[0]).alpha(1f).setDuration(180).start();
                        }
                        return true;
                    }
                    v.animate().scaleX(origScale[0]).scaleY(origScale[0]).setDuration(100).start();
                    break;
            }
            return dragging[0];
        });
    }

    private void showTrashZone() {
        isTrashVisible = true;
        trashZone.setVisibility(View.VISIBLE);
        trashZone.setAlpha(0f);
        trashZone.animate().alpha(0.8f).setDuration(200).start();
    }

    private void hideTrashZone() {
        isTrashVisible = false;
        trashZone.animate().alpha(0f).setDuration(180)
                .withEndAction(() -> trashZone.setVisibility(View.GONE)).start();
    }

    private void applyTextBackground(View v) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(dp(10));
        switch (currentTextBgMode) {
            case 0:
                v.setBackground(null);
                break;
            case 1:
                bg.setColor(Color.argb(190, 0, 0, 0));
                v.setBackground(bg);
                break;
            case 2:
                bg.setColor(Color.TRANSPARENT);
                bg.setStroke(dp(2), Color.WHITE);
                v.setBackground(bg);
                break;
        }
    }

    private void cycleTextAlign() {
        currentTextAlign = currentTextAlign == Gravity.CENTER ? Gravity.START
                : currentTextAlign == Gravity.START ? Gravity.END : Gravity.CENTER;
        textInput.setGravity(currentTextAlign);
        int[] icons = {R.drawable.ic_format_align_center, R.drawable.ic_format_align_left, R.drawable.ic_format_align_right};
        int idx = currentTextAlign == Gravity.CENTER ? 0 : currentTextAlign == Gravity.START ? 1 : 2;
        textAlignBtn.setImageResource(icons[idx]);
    }

    private void cycleTextBg() {
        currentTextBgMode = (currentTextBgMode + 1) % 3;
        applyTextBackground(textInput);
    }

    // =========================================================================
    // COLOR + FONT STRIPS
    // =========================================================================
    private void buildColorStrip() {
        colorStripLayout.removeAllViews();
        for (int i = 0; i < TEXT_COLORS.length; i++) {
            final int color = TEXT_COLORS[i], index = i;
            FrameLayout dot = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.setMargins(dp(5), 0, dp(5), 0);
            dot.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(color);
            gd.setStroke(dp(2), i == 0 ? 0x88FFFFFF : Color.WHITE);
            dot.setBackground(gd);
            dot.setOnClickListener(v -> {
                currentTextColor = color;
                textInput.setTextColor(color);
                highlightColorDot(index);
            });
            colorStripLayout.addView(dot);
        }
    }

    private void highlightColorDot(int selected) {
        for (int i = 0; i < colorStripLayout.getChildCount(); i++) {
            boolean sel = i == selected;
            colorStripLayout.getChildAt(i).animate().scaleX(sel ? 1.35f : 1f).scaleY(sel ? 1.35f : 1f).setDuration(150).start();
        }
    }

    private void buildFontStrip() {
        fontStripLayout.removeAllViews();
        for (int i = 0; i < FONT_FAMILIES.length; i++) {
            final int idx = i;
            TextView chip = new TextView(this);
            chip.setText(FONT_LABELS[i]);
            chip.setTextColor(i == 0 ? Color.WHITE : 0x88FFFFFF);
            chip.setTextSize(14f);
            chip.setTypeface(Typeface.create(FONT_FAMILIES[i], Typeface.NORMAL));
            chip.setPadding(dp(14), dp(5), dp(14), dp(5));
            if (i == 0)
                chip.setPaintFlags(chip.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            chip.setOnClickListener(v -> {
                currentFontIndex = idx;
                textInput.setTypeface(Typeface.create(FONT_FAMILIES[idx], Typeface.NORMAL));
                updateFontSelection(idx);
            });
            fontStripLayout.addView(chip);
        }
    }

    private void updateFontSelection(int selected) {
        for (int i = 0; i < fontStripLayout.getChildCount(); i++) {
            TextView tv = (TextView) fontStripLayout.getChildAt(i);
            boolean sel = i == selected;
            tv.setTextColor(sel ? Color.WHITE : 0x88FFFFFF);
            int flags = tv.getPaintFlags();
            if (sel) flags |= android.graphics.Paint.UNDERLINE_TEXT_FLAG;
            else flags &= ~android.graphics.Paint.UNDERLINE_TEXT_FLAG;
            tv.setPaintFlags(flags);
        }
    }

    // =========================================================================
    // EMOJI + STICKER
    // =========================================================================
    private void setupEmojiPanel() {
        String[] catIcons = {"😀", "❤️", "🎉", "🐶", "🍕", "⚽", "🌍", "✈️"};
        buildCategoryTabs(emojiCategoryStrip, catIcons, idx -> emojiAdapter.updateData(EmojiData.getCategory(idx)));
        emojiAdapter = new EmojiAdapter(this, EmojiData.getCategory(0), emoji -> {
            addDraggableEmoji(emoji);
            slideDownPanel(emojiPanel);
        });
        emojiRecycler.setLayoutManager(new GridLayoutManager(this, 8));
        emojiRecycler.setAdapter(emojiAdapter);
    }

    private void setupStickerPanel() {
        String[] catLabels = {"🔥 Trending", "💬 Text", "🎨 Fun", "✨ Glitter", "🌈 Vibes"};
        buildCategoryTabs(stickerCategoryStrip, catLabels, idx -> stickerAdapter.updateData(StickerData.getCategory(idx)));
        stickerAdapter = new StickerAdapter(this, StickerData.getCategory(0), sticker -> {
            addDraggableSticker(sticker);
            slideDownPanel(stickerPanel);
        });
        stickerRecycler.setLayoutManager(new GridLayoutManager(this, 4));
        stickerRecycler.setAdapter(stickerAdapter);
    }

    private void buildCategoryTabs(LinearLayout strip, String[] labels, OnCatSelected cb) {
        strip.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView tv = new TextView(this);
            tv.setText(labels[i]);
            tv.setTextSize(labels[i].length() > 4 ? 11f : 20f);
            tv.setTextColor(0xCCFFFFFF);
            tv.setPadding(dp(10), dp(5), dp(10), dp(5));
            tv.setAlpha(i == 0 ? 1f : 0.5f);
            if (i == 0) setTabSelected(tv);
            tv.setOnClickListener(v -> {
                for (int j = 0; j < strip.getChildCount(); j++) {
                    View c = strip.getChildAt(j);
                    c.setAlpha(j == idx ? 1f : 0.5f);
                    if (j == idx) setTabSelected((TextView) c);
                    else c.setBackground(null);
                }
                cb.onSelected(idx);
            });
            strip.addView(tv);
        }
    }

    private void setTabSelected(TextView tv) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(0x33FFFFFF);
        bg.setCornerRadius(dp(20));
        tv.setBackground(bg);
    }

    // =========================================================================
    // CLIP STRIP
    // =========================================================================
    private void buildClipStrip() {
        clipStrip.removeAllViews();
        for (int i = 0; i < mediaList.size(); i++) {
            final int idx = i;
            MediaModel m = mediaList.get(i);
            FrameLayout frame = new FrameLayout(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(52), dp(52));
            lp.setMargins(dp(3), 0, dp(3), 0);
            frame.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xFF222222);
            bg.setCornerRadius(dp(8));
            frame.setBackground(bg);
            frame.setClipToOutline(true);
            frame.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
            ImageView thumb = new ImageView(this);
            thumb.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            frame.addView(thumb);
            if (m.isVideo()) ThumbnailHelper.load(this, m.getPath(), thumb);
            else Glide.with(this).load(m.getPath()).centerCrop().into(thumb);
            if (m.isVideo()) {
                ImageView badge = new ImageView(this);
                FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(dp(16), dp(16), Gravity.BOTTOM | Gravity.START);
                blp.setMargins(dp(4), 0, 0, dp(4));
                badge.setLayoutParams(blp);
                badge.setImageResource(R.drawable.ic_play_circle_outline);
                badge.setColorFilter(Color.WHITE);
                frame.addView(badge);
                if (m.getDuration() > 0) {
                    TextView tvDur = new TextView(this);
                    FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
                    dlp.setMargins(0, 0, dp(3), dp(3));
                    tvDur.setLayoutParams(dlp);
                    tvDur.setTextColor(Color.WHITE);
                    tvDur.setTextSize(9f);
                    tvDur.setBackgroundColor(0x88000000);
                    tvDur.setPadding(dp(2), 0, dp(2), 0);
                    long sec = m.getDuration() / 1000;
                    tvDur.setText(String.format(java.util.Locale.US, "%d:%02d", sec / 60, sec % 60));
                    frame.addView(tvDur);
                }
            }
            if (i == 0) setBorderActive(frame, true);
            frame.setOnClickListener(v -> {
                updateClipStripSelection(idx);
                loadMediaAtIndex(idx);
            });
            clipStrip.addView(frame);
        }
        // + button
        FrameLayout addBtn = new FrameLayout(this);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(52), dp(52));
        alp.setMargins(dp(3), 0, dp(3), 0);
        addBtn.setLayoutParams(alp);
        android.graphics.drawable.GradientDrawable addBg = new android.graphics.drawable.GradientDrawable();
        addBg.setColor(Color.TRANSPARENT);
        addBg.setStroke(dp(1), 0x66FFFFFF);
        addBg.setCornerRadius(dp(8));
        addBtn.setBackground(addBg);
        ImageView addIcon = new ImageView(this);
        addIcon.setLayoutParams(new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        addIcon.setImageResource(R.drawable.ic_add);
        addIcon.setColorFilter(0xAAFFFFFF);
        addBtn.addView(addIcon);
        addBtn.setOnClickListener(v -> finish());
        clipStrip.addView(addBtn);
    }

    private void updateClipStripSelection(int selected) {
        currentMediaIndex = selected;
        for (int i = 0; i < clipStrip.getChildCount() - 1; i++)
            setBorderActive((FrameLayout) clipStrip.getChildAt(i), i == selected);
    }

    private void setBorderActive(FrameLayout frame, boolean active) {
        android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
        border.setColor(Color.TRANSPARENT);
        if (active) border.setStroke(dp(2), Color.WHITE);
        border.setCornerRadius(dp(8));
        frame.setForeground(active ? border : null);
    }

    // =========================================================================
    // BOTTOM ACTIONS
    // =========================================================================
    private void setupBottomActions() {
        findViewById(R.id.btnAddClip).setOnClickListener(v -> finish());
        findViewById(R.id.btnAudio).setOnClickListener(v -> openAudioMixer());
        findViewById(R.id.btnSpeed).setOnClickListener(v -> openSpeedControl());
    }

    private void loadMediaAtIndex(int index) {
        if (index < 0 || index >= mediaList.size()) return;
        MediaModel m = mediaList.get(index);
        if (m.isVideo()) playVideo(m.getPath());
        else showImage(m.getPath());
    }

    // =========================================================================
    // PROCEED TO POST — ALL FIXES APPLIED
    // =========================================================================
   /* private void proceedToPost() {
        if (mediaList == null || mediaList.isEmpty()) {
            showToast("No media found");
            return;
        }
        hideEditorUIForCapture();
        renderDialog.show();
        MediaModel m = mediaList.get(currentMediaIndex);
        boolean isImage = !m.isVideo();
        int durSec = isImage ? 15 : (int) (m.getDuration() / 1000);

        renderEngine.setSpeed(currentSpeed);
        renderEngine.setOriginalVolume(originalAudioVolume);
        renderEngine.setMusicVolume(musicAudioVolume);
        // FIX 1: TRIM — pass to engine
        renderEngine.setTrim(trimStartMs, trimEndMs);
        // FIX 2: FILTER — pass full state (not just preset)
        renderEngine.setFilterState(filterState);
        // FIX 3: TEMPLATE duration
        if (templateName != null && templateDuration > 0)
            renderEngine.setTemplate(templateName, templateDuration);
        if (currentEffect != null) renderEngine.setEffect(currentEffect);
        // FIX 4: GREEN SCREEN — pass key color too
        if (gsBgPath != null) {
            renderEngine.setGreenScreenBg(gsBgPath, gsTolerance, gsSpill);
            renderEngine.setGreenScreenKeyColor(gsKeyColor);
        }

        renderEngine.render(m.getPath(), isImage, overlayCanvas,
                selectedMusicPath, musicStartMs, musicEndMs, durSec,
                new ReelRenderEngine.RenderCallback() {
                    @Override
                    public void onProgress(int percent, String message) {
                        runOnUiThread(() -> renderDialog.updateProgress(percent, message));
                    }

                    @Override
                    public void onComplete(String outputPath) {
                        runOnUiThread(() -> {
                            renderDialog.showSuccess();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                renderDialog.dismiss();
                                showEditorUI();

                                // ★ FIX: Background thread pe thumb banao, JPEG guaranteed
                                executor.execute(() -> {
                                    String thumbPath = extractThumbFromVideo(outputPath);

                                    // ★ Fallback: agar rendered video se na mile, original se lo
                                    if (thumbPath == null && !mediaList.isEmpty()) {
                                        thumbPath = extractThumbFromVideo(mediaList.get(0).getPath());
                                    }

                                    final String finalThumb = thumbPath;
                                    mainHandler.post(() -> {
                                        Intent intent = new Intent(ReelEditorActivity.this, ReelPostActivity.class);
                                        intent.putExtra("rendered_video_path", outputPath);
                                        intent.putExtra("thumbnail_path", finalThumb); // ★ ab JPEG path hai
                                        intent.putExtra("media_list", new ArrayList<>(mediaList));
                                        intent.putExtra("pageId", finalPageId);
                                        startActivity(intent);
                                        overridePendingTransition(
                                                android.R.anim.slide_in_left,
                                                android.R.anim.slide_out_right);
                                    });
                                });
                            }, 800);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            renderDialog.showError(error);
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                renderDialog.dismiss();
                                showEditorUI();
                            }, 2000);
                        });
                    }
                });
    }*/
    private void proceedToPost() {
        if (mediaList == null || mediaList.isEmpty()) {
            showToast("No media found");
            return;
        }

        hideEditorUIForCapture();
        renderDialog.show();

        // Engine settings
        renderEngine.setSpeed(currentSpeed);
        renderEngine.setOriginalVolume(originalMuted ? 0f : originalAudioVolume);
        renderEngine.setMusicVolume(musicMuted    ? 0f : musicAudioVolume);
        renderEngine.setTrim(trimStartMs, trimEndMs);
        renderEngine.setFilterState(filterState);
        if (templateName != null && templateDuration > 0)
            renderEngine.setTemplate(templateName, templateDuration);
        if (currentEffect != null)
            renderEngine.setEffect(currentEffect);
        if (gsBgPath != null) {
            renderEngine.setGreenScreenBg(gsBgPath, gsTolerance, gsSpill);
            renderEngine.setGreenScreenKeyColor(gsKeyColor);
        }

        ReelRenderEngine.RenderCallback cb = new ReelRenderEngine.RenderCallback() {
            @Override public void onProgress(int p, String msg) {
                runOnUiThread(() -> renderDialog.updateProgress(p, msg));
            }
            @Override public void onComplete(String out) { handleRenderComplete(out); }
            @Override public void onError(String err)    { handleRenderError(err);    }
        };

        if (mediaList.size() == 1) {
            // ── Single clip ────────────────────────────────────────────────────
            MediaModel m = mediaList.get(0);
            renderEngine.render(m.getPath(), !m.isVideo(), overlayCanvas,
                    selectedMusicPath, musicStartMs, musicEndMs,
                    m.isVideo() ? (int)(m.getDuration()/1000) : 5, cb);
        } else {
            // ── Multiple clips (image + video mix) ─────────────────────────────
            List<String> paths = new ArrayList<>();
            int totalSec = 0;
            for (MediaModel m : mediaList) {
                paths.add(m.getPath());
                totalSec += m.isVideo() ? (int)(m.getDuration()/1000) : 5;
            }
            renderEngine.renderMultiple(mediaList, overlayCanvas,
                    selectedMusicPath, musicStartMs, musicEndMs, totalSec, cb);
        }
    }

    private void handleRenderComplete(String outputPath) {
        runOnUiThread(() -> {
            renderDialog.showSuccess();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                renderDialog.dismiss();
                showEditorUI();
                executor.execute(() -> {
                    String thumb = extractThumbFromVideo(outputPath);
                    if (thumb == null && !mediaList.isEmpty())
                        thumb = extractThumbFromVideo(mediaList.get(0).getPath());
                    final String ft = thumb;
                    mainHandler.post(() -> {
                        Intent intent = new Intent(ReelEditorActivity.this,
                                ReelPostActivity.class);
                        intent.putExtra("rendered_video_path", outputPath);
                        intent.putExtra("thumbnail_path", ft);
                        intent.putExtra("media_list", new ArrayList<>(mediaList));
                        intent.putExtra("pageId", finalPageId);
                        intent.putExtra("clip_count", mediaList.size());
                        if (selectedMusicPath != null) {
                            intent.putExtra("music_uri",   selectedMusicPath);
                            intent.putExtra("music_start", musicStartMs);
                            intent.putExtra("music_end",   musicEndMs);
                        }
                        startActivity(intent);
                        overridePendingTransition(
                                android.R.anim.slide_in_left,
                                android.R.anim.slide_out_right);
                    });
                });
            }, 800);
        });
    }

    private void handleRenderError(String error) {
        runOnUiThread(() -> {
            renderDialog.showError(error);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                renderDialog.dismiss();
                showEditorUI();
            }, 2000);
        });
    }
    private void openSpeedControl() {
        closeAllPanels();
        SpeedControlBottomSheet sheet = SpeedControlBottomSheet.newInstance(currentSpeed);
        sheet.setListener(speed -> {
            currentSpeed = speed;
            if (videoView.getVisibility() == View.VISIBLE && isVideoPlaying) {
                try {
                    @SuppressLint("DiscouragedPrivateApi")
                    java.lang.reflect.Field field = VideoView.class.getDeclaredField("mMediaPlayer");
                    field.setAccessible(true);
                    android.media.MediaPlayer mp = (android.media.MediaPlayer) field.get(videoView);
                    if (mp != null) {
                        android.media.PlaybackParams p = new android.media.PlaybackParams();
                        p.setSpeed(speed);
                        mp.setPlaybackParams(p);
                    }
                } catch (Exception e) {
                    showToast("Speed " + speed + "x — applies on export");
                }
            }
            showToast("Speed: " + (speed == 1.0f ? "Normal" : speed + "x"));
        });
        sheet.show(getSupportFragmentManager(), "speed_control");
    }

    private void openAudioMixer() {
        closeAllPanels();
        boolean hasMusicTrack = selectedMusicPath != null && !selectedMusicPath.isEmpty();
        AudioMixerBottomSheet sheet = AudioMixerBottomSheet.newInstance(hasMusicTrack, originalAudioVolume, musicAudioVolume);
        sheet.setListener(new AudioMixerBottomSheet.AudioMixListener() {
            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onOriginalVolumeChanged(float volume) {
                originalAudioVolume = volume;
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        java.lang.reflect.Field field = VideoView.class.getDeclaredField("mMediaPlayer");
                        field.setAccessible(true);
                        android.media.MediaPlayer mp = (android.media.MediaPlayer) field.get(videoView);
                        if (mp != null) mp.setVolume(volume, volume);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onMusicVolumeChanged(float volume) {
                musicAudioVolume = volume;
            }

            @Override
            public void onMuteOriginalChanged(boolean muted) {
                originalMuted = muted;
                onOriginalVolumeChanged(muted ? 0f : originalAudioVolume);
            }

            @Override
            public void onMuteMusicChanged(boolean muted) {
                musicMuted = muted;
                onMusicVolumeChanged(muted ? 0f : musicAudioVolume);
            }
        });
        sheet.show(getSupportFragmentManager(), "audio_mixer");
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private String extractThumbFromVideo(String videoPath) {
        if (videoPath == null || !new File(videoPath).exists()) return null;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(videoPath);

            // ★ FIX: Pehle 1 second try karo, warna 0 pe fallback
            Bitmap frame = mmr.getFrameAtTime(
                    1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) {
                frame = mmr.getFrameAtTime(
                        0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            }
            if (frame == null) return null;

            // ★ FIX: File name mein .jpg extension pakka karo
            File tf = new File(getCacheDir(),
                    "thumb_final_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tf);
            frame.compress(Bitmap.CompressFormat.JPEG, 90, fos); // ★ quality 85→90
            fos.flush();
            fos.close();
            frame.recycle(); // ★ memory leak band karo
            return tf.getAbsolutePath(); // yeh sirf .jpg path return karega

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
    }

    private Bitmap extractVideoFrame(String videoPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            Uri uri = Uri.parse(videoPath);
            if ("content".equals(uri.getScheme())) retriever.setDataSource(this, uri);
            else retriever.setDataSource(videoPath);
            Bitmap frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null)
                frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            return frame;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    private String saveBitmap(Bitmap bmp, String prefix) {
        try {
            File file = new File(getCacheDir(), prefix + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void hideEditorUIForCapture() {
        findViewById(R.id.topBar).setVisibility(View.INVISIBLE);
    }

    private void showEditorUI() {
        findViewById(R.id.topBar).setVisibility(View.VISIBLE);
    }

    private void dismissKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView.getVisibility() == View.VISIBLE && !videoView.isPlaying()) {
            videoView.start();
            isVideoPlaying = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.pause();
            isVideoPlaying = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdown();
        if (renderEngine != null) renderEngine.cancel();
    }

    interface OnCatSelected {
        void onSelected(int index);
    }
}