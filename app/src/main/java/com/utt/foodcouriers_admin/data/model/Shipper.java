package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Shipper implements Serializable {

    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("restaurant_id")
    private String restaurantId;
    @SerializedName("role_in_restaurant")
    private String roleInRestaurant;
    @SerializedName("is_active")
    private boolean isActive;
    @SerializedName("created_at")
    private String createdAt;

    private User user;

    private String fullName;
    private String phone;
    private String email;
    @SerializedName("avatar_url")
    private String avatarUrl;
    private String restaurantName;

    public Shipper() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getRoleInRestaurant() { return roleInRestaurant; }
    public void setRoleInRestaurant(String roleInRestaurant) { this.roleInRestaurant = roleInRestaurant; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFullName() { 
        if (user != null) return user.getFullName();
        return fullName; 
    }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { 
        if (user != null) return user.getPhone();
        return phone; 
    }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { 
        if (user != null) return user.getEmail();
        return email; 
    }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { 
        if (user != null) return user.getAvatarUrl();
        return avatarUrl; 
    }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public boolean isShipper() {
        return "shipper".equalsIgnoreCase(roleInRestaurant);
    }
}
