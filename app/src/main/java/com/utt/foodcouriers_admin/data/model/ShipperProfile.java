package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ShipperProfile implements Serializable {

    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("restaurant_id")
    private String restaurantId;
    @SerializedName("license_plate")
    private String licensePlate;
    @SerializedName("vehicle_type")
    private String vehicleType;
    @SerializedName("is_available")
    private boolean isAvailable;
    @SerializedName("is_active")
    private boolean isActive;
    @SerializedName("total_delivered")
    private int totalDelivered;
    @SerializedName("total_revenue")
    private long totalRevenue;
    private Double rating;
    @SerializedName("joined_at")
    private String joinedAt;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("user")
    private NestedUser user;
    
    @SerializedName("restaurant")
    private NestedRestaurant restaurant;

    public ShipperProfile() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getTotalDelivered() { return totalDelivered; }
    public void setTotalDelivered(int totalDelivered) { this.totalDelivered = totalDelivered; }

    public long getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(long totalRevenue) { this.totalRevenue = totalRevenue; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() {
        return user != null ? user.fullName : null;
    }

    public String getPhone() {
        return user != null ? user.phone : null;
    }

    public String getEmail() {
        return user != null ? user.email : null;
    }

    public String getAvatarUrl() {
        return user != null ? user.avatarUrl : null;
    }

    public String getRestaurantName() {
        return restaurant != null ? restaurant.name : null;
    }

    private static class NestedUser implements Serializable {
        @SerializedName("full_name")
        String fullName;
        @SerializedName("phone")
        String phone;
        @SerializedName("email")
        String email;
        @SerializedName("avatar_url")
        String avatarUrl;
    }

    private static class NestedRestaurant implements Serializable {
        @SerializedName("name")
        String name;
    }
}