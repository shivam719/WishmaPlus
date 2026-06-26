package com.infotech.wishmaplus;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

public class GlideLoadHelper {

    // Common grey placeholder
    private static final ColorDrawable PLACEHOLDER = new ColorDrawable(0xFFEEEEEE);

    // ─── Simple load - no loader needed (profile pics, small images) ────────────
    public static void loadImage(Context context, String url, ImageView imageView, RequestOptions options) {
        Glide.with(context).load(url).placeholder(PLACEHOLDER).apply(options).transition(DrawableTransitionOptions.withCrossFade(200)).into(imageView);
    }

    // ─── Load with ProgressBar loader (post images, cover photos) ───────────────
    public static void loadWithProgress(Context context, String url, ImageView imageView, ProgressBar progressBar) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        imageView.setImageDrawable(PLACEHOLDER);

        Glide.with(context).load(url).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                return false;
            }
        }).transition(DrawableTransitionOptions.withCrossFade(200)).into(imageView);
    }

    // ─── Load with circle crop (profile images) ──────────────────────────────────
    public static void loadCircle(Context context, String url, ImageView imageView) {
        Glide.with(context).load(url).placeholder(PLACEHOLDER).circleCrop().transition(DrawableTransitionOptions.withCrossFade(200)).into(imageView);
    }

    // ─── Load with circle crop + ProgressBar ─────────────────────────────────────
    public static void loadCircleWithProgress(Context context, String url, ImageView imageView, ProgressBar progressBar) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        imageView.setImageDrawable(PLACEHOLDER);

        Glide.with(context).load(url).circleCrop().listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                return false;
            }
        }).transition(DrawableTransitionOptions.withCrossFade(200)).into(imageView);
    }
    // ─── Load with Lottie loader ──────────────────────────────────────────────────
    public static void loadWithLottie(Context context, String url, ImageView imageView,
                                      LottieAnimationView lottieView) {
        if (lottieView != null) {
            lottieView.setVisibility(View.VISIBLE);
            lottieView.playAnimation();
        }
        imageView.setImageDrawable(PLACEHOLDER);

        Glide.with(context)
                .load(url)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        if (lottieView != null) {
                            lottieView.cancelAnimation();
                            lottieView.setVisibility(View.GONE);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        if (lottieView != null) {
                            lottieView.cancelAnimation();
                            lottieView.setVisibility(View.GONE);
                        }
                        return false;
                    }
                })
                .transition(DrawableTransitionOptions.withCrossFade(200))
                .into(imageView);
    }
}