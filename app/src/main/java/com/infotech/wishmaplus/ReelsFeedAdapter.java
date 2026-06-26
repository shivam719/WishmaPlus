package com.infotech.wishmaplus;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.infotech.wishmaplus.Utils.CustomLoader;
import com.infotech.wishmaplus.Utils.UtilMethods;
import com.infotech.wishmaplus.reels.ReelWatchTracker.ReelWatchTracker;
import com.infotech.wishmaplus.reels.reels_comments.CommentsBottomSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReelsFeedAdapter extends RecyclerView.Adapter<ReelsFeedAdapter.ReelVH> {

    private final Context context;
    private final List<ReelModel> reels;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SparseArray<ExoPlayer> playerPool = new SparseArray<>();
    public int currentPlayingPosition = -1;
    private boolean isMuted = false;
    private CustomLoader customLoader;

    public ReelsFeedAdapter(Context context, List<ReelModel> reels, CustomLoader loader) {
        this.context = context;
        this.reels = reels;
        this.customLoader = loader;
    }

    // ══════════════════════════════════════════════════════════════════════
    // ★ NEW — Same userId ke saare reels ka following state ek saath sync karo
    // Jab bhi follow/unfollow ho, yeh method call karo
    // ══════════════════════════════════════════════════════════════════════
    private void syncFollowStateForUser(String userId, boolean isFollowing) {
        for (int i = 0; i < reels.size(); i++) {
            ReelModel r = reels.get(i);
            if (r.getUserId().equals(userId) && r.isFollowing() != isFollowing) {
                r.setFollowing(isFollowing);
                notifyItemChanged(i, "FOLLOW_UPDATE"); // ★ payload se sirf btnFollow refresh hoga
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // ★ NEW — Partial bind: sirf FOLLOW_UPDATE payload aane par btnFollow update karo
    //         Baki sab (player, thumbnail, etc.) touch nahi hoga — smooth experience
    // ══════════════════════════════════════════════════════════════════════
    @Override
    public void onBindViewHolder(@NonNull ReelVH holder, int position,
                                 @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && "FOLLOW_UPDATE".equals(payloads.get(0))) {
            // ★ Sirf follow button update — player/thumbnail skip
            ReelModel reel = reels.get(position);
            holder.updateFollowButton(reel);
        } else {
            // Normal full bind
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    // ══════════════════════════════════════════════════════════════════════

    public static void setCaptionWithSeeMore(TextView textView, String caption, String hashtags) {
        StringBuilder finalText = new StringBuilder();
        if (caption != null && !caption.trim().isEmpty()) {
            finalText.append(caption.trim());
        }
        if (hashtags != null && !hashtags.trim().isEmpty()) {
            String[] tags = hashtags.split(",");
            for (String tag : tags) {
                tag = tag.trim();
                if (!tag.isEmpty()) finalText.append(" #").append(tag);
            }
        }
        String fullText = finalText.toString();
        textView.post(() -> {
            if (textView.getLineCount() <= 2) {
                applyHashtagStyle(textView, fullText, false);
            } else {
                makeCollapsedText(textView, fullText);
            }
        });
    }

    private static void applyHashtagStyle(TextView textView, CharSequence text, boolean expanded) {
        SpannableString spannable = new SpannableString(text);
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            final String tag = text.subSequence(start, end).toString();
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Toast.makeText(widget.getContext(), "Clicked " + tag, Toast.LENGTH_SHORT).show();
                }
                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setColor(Color.WHITE);
                    ds.setUnderlineText(false);
                    ds.setFakeBoldText(true);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setText(spannable);
        if (expanded) textView.setMaxLines(Integer.MAX_VALUE);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private static void makeCollapsedText(TextView textView, String fullText) {
        textView.setMaxLines(2);
        String seeMore = " See More";
        SpannableString spannable = new SpannableString(fullText + seeMore);
        int start = fullText.length();
        int end = spannable.length();
        spannable.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                applyHashtagStyle(textView, fullText + " See Less", true);
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(Color.LTGRAY);
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        applyHashtagStyle(textView, spannable, false);
    }

    @NonNull
    @Override
    public ReelVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_reel_feed, parent, false);
        return new ReelVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReelVH holder, int position) {
        holder.bind(reels.get(position), position);
    }

    @Override
    public void onViewRecycled(@NonNull ReelVH holder) {
        super.onViewRecycled(holder);
        holder.releasePlayer();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ReelVH holder) {
        super.onViewDetachedFromWindow(holder);
        holder.pausePlayer();
        int pos = holder.getBindingAdapterPosition();
        if (pos >= 0) {
            ExoPlayer p = playerPool.get(pos);
            if (p != null) p.setPlayWhenReady(false);
        }
    }

    public void playPosition(int position) {
        try {
            ReelWatchTracker.getInstance().onReelScrolledAway();
            for (int i = 0; i < playerPool.size(); i++) {
                ExoPlayer p = playerPool.valueAt(i);
                if (p != null && playerPool.keyAt(i) != position) {
                    p.setPlayWhenReady(false);
                    p.pause();
                }
            }
            currentPlayingPosition = position;
            ExoPlayer current = playerPool.get(position);
            if (current != null && current.getPlaybackState() != Player.STATE_ENDED) {
                current.setPlayWhenReady(true);
                current.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshItem(int position) {
        if (position >= 0 && position < reels.size()) notifyItemChanged(position);
    }

    public void pauseAll() {
        for (int i = 0; i < playerPool.size(); i++) {
            try {
                ExoPlayer p = playerPool.valueAt(i);
                if (p != null && p.isPlaying()) p.pause();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        handler.removeCallbacksAndMessages(null);
    }

    public void resumeCurrent() {
        if (currentPlayingPosition < 0) return;
        ExoPlayer p = playerPool.get(currentPlayingPosition);
        if (p != null && p.getPlaybackState() != Player.STATE_ENDED && !p.isPlaying()) {
            p.play();
        }
    }

    public void releaseAll() {
        handler.removeCallbacksAndMessages(null);
        for (int i = 0; i < playerPool.size(); i++) {
            try {
                ExoPlayer p = playerPool.valueAt(i);
                if (p != null) {
                    p.stop();
                    p.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        playerPool.clear();
        currentPlayingPosition = -1;
    }

    @Override
    public int getItemCount() {
        return reels.size();
    }

    // ══════════════════════════════════════════════════════════════════════
    public class ReelVH extends RecyclerView.ViewHolder {

        private static final long DOUBLE_TAP_MS = 280;

        PlayerView playerView;
        ImageView thumbnail, playPauseOverlay, heartOverlay;
        ImageView btnLike, btnComment, btnShare, btnBookmark, btnMore, btnMute;
        ImageView authorAvatar, btnSeekBack, btnSeekForward;
        FrameLayout touchLayer, seekBackZone, seekForwardZone;
        TextView authorName, descriptionTxt, btnFollow;
        TextView likeCount, commentCount, musicName;
        TextView tvCurrentTime, tvTotalTime;
        ImageView musicDisc;
        ProgressBar progress;
        SeekBar videoSeekBar;
        private View dimOverlay;

        private ExoPlayer exoPlayer;
        private boolean isViewCounted = false;
        private long videoStartTime = 0;
        private boolean isUserSeeking = false;
        private boolean isEnded = false;
        private long lastTapTime = 0;
        private Runnable singleTapRunnable = null;
        private ObjectAnimator discAnimator = null;

        // ── Progress runnable ─────────────────────────────────────────────
        private final Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && exoPlayer.isPlaying() && !isUserSeeking) {
                    long dur = exoPlayer.getDuration();
                    long curr = exoPlayer.getCurrentPosition();
                    if (dur > 0) {
                        videoSeekBar.setProgress((int) ((curr / (float) dur) * 1000));
                        tvCurrentTime.setText(formatTime(curr));
                        if (!isViewCounted && curr >= dur * 0.8) {
                            isViewCounted = true;
                            int sec = (int) ((System.currentTimeMillis() - videoStartTime) / 1000);
                            int pos = getBindingAdapterPosition();
                            if (pos >= 0) callAddReelView(reels.get(pos).getReelId(), sec);
                        }
                    }
                }
                handler.postDelayed(this, 200);
            }
        };

        ReelVH(@NonNull View v) {
            super(v);
            playerView       = v.findViewById(R.id.reelPlayerView);
            dimOverlay       = v.findViewById(R.id.dimOverlay);
            thumbnail        = v.findViewById(R.id.reelThumbnail);
            progress         = v.findViewById(R.id.reelLoadingBar);
            videoSeekBar     = v.findViewById(R.id.videoSeekBar);
            playPauseOverlay = v.findViewById(R.id.playPauseOverlay);
            heartOverlay     = v.findViewById(R.id.heartOverlay);
            touchLayer       = v.findViewById(R.id.touchLayer);
            seekBackZone     = v.findViewById(R.id.seekBackZone);
            seekForwardZone  = v.findViewById(R.id.seekForwardZone);
            btnSeekBack      = v.findViewById(R.id.btnSeekBack);
            btnSeekForward   = v.findViewById(R.id.btnSeekForward);
            authorAvatar     = v.findViewById(R.id.authorAvatar);
            authorName       = v.findViewById(R.id.authorName);
            descriptionTxt   = v.findViewById(R.id.descriptionTxt);
            btnFollow        = v.findViewById(R.id.btnFollow);
            btnLike          = v.findViewById(R.id.btnLike);
            likeCount        = v.findViewById(R.id.likeCount);
            btnComment       = v.findViewById(R.id.btnComment);
            commentCount     = v.findViewById(R.id.commentCount);
            btnShare         = v.findViewById(R.id.btnShare);
            btnBookmark      = v.findViewById(R.id.btnBookmark);
            btnMore          = v.findViewById(R.id.btnMore);
            btnMute          = v.findViewById(R.id.btnMute);
            musicDisc        = v.findViewById(R.id.musicDisc);
            musicName        = v.findViewById(R.id.musicName);
            tvCurrentTime    = v.findViewById(R.id.tvCurrentTime);
            tvTotalTime      = v.findViewById(R.id.tvTotalTime);
        }

        // ══════════════════════════════════════════════════════════════════
        // ★ NEW — Sirf follow button ko update karo (partial bind ke liye)
        // ══════════════════════════════════════════════════════════════════
        void updateFollowButton(ReelModel reel) {
            if (context instanceof ReelsFeedActivity activity) {
                if (activity.isMyReel || reel.isMyReel()) {
                    btnFollow.setVisibility(View.GONE);
                    return;
                }
            }
            btnFollow.setVisibility(View.VISIBLE);
            btnFollow.setText(reel.isFollowing() ? "Following" : "Follow");
        }

        @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
        void bind(ReelModel reel, int position) {
            isViewCounted = false;
            videoStartTime = 0;
            isEnded = false;

            // ── Author info ───────────────────────────────────────────────
            authorName.setText(reel.getFullName());
            setCaptionWithSeeMore(descriptionTxt, reel.getCaption(), reel.getHashtags());
            Glide.with(context)
                    .load(reel.getProfilePictureUrl())
                    .transform(new CircleCrop())
                    .placeholder(R.drawable.circle_background)
                    .into(authorAvatar);

            // ── Thumbnail ─────────────────────────────────────────────────
            thumbnail.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(reel.getThumbnailUrl() != null ? reel.getThumbnailUrl() : reel.getVideoUrl())
                    .centerCrop()
                    .into(thumbnail);

            // ── Counters / state ──────────────────────────────────────────
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            videoSeekBar.setProgress(0);
            likeCount.setText(formatCount(reel.getLikeCount()));
            commentCount.setText(formatCount(reel.getCommentCount()));
            updateLikeState(reel);
            startDiscSpin();

            // ── Follow button (initial state) ─────────────────────────────
            updateFollowButton(reel); // ★ reuse the partial-bind method

            // ── Player ────────────────────────────────────────────────────
            setupExoPlayer(reel, position);

            // ── Touch handler ─────────────────────────────────────────────
            touchLayer.setOnTouchListener((v, event) -> {
                if (event.getAction() != MotionEvent.ACTION_UP) return true;
                long now = System.currentTimeMillis();
                if (now - lastTapTime < DOUBLE_TAP_MS) {
                    handler.removeCallbacks(singleTapRunnable);
                    lastTapTime = 0;
                    if (!reel.getLiked()) doLikeApi(reel);
                    showHeartAnimation();
                } else {
                    lastTapTime = now;
                    singleTapRunnable = this::togglePlayPause;
                    handler.postDelayed(singleTapRunnable, DOUBLE_TAP_MS);
                }
                return true;
            });

            // ── Seek zones ────────────────────────────────────────────────
            seekBackZone.setOnClickListener(v -> {
                if (singleTapRunnable != null) { handler.removeCallbacks(singleTapRunnable); singleTapRunnable = null; }
                lastTapTime = 0;
                if (exoPlayer != null) {
                    exoPlayer.seekTo(Math.max(0, exoPlayer.getCurrentPosition() - 5000));
                    flashSeek(btnSeekBack);
                }
            });

            seekForwardZone.setOnClickListener(v -> {
                if (singleTapRunnable != null) { handler.removeCallbacks(singleTapRunnable); singleTapRunnable = null; }
                lastTapTime = 0;
                if (exoPlayer != null) {
                    long dur = exoPlayer.getDuration();
                    long pos = exoPlayer.getCurrentPosition() + 5000;
                    if (dur > 0) pos = Math.min(pos, dur);
                    exoPlayer.seekTo(pos);
                    flashSeek(btnSeekForward);
                }
            });

            // ── SeekBar ───────────────────────────────────────────────────
            videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
                    if (fromUser && exoPlayer != null) {
                        long dur = exoPlayer.getDuration();
                        if (dur > 0) {
                            long seekTo = (long) ((prog / 1000f) * dur);
                            exoPlayer.seekTo(seekTo);
                            tvCurrentTime.setText(formatTime(seekTo));
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
                @Override public void onStopTrackingTouch(SeekBar sb)  { isUserSeeking = false; }
            });

            // ── Like ──────────────────────────────────────────────────────
            btnLike.setOnClickListener(v -> {
                animateBounce(btnLike);
                doLikeApi(reel);
            });

            // ── Follow ────────────────────────────────────────────────────
            btnFollow.setOnClickListener(v -> {
                animateBounce(btnFollow);
                if (!(context instanceof Activity)) return;

                // ★ Optimistic UI update — button turant badlo, API baad mein
                boolean willFollow = !reel.isFollowing();
                reel.setFollowing(willFollow);
                updateFollowButton(reel);
                // ★ Saare reels mein same user ka follow state sync karo
                syncFollowStateForUser(reel.getUserId(), willFollow);

                UtilMethods.INSTANCE.doFollow(
                        (Activity) context,
                        String.valueOf(reel.getUserId()),
                        new UtilMethods.ApiCallBackMulti() {
                            @Override
                            public void onSuccess(Object response) {
                                ((Activity) context).runOnUiThread(() -> {
                                    int status = (int) response;

                                    if (status == 1) {
                                        // Server confirmed follow
                                        reel.setFollowing(true);
                                    } else if (status == -1) {
                                        // Server confirmed unfollow
                                        reel.setFollowing(false);
                                    }

                                    // ★ Server response se final state sync karo
                                    syncFollowStateForUser(reel.getUserId(), reel.isFollowing());

                                    String msg = reel.isFollowing() ? "User Followed" : "User Unfollowed";
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onError(String e) {
                                ((Activity) context).runOnUiThread(() -> {
                                    // ★ Error aane par rollback karo
                                    boolean rolledBack = !reel.isFollowing();
                                    reel.setFollowing(rolledBack);
                                    syncFollowStateForUser(reel.getUserId(), rolledBack);
                                    Toast.makeText(context, e, Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            });

            // ── Comment ───────────────────────────────────────────────────
            btnComment.setOnClickListener(v -> {
                if (!(context instanceof FragmentActivity)) return;
                RecyclerView rv = (RecyclerView) itemView.getParent();
                CommentsBottomSheet sheet = CommentsBottomSheet.newInstance(reel.getReelId());
                sheet.setOnDismissListener(() -> {
                    if (rv != null) rv.post(() -> rv.scrollToPosition(position));
                    int pos = getBindingAdapterPosition();
                    if (pos >= 0) notifyItemChanged(pos);
                });
                sheet.setOnCommentCountChanged((reelId, newCount) -> {
                    ((Activity) context).runOnUiThread(() -> {
                        try {
                            reel.setCommentCount(newCount);
                            commentCount.setText(formatCount(newCount));
                            if (newCount > 0) animateBounce(commentCount);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
                sheet.show(((FragmentActivity) context).getSupportFragmentManager(), "comments");
            });

            // ── Share ─────────────────────────────────────────────────────
            btnShare.setOnClickListener(v -> {
                animateBounce(btnShare);
                try {
                    String fullName  = (reel.getFullName() != null && !reel.getFullName().isEmpty()) ? reel.getFullName() : "User";
                    int reelId       = reel.getReelId() != 0 ? reel.getReelId() : 0;
                    String shareUrl  = "https://wishmaplus.com/reel/" + reelId;
                    String shareMsg  = "Check this reel by " + fullName + " on WishmaPlus!\n\n" + shareUrl;
                    Intent intent    = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, shareMsg);
                    context.startActivity(Intent.createChooser(intent, "Share via"));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Unable to share reel", Toast.LENGTH_SHORT).show();
                }
            });

            // ── Bookmark ──────────────────────────────────────────────────
            btnBookmark.setOnClickListener(v -> {
                boolean saved = btnBookmark.getTag() != null && (boolean) btnBookmark.getTag();
                btnBookmark.setTag(!saved);
                btnBookmark.setImageResource(!saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
                btnBookmark.setColorFilter(!saved ? 0xFFFFD700 : 0xFFFFFFFF);
                animateBounce(btnBookmark);
            });

            // ── More ──────────────────────────────────────────────────────
            btnMore.setOnClickListener(v -> showMoreOptions(reel));

            // ── Mute ──────────────────────────────────────────────────────
            updateMuteIcon();
            btnMute.setOnClickListener(v -> {
                isMuted = !isMuted;
                if (exoPlayer != null) exoPlayer.setVolume(isMuted ? 0f : 1f);
                updateMuteIcon();
            });

            // ── Description expand ────────────────────────────────────────
            descriptionTxt.setOnClickListener(v -> {
                if (descriptionTxt.getMaxLines() == 2) {
                    descriptionTxt.setMaxLines(Integer.MAX_VALUE);
                    descriptionTxt.setEllipsize(null);
                } else {
                    descriptionTxt.setMaxLines(2);
                    descriptionTxt.setEllipsize(android.text.TextUtils.TruncateAt.END);
                }
            });
        }

        // ── ExoPlayer setup ───────────────────────────────────────────────
        @OptIn(markerClass = UnstableApi.class)
        private void setupExoPlayer(ReelModel reel, int position) {
            ExoPlayer old = playerPool.get(position);
            if (old != null) {
                playerPool.remove(position);
                try { old.setPlayWhenReady(false); old.stop(); } catch (Exception ignored) {}
                final ExoPlayer toRelease = old;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try { toRelease.release(); } catch (Exception ignored) {}
                }, 200);
            }
            exoPlayer = null;
            playerView.setPlayer(null);

            String videoUrl = reel.getVideoUrl();
            if (videoUrl == null || videoUrl.trim().isEmpty()) return;

            DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .build());

            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);

            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                    .setBufferDurationsMs(1500, 5000, 800, 1500)
                    .build();

            exoPlayer = new ExoPlayer.Builder(context, renderersFactory)
                    .setLoadControl(loadControl)
                    .setTrackSelector(trackSelector)
                    .build();

            playerPool.put(position, exoPlayer);
            playerView.setPlayer(exoPlayer);
            playerView.setUseController(false);
            playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);

            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)));
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            exoPlayer.setVolume(isMuted ? 0f : 1f);

            boolean shouldPlay = (position == currentPlayingPosition);
            exoPlayer.setPlayWhenReady(shouldPlay);
            exoPlayer.prepare();
            if (shouldPlay) videoStartTime = System.currentTimeMillis();

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    try {
                        switch (state) {
                            case Player.STATE_BUFFERING:
                                progress.setVisibility(View.VISIBLE);
                                break;
                            case Player.STATE_READY:
                                progress.setVisibility(View.GONE);
                                thumbnail.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                                    thumbnail.setVisibility(View.GONE);
                                    thumbnail.setAlpha(1f);
                                }).start();
                                long dur = exoPlayer.getDuration();
                                if (dur > 0 && dur != androidx.media3.common.C.TIME_UNSET) {
                                    tvTotalTime.setText(formatTime(dur));
                                }
                                if (position == currentPlayingPosition && !exoPlayer.isPlaying()) {
                                    exoPlayer.play();
                                    if (videoStartTime == 0) videoStartTime = System.currentTimeMillis();
                                }
                                handler.removeCallbacks(progressRunnable);
                                handler.post(progressRunnable);
                                if (position == currentPlayingPosition) {
                                    ReelWatchTracker.getInstance().onReelStarted(reel.getReelId());
                                }
                                break;
                            case Player.STATE_ENDED:
                                progress.setVisibility(View.GONE);
                                isEnded = true;
                                exoPlayer.setPlayWhenReady(false);
                                showReplayState();
                                ReelWatchTracker.getInstance().forceFlush();
                                break;
                            default:
                                progress.setVisibility(View.GONE);
                                break;
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    try {
                        if (isPlaying) {
                            ReelWatchTracker.getInstance().onReelResumed();
                            if (videoStartTime == 0) videoStartTime = System.currentTimeMillis();
                        } else {
                            if (exoPlayer != null && exoPlayer.getPlaybackState() != Player.STATE_ENDED) {
                                ReelWatchTracker.getInstance().onReelPaused();
                            }
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }

                @Override
                public void onPlayerError(androidx.media3.common.PlaybackException error) {
                    progress.setVisibility(View.GONE);
                    thumbnail.setAlpha(1f);
                    thumbnail.setVisibility(View.VISIBLE);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            if (exoPlayer != null && error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                                exoPlayer.prepare();
                            }
                        } catch (Exception ignored) {}
                    }, 1000);
                }
            });
        }

        // ── Replay ───────────────────────────────────────────────────────
        private void showReplayState() {
            playPauseOverlay.setOnClickListener(null);
            playPauseOverlay.setImageResource(R.drawable.pause_circle);
            playPauseOverlay.setAlpha(1f);
            playPauseOverlay.setScaleX(1f);
            playPauseOverlay.setScaleY(1f);
            playPauseOverlay.setVisibility(View.VISIBLE);
            playPauseOverlay.setClickable(true);
            playPauseOverlay.setOnClickListener(v -> {
                playPauseOverlay.setOnClickListener(null);
                playPauseOverlay.setClickable(false);
                playPauseOverlay.setVisibility(View.GONE);
                if (exoPlayer != null) {
                    isEnded = false;
                    exoPlayer.seekTo(0);
                    exoPlayer.play();
                    videoStartTime = System.currentTimeMillis();
                    isViewCounted = false;
                }
            });
        }

        // ── Controls ─────────────────────────────────────────────────────
        void pausePlayer() {
            handler.removeCallbacks(progressRunnable);
            if (exoPlayer != null) {
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.pause();
            }
        }

        void resumePlayer() {
            if (exoPlayer == null) return;
            if (isEnded) { showReplayState(); return; }
            if (exoPlayer.getPlaybackState() != Player.STATE_ENDED && !exoPlayer.isPlaying()) {
                exoPlayer.play();
                handler.removeCallbacks(progressRunnable);
                handler.post(progressRunnable);
            }
        }

        void releasePlayer() {
            handler.removeCallbacks(progressRunnable);
            if (singleTapRunnable != null) { handler.removeCallbacks(singleTapRunnable); singleTapRunnable = null; }
            if (discAnimator != null) discAnimator.cancel();
            if (exoPlayer != null) {
                int pos = getBindingAdapterPosition();
                if (pos >= 0) playerPool.remove(pos);
                exoPlayer.release();
                exoPlayer = null;
                playerView.setPlayer(null);
            }
        }

        private void togglePlayPause() {
            if (exoPlayer == null) return;
            if (exoPlayer.getPlaybackState() == Player.STATE_ENDED) {
                playPauseOverlay.setOnClickListener(null);
                playPauseOverlay.setClickable(false);
                playPauseOverlay.setVisibility(View.GONE);
                isEnded = false;
                exoPlayer.seekTo(0);
                exoPlayer.play();
                videoStartTime = System.currentTimeMillis();
                isViewCounted = false;
                return;
            }
            if (exoPlayer.isPlaying()) {
                exoPlayer.pause();
                showCentreIcon(R.drawable.pause_circle, false);
            } else {
                exoPlayer.play();
                showCentreIcon(R.drawable.ic_play_circle_outline, true);
            }
        }

        private void updateMuteIcon() {
            btnMute.setImageResource(isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
        }

        private void startDiscSpin() {
            if (discAnimator != null) discAnimator.cancel();
            discAnimator = ObjectAnimator.ofFloat(musicDisc, "rotation", 0f, 360f);
            discAnimator.setDuration(4000);
            discAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            discAnimator.setInterpolator(new LinearInterpolator());
            discAnimator.start();
        }

        private void flashSeek(ImageView icon) {
            icon.animate().cancel();
            icon.setAlpha(1f);
            icon.setScaleX(1.4f);
            icon.setScaleY(1.4f);
            icon.animate().alpha(0f).scaleX(1f).scaleY(1f).setDuration(500).start();
        }

        private void showCentreIcon(int res, boolean autoHide) {
            playPauseOverlay.setOnClickListener(null);
            playPauseOverlay.setClickable(false);
            playPauseOverlay.setImageResource(res);
            playPauseOverlay.animate().cancel();
            playPauseOverlay.setAlpha(1f);
            playPauseOverlay.setScaleX(0.7f);
            playPauseOverlay.setScaleY(0.7f);
            playPauseOverlay.setVisibility(View.VISIBLE);
            playPauseOverlay.animate().scaleX(1f).scaleY(1f).setDuration(200)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> {
                        if (autoHide) {
                            playPauseOverlay.animate().alpha(0f).setStartDelay(600).setDuration(300)
                                    .withEndAction(() -> playPauseOverlay.setVisibility(View.GONE)).start();
                        }
                    }).start();
        }

        // ── APIs ─────────────────────────────────────────────────────────
        private void callAddReelView(int reelId, int watchSec) {
            TrackReelViewRequest session = new TrackReelViewRequest(reelId, watchSec);
            List<TrackReelViewRequest> sessions = new ArrayList<>();
            sessions.add(session);
            UtilMethods.INSTANCE.trackReelViewBatch(sessions, new UtilMethods.ApiCallBackMulti() {
                @Override public void onSuccess(Object r) {}
                @Override public void onError(String e) {}
            });
        }

        private void doLikeApi(ReelModel reel) {
            if (!(context instanceof Activity)) return;
            UtilMethods.INSTANCE.doLikeUnLikeReel((Activity) context, reel.getReelId(), new UtilMethods.ApiCallBackMulti() {
                @Override
                public void onSuccess(Object response) {
                    reel.setLiked(!reel.getLiked());
                    if (reel.getLiked()) {
                        reel.setLikeCount(reel.getLikeCount() + 1);
                        showHeartAnimation();
                    } else {
                        if (reel.getLikeCount() > 0) reel.setLikeCount(reel.getLikeCount() - 1);
                    }
                    updateLikeState(reel);
                }
                @Override
                public void onError(String e) {
                    Toast.makeText(context, e, Toast.LENGTH_SHORT).show();
                }
            });
        }

        // ── UI helpers ────────────────────────────────────────────────────
        private void updateLikeState(ReelModel reel) {
            likeCount.setText(formatCount(reel.getLikeCount()));
            btnLike.setImageResource(reel.getLiked() ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
            btnLike.setColorFilter(reel.getLiked() ? 0xFFFF3B30 : Color.WHITE);
        }

        private void showHeartAnimation() {
            heartOverlay.setVisibility(View.VISIBLE);
            heartOverlay.setScaleX(0f);
            heartOverlay.setScaleY(0f);
            heartOverlay.setAlpha(1f);
            heartOverlay.animate().scaleX(1f).scaleY(1f).setDuration(300)
                    .setInterpolator(new OvershootInterpolator(1.5f))
                    .withEndAction(() -> heartOverlay.animate().alpha(0f).setStartDelay(400).setDuration(400)
                            .withEndAction(() -> heartOverlay.setVisibility(View.GONE)).start())
                    .start();
        }

        private void animateBounce(View v) {
            ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(v,
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.3f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.3f, 1f));
            anim.setDuration(300);
            anim.setInterpolator(new OvershootInterpolator());
            anim.start();
        }

        private void showMoreOptions(ReelModel reel) {
            android.view.View sheetView = android.view.LayoutInflater.from(context)
                    .inflate(R.layout.dialog_more_options, null);

            android.view.View optDelete = sheetView.findViewById(R.id.optDelete);
            android.view.View dividerDelete = sheetView.findViewById(R.id.dividerDelete);
            if (reel.isMyReel()) {
                optDelete.setVisibility(View.VISIBLE);
                dividerDelete.setVisibility(View.VISIBLE);
            } else {
                optDelete.setVisibility(View.GONE);
                dividerDelete.setVisibility(View.GONE);
            }

            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                    new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.BottomSheetStyleCustom);
            bottomSheet.setContentView(sheetView);
            if (bottomSheet.getWindow() != null) bottomSheet.getWindow().setDimAmount(0f);
            bottomSheet.setOnShowListener(d -> dimOverlay.animate().alpha(0.5f).setDuration(250).start());
            bottomSheet.setOnDismissListener(d -> dimOverlay.animate().alpha(0f).setDuration(250).start());
            bottomSheet.show();

            sheetView.findViewById(R.id.optReport).setOnClickListener(v -> {
                bottomSheet.dismiss();
                Toast.makeText(context, "Reported. We'll review this content.", Toast.LENGTH_SHORT).show();
            });
            sheetView.findViewById(R.id.optNotInterested).setOnClickListener(v -> {
                bottomSheet.dismiss();
                Toast.makeText(context, "Got it! You'll see less like this.", Toast.LENGTH_SHORT).show();
            });
            sheetView.findViewById(R.id.optCopyLink).setOnClickListener(v -> {
                bottomSheet.dismiss();
                android.content.ClipboardManager clipboard =
                        (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(
                        "Reel Link", "https://wishmaplus.com/reel/" + reel.getReelId());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show();
            });
            sheetView.findViewById(R.id.optAbout).setOnClickListener(v -> {
                bottomSheet.dismiss();
                Toast.makeText(context, "About account", Toast.LENGTH_SHORT).show();
            });
            optDelete.setOnClickListener(v -> {
                bottomSheet.dismiss();
                showDeleteConfirmDialog(reel);
            });
        }

        private void showDeleteConfirmDialog(ReelModel reel) {
            android.view.View dialogView = android.view.LayoutInflater.from(context)
                    .inflate(R.layout.dialog_delete_confirm, null);
            android.app.Dialog dialog = new android.app.Dialog(context);
            dialog.setContentView(dialogView);
            dialog.setCancelable(true);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialogView.findViewById(R.id.btnCancelDelete).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
                dialog.dismiss();
                int position = getBindingAdapterPosition();
                if (position < 0) return;
                if (customLoader != null) customLoader.show();
                UtilMethods.INSTANCE.deleteReel(reel.getReelId(), customLoader, new UtilMethods.ApiCallBackMulti() {
                    @Override
                    public void onSuccess(Object response) {
                        ((Activity) context).runOnUiThread(() -> {
                            if (customLoader != null) customLoader.dismiss();
                            releasePlayer();
                            int pos = getBindingAdapterPosition();
                            if (pos >= 0 && pos < reels.size()) {
                                reels.remove(pos);
                                notifyItemRemoved(pos);
                                notifyItemRangeChanged(pos, reels.size());
                                if (currentPlayingPosition == pos) currentPlayingPosition = -1;
                                else if (currentPlayingPosition > pos) currentPlayingPosition--;
                            }
                            Toast.makeText(context, "Reel deleted successfully", Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onError(String error) {
                        ((Activity) context).runOnUiThread(() -> {
                            if (customLoader != null) customLoader.dismiss();
                            Toast.makeText(context, "Delete failed: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            });
            dialog.show();
        }

        private String formatTime(long ms) {
            long s = ms / 1000;
            return String.format(Locale.getDefault(), "%d:%02d", s / 60, s % 60);
        }

        @SuppressLint("DefaultLocale")
        private String formatCount(long c) {
            if (c >= 1_000_000) return String.format("%.1fM", c / 1_000_000.0);
            if (c >= 1_000) return String.format("%.1fK", c / 1_000.0);
            return String.valueOf(c);
        }
    }
}