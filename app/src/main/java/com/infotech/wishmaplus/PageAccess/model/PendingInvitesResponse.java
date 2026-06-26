package com.infotech.wishmaplus.PageAccess.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

// ─── Top-level API wrapper ───────────────────────────────────────────────────

public class PendingInvitesResponse {

    @SerializedName("statusCode")
    private int statusCode;

    @SerializedName("responseText")
    private String responseText;

    @SerializedName("result")
    private List<ModeratorInvite> result;

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseText() {
        return responseText;
    }

    public List<ModeratorInvite> getResult() {
        return result;
    }

    public boolean isSuccess() {
        return statusCode == 1 && result != null;
    }
}