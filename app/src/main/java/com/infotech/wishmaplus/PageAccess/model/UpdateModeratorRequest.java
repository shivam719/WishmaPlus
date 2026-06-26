package com.infotech.wishmaplus.PageAccess.model;

import com.google.gson.annotations.SerializedName;

public class UpdateModeratorRequest {
    @SerializedName("pageId")
    public String pageId;
    @SerializedName("userId")
    public String userId;
    @SerializedName("moderatorId")
    public int moderatorId;
    @SerializedName("canManageContent")
    public boolean canManageContent;
    @SerializedName("canManageMessages")
    public boolean canManageMessages;
    @SerializedName("canManageCommunity")
    public boolean canManageCommunity;
    @SerializedName("canViewInsights")
    public boolean canViewInsights;

    public UpdateModeratorRequest() {
    }

    public UpdateModeratorRequest(String pageId, String userId,
                                  int moderatorId, boolean canManageContent,
                                  boolean canManageMessages,
                                  boolean canManageCommunity,
                                  boolean canViewInsights) {
        this.pageId = pageId;
        this.userId = userId;
        this.moderatorId = moderatorId;
        this.canManageContent = canManageContent;
        this.canManageMessages = canManageMessages;
        this.canManageCommunity = canManageCommunity;
        this.canViewInsights = canViewInsights;
    }
}