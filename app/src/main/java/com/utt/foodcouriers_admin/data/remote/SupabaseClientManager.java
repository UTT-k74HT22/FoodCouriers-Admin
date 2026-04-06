package com.utt.foodcouriers_admin.data.remote;

import android.content.Context;
import com.utt.foodcouriers_admin.utils.SessionManager;

/**
 * Manager to handle session initialization for all Supabase clients.
 */
public class SupabaseClientManager {

    public static void initializeClients(Context context) {
        SessionManager sessionManager = SessionManager.getInstance(context);
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
        // Add other clients here as they are created
    }

    public static void clearAllClients() {
        AuthClient.getInstance().clearSession();
        MenuClient.getInstance().clearSession();
        RestaurantClient.getInstance().clearSession();
    }
}
