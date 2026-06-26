package com.infotech.wishmaplus.reels.ui.componets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.infotech.wishmaplus.Api.Response.SongModel;
import com.infotech.wishmaplus.Api.Response.SongSearchResponse;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.UtilMethods;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n")
public class ServerMusicPickerBottomSheet extends BottomSheetDialogFragment {

    private static final int REQ_AUDIO_PERMISSION = 1001;
    private final Handler handler = new Handler(Looper.getMainLooper());
    // ── Views ─────────────────────────────────────────────────────────────────
    private EditText searchInput;
    private TabLayout musicTabLayout;
    private boolean isOnlineTab = true;
    private RecyclerView trackList;
    private LinearLayout trimPanel;
    private SeekBar seekTrimStart, seekTrimEnd;
    private TextView tvTrimStart, tvTrimEnd, tvSelectedTrack;
    private ImageButton btnPlayPreview;
    private Button btnApplyMusic;
    private ProgressBar searchProgress;
    private View emptyView;
    // ── State ─────────────────────────────────────────────────────────────────
    private OnMusicSelected callback;
    private ServerTrackAdapter adapter;
    private SongModel selectedSong;
    private long trimStartMs = 0, trimEndMs = 30000;
    private MediaPlayer previewPlayer;
    // Holds the local file URI string when an offline track is selected
    private String selectedOfflineUri = null;
    private Runnable seekUpdateRunnable;
    private Runnable pendingSearchRunnable;

    private boolean isPlayerPrepared = false;
    private AudioFocusRequest audioFocusRequest;

