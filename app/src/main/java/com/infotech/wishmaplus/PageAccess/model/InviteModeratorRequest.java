package com.infotech.wishmaplus.PageAccess.model;


import com.google.gson.annotations.SerializedName;

public class InviteModeratorRequest {

    @SerializedName("pageId")
    private String pageId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("canManageContent")
    private boolean canManageContent;

    @SerializedName("canManageMessages")
    private boolean canManageMessages;

    @SerializedName("canManageCommunity")
    private boolean canManageCommunity;

    @SerializedName("canViewInsights")
    private boolean canViewInsights;

    public InviteModeratorRequest(String pageId, String userId, boolean canManageContent, boolean canManageMessages, boolean canManageCommunity, boolean canViewInsights) {
        this.pageId = pageId;
        this.userId = userId;
        this.canManageContent = canManageContent;
        this.canManageMessages = canManageMessages;
        this.canManageCommunity = canManageCommunity;
        this.canViewInsights = canViewInsights;
    }
}