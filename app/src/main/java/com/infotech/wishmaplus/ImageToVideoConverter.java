package com.infotech.wishmaplus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

public class ImageToVideoConverter {

    private static final String TAG        = "ImgToVideo";
    private static final String MIME_TYPE  = "video/avc";
    private static final int    WIDTH      = 1080;
    private static final int    HEIGHT     = 1920;
    private static final int    BIT_RATE   = 4_000_000;
    private static final int    FRAME_RATE = 30;
    private static final int    TIMEOUT_US = 10_000;

    private final Context context;

    public interface ConvertCallback {
        void onProgress(int percent, String msg);
        void onComplete(String outputPath);
        void onError(String error);
    }

    public ImageToVideoConverter(Context context) {
        this.context = context;
    }

    // ── Single image → MP4 (blocking, call from background thread) ────────────
    public String convertSync(String imagePath, int durationSec) {
        try {
            String out = new File(context.getCacheDir(),
                    "img2vid_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();

            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE,        BIT_RATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE,      FRAME_RATE);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

            MediaCodec encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface inputSurface = encoder.createInputSurface();
            encoder.start();

            MediaMuxer muxer = new MediaMuxer(out,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            Bitmap bmp = loadAndScale(imagePath, WIDTH, HEIGHT);
            if (bmp == null) throw new Exception("Bitmap null: " + imagePath);

            int totalFrames   = durationSec * FRAME_RATE;
            int muxTrack      = -1;
            boolean muxStarted = false;
            long ptsUs        = 0;

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            for (int f = 0; f < totalFrames; f++) {
                // Draw frame to surface
                drawToSurface(inputSurface, bmp);
                ptsUs = (long) f * 1_000_000L / FRAME_RATE;

                // Drain encoder
                drain:
                while (true) {
                    int idx = encoder.dequeueOutputBuffer(info, TIMEOUT_US);
                    if (idx == MediaCodec.INFO_TRY_AGAIN_LATER)  break;
                    if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxStarted) {
                            muxTrack = muxer.addTrack(encoder.getOutputFormat());
                            muxer.start();
                            muxStarted = true;
                        }
                        continue;
                    }
                    if (idx >= 0) {
                        ByteBuffer buf = encoder.getOutputBuffer(idx);
                        if (buf != null && info.size > 0 && muxStarted) {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                info.presentationTimeUs = ptsUs;
                                buf.position(info.offset);
                                buf.limit(info.offset + info.size);
                                muxer.writeSampleData(muxTrack, buf, info);
                            }
                        }
                        boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        encoder.releaseOutputBuffer(idx, false);
                        if (eos) break drain;
                    }
                }
            }

            // Signal EOS
            encoder.signalEndOfInputStream();
            boolean eos = false;
            while (!eos) {
                int idx = encoder.dequeueOutputBuffer(info, TIMEOUT_US);
                if (idx >= 0) {
                    ByteBuffer buf = encoder.getOutputBuffer(idx);
                    if (buf != null && info.size > 0 && muxStarted
                            && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        info.presentationTimeUs = ptsUs + 1_000_000L / FRAME_RATE;
                        buf.position(info.offset);
                        buf.limit(info.offset + info.size);
                        muxer.writeSampleData(muxTrack, buf, info);
                    }
                    eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    encoder.releaseOutputBuffer(idx, false);
                }
            }

            bmp.recycle();
            encoder.stop();
            encoder.release();
            inputSurface.release();
            if (muxStarted) { muxer.stop(); muxer.release(); }
            else muxer.release();

            Log.d(TAG, "Image → video: " + out);
            return out;

        } catch (Exception e) {
            Log.e(TAG, "convertSync failed: " + e.getMessage());
            return null;
        }
    }

    // ── Draw bitmap to encoder surface ─────────────────────────────────────────
    private void drawToSurface(Surface surface, Bitmap bmp) {
        Canvas canvas = null;
        try {
            canvas = surface.lockHardwareCanvas();
        } catch (Exception e) {
            try { canvas = surface.lockCanvas(null); } catch (Exception ignored) {}
        }
        if (canvas != null) {
            try {
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(bmp, 0, 0, null);
            } finally {
                surface.unlockCanvasAndPost(canvas);
            }
        }
    }

    // ── Load + letterbox scale ──────────────────────────────────────────────────
    public static Bitmap loadAndScale(String path, int targetW, int targetH) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, opts);

            int sample = 1;
            while ((opts.outWidth  / (sample * 2)) >= targetW &&
                    (opts.outHeight / (sample * 2)) >= targetH) sample *= 2;

            opts.inSampleSize     = sample;
            opts.inJustDecodeBounds = false;
            opts.inPreferredConfig  = Bitmap.Config.ARGB_8888;
            Bitmap raw = BitmapFactory.decodeFile(path, opts);
            if (raw == null) return null;

            Bitmap result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            canvas.drawColor(Color.BLACK);

            float srcAr = (float) raw.getWidth()  / raw.getHeight();
            float dstAr = (float) targetW / targetH;
            int dW, dH, left, top;
            if (srcAr > dstAr) {
                dW = targetW; dH = (int)(targetW / srcAr);
                left = 0;    top = (targetH - dH) / 2;
            } else {
                dH = targetH; dW = (int)(targetH * srcAr);
                left = (targetW - dW) / 2; top = 0;
            }
            canvas.drawBitmap(raw,
                    new android.graphics.Rect(0, 0, raw.getWidth(), raw.getHeight()),
                    new android.graphics.Rect(left, top, left+dW, top+dH), null);
            raw.recycle();
            return result;
        } catch (Exception e) {
            Log.e(TAG, "loadAndScale: " + e.getMessage());
            return null;
        }
    }
}