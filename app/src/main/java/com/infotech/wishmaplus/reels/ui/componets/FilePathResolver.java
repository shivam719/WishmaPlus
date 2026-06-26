package com.infotech.wishmaplus.reels.ui.componets;


import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
/**
 * content:// URI ko real file path mein convert karta hai
 * FFmpeg sirf real path accept karta hai, content:// nahi
 */
public class FilePathResolver {

    private static final String TAG = "FilePathResolver";

    // =========================================================================
    // MAIN METHOD — yahi call karo
    // =========================================================================

    /**
     * content:// ya file:// URI ko real absolute path mein convert karta hai.
     * Agar path nahi milta toh file copy karke cache mein deta hai.
     *
     * @param context  Application context
     * @param uri      content:// URI (MediaStore se mila hua)
     * @return         Real file path string jaise /storage/emulated/0/Music/song.mp3
     *                 null agar kuch bhi kaam nahi aaya
     */
    public static String getRealPath(Context context, Uri uri) {
        if (uri == null) return null;

        String scheme = uri.getScheme();

        // ── Already file path hai ─────────────────────────────────────────────
        if ("file".equalsIgnoreCase(scheme)) {
            return uri.getPath();
        }

        // ── Content URI ───────────────────────────────────────────────────────
        if ("content".equalsIgnoreCase(scheme)) {

            // Android 4.4+ DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String auth  = uri.getAuthority();

                if ("com.android.externalstorage.documents".equals(auth)) {
                    // External storage
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if ("com.android.providers.downloads.documents".equals(auth)) {
                    // Downloads folder
                    try {
                        Uri contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                Long.parseLong(docId));
                        return queryMediaStore(context, contentUri,
                                MediaStore.MediaColumns.DATA);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Downloads doc id parse error", e);
                    }
                } else if ("com.android.providers.media.documents".equals(auth)) {
                    // MediaStore document
                    String[] split = docId.split(":");
                    String mediaType = split[0];
                    Uri mediaUri;
                    if ("image".equals(mediaType)) {
                        mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(mediaType)) {
                        mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(mediaType)) {
                        mediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    } else {
                        return null;
                    }
                    String selection = "_id=?";
                    String[] selectionArgs = new String[]{split[1]};
                    return queryMediaStore(context, mediaUri,
                            MediaStore.MediaColumns.DATA, selection, selectionArgs);
                }
            }

            // ── Normal MediaStore content URI ─────────────────────────────────
            // media/external/audio/media/123 type URIs
            String path = queryMediaStore(context, uri, MediaStore.MediaColumns.DATA);
            if (path != null && new File(path).exists()) {
                return path;
            }

            // ── Last resort: Stream copy to cache ─────────────────────────────
            // Kuch devices pe DATA column null aata hai — file cache mein copy karo
            return copyToCacheFile(context, uri);
        }

        return null;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** MediaStore se DATA column query karta hai */
    private static String queryMediaStore(Context context, Uri uri, String column) {
        return queryMediaStore(context, uri, column, null, null);
    }

    private static String queryMediaStore(Context context, Uri uri,
                                          String column,
                                          String selection,
                                          String[] selectionArgs) {
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{column}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaStore query error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Jab real path nahi milta — file ko cache directory mein copy karta hai.
     * FFmpeg cache path se perfectly kaam karta hai.
     */
    private static String copyToCacheFile(Context context, Uri uri) {
        try {
            // File name nikalo
            String fileName = "audio_" + System.currentTimeMillis() + ".mp3";

            // Cache file banao
            File cacheFile = new File(context.getCacheDir(), fileName);

            // Stream se copy karo
            try (InputStream in  = context.getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(cacheFile)) {

                if (in == null) return null;

                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }

            Log.d(TAG, "Copied to cache: " + cacheFile.getAbsolutePath());
            return cacheFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Cache copy error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cache mein copy kiye gaye audio files clean karo
     * (optional — app start pe ya merge ke baad call karo)
     */
    public static void clearAudioCache(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files  = cacheDir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.getName().startsWith("audio_")) {
                f.delete();
            }
        }
    }
}