    // ── Factory ───────────────────────────────────────────────────────────────
    public static ServerMusicPickerBottomSheet newInstance(OnMusicSelected cb) {
        ServerMusicPickerBottomSheet sheet = new ServerMusicPickerBottomSheet();
        sheet.callback = cb;
        return sheet;
    }

    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_server_music_picker, container, false);
        bindViews(v);
        setupRecyclerView();
        setupSearch();
        setupTrimControls();
        setupApplyButton();
        loadTracks(""); // load all online songs on open
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        stopSeekUpdate();
        releasePreviewPlayer();
    }

    // =========================================================================
    // BIND
    // =========================================================================
    private void bindViews(View v) {
        musicTabLayout = v.findViewById(R.id.musicTabLayout);
        searchInput = v.findViewById(R.id.musicSearchInput);
        trackList = v.findViewById(R.id.musicTrackList);
        trimPanel = v.findViewById(R.id.trimPanel);
        seekTrimStart = v.findViewById(R.id.seekTrimStart);
        seekTrimEnd = v.findViewById(R.id.seekTrimEnd);
        tvTrimStart = v.findViewById(R.id.tvTrimStart);
        tvTrimEnd = v.findViewById(R.id.tvTrimEnd);
        tvSelectedTrack = v.findViewById(R.id.tvSelectedTrack);
        btnPlayPreview = v.findViewById(R.id.btnPlayPreview);
        btnApplyMusic = v.findViewById(R.id.btnApplyMusic);
        searchProgress = v.findViewById(R.id.searchProgress);
        emptyView = v.findViewById(R.id.emptyView);

        musicTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isOnlineTab = tab.getPosition() == 0;
                resetSelection();
                if (isOnlineTab) {
                    searchInput.setVisibility(View.VISIBLE);
                    loadTracks(searchInput.getText().toString().trim());
                } else {
                    searchInput.setVisibility(View.GONE);
                    loadOfflineTracks();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    /**
     * Clears the current selection without nulling any view reference.
     */
    private void resetSelection() {
        selectedSong = null;
        selectedOfflineUri = null;
        if (tvSelectedTrack != null) tvSelectedTrack.setText("No track selected");
        if (trimPanel != null) trimPanel.setVisibility(View.GONE);
        if (btnApplyMusic != null) btnApplyMusic.setVisibility(View.GONE);
        releasePreviewPlayer();
    }

    // =========================================================================
    // RECYCLERVIEW
    // =========================================================================
    private void setupRecyclerView() {
        adapter = new ServerTrackAdapter(this::onTrackSelected);
        trackList.setLayoutManager(new LinearLayoutManager(getContext()));
        trackList.setAdapter(adapter);
    }

    // =========================================================================
    // SEARCH — debounced 400 ms (online only)
    // =========================================================================
    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (!isOnlineTab) return;
                if (pendingSearchRunnable != null) handler.removeCallbacks(pendingSearchRunnable);
                pendingSearchRunnable = () -> loadTracks(s.toString().trim());
                handler.postDelayed(pendingSearchRunnable, 400);
            }
        });
    }

    // =========================================================================
    // LOAD ONLINE TRACKS
    // =========================================================================
    private void loadTracks(String query) {
        if (!isAdded()) return;
        showProgress(true);
        showEmptyView(false);

        UtilMethods.INSTANCE.searchSongs(query, 20, 0, new UtilMethods.ApiCallBackMulti() {

            @Override
            public void onSuccess(Object object) {
                if (!isAdded()) return;
                SongSearchResponse response = (SongSearchResponse) object;
                showProgress(false);

                List<SongModel> songs = (response.getResult() != null && response.getResult().getSongs() != null) ? response.getResult().getSongs() : new ArrayList<>();

                if (songs.isEmpty()) {
                    showEmptyView(true);
                    adapter.submitList(new ArrayList<>());
                } else {
                    showEmptyView(false);
                    adapter.submitList(songs);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                showProgress(false);
                showEmptyView(true);
                adapter.submitList(new ArrayList<>());
                Toast.makeText(getContext(), "Could not load songs. Check your connection.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================================
    // LOAD OFFLINE TRACKS  (MediaStore)
    // =========================================================================
    private void loadOfflineTracks() {
        if (!isAdded()) return;

        // ── 1. Runtime permission check ──────────────────────────────────────
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, REQ_AUDIO_PERMISSION);
            return;
        }

        showProgress(true);
        showEmptyView(false);

        // ── 2. Query MediaStore on a background thread ────────────────────────
        new Thread(() -> {
            List<SongModel> offlineSongs = new ArrayList<>();

            Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

            String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID};

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

            try (Cursor cursor = requireContext().getContentResolver().query(collection, projection, selection, null, sortOrder)) {

                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                    int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                    int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                    int durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                    int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String title = cursor.getString(titleCol);
                        String artist = cursor.getString(artistCol);
                        String album = cursor.getString(albumCol);
                        long durMs = cursor.getLong(durCol);
                        long albumId = cursor.getLong(albumIdCol);

                        // Content URI for playback
                        Uri audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                        // Album art URI (may not exist for every track — Glide handles the fallback)
                        Uri artUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                        SongModel song = new SongModel();
                        song.setSongId((int) id);
                        song.setTitle(title != null ? title : "Unknown");
                        song.setArtist(artist != null ? artist : "Unknown Artist");
                        song.setGenre(album);                          // reuse genre field for album
                        song.setDurationSec((int) (durMs / 1000));
                        song.setAudioUrl(audioUri.toString());         // content:// URI as string
                        song.setCoverImageUrl(artUri.toString());      // album art
                        offlineSongs.add(song);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // ── 3. Post results back to UI thread ────────────────────────────
            List<SongModel> result = offlineSongs;
            handler.post(() -> {
                if (!isAdded()) return;
                showProgress(false);
                if (result.isEmpty()) {
                    showEmptyView(true);
                    adapter.submitList(new ArrayList<>());
                } else {
                    showEmptyView(false);
                    adapter.submitList(result);
                }
            });
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadOfflineTracks();
            } else {
                Toast.makeText(getContext(), "Storage permission required to browse device music.", Toast.LENGTH_SHORT).show();
                showEmptyView(true);
            }
        }
    }

    // =========================================================================
    // TRACK SELECTED  (works for both online SongModel and offline SongModel)
    // =========================================================================
    private void onTrackSelected(SongModel song) {
        selectedSong = song;
        selectedOfflineUri = null;
        tvSelectedTrack.setText(song.getTitle() + " — " + song.getArtist());
        trimPanel.setVisibility(View.VISIBLE);
        btnApplyMusic.setVisibility(View.VISIBLE);
        btnApplyMusic.setEnabled(false);
        resetTrimUI();
        releasePreviewPlayer();
        btnPlayPreview.setImageResource(R.drawable.ic_play_circle_outline);
        btnPlayPreview.setEnabled(false);
        searchProgress.setVisibility(View.VISIBLE);

        if (!isOnlineTab) {
            copyOfflineTrackToCache(song.getAudioUrl(), () -> {
                btnApplyMusic.setEnabled(true);
                initPreviewPlayer(selectedOfflineUri);
            });
        } else {
            initPreviewPlayer(song.getAudioUrl());
        }
    }

    private void copyOfflineTrackToCache(String contentUriStr, Runnable onDone) {
        showProgress(true);
        new Thread(() -> {
            try {
                Uri uri = Uri.parse(contentUriStr);
                File out = new File(requireContext().getCacheDir(), "offline_music_" + System.currentTimeMillis() + ".mp3");

                try (InputStream is = requireContext().getContentResolver().openInputStream(uri); FileOutputStream fos = new FileOutputStream(out)) {

                    byte[] buf = new byte[8192];
                    int n;
                    if (is != null) {
                        while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                    }
                    fos.flush();
                }

                selectedOfflineUri = out.getAbsolutePath(); //  real path

                handler.post(() -> {
                    if (!isAdded()) return;
                    showProgress(false);
                    onDone.run();
                });

            } catch (Exception e) {
                handler.post(() -> {
                    if (!isAdded()) return;
                    showProgress(false);
                    Toast.makeText(getContext(), "Track load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // =========================================================================
    // TRIM CONTROLS
    // =========================================================================
    private void setupTrimControls() {
        seekTrimStart.setMax(1000);
        seekTrimEnd.setMax(1000);
        seekTrimEnd.setProgress(1000);

        seekTrimStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (!fromUser) return;
                long total = getTotalDuration();
                trimStartMs = (long) (p / 1000f * total);
                if (trimStartMs >= trimEndMs) {
                    trimStartMs = Math.max(0, trimEndMs - 1000);
                    sb.setProgress((int) (trimStartMs / (float) total * 1000));
                }
                tvTrimStart.setText(formatMs(trimStartMs));
                if (previewPlayer != null) {
                    try {
                        previewPlayer.seekTo((int) trimStartMs);
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                stopSeekUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });

        seekTrimEnd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (!fromUser) return;
                long total = getTotalDuration();
                trimEndMs = (long) (p / 1000f * total);
                if (trimEndMs <= trimStartMs) {
                    trimEndMs = Math.min(total, trimStartMs + 1000);
                    sb.setProgress((int) (trimEndMs / (float) total * 1000));
                }
                tvTrimEnd.setText(formatMs(trimEndMs));
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });

        btnPlayPreview.setOnClickListener(v -> togglePreview());
    }

    private void setupApplyButton() {
        btnApplyMusic.setOnClickListener(v -> {
            if (selectedSong == null || callback == null) return;
            String path = isOnlineTab ? selectedSong.getAudioUrl() : selectedOfflineUri;
            callback.onSelected(path, trimStartMs, trimEndMs, selectedSong.getTitle());
            dismiss();
        });
    }

    // =========================================================================
    // PREVIEW PLAYER  (handles both http:// and content:// URIs)
    // =========================================================================
    private void initPreviewPlayer(String url) {
        releasePreviewPlayer();
        try {
            previewPlayer = new MediaPlayer();
            previewPlayer.setAudioAttributes(new
                    AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build());

            // content:// URIs need setDataSource(Context, Uri)
            if (url.startsWith("content://")) {
                previewPlayer.setDataSource(requireContext(), Uri.parse(url));
            } else {
                previewPlayer.setDataSource(url);
            }
            isPlayerPrepared = false;
            previewPlayer.setOnPreparedListener(mp -> {
                if (!isAdded()) return;
                isPlayerPrepared = true;
                long totalMs = mp.getDuration();
                trimEndMs = Math.min(30000, totalMs);
                updateTrimLabels(totalMs);
                btnApplyMusic.setEnabled(true);
                searchProgress.setVisibility(View.GONE);
                btnPlayPreview.setEnabled(true);
                // Auto play
                boolean focusGranted = requestAudioFocus();
                if (focusGranted) {
                    previewPlayer.seekTo((int) trimStartMs);
                    previewPlayer.start();
                    startSeekUpdate();
                    btnPlayPreview.setImageResource(R.drawable.outline_pause_circle_24);
                    scheduleAutoStop();
                }
            });

            previewPlayer.setOnCompletionListener(mp -> {
                btnPlayPreview.setImageResource(R.drawable.ic_play_circle_outline);
                stopSeekUpdate();
            });

            previewPlayer.setOnErrorListener((mp, what, extra) -> {
                btnPlayPreview.setImageResource(R.drawable.ic_play_circle_outline);
                stopSeekUpdate();
                releasePreviewPlayer();
                if (isAdded())
                    Toast.makeText(getContext(), "Playback error", Toast.LENGTH_SHORT).show();
                return true;
            });

            previewPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
            releasePreviewPlayer();
        }
    }

    private void togglePreview() {
        if (previewPlayer == null || !isPlayerPrepared) return;
        try {
            if (previewPlayer.isPlaying()) {
                previewPlayer.pause();
                stopSeekUpdate();
                btnPlayPreview.setImageResource(R.drawable.ic_play_circle_outline);
                abandonAudioFocus();
            } else {
                boolean focusGranted = requestAudioFocus();
                if (!focusGranted) {
                    Toast.makeText(getContext(), "Audio busy, try again", Toast.LENGTH_SHORT).show();
                    return;
                }
                previewPlayer.seekTo((int) trimStartMs);
                previewPlayer.start();
                startSeekUpdate();
                btnPlayPreview.setImageResource(R.drawable.outline_pause_circle_24);
                scheduleAutoStop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean requestAudioFocus() {
        AudioManager am = (AudioManager) requireContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setOnAudioFocusChangeListener(focusChange -> {
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            handler.post(() -> {
                                if (previewPlayer != null && isPlayerPrepared
                                        && previewPlayer.isPlaying()) {
                                    previewPlayer.pause();
                                    stopSeekUpdate();
                                    if (btnPlayPreview != null)
                                        btnPlayPreview.setImageResource(
                                                R.drawable.ic_play_circle_outline);
                                }
                            });
                        }
                    })
                    .build();
            int result = am.requestAudioFocus(audioFocusRequest);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            // API < 26
            int result = am.requestAudioFocus(null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    // ── Audio Focus ───────────────────────────────────────────────────────────

    private void abandonAudioFocus() {
        AudioManager am = (AudioManager) requireContext()
                .getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            am.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            am.abandonAudioFocus(null);
        }
    }

    private void scheduleAutoStop() {
        handler.removeCallbacks(autoStopRunnable);
        long delay = trimEndMs - trimStartMs;
        if (delay > 0) handler.postDelayed(autoStopRunnable, delay);
    }

    private void startSeekUpdate() {
        stopSeekUpdate();
        seekUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (previewPlayer != null) {
                    try {
                        int curr = previewPlayer.getCurrentPosition();
                        int total = previewPlayer.getDuration();
                        if (total > 0) {
                            seekTrimStart.setProgress((int) (curr / (float) total * 1000));
                            tvTrimStart.setText(formatMs(curr));
                        }
                    } catch (Exception ignored) {
                    }
                }
                handler.postDelayed(this, 200);
            }
        };
        handler.post(seekUpdateRunnable);
    }

    private void stopSeekUpdate() {
        if (seekUpdateRunnable != null) {
            handler.removeCallbacks(seekUpdateRunnable);
            seekUpdateRunnable = null;
        }
        handler.removeCallbacks(autoStopRunnable);
    }    private final Runnable autoStopRunnable = () -> {
        if (previewPlayer != null && previewPlayer.isPlaying()) {
            previewPlayer.pause();
            stopSeekUpdate();
            btnPlayPreview.setImageResource(R.drawable.ic_play_circle_outline);
        }
    };

    private void releasePreviewPlayer() {
        stopSeekUpdate();
        abandonAudioFocus();
        if (previewPlayer != null) {
            try {
                previewPlayer.setOnPreparedListener(null);
                previewPlayer.setOnErrorListener(null);
                previewPlayer.setOnCompletionListener(null);
                if (previewPlayer.isPlaying()) previewPlayer.stop();
                previewPlayer.release();
            } catch (Exception ignored) {
            }
            previewPlayer = null;
        }
        isPlayerPrepared = false;
        if (btnPlayPreview != null)
            btnPlayPreview.setImageResource(R.drawable.ic_play_circle_outline);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private long getTotalDuration() {
        if (previewPlayer != null) {
            try {
                return previewPlayer.getDuration();
            } catch (Exception ignored) {
            }
        }
        return 120_000;
    }

    private void resetTrimUI() {
        trimStartMs = 0;
        trimEndMs = 30_000;
        seekTrimStart.setProgress(0);
        seekTrimEnd.setProgress(1000);
        tvTrimStart.setText("0:00");
        tvTrimEnd.setText("0:30");
    }

    private void updateTrimLabels(long totalMs) {
        trimEndMs = Math.min(30_000, totalMs);
        tvTrimStart.setText("0:00");
        tvTrimEnd.setText(formatMs(trimEndMs));
        seekTrimEnd.setProgress((int) (trimEndMs / (float) totalMs * 1000));
    }

    private void showProgress(boolean show) {
        if (searchProgress != null) searchProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyView(boolean show) {
        if (emptyView != null) emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (trackList != null) trackList.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("DefaultLocale")
    private String formatMs(long ms) {
        long sec = ms / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    // ── Callback ──────────────────────────────────────────────────────────────
    public interface OnMusicSelected {
        void onSelected(String audioUrl, long startMs, long endMs, String title);
    }

    // =========================================================================
    // ADAPTER — DiffUtil for smooth updates
    // =========================================================================
    static class ServerTrackAdapter extends RecyclerView.Adapter<ServerTrackAdapter.VH> {

        private final OnTrackClick listener;
        private List<SongModel> data = new ArrayList<>();
        private int selectedPos = -1;
        ServerTrackAdapter(OnTrackClick listener) {
            this.listener = listener;
        }

        void submitList(List<SongModel> newList) {
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return data.size();
                }

                @Override
                public int getNewListSize() {
                    return newList.size();
                }

                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    return data.get(oldPos).getSongId() == newList.get(newPos).getSongId();
                }

                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    SongModel o = data.get(oldPos), n = newList.get(newPos);
                    return o.getSongId() == n.getSongId() && safeEq(o.getTitle(), n.getTitle()) && safeEq(o.getArtist(), n.getArtist());
                }

                private boolean safeEq(String a, String b) {
                    if (a == null && b == null) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            });
            data = new ArrayList<>(newList);
            selectedPos = -1;
            diff.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_server_music_track, parent, false);
            return new VH(v);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SongModel song = data.get(position);

            h.title.setText(song.getTitle());
            h.artist.setText(song.getArtist() != null ? song.getArtist() + (song.getGenre() != null ? " • " + song.getGenre() : "") : "");

            int dur = song.getDurationSec();
            h.duration.setText(String.format("%d:%02d", dur / 60, dur % 60));

            // Cover image
            if (song.getCoverImageUrl() != null && !song.getCoverImageUrl().isEmpty()) {
                h.cover.setColorFilter(null);
                Glide.with(h.itemView.getContext()).load(song.getCoverImageUrl()).transform(new RoundedCorners(16)).placeholder(R.drawable.ic_music).error(R.drawable.ic_music).into(h.cover);
            } else {
                h.cover.setImageResource(R.drawable.ic_music);
                h.cover.setColorFilter(ContextCompat.getColor(h.itemView.getContext(), R.color.black));
            }

            // Selected highlight
            boolean isSelected = (position == selectedPos);
            h.itemView.setBackgroundColor(isSelected ? 0x221877F2 : 0x00000000);
            h.playIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            h.itemView.setOnClickListener(v -> {
                int prev = selectedPos;
                selectedPos = h.getBindingAdapterPosition();
                if (prev >= 0) notifyItemChanged(prev);
                notifyItemChanged(selectedPos);
                listener.onClick(song);
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        interface OnTrackClick {
            void onClick(SongModel song);
        }

        static class VH extends RecyclerView.ViewHolder {
            ImageView cover, playIcon;
            TextView title, artist, duration;

            VH(@NonNull View v) {
                super(v);
                cover = v.findViewById(R.id.serverTrackCover);
                playIcon = v.findViewById(R.id.serverTrackPlayIcon);
                title = v.findViewById(R.id.serverTrackTitle);
                artist = v.findViewById(R.id.serverTrackArtist);
                duration = v.findViewById(R.id.serverTrackDuration);
            }
        }
    }


}