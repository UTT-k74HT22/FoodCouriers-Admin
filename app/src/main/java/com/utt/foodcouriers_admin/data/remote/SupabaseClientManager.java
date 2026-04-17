package com.utt.foodcouriers_admin.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.utt.foodcouriers_admin.utils.SessionManager;

/**
 * Manager to handle session initialization for all Supabase clients.
 */
public class SupabaseClientManager {

    private static final String TAG = "SupabaseClientManager";
    private static final long TOKEN_REFRESH_SKEW_MILLIS = 5 * 60 * 1000L;
    private static SessionManager sessionManager;
    private static boolean isRefreshing = false;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Runnable scheduledRefresh;

    public static void initializeClients(Context context) {
        sessionManager = SessionManager.getInstance(context);
        if (sessionManager.isLoggedIn()) {
            String accessToken = sessionManager.getAccessToken();
            String refreshToken = sessionManager.getRefreshToken();
            
            updateAllClients(accessToken, refreshToken);
            scheduleTokenRefresh();
        }
    }

    public static void updateAllClients(String accessToken, String refreshToken) {
        AuthClient.getInstance().setSession(accessToken, refreshToken);
        RestaurantClient.getInstance().setSession(accessToken, refreshToken);
        OrderClient.getInstance().setSession(accessToken, refreshToken);
        SupabaseRealtimeClient.getInstance().setAccessToken(accessToken);
    }

    public static void clearAllClients() {
        AuthClient.getInstance().clearSession();
        RestaurantClient.getInstance().clearSession();
        OrderClient.getInstance().clearSession();
        SupabaseRealtimeClient.getInstance().disconnect();
        cancelScheduledRefresh();
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

        refreshTokenNow(callback);
    }

    public static void refreshTokenNow(BaseSupabaseClient.ApiCallback<Boolean> callback) {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            postSuccess(callback, false);
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
                    scheduleTokenRefresh();
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

    private static void scheduleTokenRefresh() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            cancelScheduledRefresh();
            return;
        }

        cancelScheduledRefresh();

        long expiresAt = sessionManager.getTokenExpiresAt();
        if (expiresAt <= 0) {
            return;
        }

        long delayMillis = Math.max(0L, expiresAt - System.currentTimeMillis() - TOKEN_REFRESH_SKEW_MILLIS);
        scheduledRefresh = () -> refreshTokenIfNeeded(new BaseSupabaseClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (Boolean.FALSE.equals(result)) {
                    Log.w(TAG, "Scheduled token refresh did not refresh the session");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Scheduled token refresh failed: " + error);
            }
        });
        mainHandler.postDelayed(scheduledRefresh, delayMillis);
    }

    private static void cancelScheduledRefresh() {
        if (scheduledRefresh != null) {
            mainHandler.removeCallbacks(scheduledRefresh);
            scheduledRefresh = null;
        }
    }
}
