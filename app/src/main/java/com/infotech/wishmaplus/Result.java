package com.infotech.wishmaplus;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;
import com.google.gson.annotations.SerializedName;
import com.infotech.wishmaplus.zego.DateWiseItem;

public class Result implements Parcelable {

	@SerializedName("summary")
	private Summary summary;

	@SerializedName("dateWise")
	private List<DateWiseItem> dateWise;

	protected Result(Parcel in) {
		dateWise = in.createTypedArrayList(DateWiseItem.CREATOR);
	}

	public static final Creator<Result> CREATOR = new Creator<Result>() {
		@Override
		public Result createFromParcel(Parcel in) {
			return new Result(in);
		}

		@Override
		public Result[] newArray(int size) {
			return new Result[size];
		}
	};

	public Summary getSummary(){
		return summary;
	}

	public List<DateWiseItem> getDateWise(){
		return dateWise;
	}

	@Override
	public int describeContents() {
		return hashCode();
	}

	@Override
	public void writeToParcel(@NonNull Parcel dest, int flags) {
		dest.writeTypedList(dateWise);
	}
}