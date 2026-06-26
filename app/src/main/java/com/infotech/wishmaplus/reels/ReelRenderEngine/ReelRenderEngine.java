package com.infotech.wishmaplus.reels.ReelRenderEngine;



import static com.infotech.wishmaplus.ThumbnailHelper.executor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.MultiClipMerger;
import com.arthenica.mobileffmpeg.LogMessage;
import com.arthenica.mobileffmpeg.Statistics;
import com.infotech.wishmaplus.Api.Response.MediaModel;
import com.infotech.wishmaplus.ImageToVideoConverter;
import com.infotech.wishmaplus.ThumbnailHelper;
import com.infotech.wishmaplus.reels.bottomsheet.EffectsBottomSheet;
import com.infotech.wishmaplus.Utils.VideoEdit.CallBackOfQuery;
import com.infotech.wishmaplus.Utils.VideoEdit.interfaces.FFmpegCallBack;
import com.infotech.wishmaplus.reels.ui.componets.FilterEngine;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ReelRenderEngine {

    public interface RenderCallback {
        void onProgress(int percent, String message);
        void onComplete(String outputPath);
        void onError(String error);
    }

    // ── Universal encode flags — sabhi Android devices pe chalega ────────────
    // H.264 Baseline Profile Level 3.1 = maximum compatibility
    // yuv420p = sabhi players support karte hain
    private static final String[] UNIVERSAL_VIDEO_FLAGS = {
            "-c:v", "libx264",
            "-profile:v", "baseline",
            "-level", "3.1",
            "-pix_fmt", "yuv420p",
            "-movflags", "+faststart",  // web streaming ke liye
            "-preset", "ultrafast"
    };

    // ── Settings ──────────────────────────────────────────────────────────────
    private float speed = 1.0f;
    private float originalVolume = 1.0f;
    private float musicVolume = 0.8f;
    private EffectsBottomSheet.EffectModel effect = null;

    // Green screen
    private String gsBgPath = null;
    private float gsTolerance = 0.4f;
    private float gsSpill = 0.3f;
    private int gsKeyColor = android.graphics.Color.GREEN;

    // Trim
    private long trimStartMs = 0;
    private long trimEndMs = 0;

    // Filter
    private FilterEngine.FilterState filterState = null;

    // Template
    private String templateName = null;
    private int templateDuration = 0;

    private boolean cancelled = false;

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setSpeed(float speed) { this.speed = speed; }
    public void setOriginalVolume(float v) { this.originalVolume = v; }
    public void setMusicVolume(float v) { this.musicVolume = v; }
    public void setEffect(EffectsBottomSheet.EffectModel e) { this.effect = e; }
    public void setTrim(long startMs, long endMs) { this.trimStartMs = startMs; this.trimEndMs = endMs; }
    public void setFilterState(FilterEngine.FilterState fs) { this.filterState = fs; }
    public void setGreenScreenKeyColor(int color) { this.gsKeyColor = color; }
    public void setTemplate(String name, int durationSec) { this.templateName = name; this.templateDuration = durationSec; }
    public void setGreenScreenBg(String bgPath, float tolerance, float spill) {
        this.gsBgPath = bgPath;
        this.gsTolerance = tolerance;
        this.gsSpill = spill;
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final CallBackOfQuery callBackOfQuery = new CallBackOfQuery();

    public ReelRenderEngine(Context context) {
        this.context = context;
    }

    // =========================================================================
    // MAIN ENTRY
    // =========================================================================
    public void render(
            String mediaPath, boolean isImage, View overlayView,
            String musicPath, long musicStartMs, long musicEndMs,
            int durationSec, RenderCallback callback) {

        cancelled = false;
        callback.onProgress(5, "Capturing overlays...");
        Bitmap overlayBmp = captureView(overlayView);

        if (templateDuration > 0 && isImage) durationSec = templateDuration;

        if (isImage) {
            renderImage(mediaPath, overlayBmp, musicPath, musicStartMs, musicEndMs, durationSec, callback);
        } else {
            renderVideo(mediaPath, overlayBmp, musicPath, musicStartMs, musicEndMs, callback);
        }
    }

    // =========================================================================
    // IMAGE RENDER
    // =========================================================================
    private void renderImage(
            String imagePath, Bitmap overlayBmp,
            String musicPath, long musicStartMs, long musicEndMs,
            int durationSec, RenderCallback callback) {

        callback.onProgress(10, "Preparing image...");
        Bitmap base = android.graphics.BitmapFactory.decodeFile(imagePath);
        if (base == null) { callback.onError("Image decode failed"); return; }

        // Filter apply karo image pe
        if (hasAnyFilter()) base = FilterEngine.applyToBitmap(base, filterState);

        String mergedPath = mergeOverlayOnBitmap(base, overlayBmp);
        if (mergedPath == null) { callback.onError("Image prepare failed"); return; }

        callback.onProgress(20, "Converting image to video...");
        String outPath = newCacheFile("img_video_", ".mp4");

        String vfSpeed = (speed != 1.0f)
                ? ",setpts=" + String.format(java.util.Locale.US, "%.4f", 1f / speed) + "*PTS"
                : "";

        // ★ UNIVERSAL: scale + pad + baseline profile
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("-loop"); cmd.add("1");
        cmd.add("-i"); cmd.add(mergedPath);
        addAll(cmd, UNIVERSAL_VIDEO_FLAGS);
        cmd.add("-t"); cmd.add(String.valueOf(durationSec));
        cmd.add("-vf");
        cmd.add("scale=1080:1920:force_original_aspect_ratio=decrease,"
                + "pad=1080:1920:(ow-iw)/2:(oh-ih)/2,format=yuv420p" + vfSpeed);
        cmd.add("-r"); cmd.add("30");
        cmd.add("-y"); cmd.add(outPath);

        callBackOfQuery.callQuery(cmd.toArray(new String[0]), new FFmpegCallBack() {
            @Override public void process(LogMessage l) {}
            @Override public void statisticsProcess(Statistics s) {
                int pct = 20 + (int) Math.min(40, s.getTime() / (durationSec * 10f));
                mainHandler.post(() -> callback.onProgress(pct, "Converting image..."));
            }
            @Override public void success() {
                if (notEmpty(musicPath))
                    mixAudio(outPath, musicPath, musicStartMs, musicEndMs, callback);
                else mainHandler.post(() -> { callback.onProgress(100, "Done!"); callback.onComplete(outPath); });
            }
            @Override public void cancel() { mainHandler.post(() -> callback.onError("Cancelled")); }
            @Override public void failed() { mainHandler.post(() -> callback.onError("Image→video failed")); }
        });
    }

    // =========================================================================
    // VIDEO RENDER — trim → green screen → overlay+filter+speed → audio
    // =========================================================================
    private void renderVideo(
            String videoPath, Bitmap overlayBmp,
            String musicPath, long musicStartMs, long musicEndMs,
            RenderCallback callback) {

        boolean hasTrim = (trimStartMs > 0) || (trimEndMs > 0 && trimEndMs > trimStartMs);

        if (hasTrim) {
            callback.onProgress(10, "Trimming video...");
            String trimOut = newCacheFile("trimmed_", ".mp4");
            double startSec = trimStartMs / 1000.0;
            double durSec = (trimEndMs > trimStartMs) ? (trimEndMs - trimStartMs) / 1000.0 : 60.0;

            // ★ -ss BEFORE -i for accurate seek + universal flags
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add("-ss"); cmd.add(String.format(java.util.Locale.US, "%.3f", startSec));
            cmd.add("-i"); cmd.add(videoPath);
            cmd.add("-t"); cmd.add(String.format(java.util.Locale.US, "%.3f", durSec));
            addAll(cmd, UNIVERSAL_VIDEO_FLAGS);
            cmd.add("-c:a"); cmd.add("aac");
            cmd.add("-avoid_negative_ts"); cmd.add("make_zero");
            cmd.add("-y"); cmd.add(trimOut);

            callBackOfQuery.callQuery(cmd.toArray(new String[0]), new FFmpegCallBack() {
                @Override public void process(LogMessage l) {}
                @Override public void statisticsProcess(Statistics s) {
                    mainHandler.post(() -> callback.onProgress(
                            Math.min(25, 10 + (int)(s.getTime()/200f)), "Trimming..."));
                }
                @Override public void success() {
                    String src = fileValid(trimOut) ? trimOut : videoPath;
                    stepGreenScreen(src, overlayBmp, musicPath, musicStartMs, musicEndMs, callback);
                }
                @Override public void cancel() { mainHandler.post(() -> callback.onError("Trim cancelled")); }
                @Override public void failed() {
                    // Trim fail → original se continue
                    stepGreenScreen(videoPath, overlayBmp, musicPath, musicStartMs, musicEndMs, callback);
                }
            });
        } else {
            stepGreenScreen(videoPath, overlayBmp, musicPath, musicStartMs, musicEndMs, callback);
        }
    }

    // ── STEP B: GREEN SCREEN ──────────────────────────────────────────────────
    private void stepGreenScreen(
            String videoPath, Bitmap overlayBmp,
            String musicPath, long musicStartMs, long musicEndMs, RenderCallback callback) {

        if (!notEmpty(gsBgPath)) {
            stepBurnOverlay(videoPath, overlayBmp, musicPath, musicStartMs, musicEndMs, callback);
            return;
        }

        callback.onProgress(28, "Applying green screen...");
        String gsOut = newCacheFile("gs_", ".mp4");
        String keyHex = String.format("0x%06X", gsKeyColor & 0x00FFFFFF);
        String chromaFilter = String.format(java.util.Locale.US,
                "[1:v]scale=iw:ih[bg];[0:v]chromakey=%s:%.2f:%.2f[fg];[bg][fg]overlay,format=yuv420p[out]",
                keyHex, gsTolerance, gsSpill);

        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add("-i"); cmd.add(videoPath);
        cmd.add("-i"); cmd.add(gsBgPath);
        cmd.add("-filter_complex"); cmd.add(chromaFilter);
        cmd.add("-map"); cmd.add("[out]");
        cmd.add("-map"); cmd.add("0:a?");
        addAll(cmd, UNIVERSAL_VIDEO_FLAGS);
        cmd.add("-c:a"); cmd.add("copy");
        cmd.add("-y"); cmd.add(gsOut);

        callBackOfQuery.callQuery(cmd.toArray(new String[0]), new FFmpegCallBack() {
            @Override public void process(LogMessage l) {}
            @Override public void statisticsProcess(Statistics s) {
                mainHandler.post(() -> callback.onProgress(
                        Math.min(45, 28 + (int)(s.getTime()/300f)), "Green screen..."));
            }
            @Override public void success() {
                String src = fileValid(gsOut) ? gsOut : videoPath;
                stepBurnOverlay(src, overlayBmp, musicPath, musicStartMs, musicEndMs, callback);
            }
            @Override public void cancel() { mainHandler.post(() -> callback.onError("GS cancelled")); }
            @Override public void failed() {
                stepBurnOverlay(videoPath, overlayBmp, musicPath, musicStartMs, musicEndMs, callback);
            }
        });
    }

    // ── STEP C: OVERLAY + FILTER + SPEED ─────────────────────────────────────
    private void stepBurnOverlay(
            String videoPath, Bitmap overlay,
            String musicPath, long musicStartMs, long musicEndMs, RenderCallback callback) {

        String vfChain = buildVideoFilterChain();
        boolean hasOverlay = overlay != null;
        boolean hasVf = !vfChain.isEmpty();

        // Kuch nahi lagana — seedha audio step pe jao
        if (!hasOverlay && !hasVf) {
            stepAudio(videoPath, musicPath, musicStartMs, musicEndMs, callback);
            return;
        }

        try {
            String outPath = newCacheFile("burned_", ".mp4");
            java.util.List<String> cmd = new java.util.ArrayList<>();

            if (hasOverlay) {
                // Overlay PNG save karo
                File ovFile = new File(context.getCacheDir(),
                        "overlay_" + System.currentTimeMillis() + ".png");
                FileOutputStream fos = new FileOutputStream(ovFile);
                overlay.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();

                String fc;
                if (hasVf) {
                    // Overlay + Filter dono
                    // ★ FIX: [out] label properly assign karo
                    fc = "[1:v]scale=iw:ih[ov];" +
                            "[0:v][ov]overlay=0:0[tmp];" +
                            "[tmp]" + vfChain + ",format=yuv420p[out]";
                } else {
                    // Sirf overlay
                    fc = "[1:v]scale=iw:ih[ov];" +
                            "[0:v][ov]overlay=0:0,format=yuv420p[out]";
                }

                cmd.add("-i"); cmd.add(videoPath);
                cmd.add("-i"); cmd.add(ovFile.getAbsolutePath());
                cmd.add("-filter_complex"); cmd.add(fc);
                cmd.add("-map"); cmd.add("[out]");
                cmd.add("-map"); cmd.add("0:a?");   // ★ audio optional

            } else {
                // Sirf filter/speed — simple -vf
                cmd.add("-i"); cmd.add(videoPath);
                /*cmd.add("-vf"); cmd.add(vfChain + ",format=yuv420p");*/
                cmd.add("-vf");
                cmd.add("scale=1080:1920:force_original_aspect_ratio=decrease,"
                        + "pad=1080:1920:(ow-iw)/2:(oh-ih)/2,"
                        + vfChain + ","
                        + "format=yuv420p");
            }

            // ★ Universal flags sabhi cases mein
            addAll(cmd, UNIVERSAL_VIDEO_FLAGS);
            cmd.add("-c:a"); cmd.add("copy");
            cmd.add("-y"); cmd.add(outPath);

            callBackOfQuery.callQuery(cmd.toArray(new String[0]), new FFmpegCallBack() {
                @Override public void process(LogMessage l) {
                    android.util.Log.d("FFMPEG_LOG", l.getText());
                }
                @Override public void statisticsProcess(Statistics s) {
                    mainHandler.post(() -> callback.onProgress(
                            Math.min(65, 45 + (int)(s.getTime()/200f)), "Applying effects..."));
                }
                @Override public void success() {
                    // ★ File valid hai tabhi aage jao
                    if (fileValid(outPath)) {
                        stepAudio(outPath, musicPath, musicStartMs, musicEndMs, callback);
                    } else {
                        android.util.Log.e("FFMPEG_ERR", "Output invalid: " + outPath);
                        mainHandler.post(() -> callback.onError("Filter apply failed — try again"));
                    }
                }
                @Override public void cancel() { mainHandler.post(() -> callback.onError("Cancelled")); }
                @Override public void failed() {
                    android.util.Log.e("FFMPEG_ERR", "stepBurnOverlay failed. vfChain=" + vfChain);
                    mainHandler.post(() -> callback.onError("Effect apply failed — please retry"));
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Overlay prepare failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // VIDEO FILTER CHAIN BUILDER
    // =========================================================================
    private String buildVideoFilterChain() {
        StringBuilder vf = new StringBuilder();

        if (filterState != null) {
            float b = filterState.brightness;   // -1..+1  neutral=0
            float c = filterState.contrast;     //  0..3   neutral=1
            float s = filterState.saturation;   //  0..3   neutral=1
            float w = filterState.warmth;       // -1..+1  neutral=0

            boolean needEq = Math.abs(b) > 0.01f
                    || Math.abs(c - 1f) > 0.01f
                    || Math.abs(s - 1f) > 0.01f
                    || (filterState.preset != FilterEngine.Preset.ORIGINAL);

            // Warmth → colorchannelmixer
            if (Math.abs(w) > 0.01f) {
                float rr = Math.max(0.0f, Math.min(2.0f, 1.0f + w * 0.3f));
                float bb = Math.max(0.0f, Math.min(2.0f, 1.0f - w * 0.3f));
                appendVf(vf, String.format(java.util.Locale.US,
                        "colorchannelmixer=rr=%.3f:bb=%.3f", rr, bb));
            }

            // eq filter — brightness + contrast + saturation
            if (needEq) {
                // ★ FFmpeg eq: brightness -1..+1, contrast 0..3, saturation 0..3
                appendVf(vf, String.format(java.util.Locale.US,
                        "eq=brightness=%.3f:contrast=%.3f:saturation=%.3f", b, c, s));
            }
        }

        // Speed
        if (speed != 1.0f) {
            appendVf(vf, String.format(java.util.Locale.US, "setpts=%.4f*PTS", 1f / speed));
        }

        return vf.toString();
    }

    private void appendVf(StringBuilder sb, String filter) {
        if (sb.length() > 0) sb.append(",");
        sb.append(filter);
    }

    private boolean hasAnyFilter() {
        if (filterState == null) return false;
        return filterState.preset != FilterEngine.Preset.ORIGINAL
                || Math.abs(filterState.brightness) > 0.01f
                || Math.abs(filterState.contrast - 1f) > 0.01f
                || Math.abs(filterState.saturation - 1f) > 0.01f
                || Math.abs(filterState.warmth) > 0.01f;
    }

    // ── STEP D: AUDIO ─────────────────────────────────────────────────────────
    private void stepAudio(String videoPath, String musicPath,
                           long musicStartMs, long musicEndMs, RenderCallback callback) {
        if (notEmpty(musicPath)) {
            mainHandler.post(() -> callback.onProgress(68, "Mixing audio..."));
            mixAudio(videoPath, musicPath, musicStartMs, musicEndMs, callback);
        } else if (Math.abs(originalVolume - 1f) > 0.01f) {
            adjustOriginalVolume(videoPath, callback);
        } else {
            mainHandler.post(() -> {
                callback.onProgress(100, "Done!");
                callback.onComplete(videoPath);
            });
        }
    }

    private void adjustOriginalVolume(String videoPath, RenderCallback callback) {
        if (!hasAudioTrack(videoPath)) {
            mainHandler.post(() -> { callback.onProgress(100, "Done!"); callback.onComplete(videoPath); });
            return;
        }
        String outPath = newCacheFile("vol_", ".mp4");
        String[] cmd = {
                "-i", videoPath,
                "-filter:a", String.format(java.util.Locale.US, "volume=%.2f", originalVolume),
                "-c:v", "copy",
                "-y", outPath
        };
        callBackOfQuery.callQuery(cmd, new FFmpegCallBack() {
            @Override public void process(LogMessage l) {}
            @Override public void statisticsProcess(Statistics s) {}
            @Override public void success() {
                String r = fileValid(outPath) ? outPath : videoPath;
                mainHandler.post(() -> { callback.onProgress(100, "Done!"); callback.onComplete(r); });
            }
            @Override public void cancel() { mainHandler.post(() -> callback.onError("Cancelled")); }
            @Override public void failed() {
                mainHandler.post(() -> { callback.onProgress(100, "Done!"); callback.onComplete(videoPath); });
            }
        });
    }

    @SuppressLint("DefaultLocale")
    private void mixAudio(String videoPath, String musicPath,
                          long startMs, long endMs, RenderCallback callback) {
        try {
            String outPath = newCacheFile("final_reel_", ".mp4");
            boolean videoHasAudio = hasAudioTrack(videoPath);
            String origVol = String.format(java.util.Locale.US, "volume=%.2f", originalVolume);
            String musicVol = String.format(java.util.Locale.US, "volume=%.2f", musicVolume);
            String atempo = buildAtempoFilter(speed);

            double startSec = startMs / 1000.0;
            double durSec = (endMs > startMs) ? (endMs - startMs) / 1000.0 : -1;
            boolean hasTrimMs = startMs > 0 || durSec > 0;

            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add("-i"); cmd.add(videoPath);
            if (hasTrimMs) {
                cmd.add("-ss"); cmd.add(String.format(java.util.Locale.US, "%.2f", startSec));
                if (durSec > 0) { cmd.add("-t"); cmd.add(String.format(java.util.Locale.US, "%.2f", durSec)); }
            }
            cmd.add("-i"); cmd.add(musicPath);

            String fc;
            if (videoHasAudio) {
                fc = String.format("[0:a]%s[a0];[1:a]%s%s[a1];[a0][a1]amix=inputs=2:duration=shortest:dropout_transition=2[aout]",
                        origVol, atempo.isEmpty() ? "" : atempo + ",", musicVol);
            } else {
                fc = String.format("[1:a]%s%s[aout]", atempo.isEmpty() ? "" : atempo + ",", musicVol);
            }

            cmd.add("-filter_complex"); cmd.add(fc);
            cmd.add("-map"); cmd.add("0:v");
            cmd.add("-map"); cmd.add("[aout]");
            cmd.add("-c:v"); cmd.add("copy");   // video already encoded hai — copy karo
            cmd.add("-c:a"); cmd.add("aac");
            cmd.add("-shortest");
            cmd.add("-y"); cmd.add(outPath);

            callBackOfQuery.callQuery(cmd.toArray(new String[0]), new FFmpegCallBack() {
                @Override public void process(LogMessage l) {}
                @Override public void statisticsProcess(Statistics s) {
                    mainHandler.post(() -> callback.onProgress(
                            Math.min(95, 68 + (int)(s.getTime()/300f)), "Mixing audio..."));
                }
                @Override public void success() {
                    String fp = fileValid(outPath) ? outPath : videoPath;
                    mainHandler.post(() -> { callback.onProgress(100, "Done!"); callback.onComplete(fp); });
                }
                @Override public void cancel() { mainHandler.post(() -> callback.onError("Audio cancelled")); }
                @Override public void failed() {
                    // Audio mix fail → video bhi sahi ja sakta hai
                    mainHandler.post(() -> { callback.onProgress(100, "Done (no audio)!"); callback.onComplete(videoPath); });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            callback.onError("Audio mix error: " + e.getMessage());
        }
    }

    private String buildAtempoFilter(float speed) {
        if (speed == 1.0f) return "";
        if (speed >= 0.5f && speed <= 2.0f)
            return String.format(java.util.Locale.US, "atempo=%.4f", speed);
        StringBuilder sb = new StringBuilder();
        float r = speed;
        while (r > 2.0f) { if (sb.length() > 0) sb.append(","); sb.append("atempo=2.0"); r /= 2f; }
        while (r < 0.5f) { if (sb.length() > 0) sb.append(","); sb.append("atempo=0.5"); r *= 2f; }
        if (sb.length() > 0) sb.append(",");
        sb.append(String.format(java.util.Locale.US, "atempo=%.4f", r));
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String mergeOverlayOnBitmap(Bitmap base, Bitmap overlay) {
        try {
            Bitmap merged = Bitmap.createBitmap(base.getWidth(), base.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(merged);
            canvas.drawBitmap(base, 0, 0, null);
            if (overlay != null) {
                android.graphics.Matrix mx = new android.graphics.Matrix();
                mx.setScale((float) base.getWidth() / overlay.getWidth(),
                        (float) base.getHeight() / overlay.getHeight());
                canvas.drawBitmap(overlay, mx, null);
            }
            File out = new File(context.getCacheDir(), "merged_img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(out);
            merged.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.close();
            return out.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean hasAudioTrack(String path) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(path);
            return "yes".equals(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO));
        } catch (Exception e) {
            return false;
        } finally {
            try { mmr.release(); } catch (Exception ignored) {}
        }
    }

    private Bitmap captureView(View view) {
        if (view == null || view.getWidth() == 0) return null;
        try {
            Bitmap bmp = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            view.draw(new Canvas(bmp));
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    /** Cache folder mein unique file path banao */
    private String newCacheFile(String prefix, String ext) {
        return new File(context.getCacheDir(), prefix + System.currentTimeMillis() + ext)
                .getAbsolutePath();
    }

    /** File exist karti hai aur empty nahi hai */
    private boolean fileValid(String path) {
        if (path == null) return false;
        File f = new File(path);
        return f.exists() && f.length() > 1000;
    }

    /** List mein array elements add karo */
    private void addAll(java.util.List<String> list, String[] arr) {
        for (String s : arr) list.add(s);
    }

    private boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    public void cancel() {
        cancelled = true;
        callBackOfQuery.cancelProcess();
    }
    public void renderMultiple(
            List<MediaModel> mediaPaths,
            android.view.View overlayCanvas,
            String musicPath,
            long musicStartMs,
            long musicEndMs,
            int totalDurSec,
            RenderCallback callback) {

       executor.execute(() -> {
            try {
                List<String> allVideoClips = new ArrayList<>();
                int total = mediaPaths.size();

                // ── Step 1: Har media ko video clip mein convert karo ──────────────
                for (int i = 0; i < total; i++) {
                    MediaModel item = mediaPaths.get(i);
                    String path   = item.getPath();
                    boolean isImg = isImagePath(path);
                    int progress  = 5 + (i * 50) / total;

                    final int fi = i + 1;
                    mainHandler.post(() -> callback.onProgress(progress,
                            (isImg ? "Converting image " : "Processing video ")
                                    + fi + " of " + total + "..."));

                    String clipOut;
                    if (isImg) {
                        // Image → 5s video
                        clipOut = new ImageToVideoConverter(context).convertSync(path, 5);
                    } else {
                        // Video → copy to cache (filter/speed bhi yahan apply ho sakta hai)
                        clipOut = copyVideoToCache(path);
                    }

                    if (clipOut != null) {
                        allVideoClips.add(clipOut);
                        Log.d("RenderEngine", "Clip ready [" + fi + "]: " + clipOut);
                    } else {
                        Log.w("RenderEngine", "Clip skipped [" + fi + "]: " + path);
                    }
                }

                if (allVideoClips.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No valid clips to render"));
                    return;
                }

                mainHandler.post(() -> callback.onProgress(60,
                        "Merging " + allVideoClips.size() + " clips..."));

                // ── Step 2: Sab clips merge karo ──────────────────────────────────
                String merged = new MultiClipMerger(context).mergeSync(allVideoClips);
                if (merged == null) {
                    mainHandler.post(() -> callback.onError("Merge failed"));
                    return;
                }

                mainHandler.post(() -> callback.onProgress(80, "Mixing audio..."));

                // ── Step 3: Music mix karo ─────────────────────────────────────────
                String finalPath = merged;
                if (musicPath != null && !musicPath.isEmpty()) {
                    try {
                        String mixed = mixAudioTracks(merged, musicPath,
                                musicStartMs, musicEndMs);
                        if (mixed != null) finalPath = mixed;
                    } catch (Exception e) {
                        Log.e("RenderEngine", "Music mix failed (using no-music): " + e.getMessage());
                    }
                }

                mainHandler.post(() -> callback.onProgress(95, "Finalizing..."));

                // ── Step 4: Temp clips cleanup ────────────────────────────────────
                for (String clip : allVideoClips) {
                    try {
                        File f = new File(clip);
                        if (f.exists() && !clip.equals(finalPath)) f.delete();
                    } catch (Exception ignored) {}
                }

                final String output = finalPath;
                mainHandler.post(() -> {
                    callback.onProgress(100, "Done!");
                    callback.onComplete(output);
                });

            } catch (Exception e) {
                Log.e("RenderEngine", "renderMultiple failed: " + e.getMessage());
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ── Check image path ───────────────────────────────────────────────────────────
    private boolean isImagePath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.endsWith(".jpg")  || lower.endsWith(".jpeg") ||
                lower.endsWith(".png")  || lower.endsWith(".webp") ||
                lower.endsWith(".bmp")  || lower.endsWith(".gif");
    }

    // ── Video ko cache mein copy karo (content:// → absolute path) ───────────────
    private String copyVideoToCache(String path) {
        try {
            Uri uri = Uri.parse(path);
            if (!"content".equals(uri.getScheme())) return path; // already file path

            File dest = new File(context.getCacheDir(),
                    "vid_cache_" + System.currentTimeMillis() + ".mp4");
            try (java.io.InputStream in = context.getContentResolver().openInputStream(uri);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                if (in == null) return null;
                byte[] buf = new byte[64 * 1024];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return dest.getAbsolutePath();
        } catch (Exception e) {
            Log.e("RenderEngine", "copyVideoToCache: " + e.getMessage());
            return path; // fallback
        }
    }
    /**
     * Synchronous variant of mixAudio — blocks the calling thread until ffmpeg finishes.
     * Safe to call here because renderMultiple() already runs on a background executor.
     *
     * @return path to the new file with mixed audio
     * @throws Exception if the mix fails or produces an invalid file
     */
    private String mixAudioTracks(String videoPath, String musicPath,
                                  long startMs, long endMs) throws Exception {

        String outPath = newCacheFile("multi_mixed_", ".mp4");
        boolean videoHasAudio = hasAudioTrack(videoPath);
        String origVol = String.format(java.util.Locale.US, "volume=%.2f", originalVolume);
        String musicVol = String.format(java.util.Locale.US, "volume=%.2f", musicVolume);
        String atempo = buildAtempoFilter(speed);

        double startSec = startMs / 1000.0;
        double durSec = (endMs > startMs) ? (endMs - startMs) / 1000.0 : -1;
        boolean hasTrimMs = startMs > 0 || durSec > 0;

        List<String> cmd = new ArrayList<>();
        cmd.add("-i"); cmd.add(videoPath);
        if (hasTrimMs) {
            cmd.add("-ss"); cmd.add(String.format(java.util.Locale.US, "%.2f", startSec));
            if (durSec > 0) { cmd.add("-t"); cmd.add(String.format(java.util.Locale.US, "%.2f", durSec)); }
        }
        cmd.add("-i"); cmd.add(musicPath);

        String fc;
        if (videoHasAudio) {
            fc = String.format(
                    "[0:a]%s[a0];[1:a]%s%s[a1];[a0][a1]amix=inputs=2:duration=shortest:dropout_transition=2[aout]",
                    origVol, atempo.isEmpty() ? "" : atempo + ",", musicVol);
        } else {
            fc = String.format("[1:a]%s%s[aout]", atempo.isEmpty() ? "" : atempo + ",", musicVol);
        }

        cmd.add("-filter_complex"); cmd.add(fc);
        cmd.add("-map"); cmd.add("0:v");
        cmd.add("-map"); cmd.add("[aout]");
        cmd.add("-c:v"); cmd.add("copy");
        cmd.add("-c:a"); cmd.add("aac");
        cmd.add("-shortest");
        cmd.add("-y"); cmd.add(outPath);

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final boolean[] ok = {false};

        callBackOfQuery.callQuery(cmd.toArray(new String[0]), new FFmpegCallBack() {
            @Override public void process(LogMessage l) {}
            @Override public void statisticsProcess(Statistics s) {}
            @Override public void success() { ok[0] = fileValid(outPath); latch.countDown(); }
            @Override public void cancel() { latch.countDown(); }
            @Override public void failed() { latch.countDown(); }
        });

        latch.await(); // blocks here — fine, we're already on the background executor thread

        if (!ok[0]) {
            throw new Exception("Audio mix failed for multi-clip render");
        }
        return outPath;
    }
}