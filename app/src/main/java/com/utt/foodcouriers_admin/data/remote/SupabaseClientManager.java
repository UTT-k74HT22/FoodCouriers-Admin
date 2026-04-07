package com.utt.foodcouriers_admin.data.remote;

import android.content.Context;
import android.util.Log;

import com.utt.foodcouriers_admin.utils.SessionManager;

/**
 * Manager to handle session initialization for all Supabase clients.
 */
public class SupabaseClientManager {

    private static final String TAG = "SupabaseClientManager";
    private static SessionManager sessionManager;
    private static boolean isRefreshing = false;

    public static void initializeClients(Context context) {
        sessionManager = SessionManager.getInstance(context);
        if (sessionManager.isLoggedIn()) {
            String accessToken = sessionManager.getAccessToken();
            String refreshToken = sessionManager.getRefreshToken();
            
            updateAllClients(accessToken, refreshToken);
        }
    }

    public static void updateAllClients(String accessToken, String refreshToken) {
        AuthClient.getInstance().setSession(accessToken, refreshToken);
        MenuClient.getInstance().setSession(accessToken, refreshToken);
        RestaurantClient.getInstance().setSession(accessToken, refreshToken);
    }

    public static void clearAllClients() {
        AuthClient.getInstance().clearSession();
        MenuClient.getInstance().clearSession();
        RestaurantClient.getInstance().clearSession();
    }

    public static void refreshTokenIfNeeded(BaseSupabaseClient.ApiCallback<Boolean> callback) {
        if (sessionManager == null) {
            postSuccess(callback, false);
            return;
        }

        if (!sessionManager.isLoggedIn()) {
            postSuccess(callback, false);
            return;
        }

        if (!sessionManager.isTokenExpired()) {
            postSuccess(callback, true);
            return;
        }

        if (isRefreshing) {
            postSuccess(callback, false);
            return;
        }

        isRefreshing = true;
        Log.d(TAG, "Token expired, refreshing...");

        AuthClient.getInstance().refreshToken(new BaseSupabaseClient.ApiCallback<AuthClient.AuthResponse>() {
            @Override
            public void onSuccess(AuthClient.AuthResponse result) {
                isRefreshing = false;
                if (result != null && result.getAccessToken() != null) {
                    String newAccessToken = result.getAccessToken();
                    String newRefreshToken = result.getRefreshToken();
                    long expiresIn = (result.getExpiresIn() != null ? result.getExpiresIn() : 3600L) * 1000L;

                    updateAllClients(newAccessToken, newRefreshToken);
                    sessionManager.updateSession(newAccessToken, newRefreshToken, expiresIn);
                    Log.d(TAG, "Token refreshed successfully");
                    postSuccess(callback, true);
                } else {
                    Log.e(TAG, "Refresh response invalid");
                    postSuccess(callback, false);
                }
            }

            @Override
            public void onError(String error) {
                isRefreshing = false;
                Log.e(TAG, "Token refresh failed: " + error);
                postSuccess(callback, false);
            }
        });
    }

    private static <T> void postSuccess(BaseSupabaseClient.ApiCallback<T> callback, T result) {
        if (callback != null) {
            callback.onSuccess(result);
        }
    }
}
