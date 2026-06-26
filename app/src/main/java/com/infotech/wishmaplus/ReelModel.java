package com.infotech.wishmaplus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ReelModel implements Parcelable {
    @SerializedName("reelId")
    @Expose
    private int reelId;
    @SerializedName("userId")
    @Expose
    private String userId;
    @SerializedName("fullName")
    @Expose
    private String fullName;
    @SerializedName("profilePictureUrl")
    @Expose
    private String profilePictureUrl;
    @SerializedName("viewCount")
    @Expose
    private int viewCount;
    @SerializedName("likeCount")
    @Expose
    private int likeCount;
    @SerializedName("commentCount")
    @Expose
    private int commentCount;
    @SerializedName("isLiked")
    @Expose
    private boolean isLiked;

    @SerializedName("isMyReel")
    @Expose
    private boolean isMyReel;
    @SerializedName("totalCount")
    @Expose
    private int totalCount;
    @SerializedName("videoUrl")
    @Expose
    private String videoUrl;
    @SerializedName("thumbnailUrl")
    @Expose
    private String thumbnailUrl;
    @SerializedName("caption")
    @Expose
    private String caption;
    @SerializedName("duration")
    @Expose
    private int duration;
    @SerializedName("hashtags")
    @Expose
    private String hashtags;

    @SerializedName("isPinned")
    @Expose
    private boolean isPinned;
    @SerializedName("isFollowing")
    @Expose
    private boolean isFollowing;


    protected ReelModel(Parcel in) {
        reelId = in.readInt();
        userId = in.readString();
        fullName = in.readString();
        profilePictureUrl = in.readString();
        viewCount = in.readInt();
        likeCount = in.readInt();
        commentCount = in.readInt();
        isLiked = in.readByte() != 0;
        isMyReel = in.readByte() != 0;
        totalCount = in.readInt();
        videoUrl = in.readString();
        thumbnailUrl = in.readString();
        caption = in.readString();
        duration = in.readInt();
        hashtags = in.readString();
        isPinned = in.readByte() != 0;
        isFollowing = in.readByte() != 0;
    }

    public static final Creator<ReelModel> CREATOR = new Creator<ReelModel>() {
        @Override
        public ReelModel createFromParcel(Parcel in) {
            return new ReelModel(in);
        }

        @Override
        public ReelModel[] newArray(int size) {
            return new ReelModel[size];
        }
    };

    public int getReelId() {
        return reelId;
    }

    public void setReelId(int reelId) {
        this.reelId = reelId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public boolean getLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getHashtags() {
        return hashtags;
    }
    public void setHashtags(String hashtags) {
        this.hashtags = hashtags;
    }

    public boolean isMyReel() {
        return isMyReel;
    }

    public void setMyReel(boolean myReel) {
        isMyReel = myReel;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(reelId);
        dest.writeString(userId);
        dest.writeString(fullName);
        dest.writeString(profilePictureUrl);
        dest.writeInt(viewCount);
        dest.writeInt(likeCount);
        dest.writeInt(commentCount);
        dest.writeByte((byte) (isLiked ? 1 : 0));
        dest.writeByte((byte) (isMyReel ? 1 : 0));
        dest.writeByte((byte) (isPinned ? 1 : 0));
        dest.writeByte((byte) (isFollowing ? 1 : 0));
        dest.writeInt(totalCount);
        dest.writeString(videoUrl);
        dest.writeString(thumbnailUrl);
        dest.writeString(caption);
        dest.writeInt(duration);
        dest.writeString(hashtags);
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }
}