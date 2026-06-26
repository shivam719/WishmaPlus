package com.infotech.wishmaplus.Utils;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GetPageInsightsResponse {

    @SerializedName("statusCode")
    private int statusCode;

    @SerializedName("responseText")
    private String responseText;

    @SerializedName("result")
    private Result result;

    public int getStatusCode() { return statusCode; }
    public String getResponseText() { return responseText; }
    public Result getResult() { return result; }

    // ── Result ────────────────────────────────────────────────────────────
    public static class Result {
        @SerializedName("summary")
        private Summary summary;

        @SerializedName("topPosts")
        private List<TopPost> topPosts;

        public Summary getSummary() { return summary; }
        public List<TopPost> getTopPosts() { return topPosts; }
    }

    // ── Summary ───────────────────────────────────────────────────────────
    public static class Summary {
        @SerializedName("impressions")
        private int impressions;

        @SerializedName("impressionsGrowth")
        private double impressionsGrowth;

        @SerializedName("pageLikes")
        private int pageLikes;

        @SerializedName("pageLikesGrowth")
        private double pageLikesGrowth;

        @SerializedName("newPageLikes")
        private int newPageLikes;

        @SerializedName("newPageLikesGrowth")
        private double newPageLikesGrowth;

        @SerializedName("viralReach")
        private int viralReach;

        @SerializedName("viralReachGrowth")
        private double viralReachGrowth;

        @SerializedName("reach")
        private int reach;

        @SerializedName("reachGrowth")
        private double reachGrowth;

        @SerializedName("engagement")
        private int engagement;

        @SerializedName("engagementGrowth")
        private double engagementGrowth;

        @SerializedName("adClicks")
        private int adClicks;

        @SerializedName("adClicksGrowth")
        private double adClicksGrowth;

        @SerializedName("actions")
        private int actions;

        @SerializedName("actionsGrowth")
        private double actionsGrowth;

        public int getImpressions() { return impressions; }
        public double getImpressionsGrowth() { return impressionsGrowth; }
        public int getPageLikes() { return pageLikes; }
        public double getPageLikesGrowth() { return pageLikesGrowth; }
        public int getNewPageLikes() { return newPageLikes; }
        public double getNewPageLikesGrowth() { return newPageLikesGrowth; }
        public int getViralReach() { return viralReach; }
        public double getViralReachGrowth() { return viralReachGrowth; }
        public int getReach() { return reach; }
        public double getReachGrowth() { return reachGrowth; }
        public int getEngagement() { return engagement; }
        public double getEngagementGrowth() { return engagementGrowth; }
        public int getAdClicks() { return adClicks; }
        public double getAdClicksGrowth() { return adClicksGrowth; }
        public int getActions() { return actions; }
        public double getActionsGrowth() { return actionsGrowth; }
    }

    // ── TopPost ───────────────────────────────────────────────────────────
    public static class TopPost {
        @SerializedName("postId")
        private String postId;

        @SerializedName("caption")
        private String caption;

        @SerializedName("thumbnailUrl")
        private String thumbnailUrl;

        @SerializedName("createdAt")
        private String createdAt;

        @SerializedName("reach")
        private int reach;

        @SerializedName("likes")
        private int likes;

        @SerializedName("comments")
        private int comments;

        @SerializedName("shares")
        private int shares;

        public String getPostId() { return postId; }
        public String getCaption() { return caption; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public String getCreatedAt() { return createdAt; }
        public int getReach() { return reach; }
        public int getLikes() { return likes; }
        public int getComments() { return comments; }
        public int getShares() { return shares; }
    }
}