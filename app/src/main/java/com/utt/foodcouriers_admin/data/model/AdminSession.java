package com.utt.foodcouriers_admin.data.model;

public class AdminSession {

    private final AdminProfile profile;
    private final String accessToken;
    private final String refreshToken;

    public AdminSession(AdminProfile profile, String accessToken, String refreshToken) {
        this.profile = profile;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public AdminProfile getProfile() {
        return profile;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
