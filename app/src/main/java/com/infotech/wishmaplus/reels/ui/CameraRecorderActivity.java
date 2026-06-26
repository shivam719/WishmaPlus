package com.infotech.wishmaplus.reels.ui;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.ReelPostActivity;
import com.infotech.wishmaplus.reels.ui.componets.ServerMusicPickerBottomSheet;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * ══════════════════════════════════════════════════════════════════
 * CameraRecorderActivity  — Professional Reels Camera
 * Features:
 * All options visible on camera open (Music, Speed, Filter, Timer, Beauty)
 * Front / Back camera toggle
 * Music selected → plays during recording + mixed into final video
 * Filter overlay baked into recorded video via thumbnail
 * Proper thumbnail generation (JPEG)
 * Timestamp, duration, music name → ReelPostActivity
 * Wishma Plus-style professional UI
 * ══════════════════════════════════════════════════════════════════
 */
public class CameraRecorderActivity extends AppCompatActivity {

    private static final String TAG = "CameraRecorder";

    // ─── Permissions ──────────────────────────────────────────────
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    // ─── Speed Options ────────────────────────────────────────────
    private final float[] speedOptions = {0.3f, 0.5f, 1.0f, 2.0f, 3.0f};
    private final String[] speedLabels = {"0.3x", "0.5x", "1x", "2x", "3x"};

    // ─── Filter Options ───────────────────────────────────────────
    private final String[] filterNames = {"None", "Vivid", "Warm", "Cool", "B&W", "Vintage", "Fade"};
    private final int[] filterColors = {
            Color.TRANSPARENT,
            Color.parseColor("#60FF4500"),
            Color.parseColor("#50FF8C00"),
            Color.parseColor("#500066FF"),
            Color.parseColor("#70000000"),
            Color.parseColor("#50C68642"),
            Color.parseColor("#30FFFFFF")
    };

    // ─── Handler ──────────────────────────────────────────────────
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ─── Views: Camera Preview ────────────────────────────────────
    private PreviewView cameraPreview;
    private View filterOverlay;

    // ─── Views: Top Bar ───────────────────────────────────────────
    private ImageButton btnClose;
    private ImageButton btnFlash;

    // ─── Views: Left Options (always visible) ─────────────────────
    private LinearLayout leftOptionsPanel;
    private LinearLayout btnAddMusic;
    private LinearLayout btnSpeed;
    private LinearLayout btnFilter;
    private LinearLayout btnTimer;
    private LinearLayout btnBeauty;
    private TextView tvMusicName;
    private TextView tvSpeedValue;
    private TextView tvTimerValue;
    private TextView tvBeautyValue;

    // ─── Views: Bottom Bar ────────────────────────────────────────
    private ImageButton btnGallery, btnRecord, btnFlipCamera;
    private LinearLayout bottomBar, durationBar;
    private TextView dur15s, dur30s, dur60s, dur3min;

    // ─── Views: Recording Info ────────────────────────────────────
    private LinearLayout recordingTopBar;
    private View recordingIndicatorDot;
    private TextView tvRecordingTimer;
    private View progressBar;

    // ─── Views: Panels ────────────────────────────────────────────
    private LinearLayout speedPanel, timerPanel, beautyPanel, filterPanel;
    private LinearLayout filterList;
    private TextView speed03x, speed05x, speed1x, speed2x, speed3x;
    private TextView timerOff, timer3s, timer10s;
    private SeekBar seekBeautySmooth, seekBeautyWhiten;
    private TextView tvBeautySmoothVal, tvBeautyWhitenVal;

    // ─── Countdown ────────────────────────────────────────────────
    private LinearLayout countdownOverlay;
    private TextView tvCountdown;

    // ─── CameraX ──────────────────────────────────────────────────
    private ProcessCameraProvider cameraProvider;
    private VideoCapture<Recorder> videoCapture;
    private Recording activeRecording;
    private CameraControl cameraControl;

    // ─── State ────────────────────────────────────────────────────
    private boolean isRecording = false;
    private boolean isFrontCamera = false;
    private boolean isFlashOn = false;
    private boolean isDotVisible = true;

    private int recordingSeconds = 0;
    private int maxRecordingSeconds = 60;
    private int countdownSeconds = 0;
    private float currentSpeed = 1.0f;
    private int speedIndex = 2;
    private int beautySmoothLevel = 50;
    private int beautyWhitenLevel = 30;
    private int selectedFilterIndex = 0;

    private String selectedMusicUri = null;
    private String selectedMusicName = null;
    private long musicStartMs = 0;    // ★ ServerMusicPicker se milega
    private long musicEndMs = 0;    // ★ ServerMusicPicker se milega

    private View currentOpenPanel = null;
    private String savedVideoUri = null;   // URI string after recording

    // ─── Runnables ────────────────────────────────────────────────
    private Runnable timerRunnable;
    private Runnable blinkRunnable;
    private Runnable countdownRunnable;

    // ─── Music Player (for preview + sync) ───────────────────────
    private android.media.MediaPlayer musicPlayer = null;
    private ActivityResultLauncher<Intent> galleryLauncher;
    // ══════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera_recorder);
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Uri videoUri = result.getData().getData();
                    if (videoUri == null) return;
                    try {
                        getContentResolver().takePersistableUriPermission(
                                videoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {}
                    handleGalleryVideo(videoUri);
                }
        );
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        initViews();
        setupFilters();
        setupTimerRunnable();
        setClickListeners();

        // Default: 60s selected
        updateDurationSelection(dur60s);
        setMaxDuration(60);

