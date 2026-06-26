package com.infotech.wishmaplus.my_reel.adapter;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.infotech.wishmaplus.R;
import com.infotech.wishmaplus.ReelModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A self-contained ViewHolder that renders the Reels grid inside the
 * Profile screen's adapter row (view type 444 = VIEW_TYPE_PROFILE_REELS).
 * <p>
 * This class is used by {@link MultiContentAdapter} when
 * {@code buttonContentTypeId == 4} (the Reels tab).
 * <p>
 * Layout file: adapter_profile_reels_grid.xml
 */
public class ProfileReelsViewHolder extends RecyclerView.ViewHolder {

    private final RecyclerView reelsGrid;
    private final TextView noReelsTv;
    private final MyReelsGridAdapter gridAdapter;
    private final List<ReelModel> reelList = new ArrayList<>();

    private String pageId;

    public ProfileReelsViewHolder(@NonNull View itemView) {
        super(itemView);

        reelsGrid = itemView.findViewById(R.id.reelsGrid);
        noReelsTv = itemView.findViewById(R.id.noReelsTv);

        GridLayoutManager glm = new GridLayoutManager(itemView.getContext(), 3);

        reelsGrid.setLayoutManager(glm);
        reelsGrid.setHasFixedSize(false);
        reelsGrid.setNestedScrollingEnabled(false);

        // REMOVE pageId from constructor
        gridAdapter = new MyReelsGridAdapter(
                itemView.getContext(),
                reelList
        );

        reelsGrid.setAdapter(gridAdapter);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void bind(List<ReelModel> newReels, String pageId) {

        this.pageId = pageId;

        // update adapter value
        gridAdapter.setPageId(pageId);

        reelList.clear();

        if (newReels != null && !newReels.isEmpty()) {

            reelList.addAll(newReels);

            reelsGrid.setVisibility(View.VISIBLE);
            noReelsTv.setVisibility(View.GONE);

        } else {

            reelsGrid.setVisibility(View.GONE);
            noReelsTv.setVisibility(View.VISIBLE);
        }

        gridAdapter.notifyDataSetChanged();
    }

    public void appendReels(List<ReelModel> moreReels) {

        if (moreReels == null || moreReels.isEmpty()) return;

        int start = reelList.size();

        reelList.addAll(moreReels);

        gridAdapter.notifyItemRangeInserted(start, moreReels.size());

        if (reelsGrid.getVisibility() != View.VISIBLE) {

            reelsGrid.setVisibility(View.VISIBLE);
            noReelsTv.setVisibility(View.GONE);
        }
    }
}