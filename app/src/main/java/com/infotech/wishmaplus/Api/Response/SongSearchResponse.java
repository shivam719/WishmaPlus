package com.infotech.wishmaplus.Api.Response;

import com.google.gson.annotations.SerializedName;
import com.infotech.wishmaplus.reels.ui.componets.SongResult;

import java.util.List;

public class SongSearchResponse {
    @SerializedName("statusCode")
    public int statusCode;
    @SerializedName("responseText")
    public String responseText;
    @SerializedName("result")
    public SongResult result;

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public SongResult getResult() {
        return result;
    }

    public void setResult(SongResult result) {
        this.result = result;
    }
}