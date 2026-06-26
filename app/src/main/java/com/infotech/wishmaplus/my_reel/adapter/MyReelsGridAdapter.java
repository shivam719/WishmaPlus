package com.infotech.wishmaplus.my_reel.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.ReelModel;
import com.infotech.wishmaplus.ReelsFeedActivity;

import java.util.List;
import java.util.Locale;

/**
 * Grid adapter shown inside the Profile screen's "Reels" tab.
 * <p>
 * Clicking any thumbnail opens {@link ReelsFeedActivity} and starts
 * playback from that exact reel (via the "reelId" intent extra that
 * ReelsFeedActivity already reads from its deep-link logic).
 */

public class MyReelsGridAdapter extends RecyclerView.Adapter<MyReelsGridAdapter.ReelThumbVH> {

    private final Context context;
    private final List<ReelModel> reels;

   String pageId;

    public MyReelsGridAdapter(Context context, List<ReelModel> reels) {
        this.context = context;
        this.reels = reels;
    }
    public void setPageId(String pageId) {
        this.pageId = pageId;
    }
    @NonNull
    @Override
    public ReelThumbVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_reel_thumbnail, parent, false);
        return new ReelThumbVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ReelThumbVH holder, int position) {
        holder.bind(reels.get(position));
    }

    @Override
    public int getItemCount() {
        return reels == null ? 0 : reels.size();
    }

    // ── ViewHolder ───────────────────────────────────────────────────────

    class ReelThumbVH extends RecyclerView.ViewHolder {

        private final ImageView thumbnail;
        private final TextView tvDuration;
        private final TextView tvViews;
        private final ImageView icPinned;
        private final TextView tvReelLabel;   // "Reel" text inside play badge

        ReelThumbVH(@NonNull View itemView) {
            super(itemView);
            thumbnail   = itemView.findViewById(R.id.reelThumb);
            tvDuration  = itemView.findViewById(R.id.tvReelDuration);
            tvViews     = itemView.findViewById(R.id.tvReelViews);
            icPinned    = itemView.findViewById(R.id.icPinned);
            tvReelLabel = itemView.findViewById(R.id.tvReelLabel);
        }

        void bind(ReelModel reel) {

            // ── Thumbnail ────────────────────────────────────────────────
            String thumb = reel.getThumbnailUrl() != null
                    ? reel.getThumbnailUrl()
                    : reel.getVideoUrl();

            Glide.with(context)
                    .load(thumb)
                    .centerCrop()
                    .placeholder(R.drawable.app_logo)
                    .transition(DrawableTransitionOptions.withCrossFade(150))
                    .into(thumbnail);

            // ── Play badge label (always "Reel") ─────────────────────────
            // tvReelLabel is inside the LinearLayout badge — always visible
            tvReelLabel.setText(context.getString(R.string.reel));

            // ── Duration pill ────────────────────────────────────────────
            if (reel.getDuration() > 0) {
                tvDuration.setVisibility(View.VISIBLE);
                tvDuration.setText(formatDuration(reel.getDuration()));
            } else {
                tvDuration.setVisibility(View.GONE);
            }

            // ── View count ───────────────────────────────────────────────
            if (reel.getViewCount() > 0) {
                tvViews.setVisibility(View.VISIBLE);
                tvViews.setText(formatCount(reel.getViewCount()));
            } else {
                tvViews.setVisibility(View.GONE);
            }

            // ── Pinned / Bookmark icon ───────────────────────────────────
            // Show gold bookmark if reel is pinned, hide otherwise
            if (icPinned != null) {
                icPinned.setVisibility(reel.isPinned() ? View.VISIBLE : View.GONE);
            }

            // ── Click → open ReelsFeedActivity ───────────────────────────
            itemView.setOnClickListener(v -> openReelFeed(reel.getReelId(), reel.isMyReel()));
        }

        // ── Helpers ──────────────────────────────────────────────────────

        private void openReelFeed(int reelId, boolean myReel) {
            Intent intent = new Intent(context, ReelsFeedActivity.class);
            intent.putExtra("reelId", String.valueOf(reelId));
            intent.putExtra("pageId", pageId);
            intent.putExtra("isMyReel", true);  // ★ fix: was hardcoded true
            context.startActivity(intent);
        }

        /** Converts total seconds → "m:ss" */
        private String formatDuration(int totalSeconds) {
            int m = totalSeconds / 60;
            int s = totalSeconds % 60;
            return String.format(Locale.getDefault(), "%d:%02d", m, s);
        }

        /** 1200 → "1.2K" | 2000000 → "2.0M" | else raw number */
        private String formatCount(long count) {
            if (count >= 1_000_000)
                return String.format(Locale.getDefault(), "%.1fM", count / 1_000_000.0);
            if (count >= 1_000)
                return String.format(Locale.getDefault(), "%.1fK", count / 1_000.0);
            return String.valueOf(count);
        }
    }
}