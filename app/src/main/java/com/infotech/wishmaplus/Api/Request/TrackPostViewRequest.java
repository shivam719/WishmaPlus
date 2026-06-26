package com.infotech.wishmaplus.Api.Request;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class TrackPostViewRequest implements Serializable {

    @SerializedName("postId")
    private String postId;

    @SerializedName("viewDurationSeconds")
    private int viewDurationSeconds;

    public TrackPostViewRequest(String postId, int viewDurationSeconds) {
        this.postId = postId;
        this.viewDurationSeconds = viewDurationSeconds;
    }
}
