package com.utt.foodcouriers_admin.data.request;

import com.google.gson.annotations.SerializedName;

public class ShipperUpsertRequest {

    @SerializedName("user_id")
    private String userId;
    @SerializedName("restaurant_id")
    private String restaurantId;
    @SerializedName("license_plate")
    private String licensePlate;
    @SerializedName("vehicle_type")
    private String vehicleType;
    @SerializedName("is_available")
    private Boolean isAvailable;
    @SerializedName("is_active")
    private Boolean isActive;
    @SerializedName("total_delivered")
    private Integer totalDelivered;
    @SerializedName("total_revenue")
    private Long totalRevenue;
    private Double rating;
    @SerializedName("joined_at")
    private String joinedAt;

    private String fullName;
    private String phone;
    private String email;
    private String password;
    private String avatarUrl;

    public ShipperUpsertRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRestaurantId() { return restaurantId; }
    public void setRestaurantId(String restaurantId) { this.restaurantId = restaurantId; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean available) { isAvailable = available; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public Integer getTotalDelivered() { return totalDelivered; }
    public void setTotalDelivered(Integer totalDelivered) { this.totalDelivered = totalDelivered; }

    public Long getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Long totalRevenue) { this.totalRevenue = totalRevenue; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getJoinedAt() { return joinedAt; }
    public void setJoinedAt(String joinedAt) { this.joinedAt = joinedAt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}