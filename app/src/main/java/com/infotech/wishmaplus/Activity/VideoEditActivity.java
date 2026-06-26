package com.infotech.wishmaplus.Activity;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.Utils.ApplicationConstant;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.FileUtils;
import com.infotech.wishmaplus.Utils.Utility;
import com.infotech.wishmaplus.Utils.VideoEdit.Trimmer.TrimView;
import com.infotech.wishmaplus.Utils.VideoEdit.VideoEditConstant;
import com.infotech.wishmaplus.Utils.VideoEdit.VideoEditor;
import com.infotech.wishmaplus.Utils.VideoEdit.interfaces.OptiFFMpegCallback;
import com.infotech.wishmaplus.reels.ui.componets.ServerMusicPickerBottomSheet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VideoEditActivity extends AppCompatActivity implements OptiFFMpegCallback {

    private final int REQUEST_PERMISSIONS_MUSIC = 8642;
    private final int REQUEST_PERMISSIONS_VIDEO = 9876;
    private final int REQUEST_PERMISSIONS_CAMERA = 5432;
    TrimView trimView;
    private String videoPath;
    private VideoView videoView;
    private ImageView playBtn, musicAlbum;
    private TextView musicName;
    private View musicDetails, videoGallery, camera, music, send, closeBtn;
    /*private RangeSlider seekBar;*/
    private File captureFile;
    private int positionVideo;
    private Snackbar mSnackBar;
    private MediaPlayer musicMediaPlayer;
    private VideoEditor videoEditor;
    private long videoDuration;
    private File masterAudioFile;
    private CustomLoader loader;
    private File convertedAudioFile;
    private boolean isDeleteVideoAllow = false;
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        if (result.getResultCode() == Activity.RESULT_OK) {

            if (captureFile != null) {
                videoPath = captureFile.getPath();
            } else if (result.getData() != null && result.getData().getData() != null) {
                Uri videoUri = result.getData().getData();
                videoPath = FileUtils.getPath(VideoEditActivity.this, videoUri);
            }
            if (videoPath != null && !videoPath.isEmpty()) {
                playBtn.setVisibility(View.GONE);
                isDeleteVideoAllow = false;
                videoView.setVideoPath(videoPath);
                positionVideo = 0;
                videoView.start();
                /*videoView.seekTo(positionVideo);
                videoView.start();*/


            }
        }

    });
    private boolean isMusicFromSystemOnly;
    private int postType = 1;
    ActivityResultLauncher<Intent> audioResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        if (result.getResultCode() == Activity.RESULT_OK) {
            if (result.getData() != null && result.getData().getData() != null) {
                try {

                    Uri selectedAudioUri = result.getData().getData();
                    String path = FileUtils.getPath(this, selectedAudioUri);
                    if (path != null && !path.isEmpty()) {
                        setResultAudio(path);
                    } else {
                        String[] filePathColumn = {MediaStore.MediaColumns.DATA};
                        Cursor cursor = getContentResolver().query(selectedAudioUri, filePathColumn, null, null, null);
                        if (cursor != null) {
                            cursor.moveToFirst();
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String filePath = cursor.getString(columnIndex);
                            cursor.close();

                            setResultAudio(filePath);

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }

            /*if (captureFile!=null){
                videoPath =  captureFile.getPath();
            } else if(result.getData() != null && result.getData().getData()!=null){
                Uri videoUri = result.getData().getData();
                videoPath = FileUtils.getPath(VideoEditActivity.this, videoUri);
            }
            if (videoPath != null && !videoPath.isEmpty()) {
                playBtn.setVisibility(View.GONE);

                videoView.setVideoPath(videoPath);
                positionVideo=0;
                videoView.seekTo(positionVideo);
                videoView.start();



            }*/
        }

    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video_edit);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.videoED), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        loader = new CustomLoader(this, android.R.style.Theme_Translucent_NoTitleBar);
        isMusicFromSystemOnly = getIntent().getBooleanExtra("MusicFromSystemOnly", false);
        videoPath = getIntent().getStringExtra("VideoPath");
        postType = getIntent().getIntExtra("postType", 1);
        videoEditor = new VideoEditor(this);
        musicDetails = findViewById(R.id.musicDetails);
        closeBtn = findViewById(R.id.closeBtn);
        musicName = findViewById(R.id.musicName);
        musicAlbum = findViewById(R.id.musicAlbum);
        /*seekBar = findViewById(R.id.seekBar);*/
        playBtn = findViewById(R.id.playBtn);
        videoView = findViewById(R.id.videoView);
        videoGallery = findViewById(R.id.videoGallery);
        camera = findViewById(R.id.camera);
        music = findViewById(R.id.music);
        send = findViewById(R.id.send);
        videoView.setVideoPath(videoPath);


        trimView = findViewById(R.id.trimmer);


        trimView.onTrimChangeListener = new TrimView.TrimChangeListener() {


            @Override
            public void onDragStarted(long trimStart, long trim) {
                trimView.setProgress(0);

            }

            @Override
            public void onLeftEdgeChanged(long trimStart, long trim) {
                trimView.setProgress(0);
            }

            @Override
            public void onRightEdgeChanged(long trimStart, long trim) {
                trimView.setProgress(0);
            }

            @Override
            public void onRangeChanged(long trimStart, long trim) {
                trimView.setProgress(0);

                if (musicMediaPlayer != null) {
                    positionVideo = 0;
                    playBtn.setVisibility(View.GONE);
                    videoView.seekTo(positionVideo);
                    videoView.start();
                    musicMediaPlayer.seekTo((int) trimStart);
                    musicMediaPlayer.start();

                }
            }

            @Override
            public void onDragStopped(long trimStart, long trim) {
                trimView.setProgress(0);
            }
        };


        // videoView.start();
        videoView.setOnCompletionListener(mediaPlayer -> {
            if (musicMediaPlayer != null && musicMediaPlayer.isPlaying()) {
                musicMediaPlayer.pause();
            }
        });
        videoView.setOnClickListener(view -> {
            if (videoView.isPlaying()) {
                playBtn.setVisibility(View.VISIBLE);
                positionVideo = videoView.getCurrentPosition();
                videoView.pause();
                if (musicMediaPlayer != null && musicMediaPlayer.isPlaying()) {
                    musicMediaPlayer.pause();
                }
            } else {
                playBtn.setVisibility(View.GONE);
                videoView.seekTo(positionVideo);
                videoView.start();
                if (musicMediaPlayer != null && !musicMediaPlayer.isPlaying()) {
                    musicMediaPlayer.start();
                }
            }
        });
        /*seekBar.setRangeChangeListener((view, leftPinIndex, rightPinIndex, totalCount) -> {
            if (musicMediaPlayer != null) {
                positionVideo = 0;
                playBtn.setVisibility(View.GONE);
                videoView.seekTo(positionVideo);
                videoView.start();
                musicMediaPlayer.seekTo((int) leftPinIndex);
                musicMediaPlayer.start();

            }
        });*/

        videoGallery.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_PERMISSIONS_VIDEO);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_VIDEO);
            } else {
                selectMedia();
            }
        });

        camera.setOnClickListener(view -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_PERMISSIONS_CAMERA);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_CAMERA);
            } else {
                selectCamera();
            }
        });

        music.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_PERMISSIONS_MUSIC);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_MUSIC);
            } else {
                openMusicPicker();
                //selectMusic();
            }


        });
        send.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.putExtra("VideoPath", videoPath);
            intent.putExtra("IS_DELETE_ALLOW", isDeleteVideoAllow);
            setResult(RESULT_OK, intent);
            finish();
        });

        findViewById(R.id.back_button).setOnClickListener(view -> {
            finish();
        });

        closeBtn.setOnClickListener(view -> {

            musicMediaPlayer.pause();
            musicMediaPlayer.release();
            musicMediaPlayer = null;
            masterAudioFile = null;
            videoGallery.setVisibility(View.VISIBLE);
            camera.setVisibility(View.VISIBLE);
            music.setVisibility(View.VISIBLE);
            send.setVisibility(View.VISIBLE);
            musicDetails.setVisibility(View.GONE);
            closeBtn.setVisibility(View.GONE);

            positionVideo = videoView.getCurrentPosition();
            videoView.setVideoPath(videoPath);
            videoView.seekTo(positionVideo);
            videoView.start();
            videoView.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.setVolume(1f, 1f);
            });
        });
        findViewById(R.id.syncBtn).setOnClickListener(view -> {

            try {

                Log.d("SYNC_DEBUG", "========== START ==========");

                videoEditor.stopRunningProcess();

                Log.d("SYNC_DEBUG", "stopRunningProcess done");

                long trimDuration = trimView.trim;

                Log.d("SYNC_DEBUG", "trimDuration : " + trimDuration);
                Log.d("SYNC_DEBUG", "videoDuration : " + videoDuration);
                Log.d("SYNC_DEBUG", "trimStart : " + trimView.trimStart);

                if (trimDuration > videoDuration) {

                    Log.d("SYNC_DEBUG",
                            "Toast Triggered : Please trim audio under duration");

                    Toast.makeText(
                            this,
                            "Please trim audio under "
                                    + convertDuration(videoDuration) + ".",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Log.d("SYNC_DEBUG", "Trim validation passed");

                File outputFile = createAudioFile();

                if (outputFile == null) {

                    Log.d("SYNC_DEBUG",
                            "Toast Triggered : outputFile null");

                    Toast.makeText(
                            this,
                            "Output file creation failed",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Log.d("SYNC_DEBUG",
                        "Output File Path : "
                                + outputFile.getAbsolutePath());

                loader.setCancelable(false);

                loader.show();

                Log.d("SYNC_DEBUG", "Loader shown");

                videoEditor.setType(
                        VideoEditConstant.INSTANCE.AUDIO_TRIM
                );

                Log.d("SYNC_DEBUG", "Type set");

                videoEditor.setAudioFile(masterAudioFile);

                Log.d("SYNC_DEBUG",
                        "Audio File : "
                                + (masterAudioFile != null
                                ? masterAudioFile.getAbsolutePath()
                                : "NULL"));

                videoEditor.setOutputPath(
                        outputFile.getAbsolutePath()
                );

                Log.d("SYNC_DEBUG", "Output path set");

                String startTime =
                        secToTime(trimView.trimStart);

                String endTime =
                        secToTime(trimView.trimStart + trimView.trim);

                Log.d("SYNC_DEBUG", "Start Time : " + startTime);
                Log.d("SYNC_DEBUG", "End Time : " + endTime);

                videoEditor.setStartTime(startTime);

                videoEditor.setEndTime(endTime);

                Log.d("SYNC_DEBUG", "Time set complete");

                videoEditor.setCallback(this);

                Log.d("SYNC_DEBUG", "Callback set");

                Log.d("SYNC_DEBUG", "Calling main()");

                videoEditor.main();

                Log.d("SYNC_DEBUG", "main() called successfully");

                Log.d("SYNC_DEBUG", "========== END ==========");

            } catch (Exception e) {

                Log.e("SYNC_DEBUG",
                        "Exception : " + e.getMessage());

                e.printStackTrace();

                Toast.makeText(
                        this,
                        "Exception : " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });


    }

    private void selectMusic() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, "audio/*");
            audioResultLauncher.launch(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("audio/*");
                audioResultLauncher.launch(intent);
            } catch (Exception e1) {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("audio/*");
                    audioResultLauncher.launch(intent);
                } catch (Exception e2) {
                    Toast.makeText(VideoEditActivity.this, "Application is not available", Toast.LENGTH_LONG).show();
                    e2.printStackTrace();
                }


            }
        }
    }

    private void selectMedia() {
        captureFile = null;
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, "video/*");
            activityResultLauncher.launch(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("video/*");
                activityResultLauncher.launch(intent);
            } catch (Exception e1) {
                try {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("video/*");
                    activityResultLauncher.launch(intent);
                } catch (Exception e2) {
                    Toast.makeText(VideoEditActivity.this, "Application is not available", Toast.LENGTH_LONG).show();
                    e2.printStackTrace();
                }
            }
        }
    }

    private void selectCamera() {

        Intent cameraIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        captureFile = createVideoFile();
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, getPackageName() + ".provider_smart_image_picker", captureFile));
        activityResultLauncher.launch(cameraIntent);
    }

    File createVideoFile() {
        String timeStamp = new SimpleDateFormat(ApplicationConstant.INSTANCE.DATE_FORMAT, Locale.getDefault()).format(new Date());
        String imageFileName = getString(R.string.app_name) + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (!storageDir.exists()) storageDir.mkdirs();
        try {
            return File.createTempFile(imageFileName, VideoEditConstant.INSTANCE.VIDEO_FORMAT, storageDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    File createAudioFile() {
        String timeStamp = new SimpleDateFormat(ApplicationConstant.INSTANCE.DATE_FORMAT, Locale.getDefault()).format(new Date());
        String imageFileName = getString(R.string.app_name) + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (!storageDir.exists()) storageDir.mkdirs();
        try {
            return File.createTempFile(imageFileName, VideoEditConstant.INSTANCE.AUDIO_FORMAT, storageDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_MUSIC) {
            int permissionCheck = PackageManager.PERMISSION_GRANTED;
            for (int permission : grantResults) {
                permissionCheck = permissionCheck + permission;
            }
            if ((grantResults.length > 0) && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                //  selectMusic();
                openMusicPicker();
            } else {
                showWarningSnack(R.string.str_ShowOnPermisstionDenied, "Enable", true);
            }
        } else if (requestCode == REQUEST_PERMISSIONS_VIDEO) {
            int permissionCheck = PackageManager.PERMISSION_GRANTED;
            for (int permission : grantResults) {
                permissionCheck = permissionCheck + permission;
            }
            if ((grantResults.length > 0) && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                selectMedia();
            } else {
                showWarningSnack(R.string.str_ShowOnPermisstionDenied, "Enable", true);
            }
        } else if (requestCode == REQUEST_PERMISSIONS_CAMERA) {
            int permissionCheck = PackageManager.PERMISSION_GRANTED;
            for (int permission : grantResults) {
                permissionCheck = permissionCheck + permission;
            }
            if ((grantResults.length > 0) && permissionCheck == PackageManager.PERMISSION_GRANTED) {

                selectCamera();
            } else {
                showWarningSnack(R.string.str_ShowOnPermisstionDenied, "Enable", true);
            }
        }
    }

    private void openMusicPicker() {
        ServerMusicPickerBottomSheet sheet = ServerMusicPickerBottomSheet.newInstance(
                (audioPath, startMs, endMs, title) ->
                        handleMusicSelected(audioPath, startMs, endMs));
        sheet.show(getSupportFragmentManager(), "MusicPicker");
    }

    private void handleMusicSelected(
            String audioPath,
            long startMs,
            long endMs
    ) {

        if (audioPath == null || audioPath.isEmpty()) {
            return;
        }

        if (audioPath.startsWith("http://")
                || audioPath.startsWith("https://")) {

            loader.show();

            new Thread(() -> {

                try {

                    String ext;

                    if (audioPath.contains(".m4a")) {
                        ext = ".m4a";
                    } else if (audioPath.contains(".aac")) {
                        ext = ".aac";
                    } else if (audioPath.contains(".wav")) {
                        ext = ".wav";
                    } else if (audioPath.contains(".ogg")) {
                        ext = ".ogg";
                    } else {
                        ext = ".mp3";
                    }

                    File rawFile = File.createTempFile(
                            "raw_audio_",
                            ext,
                            getExternalFilesDir(
                                    Environment.DIRECTORY_MUSIC
                            )
                    );

                    java.net.URL url =
                            new java.net.URL(audioPath);

                    java.net.HttpURLConnection conn =
                            (java.net.HttpURLConnection)
                                    url.openConnection();

                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);
                    conn.connect();

                    try (
                            java.io.InputStream is =
                                    conn.getInputStream();

                            java.io.FileOutputStream fos =
                                    new java.io.FileOutputStream(rawFile)
                    ) {

                        byte[] buf = new byte[8192];

                        int n;

                        while ((n = is.read(buf)) != -1) {
                            fos.write(buf, 0, n);
                        }

                        fos.flush();
                    }

                    Log.d(
                            "AUDIO_DOWNLOAD",
                            "DOWNLOADED : "
                                    + rawFile.getAbsolutePath()
                    );

                    boolean needConversion =
                            !ext.equals(".mp3");

                    if (needConversion) {

                        File mp3File = createMp3File();

                        String[] command = {
                                "-y",
                                "-i",
                                rawFile.getAbsolutePath(),
                                "-vn",
                                "-ar",
                                "44100",
                                "-ac",
                                "2",
                                "-b:a",
                                "192k",
                                "-c:a",
                                "libmp3lame",
                                mp3File.getAbsolutePath()
                        };

                        com.arthenica.mobileffmpeg.FFmpeg.executeAsync(
                                command,
                                (executionId, returnCode) -> {

                                    runOnUiThread(() -> {

                                        loader.dismiss();

                                        if (returnCode ==
                                                com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS) {

                                            Log.d(
                                                    "FFMPEG_CONVERT",
                                                    "MP3 SUCCESS"
                                            );

                                            rawFile.delete();

                                            applySelectedAudio(
                                                    mp3File.getAbsolutePath(),
                                                    startMs,
                                                    endMs
                                            );

                                        } else {

                                            Log.e(
                                                    "FFMPEG_CONVERT",
                                                    "FAILED : "
                                                            + returnCode
                                            );

                                            Toast.makeText(
                                                    VideoEditActivity.this,
                                                    "Audio conversion failed",
                                                    Toast.LENGTH_LONG
                                            ).show();

                                            applySelectedAudio(
                                                    rawFile.getAbsolutePath(),
                                                    startMs,
                                                    endMs
                                            );
                                        }
                                    });
                                }
                        );

                    } else {

                        runOnUiThread(() -> {

                            loader.dismiss();

                            applySelectedAudio(
                                    rawFile.getAbsolutePath(),
                                    startMs,
                                    endMs
                            );
                        });
                    }

                } catch (Exception e) {

                    e.printStackTrace();

                    runOnUiThread(() -> {

                        loader.dismiss();

                        Toast.makeText(
                                VideoEditActivity.this,
                                "Music download failed : "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }

            }).start();

        } else {

            applySelectedAudio(
                    audioPath,
                    startMs,
                    endMs
            );
        }
    }

    private File createMp3File() {

        String timeStamp =
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.getDefault()
                ).format(new Date());

        File storageDir =
                getExternalFilesDir(
                        Environment.DIRECTORY_MUSIC
                );

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        return new File(
                storageDir,
                "converted_" + timeStamp + ".mp3"
        );
    }

    private void applySelectedAudio(String localPath, long startMs, long endMs) {

        if (musicMediaPlayer != null) {
            try {
                if (musicMediaPlayer.isPlaying()) musicMediaPlayer.stop();
                musicMediaPlayer.release();
            } catch (Exception ignored) {
            }
            musicMediaPlayer = null;
        }

        // 2. masterAudioFile set
        masterAudioFile = new File(localPath);

        // 3. Durations
        videoDuration = Utility.INSTANCE.getFileDuration(VideoEditActivity.this, videoPath);
        long audioDuration = Utility.INSTANCE.getFileDuration(VideoEditActivity.this, localPath);

        if (postType != 2 && videoDuration > audioDuration) {
            Toast.makeText(this, "Audio duration is not matching with video duration", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. UI — musicDetails panel show, buttons hide
        videoGallery.setVisibility(View.GONE);
        camera.setVisibility(View.GONE);
        music.setVisibility(View.GONE);
        send.setVisibility(View.GONE);
        musicDetails.setVisibility(View.VISIBLE);
        closeBtn.setVisibility(View.VISIBLE);

        if (postType == 2 && videoDuration > 60000 && audioDuration > 60000) {
            Toast.makeText(this, "Longer audio are shortened to 60 seconds long", Toast.LENGTH_SHORT).show();
            trimView.max = audioDuration;
            trimView.minTrim = 0;
            trimView.maxTrim = 60000;
            trimView.setTrim(trimView.maxTrim);
        } else if (postType == 2 && videoDuration < 60000 && audioDuration > 60000) {
            Toast.makeText(this, "Longer audio are shortened to 60 seconds long", Toast.LENGTH_SHORT).show();
            trimView.max = audioDuration;
            trimView.minTrim = 0;
            trimView.maxTrim = videoDuration;
            trimView.setTrim(trimView.maxTrim);
        } else if (postType == 2 && videoDuration > 60000 && audioDuration < 60000) {
            trimView.max = audioDuration;
            trimView.minTrim = 0;
            trimView.maxTrim = audioDuration;
            trimView.setTrim(trimView.maxTrim);
        } else if (postType == 2 && videoDuration < 60000 && audioDuration < 60000) {
            trimView.max = audioDuration;
            trimView.minTrim = 0;
            trimView.maxTrim = Math.min(audioDuration, videoDuration);
            trimView.setTrim(trimView.maxTrim);
        } else {
            trimView.max = audioDuration;
            trimView.minTrim = videoDuration;
            trimView.maxTrim = videoDuration;
            trimView.setTrim(videoDuration);
        }

        if (startMs > 0 || endMs > 0) {
            trimView.trimStart = startMs;
            trimView.setTrim(endMs - startMs);
        }

        Glide.with(VideoEditActivity.this)
                .load(getSongCover(masterAudioFile.getPath()))
                .placeholder(R.drawable.music_ph)
                .error(R.drawable.music_ph)
                .dontAnimate()
                .into(musicAlbum);
        musicName.setText(masterAudioFile.getName());

        playBtn.setVisibility(View.GONE);
        positionVideo = 0;
        videoView.setVideoPath(videoPath);
        videoView.setOnPreparedListener(mp -> {
            mp.setVolume(0f, 0f);
            videoView.seekTo(0);
            videoView.start();
        });

        try {
            musicMediaPlayer = new MediaPlayer();
            musicMediaPlayer.setDataSource(localPath);
            musicMediaPlayer.prepare();
            musicMediaPlayer.seekTo((int) startMs);
            musicMediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        syncMusicWithVideo(startMs, endMs);
    }

    private void syncMusicWithVideo(long startMs, long endMs) {

        if (masterAudioFile == null) return;

        videoEditor.stopRunningProcess();
        long resolvedStart = startMs;
        long resolvedEnd = (endMs > 0) ? endMs : videoDuration;
        long trimDuration = resolvedEnd - resolvedStart;
        if (trimDuration > videoDuration) {
            resolvedEnd = resolvedStart + videoDuration;
            trimDuration = videoDuration;
        }

        trimView.trimStart = resolvedStart;
        trimView.setTrim(trimDuration);

        File outputAudioFile = createAudioFile();

        loader.setCancelable(false);
        loader.show();

        videoEditor.setType(VideoEditConstant.INSTANCE.AUDIO_TRIM);
        videoEditor.setAudioFile(masterAudioFile);
        videoEditor.setOutputPath(outputAudioFile.getAbsolutePath());
        videoEditor.setStartTime(secToTime(resolvedStart));
        videoEditor.setEndTime(secToTime(resolvedEnd));
        videoEditor.setCallback(this);
        videoEditor.main();
    }

    void showWarningSnack(int stringId, String btn, final boolean isForSetting) {
        if (mSnackBar != null && mSnackBar.isShown()) {
            return;
        }

        mSnackBar = Snackbar.make(findViewById(android.R.id.content), stringId,
                Snackbar.LENGTH_INDEFINITE).setAction(btn,
                v -> {
                    if (isForSetting) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        startActivity(intent);
                    } else {
                        ActivityCompat.requestPermissions(VideoEditActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_MUSIC);
                    }

                });

        mSnackBar.setActionTextColor(getResources().getColor(R.color.colorPrimary));
        TextView mainTextView = (TextView) (mSnackBar.getView()).findViewById(com.google.android.material.R.id.snackbar_text);
        mainTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(com.intuit.ssp.R.dimen._12ssp));
        mainTextView.setMaxLines(4);
        mSnackBar.show();

    }

    void setResultAudio(String filePath) {
        masterAudioFile = new File(filePath);
        videoDuration = Utility.INSTANCE.getFileDuration(VideoEditActivity.this, videoPath);
        long audioDuration = Utility.INSTANCE.getFileDuration(VideoEditActivity.this, filePath);
        if (postType != 2 && videoDuration > audioDuration) {
            Toast.makeText(this, "Audio duration is not matching with video duration", Toast.LENGTH_SHORT).show();
        } else {
            videoGallery.setVisibility(View.GONE);
            camera.setVisibility(View.GONE);
            music.setVisibility(View.GONE);
            send.setVisibility(View.GONE);
            musicDetails.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.VISIBLE);


            if (postType == 2 && videoDuration > 60000 && audioDuration > 60000) {
                Toast.makeText(this, "Longer audio are shortened to 60 seconds long", Toast.LENGTH_SHORT).show();
                trimView.max = audioDuration;
                trimView.minTrim = 0;
                trimView.maxTrim = 60000;//60 sec
                trimView.setTrim(trimView.maxTrim);
            } else if (postType == 2 && videoDuration < 60000 && audioDuration > 60000) {
                Toast.makeText(this, "Longer audio are shortened to 60 seconds long", Toast.LENGTH_SHORT).show();
                trimView.max = audioDuration;
                trimView.minTrim = 0;
                trimView.maxTrim = videoDuration;
                trimView.setTrim(trimView.maxTrim);
            } else if (postType == 2 && videoDuration > 60000 && audioDuration < 60000) {
                //Toast.makeText(this, "Longer audio are shortened to 60 seconds long", Toast.LENGTH_SHORT).show();
                trimView.max = audioDuration;
                trimView.minTrim = 0;
                trimView.maxTrim = audioDuration;
                trimView.setTrim(trimView.maxTrim);
            } else if (postType == 2 && videoDuration < 60000 && audioDuration < 60000) {
                //Toast.makeText(this, "Longer audio are shortened to 60 seconds long", Toast.LENGTH_SHORT).show();
                trimView.max = audioDuration;
                trimView.minTrim = 0;
                trimView.maxTrim = Math.min(audioDuration, videoDuration);
                trimView.setTrim(trimView.maxTrim);
            } else {
                trimView.max = audioDuration;
                trimView.minTrim = videoDuration;
                trimView.maxTrim = videoDuration;
                trimView.setTrim(videoDuration);
            }




                           /* seekBar.setTickCount((int) audioDuration);
                            seekBar.setRangeIndex(0, videoDuration);
                            seekBar.moveThumb(0, videoDuration);
                            seekBar.setVideoDuration(videoDuration);*/

            Glide.with(VideoEditActivity.this)
                    .load(getSongCover(masterAudioFile.getPath()))
                    .placeholder(R.drawable.music_ph)
                    .error(R.drawable.music_ph)
                    .dontAnimate()
                    .into(musicAlbum);
            musicName.setText(masterAudioFile.getName());

            playBtn.setVisibility(View.GONE);
            positionVideo = 0;
            videoView.seekTo(0);
            videoView.setOnPreparedListener(mp -> mp.setVolume(0, 0));
            try {
                musicMediaPlayer = new MediaPlayer();
                musicMediaPlayer.setDataSource(masterAudioFile.getPath());
                musicMediaPlayer.prepare();
                musicMediaPlayer.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        //Log.e("File Path",masterAudioFile.getPath());
                            /*masterAudioFile?.let { file ->
                                    tvSelectedAudio!!.text = masterAudioFile!!.name.toString();
                                //setFilePathFromSource(file)
                                setControls(true);

                                if (Util.SDK_INT <= 23) {
                                    initializePlayer();
                                }
                            }*/
    }

    public byte[] getSongCover(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(path);
        return mmr.getEmbeddedPicture();
    }

    public String convertDuration(long duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        //Log.v(tagName, "min: " + minutes);

        if (minutes > 0) {
            return minutes + " Minutes";
        } else {
            return "00:" + (TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
        }
    }

    String secToTime(Long totalSeconds) {
        return String.format(
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(totalSeconds),
                TimeUnit.MILLISECONDS.toMinutes(totalSeconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(totalSeconds)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(totalSeconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalSeconds))
        );
    }

    @Override
    protected void onPause() {

        positionVideo = videoView.getCurrentPosition();
        videoView.pause();

        if (musicMediaPlayer != null && musicMediaPlayer.isPlaying()) {
            musicMediaPlayer.pause();
        }
       /* if(mediaPlayer!=null) {
            mediaPlayer.pause();
        }*/
        super.onPause();
    }

    @Override
    protected void onResume() {
        videoView.seekTo(positionVideo);
        videoView.start();
        if (musicMediaPlayer != null && musicDetails.getVisibility() == View.VISIBLE && !musicMediaPlayer.isPlaying()) {
            musicMediaPlayer.start();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        videoView.suspend();
        videoView.stopPlayback();
        if (musicMediaPlayer != null) {
            musicMediaPlayer.pause();
            musicMediaPlayer.release();
        }

        super.onDestroy();
    }

    @Override
    public void onProgress(@NonNull String progress) {
        /* Log.e("Test",progress);*/
    }

    @Override
    public void onSuccess(@NonNull File convertedFile, @NonNull int type) {

        Log.d("FFMPEG_SUCCESS", "TYPE : " + type);

        if (type == VideoEditConstant.INSTANCE.AUDIO_TRIM) {

            Log.d("FFMPEG_SUCCESS", "AUDIO TRIM SUCCESS");

            convertedAudioFile = convertedFile;

            Log.d("FFMPEG_SUCCESS",
                    "Trimmed Audio : "
                            + convertedFile.getAbsolutePath());

            videoEditor.setType(
                    VideoEditConstant.INSTANCE.VIDEO_AUDIO_MERGE
            );

            videoEditor.setAudioFile(convertedFile);

            // FIXED
            videoEditor.setAudioDuration(
                    String.valueOf(trimView.trim)
            );

            Log.d("FFMPEG_SUCCESS",
                    "Audio Duration : "
                            + trimView.trim);

            videoEditor.setFile(new File(videoPath));

            File outputVideo = createVideoFile();

            Log.d("FFMPEG_SUCCESS",
                    "Output Video : "
                            + outputVideo.getAbsolutePath());

            videoEditor.setOutputPath(
                    outputVideo.getAbsolutePath()
            );

            videoEditor.setCallback(this);

            Log.d("FFMPEG_SUCCESS",
                    "STARTING VIDEO_AUDIO_MERGE");

            videoEditor.main();

        } else {

            Log.d("FFMPEG_SUCCESS",
                    "VIDEO MERGE SUCCESS");

            if (convertedAudioFile != null
                    && convertedAudioFile.exists()) {

                convertedAudioFile.delete();
            }

            loader.dismiss();

            isDeleteVideoAllow = true;

            if (musicMediaPlayer != null) {

                try {

                    if (musicMediaPlayer.isPlaying()) {
                        musicMediaPlayer.pause();
                    }

                    musicMediaPlayer.release();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                musicMediaPlayer = null;
            }

            masterAudioFile = null;

            videoGallery.setVisibility(View.VISIBLE);
            camera.setVisibility(View.VISIBLE);
            music.setVisibility(View.VISIBLE);
            send.setVisibility(View.VISIBLE);

            musicDetails.setVisibility(View.GONE);
            closeBtn.setVisibility(View.GONE);

            videoPath = convertedFile.getPath();

            Log.d("FFMPEG_SUCCESS",
                    "FINAL VIDEO : " + videoPath);

            videoView.setVideoPath(videoPath);

            positionVideo = 0;

            videoView.start();

            videoView.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.setVolume(1f, 1f);
            });
        }
    }

    @Override
    public void onFailure(@NonNull Exception error) {
        Log.e("FFMPEG_ERROR", "========== FAILED ==========");
        Log.e("FFMPEG_ERROR", "Message : " + error.getMessage());
        Log.e("FFMPEG_ERROR", "Cause : " + error.getCause());

        error.printStackTrace();

        Toast.makeText(
                this,
                "FFMPEG Error : " + error.getMessage(),
                Toast.LENGTH_LONG
        ).show();

        loader.dismiss();
    }

   /* @Override
    public void onNotAvailable(@NonNull Exception error) {
        loader.dismiss();
    }*/

    @Override
    public void onCancel() {
        loader.dismiss();
    }
}