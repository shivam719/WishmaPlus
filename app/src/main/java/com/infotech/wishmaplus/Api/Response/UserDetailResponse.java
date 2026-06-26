package com.infotech.wishmaplus.Api.Response;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.infotech.wishmaplus.Api.Object.GroupResult;
import com.infotech.wishmaplus.Api.Object.PackageResult;

public class UserDetailResponse implements Parcelable {

    public static final Creator<UserDetailResponse> CREATOR = new Creator<UserDetailResponse>() {
        @Override
        public UserDetailResponse createFromParcel(Parcel in) {
            return new UserDetailResponse(in);
        }

        @Override
        public UserDetailResponse[] newArray(int size) {
            return new UserDetailResponse[size];
        }
    };
    @SerializedName("bankId")
    @Expose
    public int bankId;
    @SerializedName("branchName")
    @Expose
    public String branchName;
    @SerializedName("accountNumber")
    @Expose
    public String accountNumber;
    @SerializedName("ifsc")
    @Expose
    public String ifsc;
    @SerializedName("accountHolder")
    @Expose
    public String accountHolder;
    @SerializedName("website")
    @Expose
    public String website;
    @SerializedName("balance")
    @Expose
    private double balance;
    @SerializedName("accessLink")
    @Expose
    private String accessLink;
    @SerializedName("refreshToken")
    @Expose
    private String refreshToken;
    @SerializedName("refreshTokenExpiryTime")
    @Expose
    private String refreshTokenExpiryTime;
    @SerializedName("isActive")
    @Expose
    private boolean isActive;
    @SerializedName("isSelfProfile")
    @Expose
    private boolean isSelfProfile;
    @SerializedName("isProfessional")
    @Expose
    private boolean isProfessional;
    @SerializedName("isShowProfessionalDashboard")
    @Expose
    private boolean isShowProfessionalDashboard;
    @SerializedName("canManageContent")
    @Expose
    private boolean canManageContent;
    @SerializedName("isRequestPending")
    @Expose
    private boolean isRequestPending;
    @SerializedName("id")
    @Expose
    private int id;
    @SerializedName("referralID")
    @Expose
    private int referralID;
    @SerializedName("prefix")
    @Expose
    private String prefix;
    @SerializedName("referralUrl")
    @Expose
    private String referralUrl;
    @SerializedName("referralIdStr")
    @Expose
    private String referralIdStr;
    @SerializedName("referralAmount")
    @Expose
    private double referralAmount;
    @SerializedName("userId")
    @Expose
    private String userId;
    @SerializedName("mobileNo")
    @Expose
    private String mobileNo;
    @SerializedName("role")
    @Expose
    private int role;
    @SerializedName("fisrtName")
    @Expose
    private String fisrtName;
    @SerializedName("lastName")
    @Expose
    private String lastName;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("gAuthPin")
    @Expose
    private String gAuthPin;
    @SerializedName("password")
    @Expose
    private String password;
    @SerializedName("profilePictureUrl")
    @Expose
    private String profilePictureUrl;
    @SerializedName("coverPictureUrl")
    @Expose
    private String coverPictureUrl;
    @SerializedName("file")
    @Expose
    private String file;
    @SerializedName("genderId")
    @Expose
    private int genderId;
    @SerializedName("gender")
    @Expose
    private String gender;
    @SerializedName("cityId")
    @Expose
    private int cityId;
    @SerializedName("stateId")
    @Expose
    private int stateId;
    @SerializedName("requestSentStatus")
    @Expose
    private int requestSentStatus;
    @SerializedName("cityName")
    @Expose
    private String cityName;
    @SerializedName("stateName")
    @Expose
    private String stateName;
    @SerializedName("bio")
    @Expose
    private String bio;
    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("following")
    @Expose
    private String following;
    @SerializedName("follower")
    @Expose
    private String follower;
    @SerializedName("isFollowed")
    @Expose
    private String isFollowed;
    @SerializedName("joiningDate")
    @Expose
    private String joiningDate;
    @SerializedName("totalDownline")
    @Expose
    private int totalDownline;
    @SerializedName("paidDownline")
    @Expose
    private int paidDownline;
    @SerializedName("unpaidDownline")
    @Expose
    private int unpaidDownline;
    @SerializedName("packageDetail")
    @Expose
    private PackageResult packageDetail;
    @SerializedName("groupDetails")
    @Expose
    private GroupResult result;
    @SerializedName("categoryName")
    private String categoryName;
    @SerializedName("pageCategoryId")
    private String pageCategoryId;
    @SerializedName("dob")
    private String dob;

