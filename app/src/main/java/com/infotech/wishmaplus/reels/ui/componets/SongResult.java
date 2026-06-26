package com.infotech.wishmaplus.reels.ui.componets;

import com.google.gson.annotations.SerializedName;
import com.infotech.wishmaplus.Api.Response.SongModel;

import java.util.List;


public class SongResult {
    @SerializedName("songs")
    public List<SongModel> songs;
    @SerializedName("total")
    public int total;
    @SerializedName("hasMore")
    public boolean hasMore;

    public List<SongModel> getSongs() {
        return songs;
    }

    public void setSongs(List<SongModel> songs) {
        this.songs = songs;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}