        if (hasAllPermissions()) startCamera();
        else requestPermissions();
    }

    // ──────────────────────────────────────────────────────────────
    //  initViews
    // ──────────────────────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    private void initViews() {
        cameraPreview = findViewById(R.id.cameraPreview);
        filterOverlay = findViewById(R.id.filterOverlay);

        btnClose = findViewById(R.id.btnClose);
        btnFlash = findViewById(R.id.btnFlash);

        leftOptionsPanel = findViewById(R.id.leftOptionsPanel);
        btnAddMusic = findViewById(R.id.btnAddMusic);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnFilter = findViewById(R.id.btnFilter);
        btnTimer = findViewById(R.id.btnTimer);
        btnBeauty = findViewById(R.id.btnBeauty);
        tvMusicName = findViewById(R.id.tvMusicName);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        tvTimerValue = findViewById(R.id.tvTimerValue);
        tvBeautyValue = findViewById(R.id.tvBeautyValue);

        btnGallery = findViewById(R.id.btnGallery);
        btnRecord = findViewById(R.id.btnRecord);
        btnFlipCamera = findViewById(R.id.btnFlipCamera);
        bottomBar = findViewById(R.id.bottomBar);
        durationBar = findViewById(R.id.durationBar);
        dur15s = findViewById(R.id.dur15s);
        dur30s = findViewById(R.id.dur30s);
        dur60s = findViewById(R.id.dur60s);
        dur3min = findViewById(R.id.dur3min);

        recordingTopBar = findViewById(R.id.recordingTopBar);
        recordingIndicatorDot = findViewById(R.id.recordingIndicatorDot);
        tvRecordingTimer = findViewById(R.id.tvRecordingTimer);
        progressBar = findViewById(R.id.recordingProgressBar);

        speedPanel = findViewById(R.id.speedPanel);
        timerPanel = findViewById(R.id.timerPanel);
        beautyPanel = findViewById(R.id.beautyPanel);
        filterPanel = findViewById(R.id.filterPanel);
        filterList = findViewById(R.id.filterList);

        speed03x = findViewById(R.id.speed03x);
        speed05x = findViewById(R.id.speed05x);
        speed1x = findViewById(R.id.speed1x);
        speed2x = findViewById(R.id.speed2x);
        speed3x = findViewById(R.id.speed3x);

        timerOff = findViewById(R.id.timerOff);
        timer3s = findViewById(R.id.timer3s);
        timer10s = findViewById(R.id.timer10s);

        seekBeautySmooth = findViewById(R.id.seekBeautySmooth);
        seekBeautyWhiten = findViewById(R.id.seekBeautyWhiten);
        tvBeautySmoothVal = findViewById(R.id.tvBeautySmoothVal);
        tvBeautyWhitenVal = findViewById(R.id.tvBeautyWhitenVal);

        countdownOverlay = findViewById(R.id.countdownOverlay);
        tvCountdown = findViewById(R.id.tvCountdown);

        // Defaults
        if (tvSpeedValue != null) tvSpeedValue.setText("1x");
        if (tvTimerValue != null) tvTimerValue.setText("Off");
        if (tvMusicName != null) tvMusicName.setText("Add Music");
        if (durationBar != null) durationBar.setVisibility(View.VISIBLE);

        // LEFT PANEL always visible on open
        if (leftOptionsPanel != null) leftOptionsPanel.setVisibility(View.VISIBLE);
    }

    // ──────────────────────────────────────────────────────────────
    //  Click Listeners
    // ──────────────────────────────────────────────────────────────
    private void setClickListeners() {

        // ── Close ──
        btnClose.setOnClickListener(v -> {
            if (isRecording) stopRecording();
            releaseMusic();
            finish();
        });

        // ── Flash ──
        btnFlash.setOnClickListener(v -> toggleFlash());

        // ── Record ──
        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                closeAllPanels();
                if (countdownSeconds > 0) startCountdownThenRecord();
                else startRecording();
            }
        });

        // ── Flip Camera (bottom-right) ──
        btnFlipCamera.setOnClickListener(v -> {
            if (isRecording) {
                Toast.makeText(this, "Stop the recording before switching the camera.", Toast.LENGTH_SHORT).show();
                return;
            }
            isFrontCamera = !isFrontCamera;
            if (isFrontCamera && isFlashOn) {
                isFlashOn = false;
                btnFlash.setImageResource(R.drawable.flash_off);
            }
            startCamera();
        });

        // ── Gallery ──
