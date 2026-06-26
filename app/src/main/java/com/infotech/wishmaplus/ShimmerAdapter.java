package com.infotech.wishmaplus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.infotech.wishmaplus.R;

/**
 * ShimmerAdapter — Shows skeleton shimmer cards while real data is loading.
 *
 * USAGE in your Activity / Fragment:
 * ──────────────────────────────────
 *
 * 1. Before API call → show shimmer:
 *    ShimmerAdapter shimmerAdapter = new ShimmerAdapter(5); // 5 placeholder cards
 *    recyclerView.setAdapter(shimmerAdapter);
 *
 * 2. After data loads → swap to real adapter:
 *    recyclerView.setAdapter(multiContentAdapter);
 *    multiContentAdapter.notifyDataSetChanged();
 *
 * GRADLE dependency (add to app/build.gradle):
 *    implementation 'com.facebook.shimmer:shimmer:0.5.0'
 */
public class ShimmerAdapter extends RecyclerView.Adapter<ShimmerAdapter.ShimmerViewHolder> {

    private final int itemCount;

    /**
     * @param itemCount how many shimmer skeleton cards to show (3–5 is ideal)
     */
    public ShimmerAdapter(int itemCount) {
        this.itemCount = itemCount;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shimmer_item, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        // Start shimmer animation
        holder.shimmerLayout.startShimmer();
    }

    @Override
    public void onViewRecycled(@NonNull ShimmerViewHolder holder) {
        super.onViewRecycled(holder);
        // Stop shimmer when view is recycled to save resources
        holder.shimmerLayout.stopShimmer();
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerLayout;

        ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
            shimmerLayout = itemView.findViewById(R.id.shimmerLayout);
        }
    }
}
