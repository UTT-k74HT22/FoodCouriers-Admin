package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class AdminResetUserPasswordRequest {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("auth_id")
    private String authId;

    @SerializedName("new_password")
    private String newPassword;

    @SerializedName("force_change_password")
    private Boolean forceChangePassword;

    public AdminResetUserPasswordRequest() {
    }

    public AdminResetUserPasswordRequest(String userId, String authId, String newPassword, Boolean forceChangePassword) {
        this.userId = userId;
        this.authId = authId;
        this.newPassword = newPassword;
        this.forceChangePassword = forceChangePassword;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public Boolean getForceChangePassword() {
        return forceChangePassword;
    }

    public void setForceChangePassword(Boolean forceChangePassword) {
        this.forceChangePassword = forceChangePassword;
    }
}
