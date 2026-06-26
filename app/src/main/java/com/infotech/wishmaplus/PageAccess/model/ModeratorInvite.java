package com.infotech.wishmaplus.PageAccess.model;

import com.google.gson.annotations.SerializedName;

public class ModeratorInvite {

    @SerializedName("moderatorId")
    private int moderatorId;

    @SerializedName("pageId")
    private String pageId;

    @SerializedName("pageName")
    private String pageName;

    @SerializedName("pageProfileImageUrl")
    private String pageProfileImageUrl;

    @SerializedName("invitedByName")
    private String invitedByName;

    @SerializedName("canManageContent")
    private boolean canManageContent;

    @SerializedName("canManageMessages")
    private boolean canManageMessages;

    @SerializedName("canManageCommunity")
    private boolean canManageCommunity;

    @SerializedName("canViewInsights")
    private boolean canViewInsights;

    @SerializedName("entryAt")
    private String entryAt;

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getModeratorId() {
        return moderatorId;
    }

    public String getPageId() {
        return pageId;
    }

    public String getPageName() {
        return pageName;
    }

    public String getPageProfileImageUrl() {
        return pageProfileImageUrl;
    }

    public String getInvitedByName() {
        return invitedByName;
    }

    public boolean isCanManageContent() {
        return canManageContent;
    }

    public boolean isCanManageMessages() {
        return canManageMessages;
    }

    public boolean isCanManageCommunity() {
        return canManageCommunity;
    }

    public boolean isCanViewInsights() {
        return canViewInsights;
    }

    public String getEntryAt() {
        return entryAt;
    }
}