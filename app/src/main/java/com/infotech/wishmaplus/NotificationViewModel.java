package com.infotech.wishmaplus;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NotificationViewModel extends AndroidViewModel {

    // Singleton instance
    private static NotificationViewModel INSTANCE;

    private final MutableLiveData<Boolean> newNotification = new MutableLiveData<>();

    public NotificationViewModel(@NonNull Application application) {
        super(application);
        INSTANCE = this;
    }
    public static void notifyNewNotification() {
        if (INSTANCE != null) {
            INSTANCE.newNotification.postValue(true);
        }
    }

    public LiveData<Boolean> getNewNotification() {
        return newNotification;
    }
}
