package com.utt.foodcouriers_admin.data.repository;

import android.os.Handler;
import android.os.Looper;

import com.utt.foodcouriers_admin.data.model.AdminProfile;
import com.utt.foodcouriers_admin.data.model.AdminSession;
import com.utt.foodcouriers_admin.data.model.AuthTokenResponse;
import com.utt.foodcouriers_admin.data.remote.SupabaseApi;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuthRepository {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SupabaseApi supabaseApi = new SupabaseApi();

    public boolean hasValidConfiguration() {
        return supabaseApi.hasValidConfiguration();
    }

    public void signIn(String email, String password, AuthCallback callback) {
        executorService.execute(() -> {
            try {
                AuthTokenResponse tokenResponse = supabaseApi.signIn(email, password);
                String accessToken = tokenResponse.getAccessToken();
                String userId = tokenResponse.getUser().getId();

                AdminProfile adminProfile = supabaseApi.fetchAdminProfile(userId, accessToken);
                if (!"active".equalsIgnoreCase(adminProfile.getStatus())) {
                    throw new SupabaseApi.ApiException("Tài khoản admin hiện không ở trạng thái active.");
                }
                supabaseApi.updateLastLogin(userId, accessToken);
                AdminSession adminSession = new AdminSession(
                        adminProfile,
                        tokenResponse.getAccessToken(),
                        tokenResponse.getRefreshToken()
                );
                mainHandler.post(() -> callback.onSuccess(adminSession));
            } catch (Exception exception) {
                mainHandler.post(() -> callback.onError(exception.getMessage()));
            }
        });
    }

    public interface AuthCallback {
        void onSuccess(AdminSession session);

        void onError(String message);
    }
}
