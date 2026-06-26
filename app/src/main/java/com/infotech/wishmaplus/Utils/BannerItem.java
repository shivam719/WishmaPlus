package com.infotech.wishmaplus.Utils;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class BannerItem implements Parcelable {

    @SerializedName("id")
    private Integer id;

    @SerializedName("title")
    private String title;

    @SerializedName("subTitle")
    private String subTitle;

    @SerializedName("buttonText")
    private String buttonText;

    @SerializedName("buttonUrl")
    private String buttonUrl;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("sortOrder")
    private Integer sortOrder;

    public BannerItem() {
    }

    protected BannerItem(Parcel in) {
        id = (Integer) in.readValue(Integer.class.getClassLoader());
        title = in.readString();
        subTitle = in.readString();
        buttonText = in.readString();
        buttonUrl = in.readString();
        imageUrl = in.readString();
        sortOrder = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(id);
        dest.writeString(title);
        dest.writeString(subTitle);
        dest.writeString(buttonText);
        dest.writeString(buttonUrl);
        dest.writeString(imageUrl);
        dest.writeValue(sortOrder);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public static final Creator<BannerItem> CREATOR = new Creator<BannerItem>() {
        @Override
        public BannerItem createFromParcel(Parcel in) {
            return new BannerItem(in);
        }

        @Override
        public BannerItem[] newArray(int size) {
            return new BannerItem[size];
        }
    };

    // Null Safe Getters

    public int getId() {
        return id != null ? id : 0;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public String getSubTitle() {
        return subTitle != null ? subTitle : "";
    }

    public String getButtonText() {
        return buttonText != null ? buttonText : "";
    }

    public String getButtonUrl() {
        return buttonUrl != null ? buttonUrl : "";
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public int getSortOrder() {
        return sortOrder != null ? sortOrder : 0;
    }
}