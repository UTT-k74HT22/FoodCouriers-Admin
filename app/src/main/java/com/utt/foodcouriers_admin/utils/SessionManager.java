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
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    
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
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_NAME, user.getFullName())
                .putString(KEY_USER_ROLE, user.getRole())
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .apply();
    }
    
    public void updateUserInfo(User user) {
        prefs.edit()
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_USER_EMAIL, user.getEmail())
                .putString(KEY_USER_NAME, user.getFullName())
                .putString(KEY_USER_ROLE, user.getRole())
                .apply();
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
    
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getUserRole());
    }
    
    public boolean isStaff() {
        return "staff".equalsIgnoreCase(getUserRole());
    }
    
    public User getCurrentUser() {
        if (!isLoggedIn()) return null;
        
        User user = new User();
        user.setId(getUserId());
        user.setEmail(getUserEmail());
        user.setFullName(getUserName());
        user.setRole(getUserRole());
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
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .apply();
    }
}