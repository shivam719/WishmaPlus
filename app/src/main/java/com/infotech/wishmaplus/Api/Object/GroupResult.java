package com.infotech.wishmaplus.Api.Object;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class GroupResult implements Parcelable {

    @SerializedName("groupId")
    private String groupId;

    @SerializedName("ownerUserId")
    private String ownerUserId;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("profileImageUrl")
    private String profileImageUrl;

    @SerializedName("coverImageUrl")
    private String coverImageUrl;

    @SerializedName("isPrivate")
    private boolean isPrivate;

    @SerializedName("isVisible")
    private boolean isVisible;

    @SerializedName("isActive")
    private boolean isActive;

    @SerializedName("isAdmin")
    private boolean isAdmin;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("ownerName")
    private String ownerName;

    @SerializedName("ownerProfileImage")
    private String ownerProfileImage;

    @SerializedName("pageId")
    private String pageId;

    @SerializedName("pageName")
    private String pageName;

    @SerializedName("categoryId")
    private String categoryId;

    @SerializedName("bio")
    private String bio;

    @SerializedName("website")
    private String website;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("createdByUserId")
    private String createdByUserId;

    @SerializedName("isProfile")
    private boolean isProfile;


    // 🔹 Empty constructor
    public GroupResult() {
    }

    // 🔹 Parcelable constructor
    protected GroupResult(Parcel in) {
        groupId = in.readString();
        ownerUserId = in.readString();
        title = in.readString();
        description = in.readString();
        profileImageUrl = in.readString();
        coverImageUrl = in.readString();
        isPrivate = in.readByte() != 0;
        isVisible = in.readByte() != 0;
        isActive = in.readByte() != 0;
        isAdmin = in.readByte() != 0;
        createdAt = in.readString();
        ownerName = in.readString();
        ownerProfileImage = in.readString();
        pageId = in.readString();
        pageName = in.readString();
        categoryId = in.readString();
        bio = in.readString();
        website = in.readString();
        email = in.readString();
        phone = in.readString();
        address = in.readString();
        createdByUserId = in.readString();
        isProfile = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(groupId);
        dest.writeString(ownerUserId);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(profileImageUrl);
        dest.writeString(coverImageUrl);
        dest.writeByte((byte) (isPrivate ? 1 : 0));
        dest.writeByte((byte) (isVisible ? 1 : 0));
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeByte((byte) (isAdmin ? 1 : 0));
        dest.writeString(createdAt);
        dest.writeString(ownerName);
        dest.writeString(ownerProfileImage);
        dest.writeString(pageId);
        dest.writeString(pageName);
        dest.writeString(categoryId);
        dest.writeString(bio);
        dest.writeString(website);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeString(address);
        dest.writeString(createdByUserId);
        dest.writeByte((byte) (isProfile ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<GroupResult> CREATOR = new Creator<GroupResult>() {
        @Override
        public GroupResult createFromParcel(Parcel in) {
            return new GroupResult(in);
        }

        @Override
        public GroupResult[] newArray(int size) {
            return new GroupResult[size];
        }
    };

    // 🔹 Getters & Setters

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerProfileImage() {
        return ownerProfileImage;
    }

    public void setOwnerProfileImage(String ownerProfileImage) {
        this.ownerProfileImage = ownerProfileImage;
    }
    public String getPageId() {
        return pageId != null ? pageId : "";
    }

    public void setPageId(String pageId) {
        this.pageId = pageId;
    }

    public String getPageName() {
        return pageName != null ? pageName : "";
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getCategoryId() {
        return categoryId != null ? categoryId : "";
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getBio() {
        return bio != null ? bio : "";
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getWebsite() {
        return website != null ? website : "";
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCreatedByUserId() {
        return createdByUserId != null ? createdByUserId : "";
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public boolean isProfile() {
        return isProfile;
    }

    public void setProfile(boolean profile) {
        isProfile = profile;
    }
}
