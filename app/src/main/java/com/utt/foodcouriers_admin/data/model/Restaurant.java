package com.utt.foodcouriers_admin.data.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Restaurant implements Serializable {

    private String id;
    private String name;
    private String description;
    private String address;
    private String phone;
    @SerializedName("image_url")
    private String imageUrl;
    private double rating;
    @SerializedName("review_count")
    private int reviewCount;
    @SerializedName("is_active")
    private boolean isActive;
    @SerializedName("is_open")
    private boolean isOpen;
    @SerializedName("open_time")
    private String openTime;
    @SerializedName("close_time")
    private String closeTime;
    @SerializedName("delivery_fee")
    private int deliveryFee;
    @SerializedName("min_order")
    private int minOrder;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("updated_at")
    private String updatedAt;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getRating() {
        return rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public String getOpenTime() {
        return openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public int getDeliveryFee() {
        return deliveryFee;
    }

    public int getMinOrder() {
        return minOrder;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