/*        btnGallery.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            pick.setType("video/*");
            startActivityForResult(pick, GALLERY_REQUEST_CODE);
        });*/
        btnGallery.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            pick.setType("video/*");
            galleryLauncher.launch(pick);
        });

        // ── Add Music ──
        if (btnAddMusic != null) btnAddMusic.setOnClickListener(v -> {
            closeAllPanels();
            openMusicPicker();
        });

        // ── Speed ──
        if (btnSpeed != null) btnSpeed.setOnClickListener(v -> togglePanel(speedPanel));

        // ── Filter ──
        if (btnFilter != null) btnFilter.setOnClickListener(v -> togglePanel(filterPanel));

        // ── Timer ──
        if (btnTimer != null) btnTimer.setOnClickListener(v -> togglePanel(timerPanel));

        // ── Beauty ──
        if (btnBeauty != null) btnBeauty.setOnClickListener(v -> togglePanel(beautyPanel));

        // ── Speed Panel ──
        View.OnClickListener speedListener = v -> {
            int idx = 2;
            if (v.getId() == R.id.speed03x) idx = 0;
            else if (v.getId() == R.id.speed05x) idx = 1;
            else if (v.getId() == R.id.speed1x) idx = 2;
            else if (v.getId() == R.id.speed2x) idx = 3;
            else if (v.getId() == R.id.speed3x) idx = 4;
            setSpeed(idx);
        };
        if (speed03x != null) speed03x.setOnClickListener(speedListener);
        if (speed05x != null) speed05x.setOnClickListener(speedListener);
        if (speed1x != null) speed1x.setOnClickListener(speedListener);
        if (speed2x != null) speed2x.setOnClickListener(speedListener);
        if (speed3x != null) speed3x.setOnClickListener(speedListener);

        // ── Timer Panel ──
        if (timerOff != null) timerOff.setOnClickListener(v -> setCountdownTimer(0));
        if (timer3s != null) timer3s.setOnClickListener(v -> setCountdownTimer(3));
        if (timer10s != null) timer10s.setOnClickListener(v -> setCountdownTimer(10));

        // ── Duration Bar ──
        if (dur15s != null) dur15s.setOnClickListener(v -> {
            setMaxDuration(15);
            updateDurationSelection(dur15s);
        });
        if (dur30s != null) dur30s.setOnClickListener(v -> {
            setMaxDuration(30);
            updateDurationSelection(dur30s);
        });
        if (dur60s != null) dur60s.setOnClickListener(v -> {
            setMaxDuration(60);
            updateDurationSelection(dur60s);
        });
        if (dur3min != null) dur3min.setOnClickListener(v -> {
            setMaxDuration(180);
            updateDurationSelection(dur3min);
        });

        // ── Beauty Sliders ──
        if (seekBeautySmooth != null)
            seekBeautySmooth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean u) {
                    beautySmoothLevel = p;
                    if (tvBeautySmoothVal != null) tvBeautySmoothVal.setText(String.valueOf(p));
                    applyBeautyEffect();
                }

                @Override
                public void onStartTrackingTouch(SeekBar s) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar s) {
                }
            });

        if (seekBeautyWhiten != null)
            seekBeautyWhiten.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar s, int p, boolean u) {
                    beautyWhitenLevel = p;
                    if (tvBeautyWhitenVal != null) tvBeautyWhitenVal.setText(String.valueOf(p));
                    applyBeautyEffect();
                }

                @Override
                public void onStartTrackingTouch(SeekBar s) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar s) {
                }
            });
    }
    private void handleGalleryVideo(Uri videoUri) {
        new Thread(() -> {

            //  Step 1: content:// ko cache file mein copy karo
            String cachedPath = copyUriToCacheFile(videoUri.toString());
            if (cachedPath == null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Video load failed", Toast.LENGTH_SHORT).show());
                return;
            }

            //  Step 2: Thumbnail generate karo
            String thumbPath = null;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(cachedPath); //  absolute path use karo, URI nahi
                Bitmap frame = mmr.getFrameAtTime(500_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                mmr.release();

                if (frame != null) {
                    File tf = new File(getCacheDir(),
                            "thumb_gallery_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(tf);
                    frame.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.close();
                    thumbPath = tf.getAbsolutePath();
                }
            } catch (Exception e) {
                Log.e(TAG, "Gallery thumbnail failed: " + e.getMessage());
            }
            //  Step 3: Duration nikalo
            long durationMs = 0;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(cachedPath);
                String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (dur != null) durationMs = Long.parseLong(dur);
                mmr.release();
            } catch (Exception ignored) {}

            final String finalThumb = thumbPath;
            final long finalDuration = durationMs;

            runOnUiThread(() -> {
                Intent intent = new Intent(this, ReelPostActivity.class);
                intent.putExtra("rendered_video_path", cachedPath);
                intent.putExtra("duration_ms", finalDuration);
                if (finalThumb != null) intent.putExtra("thumbnail_path", finalThumb);
                startActivity(intent);
                overridePendingTransition(
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right);
            });

        }).start();
    }
    // ──────────────────────────────────────────────────────────────
    //  Music Picker  ★ ReelEditorActivity jaisa — ServerMusicPickerBottomSheet
    // ──────────────────────────────────────────────────────────────
    private void openMusicPicker() {
        ServerMusicPickerBottomSheet sheet = ServerMusicPickerBottomSheet.newInstance(
                (path, start, end, title) -> {
                    // path = server-downloaded local file path
                    selectedMusicUri = path;   // absolute path (server music cache)
                    selectedMusicName = title;
                    musicStartMs = start;
                    musicEndMs = end;

                    // UI update
                    if (tvMusicName != null) tvMusicName.setText(title);

                    // Music icon highlight — same as ReelEditorActivity
                    if (btnAddMusic != null) btnAddMusic.setAlpha(1f);

                    Toast.makeText(this, "✓ Music: " + title, Toast.LENGTH_SHORT).show();
                });
        sheet.show(getSupportFragmentManager(), "music_picker");
    }

    /**
     * Start music playback alongside recording so user can hear it.
     * The actual audio mixing into final video happens in mixMusicIntoVideo().
     */
    private void startMusicPlayback() {
        if (selectedMusicUri == null) return;
        try {
            releaseMusic();
            musicPlayer = new android.media.MediaPlayer();

            // ★ ServerMusicPicker returns absolute file path — use setDataSource(path)
            // If it's still a content:// URI (fallback), use context overload
            Uri uri = Uri.parse(selectedMusicUri);
            if ("content".equals(uri.getScheme())) {
                musicPlayer.setDataSource(this, uri);
            } else {
                musicPlayer.setDataSource(selectedMusicUri); // absolute path
            }

            // ★ Seek to musicStartMs if set (user picked a clip trim)
            musicPlayer.setLooping(false);
            musicPlayer.setVolume(0.8f, 0.8f);
            musicPlayer.prepareAsync();
            musicPlayer.setOnPreparedListener(mp -> {
                if (musicStartMs > 0) mp.seekTo((int) musicStartMs);
                mp.start();
            });
            musicPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: " + what);
                return true;
            });
        } catch (Exception e) {
            Log.e(TAG, "Music playback failed: " + e.getMessage());
        }
    }

    private void stopMusicPlayback() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.stop();
        }
    }

    private void releaseMusic() {
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
            } catch (Exception ignored) {
            }
            musicPlayer.release();
            musicPlayer = null;
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Panel Helper
    // ──────────────────────────────────────────────────────────────
    private void togglePanel(View panel) {
        if (panel == null) return;
        if (currentOpenPanel == panel) {
            panel.setVisibility(View.GONE);
            currentOpenPanel = null;
        } else {
            if (currentOpenPanel != null) currentOpenPanel.setVisibility(View.GONE);
            panel.setVisibility(View.VISIBLE);
            currentOpenPanel = panel;
        }
    }

    private void closeAllPanels() {
        if (speedPanel != null) speedPanel.setVisibility(View.GONE);
        if (timerPanel != null) timerPanel.setVisibility(View.GONE);
        if (beautyPanel != null) beautyPanel.setVisibility(View.GONE);
        if (filterPanel != null) filterPanel.setVisibility(View.GONE);
        currentOpenPanel = null;
    }

    // ──────────────────────────────────────────────────────────────
    //  Speed
    // ──────────────────────────────────────────────────────────────
    private void setSpeed(int idx) {
        speedIndex = idx;
        currentSpeed = speedOptions[idx];
        String label = speedLabels[idx];
        if (tvSpeedValue != null) tvSpeedValue.setText(label);

        TextView[] views = {speed03x, speed05x, speed1x, speed2x, speed3x};
        for (int i = 0; i < views.length; i++) {
            if (views[i] == null) continue;
            views[i].setTextColor(i == idx ? 0xFFFF3B30 : 0xAAFFFFFF);
            views[i].setTextSize(i == idx ? 18f : 15f);
        }
        closeAllPanels();
        Toast.makeText(this, "Speed: " + label, Toast.LENGTH_SHORT).show();
    }

    // ──────────────────────────────────────────────────────────────
    //  Timer
    // ──────────────────────────────────────────────────────────────
    private void setCountdownTimer(int seconds) {
        countdownSeconds = seconds;
        if (tvTimerValue != null) tvTimerValue.setText(seconds == 0 ? "Off" : seconds + "s");
        int active = 0xFFFF3B30;
        int inactive = 0xAAFFFFFF;
        if (timerOff != null) timerOff.setTextColor(seconds == 0 ? active : inactive);
        if (timer3s != null) timer3s.setTextColor(seconds == 3 ? active : inactive);
        if (timer10s != null) timer10s.setTextColor(seconds == 10 ? active : inactive);
        closeAllPanels();
    }

    // ──────────────────────────────────────────────────────────────
    //  Duration
    // ──────────────────────────────────────────────────────────────
    private void setMaxDuration(int seconds) {
        maxRecordingSeconds = seconds;
    }

    private void updateDurationSelection(TextView selected) {
        TextView[] durs = {dur15s, dur30s, dur60s, dur3min};
        for (TextView t : durs) {
            if (t == null) continue;
            if (t == selected) {
                t.setBackgroundColor(0xFFFF3B30);
                t.setTextColor(0xFFFFFFFF);
                t.setTypeface(t.getTypeface(), Typeface.BOLD);
            } else {
                t.setBackgroundColor(0x00000000);
                t.setTextColor(0xAAFFFFFF);
                t.setTypeface(t.getTypeface(), Typeface.NORMAL);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Filters
    // ──────────────────────────────────────────────────────────────
    private void setupFilters() {
        if (filterList == null) return;
        filterList.removeAllViews();
        int[] tints = {0xFF888888, 0xFFFF6B35, 0xFFFFB347, 0xFF4FC3F7, 0xFF757575, 0xFFD4A574, 0xFFB0BEC5};

        for (int i = 0; i < filterNames.length; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(android.view.Gravity.CENTER);

            View circle = new View(this);
            int size = (int) (52 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(12, 0, 12, 0);
            circle.setLayoutParams(lp);
            circle.setBackgroundColor(tints[i % tints.length]);

            TextView label = new TextView(this);
            label.setText(filterNames[i]);
            label.setTextColor(0xFFFFFFFF);
            label.setTextSize(10);
            label.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    WRAP_CONTENT,
                   WRAP_CONTENT);
            tlp.topMargin = 4;
            label.setLayoutParams(tlp);

            item.addView(circle);
            item.addView(label);
            item.setOnClickListener(v -> {
                selectedFilterIndex = idx;
                applyFilter(idx);
                highlightSelectedFilter(idx);
                closeAllPanels();
            });
            filterList.addView(item);
        }
        highlightSelectedFilter(0);
    }

    private void highlightSelectedFilter(int idx) {
        if (filterList == null) return;
        for (int i = 0; i < filterList.getChildCount(); i++) {
            filterList.getChildAt(i).setAlpha(i == idx ? 1.0f : 0.5f);
        }
    }

    private void applyFilter(int idx) {
        if (filterOverlay == null) return;
        if (idx == 0) {
            filterOverlay.setBackgroundColor(Color.TRANSPARENT);
            filterOverlay.setAlpha(0f);
        } else {
            filterOverlay.setBackgroundColor(filterColors[idx]);
            filterOverlay.setAlpha(1f);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Beauty
    // ──────────────────────────────────────────────────────────────
    private void applyBeautyEffect() {
        boolean enabled = (beautySmoothLevel > 0 || beautyWhitenLevel > 0);
        if (filterOverlay == null || cameraPreview == null) return;
        if (enabled) {
            int alpha = Math.min(80, beautyWhitenLevel);
            filterOverlay.setBackgroundColor(Color.WHITE);
            filterOverlay.setAlpha(alpha / 255f);
            cameraPreview.setAlpha(1f - (beautySmoothLevel / 500f));
        } else {
            filterOverlay.setBackgroundColor(Color.TRANSPARENT);
            filterOverlay.setAlpha(0f);
            cameraPreview.setAlpha(1f);
        }
        if (tvBeautyValue != null) tvBeautyValue.setText(enabled ? "On" : "Beauty");
    }

    // ──────────────────────────────────────────────────────────────
    //  Flash
    // ──────────────────────────────────────────────────────────────
    private void toggleFlash() {
        if (isFrontCamera) {
            Toast.makeText(this, "Flash is not supported on the front camera.", Toast.LENGTH_SHORT).show();
            return;
        }
        isFlashOn = !isFlashOn;
        if (cameraControl != null) cameraControl.enableTorch(isFlashOn);
        btnFlash.setImageResource(isFlashOn ? R.drawable.flash_on : R.drawable.flash_off);
    }

    // ──────────────────────────────────────────────────────────────
    //  Camera Setup (CameraX)
    // ──────────────────────────────────────────────────────────────
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera start failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        CameraSelector selector = isFrontCamera
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;

        provider.unbindAll();
        Camera camera = provider.bindToLifecycle(this, selector, preview, videoCapture);
        cameraControl = camera.getCameraControl();

        if (!isFrontCamera && isFlashOn) cameraControl.enableTorch(true);
    }

    // ──────────────────────────────────────────────────────────────
    //  Countdown
    // ──────────────────────────────────────────────────────────────
    private void startCountdownThenRecord() {
        if (countdownOverlay == null || tvCountdown == null) {
            startRecording();
            return;
        }
        countdownOverlay.setVisibility(View.VISIBLE);
        setSideOptionsEnabled(false);

        final int[] remaining = {countdownSeconds};
        tvCountdown.setText(String.valueOf(remaining[0]));
        tvCountdown.setScaleX(1.5f);
        tvCountdown.setScaleY(1.5f);
        tvCountdown.animate().scaleX(1f).scaleY(1f).setDuration(700).start();

        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                remaining[0]--;
                if (remaining[0] <= 0) {
                    countdownOverlay.setVisibility(View.GONE);
                    startRecording();
                } else {
                    tvCountdown.setText(String.valueOf(remaining[0]));
                    tvCountdown.setScaleX(1.5f);
                    tvCountdown.setScaleY(1.5f);
                    tvCountdown.animate().scaleX(1f).scaleY(1f).setDuration(700).start();
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(countdownRunnable, 1000);
    }

    // ──────────────────────────────────────────────────────────────
    //  Start Recording
    // ──────────────────────────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private void startRecording() {
        if (videoCapture == null) {
            Toast.makeText(this, "Camera is not ready yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Reel_" + timestamp + ".mp4";

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        cv.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Reels");

        MediaStoreOutputOptions outputOptions = new MediaStoreOutputOptions
                .Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(cv)
                .build();

        activeRecording = videoCapture.getOutput()
                .prepareRecording(this, outputOptions)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), event -> {
                    if (event instanceof VideoRecordEvent.Start) {
                        isRecording = true;
                        runOnUiThread(this::onRecordingStarted);

                    } else if (event instanceof VideoRecordEvent.Finalize fin) {
                        isRecording = false;
                        runOnUiThread(() -> {
                            stopMusicPlayback();
                            if (!fin.hasError()) {
                                savedVideoUri = fin.getOutputResults().getOutputUri().toString();
                                onRecordingStopped(savedVideoUri);
                            } else {
                                Log.e(TAG, "Recording error code: " + fin.getError());
                                Toast.makeText(this, "Recording mein error aayi", Toast.LENGTH_SHORT).show();
                                onRecordingCancelled();
                            }
                        });
                    }
                });
    }

    // ──────────────────────────────────────────────────────────────
    //  Stop Recording
    // ──────────────────────────────────────────────────────────────
    private void stopRecording() {
        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
        stopTimer();
    }

    // ──────────────────────────────────────────────────────────────
    //  Recording UI Callbacks
    // ──────────────────────────────────────────────────────────────
    private void onRecordingStarted() {
        btnRecord.setImageResource(android.R.drawable.ic_media_pause);
        btnRecord.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFFFF3B30));

        if (recordingTopBar != null) recordingTopBar.setVisibility(View.VISIBLE);
        if (durationBar != null) durationBar.setVisibility(View.GONE);
        if (leftOptionsPanel != null) leftOptionsPanel.setVisibility(View.GONE);

        recordingSeconds = 0;
        startBlinkAnimation();
        startTimer();
        setSideOptionsEnabled(false);

        // ★ Start music playback so user hears it while recording
        startMusicPlayback();
    }

    private void onRecordingStopped(String videoUri) {
        resetRecordButton();
        if (recordingTopBar != null) recordingTopBar.setVisibility(View.GONE);
        if (durationBar != null) durationBar.setVisibility(View.VISIBLE);
        if (leftOptionsPanel != null) leftOptionsPanel.setVisibility(View.VISIBLE);
        stopBlinkAnimation();
        setSideOptionsEnabled(true);

        // ★ Generate thumbnail then go to post screen
        generateThumbnailAndNavigate(videoUri);
    }

    private void onRecordingCancelled() {
        resetRecordButton();
        if (recordingTopBar != null) recordingTopBar.setVisibility(View.GONE);
        if (durationBar != null) durationBar.setVisibility(View.VISIBLE);
        if (leftOptionsPanel != null) leftOptionsPanel.setVisibility(View.VISIBLE);
        stopBlinkAnimation();
        setSideOptionsEnabled(true);
    }

    private void resetRecordButton() {
        btnRecord.setImageResource(android.R.drawable.ic_media_play);
        btnRecord.setBackgroundResource(R.drawable.record_inner_circle);
        btnRecord.setBackgroundTintList(null);
    }

    // ──────────────────────────────────────────────────────────────
    //  ★ Thumbnail Generation
    //  Async: video se JPEG thumbnail nikalte hain, then navigate
    // ──────────────────────────────────────────────────────────────
    private void generateThumbnailAndNavigate(String videoUri) {
        runOnUiThread(() ->
                Toast.makeText(this, "Video is being processed...", Toast.LENGTH_SHORT).show());

        new Thread(() -> {
            String cachedVideoPath = copyUriToCacheFile(videoUri);
            if (cachedVideoPath == null) {
                Log.e(TAG, "Video copy failed, falling back to original URI");
                cachedVideoPath = videoUri;
            }
            Log.d(TAG, "Cached video path: " + cachedVideoPath);

            // ★ STEP 2: Thumbnail generate karo cached file se
            String thumbPath = null;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(cachedVideoPath);  // absolute path — always works
                Bitmap frame = mmr.getFrameAtTime(500_000L,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                mmr.release();

                if (frame != null) {
                    if (selectedFilterIndex > 0) {
                        frame = applyFilterToBitmap(frame, filterColors[selectedFilterIndex]);
                    }
                    File thumbFile = new File(getCacheDir(),
                            "thumb_reel_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(thumbFile);
                    frame.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.close();
                    thumbPath = thumbFile.getAbsolutePath();
                    Log.d(TAG, "Thumbnail saved: " + thumbPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "Thumbnail generation failed: " + e.getMessage());
            }

            // ★ STEP 3: Music mix karo (file path se) ya directly navigate karo
            final String finalThumbPath = thumbPath;
            final String finalVideoPath = cachedVideoPath;

            if (selectedMusicUri != null) {
                mixMusicAndNavigate(finalVideoPath, finalThumbPath);
            } else {
                navigateToPostScreen(finalVideoPath, null, finalThumbPath);
            }
        }).start();
    }

    /**
     * ★ content:// URI se video ko cache directory mein copy karo.
     * Sirf absolute file path return karta hai — File operations ke liye safe.
     */
    private String copyUriToCacheFile(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            String scheme = uri.getScheme();
            // Already absolute path — copy nahi chahiye
            if ("file".equals(scheme)) return uri.getPath();
            if (!("content".equals(scheme))) return uriString;

            File destFile = new File(getCacheDir(),
                    "reel_raw_" + System.currentTimeMillis() + ".mp4");

            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                 java.io.OutputStream out = new java.io.FileOutputStream(destFile)) {
                if (in == null) {
                    Log.e(TAG, "Cannot open input stream for URI: " + uriString);
                    return null;
                }
                byte[] buf = new byte[64 * 1024]; // 64 KB chunks
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            Log.d(TAG, "Video copied → " + destFile.getAbsolutePath()
                    + "  size=" + destFile.length() + " bytes");
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "copyUriToCacheFile failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * ★ Mix music audio into the recorded video using MediaMuxer.
     * This creates a new MP4 with both the original video+mic track and the music track.
     */
    private void mixMusicAndNavigate(String videoPath, String thumbPath) {
        // videoPath is always an absolute cache file path at this point
        runOnUiThread(() ->
                Toast.makeText(this, "Music is being mixed successfully.", Toast.LENGTH_SHORT).show());

        new Thread(() -> {
            String mixedPath = null;
            try {
                mixedPath = mixAudioTracks(videoPath, selectedMusicUri);
            } catch (Exception e) {
                Log.e(TAG, "Music mixing failed: " + e.getMessage());
                // Fallback: original video (without music) pass karo
            }
            // Mixed path ya fallback original path
            String finalVideo = (mixedPath != null) ? mixedPath : videoPath;
            navigateToPostScreen(finalVideo, selectedMusicName, thumbPath);
        }).start();
    }

    /**
     * ★ Core audio mixing:
     * - Extracts video track from recorded file
     * - Extracts audio track from selected music file
     * - Muxes both into a new MP4 file
     */
    private String mixAudioTracks(String videoPath, String musicPath) throws Exception {
        File outputFile = new File(getCacheDir(),
                "mixed_reel_" + System.currentTimeMillis() + ".mp4");

        // videoPath is always an absolute file path (copied from content:// earlier)
        // musicUri may still be content:// — use context overload for it
        MediaExtractor videoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(videoPath);   // ★ absolute path, no context needed

        MediaExtractor musicExtractor = new MediaExtractor();
        // ★ ServerMusicPicker returns absolute path — use direct setDataSource
        // Fallback: content:// URI support via context overload
        Uri musicUriParsed = Uri.parse(musicPath);
        if ("content".equals(musicUriParsed.getScheme())) {
            musicExtractor.setDataSource(this, musicUriParsed, null);
        } else {
            musicExtractor.setDataSource(musicPath); // absolute path
        }

        MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        // ── Find video track ──
        int videoTrackIndex = -1;
        long videoDurationUs = 0;
        MediaFormat videoFormat = null;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat fmt = videoExtractor.getTrackFormat(i);
            String mime = fmt.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("video/")) {
                videoTrackIndex = i;
                videoFormat = fmt;
                if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
                    videoDurationUs = fmt.getLong(MediaFormat.KEY_DURATION);
                }
                break;
            }
        }

        // ── Find audio track from video (mic) ──
        int micTrackIndex = -1;
        MediaFormat micFormat = null;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat fmt = videoExtractor.getTrackFormat(i);
            String mime = fmt.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                micTrackIndex = i;
                micFormat = fmt;
                break;
            }
        }

        // ── Find music audio track ──
        int musicTrackIndex = -1;
        MediaFormat musicFormat = null;
        for (int i = 0; i < musicExtractor.getTrackCount(); i++) {
            MediaFormat fmt = musicExtractor.getTrackFormat(i);
            String mime = fmt.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                musicTrackIndex = i;
                musicFormat = fmt;
                break;
            }
        }

        if (videoTrackIndex < 0) {
            videoExtractor.release();
            musicExtractor.release();
            muxer.release();
            throw new Exception("No video track found");
        }

        // ── Add tracks to muxer ──
        videoExtractor.selectTrack(videoTrackIndex);
        int muxVideoTrack = muxer.addTrack(videoFormat);

        int muxMicTrack = -1;
        if (micTrackIndex >= 0) {
            videoExtractor.selectTrack(micTrackIndex);
            muxMicTrack = muxer.addTrack(micFormat);
        }

        int muxMusicTrack = -1;
        if (musicTrackIndex >= 0 && musicFormat != null) {
            musicExtractor.selectTrack(musicTrackIndex);
            muxMusicTrack = muxer.addTrack(musicFormat);
        }

        muxer.start();

        ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // 1 MB buffer
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        // ── Write video track ──
        videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        while (true) {
            bufferInfo.offset = 0;
            bufferInfo.size = videoExtractor.readSampleData(buffer, 0);
            if (bufferInfo.size < 0) break;

            int trackIdx = videoExtractor.getSampleTrackIndex();
            bufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
            // ★ Convert MediaExtractor flags → MediaCodec.BufferInfo flags
            bufferInfo.flags = convertExtractorFlags(videoExtractor.getSampleFlags());

            if (trackIdx == videoTrackIndex) {
                muxer.writeSampleData(muxVideoTrack, buffer, bufferInfo);
            } else if (trackIdx == micTrackIndex && muxMicTrack >= 0) {
                muxer.writeSampleData(muxMicTrack, buffer, bufferInfo);
            }
            videoExtractor.advance();
        }

        // ── Write music audio track (trim to video duration, respect musicStartMs) ──
        if (muxMusicTrack >= 0) {
            // ★ Seek to user-selected start time (ServerMusicPicker trim)
            long seekToUs = musicStartMs * 1000L;
            musicExtractor.seekTo(seekToUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC);

            // ★ Max music duration = video duration (loop music if shorter)
            long musicMaxUs = (musicEndMs > musicStartMs)
                    ? (musicEndMs - musicStartMs) * 1000L
                    : videoDurationUs;

            long presentationOffsetUs = 0; // re-base timestamps to 0

            while (true) {
                bufferInfo.offset = 0;
                bufferInfo.size = musicExtractor.readSampleData(buffer, 0);
                if (bufferInfo.size < 0) break;

                long rawTimeUs = musicExtractor.getSampleTime();
                presentationOffsetUs = rawTimeUs - seekToUs; // normalize to 0-based

                // Don't exceed video duration or music clip end
                if (presentationOffsetUs < 0) {
                    musicExtractor.advance();
                    continue;
                }
                if (videoDurationUs > 0 && presentationOffsetUs > videoDurationUs) break;
                if (musicMaxUs > 0 && presentationOffsetUs > musicMaxUs) break;

                bufferInfo.presentationTimeUs = presentationOffsetUs;
                // ★ Convert MediaExtractor flags → MediaCodec.BufferInfo flags
                bufferInfo.flags = convertExtractorFlags(musicExtractor.getSampleFlags());

                muxer.writeSampleData(muxMusicTrack, buffer, bufferInfo);
                musicExtractor.advance();
            }
        }

        videoExtractor.release();
        musicExtractor.release();
        muxer.stop();
        muxer.release();

        Log.d(TAG, " Music mixed successfully: " + outputFile.getAbsolutePath());
        return outputFile.getAbsolutePath();
    }

    /**
     * ★ Convert MediaExtractor sample flags → MediaCodec.BufferInfo flags.
     * <p>
     * MediaExtractor uses:
     * SAMPLE_FLAG_SYNC      (1) → sync/key frame
     * SAMPLE_FLAG_ENCRYPTED (2) → encrypted
     * SAMPLE_FLAG_PARTIAL_FRAME (4) → partial
     * <p>
     * MediaCodec.BufferInfo expects:
     * BUFFER_FLAG_KEY_FRAME / BUFFER_FLAG_SYNC_FRAME (1)
     * BUFFER_FLAG_CODEC_CONFIG (2)
     * BUFFER_FLAG_PARTIAL_FRAME (8)
     * BUFFER_FLAG_END_OF_STREAM (4)
     * <p>
     * We only map the flags MediaMuxer actually needs.
     */
    private int convertExtractorFlags(int extractorFlags) {
        int muxerFlags = 0;
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
            muxerFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;   // = 1, same value but explicit
        }
        if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
            muxerFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
        }
        // SAMPLE_FLAG_ENCRYPTED is not written to muxer — skip it
        return muxerFlags;
    }

    /**
     * ★ Apply filter color tint to a Bitmap (for thumbnail)
     */
    private Bitmap applyFilterToBitmap(Bitmap original, int filterColor) {
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);
        android.graphics.Canvas canvas = new android.graphics.Canvas(result);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(filterColor);
        paint.setXfermode(new android.graphics.PorterDuffXfermode(
                android.graphics.PorterDuff.Mode.SRC_OVER));
        canvas.drawRect(0, 0, result.getWidth(), result.getHeight(), paint);
        return result;
    }

    // ──────────────────────────────────────────────────────────────
    //  Navigate to ReelPostActivity
    // ──────────────────────────────────────────────────────────────
    private void navigateToPostScreen(String videoPath, String musicName, String thumbPath) {
        runOnUiThread(() -> {
            Intent intent = new Intent(this, ReelPostActivity.class);

            // ★ Use rendered_video_path (ReelPostActivity expects this key)
            intent.putExtra("rendered_video_path", videoPath);

            // Duration
            intent.putExtra("duration_ms", recordingSeconds * 1000L);

            // Thumbnail
            if (thumbPath != null) {
                intent.putExtra("thumbnail_path", thumbPath);
            }

            // Music info
            if (musicName != null) {
                intent.putExtra("music_name", musicName);
                intent.putExtra("music_uri", selectedMusicUri);
            }

            // Filter info
            intent.putExtra("filter_name", filterNames[selectedFilterIndex]);
            intent.putExtra("speed_label", speedLabels[speedIndex]);

            // Timestamp
            intent.putExtra("recorded_at", new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date()));

            startActivity(intent);
            overridePendingTransition(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right);
        });
    }

    // ──────────────────────────────────────────────────────────────
    //  Resolve content:// URI to file path
    // ──────────────────────────────────────────────────────────────
    private String resolveContentUriToPath(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            if ("content".equals(uri.getScheme())) {
                Cursor cursor = getContentResolver().query(uri,
                        new String[]{MediaStore.Video.Media.DATA}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    String path = cursor.getString(0);
                    cursor.close();
                    return path;
                }
            } else if ("file".equals(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            Log.e(TAG, "URI resolve failed: " + e.getMessage());
        }
        return null;
    }

    // ──────────────────────────────────────────────────────────────
    //  Timer Logic
    // ──────────────────────────────────────────────────────────────
    private void setupTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                recordingSeconds++;
                int min = recordingSeconds / 60;
                int sec = recordingSeconds % 60;
                if (tvRecordingTimer != null)
                    tvRecordingTimer.setText(String.format(Locale.US, "%02d:%02d", min, sec));

                if (progressBar != null) {
                    float fraction = (float) recordingSeconds / maxRecordingSeconds;
                    progressBar.setScaleX(Math.min(fraction, 1f));
                }

                if (recordingSeconds >= maxRecordingSeconds) {
                    stopRecording();
                    return;
                }
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void startTimer() {
        handler.postDelayed(timerRunnable, 1000);
    }

    private void stopTimer() {
        handler.removeCallbacks(timerRunnable);
    }

    // ──────────────────────────────────────────────────────────────
    //  Blink Animation (REC dot)
    // ──────────────────────────────────────────────────────────────
    private void startBlinkAnimation() {
        if (recordingIndicatorDot == null) return;
        blinkRunnable = new Runnable() {
            @Override
            public void run() {
                isDotVisible = !isDotVisible;
                recordingIndicatorDot.setVisibility(isDotVisible ? View.VISIBLE : View.INVISIBLE);
                handler.postDelayed(this, 600);
            }
        };
        handler.post(blinkRunnable);
    }

    private void stopBlinkAnimation() {
        if (blinkRunnable != null) handler.removeCallbacks(blinkRunnable);
        if (recordingIndicatorDot != null) recordingIndicatorDot.setVisibility(View.GONE);
    }

    // ──────────────────────────────────────────────────────────────
    //  Enable / Disable Side Options
    // ──────────────────────────────────────────────────────────────
    private void setSideOptionsEnabled(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.4f;
        View[] views = {btnAddMusic, btnSpeed, btnFilter, btnTimer, btnBeauty,
                btnFlipCamera, btnGallery, btnFlash};
        for (View v : views) {
            if (v != null) {
                v.setEnabled(enabled);
                v.setAlpha(alpha);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Activity Result
    // ──────────────────────────────────────────────────────────────
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        // ── Gallery pick ──
        if (requestCode == GALLERY_REQUEST_CODE && data.getData() != null) {
            String pickedUri = data.getData().toString();
            // Generate thumbnail for gallery video too
            new Thread(() -> {
                String thumbPath = null;
                try {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(this, data.getData());
                    Bitmap frame = mmr.getFrameAtTime(500_000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    mmr.release();
                    if (frame != null) {
                        File tf = new File(getCacheDir(), "thumb_gallery_" + System.currentTimeMillis() + ".jpg");
                        FileOutputStream fos = new FileOutputStream(tf);
                        frame.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                        fos.close();
                        thumbPath = tf.getAbsolutePath();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Gallery thumbnail failed: " + e.getMessage());
                }
                final String finalThumb = thumbPath;
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, ReelPostActivity.class);
                    intent.putExtra("rendered_video_path", pickedUri);
                    intent.putExtra("duration_ms", 0L);
                    if (finalThumb != null) intent.putExtra("thumbnail_path", finalThumb);
                    startActivity(intent);
                    overridePendingTransition(
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right);
                });
            }).start();
        }

        // ── Music pick — handled by ServerMusicPickerBottomSheet (no onActivityResult needed) ──
    }


    // ──────────────────────────────────────────────────────────────
    //  Permissions
    // ──────────────────────────────────────────────────────────────
    private boolean hasAllPermissions() {
        for (String p : REQUIRED_PERMISSIONS)
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                return false;
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && hasAllPermissions()) {
            startCamera();
        } else {
            Toast.makeText(this,
                    "Camera and microphone permissions are required.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────
    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) stopRecording();
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            if (countdownOverlay != null) countdownOverlay.setVisibility(View.GONE);
            setSideOptionsEnabled(true);
        }
        stopMusicPlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        stopBlinkAnimation();
        if (countdownRunnable != null) handler.removeCallbacks(countdownRunnable);
        releaseMusic();
    }
}