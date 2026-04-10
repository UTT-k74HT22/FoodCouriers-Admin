package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class AdminCreateUserAccountRequest {

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("role")
    private String role;

    @SerializedName("is_active")
    private Boolean isActive;

    @SerializedName("email_confirm")
    private Boolean emailConfirm;

    public AdminCreateUserAccountRequest() {
    }

    public AdminCreateUserAccountRequest(String email, String password, String fullName, String phone, String avatarUrl, String role, Boolean isActive, Boolean emailConfirm) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.isActive = isActive;
        this.emailConfirm = emailConfirm;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Boolean getEmailConfirm() {
        return emailConfirm;
    }

    public void setEmailConfirm(Boolean emailConfirm) {
        this.emailConfirm = emailConfirm;
    }
}
