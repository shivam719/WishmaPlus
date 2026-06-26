package com.infotech.wishmaplus.Adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.infotech.wishmaplus.ReelModel;
import com.infotech.wishmaplus.my_reel.adapter.ProfileReelsViewHolder;

import java.util.List;

class ReelsGridViewHolder extends RecyclerView.ViewHolder {

    private final ProfileReelsViewHolder delegate;
    private String pageId;
    ReelsGridViewHolder(android.view.View itemView) {
        super(itemView);
        // Delegates all logic to the standalone ProfileReelsViewHolder
        delegate = new ProfileReelsViewHolder(itemView);
    }

    void bind(List<ReelModel> reels, String pageId) {
        delegate.bind(reels,pageId);
        this.pageId = pageId;
    }

    /** Called by ProfileActivity when a pagination response arrives */
    void appendReels(List<ReelModel> moreReels) {
        delegate.appendReels(moreReels);
    }
}
