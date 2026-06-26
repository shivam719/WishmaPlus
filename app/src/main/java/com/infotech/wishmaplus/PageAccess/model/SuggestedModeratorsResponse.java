package com.infotech.wishmaplus.PageAccess.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SuggestedModeratorsResponse {

    @SerializedName("statusCode")
    private int statusCode;

    @SerializedName("responseText")
    private String responseText;

    @SerializedName("result")
    private List<SuggestedUser> result;

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseText() {
        return responseText;
    }

    public List<SuggestedUser> getResult() {
        return result;
    }

    public static class SuggestedUser {
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
        private int inviteStatus;

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
        @SerializedName("followerCount")
        private int followerCount;

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

        public int getFollowerCount() {
            return followerCount;
        }

        public int getModeratorId() {
            return moderatorId;
        }

        public void setModeratorId(int moderatorId) {
            this.moderatorId = moderatorId;
        }

        public String getPageId() {
            return pageId;
        }

        public void setPageId(String pageId) {
            this.pageId = pageId;
        }

        public void setUserIntId(int userIntId) {
            this.userIntId = userIntId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public void setProfilePictureUrl(String profilePictureUrl) {
            this.profilePictureUrl = profilePictureUrl;
        }

        public void setMobileNo(String mobileNo) {
            this.mobileNo = mobileNo;
        }

        public int getInviteStatus() {
            return inviteStatus;
        }

        public void setInviteStatus(int inviteStatus) {
            this.inviteStatus = inviteStatus;
        }

        public String getInviteStatusText() {
            return inviteStatusText;
        }

        public void setInviteStatusText(String inviteStatusText) {
            this.inviteStatusText = inviteStatusText;
        }

        public boolean isCanManageContent() {
            return canManageContent;
        }

        public void setCanManageContent(boolean canManageContent) {
            this.canManageContent = canManageContent;
        }

        public boolean isCanManageMessages() {
            return canManageMessages;
        }

        public void setCanManageMessages(boolean canManageMessages) {
            this.canManageMessages = canManageMessages;
        }

        public boolean isCanManageCommunity() {
            return canManageCommunity;
        }

        public void setCanManageCommunity(boolean canManageCommunity) {
            this.canManageCommunity = canManageCommunity;
        }

        public boolean isCanViewInsights() {
            return canViewInsights;
        }

        public void setCanViewInsights(boolean canViewInsights) {
            this.canViewInsights = canViewInsights;
        }

        public String getEntryAt() {
            return entryAt;
        }

        public void setEntryAt(String entryAt) {
            this.entryAt = entryAt;
        }

        public void setFollowerCount(int followerCount) {
            this.followerCount = followerCount;
        }
    }
}