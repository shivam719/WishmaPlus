package com.infotech.wishmaplus.PageAccess.model;

import com.google.gson.annotations.SerializedName;

public class RespondToInviteRequest {

    @SerializedName("moderatorId")
    private int moderatorId;

    @SerializedName("isAccepted")
    private boolean isAccepted;

    public RespondToInviteRequest(int moderatorId, boolean isAccepted) {
        this.moderatorId = moderatorId;
        this.isAccepted  = isAccepted;
    }

    public int getModeratorId()  { return moderatorId; }
    public boolean isAccepted()  { return isAccepted; }
}