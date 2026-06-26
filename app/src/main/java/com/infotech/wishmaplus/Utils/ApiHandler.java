package com.infotech.wishmaplus.Utils;

import android.app.Activity;

import retrofit2.Response;

public class ApiHandler {

    public static <T> void handleResponse(Response<T> response, Activity context, ApiSuccess<T> successCallback) {

        try {
            if (!response.isSuccessful()) {

                String message = switch (response.code()) {
                    case 404 -> "API Not Found (404)";
                    case 500 -> "Internal Server Error (500)";
                    case 401 -> "Unauthorized Access (401)";
                    default -> "Server Error: " + response.code();
                };

                UtilMethods.INSTANCE.Error(context, message);
                return;
            }

            if (response.body() == null) {
                UtilMethods.INSTANCE.Error(context, "Empty response from server");
                return;
            }
            successCallback.onSuccess(response.body());

        } catch (Exception e) {
            UtilMethods.INSTANCE.Error(context, e.getMessage());
        }
    }

    // Callback Interface
    public interface ApiSuccess<T> {
        void onSuccess(T response);
    }
}