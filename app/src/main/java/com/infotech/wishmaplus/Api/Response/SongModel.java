package com.infotech.wishmaplus.Api.Response;

import com.google.gson.annotations.SerializedName;

public class SongModel {
    @SerializedName("songId")
    public int songId;
    @SerializedName("title")
    public String title;
    @SerializedName("artist")
    public String artist;
    @SerializedName("genre")
    public String genre;
    @SerializedName("audioUrl")
    public String audioUrl;
    @SerializedName("coverImageUrl")
    public String coverImageUrl;
    @SerializedName("durationSec")
    public int durationSec;

    public int getSongId() {
        return songId;
    }

    public void setSongId(int songId) {
        this.songId = songId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public int getDurationSec() {
        return durationSec;
    }

    public void setDurationSec(int durationSec) {
        this.durationSec = durationSec;
    }
}
