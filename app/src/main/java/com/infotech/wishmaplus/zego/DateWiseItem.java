package com.infotech.wishmaplus.zego;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class DateWiseItem implements Parcelable {

	@SerializedName("engagement")
	private int engagement;

	@SerializedName("insightDate")
	private String insightDate;

	@SerializedName("minutesViewed")
	private int minutesViewed;

	@SerializedName("views")
	private int views;

	protected DateWiseItem(Parcel in) {
		engagement = in.readInt();
		insightDate = in.readString();
		minutesViewed = in.readInt();
		views = in.readInt();
	}

	public static final Creator<DateWiseItem> CREATOR = new Creator<DateWiseItem>() {
		@Override
		public DateWiseItem createFromParcel(Parcel in) {
			return new DateWiseItem(in);
		}

		@Override
		public DateWiseItem[] newArray(int size) {
			return new DateWiseItem[size];
		}
	};

	public int getEngagement(){
		return engagement;
	}

	public String getInsightDate(){
		return insightDate;
	}

	public int getMinutesViewed(){
		return minutesViewed;
	}

	public int getViews(){
		return views;
	}

	@Override
	public int describeContents() {
		return hashCode();
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeInt(engagement);
		dest.writeString(insightDate);
		dest.writeInt(minutesViewed);
		dest.writeInt(views);
	}
}