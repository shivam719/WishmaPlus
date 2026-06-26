package com.infotech.wishmaplus.PageAccess.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// ── Top-level response ────────────────────────────────────────────────────
public class ModeratorsResponse {

    @SerializedName("statusCode")
    private int statusCode;

    @SerializedName("responseText")
    private String responseText;

    @SerializedName("result")
    private List<Moderator> result;

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseText() {
        return responseText;
    }

    public List<Moderator> getResult() {
        return result;
    }

    // ── Moderator item ────────────────────────────────────────────────────
    public static class Moderator {

        @SerializedName("moderatorId")
        private int moderatorId;

        @SerializedName("pageId")
        private String pageId;

        @SerializedName("userIntId")
        private int userIntId;

        @SerializedName("userId")
        private String userId;

        @SerializedName("fullName")
        private String fullName;

        @SerializedName("profilePictureUrl")
        private String profilePictureUrl;

        @SerializedName("mobileNo")
        private String mobileNo;

        @SerializedName("inviteStatus")
        private int inviteStatus;          // 0 = Pending, 1 = Accepted, 2 = Declined

        @SerializedName("inviteStatusText")
        private String inviteStatusText;

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

        public int getModeratorId() {
            return moderatorId;
        }

        public String getPageId() {
            return pageId;
        }

        public int getUserIntId() {
            return userIntId;
        }

        public String getUserId() {
            return userId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getProfilePictureUrl() {
            return profilePictureUrl;
        }

        public String getMobileNo() {
            return mobileNo;
        }

        public int getInviteStatus() {
            return inviteStatus;
        }

        public String getInviteStatusText() {
            return inviteStatusText;
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
}