    protected UserDetailResponse(Parcel in) {
        balance = in.readDouble();
        accessLink = in.readString();
        refreshToken = in.readString();
        refreshTokenExpiryTime = in.readString();
        isActive = in.readByte() != 0;
        isSelfProfile = in.readByte() != 0;
        isProfessional = in.readByte() != 0;
        isShowProfessionalDashboard = in.readByte() != 0;
        canManageContent = in.readByte() != 0;
        isRequestPending = in.readByte() != 0;
        id = in.readInt();
        referralID = in.readInt();
        prefix = in.readString();
        referralUrl = in.readString();
        referralIdStr = in.readString();
        referralAmount = in.readDouble();
        userId = in.readString();
        mobileNo = in.readString();
        role = in.readInt();
        fisrtName = in.readString();
        lastName = in.readString();
        email = in.readString();
        gAuthPin = in.readString();
        password = in.readString();
        profilePictureUrl = in.readString();
        coverPictureUrl = in.readString();
        file = in.readString();
        genderId = in.readInt();
        gender = in.readString();
        cityId = in.readInt();
        stateId = in.readInt();
        requestSentStatus = in.readInt();
        cityName = in.readString();
        stateName = in.readString();
        bio = in.readString();
        address = in.readString();
        following = in.readString();
        follower = in.readString();
        isFollowed = in.readString();
        joiningDate = in.readString();
        totalDownline = in.readInt();
        paidDownline = in.readInt();
        unpaidDownline = in.readInt();
        packageDetail = in.readParcelable(PackageResult.class.getClassLoader());
        bankId = in.readInt();
        branchName = in.readString();
        accountNumber = in.readString();
        ifsc = in.readString();
        accountHolder = in.readString();
        website = in.readString();
        result = in.readParcelable(GroupResult.class.getClassLoader());
        categoryName = in.readString();
        pageCategoryId = in.readString();
        dob = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(balance);
        dest.writeString(accessLink);
        dest.writeString(refreshToken);
        dest.writeString(refreshTokenExpiryTime);
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeByte((byte) (isSelfProfile ? 1 : 0));
        dest.writeByte((byte) (isProfessional ? 1 : 0));
        dest.writeByte((byte) (isShowProfessionalDashboard ? 1 : 0));
        dest.writeByte((byte) (canManageContent ? 1 : 0));
        dest.writeByte((byte) (isRequestPending ? 1 : 0));
        dest.writeInt(id);
        dest.writeInt(referralID);
        dest.writeString(prefix);
        dest.writeString(referralUrl);
        dest.writeString(referralIdStr);
        dest.writeDouble(referralAmount);
        dest.writeString(userId);
        dest.writeString(mobileNo);
        dest.writeInt(role);
        dest.writeString(fisrtName);
        dest.writeString(lastName);
        dest.writeString(email);
        dest.writeString(gAuthPin);
        dest.writeString(password);
        dest.writeString(profilePictureUrl);
        dest.writeString(coverPictureUrl);
        dest.writeString(file);
        dest.writeInt(genderId);
        dest.writeString(gender);
        dest.writeInt(cityId);
        dest.writeInt(stateId);
        dest.writeInt(requestSentStatus);
        dest.writeString(cityName);
        dest.writeString(stateName);
        dest.writeString(bio);
        dest.writeString(address);
        dest.writeString(following);
        dest.writeString(follower);
        dest.writeString(isFollowed);
        dest.writeString(joiningDate);
        dest.writeInt(totalDownline);
        dest.writeInt(paidDownline);
        dest.writeInt(unpaidDownline);
        dest.writeParcelable(packageDetail, flags);
        dest.writeInt(bankId);
        dest.writeString(branchName);
        dest.writeString(accountNumber);
        dest.writeString(ifsc);
        dest.writeString(accountHolder);
        dest.writeString(website);
        dest.writeParcelable(result, flags);
        dest.writeString(categoryName);
        dest.writeString(pageCategoryId);
        dest.writeString(dob);
    }

    @Override
    public int describeContents() {
        return hashCode();
    }

