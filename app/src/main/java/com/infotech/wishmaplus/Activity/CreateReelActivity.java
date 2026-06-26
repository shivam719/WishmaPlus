package com.infotech.wishmaplus.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.infotech.wishmaplus.Adapter.CreateReelAdapter;
import com.infotech.wishmaplus.Adapter.FolderDropdownAdapter;
import com.infotech.wishmaplus.Api.Response.FolderModel;
import com.infotech.wishmaplus.Api.Response.MediaModel;
import com.infotech.wishmaplus.reels.bottomsheet.DraftsBottomSheet;
import com.infotech.wishmaplus.reels.bottomsheet.EffectsBottomSheet;
import com.infotech.wishmaplus.reels.ui.GreenScreenActivity;
import com.infotech.wishmaplus.reels.bottomsheet.MultiSelectBottomSheet;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.ReelEditorActivity;
import com.infotech.wishmaplus.reels.bottomsheet.TemplatesBottomSheet;
import com.infotech.wishmaplus.reels.ui.CameraRecorderActivity;
import com.infotech.wishmaplus.reels.ui.componets.ServerMusicPickerBottomSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CreateReelActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView videoRecycler;
    private LinearLayout folderDropdownTrigger, cameraView;
    private LinearLayout multipleSelectBtn;
    private TextView folderName;
    private ImageView dropdownArrow, back;
    private View dropdownOverlay;
    private RecyclerView folderRecycler;
    private AppBarLayout appBarLayout;

    // ── Data ───────────────────────────────────────────────────────────────────
    private final List<MediaModel> mediaList = new ArrayList<>();
    private final List<FolderModel> folderList = new ArrayList<>();
    private CreateReelAdapter adapter;
    private FolderDropdownAdapter folderDropdownAdapter;

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean isDropdownOpen = false;
    private String currentFolderPath = null;
    private String currentFolderName = "Gallery";
    private int selectedFolderPosition = -1;

    // ── Selected effect to pass to editor ─────────────────────────────────────
    private EffectsBottomSheet.EffectModel pendingEffect = null;

    // =========================================================================
    // LAUNCHERS
    // =========================================================================

    /**
     * Green Screen result launcher
     */

    private String finalPageId=null;
    private ActivityResultLauncher<Intent> greenScreenLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_reel);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (getIntent() != null) {
            finalPageId=getIntent().getStringExtra("pageId");
        }
         greenScreenLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(), result -> {
                            if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                            Intent data = result.getData();

                            String fgPath = data.getStringExtra(GreenScreenActivity.EXTRA_FG_PATH);
                            String bgPath = data.getStringExtra(GreenScreenActivity.EXTRA_BG_PATH);
                            float tolerance = data.getFloatExtra(GreenScreenActivity.EXTRA_TOLERANCE, 0.4f);
                            float spill = data.getFloatExtra(GreenScreenActivity.EXTRA_SPILL, 0.3f);
                            int keyColor = data.getIntExtra(GreenScreenActivity.EXTRA_KEY_COLOR, android.graphics.Color.GREEN);
                            String compositePath = data.getStringExtra(GreenScreenActivity.EXTRA_COMPOSITE_PATH);

                            if (compositePath == null || !new File(compositePath).exists()) {
                                Toast.makeText(this, "Green screen composite not ready — try again", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (fgPath == null || bgPath == null) {
                                Toast.makeText(this, "Missing source paths", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Pass the COMPOSITE image as the media clip for the editor
                            ArrayList<MediaModel> list = new ArrayList<>();
                            list.add(new MediaModel(compositePath, false, 0));

                            Intent editorIntent = new Intent(this, ReelEditorActivity.class);
                            editorIntent.putExtra("media_list", list);
                            editorIntent.putExtra("gs_fg_path", fgPath);
                            editorIntent.putExtra("gs_bg_path", bgPath);
                            editorIntent.putExtra("gs_tolerance", tolerance);
                            editorIntent.putExtra("gs_spill", spill);
                            editorIntent.putExtra("gs_key_color", keyColor);
                            editorIntent.putExtra("pageId", finalPageId);
                            editorIntent.putExtra("gs_composite_path", compositePath);
                            startActivity(editorIntent);
                        });

        /**
         * Permission launcher
         */
         permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean granted = false;
                    for (Boolean val : result.values()) {
                        if (val) {
                            granted = true;
                            break;
                        }
                    }
                    if (granted) {
                        loadAllMedia();
                        loadFolders();
                    }
                });
        initViews();
        setupRecycler();
        setupDropdown();
        setupScrollBehavior();
        requestPermissionsIfNeeded();
    }

    // =========================================================================
    // INIT VIEWS
    // =========================================================================
    private void initViews() {
        videoRecycler = findViewById(R.id.videoRecycler);
        folderDropdownTrigger = findViewById(R.id.folderDropdownTrigger);
        multipleSelectBtn = findViewById(R.id.multipleSelectBtn);
        cameraView = findViewById(R.id.cameraView);
        folderName = findViewById(R.id.folderName);
        dropdownArrow = findViewById(R.id.dropdownArrow);
        back = findViewById(R.id.back);
        dropdownOverlay = findViewById(R.id.dropdownOverlay);
        folderRecycler = findViewById(R.id.folderRecycler);
        appBarLayout = findViewById(R.id.appBarLayout);


        // ── Music ─────────────────────────────────────────────────────────────
/*        findViewById(R.id.musicView).setOnClickListener(v -> {
            ServerMusicPickerBottomSheet sheet = ServerMusicPickerBottomSheet.newInstance(
                    (path, start, end, title) ->
                            Toast.makeText(this, "Music added: " + title, Toast.LENGTH_SHORT).show());
            sheet.show(getSupportFragmentManager(), "music_picker");
        });*/
        findViewById(R.id.musicView).setOnClickListener(v -> {
            ServerMusicPickerBottomSheet sheet = ServerMusicPickerBottomSheet.newInstance(
                    (path, start, end, title) -> {
                        Intent intent = new Intent(this, ReelEditorActivity.class);
                        intent.putExtra("media_list", new ArrayList<>(mediaList));
                        intent.putExtra("pageId", finalPageId);
                        intent.putExtra("music_path", path);
                        intent.putExtra("music_start", start);
                        intent.putExtra("music_end", end);
                        intent.putExtra("music_title", title);
                        startActivity(intent);
                    });
            sheet.show(getSupportFragmentManager(), "music_picker");
        });

        // ── Templates — FIX: open editor properly with template settings ──────
        //   Now selects media clips automatically based on template duration,
        //   passes template_name + template_duration to ReelEditorActivity so
        //   the editor can apply transitions, duration, and style.
        findViewById(R.id.templatesView).setOnClickListener(v -> {
            TemplatesBottomSheet sheet = TemplatesBottomSheet.newInstance();
            sheet.setListener(template -> {
                if (mediaList.isEmpty()) {
                    Toast.makeText(this, "Add media first to use a template", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Auto-select clips: ~1 clip per 5 seconds of template duration
                ArrayList<MediaModel> list = new ArrayList<>();
                int count = Math.max(1, Math.min(mediaList.size(), template.durationSec / 5));
                for (int i = 0; i < count; i++) list.add(mediaList.get(i));

                Intent intent = new Intent(this, ReelEditorActivity.class);
                intent.putExtra("media_list", list);
                intent.putExtra("template_name", template.name);
                intent.putExtra("template_duration", template.durationSec);
                intent.putExtra("template_style", template.style);
                intent.putExtra("template_emoji", template.emoji);
                startActivity(intent);
            });
            sheet.show(getSupportFragmentManager(), "templates");
        });

        // ── Drafts ────────────────────────────────────────────────────────────
        findViewById(R.id.draftsView).setOnClickListener(v -> {
            DraftsBottomSheet sheet = DraftsBottomSheet.newInstance();
            sheet.setListener(new DraftsBottomSheet.DraftSelectedListener() {
                @Override
                public void onDraftSelected(DraftsBottomSheet.DraftModel draft) {
                    ArrayList<MediaModel> list = new ArrayList<>();
                    for (String path : draft.mediaPaths) {
                        boolean isVid = path.endsWith(".mp4") || path.endsWith(".mov")
                                || path.endsWith(".mkv") || path.endsWith(".avi");
                        list.add(new MediaModel(path, isVid, 0));
                    }
                    if (!list.isEmpty()) {
                        Intent intent = new Intent(CreateReelActivity.this, ReelEditorActivity.class);
                        intent.putExtra("media_list", list);
                        intent.putExtra("draft_id", draft.id);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CreateReelActivity.this, "Draft media not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onDraftDeleted(DraftsBottomSheet.DraftModel draft) {
                }
            });
            sheet.show(getSupportFragmentManager(), "drafts");
        });

        // ── Green Screen ──────────────────────────────────────────────────────
        findViewById(R.id.greenScreenView).setOnClickListener(v ->
                greenScreenLauncher.launch(new Intent(this, GreenScreenActivity.class)));

        // ── Effects — FIX: store selected effect, open editor with it ─────────
        //   Old code just showed a Toast. Now stores the effect and opens
        //   ReelEditorActivity with the effect passed as an extra so the
        //   render engine applies it on export.
        findViewById(R.id.effectsView).setOnClickListener(v -> {
            EffectsBottomSheet sheet = EffectsBottomSheet.newInstance();

            // Pass a preview thumbnail if available
            if (!mediaList.isEmpty()) {
                MediaModel first = mediaList.get(0);
                if (!first.isVideo()) {
                    android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                    opts.inSampleSize = 4;
                    android.graphics.Bitmap thumb =
                            android.graphics.BitmapFactory.decodeFile(first.getPath(), opts);
                    if (thumb != null) sheet.setPreviewBitmap(thumb);
                }
            }

            sheet.setListener(new EffectsBottomSheet.EffectAppliedListener() {
                @Override
                public void onEffectApplied(EffectsBottomSheet.EffectModel effect) {
                    pendingEffect = effect;
                    Toast.makeText(CreateReelActivity.this,
                            effect.emoji + " " + effect.name + " selected — pick a clip",
                            Toast.LENGTH_SHORT).show();
                    // Highlight the effects icon to show an effect is active
                    View effectsView = findViewById(R.id.effectsView);
                    if (effectsView != null) effectsView.setAlpha(1f);
                }

                @Override
                public void onEffectCleared() {
                    pendingEffect = null;
                    Toast.makeText(CreateReelActivity.this, "Effect cleared", Toast.LENGTH_SHORT).show();
                }
            });
            sheet.show(getSupportFragmentManager(), "effects");
        });

        // ── Camera ────────────────────────────────────────────────────────────
        cameraView.setOnClickListener(v ->
                startActivity(new Intent(this, CameraRecorderActivity.class)));

        // ── Back ──────────────────────────────────────────────────────────────
        back.setOnClickListener(v -> finish());

        // ── Dropdown ──────────────────────────────────────────────────────────
        dropdownOverlay.setOnClickListener(v -> closeDropdown());
        folderDropdownTrigger.setOnClickListener(v -> {
            if (isDropdownOpen) closeDropdown();
            else openDropdown();
        });

        // ── Multi-select ──────────────────────────────────────────────────────
        multipleSelectBtn.setOnClickListener(v -> openMultiSelectSheet());
    }

    // =========================================================================
    // RECYCLER — FIX: single clip tap passes pendingEffect to editor
    // =========================================================================
    private void setupRecycler() {
        videoRecycler.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new CreateReelAdapter(this, mediaList, media -> {
            ArrayList<MediaModel> singleList = new ArrayList<>();
            singleList.add(media);
            openEditorWithMedia(singleList, null, null, 0, null);
        });
        videoRecycler.setAdapter(adapter);
    }

    /**
     * Central method to open ReelEditorActivity.
     * Handles: normal clip, template, effect.
     */
    private void openEditorWithMedia(
            ArrayList<MediaModel> list,
            String templateName,
            String templateStyle,
            int templateDuration,
            String templateEmoji) {

        Intent intent = new Intent(this, ReelEditorActivity.class);
        intent.putExtra("media_list", list);
        intent.putExtra("pageId", finalPageId);
        // Template extras
        if (templateName != null) {
            intent.putExtra("template_name", templateName);
            intent.putExtra("template_duration", templateDuration);
            intent.putExtra("template_style", templateStyle);
            intent.putExtra("template_emoji", templateEmoji != null ? templateEmoji : "");
        }

        // Effect extra — FIX: effect is now passed to editor
        if (pendingEffect != null) {
            intent.putExtra("effect_name", pendingEffect.name);
            intent.putExtra("effect_emoji", pendingEffect.emoji);
            // If EffectModel is Serializable, pass it directly:
            // intent.putExtra("effect_model", pendingEffect);
        }

        startActivity(intent);
    }

    // =========================================================================
    // SCROLL BEHAVIOR
    // =========================================================================
    private void setupScrollBehavior() {
        appBarLayout.addOnOffsetChangedListener((appBar, verticalOffset) -> {
            float totalScroll = appBar.getTotalScrollRange();
            if (totalScroll == 0) return;
            // CoordinatorLayout handles collapse; nothing extra needed here
        });
    }

    // =========================================================================
    // FOLDER DROPDOWN
    // =========================================================================
    private void setupDropdown() {
        folderDropdownAdapter = new FolderDropdownAdapter(this, folderList, position -> {
            selectedFolderPosition = position;
            if (position == 0) {
                currentFolderPath = null;
                currentFolderName = "Gallery";
                loadAllMedia();
            } else {
                FolderModel selected = folderList.get(position);
                currentFolderPath = selected.getFolderPath();
                currentFolderName = selected.getFolderName();
                loadMediaByFolder(currentFolderPath);
            }
            folderName.setText(currentFolderName);
            folderDropdownAdapter.setSelectedPosition(position);
            closeDropdown();
        });
        folderRecycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        folderRecycler.setAdapter(folderDropdownAdapter);
        folderRecycler.setNestedScrollingEnabled(true);
    }

    private void openDropdown() {
        isDropdownOpen = true;
        dropdownOverlay.setVisibility(View.VISIBLE);
        dropdownOverlay.setAlpha(0f);
        dropdownOverlay.animate().alpha(1f).setDuration(200).start();
        dropdownArrow.animate().rotation(180f).setDuration(200).start();
    }

    private void closeDropdown() {
        isDropdownOpen = false;
        dropdownOverlay.animate().alpha(0f).setDuration(180)
                .withEndAction(() -> dropdownOverlay.setVisibility(View.GONE)).start();
        dropdownArrow.animate().rotation(0f).setDuration(200).start();
    }

    // =========================================================================
    // MULTI-SELECT
    // =========================================================================
    private void openMultiSelectSheet() {
        MultiSelectBottomSheet sheet = MultiSelectBottomSheet.newInstance(
                new ArrayList<>(mediaList),
                selectedItems -> {
                    if (!selectedItems.isEmpty()) {
                        openEditorWithMedia(new ArrayList<>(selectedItems),
                                null, null, 0, null);
                    }
                });
        sheet.show(getSupportFragmentManager(), "multi_select");
    }

    // =========================================================================
    // PERMISSIONS
    // =========================================================================
    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean videoOk = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            boolean imageOk = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            if (videoOk && imageOk) {
                loadAllMedia();
                loadFolders();
            } else permissionLauncher.launch(new String[]{
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_IMAGES});
        } else {
            boolean granted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                loadAllMedia();
                loadFolders();
            } else
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAllMedia();
            loadFolders();
        }
    }

    // =========================================================================
    // MEDIA LOADING
    // =========================================================================
    private void loadFolders() {
        folderList.clear();
        folderList.add(new FolderModel("Gallery", null, 0));

        Uri collection = MediaStore.Files.getContentUri("external");
        String[] projection = {
                MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE
        };
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Cursor cursor = getContentResolver().query(collection, projection,
                selection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

        if (cursor != null) {
            int fnIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME);
            int pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

            java.util.HashMap<String, Integer> folderCount = new java.util.HashMap<>();
            java.util.HashMap<String, String> folderPaths = new java.util.HashMap<>();
            java.util.HashMap<String, String> folderThumbs = new java.util.HashMap<>();

            while (cursor.moveToNext()) {
                String fName = cursor.getString(fnIdx);
                String path = cursor.getString(pathIdx);
                if (fName == null) continue;
                folderCount.put(fName, folderCount.getOrDefault(fName, 0) + 1);
                if (!folderPaths.containsKey(fName)) {
                    folderPaths.put(fName, new File(path).getParent());
                    folderThumbs.put(fName, path);
                }
            }
            cursor.close();
            for (String fName : folderPaths.keySet()) {
                folderList.add(new FolderModel(fName, folderPaths.get(fName),
                        folderCount.getOrDefault(fName, 0), folderThumbs.get(fName)));
            }
        }
        folderList.get(0).setCount(mediaList.size());
        folderDropdownAdapter.notifyDataSetChanged();
    }

    private void loadAllMedia() {
        mediaList.clear();
        Uri collection = MediaStore.Files.getContentUri("external");
        String[] proj = {MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Video.Media.DURATION};
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

        Cursor cursor = getContentResolver().query(collection, proj,
                selection, null, MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
        if (cursor != null) {
            int pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            int typeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int durIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            while (cursor.moveToNext()) {
                String path = cursor.getString(pathIdx);
                int type = cursor.getInt(typeIdx);
                boolean isVid = type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                long dur = isVid ? cursor.getLong(durIdx) : 0;
                mediaList.add(new MediaModel(path, isVid, dur));
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void loadMediaByFolder(String folderPath) {
        mediaList.clear();
        Uri collection = MediaStore.Files.getContentUri("external");
        String[] proj = {MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Video.Media.DURATION};
        String selection = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE + " OR "
                + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO + ") AND "
                + MediaStore.Files.FileColumns.DATA + " LIKE ?";

        Cursor cursor = getContentResolver().query(collection, proj,
                selection, new String[]{folderPath + "%"},
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC");
        if (cursor != null) {
            int pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            int typeIdx = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE);
            int durIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            while (cursor.moveToNext()) {
                String path = cursor.getString(pathIdx);
                int type = cursor.getInt(typeIdx);
                boolean isVid = type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;
                long dur = isVid ? cursor.getLong(durIdx) : 0;
                mediaList.add(new MediaModel(path, isVid, dur));
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}