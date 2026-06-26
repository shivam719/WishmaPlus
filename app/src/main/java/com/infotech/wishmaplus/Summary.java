package com.infotech.wishmaplus;

import com.google.gson.annotations.SerializedName;

public class Summary{

	@SerializedName("threeSecondViews")
	private int threeSecondViews;

	@SerializedName("engagement")
	private int engagement;

	@SerializedName("oneMinuteViews")
	private int oneMinuteViews;

	@SerializedName("netFollowers")
	private int netFollowers;

	@SerializedName("minutesViewed")
	private int minutesViewed;

	public int getThreeSecondViews(){
		return threeSecondViews;
	}

	public int getEngagement(){
		return engagement;
	}

	public int getOneMinuteViews(){
		return oneMinuteViews;
	}

	public int getNetFollowers(){
		return netFollowers;
	}

	public int getMinutesViewed(){
		return minutesViewed;
	}
}