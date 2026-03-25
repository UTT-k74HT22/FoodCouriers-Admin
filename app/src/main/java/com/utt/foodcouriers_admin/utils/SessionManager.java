package com.utt.foodcouriers_admin.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.utt.foodcouriers_admin.data.model.AdminProfile;
import com.utt.foodcouriers_admin.data.model.AdminSession;

public class SessionManager {

    private static final String PREF_NAME = "foodcouriers_admin_session";
    private static final String KEY_PROFILE = "key_profile";
    private static final String KEY_ACCESS_TOKEN = "key_access_token";
    private static final String KEY_REFRESH_TOKEN = "key_refresh_token";
    private static final String KEY_REMEMBERED_EMAIL = "key_remembered_email";

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(AdminSession session) {
        sharedPreferences.edit()
                .putString(KEY_PROFILE, gson.toJson(session.getProfile()))
                .putString(KEY_ACCESS_TOKEN, session.getAccessToken())
                .putString(KEY_REFRESH_TOKEN, session.getRefreshToken())
                .apply();
    }

    public boolean hasSession() {
        return !getAccessToken().isBlank() && getProfile() != null;
    }

    public AdminProfile getProfile() {
        String rawProfile = sharedPreferences.getString(KEY_PROFILE, "");
        if (rawProfile == null || rawProfile.isBlank()) {
            return null;
        }
        return gson.fromJson(rawProfile, AdminProfile.class);
    }

    public AdminSession getSession() {
        AdminProfile profile = getProfile();
        if (profile == null) {
            return null;
        }
        return new AdminSession(
                profile,
                sharedPreferences.getString(KEY_ACCESS_TOKEN, ""),
                sharedPreferences.getString(KEY_REFRESH_TOKEN, "")
        );
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, "");
    }

    public void rememberEmail(String email) {
        sharedPreferences.edit().putString(KEY_REMEMBERED_EMAIL, email).apply();
    }

    public String getRememberedEmail() {
        return sharedPreferences.getString(KEY_REMEMBERED_EMAIL, "");
    }

    public void clearRememberedEmail() {
        sharedPreferences.edit().remove(KEY_REMEMBERED_EMAIL).apply();
    }

    public void clearSession() {
        sharedPreferences.edit()
                .remove(KEY_PROFILE)
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply();
    }
}
