package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class UserCreateRequest {

    @SerializedName("auth_id")
    private String authId;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("email")
    private String email;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("role")
    private String role;

    @SerializedName("is_active")
    private Boolean isActive;

    public UserCreateRequest() {

    }

    public UserCreateRequest(String authId, String fullName, String phone, String email, String avatarUrl, String role, Boolean isActive) {
        this.authId = authId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.isActive = isActive;
    }

    public String getAuthId() {
        return authId;
    }

    public void setAuthId(String authId) {
        this.authId = authId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    // Backward-compatible alias while call sites are being migrated.
    public Boolean getActive() {
        return getIsActive();
    }

    // Backward-compatible alias while call sites are being migrated.
    public void setActive(Boolean active) {
        setIsActive(active);
    }
}
