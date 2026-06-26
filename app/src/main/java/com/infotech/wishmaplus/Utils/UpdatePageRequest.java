package com.infotech.wishmaplus.Utils;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UpdatePageRequest implements Serializable {
/*{
  "pageId": "string",
  "pageName": "string",
  "categoryId": "string",
  "bio": "string",
  "website": "string",
  "email": "string",
  "phone": "string",
  "address": "string"
}*/
    @SerializedName("address")
    private String address = "";

    @SerializedName("bio")
    private String bio = "";

    @SerializedName("categoryId")
    private String categoryId = "";

    @SerializedName("email")
    private String email = "";

    @SerializedName("pageId")
    private String pageId = "";

    @SerializedName("pageName")
    private String pageName = "";

    @SerializedName("phone")
    private String phone = "";

    @SerializedName("website")
    private String website = "";

    public String getAddress() {
        return address != null ? address : "";
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBio() {
        return bio != null ? bio : "";
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getCategoryId() {
        return categoryId != null ? categoryId : "";
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website != null ? website : "";
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
