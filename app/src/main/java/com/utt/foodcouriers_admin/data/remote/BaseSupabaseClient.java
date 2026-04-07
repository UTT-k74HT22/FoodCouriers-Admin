package com.utt.foodcouriers_admin.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Class cơ sở cho tất cả các Supabase clients.
 * Đóng gói các logic chung như HTTP client, Gson instance, main thread handler,
 * và các tiện ích xử lý lỗi.
 */
public abstract class BaseSupabaseClient {
    
    protected static final String TAG = "BaseSupabaseClient";
    
    protected final OkHttpClient client;
    protected final Gson gson;
    protected final Handler mainHandler;
    
    protected String accessToken;
    protected String refreshToken;
    
    protected BaseSupabaseClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new GsonBuilder().setLenient().create();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setSession(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    public void clearSession() {
        this.accessToken = null;
        this.refreshToken = null;
    }
    
    public boolean isAuthenticated() {
        return accessToken != null && !accessToken.isEmpty();
    }
    
    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    protected boolean needsRefresh() {
        return refreshToken != null && !refreshToken.isEmpty();
    }

    public void refreshTokenIfNeeded(ApiCallback<Boolean> callback) {
        SupabaseClientManager.refreshTokenIfNeeded(callback);
    }
    
    /**
     * Interface xử lý callback bất đồng bộ.
     * @param <T> Kiểu dữ liệu kết quả mong đợi.
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    /**
     * Parse lỗi authentication từ JSON response.
     * @param json Response body dạng String.
     * @return Thông báo lỗi thân thiện với người dùng.
     */
    protected String parseAuthError(String json) {
        try {
            Log.e(TAG, "Raw Auth Error Response: " + json);
            return "Authentication failed. Please check your credentials.";
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse auth error", e);
            return "Authentication failed";
        }
    }

    /**
     * Parse lỗi REST API từ JSON response.
     * @param fallbackMessage Thông báo mặc định nếu parse thất bại.
     * @param statusCode HTTP status code.
     * @param json Response body dạng String.
     * @return Thông báo lỗi thân thiện với người dùng.
     */
    protected String parseRestError(String fallbackMessage, int statusCode, String json) {
        try {
            AuthError error = gson.fromJson(json, AuthError.class);
            if (error != null) {
                if (error.getMessage() != null && !error.getMessage().isBlank()) {
                    return error.getMessage();
                }
                if (error.getMsg() != null && !error.getMsg().isBlank()) {
                    return error.getMsg();
                }
                if (error.getErrorDescription() != null && !error.getErrorDescription().isBlank()) {
                    return error.getErrorDescription();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse REST error", e);
        }

        Log.d(TAG, "REST request failed with status=" + statusCode + " body=" + json);
        return fallbackMessage + " (Status: " + statusCode + ")";
    }
    
    /**
     * Đưa success callback về main thread.
     * @param callback Callback cần thực thi.
     * @param result Dữ liệu kết quả.
     */
    protected <T> void postSuccess(ApiCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }
    
    /**
     * Đưa error callback về main thread.
     * @param callback Callback cần thực thi.
     * @param error Thông báo lỗi.
     */
    protected <T> void postError(ApiCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
    
    // --- Các inner class chung ---
    
    protected static class AuthResponse { 
        @com.google.gson.annotations.SerializedName("access_token") private String accessToken; 
        @com.google.gson.annotations.SerializedName("refresh_token") private String refreshToken; 
        private AuthUser user; 
        public String getAccessToken() { return accessToken; } 
        public String getRefreshToken() { return refreshToken; } 
        public AuthUser getUser() { return user; } 
    }
    protected static class AuthUser { 
        private String id; 
        public String getId() { return id; } 
    }
    protected static class AuthError { 
        private String message; 
        private String msg; 
        @com.google.gson.annotations.SerializedName("error_description") private String errorDescription; 
        public String getMessage() { return message; } 
        public String getMsg() { return msg; } 
        public String getErrorDescription() { return errorDescription; } 
    }

}
