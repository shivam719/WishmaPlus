package com;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;

public class MultiClipMerger {

    private static final String TAG = "MultiClipMerger";
    private final Context context;

    public MultiClipMerger(Context context) {
        this.context = context;
    }

    // ── Blocking merge — call from background thread ────────────────────────────
    public String mergeSync(List<String> clipPaths) {
        if (clipPaths == null || clipPaths.isEmpty()) return null;
        if (clipPaths.size() == 1) return clipPaths.get(0);

        String outPath = new File(context.getCacheDir(),
                "merged_" + System.currentTimeMillis() + ".mp4").getAbsolutePath();
        MediaMuxer muxer = null;

        try {
            muxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            boolean muxStarted  = false;
            int     muxVidTrack = -1;
            int     muxAudTrack = -1;
            long    timeOffUs   = 0;

            for (String clipPath : clipPaths) {
                if (clipPath == null) continue;

                MediaExtractor ex = new MediaExtractor();
                try {
                    Uri uri = Uri.parse(clipPath);
                    if ("content".equals(uri.getScheme()))
                        ex.setDataSource(context, uri, null);
                    else
                        ex.setDataSource(clipPath);
                } catch (Exception e) {
                    Log.e(TAG, "Extractor failed: " + clipPath + " → " + e.getMessage());
                    ex.release();
                    continue;
                }

                // ── Discover tracks ────────────────────────────
                int    srcVid = -1, srcAud = -1;
                long   clipDurUs = 0;
                MediaFormat vidFmt = null, audFmt = null;

                for (int i = 0; i < ex.getTrackCount(); i++) {
                    MediaFormat fmt  = ex.getTrackFormat(i);
                    String      mime = fmt.getString(MediaFormat.KEY_MIME);
                    if (mime == null) continue;
                    if (mime.startsWith("video/") && srcVid < 0) {
                        srcVid = i; vidFmt = fmt;
                        if (fmt.containsKey(MediaFormat.KEY_DURATION))
                            clipDurUs = fmt.getLong(MediaFormat.KEY_DURATION);
                    } else if (mime.startsWith("audio/") && srcAud < 0) {
                        srcAud = i; audFmt = fmt;
                        if (clipDurUs == 0 && fmt.containsKey(MediaFormat.KEY_DURATION))
                            clipDurUs = fmt.getLong(MediaFormat.KEY_DURATION);
                    }
                }

                if (srcVid < 0) {
                    Log.w(TAG, "No video track in: " + clipPath);
                    ex.release();
                    continue;
                }

                // ── Add tracks to muxer (first clip only) ──────
                if (!muxStarted) {
                    muxVidTrack = muxer.addTrack(vidFmt);
                    if (audFmt != null) muxAudTrack = muxer.addTrack(audFmt);
                    muxer.start();
                    muxStarted = true;
                }

                ex.selectTrack(srcVid);
                if (srcAud >= 0 && muxAudTrack >= 0) ex.selectTrack(srcAud);

                // ── Copy samples ───────────────────────────────
                ByteBuffer           buf  = ByteBuffer.allocate(1024 * 1024);
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                while (true) {
                    info.size = ex.readSampleData(buf, 0);
                    if (info.size < 0) break;

                    int    srcTrack = ex.getSampleTrackIndex();
                    int    dstTrack = (srcTrack == srcVid) ? muxVidTrack
                            : (srcTrack == srcAud && muxAudTrack >= 0) ? muxAudTrack : -1;
                    if (dstTrack >= 0) {
                        info.offset             = 0;
                        info.presentationTimeUs = ex.getSampleTime() + timeOffUs;
                        info.flags              = (ex.getSampleFlags()
                                & MediaExtractor.SAMPLE_FLAG_SYNC) != 0
                                ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0;
                        muxer.writeSampleData(dstTrack, buf, info);
                    }
                    ex.advance();
                }

                // ── Advance time offset ────────────────────────
                timeOffUs += (clipDurUs > 0) ? clipDurUs : 5_000_000L;
                ex.release();

                Log.d(TAG, "Merged clip: " + clipPath
                        + "  offset=" + timeOffUs / 1_000_000 + "s");
            }

            if (!muxStarted) { muxer.release(); return null; }
            muxer.stop();
            muxer.release();
            Log.d(TAG, "Final merged: " + outPath);
            return outPath;

        } catch (Exception e) {
            Log.e(TAG, "mergeSync failed: " + e.getMessage());
            try { if (muxer != null) muxer.release(); } catch (Exception ignored) {}
            return null;
        }
    }
}