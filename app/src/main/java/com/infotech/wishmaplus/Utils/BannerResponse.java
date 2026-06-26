package com.infotech.wishmaplus.Utils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class BannerResponse {

    @SerializedName("statusCode")
    private Integer statusCode;

    @SerializedName("responseText")
    private String responseText;

    @SerializedName("result")
    private List<BannerItem> result;

    public Integer getStatusCode() {
        return statusCode != null ? statusCode : 0;
    }

    public String getResponseText() {
        return responseText != null ? responseText : "";
    }

    public List<BannerItem> getResult() {
        return result != null ? result : new ArrayList<>();
    }
}