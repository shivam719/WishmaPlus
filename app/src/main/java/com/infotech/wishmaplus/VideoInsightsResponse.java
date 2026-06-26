package com.infotech.wishmaplus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class VideoInsightsResponse implements Parcelable {

	@SerializedName("result")
	private Result result;

	@SerializedName("responseText")
	private String responseText;

	@SerializedName("statusCode")
	private int statusCode;

	protected VideoInsightsResponse(Parcel in) {
		responseText = in.readString();
		statusCode = in.readInt();
	}

	public static final Creator<VideoInsightsResponse> CREATOR = new Creator<VideoInsightsResponse>() {
		@Override
		public VideoInsightsResponse createFromParcel(Parcel in) {
			return new VideoInsightsResponse(in);
		}

		@Override
		public VideoInsightsResponse[] newArray(int size) {
			return new VideoInsightsResponse[size];
		}
	};

	public Result getResult(){
		return result;
	}

	public String getResponseText(){
		return responseText;
	}

	public int getStatusCode(){
		return statusCode;
	}

	@Override
	public int describeContents() {
		return hashCode();
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeString(responseText);
		dest.writeInt(statusCode);
	}
}