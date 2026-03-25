package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;

public class AdminProfile {

    @SerializedName("account_id")
    private String accountId;

    @SerializedName("auth_user_id")
    private String authUserId;

    @SerializedName("email")
    private String email;

    @SerializedName("username")
    private String username;

    @SerializedName("role")
    private String role;

    @SerializedName("status")
    private String status;

    @SerializedName("last_login_at")
    private String lastLoginAt;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("phone")
    private String phone;

    @SerializedName("job_title")
    private String jobTitle;

    @SerializedName("department")
    private String department;

    @SerializedName("note")
    private String note;

    public String getAccountId() {
        return accountId;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getLastLoginAt() {
        return lastLoginAt;
    }

    public String getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public String getNote() {
        return note;
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return email == null ? "" : email;
    }
}
