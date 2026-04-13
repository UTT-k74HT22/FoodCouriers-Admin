package com.utt.foodcouriers_admin.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.utt.foodcouriers_admin.data.model.User;

public class SessionManager {
    
    private static final String PREF_NAME = "FoodCouriersAdminPrefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_AVATAR = "user_avatar";
    private static final String KEY_USER_IS_ACTIVE = "user_is_active";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_TOKEN_EXPIRES_AT = "token_expires_at";
    
    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;
    
    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }
    
    public void saveSession(String accessToken, String refreshToken, User user) {
        saveSession(accessToken, refreshToken, user, 3600000L);
    }

    public void saveSession(String accessToken, String refreshToken, User user, long expiresInMillis) {
        long expiresAt = System.currentTimeMillis() + expiresInMillis;
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_NAME, user.getFullName())
                .putString(KEY_USER_ROLE, user.getRole())
                .putString(KEY_USER_PHONE, user.getPhone())
                .putString(KEY_USER_AVATAR, user.getAvatarUrl())
                .putBoolean(KEY_USER_IS_ACTIVE, user.isActive())
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
                .apply();
    }
    
    public void updateUserInfo(User user) {
        prefs.edit()
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_NAME, user.getFullName())
                .putString(KEY_USER_ROLE, user.getRole())
                .putString(KEY_USER_PHONE, user.getPhone())
                .putString(KEY_USER_AVATAR, user.getAvatarUrl())
                .putBoolean(KEY_USER_IS_ACTIVE, user.isActive())
                .apply();
    }

    public void saveUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public void saveUserPhone(String phone) {
        prefs.edit().putString(KEY_USER_PHONE, phone).apply();
    }

    public void saveUserAvatar(String avatarUrl) {
        prefs.edit().putString(KEY_USER_AVATAR, avatarUrl).apply();
    }
    
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }

    public String getUserPhone() {
        return prefs.getString(KEY_USER_PHONE, null);
    }

    public String getUserAvatar() {
        return prefs.getString(KEY_USER_AVATAR, null);
    }

    public boolean getUserIsActive() {
        return prefs.getBoolean(KEY_USER_IS_ACTIVE, true);
    }
    
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getUserRole());
    }
    
    public boolean isStaff() {
        return "staff".equalsIgnoreCase(getUserRole());
    }

    public long getTokenExpiresAt() {
        return prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0);
    }

    public boolean isTokenExpired() {
        long expiresAt = getTokenExpiresAt();
        return expiresAt == 0 || System.currentTimeMillis() >= expiresAt;
    }

    public User getCurrentUser() {
        if (!isLoggedIn()) return null;
        
        User user = new User();
        user.setId(getUserId());
        user.setEmail(getUserEmail());
        user.setFullName(getUserName());
        user.setRole(getUserRole());
        user.setPhone(getUserPhone());
        user.setAvatarUrl(getUserAvatar());
        user.setIsActive(getUserIsActive());
        return user;
    }
    
    public void clearSession() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_ROLE)
                .remove(KEY_USER_PHONE)
                .remove(KEY_USER_AVATAR)
                .remove(KEY_USER_IS_ACTIVE)
                .remove(KEY_TOKEN_EXPIRES_AT)
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }

    public void updateSession(String accessToken, String refreshToken) {
        updateSession(accessToken, refreshToken, 3600000L);
    }

    public void updateSession(String accessToken, String refreshToken, long expiresInMillis) {
        long expiresAt = System.currentTimeMillis() + expiresInMillis;
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
                .apply();
    }
}
