package com.infotech.wishmaplus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.infotech.wishmaplus.Activity.MainActivity;
import com.infotech.wishmaplus.Api.Response.MediaModel;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.UtilMethods;
import com.infotech.wishmaplus.reels.adapter.HashtagSuggestionAdapter;
import com.infotech.wishmaplus.reels.response.HashtagResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class ReelPostActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private ImageView imageView;
    private EditText etCaption, etHashtag;
    private AppCompatButton btnPost;
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private RecyclerView rvHashtagSuggestions;
    private ChipGroup chipGroupHashtags;
    private TextView tvDurationBadge, tvUserName;
    private SwitchCompat switchShareToFeed;

    // ── Data ───────────────────────────────────────────────────────────────────
    private String thumbPath;
    private String finalPageId=null;

    private String renderedVideoPath; // ★ final MP4 from ReelEditorActivity
    private ArrayList<MediaModel> mediaList = new ArrayList<>();
    private final List<String> selectedHashtags = new ArrayList<>();
    private HashtagSuggestionAdapter suggestionAdapter;
    private int durationSeconds = 0;
    private CustomLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reel_post);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        loadIntentData();
        setupHashtagInput();
        setupSuggestionRecycler();
        setupButtons();
    }

    // ── Bind ───────────────────────────────────────────────────────────────────
    private void bindViews() {
        loader = new CustomLoader(this, android.R.style.Theme_Translucent_NoTitleBar);
        imageView = findViewById(R.id.imageView);
        etCaption = findViewById(R.id.etCaption);
        etHashtag = findViewById(R.id.etHashtag);
        btnPost = findViewById(R.id.btnPost);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        rvHashtagSuggestions = findViewById(R.id.rvHashtagSuggestions);
        chipGroupHashtags = findViewById(R.id.chipGroupHashtags);
        tvDurationBadge = findViewById(R.id.tvDurationBadge);
        tvUserName = findViewById(R.id.tvUserName);
        switchShareToFeed = findViewById(R.id.switchShareToFeed);
    }

    // ── Intent data ────────────────────────────────────────────────────────────
    private void loadIntentData() {
        thumbPath = getIntent().getStringExtra("thumbnail_path");
        finalPageId=  getIntent().getStringExtra("pageId");
        renderedVideoPath = getIntent().getStringExtra("rendered_video_path"); // ★
        if (getIntent().hasExtra("media_list")) {
            Object obj = getIntent().getSerializableExtra("media_list");
            if (obj instanceof ArrayList) {
                mediaList = (ArrayList<MediaModel>) obj;
            }
        }
        if (thumbPath != null && new File(thumbPath).exists()
                && thumbPath.endsWith(".jpg")) {
            Log.d("THUMB_DEBUG", "Loading JPEG thumbnail: " + thumbPath);
            Glide.with(this)
                    .load(new File(thumbPath))
                    .centerCrop()
                    .into(imageView);

        } else if (renderedVideoPath != null && new File(renderedVideoPath).exists()) {
            Log.d("THUMB_DEBUG", "Extracting thumb from rendered video");
            new Thread(() -> {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                try {
                    mmr.setDataSource(renderedVideoPath);
                    Bitmap frame = mmr.getFrameAtTime(
                            1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    if (frame != null) {
                        final Bitmap finalFrame = frame;
                        runOnUiThread(() -> imageView.setImageBitmap(finalFrame));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { mmr.release(); } catch (Exception ignored) {}
                }
            }).start();

        } else if (!mediaList.isEmpty()) {
            // Last resort: original media
            Glide.with(this).load(mediaList.get(0).getPath())
                    .centerCrop().into(imageView);
        }

        // Duration — rendered video se prefer karo
        if (renderedVideoPath != null && new File(renderedVideoPath).exists()) {
           MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(renderedVideoPath);
                String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durStr != null) {
                    durationSeconds = (int) (Long.parseLong(durStr) / 1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    mmr.release();
                } catch (Exception ignored) {
                }
            }
        } else if (!mediaList.isEmpty()) {
            durationSeconds = (int) (mediaList.get(0).getDuration() / 1000);
        }
        if (durationSeconds > 0) {
            tvDurationBadge.setText(formatDuration(durationSeconds));
        }
    }

    // ── Format seconds → "5:30" ───────────────────────────────────────────────
    private String formatDuration(int totalSeconds) {
        int m = totalSeconds / 60;
        int s = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", m, s);
    }

    // ── Buttons ────────────────────────────────────────────────────────────────
    private void setupButtons() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnPost.setOnClickListener(v -> postReel());
    }

    // ── Hashtag input watcher ─────────────────────────────────────────────────
    private void setupHashtagInput() {
        etHashtag.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String raw = s.toString();
                String query = raw.trim().replace("#", "");

                // ★ Invalid chars check — only letters, numbers, underscore allowed
                if (!query.isEmpty() && !query.matches("[a-zA-Z0-9_]+")) {
                    etHashtag.setError("Only letters, numbers and _ allowed");
                    hideSuggestions();
                    return;
                }

                if (!query.isEmpty()) {
                    etHashtag.setError(null);
                    fetchSuggestions(query);
                } else {
                    hideSuggestions();
                }
            }
        });

        etHashtag.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addHashtagFromInput();
                return true;
            }
            return false;
        });

        // ★ Space ya comma press karne par bhi chip add ho
        etHashtag.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == android.view.KeyEvent.ACTION_DOWN && keyCode == android.view.KeyEvent.KEYCODE_SPACE) {
                addHashtagFromInput();
                return true;
            }
            return false;
        });
    }

    // ── Hashtag validate + chip add ───────────────────────────────────────────────
    private void addHashtagFromInput() {
        String typed = etHashtag.getText().toString().trim().replace("#", "");

        // Empty check
        if (typed.isEmpty()) return;

        // Length check — min 2, max 30
        if (typed.length() < 2) {
            etHashtag.setError("Hashtag too short (min 2 chars)");
            return;
        }
        if (typed.length() > 30) {
            etHashtag.setError("Hashtag too long (max 30 chars)");
            return;
        }

        // Invalid chars check
        if (!typed.matches("[a-zA-Z0-9_]+")) {
            etHashtag.setError("Only letters, numbers and _ allowed");
            return;
        }

        // Max hashtags check
        if (selectedHashtags.size() >= 10) {
            Toast.makeText(this, "Maximum 10 hashtags allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Duplicate check
        if (selectedHashtags.contains("#" + typed)) {
            etHashtag.setError("Hashtag already added");
            return;
        }

        // ★ All valid — chip add karo
        etHashtag.setError(null);
        addChip("#" + typed);
        etHashtag.setText("");
        hideSuggestions();
    }

    // ── Suggestions RecyclerView ──────────────────────────────────────────────
    private void setupSuggestionRecycler() {
        // Suggestion tap →
        suggestionAdapter = new HashtagSuggestionAdapter(tag -> {
            addChip("#" + tag);
            etHashtag.setText("");
            hideSuggestions();
        });
        rvHashtagSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvHashtagSuggestions.setAdapter(suggestionAdapter);
        rvHashtagSuggestions.setNestedScrollingEnabled(false);
    }

    // ── API: fetch suggestions ────────────────────────────────────────────────
    private void fetchSuggestions(String query) {
        UtilMethods.INSTANCE.getHashtagSuggestions(loader, query, 10, new UtilMethods.ApiCallBackMulti() {
            @Override
            public void onSuccess(Object response) {
                HashtagResponse data = (HashtagResponse) response;
                runOnUiThread(() -> {
                    if (data.result != null && !data.result.isEmpty()) {
                        suggestionAdapter.submitList(data.result);
                        rvHashtagSuggestions.setVisibility(View.VISIBLE);
                    } else {
                        hideSuggestions();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(this::hideSuggestions);
            }

            private void hideSuggestions() {
                rvHashtagSuggestions.setVisibility(View.GONE);
            }
        });
    }

    private void hideSuggestions() {
        rvHashtagSuggestions.setVisibility(View.GONE);
    }

    // ── Add chip ──────────────────────────────────────────────────────────────
    private void addChip(String tag) {
        if (selectedHashtags.contains(tag)) return; // duplicate
        selectedHashtags.add(tag);

        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.colorPrimary);  // #1877F2 with alpha
        chip.setTextColor(getColor(R.color.colorAccent));
        chip.setCloseIconTint(android.content.res.ColorStateList.valueOf(getColor(android.R.color.white)));
        chip.setOnCloseIconClickListener(v -> {
            chipGroupHashtags.removeView(chip);
            selectedHashtags.remove(tag);
        });
        chipGroupHashtags.addView(chip);
    }

    // ── Post Reel ─────────────────────────────────────────────────────────────

    private void postReel() {
        String caption = etCaption.getText().toString().trim();
        String hashtags = String.join(",", selectedHashtags).replace("#", "");
        String pendingTag = etHashtag.getText().toString().trim();
        // Caption validate
        if (caption.isEmpty()) {
            etCaption.setError("Please write a caption");
            etCaption.requestFocus();
            return;
        }

        // Caption length check
        if (caption.length() > 2200) {
            etCaption.setError("Caption too long (max 2200 chars)");
            etCaption.requestFocus();
            return;
        }

        if (!pendingTag.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Unsaved Hashtag").setMessage("\"" + pendingTag + "\" is not added yet. " + "Press Done/Space to add it, or continue without it.").setPositiveButton("Add & Continue", (d, w) -> {
                addHashtagFromInput();
                proceedUpload(caption, hashtags);
            }).setNegativeButton("Continue Without", (d, w) -> {
                proceedUpload(caption, hashtags);
            }).setCancelable(true).show();
            return;
        }
        // ── Hashtag required ──────────────────────────────────────────────────
        if (selectedHashtags.isEmpty()) {
            etHashtag.setError("Add at least one hashtag");
            etHashtag.requestFocus();
            Toast.makeText(this, "Please add at least one hashtag", Toast.LENGTH_SHORT).show();
            return;
        }
        proceedUpload(caption, hashtags);
    }

    // ── Actual upload — validation pass hone ke baad ─────────────────────────────
    private void proceedUpload(String caption, String hashtags) {
        String videoFilePath = null;
        if (renderedVideoPath != null && new File(renderedVideoPath).exists()) {
            videoFilePath = renderedVideoPath;
        } else if (!mediaList.isEmpty()) {
            videoFilePath = mediaList.get(0).getPath();
        }

        if (videoFilePath == null || !new File(videoFilePath).exists()) {
            Toast.makeText(this, "Video file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        showSharingDialog(videoFilePath, caption, hashtags);
    }

    // ── Modern Progress Dialog ────────────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    private void showSharingDialog(String videoFilePath, String caption, String hashtags) {
        // Custom dialog view
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_sharing_progress, null);

        android.app.Dialog dialog = new android.app.Dialog(this, com.google.android.material.R.style.ThemeOverlay_Material3_Dialog);
        dialog.setContentView(dialogView);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.85f), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ProgressBar dialogProgress = dialogView.findViewById(R.id.dialogProgress);
        TextView tvDialogStatus = dialogView.findViewById(R.id.tvDialogStatus);
        TextView tvDialogSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);

        tvDialogStatus.setText("Sharing your reel...");
        tvDialogSubtitle.setText("Please wait while we upload your content");
        dialog.show();

        // Upload karo
        File videoFile = new File(videoFilePath);
        RequestBody vBody = RequestBody.create(videoFile, MediaType.parse("video/mp4"));
        MultipartBody.Part videoPart = MultipartBody.Part.createFormData("Video", videoFile.getName(), vBody);

        MultipartBody.Part thumbPart = null;
        if (thumbPath != null && new File(thumbPath).exists()) {
            File tf = new File(thumbPath);
            RequestBody tb = RequestBody.create(tf, MediaType.parse("image/jpeg"));
            thumbPart = MultipartBody.Part.createFormData("Thumbnail", tf.getName(), tb);
        }

        btnPost.setEnabled(false);
        final MultipartBody.Part finalThumbPart = thumbPart;
        final String finalVideoPath = videoFilePath;
        UtilMethods.INSTANCE.saveReel(loader, caption, durationSeconds, hashtags,finalPageId == null || "null".equalsIgnoreCase(finalPageId)
                ? ""
                : finalPageId,videoPart, finalThumbPart, new UtilMethods.ApiCallBackMulti() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(Object response) {
                runOnUiThread(() -> {
                    // ★ Success state
                    dialogProgress.setVisibility(android.view.View.GONE);
                    tvDialogStatus.setText("Reel shared successfully!");
                    tvDialogSubtitle.setText("Your reel is now live");

                    // Success icon show karo
                    ImageView ivSuccess = dialogView.findViewById(R.id.ivDialogIcon);
                    if (ivSuccess != null) {
                        ivSuccess.setVisibility(android.view.View.VISIBLE);
                        ivSuccess.setImageResource(R.drawable.ic_check_circle_new);
                    }

                    // 1.2 sec baad navigate karo
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        dialog.dismiss();
                        cleanupTempFiles(finalVideoPath);
                        Intent intent = new Intent(ReelPostActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }, 1200);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    dialog.dismiss();
                    btnPost.setEnabled(true);

                    // ★ Modern error dialog
                    showErrorDialog(error);
                });
            }
        });
    }

    // ── Error Dialog ─────────────────────────────────────────────────────────────
    private void showErrorDialog(String error) {
        android.view.View v = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_error, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this).setView(v).setCancelable(true).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ((TextView) v.findViewById(R.id.tvErrorMessage)).setText(error);
        v.findViewById(R.id.btnRetry).setOnClickListener(btn -> {
            dialog.dismiss();
            postReel(); // retry
        });
        v.findViewById(R.id.btnCancel).setOnClickListener(btn -> dialog.dismiss());

        dialog.show();
    }

    private void cleanupTempFiles(String renderedPath) {
        try {
            if (renderedPath != null) new File(renderedPath).delete();
            if (thumbPath != null) new File(thumbPath).delete();

            File[] files = getCacheDir().listFiles();
            if (files == null) return;
            for (File f : files) {
                String n = f.getName();
                if (n.startsWith("merged_") || n.startsWith("burned_") || n.startsWith("img_video_") || n.startsWith("final_reel_") || n.startsWith("music_") || n.startsWith("overlay_") || n.startsWith("thumb_final_") || n.startsWith("merged_img_")) {
                    f.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}