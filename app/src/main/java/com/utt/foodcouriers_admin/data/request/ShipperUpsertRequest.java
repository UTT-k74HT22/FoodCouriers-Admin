package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class ShipperUpsertRequest {

    @SerializedName("user_id")
    private String userId;
    @SerializedName("restaurant_id")
    private String restaurantId;
    @SerializedName("role_in_restaurant")
    private String roleInRestaurant;
    @SerializedName("is_active")
    private Boolean isActive;
    @SerializedName("avatar_url")
    private String avatarUrl;

    private String fullName;
    private String phone;
    private String email;

    public ShipperUpsertRequest() {
    }

    public ShipperUpsertRequest(String userId, String restaurantId, String fullName, String phone, String email, String avatarUrl, Boolean isActive) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.roleInRestaurant = "shipper";
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.isActive = isActive != null ? isActive : true;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getRoleInRestaurant() { return roleInRestaurant; }
    public void setRoleInRestaurant(String roleInRestaurant) { this.roleInRestaurant = roleInRestaurant; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
