package com.infotech.wishmaplus.reels.ui.componets;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.widget.ImageView;

import java.util.Objects;

/**
 * FilterEngine — applies colour filters as ColorMatrix (for preview)
 * and exposes the same values so ReelRenderEngine can pass them to FFmpeg eq=.
 * <p>
 * KEY FIX: The ColorMatrix values now exactly match what FFmpeg eq= filter
 * does, so preview and exported video look identical.
 * <p>
 * FFmpeg eq= parameter ranges:
 * brightness: -1.0 .. +1.0   (0 = neutral)
 * contrast:    0.0 .. 2.0+   (1 = neutral)  ← NOT -1/0/+1
 * saturation:  0.0 .. 3.0    (1 = neutral)
 * <p>
 * Our FilterState uses the same ranges, so no remapping is needed
 * between preview and export.
 */
public class FilterEngine {

    public enum Preset {
        ORIGINAL, VIVID, WARM, COOL, FADE, NOIR, DRAMA,
        CHROME, VINTAGE, MATTE, NEON, PASTEL
    }

    public static class FilterState {
        public Preset preset = Preset.ORIGINAL;
        public float brightness = 0f;    // -1 .. +1,  0 = neutral
        public float contrast = 1f;    //  0 .. 3,   1 = neutral
        public float saturation = 1f;    //  0 .. 3,   1 = neutral
        public float warmth = 0f;    // -1 .. +1,  0 = neutral
    }

    // ── Apply to ImageView (live preview) ─────────────────────────────────────
    public static void apply(ImageView view, FilterState state) {
        ColorMatrix cm = buildMatrix(state);
        view.setColorFilter(new ColorMatrixColorFilter(cm));
    }

    // ── Apply to Bitmap (for thumbnails and image export) ─────────────────────
    public static Bitmap applyToBitmap(Bitmap src, FilterState state) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
                Objects.requireNonNull(src.getConfig()));
        Canvas c = new Canvas(out);
        Paint p = new Paint();
        p.setColorFilter(new ColorMatrixColorFilter(buildMatrix(state)));
        c.drawBitmap(src, 0, 0, p);
        return out;
    }

    // ── Build ColorMatrix from FilterState ────────────────────────────────────
    private static ColorMatrix buildMatrix(FilterState s) {
        ColorMatrix result = new ColorMatrix();

        // 1. Start with preset base
        applyPreset(result, s.preset);

        // 2. Saturation (same as FFmpeg saturation: 0=greyscale, 1=normal, 3=vivid)
        ColorMatrix sat = new ColorMatrix();
        sat.setSaturation(s.saturation);
        result.postConcat(sat);

        // 3. Contrast — KEY FIX: scale around 0.5 (mid-grey)
        //    This matches FFmpeg eq=contrast where neutral=1 means no change.
        //    Our contrast: 0..3 where 1=neutral.
        //    ColorMatrix: scale by c, translate to keep mid-grey stable.
        float c = s.contrast;
        float t = (1f - c) * 0.5f; // offset to keep midpoint stable
        ColorMatrix contrast = new ColorMatrix(new float[]{
                c, 0, 0, 0, t * 255,
                0, c, 0, 0, t * 255,
                0, 0, c, 0, t * 255,
                0, 0, 0, 1, 0
        });
        result.postConcat(contrast);

        // 4. Brightness — additive offset (same as FFmpeg: -1..+1 maps to -255..+255)
        float b = s.brightness * 255f;
        ColorMatrix bright = new ColorMatrix(new float[]{
                1, 0, 0, 0, b,
                0, 1, 0, 0, b,
                0, 0, 1, 0, b,
                0, 0, 0, 1, 0
        });
        result.postConcat(bright);

        // 5. Warmth — shift red/blue channels (same as FFmpeg colorchannelmixer)
        float w = s.warmth * 0.3f; // scale factor
        ColorMatrix warmth = new ColorMatrix(new float[]{
                1 + w, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1 - w, 0, 0,
                0, 0, 0, 1, 0
        });
        result.postConcat(warmth);

        return result;
    }

    // ── Preset matrices ────────────────────────────────────────────────────────
    private static void applyPreset(ColorMatrix cm, Preset preset) {
        switch (preset) {
            case VIVID:
                cm.setSaturation(1.8f);
                break;

            case WARM:
                cm.set(new float[]{
                        1.15f, 0, 0, 0, 20,
                        0, 1.05f, 0, 0, 5,
                        0, 0, 0.85f, 0, -15,
                        0, 0, 0, 1, 0
                });
                break;

            case COOL:
                cm.set(new float[]{
                        0.85f, 0, 0, 0, -15,
                        0, 1.0f, 0, 0, 0,
                        0, 0, 1.2f, 0, 25,
                        0, 0, 0, 1, 0
                });
                break;

            case FADE:
                cm.set(new float[]{
                        0.78f, 0, 0, 0, 35,
                        0, 0.78f, 0, 0, 35,
                        0, 0, 0.78f, 0, 35,
                        0, 0, 0, 1, 0
                });
                break;

            case NOIR:
                cm.setSaturation(0f);
                break;

            case DRAMA: {
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(1.6f);
                ColorMatrix con = new ColorMatrix(new float[]{
                        1.35f, 0, 0, 0, -40,
                        0, 1.35f, 0, 0, -40,
                        0, 0, 1.35f, 0, -40,
                        0, 0, 0, 1, 0
                });
                cm.set(sat);
                cm.postConcat(con);
                break;
            }

            case CHROME:
                cm.set(new float[]{
                        1.05f, 0, 0, 0, 12,
                        0, 0.95f, 0, 0, 0,
                        0, 0, 0.88f, 0, -12,
                        0, 0, 0, 1, 0
                });
                break;

            case VINTAGE: {
                ColorMatrix s2 = new ColorMatrix();
                s2.setSaturation(0.65f);
                ColorMatrix warm = new ColorMatrix(new float[]{
                        1.15f, 0, 0, 0, 25,
                        0, 1.0f, 0, 0, 8,
                        0, 0, 0.75f, 0, -12,
                        0, 0, 0, 1, 0
                });
                cm.set(s2);
                cm.postConcat(warm);
                break;
            }

            case MATTE:
                cm.set(new float[]{
                        0.72f, 0, 0, 0, 45,
                        0, 0.72f, 0, 0, 45,
                        0, 0, 0.72f, 0, 45,
                        0, 0, 0, 1, 0
                });
                break;

            case NEON: {
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(2.6f);
                ColorMatrix bright = new ColorMatrix(new float[]{
                        1.12f, 0, 0, 0, 22,
                        0, 1.12f, 0, 0, 22,
                        0, 0, 1.12f, 0, 22,
                        0, 0, 0, 1, 0
                });
                cm.set(sat);
                cm.postConcat(bright);
                break;
            }

            case PASTEL: {
                ColorMatrix sat = new ColorMatrix();
                sat.setSaturation(0.45f);
                ColorMatrix light = new ColorMatrix(new float[]{
                        0.88f, 0, 0, 0, 55,
                        0, 0.88f, 0, 0, 55,
                        0, 0, 0.88f, 0, 55,
                        0, 0, 0, 1, 0
                });
                cm.set(sat);
                cm.postConcat(light);
                break;
            }

            default: // ORIGINAL — identity matrix
                cm.reset();
                break;
        }
    }
}