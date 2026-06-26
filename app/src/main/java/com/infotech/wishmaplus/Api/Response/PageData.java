package com.infotech.wishmaplus.Api.Response;



public class PageData {

    private String pageId;
    private String pageName;
    private String categoryId;
    private String bio;
    private String website;
    private String email;
    private String phone;
    private String address;
    private String profileImageUrl;
    private String coverImageUrl;
    private String createdByUserId;
    private boolean isProfile;
    private boolean isModerator;
    private boolean canManageContent;
    private boolean canManageMessages;
    private boolean canManageCommunity;
    private boolean canViewInsights;

    public PageData(String pageId, String pageName,Boolean isProfile, String categoryId, String bio, String website,
                    String email, String phone, String address, String profileImageUrl,
                    String coverImageUrl, String createdByUserId) {

        this.pageId = pageId;
        this.pageName = pageName;
        this.isProfile = isProfile;
        this.categoryId = categoryId;
        this.bio = bio;
        this.website = website;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.profileImageUrl = profileImageUrl;
        this.coverImageUrl = coverImageUrl;
        this.createdByUserId = createdByUserId;
    }

    public String getPageId() {
        return pageId;
    }

    public String getPageName() {
        return pageName;
    }


    public String getCategoryId() {
        return categoryId;
    }

    public String getBio() {
        return bio;
    }

    public String getWebsite() {
        return website;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public boolean isProfile() {
        return isProfile;
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public void setProfile(boolean profile) {
        isProfile = profile;
    }

    public boolean isModerator() {
        return isModerator;
    }

    public void setModerator(boolean moderator) {
        isModerator = moderator;
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
}