    public double getBalance() {
        return balance;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getRefreshTokenExpiryTime() {
        return refreshTokenExpiryTime;
    }

    public boolean isActive() {
        return isActive;
    }


    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public int getRole() {
        return role;
    }

    public String getFisrtName() {
        return fisrtName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getgAuthPin() {
        return gAuthPin;
    }

    public String getPassword() {
        return password;
    }

    public String getAccessLink() {
        return accessLink;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getCoverPictureUrl() {
        return coverPictureUrl;
    }

    public void setCoverPictureUrl(String coverPictureUrl) {
        this.coverPictureUrl = coverPictureUrl;
    }

    public String getFile() {
        return file;
    }

    public int getGenderId() {
        return genderId;
    }

    public String getGender() {
        return gender;
    }

    public int getCityId() {
        return cityId;
    }

    public int getStateId() {
        return stateId;
    }


    public String getCityName() {
        return cityName;
    }

    public String getStateName() {
        return stateName;
    }

    public String getBio() {
        return bio;
    }

    public String getAddress() {
        return address;
    }

    public String getJoiningDate() {
        return joiningDate;
    }

    public int getTotalDownline() {
        return totalDownline;
    }

    public int getPaidDownline() {
        return paidDownline;
    }

    public int getUnpaidDownline() {
        return unpaidDownline;
    }

    public int getReferralID() {
        return referralID;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getReferralUrl() {
        return referralUrl;
    }

    public String getReferralIdStr() {
        return referralIdStr;
    }

    public double getReferralAmount() {
        return referralAmount;
    }

    public PackageResult getPackageDetail() {
        return packageDetail;
    }

    public int getBankId() {
        return bankId;
    }


    public String getBranchName() {
        return branchName;
    }


    public String getAccountNumber() {
        return accountNumber;
    }


    public String getIfsc() {
        return ifsc;
    }


    public String getAccountHolder() {
        return accountHolder;
    }

    public String getFollowing() {
        return following;
    }

    public String getFollower() {
        return follower;
    }

    public String getIsFollowed() {
        return isFollowed;
    }

    public void setIsFollowed(String isFollowed) {
        this.isFollowed = isFollowed;
    }

    public int getRequestSentStatus() {
        return requestSentStatus;
    }

    public void setRequestSentStatus(int requestSentStatus) {
        this.requestSentStatus = requestSentStatus;
    }

    public boolean isRequestPending() {
        return isRequestPending;
    }

    public void setRequestPending(boolean requestPending) {
        isRequestPending = requestPending;
    }

    public boolean isProfessional() {
        return isProfessional;
    }

    public void setProfessional(boolean professional) {
        isProfessional = professional;
    }

    public boolean isShowProfessionalDashboard() {
        return isShowProfessionalDashboard;
    }

    public void setShowProfessionalDashboard(boolean ShowProfessionalDashboard) {
        isShowProfessionalDashboard = ShowProfessionalDashboard;
    }

    public boolean isSelfProfile() {
        return isSelfProfile;
    }

    public void setSelfProfile(boolean selfProfile) {
        isSelfProfile = selfProfile;
    }

    public GroupResult getResult() {
        return result;
    }

    public void setResult(GroupResult result) {
        this.result = result;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCategoryName() {
        return categoryName != null ? categoryName : "";
    }

    public String getPageCategoryId() {
        return pageCategoryId != null ? pageCategoryId : "";
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public void setBankId(int bankId) {
        this.bankId = bankId;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setIfsc(String ifsc) {
        this.ifsc = ifsc;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setAccessLink(String accessLink) {
        this.accessLink = accessLink;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setRefreshTokenExpiryTime(String refreshTokenExpiryTime) {
        this.refreshTokenExpiryTime = refreshTokenExpiryTime;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isCanManageContent() {
        return canManageContent;
    }

    public void setCanManageContent(boolean canManageContent) {
        this.canManageContent = canManageContent;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setReferralID(int referralID) {
        this.referralID = referralID;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setReferralUrl(String referralUrl) {
        this.referralUrl = referralUrl;
    }

    public void setReferralIdStr(String referralIdStr) {
        this.referralIdStr = referralIdStr;
    }

    public void setReferralAmount(double referralAmount) {
        this.referralAmount = referralAmount;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public void setFisrtName(String fisrtName) {
        this.fisrtName = fisrtName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setgAuthPin(String gAuthPin) {
        this.gAuthPin = gAuthPin;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public void setGenderId(int genderId) {
        this.genderId = genderId;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setCityId(int cityId) {
        this.cityId = cityId;
    }

    public void setStateId(int stateId) {
        this.stateId = stateId;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setFollowing(String following) {
        this.following = following;
    }

    public void setFollower(String follower) {
        this.follower = follower;
    }

    public void setJoiningDate(String joiningDate) {
        this.joiningDate = joiningDate;
    }

    public void setTotalDownline(int totalDownline) {
        this.totalDownline = totalDownline;
    }

    public void setPaidDownline(int paidDownline) {
        this.paidDownline = paidDownline;
    }

    public void setUnpaidDownline(int unpaidDownline) {
        this.unpaidDownline = unpaidDownline;
    }

    public void setPackageDetail(PackageResult packageDetail) {
        this.packageDetail = packageDetail;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setPageCategoryId(String pageCategoryId) {
        this.pageCategoryId = pageCategoryId;
    }
}